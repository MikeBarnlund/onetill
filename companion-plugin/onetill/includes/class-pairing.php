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
	 *
	 * Only the /pair/complete endpoint is a REST route (called by the S700).
	 * The initiate and status endpoints are AJAX handlers (called by WP Admin).
	 */
	public function register_routes() {
		register_rest_route(
			self::NAMESPACE,
			'/pair/complete',
			array(
				'methods'             => 'POST',
				'callback'            => array( $this, 'complete_pairing' ),
				'permission_callback' => '__return_true', // Token auth, not WC API keys.
			)
		);

		register_rest_route(
			self::NAMESPACE,
			'/pair/status',
			array(
				'methods'             => 'GET',
				'callback'            => array( $this, 'get_pairing_status' ),
				'permission_callback' => '__return_true', // Nonce-verified in callback.
			)
		);
	}

	/**
	 * AJAX handler: Initiate pairing.
	 *
	 * Called by WP Admin when merchant clicks "Pair New Device".
	 * Generates a pairing token, stores it, renders QR code as SVG.
	 *
	 * Authentication: WordPress admin session (nonce-verified).
	 */
	public function ajax_initiate_pairing() {
		// TODO: Implement:
		// 1. Verify nonce
		// 2. Check rate limit (5 per IP per hour via transients)
		// 3. Generate token: bin2hex(random_bytes(32))
		// 4. Generate nonce: bin2hex(random_bytes(4))
		// 5. Store in wp_onetill_pairing_tokens (pending, 5-min expiry)
		// 6. Build QR payload JSON, base64url-encode
		// 7. Construct onetill://pair?d=... URL
		// 8. Render QR as SVG via QR_Generator
		// 9. Return JSON: success, qr_svg, token, expires_at, poll_url
	}

	/**
	 * AJAX handler: Check pairing status.
	 *
	 * Polled by WP Admin every 2 seconds to detect when S700 completes pairing.
	 *
	 * Authentication: WordPress admin session (nonce in query param).
	 */
	public function ajax_check_pairing_status() {
		// TODO: Implement:
		// 1. Verify nonce
		// 2. Look up token in DB
		// 3. Return status: pending (with expires_in), complete (with device info), or expired
	}

	/**
	 * REST handler: Complete pairing.
	 *
	 * Called by the S700 after scanning the QR code. This is the only
	 * endpoint that uses token auth instead of WooCommerce API keys.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response|\WP_Error
	 */
	public function complete_pairing( $request ) {
		// TODO: Implement:
		// 1. Validate token exists and status is 'pending'
		// 2. Check expires_at > now()
		// 3. Verify nonce matches
		// 4. Generate WooCommerce API credentials programmatically
		// 5. Create device record in wp_onetill_devices
		// 6. Update token status to 'used'
		// 7. Return credentials + store settings
	}

	/**
	 * REST handler: Get pairing status.
	 *
	 * Also accessible as REST endpoint for the admin UI polling.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response|\WP_Error
	 */
	public function get_pairing_status( $request ) {
		// TODO: Implement status check.
	}

	/**
	 * Generate WooCommerce REST API credentials for a paired device.
	 *
	 * Creates a consumer_key/consumer_secret pair in the woocommerce_api_keys table.
	 * The consumer_secret is returned in plaintext this one time only — it's stored
	 * hashed in the DB and cannot be retrieved again.
	 *
	 * @param string $device_name Human-readable device name.
	 * @return array { consumer_key, consumer_secret, key_id }
	 */
	private function generate_api_credentials( $device_name ) {
		// TODO: Implement credential generation using wc_rand_hash().
		return array();
	}

	/**
	 * Build the QR code payload.
	 *
	 * @param string $token The pairing token.
	 * @param string $nonce The nonce.
	 * @return string The onetill://pair?d=... URL.
	 */
	private function build_qr_payload( $token, $nonce ) {
		// TODO: Implement:
		// 1. Build JSON: { v: 1, store_url, token, nonce, plugin_version }
		// 2. Base64url-encode
		// 3. Return onetill://pair?d=<encoded>
		return '';
	}

	/**
	 * Check rate limit for pairing attempts.
	 *
	 * Max 5 attempts per IP per hour using WordPress transients.
	 *
	 * @return bool True if within limit, false if rate limited.
	 */
	private function check_rate_limit() {
		// TODO: Implement rate limiting via transients.
		return true;
	}

	/**
	 * Clean up expired pairing tokens.
	 *
	 * Called by WP-Cron hourly.
	 */
	public function cleanup_expired_tokens() {
		// TODO: Update status to 'expired' for pending tokens past expires_at.
	}
}
