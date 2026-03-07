<?php
/**
 * Webhook and change log management.
 *
 * Listens to WooCommerce product/order hooks and records changes
 * in the wp_onetill_change_log table. The heartbeat endpoint reads
 * this table to determine pending_changes for each device.
 *
 * Also handles the critical WooCommerce quirk: variation changes
 * don't update parent date_modified. This class explicitly touches
 * the parent product timestamp on variation saves and stock changes.
 *
 * @package OneTill
 */

namespace OneTill;

defined( 'ABSPATH' ) || exit;

/**
 * Class Webhooks
 */
class Webhooks {

	/**
	 * Handle product updated hook.
	 *
	 * @param int $product_id The product ID.
	 */
	public function on_product_updated( $product_id ) {
		// TODO: Insert 'product.updated' into change log.
	}

	/**
	 * Handle new product hook.
	 *
	 * @param int $product_id The product ID.
	 */
	public function on_product_created( $product_id ) {
		// TODO: Insert 'product.created' into change log.
	}

	/**
	 * Handle product trashed/deleted hook.
	 *
	 * Also records the deletion in wp_onetill_deleted_products for delta sync.
	 *
	 * @param int $product_id The product ID.
	 */
	public function on_product_deleted( $product_id ) {
		// TODO: Insert 'product.deleted' into change log.
		// TODO: Insert into wp_onetill_deleted_products table.
	}

	/**
	 * Handle product stock changed hook.
	 *
	 * @param \WC_Product $product The product whose stock changed.
	 */
	public function on_product_stock_changed( $product ) {
		// TODO: Insert 'product.stock_changed' into change log.
	}

	/**
	 * Handle variation stock changed hook.
	 *
	 * CRITICAL: Logs the PARENT product ID in the change log (not the
	 * variation ID), because the S700 fetches parent products with
	 * embedded variations. Also touches the parent's date_modified
	 * to fix the WooCommerce delta sync quirk.
	 *
	 * @param \WC_Product_Variation|int $variation The variation or variation ID.
	 */
	public function on_variation_stock_changed( $variation ) {
		// TODO: Implement:
		// 1. Resolve variation to WC_Product if int
		// 2. Get parent_id
		// 3. Insert 'product.stock_changed' with parent_id as resource_id
		// 4. Touch parent date_modified: $parent->set_date_modified(...); $parent->save();
	}

	/**
	 * Handle variation saved hook.
	 *
	 * CRITICAL: Logs the PARENT product ID in the change log and touches
	 * the parent's date_modified to fix the WooCommerce delta sync quirk.
	 *
	 * @param int $variation_id The variation ID.
	 * @param int $loop_index   The variation loop index.
	 */
	public function on_variation_saved( $variation_id, $loop_index ) {
		// TODO: Implement:
		// 1. Get variation product
		// 2. Get parent_id
		// 3. Insert 'product.updated' with parent_id as resource_id
		// 4. Touch parent date_modified
	}

	/**
	 * Handle new order hook.
	 *
	 * @param int $order_id The order ID.
	 */
	public function on_order_created( $order_id ) {
		// TODO: Insert 'order.created' into change log.
	}

	/**
	 * Insert an event into the change log.
	 *
	 * @param string      $event_type  The event type (e.g., 'product.updated').
	 * @param int         $resource_id The resource ID (product ID, order ID).
	 * @param string|null $payload     Optional JSON payload.
	 */
	private function log_change( $event_type, $resource_id, $payload = null ) {
		// TODO: Insert into wp_onetill_change_log using $wpdb->insert().
	}

	/**
	 * Touch a parent product's date_modified timestamp.
	 *
	 * Fixes the WooCommerce quirk where variation changes don't update
	 * the parent product's date_modified, breaking delta sync.
	 *
	 * @param int $parent_id The parent product ID.
	 */
	private function touch_parent_modified( $parent_id ) {
		// TODO: $parent->set_date_modified(current_time('timestamp', true)); $parent->save();
	}

	/**
	 * Get the count of pending changes since a given timestamp.
	 *
	 * Used by the heartbeat endpoint to tell the S700 whether a delta
	 * sync is needed.
	 *
	 * @param string $since ISO 8601 timestamp.
	 * @return int Number of pending changes.
	 */
	public function get_pending_changes_count( $since ) {
		// TODO: COUNT(*) from wp_onetill_change_log WHERE timestamp > $since.
		return 0;
	}

	/**
	 * Clean up old change log entries (older than 7 days).
	 *
	 * Called by WP-Cron daily.
	 */
	public function cleanup_change_log() {
		// TODO: DELETE FROM wp_onetill_change_log WHERE timestamp < NOW() - 7 days.
	}

	/**
	 * Clean up old deleted product records (older than 30 days).
	 *
	 * Called alongside change log cleanup.
	 */
	public function cleanup_deleted_products() {
		// TODO: DELETE FROM wp_onetill_deleted_products WHERE deleted_at < NOW() - 30 days.
	}
}
