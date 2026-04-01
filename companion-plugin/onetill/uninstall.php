<?php
/**
 * OneTill uninstall script.
 *
 * Fired when the plugin is deleted via WP Admin. Performs full cleanup:
 * 1. Drop all custom database tables
 * 2. Delete all _onetill_* post meta
 * 3. Delete all onetill_* options
 * 4. Revoke WooCommerce API keys created by OneTill
 * 5. Remove scheduled cron jobs
 *
 * @package OneTill
 */

defined( 'WP_UNINSTALL_PLUGIN' ) || exit;

global $wpdb;

// 1. Revoke WooCommerce API keys created by OneTill.
// phpcs:ignore WordPress.DB.DirectDatabaseQuery.DirectQuery, WordPress.DB.DirectDatabaseQuery.NoCaching -- uninstall cleanup, custom table.
$onetill_device_key_ids = $wpdb->get_col(
	"SELECT api_key_id FROM {$wpdb->prefix}onetill_devices"
);

if ( ! empty( $onetill_device_key_ids ) ) {
	foreach ( $onetill_device_key_ids as $onetill_key_id ) {
		// phpcs:ignore WordPress.DB.DirectDatabaseQuery.DirectQuery, WordPress.DB.DirectDatabaseQuery.NoCaching -- uninstall cleanup, WooCommerce table.
		$wpdb->delete(
			$wpdb->prefix . 'woocommerce_api_keys',
			array( 'key_id' => absint( $onetill_key_id ) ),
			array( '%d' )
		);
	}
}

// 2. Drop all custom tables.
$tables = array(
	'onetill_devices',
	'onetill_pairing_tokens',
	'onetill_idempotency',
	'onetill_deleted_products',
	'onetill_change_log',
	'onetill_users',
);

foreach ( $tables as $table ) {
	$wpdb->query( "DROP TABLE IF EXISTS {$wpdb->prefix}{$table}" ); // phpcs:ignore WordPress.DB.PreparedSQL.InterpolatedNotPrepared
}

// 3. Delete all _onetill_* post meta (products, legacy order meta).
$wpdb->query( "DELETE FROM {$wpdb->postmeta} WHERE meta_key LIKE '_onetill_%'" );

// 3b. Delete _onetill_* order meta from HPOS tables (if they exist).
$hpos_meta_table = $wpdb->prefix . 'wc_orders_meta';
if ( $wpdb->get_var( $wpdb->prepare( 'SHOW TABLES LIKE %s', $hpos_meta_table ) ) === $hpos_meta_table ) {
	$wpdb->query( "DELETE FROM {$hpos_meta_table} WHERE meta_key LIKE '_onetill_%'" ); // phpcs:ignore WordPress.DB.PreparedSQL.InterpolatedNotPrepared
}

// 4. Delete all onetill_* options.
$wpdb->query( "DELETE FROM {$wpdb->options} WHERE option_name LIKE 'onetill_%'" );

// 5. Delete onetill transients (rate limiters, pairing).
$wpdb->query( "DELETE FROM {$wpdb->options} WHERE option_name LIKE '_transient_onetill_%'" );
$wpdb->query( "DELETE FROM {$wpdb->options} WHERE option_name LIKE '_transient_timeout_onetill_%'" );

// 6. Clear scheduled cron hooks.
wp_clear_scheduled_hook( 'onetill_cleanup_change_log' );
wp_clear_scheduled_hook( 'onetill_cleanup_expired_tokens' );
wp_clear_scheduled_hook( 'onetill_cleanup_idempotency' );
