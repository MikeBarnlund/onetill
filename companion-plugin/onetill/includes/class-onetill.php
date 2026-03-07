<?php
/**
 * Core plugin class.
 *
 * Registers all hooks, initializes sub-classes, and wires up
 * the REST API endpoints, admin pages, and WooCommerce integrations.
 *
 * @package OneTill
 */

namespace OneTill;

defined( 'ABSPATH' ) || exit;

/**
 * Class OneTill
 */
class OneTill {

	/**
	 * API Products handler.
	 *
	 * @var API_Products
	 */
	private $api_products;

	/**
	 * API Orders handler.
	 *
	 * @var API_Orders
	 */
	private $api_orders;

	/**
	 * API Customers handler.
	 *
	 * @var API_Customers
	 */
	private $api_customers;

	/**
	 * API Settings handler.
	 *
	 * @var API_Settings
	 */
	private $api_settings;

	/**
	 * API Sync handler.
	 *
	 * @var API_Sync
	 */
	private $api_sync;

	/**
	 * Pairing handler.
	 *
	 * @var Pairing
	 */
	private $pairing;

	/**
	 * Webhooks handler.
	 *
	 * @var Webhooks
	 */
	private $webhooks;

	/**
	 * API Coupons handler.
	 *
	 * @var API_Coupons
	 */
	private $api_coupons;

	/**
	 * Admin handler.
	 *
	 * @var Admin
	 */
	private $admin;

	/**
	 * Initialize the plugin.
	 *
	 * Instantiates all sub-classes and registers WordPress/WooCommerce hooks.
	 */
	public function init() {
		$this->api_products  = new API_Products();
		$this->api_orders    = new API_Orders();
		$this->api_customers = new API_Customers();
		$this->api_coupons   = new API_Coupons();
		$this->api_settings  = new API_Settings();
		$this->api_sync      = new API_Sync();
		$this->pairing       = new Pairing();
		$this->webhooks      = new Webhooks();
		$this->admin         = new Admin();

		$this->register_hooks();
	}

	/**
	 * Register all hooks.
	 */
	private function register_hooks() {
		// REST API routes.
		add_action( 'rest_api_init', array( $this->api_products, 'register_routes' ) );
		add_action( 'rest_api_init', array( $this->api_orders, 'register_routes' ) );
		add_action( 'rest_api_init', array( $this->api_customers, 'register_routes' ) );
		add_action( 'rest_api_init', array( $this->api_coupons, 'register_routes' ) );
		add_action( 'rest_api_init', array( $this->api_settings, 'register_routes' ) );
		add_action( 'rest_api_init', array( $this->api_sync, 'register_routes' ) );
		add_action( 'rest_api_init', array( $this->pairing, 'register_routes' ) );

		// Admin pages.
		add_action( 'admin_menu', array( $this->admin, 'register_menu' ) );
		add_action( 'admin_enqueue_scripts', array( $this->admin, 'enqueue_assets' ) );

		// WooCommerce product hooks for change log and parent touch.
		add_action( 'woocommerce_update_product', array( $this->webhooks, 'on_product_updated' ) );
		add_action( 'woocommerce_new_product', array( $this->webhooks, 'on_product_created' ) );
		add_action( 'woocommerce_trash_product', array( $this->webhooks, 'on_product_deleted' ) );
		add_action( 'woocommerce_delete_product', array( $this->webhooks, 'on_product_deleted' ) );
		add_action( 'woocommerce_product_set_stock', array( $this->webhooks, 'on_product_stock_changed' ) );
		add_action( 'woocommerce_variation_set_stock', array( $this->webhooks, 'on_variation_stock_changed' ) );
		add_action( 'woocommerce_save_product_variation', array( $this->webhooks, 'on_variation_saved' ), 20, 2 );
		add_action( 'woocommerce_new_order', array( $this->webhooks, 'on_order_created' ) );

		// Barcode meta field on product edit screen.
		add_action( 'woocommerce_product_options_inventory_product_data', array( $this->admin, 'render_barcode_field' ) );
		add_action( 'woocommerce_process_product_meta', array( $this->admin, 'save_barcode_field' ) );
		add_action( 'woocommerce_product_after_variable_attributes', array( $this->admin, 'render_variation_barcode_field' ), 10, 3 );
		add_action( 'woocommerce_save_product_variation', array( $this->admin, 'save_variation_barcode_field' ), 10, 2 );

		// Cron jobs.
		add_action( 'onetill_cleanup_change_log', array( $this->webhooks, 'cleanup_change_log' ) );
		add_action( 'onetill_cleanup_expired_tokens', array( $this->pairing, 'cleanup_expired_tokens' ) );
		add_action( 'onetill_cleanup_idempotency', array( $this->api_orders, 'cleanup_idempotency' ) );

		// AJAX handlers for admin pairing UI.
		add_action( 'wp_ajax_onetill_initiate_pairing', array( $this->pairing, 'ajax_initiate_pairing' ) );
		add_action( 'wp_ajax_onetill_check_pairing_status', array( $this->pairing, 'ajax_check_pairing_status' ) );
		add_action( 'wp_ajax_onetill_disconnect_device', array( $this->admin, 'ajax_disconnect_device' ) );

		// Rate limiting for REST API endpoints.
		add_filter( 'rest_pre_dispatch', array( $this, 'apply_rate_limit' ), 10, 3 );
	}

	/**
	 * Apply rate limiting to OneTill REST API endpoints.
	 *
	 * @param mixed            $result  Response to replace the requested version with.
	 * @param \WP_REST_Server  $server  Server instance.
	 * @param \WP_REST_Request $request Request used to generate the response.
	 * @return mixed|\WP_Error
	 */
	public function apply_rate_limit( $result, $server, $request ) {
		$route = $request->get_route();

		// Only rate-limit OneTill endpoints.
		if ( 0 !== strpos( $route, '/onetill/v1/' ) ) {
			return $result;
		}

		// Skip pairing endpoints (they have their own IP-based rate limiting).
		if ( 0 === strpos( $route, '/onetill/v1/pair' ) ) {
			return $result;
		}

		// Use the authenticated user ID as the device identifier.
		$device_id = (string) get_current_user_id();
		if ( '0' === $device_id ) {
			return $result; // Not authenticated yet — permission_callback will reject.
		}

		// Determine the endpoint group.
		if ( false !== strpos( $route, '/sync/heartbeat' ) ) {
			$group = 'heartbeat';
		} elseif ( 'GET' === $request->get_method() ) {
			$group = 'read';
		} else {
			$group = 'write';
		}

		if ( ! Rate_Limiter::check( $device_id, $group ) ) {
			return Rate_Limiter::error();
		}

		return $result;
	}
}
