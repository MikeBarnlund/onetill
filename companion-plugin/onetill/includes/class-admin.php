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
	 */
	public function register_menu() {
		add_menu_page(
			__( 'OneTill', 'onetill' ),
			__( 'OneTill', 'onetill' ),
			'manage_woocommerce',
			'onetill',
			array( $this, 'render_dashboard_page' ),
			'dashicons-store',
			56
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
				'pairingTitle'      => __( 'Pair New Device', 'onetill' ),
				'waitingForDevice'  => __( 'Waiting for device to scan QR code...', 'onetill' ),
				'pairingComplete'   => __( 'Device connected!', 'onetill' ),
				'pairingExpired'    => __( 'QR code expired. Click "Pair New Device" to try again.', 'onetill' ),
				'pairingError'      => __( 'Something went wrong. Please try again.', 'onetill' ),
				'disconnectConfirm' => __( 'Are you sure you want to disconnect this device? It will need to be re-paired.', 'onetill' ),
				'disconnected'      => __( 'Device disconnected.', 'onetill' ),
				'never'             => __( 'Never', 'onetill' ),
			),
		) );
	}

	/**
	 * Render the dashboard page.
	 */
	public function render_dashboard_page() {
		$devices     = $this->get_devices();
		$https_ok    = is_ssl();
		$change_logs = $this->get_change_log_stats();

		include ONETILL_PLUGIN_DIR . 'templates/admin-dashboard.php';
	}

	/**
	 * Render the settings page.
	 */
	public function render_settings_page() {
		// TODO: Implement settings page (barcode meta field mapping).
		echo '<div class="wrap"><h1>' . esc_html__( 'OneTill Settings', 'onetill' ) . '</h1>';
		echo '<p>' . esc_html__( 'Settings page coming soon.', 'onetill' ) . '</p></div>';
	}

	/**
	 * AJAX handler: Disconnect a device.
	 */
	public function ajax_disconnect_device() {
		check_ajax_referer( 'onetill_admin', 'nonce' );

		if ( ! current_user_can( 'manage_woocommerce' ) ) {
			wp_send_json_error( array( 'message' => __( 'Permission denied.', 'onetill' ) ), 403 );
		}

		$device_id = isset( $_POST['device_id'] ) ? sanitize_text_field( wp_unslash( $_POST['device_id'] ) ) : '';

		if ( empty( $device_id ) ) {
			wp_send_json_error( array( 'message' => __( 'Missing device ID.', 'onetill' ) ), 400 );
		}

		global $wpdb;

		$device = $wpdb->get_row(
			$wpdb->prepare(
				"SELECT api_key_id FROM {$wpdb->prefix}onetill_devices WHERE id = %s",
				$device_id
			),
			ARRAY_A
		);

		if ( ! $device ) {
			wp_send_json_error( array( 'message' => __( 'Device not found.', 'onetill' ) ), 404 );
		}

		// Revoke the WooCommerce API key.
		if ( ! empty( $device['api_key_id'] ) ) {
			$wpdb->delete(
				$wpdb->prefix . 'woocommerce_api_keys',
				array( 'key_id' => $device['api_key_id'] ),
				array( '%d' )
			);
		}

		// Delete the device record.
		$wpdb->delete(
			$wpdb->prefix . 'onetill_devices',
			array( 'id' => $device_id ),
			array( '%s' )
		);

		wp_send_json_success( array( 'message' => __( 'Device disconnected.', 'onetill' ) ) );
	}

	/**
	 * Render the barcode field on the product Inventory tab.
	 */
	public function render_barcode_field() {
		woocommerce_wp_text_input( array(
			'id'          => '_onetill_barcode',
			'label'       => __( 'Barcode / UPC / EAN', 'onetill' ),
			'desc_tip'    => true,
			'description' => __( 'Barcode for POS scanning. Supports EAN-13, UPC-A, or any string.', 'onetill' ),
			'placeholder' => __( 'e.g. 0123456789012', 'onetill' ),
		) );
	}

	/**
	 * Save the barcode field value on product save.
	 *
	 * @param int $post_id The product post ID.
	 */
	public function save_barcode_field( $post_id ) {
		if ( isset( $_POST['_onetill_barcode'] ) ) {
			update_post_meta(
				$post_id,
				'_onetill_barcode',
				sanitize_text_field( wp_unslash( $_POST['_onetill_barcode'] ) )
			);
		}
	}

	/**
	 * Render the barcode field for each variation.
	 *
	 * @param int      $loop           The variation loop index.
	 * @param array    $variation_data The variation data.
	 * @param \WP_Post $variation      The variation post object.
	 */
	public function render_variation_barcode_field( $loop, $variation_data, $variation ) {
		$value = get_post_meta( $variation->ID, '_onetill_barcode', true );
		?>
		<p class="form-row form-row-full">
			<label for="onetill_barcode_<?php echo esc_attr( $loop ); ?>">
				<?php esc_html_e( 'Barcode / UPC / EAN', 'onetill' ); ?>
			</label>
			<input type="text"
				id="onetill_barcode_<?php echo esc_attr( $loop ); ?>"
				name="onetill_variation_barcode[<?php echo esc_attr( $loop ); ?>]"
				value="<?php echo esc_attr( $value ); ?>"
				placeholder="<?php esc_attr_e( 'e.g. 0123456789012', 'onetill' ); ?>"
			/>
		</p>
		<?php
	}

	/**
	 * Save the variation barcode field value.
	 *
	 * @param int $variation_id The variation ID.
	 * @param int $loop_index   The variation loop index.
	 */
	public function save_variation_barcode_field( $variation_id, $loop_index ) {
		if ( isset( $_POST['onetill_variation_barcode'][ $loop_index ] ) ) {
			update_post_meta(
				$variation_id,
				'_onetill_barcode',
				sanitize_text_field( wp_unslash( $_POST['onetill_variation_barcode'][ $loop_index ] ) )
			);
		}
	}

	/**
	 * Get all connected devices.
	 *
	 * @return array List of device records.
	 */
	private function get_devices() {
		global $wpdb;

		return $wpdb->get_results(
			"SELECT * FROM {$wpdb->prefix}onetill_devices ORDER BY paired_at DESC",
			ARRAY_A
		);
	}

	/**
	 * Get change log stats for the dashboard.
	 *
	 * @return array { last_hour, last_24h }
	 */
	private function get_change_log_stats() {
		global $wpdb;

		$last_hour = (int) $wpdb->get_var(
			$wpdb->prepare(
				"SELECT COUNT(*) FROM {$wpdb->prefix}onetill_change_log WHERE timestamp > %s",
				gmdate( 'Y-m-d H:i:s', time() - HOUR_IN_SECONDS )
			)
		);

		$last_24h = (int) $wpdb->get_var(
			$wpdb->prepare(
				"SELECT COUNT(*) FROM {$wpdb->prefix}onetill_change_log WHERE timestamp > %s",
				gmdate( 'Y-m-d H:i:s', time() - DAY_IN_SECONDS )
			)
		);

		return array(
			'last_hour' => $last_hour,
			'last_24h'  => $last_24h,
		);
	}
}
