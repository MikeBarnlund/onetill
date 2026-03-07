<?php
/**
 * Order REST API endpoints.
 *
 * Handles:
 * - POST /onetill/v1/orders           — Create order (with idempotency)
 * - POST /onetill/v1/orders/batch     — Batch create orders (offline sync)
 * - GET  /onetill/v1/orders           — List device orders
 * - POST /onetill/v1/orders/{id}/refund — Full order refund
 *
 * @package OneTill
 */

namespace OneTill;

defined( 'ABSPATH' ) || exit;

/**
 * Class API_Orders
 */
class API_Orders {

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
			'/orders',
			array(
				array(
					'methods'             => 'POST',
					'callback'            => array( $this, 'create_order' ),
					'permission_callback' => array( $this, 'check_permissions' ),
				),
				array(
					'methods'             => 'GET',
					'callback'            => array( $this, 'get_orders' ),
					'permission_callback' => array( $this, 'check_permissions' ),
					'args'                => array(
						'page'       => array(
							'default'           => 1,
							'sanitize_callback' => 'absint',
						),
						'per_page'   => array(
							'default'           => 20,
							'sanitize_callback' => 'absint',
						),
						'device_id'  => array(
							'required'          => true,
							'sanitize_callback' => 'sanitize_text_field',
						),
						'date_after' => array(
							'sanitize_callback' => 'sanitize_text_field',
						),
					),
				),
			)
		);

		register_rest_route(
			self::NAMESPACE,
			'/orders/batch',
			array(
				'methods'             => 'POST',
				'callback'            => array( $this, 'create_orders_batch' ),
				'permission_callback' => array( $this, 'check_permissions' ),
			)
		);

		register_rest_route(
			self::NAMESPACE,
			'/orders/(?P<id>\d+)/refund',
			array(
				'methods'             => 'POST',
				'callback'            => array( $this, 'refund_order' ),
				'permission_callback' => array( $this, 'check_permissions' ),
				'args'                => array(
					'id' => array(
						'required'          => true,
						'sanitize_callback' => 'absint',
					),
				),
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
		// TODO: Validate WooCommerce API key authentication.
		return true;
	}

	/**
	 * Create a single order.
	 *
	 * Uses wc_create_order() for HPOS compatibility. Checks idempotency key
	 * to prevent duplicate orders on retry. Stores POS metadata as order meta:
	 * _onetill_device_id, _onetill_device_name, _onetill_payment_method,
	 * _onetill_transaction_id, _onetill_card_brand, _onetill_card_last4.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response|\WP_Error
	 */
	public function create_order( $request ) {
		// TODO: Implement order creation with:
		// 1. Idempotency check against wp_onetill_idempotency table
		// 2. wc_create_order() (HPOS-compatible)
		// 3. Line items, coupons, tax calculation via WooCommerce
		// 4. POS metadata storage
		// 5. Stock decrement via wc_update_product_stock()
		// 6. Idempotency key storage (TTL: 24 hours)
		// 7. Digital receipt email trigger if customer_email is set
	}

	/**
	 * Batch create orders (offline sync).
	 *
	 * Processes up to 50 orders sequentially. Each order is independently
	 * idempotent — already-created orders return from cache.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response|\WP_Error
	 */
	public function create_orders_batch( $request ) {
		// TODO: Implement batch order creation.
		// - Max 50 orders per request
		// - Process sequentially for correct stock decrements
		// - Failed orders do not prevent others from being created
	}

	/**
	 * Get orders created by a specific device.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response|\WP_Error
	 */
	public function get_orders( $request ) {
		// TODO: Implement device order listing.
	}

	/**
	 * Full refund an order.
	 *
	 * Creates WooCommerce refund record. The actual Stripe refund is
	 * initiated by the S700 app via Stripe Terminal SDK — the plugin
	 * does NOT touch Stripe directly (no PCI scope).
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response|\WP_Error
	 */
	public function refund_order( $request ) {
		// TODO: Implement full order refund with optional restock.
	}

	/**
	 * Check an idempotency key and return cached response if it exists.
	 *
	 * @param string $idempotency_key The key to check.
	 * @return array|null Cached response data or null.
	 */
	private function check_idempotency( $idempotency_key ) {
		// TODO: Check wp_onetill_idempotency table.
		return null;
	}

	/**
	 * Store an idempotency key with the response hash.
	 *
	 * @param string $idempotency_key The key.
	 * @param int    $order_id        The created order ID.
	 * @param string $response_hash   Hash of the response.
	 */
	private function store_idempotency( $idempotency_key, $order_id, $response_hash ) {
		// TODO: Insert into wp_onetill_idempotency table.
	}

	/**
	 * Clean up expired idempotency keys (older than 24 hours).
	 *
	 * Called by WP-Cron daily.
	 */
	public function cleanup_idempotency() {
		// TODO: Delete rows from wp_onetill_idempotency where created_at < NOW() - 24 hours.
	}

	/**
	 * Resolve the stock target for a line item.
	 *
	 * When manage_stock = "parent" on a variation, stock decrements
	 * must target the parent product ID, not the variation ID.
	 *
	 * @param int      $product_id   The product ID.
	 * @param int|null $variation_id The variation ID, if applicable.
	 * @return int The product ID to decrement stock on.
	 */
	private function resolve_stock_target( $product_id, $variation_id ) {
		// TODO: Implement manage_stock = "parent" resolution.
		return $variation_id ? $variation_id : $product_id;
	}
}
