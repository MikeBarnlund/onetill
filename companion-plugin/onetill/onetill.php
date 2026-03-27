<?php
/**
 * Plugin Name: OneTill — POS Connector for WooCommerce
 * Description: Connects your WooCommerce store to the OneTill POS app running on Stripe S700/S710 terminals. Enables real-time product sync, inventory management, and order creation from your point of sale.
 * Version: 1.1.0
 * Requires at least: 6.0
 * Tested up to: 6.9
 * Requires PHP: 7.4
 * Requires Plugins: woocommerce
 * WC requires at least: 8.0
 * WC tested up to: 9.6
 * Author: OneTill
 * Author URI: https://onetill.app
 * Developer: OneTill
 * Developer URI: https://onetill.app
 * License: GNU General Public License v3.0
 * License URI: https://www.gnu.org/licenses/gpl-3.0.html
 * Text Domain: onetill
 * Domain Path: /languages
 *
 * @package OneTill
 */

defined( 'ABSPATH' ) || exit;

define( 'ONETILL_VERSION', '1.1.0' );
define( 'ONETILL_PLUGIN_FILE', __FILE__ );
define( 'ONETILL_PLUGIN_DIR', plugin_dir_path( __FILE__ ) );
define( 'ONETILL_PLUGIN_URL', plugin_dir_url( __FILE__ ) );
define( 'ONETILL_PLUGIN_BASENAME', plugin_basename( __FILE__ ) );

/**
 * HPOS compatibility declaration (required for WooCommerce 8.0+).
 */
add_action( 'before_woocommerce_init', function () {
	if ( class_exists( \Automattic\WooCommerce\Utilities\FeaturesUtil::class ) ) {
		\Automattic\WooCommerce\Utilities\FeaturesUtil::declare_compatibility(
			'custom_order_tables',
			__FILE__,
			true
		);
		\Automattic\WooCommerce\Utilities\FeaturesUtil::declare_compatibility(
			'cart_checkout_blocks',
			__FILE__,
			true
		);
	}
} );

/**
 * Autoloader for OneTill classes.
 *
 * Maps OneTill\ namespace to includes/ directory using PSR-4 conventions,
 * with WordPress file naming (class-*.php).
 */
spl_autoload_register( function ( $class ) {
	$prefix = 'OneTill\\';
	if ( 0 !== strpos( $class, $prefix ) ) {
		return;
	}

	$relative_class = substr( $class, strlen( $prefix ) );
	$file           = ONETILL_PLUGIN_DIR . 'includes/class-' . strtolower( str_replace( '_', '-', $relative_class ) ) . '.php';

	if ( file_exists( $file ) ) {
		require_once $file;
	}
} );

/**
 * Check that WooCommerce is active before initializing.
 *
 * @return bool
 */
function onetill_check_woocommerce() {
	if ( ! class_exists( 'WooCommerce' ) ) {
		add_action( 'admin_notices', function () {
			?>
			<div class="notice notice-error">
				<p>
					<?php
					printf(
						/* translators: %s: WooCommerce plugin name. */
						esc_html__( '%1$s requires %2$s to be installed and active.', 'onetill' ),
						'<strong>OneTill</strong>',
						'<strong>WooCommerce</strong>'
					);
					?>
				</p>
			</div>
			<?php
		} );
		return false;
	}

	if ( version_compare( WC_VERSION, '8.0', '<' ) ) {
		add_action( 'admin_notices', function () {
			?>
			<div class="notice notice-warning">
				<p>
					<?php
					printf(
						/* translators: %s: minimum WooCommerce version. */
						esc_html__( '%1$s recommends %2$s version %3$s or higher. Some features may not work correctly.', 'onetill' ),
						'<strong>OneTill</strong>',
						'<strong>WooCommerce</strong>',
						'8.0'
					);
					?>
				</p>
			</div>
			<?php
		} );
	}

	return true;
}

/**
 * Plugin activation hook.
 */
function onetill_activate() {
	if ( ! class_exists( 'WooCommerce' ) ) {
		deactivate_plugins( ONETILL_PLUGIN_BASENAME );
		wp_die(
			esc_html__( 'OneTill requires WooCommerce to be installed and active.', 'onetill' ),
			'Plugin Activation Error',
			array( 'back_link' => true )
		);
	}

	onetill_create_tables();
	onetill_schedule_cron_jobs();

	flush_rewrite_rules();
}

/**
 * Plugin deactivation hook.
 */
function onetill_deactivate() {
	onetill_unschedule_cron_jobs();
}

register_activation_hook( __FILE__, 'onetill_activate' );
register_deactivation_hook( __FILE__, 'onetill_deactivate' );

/**
 * Create custom database tables.
 */
