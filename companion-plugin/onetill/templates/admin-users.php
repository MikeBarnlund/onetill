<?php
/**
 * Admin users page template.
 *
 * @package OneTill
 * @var array $users OneTill user records.
 */

defined( 'ABSPATH' ) || exit;
?>

<div class="wrap onetill-users-page">
	<h1><?php esc_html_e( 'OneTill Users', 'onetill' ); ?></h1>

	<div class="onetill-users-layout">
		<!-- User List -->
		<div class="onetill-card">
			<div class="onetill-card-header">
				<h2><?php esc_html_e( 'Staff', 'onetill' ); ?></h2>
				<button type="button" class="button button-primary" id="onetill-add-user-btn">
					<?php esc_html_e( 'Add User', 'onetill' ); ?>
				</button>
			</div>

			<?php if ( empty( $users ) ) : ?>
				<div class="onetill-empty-state" id="onetill-users-empty">
					<span class="dashicons dashicons-admin-users"></span>
					<p><?php esc_html_e( 'No users added yet.', 'onetill' ); ?></p>
					<p class="description"><?php esc_html_e( 'Add staff members who will use the POS terminal.', 'onetill' ); ?></p>
				</div>
			<?php endif; ?>

			<table class="widefat onetill-users-table" <?php echo empty( $users ) ? 'style="display: none;"' : ''; ?>>
				<thead>
					<tr>
						<th><?php esc_html_e( 'Name', 'onetill' ); ?></th>
						<th><?php esc_html_e( 'PIN', 'onetill' ); ?></th>
						<th><?php esc_html_e( 'Added', 'onetill' ); ?></th>
						<th><?php esc_html_e( 'Actions', 'onetill' ); ?></th>
					</tr>
				</thead>
				<tbody id="onetill-users-tbody">
					<?php foreach ( $users as $user ) : ?>
						<tr data-user-id="<?php echo esc_attr( $user['id'] ); ?>">
							<td>
								<strong>
									<span class="onetill-user-name">
										<?php echo esc_html( $user['first_name'] . ' ' . $user['last_name'] ); ?>
									</span>
								</strong>
							</td>
							<td><code>****</code></td>
							<td>
								<?php echo esc_html( date_i18n( get_option( 'date_format' ), strtotime( $user['created_at'] ) ) ); ?>
							</td>
							<td>
								<button type="button"
									class="button button-small onetill-edit-user-btn"
									data-user-id="<?php echo esc_attr( $user['id'] ); ?>"
									data-first-name="<?php echo esc_attr( $user['first_name'] ); ?>"
									data-last-name="<?php echo esc_attr( $user['last_name'] ); ?>">
									<?php esc_html_e( 'Edit', 'onetill' ); ?>
								</button>
								<button type="button"
									class="button button-small button-link-delete onetill-delete-user-btn"
									data-user-id="<?php echo esc_attr( $user['id'] ); ?>"
									data-user-name="<?php echo esc_attr( $user['first_name'] . ' ' . $user['last_name'] ); ?>">
									<?php esc_html_e( 'Delete', 'onetill' ); ?>
								</button>
							</td>
						</tr>
					<?php endforeach; ?>
				</tbody>
			</table>
		</div>
	</div>

	<!-- Add/Edit User Modal -->
	<div class="onetill-modal-overlay" id="onetill-user-modal" style="display: none;">
		<div class="onetill-modal onetill-user-modal">
			<button type="button" class="onetill-modal-close" id="onetill-user-modal-close">&times;</button>

			<h2 id="onetill-user-modal-title"><?php esc_html_e( 'Add User', 'onetill' ); ?></h2>

			<form id="onetill-user-form">
				<input type="hidden" id="onetill-user-id" value="">

				<div class="onetill-form-field">
					<label for="onetill-first-name"><?php esc_html_e( 'First Name', 'onetill' ); ?></label>
					<input type="text" id="onetill-first-name" maxlength="100" required>
				</div>

				<div class="onetill-form-field">
					<label for="onetill-last-name"><?php esc_html_e( 'Last Name', 'onetill' ); ?></label>
					<input type="text" id="onetill-last-name" maxlength="100" required>
				</div>

				<div class="onetill-form-field">
					<label for="onetill-pin">
						<?php esc_html_e( 'PIN', 'onetill' ); ?>
						<span class="description" id="onetill-pin-hint"></span>
					</label>
					<input type="password" id="onetill-pin" maxlength="4" pattern="\d{4}"
						inputmode="numeric" autocomplete="off"
						placeholder="<?php esc_attr_e( '4 digits', 'onetill' ); ?>">
				</div>

				<div class="onetill-form-error" id="onetill-user-form-error" style="display: none;"></div>

				<div class="onetill-form-actions">
					<button type="button" class="button" id="onetill-user-cancel-btn">
						<?php esc_html_e( 'Cancel', 'onetill' ); ?>
					</button>
					<button type="submit" class="button button-primary" id="onetill-user-save-btn">
						<?php esc_html_e( 'Save', 'onetill' ); ?>
					</button>
				</div>
			</form>
		</div>
	</div>
</div>
