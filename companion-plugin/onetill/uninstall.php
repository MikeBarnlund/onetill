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
$device_key_ids = $wpdb->get_col(
	"SELECT api_key_id FROM {$wpdb->prefix}onetill_devices"
);

if ( ! empty( $device_key_ids ) ) {
	$placeholders = implode( ',', array_fill( 0, count( $device_key_ids ), '%d' ) );
	$wpdb->query(
		$wpdb->prepare(
			"DELETE FROM {$wpdb->prefix}woocommerce_api_keys WHERE key_id IN ($placeholders)", // phpcs:ignore WordPress.DB.PreparedSQL.InterpolatedNotPrepared
			...$device_key_ids
		)
	);
}

// 2. Drop all custom tables.
$tables = array(
	'onetill_devices',
	'onetill_pairing_tokens',
	'onetill_idempotency',
	'onetill_deleted_products',
	'onetill_change_log',
);

foreach ( $tables as $table ) {
	$wpdb->query( "DROP TABLE IF EXISTS {$wpdb->prefix}{$table}" ); // phpcs:ignore WordPress.DB.PreparedSQL.InterpolatedNotPrepared
}

// 3. Delete all _onetill_* post meta.
$wpdb->query( "DELETE FROM {$wpdb->postmeta} WHERE meta_key LIKE '_onetill_%'" );

// 4. Delete all onetill_* options.
$wpdb->query( "DELETE FROM {$wpdb->options} WHERE option_name LIKE 'onetill_%'" );

// 5. Clear scheduled cron hooks.
wp_clear_scheduled_hook( 'onetill_cleanup_change_log' );
wp_clear_scheduled_hook( 'onetill_cleanup_expired_tokens' );
wp_clear_scheduled_hook( 'onetill_cleanup_idempotency' );
