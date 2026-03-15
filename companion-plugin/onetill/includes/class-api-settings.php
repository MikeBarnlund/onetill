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
					'shipping' => $rate['shipping'],
					'compound' => $rate['compound'],
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
						'shipping' => $rate['shipping'],
						'compound' => $rate['compound'],
						'class'    => $slug,
					);
				}
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
}
