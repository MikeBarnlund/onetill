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
	</div>
</div>
