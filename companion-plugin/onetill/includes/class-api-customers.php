<?php
/**
 * Customer REST API endpoints.
 *
 * Handles:
 * - GET  /onetill/v1/customers/search — Search by name, email, or phone
 * - POST /onetill/v1/customers        — Create new customer
 *
 * @package OneTill
 */

namespace OneTill;

defined( 'ABSPATH' ) || exit;

/**
 * Class API_Customers
 */
class API_Customers {

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
			'/customers/search',
			array(
				'methods'             => 'GET',
				'callback'            => array( $this, 'search_customers' ),
				'permission_callback' => array( $this, 'check_permissions' ),
				'args'                => array(
					'q'        => array(
						'required'          => true,
						'sanitize_callback' => 'sanitize_text_field',
					),
					'per_page' => array(
						'default'           => 10,
						'sanitize_callback' => 'absint',
					),
				),
			)
		);

		register_rest_route(
			self::NAMESPACE,
			'/customers',
			array(
				'methods'             => 'POST',
				'callback'            => array( $this, 'create_customer' ),
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
	 * Search customers by name, email, or phone.
	 *
	 * Minimum 2 characters required for the search query.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response|\WP_Error
	 */
	public function search_customers( $request ) {
		// TODO: Implement customer search.
	}

	/**
	 * Create a new customer at checkout.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response|\WP_Error
	 */
	public function create_customer( $request ) {
		// TODO: Implement customer creation.
	}

	/**
	 * Format a WP_User/WC_Customer into the POS customer payload.
	 *
	 * @param \WC_Customer $customer The customer.
	 * @return array
	 */
	private function format_customer( $customer ) {
		// TODO: Implement customer formatting.
		return array();
	}
}
