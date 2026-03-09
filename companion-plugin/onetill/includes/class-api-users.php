<?php
/**
 * Users REST API endpoint.
 *
 * Handles:
 * - GET  /onetill/v1/users      — List all staff users (with PIN hashes for offline verification)
 * - POST /onetill/v1/users/verify-pin — Verify a PIN against stored hashes
 *
 * @package OneTill
 */

namespace OneTill;

defined( 'ABSPATH' ) || exit;

/**
 * Class API_Users
 */
class API_Users {

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
			'/users',
			array(
				'methods'             => 'GET',
				'callback'            => array( $this, 'get_users' ),
				'permission_callback' => array( $this, 'check_permissions' ),
			)
		);

		register_rest_route(
			self::NAMESPACE,
			'/users/verify-pin',
			array(
				'methods'             => 'POST',
				'callback'            => array( $this, 'verify_pin' ),
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
	 * Get all staff users.
	 *
	 * Returns users with SHA-256 PIN hashes for offline verification on the device.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response
	 */
	public function get_users( $request ) {
		global $wpdb;

		$rows = $wpdb->get_results(
			"SELECT id, first_name, last_name, pin_sha256 FROM {$wpdb->prefix}onetill_users ORDER BY first_name ASC",
			ARRAY_A
		);

		$users = array();
		foreach ( $rows as $row ) {
			$users[] = array(
				'id'         => (int) $row['id'],
				'first_name' => $row['first_name'],
				'last_name'  => $row['last_name'],
				'pin_sha256' => $row['pin_sha256'] ?: null,
			);
		}

		return new \WP_REST_Response( $users, 200 );
	}

	/**
	 * Verify a PIN against stored hashes.
	 *
	 * Fallback for users without pin_sha256 (created before the column was added).
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response|\WP_Error
	 */
	public function verify_pin( $request ) {
		$pin = $request->get_param( 'pin' );

		if ( empty( $pin ) || ! preg_match( '/^\d{4}$/', $pin ) ) {
			return new \WP_Error(
				'invalid_pin',
				__( 'PIN must be exactly 4 digits.', 'onetill' ),
				array( 'status' => 400 )
			);
		}

		global $wpdb;

		$rows = $wpdb->get_results(
			"SELECT id, first_name, last_name, pin FROM {$wpdb->prefix}onetill_users",
			ARRAY_A
		);

		foreach ( $rows as $row ) {
			if ( wp_check_password( $pin, $row['pin'] ) ) {
				return new \WP_REST_Response(
					array(
						'valid' => true,
						'user'  => array(
							'id'         => (int) $row['id'],
							'first_name' => $row['first_name'],
							'last_name'  => $row['last_name'],
						),
					),
					200
				);
			}
		}

		return new \WP_REST_Response(
			array( 'valid' => false ),
			200
		);
	}
}
