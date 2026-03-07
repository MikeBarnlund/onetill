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
	 * Search customers by name, email, or phone.
	 *
	 * Minimum 2 characters required for the search query.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response|\WP_Error
	 */
	public function search_customers( $request ) {
		$query    = $request->get_param( 'q' );
		$per_page = min( $request->get_param( 'per_page' ) ?: 10, 50 );

		if ( strlen( $query ) < 2 ) {
			return new \WP_Error(
				'query_too_short',
				'Search query must be at least 2 characters.',
				array( 'status' => 400 )
			);
		}

		// Search by email (exact-ish match) first for speed.
		$customers = array();
		$seen_ids  = array();

		// Email search.
		if ( is_email( $query ) || strpos( $query, '@' ) !== false ) {
			$email_results = new \WP_User_Query( array(
				'search'         => '*' . $query . '*',
				'search_columns' => array( 'user_email' ),
				'role'           => 'customer',
				'number'         => $per_page,
				'fields'         => 'ID',
			) );

			foreach ( $email_results->get_results() as $user_id ) {
				$seen_ids[ $user_id ] = true;
				$customer = new \WC_Customer( $user_id );
				$customers[] = $this->format_customer( $customer );
			}
		}

		// Name and general search (if we still need more results).
		if ( count( $customers ) < $per_page ) {
			$remaining = $per_page - count( $customers );

			$name_results = new \WP_User_Query( array(
				'search'         => '*' . $query . '*',
				'search_columns' => array( 'user_login', 'user_email', 'user_nicename', 'display_name' ),
				'role'           => 'customer',
				'number'         => $remaining + count( $seen_ids ),
				'fields'         => 'ID',
			) );

			foreach ( $name_results->get_results() as $user_id ) {
				if ( isset( $seen_ids[ $user_id ] ) ) {
					continue;
				}
				$seen_ids[ $user_id ] = true;
				$customer = new \WC_Customer( $user_id );
				$customers[] = $this->format_customer( $customer );

				if ( count( $customers ) >= $per_page ) {
					break;
				}
			}
		}

		// Also search by meta (first_name, last_name, phone) if still need results.
		if ( count( $customers ) < $per_page ) {
			$remaining  = $per_page - count( $customers );
			$meta_query = array(
				'relation' => 'OR',
				array(
					'key'     => 'first_name',
					'value'   => $query,
					'compare' => 'LIKE',
				),
				array(
					'key'     => 'last_name',
					'value'   => $query,
					'compare' => 'LIKE',
				),
				array(
					'key'     => 'billing_phone',
					'value'   => $query,
					'compare' => 'LIKE',
				),
				array(
					'key'     => 'billing_first_name',
					'value'   => $query,
					'compare' => 'LIKE',
				),
				array(
					'key'     => 'billing_last_name',
					'value'   => $query,
					'compare' => 'LIKE',
				),
			);

			$meta_results = new \WP_User_Query( array(
				'role'       => 'customer',
				'number'     => $remaining + count( $seen_ids ),
				'fields'     => 'ID',
				'meta_query' => $meta_query,
			) );

			foreach ( $meta_results->get_results() as $user_id ) {
				if ( isset( $seen_ids[ $user_id ] ) ) {
					continue;
				}
				$seen_ids[ $user_id ] = true;
				$customer = new \WC_Customer( $user_id );
				$customers[] = $this->format_customer( $customer );

				if ( count( $customers ) >= $per_page ) {
					break;
				}
			}
		}

		return new \WP_REST_Response( array(
			'customers' => $customers,
		), 200 );
	}

	/**
	 * Create a new customer at checkout.
	 *
	 * If the email already exists, returns the existing customer
	 * instead of erroring.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response|\WP_Error
	 */
	public function create_customer( $request ) {
		$body = $request->get_json_params();

		$email      = isset( $body['email'] ) ? sanitize_email( $body['email'] ) : '';
		$first_name = isset( $body['first_name'] ) ? sanitize_text_field( $body['first_name'] ) : '';
		$last_name  = isset( $body['last_name'] ) ? sanitize_text_field( $body['last_name'] ) : '';
		$phone      = isset( $body['phone'] ) ? sanitize_text_field( $body['phone'] ) : '';

		if ( empty( $email ) || ! is_email( $email ) ) {
			return new \WP_Error(
				'invalid_email',
				'A valid email address is required.',
				array( 'status' => 400 )
			);
		}

		// Check if customer already exists with this email.
		$existing_user = get_user_by( 'email', $email );
		if ( $existing_user ) {
			$customer = new \WC_Customer( $existing_user->ID );
			return new \WP_REST_Response( array(
				'success'  => true,
				'existing' => true,
				'customer' => $this->format_customer( $customer ),
			), 200 );
		}

		// Create new WooCommerce customer.
		$customer = new \WC_Customer();
		$customer->set_email( $email );
		$customer->set_first_name( $first_name );
		$customer->set_last_name( $last_name );
		$customer->set_billing_email( $email );
		$customer->set_billing_first_name( $first_name );
		$customer->set_billing_last_name( $last_name );

		if ( $phone ) {
			$customer->set_billing_phone( $phone );
		}

		// Generate a username from the email.
		$username = wc_create_new_customer_username( $email, array(
			'first_name' => $first_name,
			'last_name'  => $last_name,
		) );
		$customer->set_username( $username );

		// Generate a random password (customer can reset via email).
		$customer->set_password( wp_generate_password() );

		$customer->save();

		if ( ! $customer->get_id() ) {
			return new \WP_Error(
				'customer_creation_failed',
				'Failed to create customer.',
				array( 'status' => 500 )
			);
		}

		// Ensure the user has the customer role.
		$user = new \WP_User( $customer->get_id() );
		$user->set_role( 'customer' );

		return new \WP_REST_Response( array(
			'success'  => true,
			'customer' => $this->format_customer( $customer ),
		), 201 );
	}

	/**
	 * Format a WC_Customer into the POS customer payload.
	 *
	 * @param \WC_Customer $customer The customer.
	 * @return array
	 */
	private function format_customer( $customer ) {
		return array(
			'id'         => $customer->get_id(),
			'first_name' => $customer->get_first_name() ?: $customer->get_billing_first_name(),
			'last_name'  => $customer->get_last_name() ?: $customer->get_billing_last_name(),
			'email'      => $customer->get_email(),
			'phone'      => $customer->get_billing_phone() ?: null,
		);
	}
}
