<?php
/**
 * WooCommerce API key authenticator for OneTill REST endpoints.
 *
 * WooCommerce's WC_REST_Authentication only processes API key credentials
 * for requests to the /wc/ namespace. Our /onetill/v1/ endpoints need to
 * authenticate the same consumer_key/consumer_secret credentials manually.
 *
 * Authentication happens in two stages:
 * 1. The `determine_current_user` filter extracts and validates credentials,
 *    returning the associated WordPress user ID so WordPress sets the user.
 * 2. The `check()` permission callback verifies the user has the required
 *    capability (manage_woocommerce).
 *
 * @package OneTill
 */

namespace OneTill;

defined( 'ABSPATH' ) || exit;

/**
 * Class Authenticator
 */
class Authenticator {

	/**
	 * Authentication error from the determine_current_user filter.
	 *
	 * @var \WP_Error|null
	 */
	private static $auth_error = null;

	/**
	 * Permission level of the authenticated API key ('read' or 'read_write').
	 *
	 * @var string|null
	 */
	private static $api_key_permissions = null;

	/**
	 * Filter: Determine the current user for OneTill REST requests.
	 *
	 * Hooked to `determine_current_user` at priority 20. Validates WooCommerce
	 * API key credentials and returns the associated user ID.
	 *
	 * @param int $user_id The current user ID (0 if not yet determined).
	 * @return int The authenticated user ID, or the original value.
	 */
	public static function determine_current_user( $user_id ) {
		// If a user is already determined (e.g. cookie auth), don't override.
		if ( $user_id ) {
			return $user_id;
		}

		// Only process requests to our REST namespace.
		if ( ! self::is_onetill_rest_request() ) {
			return $user_id;
		}

		// Extract consumer key and secret from the request.
		$credentials = self::extract_credentials_from_globals();
		if ( ! $credentials ) {
			self::$auth_error = new \WP_Error(
				'onetill_rest_unauthorized',
				__( 'Missing or invalid API credentials.', 'onetill' ),
				array( 'status' => 401 )
			);
			return $user_id;
		}

		// Look up the key in the WooCommerce API keys table.
		global $wpdb;

		// phpcs:ignore WordPress.DB.DirectDatabaseQuery.DirectQuery, WordPress.DB.DirectDatabaseQuery.NoCaching -- auth lookup, not cacheable.
		$key_row = $wpdb->get_row(
			$wpdb->prepare(
				"SELECT key_id, user_id, permissions, consumer_secret FROM {$wpdb->prefix}woocommerce_api_keys WHERE consumer_key = %s",
				wc_api_hash( $credentials['consumer_key'] )
			),
			ARRAY_A
		);

		if ( ! $key_row ) {
			self::$auth_error = new \WP_Error(
				'onetill_rest_unauthorized',
				__( 'Invalid API credentials.', 'onetill' ),
				array( 'status' => 401 )
			);
			return $user_id;
		}

		// Verify the consumer secret.
		if ( ! hash_equals( $key_row['consumer_secret'], $credentials['consumer_secret'] ) ) {
			self::$auth_error = new \WP_Error(
				'onetill_rest_unauthorized',
				__( 'Invalid API credentials.', 'onetill' ),
				array( 'status' => 401 )
			);
			return $user_id;
		}

		// Check the key has read_write or read permissions.
		if ( 'read' !== $key_row['permissions'] && 'read_write' !== $key_row['permissions'] ) {
			self::$auth_error = new \WP_Error(
				'onetill_rest_forbidden',
				__( 'Sorry, you are not allowed to access this resource.', 'onetill' ),
				array( 'status' => 403 )
			);
			return $user_id;
		}

		// Store the permission level for use in check_write().
		self::$api_key_permissions = $key_row['permissions'];

		// Update last_access timestamp on the API key.
		// phpcs:ignore WordPress.DB.DirectDatabaseQuery.DirectQuery, WordPress.DB.DirectDatabaseQuery.NoCaching -- auth timestamp update.
		$wpdb->update(
			$wpdb->prefix . 'woocommerce_api_keys',
			array( 'last_access' => current_time( 'mysql', true ) ),
			array( 'key_id' => $key_row['key_id'] ),
			array( '%s' ),
			array( '%d' )
		);

		// Return the user ID — WordPress will set the current user.
		return (int) $key_row['user_id'];
	}

