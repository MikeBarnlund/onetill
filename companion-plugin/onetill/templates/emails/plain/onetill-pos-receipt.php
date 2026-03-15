<?php
/**
 * OneTill POS Receipt Email (Plain Text).
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

echo "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=\n";
echo esc_html( wp_strip_all_tags( $email_heading ) );
echo "\n=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=\n\n";

echo esc_html__( 'Thanks for your purchase! Here are the details of your transaction.', 'onetill' ) . "\n\n";

printf(
	/* translators: %s: order number */
	esc_html__( 'Order #%s', 'onetill' ),
	esc_html( $order->get_order_number() )
);
echo ' (' . esc_html( wc_format_datetime( $order->get_date_created() ) ) . ")\n\n";

/*
 * Order items.
 */
do_action( 'woocommerce_email_order_details', $order, $sent_to_admin, $plain_text, $email );

echo "\n";

/*
 * Payment method.
 */
echo esc_html__( 'Payment method', 'onetill' ) . ': ' . esc_html( $order->get_payment_method_title() ) . "\n\n";

/*
 * Store address.
 */
$store_name     = get_bloginfo( 'name' );
$store_address  = WC()->countries->get_base_address();
$store_city     = WC()->countries->get_base_city();
$store_state    = WC()->countries->get_base_state();
$store_postcode = WC()->countries->get_base_postcode();

$address_parts = array_filter( array( $store_address, $store_city, $store_state, $store_postcode ) );

if ( ! empty( $address_parts ) ) {
	echo esc_html( $store_name ) . "\n";
	echo esc_html( implode( ', ', $address_parts ) ) . "\n\n";
}

/*
 * Additional content.
 */
if ( $additional_content ) {
	echo "---\n\n";
	echo esc_html( wp_strip_all_tags( wptexturize( $additional_content ) ) );
	echo "\n\n";
}

echo "---\n";
echo "Powered by OneTill - https://onetill.app\n";
