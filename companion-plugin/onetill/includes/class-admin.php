<?php
/**
 * WP Admin interface.
 *
 * Registers the OneTill admin menu, renders the dashboard and settings
 * pages, manages device connections, and adds barcode fields to the
 * product edit screen.
 *
 * @package OneTill
 */

namespace OneTill;

defined( 'ABSPATH' ) || exit;

/**
 * Class Admin
 */
class Admin {

	/**
	 * Register the admin menu.
	 *
	 * Adds a top-level "OneTill" menu item with submenu pages:
	 * - Dashboard — Connected devices, sync status, pair new device
	 * - Settings  — Barcode meta field mapping, plugin configuration
	 */
	public function register_menu() {
		add_menu_page(
			__( 'OneTill', 'onetill' ),
			__( 'OneTill', 'onetill' ),
			'manage_woocommerce',
			'onetill',
			array( $this, 'render_dashboard_page' ),
			'dashicons-store',
			56 // Below WooCommerce.
		);

		add_submenu_page(
			'onetill',
			__( 'Dashboard', 'onetill' ),
			__( 'Dashboard', 'onetill' ),
			'manage_woocommerce',
			'onetill',
			array( $this, 'render_dashboard_page' )
		);

		add_submenu_page(
			'onetill',
			__( 'Settings', 'onetill' ),
			__( 'Settings', 'onetill' ),
			'manage_woocommerce',
			'onetill-settings',
			array( $this, 'render_settings_page' )
		);
	}

	/**
	 * Enqueue admin CSS and JS on OneTill pages.
	 *
	 * @param string $hook_suffix The current admin page hook suffix.
	 */
	public function enqueue_assets( $hook_suffix ) {
		if ( false === strpos( $hook_suffix, 'onetill' ) ) {
			return;
		}

		wp_enqueue_style(
			'onetill-admin',
			ONETILL_PLUGIN_URL . 'assets/css/admin.css',
			array(),
			ONETILL_VERSION
		);

		wp_enqueue_script(
			'onetill-admin',
			ONETILL_PLUGIN_URL . 'assets/js/admin.js',
			array( 'jquery' ),
			ONETILL_VERSION,
			true
		);

		wp_localize_script( 'onetill-admin', 'onetillAdmin', array(
			'ajaxUrl' => admin_url( 'admin-ajax.php' ),
			'nonce'   => wp_create_nonce( 'onetill_admin' ),
			'i18n'    => array(
				'pairingTitle'     => __( 'Pair New Device', 'onetill' ),
				'waitingForDevice' => __( 'Waiting for device...', 'onetill' ),
				'pairingComplete'  => __( 'Device connected!', 'onetill' ),
				'pairingExpired'   => __( 'QR code expired. Please try again.', 'onetill' ),
				'disconnectConfirm' => __( 'Are you sure you want to disconnect this device?', 'onetill' ),
			),
		) );
	}

	/**
	 * Render the dashboard page.
	 *
	 * Displays connected devices table, "Pair New Device" button,
	 * sync status, and quick links.
	 */
	public function render_dashboard_page() {
		// TODO: Load template from templates/admin-dashboard.php.
	}

	/**
	 * Render the settings page.
	 *
	 * Displays barcode meta field mapping configuration and other
	 * plugin settings.
	 */
	public function render_settings_page() {
		// TODO: Implement settings page.
	}

	/**
	 * AJAX handler: Disconnect a device.
	 *
	 * Removes the device record and revokes its WooCommerce API keys.
	 */
	public function ajax_disconnect_device() {
		// TODO: Implement:
		// 1. Verify nonce
		// 2. Get device_id from request
		// 3. Look up api_key_id from wp_onetill_devices
		// 4. Delete WooCommerce API key
		// 5. Delete device record
		// 6. Return success
	}

	/**
	 * Render the barcode field on the product Inventory tab.
	 *
	 * Adds a "Barcode / UPC / EAN" input field for simple products.
	 */
	public function render_barcode_field() {
		// TODO: Render woocommerce_wp_text_input for _onetill_barcode.
	}

	/**
	 * Save the barcode field value on product save.
	 *
	 * @param int $post_id The product post ID.
	 */
	public function save_barcode_field( $post_id ) {
		// TODO: Sanitize and save _onetill_barcode meta.
	}

	/**
	 * Render the barcode field for each variation.
	 *
	 * @param int     $loop           The variation loop index.
	 * @param array   $variation_data The variation data.
	 * @param \WP_Post $variation      The variation post object.
	 */
	public function render_variation_barcode_field( $loop, $variation_data, $variation ) {
		// TODO: Render barcode input for variation.
	}

	/**
	 * Save the variation barcode field value.
	 *
	 * @param int $variation_id The variation ID.
	 * @param int $loop_index   The variation loop index.
	 */
	public function save_variation_barcode_field( $variation_id, $loop_index ) {
		// TODO: Sanitize and save _onetill_barcode meta for variation.
	}

	/**
	 * Get all connected devices.
	 *
	 * @return array List of device records.
	 */
	private function get_devices() {
		// TODO: Query wp_onetill_devices table.
		return array();
	}
}