	/**
	 * Permission callback for authenticated OneTill REST endpoints.
	 *
	 * Called after determine_current_user has already set the user.
	 * Checks that the authenticated user has the manage_woocommerce capability.
	 *
	 * @param \WP_REST_Request $request The REST request.
	 * @return bool|\WP_Error True if authenticated, WP_Error otherwise.
	 */
	public static function check( $request ) {
		// If the determine_current_user filter stored an error, return it.
		if ( self::$auth_error ) {
			return self::$auth_error;
		}

		// Check the current user has the required capability.
		if ( current_user_can( 'manage_woocommerce' ) ) {
			return true;
		}

		return new \WP_Error(
			'onetill_rest_forbidden',
			__( 'Sorry, you are not allowed to access this resource.', 'onetill' ),
			array( 'status' => 403 )
		);
	}

	/**
	 * Permission callback for write endpoints that require read_write API keys.
	 *
	 * @param \WP_REST_Request $request The REST request.
	 * @return bool|\WP_Error True if authenticated with write access, WP_Error otherwise.
	 */
	public static function check_write( $request ) {
		$read_check = self::check( $request );
		if ( true !== $read_check ) {
			return $read_check;
		}

		if ( 'read_write' !== self::$api_key_permissions ) {
			return new \WP_Error(
				'onetill_rest_forbidden',
				__( 'This endpoint requires read_write API key permissions.', 'onetill' ),
				array( 'status' => 403 )
			);
		}

		return true;
	}

	/**
	 * Check if the current request targets the OneTill REST namespace.
	 *
	 * @return bool
	 */
	private static function is_onetill_rest_request() {
		if ( empty( $_SERVER['REQUEST_URI'] ) ) {
			return false;
		}

		$request_uri = sanitize_text_field( wp_unslash( $_SERVER['REQUEST_URI'] ) );
		$rest_prefix = trailingslashit( rest_get_url_prefix() );

		return false !== strpos( $request_uri, $rest_prefix . 'onetill/v1/' );
	}

	/**
	 * Extract consumer key and secret from global request data.
	 *
	 * Supports HTTP Basic Auth header, query parameters, and PHP_AUTH_USER.
	 * Used by the determine_current_user filter (before WP_REST_Request exists).
	 *
	 * @return array|null Array with consumer_key and consumer_secret, or null.
	 */
	private static function extract_credentials_from_globals() {
		// 1. Check Authorization header (HTTP Basic Auth).
		$auth_header = isset( $_SERVER['HTTP_AUTHORIZATION'] ) ? sanitize_text_field( wp_unslash( $_SERVER['HTTP_AUTHORIZATION'] ) ) : '';
		if ( $auth_header && 0 === strpos( $auth_header, 'Basic ' ) ) {
			// phpcs:ignore WordPress.PHP.DiscouragedPHPFunctions.obfuscation_base64_decode -- decoding HTTP Basic Auth header.
			$decoded = base64_decode( substr( $auth_header, 6 ) );
			if ( $decoded && strpos( $decoded, ':' ) !== false ) {
				list( $key, $secret ) = explode( ':', $decoded, 2 );
				return array(
					'consumer_key'    => sanitize_text_field( $key ),
					'consumer_secret' => sanitize_text_field( $secret ),
				);
			}
		}

		// 2. Check query parameters.
		// phpcs:ignore WordPress.Security.NonceVerification.Recommended -- API key auth, not form submission.
		if ( ! empty( $_GET['consumer_key'] ) && ! empty( $_GET['consumer_secret'] ) ) {
			return array(
				// phpcs:ignore WordPress.Security.NonceVerification.Recommended -- API key auth, not form submission.
				'consumer_key'    => sanitize_text_field( wp_unslash( $_GET['consumer_key'] ) ),
				// phpcs:ignore WordPress.Security.NonceVerification.Recommended -- API key auth, not form submission.
				'consumer_secret' => sanitize_text_field( wp_unslash( $_GET['consumer_secret'] ) ),
			);
		}

		// 3. Check PHP_AUTH_USER / PHP_AUTH_PW (set by Apache/nginx for Basic auth).
		if ( ! empty( $_SERVER['PHP_AUTH_USER'] ) && ! empty( $_SERVER['PHP_AUTH_PW'] ) ) {
			return array(
				'consumer_key'    => sanitize_text_field( wp_unslash( $_SERVER['PHP_AUTH_USER'] ) ),
				'consumer_secret' => sanitize_text_field( wp_unslash( $_SERVER['PHP_AUTH_PW'] ) ),
			);
		}

		return null;
	}
}
