<?php
/**
 * Server-side QR code generator.
 *
 * Wraps the chillerlan/php-qrcode library to render QR codes as
 * inline SVG. Used by the Pairing class during device pairing.
 *
 * Error correction level M (15% recovery) balances data density
 * with scannability on the S700's hardware scanner.
 *
 * @package OneTill
 */

namespace OneTill;

defined( 'ABSPATH' ) || exit;

/**
 * Class QR_Generator
 */
class QR_Generator {

	/**
	 * Generate a QR code as an SVG string.
	 *
	 * @param string $data The data to encode in the QR code.
	 * @return string SVG markup.
	 */
	public function generate_svg( $data ) {
		// TODO: Implement using chillerlan/php-qrcode:
		// - Error correction level M
		// - SVG output (inline, no external file)
		// - Appropriate module size for admin page display
		return '';
	}
}
