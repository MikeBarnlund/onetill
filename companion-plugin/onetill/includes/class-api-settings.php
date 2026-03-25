<?php
/**
 * Settings REST API endpoint.
 *
 * Handles:
 * - GET /onetill/v1/settings — Store settings, tax rates, plugin info
 *
 * @package OneTill
 */

namespace OneTill;

defined( 'ABSPATH' ) || exit;

/**
 * Class API_Settings
 */
class API_Settings {

	/**
	 * REST API namespace.
	 *
	 * @var string
	 */
	private const NAMESPACE = 'onetill/v1';

	/**
	 * Register REST API routes.
	 */
	public function register_routes() {
		register_rest_route(
			self::NAMESPACE,
			'/settings',
			array(
				'methods'             => 'GET',
				'callback'            => array( $this, 'get_settings' ),
				'permission_callback' => array( $this, 'check_permissions' ),
			)
		);
	}

	/**
	 * Check that the request has valid WooCommerce API credentials.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return bool|\WP_Error
	 */
	public function check_permissions( $request ) {
		return Authenticator::check( $request );
	}

	/**
	 * Get store settings.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response
	 */
	public function get_settings( $request ) {
		// Tax rates.
		$tax_rates    = array();
		$tax_enabled  = wc_tax_enabled();

		if ( $tax_enabled ) {
			// Use WC_Tax::find_rates() with the shop's base address so only
			// location-applicable rates are returned (works with tax plugins,
			// automated tax services, and manually entered rates).
			$location = array(
				'country'  => WC()->countries->get_base_country(),
				'state'    => WC()->countries->get_base_state(),
				'postcode' => WC()->countries->get_base_postcode(),
				'city'     => WC()->countries->get_base_city(),
			);

			// Standard class rates at shop location.
			$standard_rates = \WC_Tax::find_rates( $location );
			foreach ( $standard_rates as $rate_id => $rate ) {
				$tax_rates[] = array(
					'id'       => (int) $rate_id,
					'country'  => $rate['country'],
					'state'    => $rate['state'],
					'rate'     => $rate['rate'],
					'name'     => $rate['label'],
					'shipping' => 'yes' === $rate['shipping'],
					'compound' => 'yes' === $rate['compound'],
					'class'    => 'standard',
				);
			}

			// Per-class rates at shop location.
			$tax_classes = \WC_Tax::get_tax_classes();
			foreach ( $tax_classes as $tax_class ) {
				$slug        = sanitize_title( $tax_class );
				$class_rates = \WC_Tax::find_rates( array_merge( $location, array( 'tax_class' => $slug ) ) );
				foreach ( $class_rates as $rate_id => $rate ) {
					$tax_rates[] = array(
						'id'       => (int) $rate_id,
						'country'  => $rate['country'],
						'state'    => $rate['state'],
						'rate'     => $rate['rate'],
						'name'     => $rate['label'],
						'shipping' => 'yes' === $rate['shipping'],
						'compound' => 'yes' === $rate['compound'],
						'class'    => $slug,
					);
				}
			}

			// Fallback: automated tax services (WooCommerce Tax, TaxJar, Avalara)
			// don't populate the tax_rates table until a calculation is triggered.
			// Use a temp order to discover effective rates when find_rates() is empty.
			if ( empty( $tax_rates ) ) {
				$tax_rates = $this->discover_rates_via_order( $tax_classes, $location );
			}
		}

		// Published product count.
		$product_counts = wp_count_posts( 'product' );
		$product_count  = isset( $product_counts->publish ) ? (int) $product_counts->publish : 0;

		return new \WP_REST_Response( array(
			'store' => array(
				'name'               => get_bloginfo( 'name' ),
				'url'                => get_site_url(),
				'currency'           => get_woocommerce_currency(),
				'currency_position'  => get_option( 'woocommerce_currency_pos', 'left' ),
				'thousand_separator' => get_option( 'woocommerce_price_thousand_sep', ',' ),
				'decimal_separator'  => get_option( 'woocommerce_price_decimal_sep', '.' ),
				'num_decimals'       => (int) get_option( 'woocommerce_price_num_decimals', 2 ),
			),
			'tax' => array(
				'enabled'            => $tax_enabled,
				'prices_include_tax' => 'yes' === get_option( 'woocommerce_prices_include_tax', 'no' ),
				'calc_taxes'         => 'yes' === get_option( 'woocommerce_calc_taxes', 'no' ),
				'tax_rates'          => $tax_rates,
			),
			'plugin_version' => ONETILL_VERSION,
			'wc_version'     => defined( 'WC_VERSION' ) ? WC_VERSION : '',
			'wp_version'     => get_bloginfo( 'version' ),
			'product_count'  => $product_count,
			'timezone'       => wp_timezone_string(),
		), 200 );
	}

	/**
	 * Discover tax rates by creating a temporary order and running calculate_taxes().
	 *
	 * Automated tax services (WooCommerce Tax, TaxJar, Avalara) only populate
	 * rates when a calculation is triggered. This creates a temp order with a
	 * $100 line item per tax class to discover the effective rates.
	 *
	 * @param array $tax_classes Custom tax class names (excludes standard).
	 * @param array $location    Shop base address components.
	 * @return array Tax rate objects in the same format as the find_rates() path.
	 */
	private function discover_rates_via_order( $tax_classes, $location ) {
		$order = wc_create_order();

		if ( is_wp_error( $order ) ) {
			return array();
		}

		// Set billing/shipping address to shop base.
		$order->set_billing_country( $location['country'] );
		$order->set_billing_state( $location['state'] );
		$order->set_billing_postcode( $location['postcode'] );
		$order->set_billing_city( $location['city'] );
		$order->set_shipping_country( $location['country'] );
		$order->set_shipping_state( $location['state'] );
		$order->set_shipping_postcode( $location['postcode'] );
		$order->set_shipping_city( $location['city'] );

		// Add a $100 line item for each tax class (standard + custom).
		$all_classes = array_merge( array( '' ), array_map( 'sanitize_title', $tax_classes ) );
		foreach ( $all_classes as $class_slug ) {
			$line_item = new \WC_Order_Item_Product();
			$line_item->set_quantity( 1 );
			$line_item->set_subtotal( '100' );
			$line_item->set_total( '100' );
			$line_item->set_tax_class( $class_slug );
			$order->add_item( $line_item );
		}

		$order->calculate_taxes();

		// Extract discovered rates.
		$tax_rates = array();
		foreach ( $order->get_taxes() as $tax_item ) {
			$rate_id = $tax_item->get_rate_id();
			if ( ! $rate_id ) {
				continue;
			}

			$rate_data = \WC_Tax::_get_tax_rate( $rate_id );
			if ( ! $rate_data ) {
				continue;
			}

			$tax_class = $rate_data['tax_rate_class'];
			$class_key = empty( $tax_class ) ? 'standard' : $tax_class;

			$tax_rates[] = array(
				'id'       => (int) $rate_id,
				'country'  => $rate_data['tax_rate_country'],
				'state'    => $rate_data['tax_rate_state'],
				'rate'     => $rate_data['tax_rate'],
				'name'     => $rate_data['tax_rate_name'],
				'shipping' => '1' === $rate_data['tax_rate_shipping'],
				'compound' => '1' === $rate_data['tax_rate_compound'],
				'class'    => $class_key,
			);
		}

		$order->delete( true );

		return $tax_rates;
	}
}
