<?php
/**
 * Settings REST API endpoint.
 *
 * Handles:
 * - GET /onetill/v1/settings — Store settings, tax rates, plugin info
 *
 * @package OneTill
 */

namespace OneTill;

defined( 'ABSPATH' ) || exit;

/**
 * Class API_Settings
 */
class API_Settings {

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
			'/settings',
			array(
				'methods'             => 'GET',
				'callback'            => array( $this, 'get_settings' ),
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
	 * Get store settings.
	 *
	 * Returns store info (name, currency, separators), tax configuration
	 * (rates, prices_include_tax), plugin/WC/WP versions, product count,
	 * and timezone.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response|\WP_Error
	 */
	public function get_settings( $request ) {
		// TODO: Implement settings response with:
		// - store: name, url, currency, currency_position, separators, num_decimals
		// - tax: enabled, prices_include_tax, calc_taxes, tax_rates[]
		// - plugin_version, wc_version, wp_version, product_count, timezone
	}
}