function onetill_create_tables() {
	global $wpdb;

	$charset_collate = $wpdb->get_charset_collate();

	$tables = array();

	// Devices table.
	$tables[] = "CREATE TABLE {$wpdb->prefix}onetill_devices (
		id VARCHAR(50) NOT NULL,
		name VARCHAR(100) NOT NULL,
		api_key_id BIGINT UNSIGNED NOT NULL,
		app_version VARCHAR(20) DEFAULT NULL,
		last_seen DATETIME DEFAULT NULL,
		last_sync DATETIME DEFAULT NULL,
		paired_at DATETIME NOT NULL,
		status VARCHAR(20) NOT NULL DEFAULT 'active',
		PRIMARY KEY  (id),
		KEY idx_status (status)
	) $charset_collate;";

	// Pairing tokens table.
	$tables[] = "CREATE TABLE {$wpdb->prefix}onetill_pairing_tokens (
		token VARCHAR(64) NOT NULL,
		nonce VARCHAR(16) NOT NULL,
		created_at DATETIME NOT NULL,
		expires_at DATETIME NOT NULL,
		status VARCHAR(20) NOT NULL DEFAULT 'pending',
		device_id VARCHAR(50) DEFAULT NULL,
		ip_address VARCHAR(45) DEFAULT NULL,
		PRIMARY KEY  (token),
		KEY idx_status_expires (status, expires_at)
	) $charset_collate;";

	// Idempotency table.
	$tables[] = "CREATE TABLE {$wpdb->prefix}onetill_idempotency (
		idempotency_key VARCHAR(100) NOT NULL,
		order_id BIGINT UNSIGNED DEFAULT NULL,
		response_body LONGTEXT DEFAULT NULL,
		created_at DATETIME NOT NULL,
		PRIMARY KEY  (idempotency_key),
		KEY idx_created (created_at)
	) $charset_collate;";

	// Deleted products tracking table.
	$tables[] = "CREATE TABLE {$wpdb->prefix}onetill_deleted_products (
		product_id BIGINT UNSIGNED NOT NULL,
		deleted_at DATETIME NOT NULL,
		PRIMARY KEY  (product_id),
		KEY idx_deleted (deleted_at)
	) $charset_collate;";

	// Users table (staff PINs).
	$tables[] = "CREATE TABLE {$wpdb->prefix}onetill_users (
		id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
		first_name VARCHAR(100) NOT NULL,
		last_name VARCHAR(100) NOT NULL,
		pin VARCHAR(255) NOT NULL,
		pin_sha256 VARCHAR(64) DEFAULT NULL,
		created_at DATETIME NOT NULL,
		updated_at DATETIME NOT NULL,
		PRIMARY KEY  (id)
	) $charset_collate;";

	// Change log table.
	$tables[] = "CREATE TABLE {$wpdb->prefix}onetill_change_log (
		id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
		event_type VARCHAR(50) NOT NULL,
		resource_id BIGINT UNSIGNED NOT NULL,
		timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
		payload TEXT DEFAULT NULL,
		PRIMARY KEY  (id),
		KEY idx_timestamp (timestamp),
		KEY idx_event_type (event_type)
	) $charset_collate;";

	require_once ABSPATH . 'wp-admin/includes/upgrade.php';

	foreach ( $tables as $sql ) {
		dbDelta( $sql );
	}

	update_option( 'onetill_db_version', ONETILL_VERSION );
}

/**
 * Schedule WP-Cron jobs.
 */
function onetill_schedule_cron_jobs() {
	if ( ! wp_next_scheduled( 'onetill_cleanup_change_log' ) ) {
		wp_schedule_event( time(), 'daily', 'onetill_cleanup_change_log' );
	}

	if ( ! wp_next_scheduled( 'onetill_cleanup_expired_tokens' ) ) {
		wp_schedule_event( time(), 'hourly', 'onetill_cleanup_expired_tokens' );
	}

	if ( ! wp_next_scheduled( 'onetill_cleanup_idempotency' ) ) {
		wp_schedule_event( time(), 'daily', 'onetill_cleanup_idempotency' );
	}
}

/**
 * Unschedule WP-Cron jobs.
 */
function onetill_unschedule_cron_jobs() {
	wp_clear_scheduled_hook( 'onetill_cleanup_change_log' );
	wp_clear_scheduled_hook( 'onetill_cleanup_expired_tokens' );
	wp_clear_scheduled_hook( 'onetill_cleanup_idempotency' );
}

/**
 * Run database migrations if the DB version is behind the code version.
 */
function onetill_maybe_upgrade() {
	$db_version = get_option( 'onetill_db_version', '0' );

	if ( version_compare( $db_version, ONETILL_VERSION, '<' ) ) {
		onetill_create_tables();
	}
}

/**
 * Initialize the plugin.
 */
function onetill_init() {
	if ( ! onetill_check_woocommerce() ) {
		return;
	}

	onetill_maybe_upgrade();

	$plugin = new OneTill\OneTill();
	$plugin->init();
}

add_action( 'plugins_loaded', 'onetill_init' );

/**
 * Register the OneTill POS Receipt email with WooCommerce.
 *
 * @param array $email_classes Registered email classes.
 * @return array
 */
add_filter( 'woocommerce_email_classes', function ( $email_classes ) {
	require_once ONETILL_PLUGIN_DIR . 'includes/emails/class-wc-onetill-email-pos-receipt.php';
	$email_classes['WC_OneTill_Email_POS_Receipt'] = new WC_OneTill_Email_POS_Receipt();
	return $email_classes;
} );
