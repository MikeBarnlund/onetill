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
			__( 'Users', 'onetill' ),
			__( 'Users', 'onetill' ),
			'manage_woocommerce',
			'onetill-users',
			array( $this, 'render_users_page' )
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
				'addUser'           => __( 'Add User', 'onetill' ),
				'editUser'          => __( 'Edit User', 'onetill' ),
				'save'              => __( 'Save', 'onetill' ),
				'saving'            => __( 'Saving...', 'onetill' ),
				'nameRequired'      => __( 'First and last name are required.', 'onetill' ),
				'pinRequired'       => __( 'PIN is required.', 'onetill' ),
				'pinFormat'         => __( 'PIN must be exactly 4 digits.', 'onetill' ),
				'pinLeaveBlank'     => __( '(leave blank to keep current)', 'onetill' ),
				/* translators: %s: staff member's name */
				'deleteConfirm'     => __( 'Are you sure you want to delete %s?', 'onetill' ),
				'userError'         => __( 'Something went wrong. Please try again.', 'onetill' ),
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
		include ONETILL_PLUGIN_DIR . 'templates/admin-settings.php';
	}

	/**
	 * Render the users page.
	 */
	public function render_users_page() {
		$users = $this->get_users();

		include ONETILL_PLUGIN_DIR . 'templates/admin-users.php';
	}

	/**
	 * AJAX handler: Create a user.
	 */
	public function ajax_create_user() {
		check_ajax_referer( 'onetill_admin', 'nonce' );

		if ( ! current_user_can( 'manage_woocommerce' ) ) {
			wp_send_json_error( array( 'message' => __( 'Permission denied.', 'onetill' ) ), 403 );
		}

		$first_name = isset( $_POST['first_name'] ) ? sanitize_text_field( wp_unslash( $_POST['first_name'] ) ) : '';
		$last_name  = isset( $_POST['last_name'] ) ? sanitize_text_field( wp_unslash( $_POST['last_name'] ) ) : '';
		$pin        = isset( $_POST['pin'] ) ? sanitize_text_field( wp_unslash( $_POST['pin'] ) ) : '';

		if ( empty( $first_name ) || empty( $last_name ) || empty( $pin ) ) {
			wp_send_json_error( array( 'message' => __( 'All fields are required.', 'onetill' ) ), 400 );
		}

		if ( ! preg_match( '/^\d{4}$/', $pin ) ) {
			wp_send_json_error( array( 'message' => __( 'PIN must be exactly 4 digits.', 'onetill' ) ), 400 );
		}

		if ( $this->is_pin_taken( $pin ) ) {
			wp_send_json_error( array( 'message' => __( 'This PIN is already in use. Please choose a different one.', 'onetill' ) ), 409 );
		}

		global $wpdb;

		$now = current_time( 'mysql', true );

		// phpcs:ignore WordPress.DB.DirectDatabaseQuery.DirectQuery -- custom table, no WP API available.
		$wpdb->insert(
			$wpdb->prefix . 'onetill_users',
			array(
				'first_name' => $first_name,
				'last_name'  => $last_name,
				'pin'        => wp_hash_password( $pin ),
				'pin_sha256' => hash( 'sha256', $pin ),
				'created_at' => $now,
				'updated_at' => $now,
			),
			array( '%s', '%s', '%s', '%s', '%s', '%s' )
		);

		$user_id = $wpdb->insert_id;

		if ( ! $user_id ) {
			wp_send_json_error( array( 'message' => __( 'Failed to create user.', 'onetill' ) ), 500 );
		}

		wp_send_json_success( array(
			'message' => __( 'User created.', 'onetill' ),
			'user'    => array(
				'id'         => $user_id,
				'first_name' => $first_name,
				'last_name'  => $last_name,
			),
		) );
	}

	/**
	 * AJAX handler: Update a user.
	 */
	public function ajax_update_user() {
		check_ajax_referer( 'onetill_admin', 'nonce' );

		if ( ! current_user_can( 'manage_woocommerce' ) ) {
			wp_send_json_error( array( 'message' => __( 'Permission denied.', 'onetill' ) ), 403 );
		}

		$user_id    = isset( $_POST['user_id'] ) ? absint( $_POST['user_id'] ) : 0;
		$first_name = isset( $_POST['first_name'] ) ? sanitize_text_field( wp_unslash( $_POST['first_name'] ) ) : '';
		$last_name  = isset( $_POST['last_name'] ) ? sanitize_text_field( wp_unslash( $_POST['last_name'] ) ) : '';
		$pin        = isset( $_POST['pin'] ) ? sanitize_text_field( wp_unslash( $_POST['pin'] ) ) : '';

		if ( empty( $user_id ) || empty( $first_name ) || empty( $last_name ) ) {
			wp_send_json_error( array( 'message' => __( 'Name fields are required.', 'onetill' ) ), 400 );
		}

		global $wpdb;

		// phpcs:ignore WordPress.DB.DirectDatabaseQuery.DirectQuery, WordPress.DB.DirectDatabaseQuery.NoCaching -- custom table, no WP API available.
		$existing = $wpdb->get_row(
			$wpdb->prepare(
				"SELECT id FROM {$wpdb->prefix}onetill_users WHERE id = %d",
				$user_id
			)
		);

		if ( ! $existing ) {
			wp_send_json_error( array( 'message' => __( 'User not found.', 'onetill' ) ), 404 );
		}

		$data   = array(
			'first_name' => $first_name,
			'last_name'  => $last_name,
			'updated_at' => current_time( 'mysql', true ),
		);
		$format = array( '%s', '%s', '%s' );

		// Only update PIN if provided.
		if ( ! empty( $pin ) ) {
			if ( ! preg_match( '/^\d{4}$/', $pin ) ) {
				wp_send_json_error( array( 'message' => __( 'PIN must be exactly 4 digits.', 'onetill' ) ), 400 );
			}

			if ( $this->is_pin_taken( $pin, $user_id ) ) {
				wp_send_json_error( array( 'message' => __( 'This PIN is already in use. Please choose a different one.', 'onetill' ) ), 409 );
			}

			$data['pin']        = wp_hash_password( $pin );
			$data['pin_sha256'] = hash( 'sha256', $pin );
			$format[]           = '%s';
			$format[]           = '%s';
		}

		// phpcs:ignore WordPress.DB.DirectDatabaseQuery.DirectQuery, WordPress.DB.DirectDatabaseQuery.NoCaching -- custom table, no WP API available.
		$wpdb->update(
			$wpdb->prefix . 'onetill_users',
			$data,
			array( 'id' => $user_id ),
			$format,
			array( '%d' )
		);

		wp_send_json_success( array(
			'message' => __( 'User updated.', 'onetill' ),
			'user'    => array(
				'id'         => $user_id,
				'first_name' => $first_name,
				'last_name'  => $last_name,
			),
		) );
	}

	/**
	 * AJAX handler: Delete a user.
	 */
	public function ajax_delete_user() {
		check_ajax_referer( 'onetill_admin', 'nonce' );

		if ( ! current_user_can( 'manage_woocommerce' ) ) {
			wp_send_json_error( array( 'message' => __( 'Permission denied.', 'onetill' ) ), 403 );
		}

		$user_id = isset( $_POST['user_id'] ) ? absint( $_POST['user_id'] ) : 0;

		if ( empty( $user_id ) ) {
			wp_send_json_error( array( 'message' => __( 'Missing user ID.', 'onetill' ) ), 400 );
		}

		global $wpdb;

		// phpcs:ignore WordPress.DB.DirectDatabaseQuery.DirectQuery, WordPress.DB.DirectDatabaseQuery.NoCaching -- custom table, no WP API available.
		$deleted = $wpdb->delete(
			$wpdb->prefix . 'onetill_users',
			array( 'id' => $user_id ),
			array( '%d' )
		);

		if ( ! $deleted ) {
			wp_send_json_error( array( 'message' => __( 'User not found.', 'onetill' ) ), 404 );
		}

		wp_send_json_success( array( 'message' => __( 'User deleted.', 'onetill' ) ) );
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

		// phpcs:ignore WordPress.DB.DirectDatabaseQuery.DirectQuery, WordPress.DB.DirectDatabaseQuery.NoCaching -- custom table, no WP API available.
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
			// phpcs:ignore WordPress.DB.DirectDatabaseQuery.DirectQuery, WordPress.DB.DirectDatabaseQuery.NoCaching -- WC API keys table, no public API to revoke.
			$wpdb->delete(
				$wpdb->prefix . 'woocommerce_api_keys',
				array( 'key_id' => $device['api_key_id'] ),
				array( '%d' )
			);
		}

		// phpcs:ignore WordPress.DB.DirectDatabaseQuery.DirectQuery, WordPress.DB.DirectDatabaseQuery.NoCaching -- custom table, no WP API available.
		$wpdb->delete(
			$wpdb->prefix . 'onetill_devices',
			array( 'id' => $device_id ),
			array( '%s' )
		);

		wp_send_json_success( array( 'message' => __( 'Device disconnected.', 'onetill' ) ) );
	}

	/**
	 * Handle saving the Stripe secret key from admin form.
	 */
	public function save_stripe_key() {
		if ( ! current_user_can( 'manage_woocommerce' ) ) {
			wp_die( esc_html__( 'Permission denied.', 'onetill' ) );
		}

		check_admin_referer( 'onetill_save_stripe_key', 'onetill_stripe_nonce' );

		$key = isset( $_POST['onetill_stripe_secret_key'] ) ? sanitize_text_field( wp_unslash( $_POST['onetill_stripe_secret_key'] ) ) : '';
		update_option( 'onetill_stripe_secret_key', $key );

		wp_safe_redirect( admin_url( 'admin.php?page=onetill-settings&stripe_saved=1' ) );
		exit;
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
		// Nonce is already verified by WooCommerce in woocommerce_process_product_meta.
		// phpcs:ignore WordPress.Security.NonceVerification.Missing
		if ( isset( $_POST['_onetill_barcode'] ) ) {
			update_post_meta(
				$post_id,
				'_onetill_barcode',
				// phpcs:ignore WordPress.Security.NonceVerification.Missing
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
		// Nonce is already verified by WooCommerce in woocommerce_save_product_variation.
		// phpcs:ignore WordPress.Security.NonceVerification.Missing
		if ( isset( $_POST['onetill_variation_barcode'][ $loop_index ] ) ) {
			update_post_meta(
				$variation_id,
				'_onetill_barcode',
				// phpcs:ignore WordPress.Security.NonceVerification.Missing
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

	/**
	 * Get all OneTill users.
	 *
	 * @return array List of user records.
	 */
	private function get_users() {
		global $wpdb;

		return $wpdb->get_results(
			"SELECT id, first_name, last_name, created_at FROM {$wpdb->prefix}onetill_users ORDER BY first_name ASC, last_name ASC",
			ARRAY_A
		);
	}

	/**
	 * Check if a PIN is already in use by another user.
	 *
	 * @param string $pin     The plaintext PIN to check.
	 * @param int    $exclude Optional user ID to exclude (for updates).
	 * @return bool
	 */
	private function is_pin_taken( $pin, $exclude = 0 ) {
		global $wpdb;

		$query = "SELECT id, pin FROM {$wpdb->prefix}onetill_users";
		if ( $exclude ) {
			$query = $wpdb->prepare(
				"SELECT id, pin FROM {$wpdb->prefix}onetill_users WHERE id != %d",
				$exclude
			);
		}

		$users = $wpdb->get_results( $query, ARRAY_A ); // phpcs:ignore WordPress.DB.PreparedSQL.NotPrepared

		foreach ( $users as $user ) {
			if ( wp_check_password( $pin, $user['pin'] ) ) {
				return true;
			}
		}

		return false;
	}
}
