<?php
/**
 * OneTill POS Receipt Email (HTML).
 *
 * @package OneTill
 * @var WC_Order $order
 * @var string   $email_heading
 * @var string   $additional_content
 * @var bool     $sent_to_admin
 * @var bool     $plain_text
 * @var WC_Email $email
 */

defined( 'ABSPATH' ) || exit;

do_action( 'woocommerce_email_header', $email_heading, $email ); ?>

<p><?php esc_html_e( 'Thanks for your purchase! Here are the details of your transaction.', 'onetill' ); ?></p>

<?php
/*
 * Order items table — uses WooCommerce's built-in template.
 */
do_action( 'woocommerce_email_order_details', $order, $sent_to_admin, $plain_text, $email );

<?php
/*
 * Store address.
 */
$store_name    = get_bloginfo( 'name' );
$store_address = WC()->countries->get_base_address();
$store_city    = WC()->countries->get_base_city();
$store_state   = WC()->countries->get_base_state();
$store_postcode = WC()->countries->get_base_postcode();
$store_country = WC()->countries->get_base_country();

$address_parts = array_filter( array( $store_address, $store_city, $store_state, $store_postcode ) );
?>

<?php if ( ! empty( $address_parts ) ) : ?>
<table cellspacing="0" cellpadding="6" style="width: 100%; margin-bottom: 20px;">
	<tr>
		<td style="text-align: left; padding: 12px 0; color: #636363;">
			<strong><?php echo esc_html( $store_name ); ?></strong><br>
			<?php echo esc_html( implode( ', ', $address_parts ) ); ?>
		</td>
	</tr>
</table>
<?php endif; ?>

<?php
/*
 * Additional content — merchant-customizable block.
 */
if ( $additional_content ) {
	echo wp_kses_post( wpautop( wptexturize( $additional_content ) ) );
}
?>

<?php
$powered_by_filter = function () {
	return '<a href="https://onetill.app?utm_source=receipt&utm_medium=email&utm_campaign=powered_by" target="_blank" rel="noopener" style="font-size: 11px; color: #b0b0b0; text-decoration: none;">Powered by OneTill</a>';
};
add_filter( 'woocommerce_email_footer_text', $powered_by_filter );
do_action( 'woocommerce_email_footer', $email );
remove_filter( 'woocommerce_email_footer_text', $powered_by_filter );
