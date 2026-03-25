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
	 * @return string Data URI for the OneTill logo SVG.
	 */
	private static function get_menu_icon() {
		$svg = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024">'
			. '<defs><mask id="m"><rect width="1024" height="1024" fill="white"/>'
			. '<path fill="black" d="M807.515 370.988C808.988 372.558 808.404 401.695 808.404 405.724L808.412 471.473L808.426 670.894C828.433 662.006 849.559 652.27 869.838 644.041C870.131 667.299 870.196 690.559 870.033 713.818C840.604 726.197 811.361 739.017 782.315 752.271L672.986 800.582L672.935 771.25C672.971 757.606 672.823 743.962 672.491 730.323C692.896 721.074 714.638 712.766 734.571 702.976L734.475 563.767L734.422 520.703C734.414 512.22 734.144 501.972 734.875 493.671C716.19 508.358 697.759 525.042 679.319 540.333C673.303 520.154 668.448 498.932 662.615 478.456C683.437 461.626 702.931 442.94 722.363 424.479C732.302 415.037 743.836 402.374 754.552 394.133C757.301 392.02 771.144 386.363 775.041 384.711L807.515 370.988Z"/>'
			. '</mask></defs>'
			. '<g mask="url(#m)">'
			. '<path d="M621.03 217.328L814.78 134.57L889.942 102.514C901.166 97.7777 913.208 92.3787 924.513 88.0539C934.115 91.9491 947.1 98.9319 956.5 103.493C979.104 114.459 1001.42 126.327 1024 137.357L1024 141.124L1024 778.713L641.979 950.057L531.852 999.534C519.938 1004.78 508.064 1010.11 496.229 1015.52C491.568 1017.65 482.386 1021.53 478.349 1024L476.398 1024L476.119 1024C473.145 1021.75 466.38 1018.27 462.721 1016.24C455.203 1012.06 447.727 1007.82 440.292 1003.5C407.521 984.503 374.623 965.725 341.599 947.169L0 755.697L0 596.128L0 594.004C2.9198 592.359 11.0489 588.995 14.4776 587.467L44.0902 574.207L138.255 531.762C137.795 527.808 137.759 519.157 138.413 515.215C138.663 513.706 139.532 513.453 140.595 512.883C147.158 509.537 155.787 505.986 162.682 502.95L206.697 483.724L348.066 422.12C351.299 418.815 361.551 329.579 365.236 327.051C369.266 325.178 373.602 323.357 377.724 321.66C427.348 301.237 476.364 278.334 526.027 258.106C526.804 260.328 526.601 279.874 526.896 284.322C532.684 287.524 561.101 303.664 564.941 304.362C583.625 296.051 602.46 288.083 621.435 280.462L621.03 217.328Z"/>'
			. '<path d="M138.255 531.762C139.154 539.864 138.545 559.703 138.562 568.691L139.002 632.44L388.274 767.816L449.574 801.515C453.035 803.422 471.639 813.44 473.382 814.979C473.68 816.588 473.791 817.409 473.927 819.04C473.942 828.176 473.699 839.291 474.071 848.233C474.93 853.511 474.617 869.811 474.643 876.202L475.019 927.005C475.163 955.105 476.136 983.199 476.338 1011.3C476.368 1015.53 476.527 1019.77 476.398 1024L476.119 1024C473.145 1021.75 466.38 1018.27 462.721 1016.24C455.203 1012.06 447.727 1007.82 440.292 1003.5C407.521 984.503 374.623 965.725 341.599 947.169L0 755.697L0 596.128L0 594.004C2.9198 592.359 11.0489 588.995 14.4776 587.467L44.0902 574.207L138.255 531.762Z"/>'
			. '<path d="M348.066 422.12C351.299 418.815 361.551 329.579 365.236 327.051C385.868 336.467 407.054 347.088 427.507 357.016C439.921 363.041 458.919 371.54 470.059 378.499C468.865 382.212 469.789 414.939 469.88 421.294L470.526 478.824L473.021 720.111L473.93 786.353C474.041 796.53 474.405 808.977 473.927 819.04C473.791 817.409 473.68 816.588 473.382 814.979C471.639 813.44 453.035 803.422 449.574 801.515L388.274 767.816L139.002 632.44L138.562 568.691C138.545 559.703 139.154 539.864 138.255 531.762C137.795 527.808 137.759 519.157 138.413 515.215C138.663 513.706 139.532 513.453 140.595 512.883C147.158 509.537 155.787 505.986 162.682 502.95L206.697 483.724L348.066 422.12Z"/>'
			. '<path d="M621.03 217.328L814.78 134.57L889.942 102.514C901.166 97.7777 913.208 92.3787 924.513 88.0539C934.115 91.9491 947.1 98.9319 956.5 103.493C979.104 114.459 1001.42 126.327 1024 137.357L1024 141.124L666.929 294.274L533.86 351.072L493.135 368.516C485.585 371.744 477.358 374.944 470.059 378.499C458.919 371.54 439.921 363.041 427.507 357.016C407.054 347.088 385.868 336.467 365.236 327.051C369.266 325.178 373.602 323.357 377.724 321.66C427.348 301.237 476.364 278.334 526.027 258.106C526.804 260.328 526.601 279.874 526.896 284.322C532.684 287.524 561.101 303.664 564.941 304.362C583.625 296.051 602.46 288.083 621.435 280.462L621.03 217.328Z"/>'
			. '<path d="M640.646 0L642.908 0C659.742 10.4548 679.06 19.2473 695.63 29.8718C694.835 40.5122 695.711 56.0971 695.509 67.1922C694.955 97.6262 696.341 129.601 695.476 159.913C670.965 171.322 645.142 180.86 620.572 192.061C621.32 198.843 621.042 210.274 621.03 217.328L621.435 280.462C602.46 288.083 583.625 296.051 564.941 304.362C561.101 303.664 532.684 287.524 526.896 284.322C526.601 279.874 526.804 260.328 526.027 258.106C527.182 256.105 526.431 235.054 526.14 231.424C505.164 239.888 483.743 250.619 463.193 258.728C459.951 258.356 415.736 232.625 409.512 229.101C408.072 195.469 409.685 161.13 408.839 127.42C408.617 118.58 408.425 108.483 409.023 99.7175C415.342 96.1636 426.3 91.7252 433.231 88.7271L474.881 70.88L591.292 21.4449L620.228 9.27463C626.676 6.57539 634.645 3.4077 640.646 0Z"/>'
			. '</g></svg>';

		return 'data:image/svg+xml;base64,' . base64_encode( $svg );
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
