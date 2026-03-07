<?php
/**
 * QR code pairing system.
 *
 * Manages the device pairing lifecycle:
 * - Token generation and storage
 * - QR code generation (via QR_Generator)
 * - Pairing completion and credential issuance
 * - Token cleanup
 *
 * REST endpoints:
 * - POST /onetill/v1/pair/complete — Called by S700 after scanning QR
 * - GET  /onetill/v1/pair/status   — Polled by WP Admin for pairing state
 *
 * AJAX handlers (WP Admin):
 * - onetill_initiate_pairing     — Generate QR code
 * - onetill_check_pairing_status — Poll for completion
 *
 * @package OneTill
 */

namespace OneTill;

defined( 'ABSPATH' ) || exit;

/**
 * Class Pairing
 */
class Pairing {

	/**
	 * REST API namespace.
	 *
	 * @var string
	 */
	private const NAMESPACE = 'onetill/v1';

	/**
	 * Token expiry in seconds (5 minutes).
	 *
	 * @var int
	 */
	private const TOKEN_EXPIRY = 300;

	/**
	 * Max pairing attempts per IP per hour.
	 *
	 * @var int
	 */
	private const RATE_LIMIT = 5;

	/**
	 * Register REST API routes.
	 */
	public function register_routes() {
		register_rest_route(
			self::NAMESPACE,
			'/pair/complete',
			array(
				'methods'             => 'POST',
				'callback'            => array( $this, 'complete_pairing' ),
				'permission_callback' => '__return_true',
			)
		);

		register_rest_route(
			self::NAMESPACE,
			'/pair/status',
			array(
				'methods'             => 'GET',
				'callback'            => array( $this, 'get_pairing_status' ),
				'permission_callback' => '__return_true',
			)
		);
	}

	/**
	 * AJAX handler: Initiate pairing.
	 *
	 * Called by WP Admin when merchant clicks "Pair New Device".
	 * Generates a pairing token, stores it, renders QR code as SVG.
	 */
	public function ajax_initiate_pairing() {
		check_ajax_referer( 'onetill_admin', 'nonce' );

		if ( ! current_user_can( 'manage_woocommerce' ) ) {
			wp_send_json_error( array( 'message' => __( 'Permission denied.', 'onetill' ) ), 403 );
		}

		if ( ! $this->check_rate_limit() ) {
			wp_send_json_error( array(
				'error'   => 'rate_limited',
				'message' => __( 'Too many pairing attempts. Please try again later.', 'onetill' ),
			), 429 );
		}

		global $wpdb;

		$token      = bin2hex( random_bytes( 32 ) );
		$nonce      = bin2hex( random_bytes( 4 ) );
		$now        = current_time( 'mysql', true );
		$expires_at = gmdate( 'Y-m-d H:i:s', time() + self::TOKEN_EXPIRY );

		$wpdb->insert(
			$wpdb->prefix . 'onetill_pairing_tokens',
			array(
				'token'      => $token,
				'nonce'      => $nonce,
				'created_at' => $now,
				'expires_at' => $expires_at,
				'status'     => 'pending',
				'ip_address' => $this->get_client_ip(),
			),
			array( '%s', '%s', '%s', '%s', '%s', '%s' )
		);

		$qr_payload  = $this->build_qr_payload( $token, $nonce );
		$qr_generator = new QR_Generator();
		$qr_svg      = $qr_generator->generate_svg( $qr_payload );

		wp_send_json_success( array(
			'qr_svg'     => $qr_svg,
			'token'      => $token,
			'expires_at' => gmdate( 'c', strtotime( $expires_at ) ),
			'poll_url'   => rest_url( self::NAMESPACE . '/pair/status' ) . '?token=' . $token,
		) );
	}

