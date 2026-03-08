<?php
/**
 * Coupon validation REST API endpoint.
 *
 * Handles:
 * - POST /onetill/v1/coupons/validate — Validate coupon before applying
 *
 * @package OneTill
 */

namespace OneTill;

defined( 'ABSPATH' ) || exit;

/**
 * Class API_Coupons
 */
class API_Coupons {

	/**
	 * REST API namespace.
	 *
	 * @var string
	 */
	private const NAMESPACE = 'onetill/v1';

	/**
	 * Get the map of WC coupon error codes to OneTill reason codes.
	 *
	 * @return array
	 */
	private function get_error_map() {
		return array(
			\WC_Coupon::E_WC_COUPON_EXPIRED                => 'coupon_expired',
			\WC_Coupon::E_WC_COUPON_USAGE_LIMIT_REACHED    => 'coupon_usage_limit',
			\WC_Coupon::E_WC_COUPON_NOT_EXIST              => 'coupon_not_found',
			\WC_Coupon::E_WC_COUPON_INVALID_REMOVED        => 'coupon_not_found',
			\WC_Coupon::E_WC_COUPON_MIN_SPEND_LIMIT_NOT_MET => 'coupon_min_amount',
			\WC_Coupon::E_WC_COUPON_NOT_APPLICABLE         => 'coupon_excluded_products',
			\WC_Coupon::E_WC_COUPON_EXCLUDED_PRODUCTS      => 'coupon_excluded_products',
			\WC_Coupon::E_WC_COUPON_EXCLUDED_CATEGORIES    => 'coupon_excluded_products',
		);
	}

	/**
	 * Register REST API routes.
	 */
	public function register_routes() {
		register_rest_route(
			self::NAMESPACE,
			'/coupons/validate',
			array(
				'methods'             => 'POST',
				'callback'            => array( $this, 'validate_coupon' ),
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
	 * Validate a coupon code against the provided line items.
	 *
	 * Uses WooCommerce's WC_Coupon and WC_Discounts for accurate
	 * validation and discount calculation.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response|\WP_Error
	 */
	public function validate_coupon( $request ) {
		$body       = $request->get_json_params();
		$code       = isset( $body['code'] ) ? sanitize_text_field( $body['code'] ) : '';
		$line_items = isset( $body['line_items'] ) ? $body['line_items'] : array();

		if ( empty( $code ) ) {
			return new \WP_Error( 'missing_code', 'Coupon code is required.', array( 'status' => 400 ) );
		}

		// Look up the coupon.
		$coupon = new \WC_Coupon( $code );

		if ( ! $coupon->get_id() ) {
			return new \WP_REST_Response( array(
				'valid'   => false,
				'code'    => $code,
				'reason'  => 'coupon_not_found',
				'message' => __( 'This coupon does not exist.', 'onetill' ),
			), 200 );
		}

		// Build a temporary order to validate the coupon against.
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

			$line_item->set_subtotal( wc_format_decimal( (float) $price * $quantity ) );
			$line_item->set_total( wc_format_decimal( (float) $price * $quantity ) );

			$order->add_item( $line_item );
		}

		$order->calculate_totals( false );

		// Use WC_Discounts for validation.
		$discounts = new \WC_Discounts( $order );
		$valid     = $discounts->is_coupon_valid( $coupon );

		if ( is_wp_error( $valid ) ) {
			$error_code = $valid->get_error_code();
			$error_map  = $this->get_error_map();
			$reason     = isset( $error_map[ $error_code ] ) ? $error_map[ $error_code ] : 'coupon_invalid';

			// Clean up the temporary order.
			$order->delete( true );

			return new \WP_REST_Response( array(
				'valid'   => false,
				'code'    => $code,
				'reason'  => $reason,
				'message' => $valid->get_error_message(),
			), 200 );
		}

		// Calculate the discount amount.
		$result = $discounts->apply_coupon( $coupon );

		if ( is_wp_error( $result ) ) {
			$order->delete( true );

			return new \WP_REST_Response( array(
				'valid'   => false,
				'code'    => $code,
				'reason'  => 'coupon_invalid',
				'message' => $result->get_error_message(),
			), 200 );
		}

		$discount_total = array_sum( $discounts->get_discounts_by_coupon() );

		// Clean up the temporary order.
		$order->delete( true );

		// Map WC discount type to a simpler label.
		$type_map = array(
			'percent'       => 'percent',
			'fixed_cart'    => 'fixed_cart',
			'fixed_product' => 'fixed_product',
		);
		$coupon_type = $coupon->get_discount_type();

		return new \WP_REST_Response( array(
			'valid'  => true,
			'coupon' => array(
				'code'           => $coupon->get_code(),
				'type'           => isset( $type_map[ $coupon_type ] ) ? $type_map[ $coupon_type ] : $coupon_type,
				'amount'         => wc_format_decimal( $coupon->get_amount(), 2 ),
				'description'    => $coupon->get_description(),
				'discount_total' => wc_format_decimal( $discount_total, 2 ),
			),
		), 200 );
	}
}
