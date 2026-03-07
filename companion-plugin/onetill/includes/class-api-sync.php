<?php
/**
 * Sync REST API endpoints.
 *
 * Handles:
 * - GET  /onetill/v1/sync/heartbeat — Lightweight connectivity check
 * - POST /onetill/v1/sync/stock     — Batch stock update
 *
 * @package OneTill
 */

namespace OneTill;

defined( 'ABSPATH' ) || exit;

/**
 * Class API_Sync
 */
class API_Sync {

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
			'/sync/heartbeat',
			array(
				'methods'             => 'GET',
				'callback'            => array( $this, 'heartbeat' ),
				'permission_callback' => array( $this, 'check_permissions' ),
			)
		);

		register_rest_route(
			self::NAMESPACE,
			'/sync/stock',
			array(
				'methods'             => 'POST',
				'callback'            => array( $this, 'update_stock' ),
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
		// TODO: Validate WooCommerce API key authentication.
		return true;
	}

	/**
	 * Heartbeat endpoint.
	 *
	 * Lightweight connectivity check pinged every 30 seconds by the S700.
	 * Returns server_time and pending_changes count (changes since last
	 * device sync). If pending_changes > 0, the S700 triggers a delta sync.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response|\WP_Error
	 */
	public function heartbeat( $request ) {
		// TODO: Implement heartbeat with:
		// - ok: true
		// - server_time: current UTC time
		// - pending_changes: count of change_log entries since device's last_sync
	}

	/**
	 * Batch stock update.
	 *
	 * Accepts relative quantity changes (not absolute values) to prevent
	 * race conditions when multiple devices or the online store are
	 * selling simultaneously.
	 *
	 * Uses wc_update_product_stock() with 'decrease' or 'increase' operation.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response|\WP_Error
	 */
	public function update_stock( $request ) {
		// TODO: Implement batch stock update with:
		// - Relative quantity changes (quantity_change field)
		// - manage_stock = "parent" resolution (decrement parent, not variation)
		// - wc_update_product_stock() for each update
	}

	/**
	 * Resolve the stock target for a product/variation.
	 *
	 * When a variation has manage_stock = "parent", stock decrements
	 * must target the parent product ID, not the variation ID.
	 *
	 * @param int      $product_id   The product ID.
	 * @param int|null $variation_id The variation ID, if applicable.
	 * @return int The ID to update stock on.
	 */
	private function resolve_stock_target( $product_id, $variation_id ) {
		// TODO: Implement manage_stock = "parent" resolution.
		return $variation_id ? $variation_id : $product_id;
	}
}
