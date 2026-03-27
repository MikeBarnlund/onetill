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
	 * Guard against recursive parent touches.
	 *
	 * When we call $parent->save() to touch date_modified, it fires
	 * woocommerce_update_product again. This flag prevents infinite loops.
	 *
	 * @var bool
	 */
	private $touching_parent = false;

	/**
	 * Handle product updated hook.
	 *
	 * @param int $product_id The product ID.
	 */
	public function on_product_updated( $product_id ) {
		if ( $this->touching_parent ) {
			return;
		}

		$product = wc_get_product( $product_id );
		if ( ! $product || $product->is_type( 'variation' ) ) {
			return;
		}

		$this->log_change( 'product.updated', $product_id );
	}

	/**
	 * Handle new product hook.
	 *
	 * @param int $product_id The product ID.
	 */
	public function on_product_created( $product_id ) {
		$product = wc_get_product( $product_id );
		if ( ! $product || $product->is_type( 'variation' ) ) {
			return;
		}

		$this->log_change( 'product.created', $product_id );
	}

	/**
	 * Handle product trashed/deleted hook.
	 *
	 * Records the deletion in wp_onetill_deleted_products for delta sync.
	 *
	 * @param int $product_id The product ID.
	 */
	public function on_product_deleted( $product_id ) {
		$this->log_change( 'product.deleted', $product_id );

		global $wpdb;

		// Track deletion for delta sync. Use REPLACE to handle re-deletes.
		$wpdb->replace(
			$wpdb->prefix . 'onetill_deleted_products',
			array(
				'product_id' => $product_id,
				'deleted_at' => current_time( 'mysql', true ),
			),
			array( '%d', '%s' )
		);
	}

	/**
	 * Handle product stock changed hook.
	 *
	 * @param \WC_Product|int $product The product whose stock changed, or a product ID.
	 */
	public function on_product_stock_changed( $product ) {
		if ( is_numeric( $product ) ) {
			$product = wc_get_product( $product );
		}
		if ( ! $product ) {
			return;
		}

		// For variations, this is handled by on_variation_stock_changed.
		if ( $product->is_type( 'variation' ) ) {
			return;
		}

		$this->log_change( 'product.stock_changed', $product->get_id() );
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
		if ( is_numeric( $variation ) ) {
			$variation = wc_get_product( $variation );
		}
		if ( ! $variation || ! $variation->get_parent_id() ) {
			return;
		}

		$parent_id = $variation->get_parent_id();

		$this->log_change( 'product.stock_changed', $parent_id );
		$this->touch_parent_modified( $parent_id );
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
		$variation = wc_get_product( $variation_id );
		if ( ! $variation || ! $variation->get_parent_id() ) {
			return;
		}

		$parent_id = $variation->get_parent_id();

		$this->log_change( 'product.updated', $parent_id );
		$this->touch_parent_modified( $parent_id );
	}

	/**
	 * Handle new order hook.
	 *
	 * @param int $order_id The order ID.
	 */
	public function on_order_created( $order_id ) {
		$this->log_change( 'order.created', $order_id );
	}

	/**
	 * Insert an event into the change log.
	 *
	 * @param string      $event_type  The event type (e.g., 'product.updated').
	 * @param int         $resource_id The resource ID (product ID, order ID).
	 * @param string|null $payload     Optional JSON payload.
	 */
	private function log_change( $event_type, $resource_id, $payload = null ) {
		global $wpdb;

		$wpdb->insert(
			$wpdb->prefix . 'onetill_change_log',
			array(
				'event_type'  => $event_type,
				'resource_id' => $resource_id,
				'timestamp'   => current_time( 'mysql', true ),
				'payload'     => $payload,
			),
			array( '%s', '%d', '%s', '%s' )
		);
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
		$parent = wc_get_product( $parent_id );
		if ( ! $parent ) {
			return;
		}

		$this->touching_parent = true;
		$parent->set_date_modified( current_time( 'timestamp', true ) );
		$parent->save();
		$this->touching_parent = false;
	}

	/**
	 * Get the count of pending changes since a given timestamp.
	 *
	 * @param string $since MySQL datetime string (UTC).
	 * @return int Number of pending changes.
	 */
	public function get_pending_changes_count( $since ) {
		global $wpdb;

		return (int) $wpdb->get_var(
			$wpdb->prepare(
				"SELECT COUNT(*) FROM {$wpdb->prefix}onetill_change_log WHERE timestamp > %s",
				$since
			)
		);
	}

	/**
	 * Clean up old change log entries (older than 7 days).
	 *
	 * Called by WP-Cron daily. Also cleans up deleted products (30 days)
	 * and expired idempotency keys (24 hours).
	 */
	public function cleanup_change_log() {
		global $wpdb;

		$wpdb->query(
			$wpdb->prepare(
				"DELETE FROM {$wpdb->prefix}onetill_change_log WHERE timestamp < %s",
				gmdate( 'Y-m-d H:i:s', time() - ( 7 * DAY_IN_SECONDS ) )
			)
		);

		$this->cleanup_deleted_products();
		$this->cleanup_idempotency();
	}

	/**
	 * Clean up old deleted product records (older than 30 days).
	 */
	public function cleanup_deleted_products() {
		global $wpdb;

		$wpdb->query(
			$wpdb->prepare(
				"DELETE FROM {$wpdb->prefix}onetill_deleted_products WHERE deleted_at < %s",
				gmdate( 'Y-m-d H:i:s', time() - ( 30 * DAY_IN_SECONDS ) )
			)
		);
	}

	/**
	 * Clean up expired idempotency keys (older than 24 hours).
	 */
	public function cleanup_idempotency() {
		global $wpdb;

		$wpdb->query(
			$wpdb->prepare(
				"DELETE FROM {$wpdb->prefix}onetill_idempotency WHERE created_at < %s",
				gmdate( 'Y-m-d H:i:s', time() - DAY_IN_SECONDS )
			)
		);
	}
}
