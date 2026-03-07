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
		if ( ! current_user_can( 'manage_woocommerce' ) ) {
			return new \WP_Error(
				'onetill_rest_forbidden',
				__( 'Sorry, you are not allowed to access this resource.', 'onetill' ),
				array( 'status' => 403 )
			);
		}
		return true;
	}

	/**
	 * Heartbeat endpoint.
	 *
	 * Lightweight connectivity check pinged every 30 seconds by the S700.
	 * Returns server_time and pending_changes count (changes since the
	 * device's last delta sync). Updates the device's last_seen timestamp.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response
	 */
	public function heartbeat( $request ) {
		global $wpdb;

		$now             = current_time( 'mysql', true );
		$pending_changes = 0;

		// Find the device making this request via WC API key.
		$device = $this->get_device_from_request( $request );

		if ( $device ) {
			// Update last_seen.
			$wpdb->update(
				$wpdb->prefix . 'onetill_devices',
				array( 'last_seen' => $now ),
				array( 'id' => $device['id'] ),
				array( '%s' ),
				array( '%s' )
			);

			// Count changes since device's last sync.
			if ( ! empty( $device['last_sync'] ) ) {
				$pending_changes = (int) $wpdb->get_var(
					$wpdb->prepare(
						"SELECT COUNT(*) FROM {$wpdb->prefix}onetill_change_log WHERE timestamp > %s",
						$device['last_sync']
					)
				);
			} else {
				// Device has never synced — everything is pending.
				$pending_changes = (int) $wpdb->get_var(
					"SELECT COUNT(*) FROM {$wpdb->prefix}onetill_change_log"
				);
			}
		}

		return new \WP_REST_Response( array(
			'ok'              => true,
			'server_time'     => gmdate( 'c' ),
			'pending_changes' => $pending_changes,
		), 200 );
	}

	/**
	 * Batch stock update.
	 *
	 * Accepts relative quantity changes (not absolute values) to prevent
	 * race conditions when multiple devices or the online store are
	 * selling simultaneously.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response
	 */
	public function update_stock( $request ) {
		$body    = $request->get_json_params();
		$updates = isset( $body['updates'] ) ? $body['updates'] : array();

		if ( empty( $updates ) || ! is_array( $updates ) ) {
			return new \WP_REST_Response( array(
				'success' => false,
				'error'   => 'validation_error',
				'message' => 'Missing or empty updates array.',
			), 400 );
		}

		$results = array();

		foreach ( $updates as $update ) {
			$product_id      = isset( $update['product_id'] ) ? absint( $update['product_id'] ) : 0;
			$variation_id    = isset( $update['variation_id'] ) ? absint( $update['variation_id'] ) : null;
			$quantity_change = isset( $update['quantity_change'] ) ? (int) $update['quantity_change'] : 0;

			if ( ! $product_id || 0 === $quantity_change ) {
				$results[] = array(
					'product_id'   => $product_id,
					'variation_id' => $variation_id,
					'success'      => false,
					'error'        => 'Invalid product_id or quantity_change.',
				);
				continue;
			}

			// Resolve the actual stock target (handles manage_stock = "parent").
			$stock_target_id = $this->resolve_stock_target( $product_id, $variation_id );

			$target_product = wc_get_product( $stock_target_id );
			if ( ! $target_product ) {
				$results[] = array(
					'product_id'   => $product_id,
					'variation_id' => $variation_id,
					'success'      => false,
					'error'        => 'Product not found.',
				);
				continue;
			}

			if ( ! $target_product->get_manage_stock() ) {
				$results[] = array(
					'product_id'   => $product_id,
					'variation_id' => $variation_id,
					'success'      => false,
					'error'        => 'Stock management not enabled for this product.',
				);
				continue;
			}

			// Use wc_update_product_stock with increase/decrease.
			$operation = $quantity_change > 0 ? 'increase' : 'decrease';
			$amount    = abs( $quantity_change );

			$new_stock = wc_update_product_stock( $stock_target_id, $amount, $operation );

			if ( false === $new_stock || is_wp_error( $new_stock ) ) {
				$results[] = array(
					'product_id'   => $product_id,
					'variation_id' => $variation_id,
					'success'      => false,
					'error'        => 'Failed to update stock.',
				);
				continue;
			}

			$result = array(
				'product_id' => $product_id,
				'new_stock'  => (int) $new_stock,
				'success'    => true,
			);

			if ( $variation_id ) {
				$result['variation_id'] = $variation_id;
			}

			$results[] = $result;
		}

		return new \WP_REST_Response( array( 'results' => $results ), 200 );
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
		if ( $variation_id ) {
			$variation = wc_get_product( $variation_id );
			if ( $variation && 'parent' === $variation->get_manage_stock() ) {
				return $variation->get_parent_id();
			}
			return $variation_id;
		}
		return $product_id;
	}

	/**
	 * Look up the OneTill device record from the current WC API request.
	 *
	 * Matches the WooCommerce API key used for authentication against
	 * the api_key_id stored in wp_onetill_devices.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return array|null Device row or null.
	 */
	private function get_device_from_request( $request ) {
		global $wpdb;

		// WooCommerce REST auth passes the consumer key via HTTP Basic.
		// Extract it from the Authorization header and look up the key_id.
		$consumer_key = $this->get_consumer_key_from_request( $request );
		if ( ! $consumer_key ) {
			return null;
		}

		$key_id = $wpdb->get_var(
			$wpdb->prepare(
				"SELECT key_id FROM {$wpdb->prefix}woocommerce_api_keys WHERE consumer_key = %s",
				wc_api_hash( $consumer_key )
			)
		);

		if ( ! $key_id ) {
			return null;
		}

		return $wpdb->get_row(
			$wpdb->prepare(
				"SELECT * FROM {$wpdb->prefix}onetill_devices WHERE api_key_id = %d AND status = 'active'",
				$key_id
			),
			ARRAY_A
		);
	}

	/**
	 * Extract the consumer key from the request's HTTP Basic auth.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return string|null The consumer key or null.
	 */
	private function get_consumer_key_from_request( $request ) {
		// Check Authorization header.
		$auth_header = $request->get_header( 'authorization' );
		if ( $auth_header && 0 === strpos( $auth_header, 'Basic ' ) ) {
			$decoded = base64_decode( substr( $auth_header, 6 ) );
			if ( $decoded && strpos( $decoded, ':' ) !== false ) {
				list( $key ) = explode( ':', $decoded, 2 );
				return $key;
			}
		}

		// Fallback: check query params (WC allows consumer_key as query param).
		$params = $request->get_query_params();
		if ( ! empty( $params['consumer_key'] ) ) {
			return sanitize_text_field( $params['consumer_key'] );
		}

		// Fallback: check PHP_AUTH_USER (set by Apache/nginx for Basic auth).
		if ( ! empty( $_SERVER['PHP_AUTH_USER'] ) ) {
			return sanitize_text_field( wp_unslash( $_SERVER['PHP_AUTH_USER'] ) );
		}

		return null;
	}

	/**
	 * Update a device's last_sync timestamp.
	 *
	 * Called by API_Products after a successful delta sync.
	 *
	 * @param \WP_REST_Request $request The request.
	 */
	public function update_device_last_sync( $request ) {
		global $wpdb;

		$device = $this->get_device_from_request( $request );
		if ( $device ) {
			$wpdb->update(
				$wpdb->prefix . 'onetill_devices',
				array( 'last_sync' => current_time( 'mysql', true ) ),
				array( 'id' => $device['id'] ),
				array( '%s' ),
				array( '%s' )
			);
		}
	}
}
