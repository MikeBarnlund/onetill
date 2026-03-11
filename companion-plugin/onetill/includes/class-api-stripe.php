<?php
/**
 * Stripe REST API endpoints.
 *
 * Handles:
 * - POST /onetill/v1/stripe/connection-token — Generate Terminal connection token
 *
 * @package OneTill
 */

namespace OneTill;

defined( 'ABSPATH' ) || exit;

/**
 * Class API_Stripe
 */
class API_Stripe {

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
			'/stripe/connection-token',
			array(
				'methods'             => 'POST',
				'callback'            => array( $this, 'create_connection_token' ),
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
	 * Create a Stripe Terminal connection token.
	 *
	 * Calls the Stripe API using the stored secret key. This is NOT
	 * PCI-scoped — connection tokens are auth tokens for the Terminal SDK,
	 * not card data.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response|\WP_Error
	 */
	public function create_connection_token( $request ) {
		$secret_key = get_option( 'onetill_stripe_secret_key', '' );

		if ( empty( $secret_key ) ) {
			return new \WP_Error(
				'stripe_not_configured',
				'Stripe secret key is not configured. Add it in WP Admin > OneTill settings.',
				array( 'status' => 400 )
			);
		}

		$response = wp_remote_post(
			'https://api.stripe.com/v1/terminal/connection_tokens',
			array(
				'headers' => array(
					'Authorization' => 'Bearer ' . $secret_key,
					'Content-Type'  => 'application/x-www-form-urlencoded',
				),
				'timeout' => 15,
			)
		);

		if ( is_wp_error( $response ) ) {
			return new \WP_Error(
				'stripe_api_error',
				'Failed to connect to Stripe: ' . $response->get_error_message(),
				array( 'status' => 502 )
			);
		}

		$status_code = wp_remote_retrieve_response_code( $response );
		$body        = json_decode( wp_remote_retrieve_body( $response ), true );

		if ( 200 !== $status_code ) {
			$error_message = isset( $body['error']['message'] ) ? $body['error']['message'] : 'Unknown Stripe error';
			return new \WP_Error(
				'stripe_api_error',
				$error_message,
				array( 'status' => $status_code )
			);
		}

		if ( empty( $body['secret'] ) ) {
			return new \WP_Error(
				'stripe_invalid_response',
				'Stripe returned an invalid connection token response.',
				array( 'status' => 502 )
			);
		}

		return new \WP_REST_Response(
			array( 'secret' => $body['secret'] ),
			200
		);
	}
}
