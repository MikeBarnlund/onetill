<?php
/**
 * Admin settings page template.
 *
 * @package OneTill
 */

defined( 'ABSPATH' ) || exit;

$stripe_key         = get_option( 'onetill_stripe_secret_key', '' );
$stripe_key_display = '';
if ( ! empty( $stripe_key ) ) {
	$stripe_key_display = str_repeat( '*', 12 ) . substr( $stripe_key, -4 );
}
?>

<div class="wrap onetill-dashboard">
	<h1><?php esc_html_e( 'OneTill Settings', 'onetill' ); ?></h1>

	<?php if ( isset( $_GET['stripe_saved'] ) ) : // phpcs:ignore WordPress.Security.NonceVerification.Recommended ?>
		<div class="notice notice-success is-dismissible">
			<p><?php esc_html_e( 'Stripe key saved.', 'onetill' ); ?></p>
		</div>
	<?php endif; ?>

	<div class="onetill-grid">
		<!-- Stripe Settings -->
		<div class="onetill-card">
			<h2><?php esc_html_e( 'Stripe Terminal', 'onetill' ); ?></h2>
			<form method="post" action="<?php echo esc_url( admin_url( 'admin-post.php' ) ); ?>">
				<?php wp_nonce_field( 'onetill_save_stripe_key', 'onetill_stripe_nonce' ); ?>
				<input type="hidden" name="action" value="onetill_save_stripe_key" />
				<table class="form-table" role="presentation">
					<tr>
						<th scope="row">
							<label for="onetill_stripe_secret_key"><?php esc_html_e( 'Secret Key', 'onetill' ); ?></label>
						</th>
						<td>
							<input
								type="password"
								id="onetill_stripe_secret_key"
								name="onetill_stripe_secret_key"
								value="<?php echo esc_attr( $stripe_key ); ?>"
								class="regular-text"
								placeholder="sk_test_..."
								autocomplete="off"
							/>
							<?php if ( ! empty( $stripe_key_display ) ) : ?>
								<p class="description">
									<?php
									printf(
										/* translators: %s: masked key ending */
										esc_html__( 'Current key: %s', 'onetill' ),
										'<code>' . esc_html( $stripe_key_display ) . '</code>'
									);
									?>
								</p>
							<?php else : ?>
								<p class="description">
									<?php esc_html_e( 'Enter your Stripe secret key (test or live) to enable card payments on terminals.', 'onetill' ); ?>
								</p>
							<?php endif; ?>
						</td>
					</tr>
				</table>
				<?php submit_button( __( 'Save Stripe Key', 'onetill' ), 'primary' ); ?>
			</form>
		</div>

		<?php if ( isset( $_GET['supabase_saved'] ) ) : // phpcs:ignore WordPress.Security.NonceVerification.Recommended ?>
			<div class="notice notice-success is-dismissible">
				<p><?php esc_html_e( 'Supabase settings saved.', 'onetill' ); ?></p>
			</div>
		<?php endif; ?>

		<!-- Supabase Settings -->
		<div class="onetill-card">
			<h2><?php esc_html_e( 'Supabase (Subscription Validation)', 'onetill' ); ?></h2>
			<form method="post" action="<?php echo esc_url( admin_url( 'admin-post.php' ) ); ?>">
				<?php wp_nonce_field( 'onetill_save_supabase', 'onetill_supabase_nonce' ); ?>
				<input type="hidden" name="action" value="onetill_save_supabase" />
				<table class="form-table" role="presentation">
					<tr>
						<th scope="row">
							<label for="onetill_supabase_url"><?php esc_html_e( 'Project URL', 'onetill' ); ?></label>
						</th>
						<td>
							<input
								type="url"
								id="onetill_supabase_url"
								name="onetill_supabase_url"
								value="<?php echo esc_attr( get_option( 'onetill_supabase_url', '' ) ); ?>"
								class="regular-text"
								placeholder="https://xxxx.supabase.co"
							/>
						</td>
					</tr>
					<tr>
						<th scope="row">
							<label for="onetill_supabase_service_key"><?php esc_html_e( 'Service Role Key', 'onetill' ); ?></label>
						</th>
						<td>
							<input
								type="password"
								id="onetill_supabase_service_key"
								name="onetill_supabase_service_key"
								value="<?php echo esc_attr( get_option( 'onetill_supabase_service_key', '' ) ); ?>"
								class="regular-text"
								placeholder="eyJhbGciOiJ..."
								autocomplete="off"
							/>
						</td>
					</tr>
				</table>
				<?php submit_button( __( 'Save Supabase Settings', 'onetill' ), 'primary' ); ?>
			</form>
		</div>
	</div>
</div>
