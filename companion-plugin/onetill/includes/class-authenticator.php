<?php
/**
 * WooCommerce API key authenticator for OneTill REST endpoints.
 *
 * WooCommerce's WC_REST_Authentication only processes API key credentials
 * for requests to the /wc/ namespace. Our /onetill/v1/ endpoints need to
 * authenticate the same consumer_key/consumer_secret credentials manually.
 *
 * This class extracts the credentials, validates them against the
 * woocommerce_api_keys table, and sets the current WordPress user so
 * that subsequent current_user_can() checks work correctly.
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
	 * Authenticate a REST request using WooCommerce API keys.
	 *
	 * Extracts consumer_key/consumer_secret from the request, validates
	 * them against the woocommerce_api_keys table, sets the current user,
	 * and checks that the user has the manage_woocommerce capability.
	 *
	 * @param \WP_REST_Request $request The REST request.
	 * @return bool|\WP_Error True if authenticated, WP_Error otherwise.
	 */
	public static function check( $request ) {
		// If a user is already authenticated (e.g. cookie auth in wp-admin), check capability directly.
		if ( get_current_user_id() ) {
			if ( current_user_can( 'manage_woocommerce' ) ) {
				return true;
			}
			return self::forbidden();
		}

		// Extract consumer key and secret from the request.
		$credentials = self::extract_credentials( $request );
		if ( ! $credentials ) {
			return new \WP_Error(
				'onetill_rest_unauthorized',
				__( 'Missing or invalid API credentials.', 'onetill' ),
				array( 'status' => 401 )
			);
		}

		// Look up the key in the WooCommerce API keys table.
		global $wpdb;

		$key_row = $wpdb->get_row(
			$wpdb->prepare(
				"SELECT key_id, user_id, permissions, consumer_secret FROM {$wpdb->prefix}woocommerce_api_keys WHERE consumer_key = %s",
				wc_api_hash( $credentials['consumer_key'] )
			),
			ARRAY_A
		);

		if ( ! $key_row ) {
			return new \WP_Error(
				'onetill_rest_unauthorized',
				__( 'Invalid API credentials.', 'onetill' ),
				array( 'status' => 401 )
			);
		}

		// Verify the consumer secret.
		if ( ! hash_equals( $key_row['consumer_secret'], $credentials['consumer_secret'] ) ) {
			return new \WP_Error(
				'onetill_rest_unauthorized',
				__( 'Invalid API credentials.', 'onetill' ),
				array( 'status' => 401 )
			);
		}

		// Check the key has read_write or read permissions.
		if ( 'read' !== $key_row['permissions'] && 'read_write' !== $key_row['permissions'] ) {
			return self::forbidden();
		}

		// Set the current user so capability checks work.
		wp_set_current_user( $key_row['user_id'] );

		// Verify the user has WooCommerce management capability.
		if ( ! current_user_can( 'manage_woocommerce' ) ) {
			return self::forbidden();
		}

		// Update last_access timestamp on the API key.
		$wpdb->update(
			$wpdb->prefix . 'woocommerce_api_keys',
			array( 'last_access' => current_time( 'mysql', true ) ),
			array( 'key_id' => $key_row['key_id'] ),
			array( '%s' ),
			array( '%d' )
		);

		return true;
	}

	/**
	 * Extract consumer key and secret from the request.
	 *
	 * Supports HTTP Basic Auth header, query parameters, and PHP_AUTH_USER.
	 *
	 * @param \WP_REST_Request $request The REST request.
	 * @return array|null Array with consumer_key and consumer_secret, or null.
	 */
	private static function extract_credentials( $request ) {
		// 1. Check Authorization header (HTTP Basic Auth).
		$auth_header = $request->get_header( 'authorization' );
		if ( $auth_header && 0 === strpos( $auth_header, 'Basic ' ) ) {
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
		$params = $request->get_query_params();
		if ( ! empty( $params['consumer_key'] ) && ! empty( $params['consumer_secret'] ) ) {
			return array(
				'consumer_key'    => sanitize_text_field( $params['consumer_key'] ),
				'consumer_secret' => sanitize_text_field( $params['consumer_secret'] ),
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

	/**
	 * Return a forbidden error.
	 *
	 * @return \WP_Error
	 */
	private static function forbidden() {
		return new \WP_Error(
			'onetill_rest_forbidden',
			__( 'Sorry, you are not allowed to access this resource.', 'onetill' ),
			array( 'status' => 403 )
		);
	}
}