	/**
	 * AJAX handler: Check pairing status.
	 *
	 * Polled by WP Admin every 2 seconds to detect when S700 completes pairing.
	 */
	public function ajax_check_pairing_status() {
		check_ajax_referer( 'onetill_admin', 'nonce' );

		if ( ! current_user_can( 'manage_woocommerce' ) ) {
			wp_send_json_error( array( 'message' => __( 'Permission denied.', 'onetill' ) ), 403 );
		}

		$token = isset( $_GET['token'] ) ? sanitize_text_field( wp_unslash( $_GET['token'] ) ) : '';

		if ( empty( $token ) ) {
			wp_send_json_error( array( 'message' => __( 'Missing token.', 'onetill' ) ), 400 );
		}

		$status = $this->get_token_status( $token );
		wp_send_json_success( $status );
	}

	/**
	 * REST handler: Get pairing status.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response
	 */
	public function get_pairing_status( $request ) {
		$token = sanitize_text_field( $request->get_param( 'token' ) );

		if ( empty( $token ) ) {
			return new \WP_REST_Response( array(
				'success' => false,
				'error'   => 'validation_error',
				'message' => 'Missing token parameter.',
			), 400 );
		}

		$status = $this->get_token_status( $token );

		return new \WP_REST_Response( $status, 200 );
	}

	/**
	 * REST handler: Complete pairing.
	 *
	 * Called by the S700 after scanning the QR code. Uses token auth.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response
	 */
	public function complete_pairing( $request ) {
		$body = $request->get_json_params();

		$token       = isset( $body['token'] ) ? sanitize_text_field( $body['token'] ) : '';
		$nonce       = isset( $body['nonce'] ) ? sanitize_text_field( $body['nonce'] ) : '';
		$device_name = isset( $body['device_name'] ) ? sanitize_text_field( $body['device_name'] ) : '';
		$device_id   = isset( $body['device_id'] ) ? sanitize_text_field( $body['device_id'] ) : '';
		$app_version = isset( $body['app_version'] ) ? sanitize_text_field( $body['app_version'] ) : '';

		if ( empty( $token ) || empty( $nonce ) || empty( $device_name ) || empty( $device_id ) ) {
			return new \WP_REST_Response( array(
				'success' => false,
				'error'   => 'validation_error',
				'message' => 'Missing required fields: token, nonce, device_name, device_id.',
			), 400 );
		}

		if ( ! $this->check_rate_limit() ) {
			return new \WP_REST_Response( array(
				'success' => false,
				'error'   => 'rate_limited',
				'message' => 'Too many pairing attempts. Please try again later.',
			), 429 );
		}

		global $wpdb;

		$row = $wpdb->get_row(
			$wpdb->prepare(
				"SELECT * FROM {$wpdb->prefix}onetill_pairing_tokens WHERE token = %s",
				$token
			),
			ARRAY_A
		);

		if ( ! $row ) {
			return new \WP_REST_Response( array(
				'success' => false,
				'error'   => 'token_invalid',
				'message' => 'Pairing token is invalid.',
			), 401 );
		}

		if ( 'used' === $row['status'] ) {
			return new \WP_REST_Response( array(
				'success' => false,
				'error'   => 'token_already_used',
				'message' => 'Pairing token has already been used.',
			), 409 );
		}

		if ( strtotime( $row['expires_at'] ) < time() ) {
			$wpdb->update(
				$wpdb->prefix . 'onetill_pairing_tokens',
				array( 'status' => 'expired' ),
				array( 'token' => $token ),
				array( '%s' ),
				array( '%s' )
			);

			return new \WP_REST_Response( array(
				'success' => false,
				'error'   => 'token_expired',
				'message' => 'Pairing token has expired. Please generate a new QR code.',
			), 401 );
		}

		if ( $row['nonce'] !== $nonce ) {
			return new \WP_REST_Response( array(
				'success' => false,
				'error'   => 'nonce_mismatch',
				'message' => 'Nonce does not match.',
			), 401 );
		}

		// Generate WooCommerce API credentials.
		$credentials = $this->generate_api_credentials( $device_name );

		if ( empty( $credentials ) ) {
			return new \WP_REST_Response( array(
				'success' => false,
				'error'   => 'server_error',
				'message' => 'Failed to generate API credentials.',
			), 500 );
		}

		$now = current_time( 'mysql', true );

		// Create device record.
		$wpdb->insert(
			$wpdb->prefix . 'onetill_devices',
			array(
				'id'          => $device_id,
				'name'        => $device_name,
				'api_key_id'  => $credentials['key_id'],
				'app_version' => $app_version,
				'paired_at'   => $now,
				'status'      => 'active',
			),
			array( '%s', '%s', '%d', '%s', '%s', '%s' )
		);

		// Mark token as used.
		$wpdb->update(
			$wpdb->prefix . 'onetill_pairing_tokens',
			array(
				'status'    => 'used',
				'device_id' => $device_id,
			),
			array( 'token' => $token ),
			array( '%s', '%s' ),
			array( '%s' )
		);

		// Build store settings.
		$store = $this->get_store_settings();

		return new \WP_REST_Response( array(
			'success'        => true,
			'credentials'    => array(
				'consumer_key'    => $credentials['consumer_key'],
				'consumer_secret' => $credentials['consumer_secret'],
			),
			'store'          => $store,
			'plugin_version' => ONETILL_VERSION,
			'api_base'       => '/wp-json/' . self::NAMESPACE,
		), 200 );
	}

