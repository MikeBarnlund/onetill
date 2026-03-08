<?php
/**
 * Product REST API endpoints.
 *
 * Handles:
 * - GET  /onetill/v1/products                — Full catalog (paginated)
 * - GET  /onetill/v1/products/delta           — Delta sync (modified_after)
 * - GET  /onetill/v1/products/{id}            — Single product
 * - GET  /onetill/v1/products/barcode/{code}  — Barcode lookup
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
	 * Barcode meta keys to check, in priority order.
	 *
	 * @var array
	 */
	private const BARCODE_META_KEYS = array(
		'_global_unique_id',
		'_onetill_barcode',
		'_barcode',
		'_ean',
		'_upc',
		'_gtin',
	);

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
		return Authenticator::check( $request );
	}

	/**
	 * Get paginated product catalog.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response
	 */
	public function get_products( $request ) {
		$page     = $request->get_param( 'page' ) ?: 1;
		$per_page = min( $request->get_param( 'per_page' ) ?: 100, 100 );
		$orderby  = $request->get_param( 'orderby' ) ?: 'id';
		$order    = strtoupper( $request->get_param( 'order' ) ?: 'asc' );

		// Map our orderby values to wc_get_products args.
		$orderby_map = array(
			'id'       => 'ID',
			'name'     => 'title',
			'modified' => 'modified',
		);
		$wc_orderby = isset( $orderby_map[ $orderby ] ) ? $orderby_map[ $orderby ] : 'ID';

		if ( ! in_array( $order, array( 'ASC', 'DESC' ), true ) ) {
			$order = 'ASC';
		}

		$args = array(
			'status'  => 'publish',
			'type'    => array( 'simple', 'variable' ),
			'limit'   => $per_page,
			'page'    => $page,
			'orderby' => $wc_orderby,
			'order'   => $order,
			'return'  => 'objects',
		);

		$products = wc_get_products( $args );

		// Total count for pagination.
		$count_args           = $args;
		$count_args['limit']  = -1;
		$count_args['page']   = 1;
		$count_args['return'] = 'ids';
		$total                = count( wc_get_products( $count_args ) );
		$total_pages          = (int) ceil( $total / $per_page );

		$data = array();
		foreach ( $products as $product ) {
			$data[] = $this->format_product( $product );
		}

		$response = new \WP_REST_Response( array(
			'products'    => $data,
			'total'       => $total,
			'total_pages' => $total_pages,
			'page'        => $page,
		), 200 );

		$response->header( 'X-OneTill-Total', $total );
		$response->header( 'X-OneTill-TotalPages', $total_pages );

		return $response;
	}

	/**
	 * Get products modified since a given timestamp (delta sync).
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response
	 */
	public function get_products_delta( $request ) {
		$modified_after  = $request->get_param( 'modified_after' );
		$page            = $request->get_param( 'page' ) ?: 1;
		$per_page        = min( $request->get_param( 'per_page' ) ?: 100, 100 );
		$include_deleted = $request->get_param( 'include_deleted' );

		$modified_timestamp = strtotime( $modified_after );
		if ( ! $modified_timestamp ) {
			return new \WP_REST_Response( array(
				'success' => false,
				'error'   => 'validation_error',
				'message' => 'Invalid modified_after timestamp.',
			), 400 );
		}

		$modified_date = gmdate( 'Y-m-d H:i:s', $modified_timestamp );

		$args = array(
			'status'       => 'publish',
			'type'         => array( 'simple', 'variable' ),
			'limit'        => $per_page,
			'page'         => $page,
			'orderby'      => 'modified',
			'order'        => 'ASC',
			'date_modified' => '>' . $modified_timestamp,
			'return'       => 'objects',
		);

		$products = wc_get_products( $args );

		// Total updated count for pagination.
		$count_args           = $args;
		$count_args['limit']  = -1;
		$count_args['page']   = 1;
		$count_args['return'] = 'ids';
		$total_updated        = count( wc_get_products( $count_args ) );
		$total_pages          = (int) ceil( $total_updated / $per_page );

		$updated = array();
		foreach ( $products as $product ) {
			$updated[] = $this->format_product( $product );
		}

		// Deleted products since modified_after.
		$deleted       = array();
		$total_deleted = 0;
		if ( $include_deleted ) {
			global $wpdb;
			$deleted = $wpdb->get_col(
				$wpdb->prepare(
					"SELECT product_id FROM {$wpdb->prefix}onetill_deleted_products WHERE deleted_at > %s ORDER BY product_id ASC",
					$modified_date
				)
			);
			$deleted       = array_map( 'intval', $deleted );
			$total_deleted = count( $deleted );
		}

		// Update device's last_sync timestamp.
		$sync_api = new API_Sync();
		$sync_api->update_device_last_sync( $request );

		return new \WP_REST_Response( array(
			'updated'       => $updated,
			'deleted'       => $deleted,
			'total_updated' => $total_updated,
			'total_deleted' => $total_deleted,
			'server_time'   => gmdate( 'c' ),
			'page'          => $page,
			'total_pages'   => $total_pages,
		), 200 );
	}

	/**
	 * Get a single product by ID.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response
	 */
	public function get_product( $request ) {
		$product_id = (int) $request->get_param( 'id' );
		$product    = wc_get_product( $product_id );

		if ( ! $product || 'publish' !== $product->get_status() ) {
			return new \WP_REST_Response( array(
				'success' => false,
				'error'   => 'not_found',
				'message' => 'Product not found.',
			), 404 );
		}

		// If this is a variation, return the parent product instead.
		if ( $product->is_type( 'variation' ) ) {
			$product = wc_get_product( $product->get_parent_id() );
			if ( ! $product ) {
				return new \WP_REST_Response( array(
					'success' => false,
					'error'   => 'not_found',
					'message' => 'Parent product not found.',
				), 404 );
			}
		}

		return new \WP_REST_Response( $this->format_product( $product ), 200 );
	}

	/**
	 * Look up a product by barcode.
	 *
	 * Uses a direct meta query for performance. Checks meta keys in order:
	 * _global_unique_id, _onetill_barcode, _barcode, _ean, _upc, _gtin.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response
	 */
	public function get_product_by_barcode( $request ) {
		$code = $request->get_param( 'code' );

		global $wpdb;

		// Build a single query that checks all barcode meta keys.
		// This is a direct meta lookup — not a full table scan.
		$placeholders = array();
		$values       = array();
		foreach ( self::BARCODE_META_KEYS as $meta_key ) {
			$placeholders[] = '(pm.meta_key = %s AND pm.meta_value = %s)';
			$values[]       = $meta_key;
			$values[]       = $code;
		}

		$where = implode( ' OR ', $placeholders );

		// phpcs:ignore WordPress.DB.PreparedSQL.InterpolatedNotPrepared
		$results = $wpdb->get_results(
			$wpdb->prepare(
				"SELECT p.ID, p.post_type, p.post_parent
				FROM {$wpdb->postmeta} pm
				INNER JOIN {$wpdb->posts} p ON p.ID = pm.post_id
				WHERE ({$where})
				AND p.post_status IN ('publish', 'inherit')
				AND p.post_type IN ('product', 'product_variation')
				ORDER BY FIELD(pm.meta_key, '_global_unique_id', '_onetill_barcode', '_barcode', '_ean', '_upc', '_gtin')
				LIMIT 1",
				...$values
			),
			ARRAY_A
		);

		if ( empty( $results ) ) {
			return new \WP_REST_Response( array(
				'found'   => false,
				'barcode' => $code,
			), 200 );
		}

		$match        = $results[0];
		$variation_id = null;

		if ( 'product_variation' === $match['post_type'] ) {
			$variation_id = (int) $match['ID'];
			$product_id   = (int) $match['post_parent'];
		} else {
			$product_id = (int) $match['ID'];
		}

		$product = wc_get_product( $product_id );

		if ( ! $product || 'publish' !== $product->get_status() ) {
			return new \WP_REST_Response( array(
				'found'   => false,
				'barcode' => $code,
			), 200 );
		}

		return new \WP_REST_Response( array(
			'found'        => true,
			'product'      => $this->format_product( $product ),
			'variation_id' => $variation_id,
		), 200 );
	}

	/**
	 * Format a WC_Product into the POS-optimized product payload.
	 *
	 * @param \WC_Product $product The product.
	 * @return array
	 */
	private function format_product( $product ) {
		// Categories.
		$categories = array();
		$term_ids   = $product->get_category_ids();
		foreach ( $term_ids as $term_id ) {
			$term = get_term( $term_id, 'product_cat' );
			if ( $term && ! is_wp_error( $term ) ) {
				$categories[] = array(
					'id'   => $term->term_id,
					'name' => $term->name,
				);
			}
		}

		// First image only — use woocommerce_thumbnail size.
		$images   = array();
		$image_id = $product->get_image_id();
		if ( $image_id ) {
			$src = wp_get_attachment_image_url( $image_id, 'woocommerce_thumbnail' );
			if ( $src ) {
				$images[] = array(
					'id'  => (int) $image_id,
					'src' => $src,
				);
			}
		}

		// Variations (embedded in parent for variable products).
		$variations = array();
		if ( $product->is_type( 'variable' ) ) {
			$children = $product->get_children();
			foreach ( $children as $child_id ) {
				$variation = wc_get_product( $child_id );
				if ( $variation && 'publish' === $variation->get_status() ) {
					$variations[] = $this->format_variation( $variation );
				}
			}
		}

		// manage_stock: pass through raw value.
		$manage_stock = $product->get_manage_stock();

		$date_modified = $product->get_date_modified();

		return array(
			'id'             => $product->get_id(),
			'name'           => $product->get_name(),
			'type'           => $product->get_type(),
			'status'         => $product->get_status(),
			'sku'            => $product->get_sku(),
			'barcode'        => $this->get_barcode( $product->get_id() ),
			'price'          => $product->get_price(),
			'regular_price'  => $product->get_regular_price(),
			'sale_price'     => $product->get_sale_price() ?: '',
			'on_sale'        => $product->is_on_sale(),
			'stock_quantity' => $product->get_stock_quantity(),
			'stock_status'   => $product->get_stock_status(),
			'manage_stock'   => $manage_stock,
			'tax_status'     => $product->get_tax_status(),
			'tax_class'      => $product->get_tax_class(),
			'categories'     => $categories,
			'images'         => $images,
			'variations'     => $variations,
			'modified'       => $date_modified ? $date_modified->date( 'c' ) : '',
		);
	}

	/**
	 * Format a WC_Product_Variation into the POS variation payload.
	 *
	 * IMPORTANT: manage_stock can be true, false, or the string "parent".
	 * We pass through the raw WooCommerce value — do NOT coerce to boolean.
	 *
	 * @param \WC_Product_Variation $variation The variation.
	 * @return array
	 */
	private function format_variation( $variation ) {
		// Attributes.
		$attributes = array();
		foreach ( $variation->get_attributes() as $attr_name => $attr_value ) {
			$taxonomy = str_replace( 'pa_', '', $attr_name );
			$label    = wc_attribute_label( $attr_name, $variation );

			// If it's a taxonomy attribute, get the term name.
			if ( taxonomy_exists( $attr_name ) ) {
				$term = get_term_by( 'slug', $attr_value, $attr_name );
				if ( $term ) {
					$attr_value = $term->name;
				}
			}

			$attributes[] = array(
				'name'   => $label,
				'option' => $attr_value,
			);
		}

		// Variation image.
		$image    = null;
		$image_id = $variation->get_image_id();
		if ( $image_id ) {
			$src = wp_get_attachment_image_url( $image_id, 'woocommerce_thumbnail' );
			if ( $src ) {
				$image = array(
					'id'  => (int) $image_id,
					'src' => $src,
				);
			}
		}

		// manage_stock: pass through raw value (true, false, or "parent").
		$manage_stock = $variation->get_manage_stock();

		return array(
			'id'             => $variation->get_id(),
			'sku'            => $variation->get_sku(),
			'barcode'        => $this->get_barcode( $variation->get_id() ),
			'price'          => $variation->get_price(),
			'regular_price'  => $variation->get_regular_price(),
			'sale_price'     => $variation->get_sale_price() ?: '',
			'on_sale'        => $variation->is_on_sale(),
			'stock_quantity' => $variation->get_stock_quantity(),
			'stock_status'   => $variation->get_stock_status(),
			'manage_stock'   => $manage_stock,
			'attributes'     => $attributes,
			'image'          => $image,
		);
	}

	/**
	 * Get the barcode value for a product or variation.
	 *
	 * Checks meta keys in priority order:
	 * _global_unique_id (WC 9.4+), _onetill_barcode, _barcode, _ean, _upc, _gtin.
	 *
	 * @param int $product_id The product/variation ID.
	 * @return string
	 */
	private function get_barcode( $product_id ) {
		foreach ( self::BARCODE_META_KEYS as $meta_key ) {
			$value = get_post_meta( $product_id, $meta_key, true );
			if ( ! empty( $value ) ) {
				return (string) $value;
			}
		}
		return '';
	}
}
