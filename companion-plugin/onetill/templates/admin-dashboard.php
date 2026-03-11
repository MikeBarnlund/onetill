<?php
/**
 * Admin dashboard page template.
 *
 * @package OneTill
 * @var array $devices     Connected device records.
 * @var bool  $https_ok    Whether the site is using HTTPS.
 * @var array $change_logs Change log stats { last_hour, last_24h }.
 */

defined( 'ABSPATH' ) || exit;
?>

<div class="wrap onetill-dashboard">
	<h1><?php esc_html_e( 'OneTill', 'onetill' ); ?></h1>

	<?php if ( ! $https_ok ) : ?>
		<div class="notice notice-warning">
			<p>
				<?php esc_html_e( 'Your site is not using HTTPS. Device pairing requires a secure connection. Please enable SSL/HTTPS.', 'onetill' ); ?>
			</p>
		</div>
	<?php endif; ?>

	<div class="onetill-grid">
		<!-- Connected Devices -->
		<div class="onetill-card">
			<div class="onetill-card-header">
				<h2><?php esc_html_e( 'Connected Devices', 'onetill' ); ?></h2>
				<button type="button" class="button button-primary" id="onetill-pair-btn">
					<?php esc_html_e( 'Pair New Device', 'onetill' ); ?>
				</button>
			</div>

			<?php if ( empty( $devices ) ) : ?>
				<div class="onetill-empty-state">
					<span class="dashicons dashicons-smartphone"></span>
					<p><?php esc_html_e( 'No devices connected yet.', 'onetill' ); ?></p>
					<p class="description"><?php esc_html_e( 'Click "Pair New Device" to connect your first S700/S710 terminal.', 'onetill' ); ?></p>
				</div>
			<?php else : ?>
				<table class="widefat onetill-devices-table">
					<thead>
						<tr>
							<th><?php esc_html_e( 'Device', 'onetill' ); ?></th>
							<th><?php esc_html_e( 'Device ID', 'onetill' ); ?></th>
							<th><?php esc_html_e( 'Last Seen', 'onetill' ); ?></th>
							<th><?php esc_html_e( 'Last Sync', 'onetill' ); ?></th>
							<th><?php esc_html_e( 'Status', 'onetill' ); ?></th>
							<th><?php esc_html_e( 'Actions', 'onetill' ); ?></th>
						</tr>
					</thead>
					<tbody>
						<?php foreach ( $devices as $device ) : ?>
							<tr data-device-id="<?php echo esc_attr( $device['id'] ); ?>">
								<td>
									<strong><?php echo esc_html( $device['name'] ); ?></strong>
									<?php if ( ! empty( $device['app_version'] ) ) : ?>
										<br><span class="description">v<?php echo esc_html( $device['app_version'] ); ?></span>
									<?php endif; ?>
								</td>
								<td>
									<code><?php echo esc_html( substr( $device['id'], 0, 12 ) ); ?>&hellip;</code>
								</td>
								<td>
									<?php
									if ( $device['last_seen'] ) {
										echo esc_html( human_time_diff( strtotime( $device['last_seen'] ), time() ) . ' ' . __( 'ago', 'onetill' ) );
									} else {
										echo '<span class="description">' . esc_html__( 'Never', 'onetill' ) . '</span>';
									}
									?>
								</td>
								<td>
									<?php
									if ( $device['last_sync'] ) {
										echo esc_html( human_time_diff( strtotime( $device['last_sync'] ), time() ) . ' ' . __( 'ago', 'onetill' ) );
									} else {
										echo '<span class="description">' . esc_html__( 'Never', 'onetill' ) . '</span>';
									}
									?>
								</td>
								<td>
									<?php if ( 'active' === $device['status'] ) : ?>
										<span class="onetill-status onetill-status-active"><?php esc_html_e( 'Active', 'onetill' ); ?></span>
									<?php else : ?>
										<span class="onetill-status onetill-status-disabled"><?php esc_html_e( 'Disabled', 'onetill' ); ?></span>
									<?php endif; ?>
								</td>
								<td>
									<button type="button"
										class="button button-link-delete onetill-disconnect-btn"
										data-device-id="<?php echo esc_attr( $device['id'] ); ?>"
										data-device-name="<?php echo esc_attr( $device['name'] ); ?>">
										<?php esc_html_e( 'Disconnect', 'onetill' ); ?>
									</button>
								</td>
							</tr>
						<?php endforeach; ?>
					</tbody>
				</table>
			<?php endif; ?>
		</div>

		<!-- Sync Status -->
		<div class="onetill-card onetill-card-narrow">
			<h2><?php esc_html_e( 'Sync Status', 'onetill' ); ?></h2>
			<div class="onetill-stats">
				<div class="onetill-stat">
					<span class="onetill-stat-number"><?php echo esc_html( $change_logs['last_hour'] ); ?></span>
					<span class="onetill-stat-label"><?php esc_html_e( 'changes in the last hour', 'onetill' ); ?></span>
				</div>
				<div class="onetill-stat">
					<span class="onetill-stat-number"><?php echo esc_html( $change_logs['last_24h'] ); ?></span>
					<span class="onetill-stat-label"><?php esc_html_e( 'changes in the last 24 hours', 'onetill' ); ?></span>
				</div>
			</div>
		</div>

		<!-- Quick Links -->
		<div class="onetill-card onetill-card-narrow">
			<h2><?php esc_html_e( 'Quick Links', 'onetill' ); ?></h2>
			<ul class="onetill-links">
				<li>
					<span class="dashicons dashicons-book"></span>
					<a href="https://onetill.app/docs" target="_blank" rel="noopener">
						<?php esc_html_e( 'Documentation', 'onetill' ); ?>
					</a>
				</li>
				<li>
					<span class="dashicons dashicons-email"></span>
					<a href="mailto:support@onetill.app">
						<?php esc_html_e( 'Contact Support', 'onetill' ); ?>
					</a>
				</li>
			</ul>
		</div>
	</div>

	<!-- Pairing Modal -->
	<div class="onetill-modal-overlay" id="onetill-pairing-modal" style="display: none;">
		<div class="onetill-modal">
			<button type="button" class="onetill-modal-close" id="onetill-modal-close">&times;</button>

			<!-- State: Loading -->
			<div class="onetill-pairing-state" id="onetill-state-loading">
				<h2><?php esc_html_e( 'Generating QR Code...', 'onetill' ); ?></h2>
				<div class="onetill-spinner"></div>
			</div>

			<!-- State: QR Code Ready -->
			<div class="onetill-pairing-state" id="onetill-state-qr" style="display: none;">
				<h2><?php esc_html_e( 'Scan with your S700/S710', 'onetill' ); ?></h2>
				<div class="onetill-qr-container" id="onetill-qr-container"></div>
				<p class="onetill-pairing-status">
					<span class="onetill-spinner-small"></span>
					<?php esc_html_e( 'Waiting for device to scan QR code...', 'onetill' ); ?>
				</p>
				<p class="description onetill-expires" id="onetill-expires">
					<?php esc_html_e( 'Expires in', 'onetill' ); ?>
					<span id="onetill-countdown"></span>
				</p>
			</div>

			<!-- State: Success -->
			<div class="onetill-pairing-state" id="onetill-state-success" style="display: none;">
				<span class="dashicons dashicons-yes-alt onetill-success-icon"></span>
				<h2><?php esc_html_e( 'Device Connected!', 'onetill' ); ?></h2>
				<p id="onetill-paired-device-name"></p>
				<button type="button" class="button button-primary" id="onetill-done-btn">
					<?php esc_html_e( 'Done', 'onetill' ); ?>
				</button>
			</div>

			<!-- State: Expired -->
			<div class="onetill-pairing-state" id="onetill-state-expired" style="display: none;">
				<span class="dashicons dashicons-warning onetill-expired-icon"></span>
				<h2><?php esc_html_e( 'QR Code Expired', 'onetill' ); ?></h2>
				<p><?php esc_html_e( 'The QR code has expired. Please generate a new one.', 'onetill' ); ?></p>
				<button type="button" class="button button-primary" id="onetill-retry-btn">
					<?php esc_html_e( 'Generate New QR Code', 'onetill' ); ?>
				</button>
			</div>

			<!-- State: Error -->
			<div class="onetill-pairing-state" id="onetill-state-error" style="display: none;">
				<span class="dashicons dashicons-dismiss onetill-error-icon"></span>
				<h2><?php esc_html_e( 'Pairing Failed', 'onetill' ); ?></h2>
				<p id="onetill-error-message"></p>
				<button type="button" class="button button-primary" id="onetill-error-retry-btn">
					<?php esc_html_e( 'Try Again', 'onetill' ); ?>
				</button>
			</div>
		</div>
	</div>
</div>
