<?php
/**
 * OneTill POS Receipt Email.
 *
 * Sends a branded receipt email to customers who provide their email
 * during an in-person sale on a OneTill POS terminal.
 *
 * @package OneTill
 */

defined( 'ABSPATH' ) || exit;

/**
 * Class WC_OneTill_Email_POS_Receipt
 */
class WC_OneTill_Email_POS_Receipt extends \WC_Email {

	/**
	 * Constructor.
	 */
	public function __construct() {
		$this->id             = 'onetill_pos_receipt';
		$this->title          = __( 'OneTill POS Receipt', 'onetill' );
		$this->description    = __( 'Receipt email sent to in-person customers when they provide their email at checkout on a OneTill POS terminal.', 'onetill' );
		$this->heading        = __( 'Your Receipt', 'onetill' );
		$this->subject        = __( 'Your receipt from {site_title}', 'onetill' );
		$this->template_html  = 'emails/onetill-pos-receipt.php';
		$this->template_plain = 'emails/plain/onetill-pos-receipt.php';
		$this->template_base  = ONETILL_PLUGIN_DIR . 'templates/';
		$this->customer_email = true;
		$this->manual         = true;

		parent::__construct();

		add_filter( 'woocommerce_email_footer_text', array( $this, 'append_powered_by' ), 10, 2 );
	}

	/**
	 * Append "Powered by OneTill" branding to the footer text for this email only.
	 *
	 * @param string    $footer_text The footer text.
	 * @param \WC_Email $email       The email instance.
	 * @return string
	 */
	public function append_powered_by( $footer_text, $email = null ) {
		if ( $email && 'onetill_pos_receipt' === $email->id ) {
			$footer_text .= '<br><a href="https://onetill.app?utm_source=receipt&utm_medium=email&utm_campaign=powered_by" target="_blank" rel="noopener" style="font-size: 11px; color: #b0b0b0; text-decoration: none;">Powered by OneTill</a>';
		}
		return $footer_text;
	}

	/**
	 * Trigger the email.
	 *
	 * @param int         $order_id      The order ID.
	 * @param string|null $receipt_email  Explicit recipient email (optional).
	 */
	public function trigger( $order_id, $receipt_email = null ) {
		$this->setup_locale();

		$order = wc_get_order( $order_id );
		if ( ! $order ) {
			$this->restore_locale();
			return;
		}

		$this->object = $order;

		if ( $receipt_email ) {
			$this->recipient = $receipt_email;
		} else {
			$this->recipient = $order->get_billing_email();
		}

		if ( ! $this->recipient || ! $this->is_enabled() ) {
			$this->restore_locale();
			return;
		}

		$this->placeholders['{order_date}']   = wc_format_datetime( $order->get_date_created() );
		$this->placeholders['{order_number}'] = $order->get_order_number();

		$this->send(
			$this->get_recipient(),
			$this->get_subject(),
			$this->get_content(),
			$this->get_headers(),
			$this->get_attachments()
		);

		$this->restore_locale();
	}

	/**
	 * Get content HTML.
	 *
	 * @return string
	 */
	public function get_content_html() {
		return wc_get_template_html(
			$this->template_html,
			array(
				'order'              => $this->object,
				'email_heading'      => $this->get_heading(),
				'additional_content' => $this->get_additional_content(),
				'sent_to_admin'      => false,
				'plain_text'         => false,
				'email'              => $this,
			),
			'',
			$this->template_base
		);
	}

	/**
	 * Get content plain text.
	 *
	 * @return string
	 */
	public function get_content_plain() {
		return wc_get_template_html(
			$this->template_plain,
			array(
				'order'              => $this->object,
				'email_heading'      => $this->get_heading(),
				'additional_content' => $this->get_additional_content(),
				'sent_to_admin'      => false,
				'plain_text'         => true,
				'email'              => $this,
			),
			'',
			$this->template_base
		);
	}

	/**
	 * Default content to show below main email content.
	 *
	 * @return string
	 */
	public function get_default_additional_content() {
		return __( 'Thanks for shopping with us.', 'onetill' );
	}

	/**
	 * Initialize form fields for the email settings page.
	 */
	public function init_form_fields() {
		/* translators: %s: list of available placeholders */
		$placeholder_text = sprintf( __( 'Available placeholders: %s', 'onetill' ), '<code>{site_title}, {order_date}, {order_number}</code>' );

		$this->form_fields = array(
			'enabled'            => array(
				'title'   => __( 'Enable/Disable', 'onetill' ),
				'type'    => 'checkbox',
				'label'   => __( 'Enable this email notification', 'onetill' ),
				'default' => 'yes',
			),
			'subject'            => array(
				'title'       => __( 'Subject', 'onetill' ),
				'type'        => 'text',
				'desc_tip'    => true,
				'description' => $placeholder_text,
				'placeholder' => $this->get_default_subject(),
				'default'     => '',
			),
			'heading'            => array(
				'title'       => __( 'Email heading', 'onetill' ),
				'type'        => 'text',
				'desc_tip'    => true,
				'description' => $placeholder_text,
				'placeholder' => $this->get_default_heading(),
				'default'     => '',
			),
			'additional_content' => array(
				'title'       => __( 'Additional content', 'onetill' ),
				'description' => __( 'Text to appear below the main email content.', 'onetill' ) . ' ' . $placeholder_text,
				'css'         => 'width:400px; height: 75px;',
				'placeholder' => __( 'N/A', 'onetill' ),
				'type'        => 'textarea',
				'default'     => $this->get_default_additional_content(),
				'desc_tip'    => true,
			),
			'email_type'         => array(
				'title'       => __( 'Email type', 'onetill' ),
				'type'        => 'select',
				'description' => __( 'Choose which format of email to send.', 'onetill' ),
				'default'     => 'html',
				'class'       => 'email_type wc-enhanced-select',
				'options'     => $this->get_email_type_options(),
				'desc_tip'    => true,
			),
		);
	}
}
