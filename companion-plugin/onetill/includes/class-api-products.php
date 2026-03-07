<?php
/**
 * Product REST API endpoints.
 *
 * Handles:
 * - GET  /onetill/v1/products           — Full catalog (paginated)
 * - GET  /onetill/v1/products/delta     — Delta sync (modified_after)
 * - GET  /onetill/v1/products/{id}      — Single product
 * - GET  /onetill/v1/products/barcode/{code} — Barcode lookup
 *
 * @package OneTill
 */

namespace OneTill;

defined( 'ABSPATH' ) || exit;

/**
 * Class API_Products
 */
class API_Products {

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
			'/products',
			array(
				'methods'             => 'GET',
				'callback'            => array( $this, 'get_products' ),
				'permission_callback' => array( $this, 'check_permissions' ),
				'args'                => array(
					'page'     => array(
						'default'           => 1,
						'sanitize_callback' => 'absint',
					),
					'per_page' => array(
						'default'           => 100,
						'sanitize_callback' => 'absint',
					),
					'orderby'  => array(
						'default'           => 'id',
						'sanitize_callback' => 'sanitize_text_field',
					),
					'order'    => array(
						'default'           => 'asc',
						'sanitize_callback' => 'sanitize_text_field',
					),
				),
			)
		);

		register_rest_route(
			self::NAMESPACE,
			'/products/delta',
			array(
				'methods'             => 'GET',
				'callback'            => array( $this, 'get_products_delta' ),
				'permission_callback' => array( $this, 'check_permissions' ),
				'args'                => array(
					'modified_after'  => array(
						'required'          => true,
						'sanitize_callback' => 'sanitize_text_field',
					),
					'page'            => array(
						'default'           => 1,
						'sanitize_callback' => 'absint',
					),
					'per_page'        => array(
						'default'           => 100,
						'sanitize_callback' => 'absint',
					),
					'include_deleted' => array(
						'default'           => true,
						'sanitize_callback' => 'rest_sanitize_boolean',
					),
				),
			)
		);

		register_rest_route(
			self::NAMESPACE,
			'/products/(?P<id>\d+)',
			array(
				'methods'             => 'GET',
				'callback'            => array( $this, 'get_product' ),
				'permission_callback' => array( $this, 'check_permissions' ),
				'args'                => array(
					'id' => array(
						'required'          => true,
						'sanitize_callback' => 'absint',
					),
				),
			)
		);

		register_rest_route(
			self::NAMESPACE,
			'/products/barcode/(?P<code>[a-zA-Z0-9\-]+)',
			array(
				'methods'             => 'GET',
				'callback'            => array( $this, 'get_product_by_barcode' ),
				'permission_callback' => array( $this, 'check_permissions' ),
				'args'                => array(
					'code' => array(
						'required'          => true,
						'sanitize_callback' => 'sanitize_text_field',
					),
				),
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
	 * Get paginated product catalog.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response|\WP_Error
	 */
	public function get_products( $request ) {
		// TODO: Implement full catalog fetch with POS-optimized payload.
	}

	/**
	 * Get products modified since a given timestamp (delta sync).
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response|\WP_Error
	 */
	public function get_products_delta( $request ) {
		// TODO: Implement delta sync with modified_after filter and deleted product tracking.
	}

	/**
	 * Get a single product by ID.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response|\WP_Error
	 */
	public function get_product( $request ) {
		// TODO: Implement single product fetch.
	}

	/**
	 * Look up a product by barcode.
	 *
	 * Checks _onetill_barcode meta first, then falls back to common
	 * barcode plugin meta keys (_barcode, _ean, _upc, _gtin).
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response|\WP_Error
	 */
	public function get_product_by_barcode( $request ) {
		// TODO: Implement barcode lookup with meta key fallback chain.
	}

	/**
	 * Format a WC_Product into the POS-optimized product payload.
	 *
	 * Returns only fields the S700 needs: id, name, type, status, sku,
	 * barcode, prices, stock, tax, categories, first image, variations.
	 *
	 * @param \WC_Product $product The product.
	 * @return array
	 */
	private function format_product( $product ) {
		// TODO: Implement product formatting.
		// IMPORTANT: manage_stock on variations must pass through raw value
		// (true, false, or the string "parent"). Do NOT coerce to boolean.
		return array();
	}

	/**
	 * Format a WC_Product_Variation into the POS variation payload.
	 *
	 * @param \WC_Product_Variation $variation The variation.
	 * @return array
	 */
	private function format_variation( $variation ) {
		// TODO: Implement variation formatting.
		// IMPORTANT: manage_stock can be true, false, or "parent".
		return array();
	}

	/**
	 * Get the barcode value for a product or variation.
	 *
	 * Checks _onetill_barcode first, then falls back to common meta keys.
	 *
	 * @param int $product_id The product/variation ID.
	 * @return string
	 */
	private function get_barcode( $product_id ) {
		// TODO: Implement barcode meta key fallback chain.
		return '';
	}
}
