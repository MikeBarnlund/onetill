<?php
/**
 * Tax estimation REST API endpoint.
 *
 * Handles:
 * - POST /onetill/v1/taxes/estimate — Server-side tax calculation for POS cart
 *
 * @package OneTill
 */

namespace OneTill;

defined( 'ABSPATH' ) || exit;

/**
 * Class API_Tax
 */
class API_Tax {

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
			'/taxes/estimate',
			array(
				'methods'             => 'POST',
				'callback'            => array( $this, 'estimate_tax' ),
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
	 * Estimate tax for the given line items using WooCommerce's tax engine.
	 *
	 * Creates a temporary order, adds line items with their products (so
	 * WooCommerce knows each item's tax class), calculates taxes using the
	 * shop's base address, then cleans up.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response|\WP_Error
	 */
	public function estimate_tax( $request ) {
		$body       = $request->get_json_params();
		$line_items = isset( $body['line_items'] ) ? $body['line_items'] : array();

		if ( empty( $line_items ) ) {
			return new \WP_Error(
				'missing_line_items',
				'At least one line item is required.',
				array( 'status' => 400 )
			);
		}

		// Build a temporary order for WooCommerce tax calculation.
		$order = wc_create_order();

		foreach ( $line_items as $item ) {
			$product_id   = isset( $item['product_id'] ) ? absint( $item['product_id'] ) : 0;
			$variation_id = ! empty( $item['variation_id'] ) ? absint( $item['variation_id'] ) : 0;
			$quantity     = isset( $item['quantity'] ) ? absint( $item['quantity'] ) : 1;
			$price        = isset( $item['price'] ) ? $item['price'] : '0';

			$product = wc_get_product( $variation_id ? $variation_id : $product_id );

			$line_item = new \WC_Order_Item_Product();
			$line_item->set_quantity( $quantity );

			if ( $product ) {
				$line_item->set_product( $product );
			} else {
				$line_item->set_product_id( $product_id );
				if ( $variation_id ) {
					$line_item->set_variation_id( $variation_id );
				}
			}

			$line_total = wc_format_decimal( (float) $price * $quantity );
			$line_item->set_subtotal( $line_total );
			$line_item->set_total( $line_total );

			$order->add_item( $line_item );
		}

		// Set the billing address to the shop's base address for POS tax calculation.
		$order->set_billing_country( WC()->countries->get_base_country() );
		$order->set_billing_state( WC()->countries->get_base_state() );
		$order->set_billing_postcode( WC()->countries->get_base_postcode() );
		$order->set_billing_city( WC()->countries->get_base_city() );
		$order->set_shipping_country( WC()->countries->get_base_country() );
		$order->set_shipping_state( WC()->countries->get_base_state() );
		$order->set_shipping_postcode( WC()->countries->get_base_postcode() );
		$order->set_shipping_city( WC()->countries->get_base_city() );

		$order->calculate_taxes();
		$order->calculate_totals( false );

		// Extract tax total.
		$tax_total = $order->get_total_tax();

		// Build rates_by_class from the order's tax lines.
		$rates_by_class = array();
		foreach ( $order->get_taxes() as $tax_item ) {
			$rate_id = $tax_item->get_rate_id();
			if ( ! $rate_id ) {
				continue;
			}

			// Look up the tax class for this rate.
			$rate_data = \WC_Tax::_get_tax_rate( $rate_id );
			$tax_class = '';
			if ( $rate_data ) {
				$tax_class = $rate_data['tax_rate_class'];
			}
			$class_key = empty( $tax_class ) ? 'standard' : $tax_class;

			if ( ! isset( $rates_by_class[ $class_key ] ) ) {
				$rates_by_class[ $class_key ] = array();
			}

			$rates_by_class[ $class_key ][] = array(
				'id'       => (int) $rate_id,
				'name'     => $tax_item->get_label(),
				'rate'     => $rate_data ? $rate_data['tax_rate'] : '0',
				'compound' => $rate_data ? ( '1' === $rate_data['tax_rate_compound'] ) : false,
			);
		}

		// Clean up the temporary order.
		$order->delete( true );

		return new \WP_REST_Response( array(
			'tax_total'      => wc_format_decimal( $tax_total, 2 ),
			'rates_by_class' => $rates_by_class,
		), 200 );
	}
}