	/**
	 * Generate WooCommerce REST API credentials for a paired device.
	 *
	 * @param string $device_name Human-readable device name.
	 * @return array|null { consumer_key, consumer_secret, key_id } or null on failure.
	 */
	private function generate_api_credentials( $device_name ) {
		global $wpdb;

		$consumer_key    = 'ck_' . wc_rand_hash();
		$consumer_secret = 'cs_' . wc_rand_hash();

		// Find the first admin user for the key ownership.
		$admin_users = get_users( array(
			'role'   => 'administrator',
			'number' => 1,
			'fields' => 'ID',
		) );

		$user_id = ! empty( $admin_users ) ? (int) $admin_users[0] : get_current_user_id();

		// Use user_id 0 as fallback if called from REST context with no auth.
		if ( ! $user_id ) {
			$user_id = 1;
		}

		$result = $wpdb->insert(
			$wpdb->prefix . 'woocommerce_api_keys',
			array(
				'user_id'         => $user_id,
				'description'     => 'OneTill POS — ' . $device_name,
				'permissions'     => 'read_write',
				'consumer_key'    => wc_api_hash( $consumer_key ),
				'consumer_secret' => $consumer_secret,
				'truncated_key'   => substr( $consumer_key, -7 ),
			),
			array( '%d', '%s', '%s', '%s', '%s', '%s' )
		);

		if ( ! $result ) {
			return null;
		}

		return array(
			'consumer_key'    => $consumer_key,
			'consumer_secret' => $consumer_secret,
			'key_id'          => (int) $wpdb->insert_id,
		);
	}

	/**
	 * Build the QR code payload URL.
	 *
	 * @param string $token The pairing token.
	 * @param string $nonce The nonce.
	 * @return string The onetill://pair?d=<encoded> URL.
	 */
	private function build_qr_payload( $token, $nonce ) {
		$payload = wp_json_encode( array(
			'v'              => 1,
			'store_url'      => get_site_url(),
			'token'          => $token,
			'nonce'          => $nonce,
			'plugin_version' => ONETILL_VERSION,
		) );

		// Base64url encoding (no padding, URL-safe).
		$encoded = rtrim( strtr( base64_encode( $payload ), '+/', '-_' ), '=' );

		return 'onetill://pair?d=' . $encoded;
	}

