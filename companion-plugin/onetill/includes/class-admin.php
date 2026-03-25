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
	 * Get the base64-encoded SVG icon for the admin menu.
	 *
	 * The path uses boolean subtraction so the "1" is a real geometric hole.
	 * No fill attribute — WordPress's svg-painter.js applies the admin color.
	 *
	 * @return string Data URI for the OneTill logo SVG.
	 */
	private static function get_menu_icon() {
		return 'data:image/svg+xml;base64,' . base64_encode(
			'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024">'
			. '<path fill="#a7aaad" d="M642.908 0C659.742 10.4548 679.06 19.2476 695.63 29.8721C694.835 40.5125 695.711 56.0974 695.509 67.1924C694.955 97.6263 696.341 129.601 695.476 159.913C670.965 171.322 645.142 180.86 620.572 192.061C621.32 198.843 621.042 210.274 621.03 217.328L814.78 134.57L889.942 102.514C901.166 97.7774 913.208 92.3784 924.513 88.0537C934.115 91.9489 947.1 98.9321 956.5 103.493C979.104 114.459 1001.42 126.327 1024 137.357V778.713L641.979 950.057L531.852 999.534C519.938 1004.78 508.064 1010.11 496.229 1015.52C491.568 1017.65 482.386 1021.53 478.349 1024H476.119C474.632 1022.87 472.197 1021.44 469.677 1020.04C468.416 1019.33 467.135 1018.64 465.939 1017.99C464.744 1017.35 463.635 1016.75 462.721 1016.24C455.203 1012.06 447.727 1007.82 440.292 1003.5C407.521 984.503 374.623 965.725 341.599 947.169L0 755.697V594.004C0.182611 593.901 0.385508 593.791 0.606445 593.676C1.26861 593.33 2.09232 592.932 3.01172 592.504C5.46436 591.363 8.59982 590.013 11.1797 588.901C12.4695 588.345 13.6204 587.849 14.4775 587.467L44.0898 574.207L138.255 531.762C137.795 527.808 137.759 519.157 138.413 515.215C138.663 513.706 139.532 513.453 140.595 512.883C141.005 512.674 141.423 512.463 141.849 512.253C148.23 509.095 156.218 505.796 162.682 502.95L206.697 483.724L348.066 422.12L348.143 422.027C348.691 421.267 349.426 418.23 350.286 413.677C350.532 412.376 350.788 410.953 351.053 409.423C355.426 384.179 362.22 330.12 365.148 327.126C365.178 327.096 365.208 327.071 365.236 327.051L366.761 326.352C370.347 324.725 374.117 323.145 377.724 321.66C425.797 301.875 473.3 279.763 521.373 260.01C522.924 259.373 524.475 258.739 526.027 258.106L526.08 257.998C527.159 255.449 526.426 234.997 526.14 231.424C505.164 239.888 483.743 250.62 463.193 258.729C459.951 258.357 415.736 232.625 409.512 229.101C408.072 195.469 409.685 161.13 408.839 127.42C408.672 120.79 408.523 113.453 408.711 106.501C408.742 105.342 408.782 104.194 408.834 103.062C408.886 101.929 408.949 100.813 409.023 99.7178L409.629 99.3818C416.023 95.8877 426.517 91.6319 433.231 88.7275L474.881 70.8799L591.292 21.4453L620.229 9.27441C626.676 6.57522 634.646 3.40762 640.646 0H642.908ZM775.041 384.711C771.144 386.363 757.301 392.02 754.552 394.133C743.836 402.374 732.302 415.038 722.363 424.479C702.931 442.94 683.437 461.626 662.615 478.456C668.448 498.932 673.303 520.154 679.319 540.333C697.759 525.042 716.19 508.358 734.875 493.671C734.144 501.972 734.414 512.22 734.422 520.703L734.475 563.767L734.571 702.976C714.638 712.766 692.896 721.074 672.491 730.323C672.823 743.962 672.971 757.606 672.935 771.25L672.986 800.582L782.315 752.271C811.361 739.018 840.604 726.197 870.033 713.818C870.196 690.559 870.131 667.299 869.838 644.041C849.559 652.27 828.433 662.006 808.426 670.894L808.412 471.473L808.404 405.724C808.404 401.693 808.988 372.558 807.515 370.988L775.041 384.711Z"/>'
			. '</svg>'
		);
	}

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
			self::get_menu_icon(),
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