	/**
	 * Get the current status of a pairing token.
	 *
	 * @param string $token The token to check.
	 * @return array Status data.
	 */
	private function get_token_status( $token ) {
		global $wpdb;

		$row = $wpdb->get_row(
			$wpdb->prepare(
				"SELECT * FROM {$wpdb->prefix}onetill_pairing_tokens WHERE token = %s",
				$token
			),
			ARRAY_A
		);

		if ( ! $row ) {
			return array( 'status' => 'expired' );
		}

		if ( 'used' === $row['status'] ) {
			$device = $wpdb->get_row(
				$wpdb->prepare(
					"SELECT id, name, paired_at FROM {$wpdb->prefix}onetill_devices WHERE id = %s",
					$row['device_id']
				),
				ARRAY_A
			);

			return array(
				'status' => 'complete',
				'device' => array(
					'id'        => $device ? $device['id'] : $row['device_id'],
					'name'      => $device ? $device['name'] : '',
					'paired_at' => $device ? gmdate( 'c', strtotime( $device['paired_at'] ) ) : '',
				),
			);
		}

		if ( 'expired' === $row['status'] || strtotime( $row['expires_at'] ) < time() ) {
			return array( 'status' => 'expired' );
		}

		return array(
			'status'     => 'pending',
			'expires_in' => max( 0, strtotime( $row['expires_at'] ) - time() ),
		);
	}

	/**
	 * Get store settings for the pairing response.
	 *
	 * @return array
	 */
	private function get_store_settings() {
		return array(
			'name'               => get_bloginfo( 'name' ),
			'url'                => get_site_url(),
			'currency'           => get_woocommerce_currency(),
			'currency_position'  => get_option( 'woocommerce_currency_pos', 'left' ),
			'thousand_separator' => get_option( 'woocommerce_price_thousand_sep', ',' ),
			'decimal_separator'  => get_option( 'woocommerce_price_decimal_sep', '.' ),
			'num_decimals'       => (int) get_option( 'woocommerce_price_num_decimals', 2 ),
			'tax_enabled'        => wc_tax_enabled(),
			'prices_include_tax' => 'yes' === get_option( 'woocommerce_prices_include_tax', 'no' ),
			'timezone'           => wp_timezone_string(),
		);
	}

	/**
	 * Check rate limit for pairing attempts.
	 *
	 * @return bool True if within limit, false if rate limited.
	 */
	private function check_rate_limit() {
		$ip  = $this->get_client_ip();
		$key = 'onetill_pair_rate_' . md5( $ip );

		$count = (int) get_transient( $key );

		if ( $count >= self::RATE_LIMIT ) {
			return false;
		}

		set_transient( $key, $count + 1, HOUR_IN_SECONDS );

		return true;
	}

	/**
	 * Get the client IP address.
	 *
	 * @return string
	 */
	private function get_client_ip() {
		if ( ! empty( $_SERVER['HTTP_X_FORWARDED_FOR'] ) ) {
			$ips = explode( ',', sanitize_text_field( wp_unslash( $_SERVER['HTTP_X_FORWARDED_FOR'] ) ) );
			return trim( $ips[0] );
		}

		return isset( $_SERVER['REMOTE_ADDR'] ) ? sanitize_text_field( wp_unslash( $_SERVER['REMOTE_ADDR'] ) ) : '127.0.0.1';
	}

	/**
	 * Clean up expired pairing tokens.
	 *
	 * Called by WP-Cron hourly.
	 */
	public function cleanup_expired_tokens() {
		global $wpdb;

		$wpdb->query(
			$wpdb->prepare(
				"UPDATE {$wpdb->prefix}onetill_pairing_tokens SET status = 'expired' WHERE status = 'pending' AND expires_at < %s",
				current_time( 'mysql', true )
			)
		);

		// Delete tokens older than 24 hours.
		$wpdb->query(
			$wpdb->prepare(
				"DELETE FROM {$wpdb->prefix}onetill_pairing_tokens WHERE created_at < %s",
				gmdate( 'Y-m-d H:i:s', time() - DAY_IN_SECONDS )
			)
		);
	}
}
