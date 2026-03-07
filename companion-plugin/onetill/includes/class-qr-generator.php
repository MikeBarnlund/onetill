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

use chillerlan\QRCode\QRCode;
use chillerlan\QRCode\QROptions;
use chillerlan\QRCode\Common\EccLevel;

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
		require_once ONETILL_PLUGIN_DIR . 'vendor/autoload.php';

		$options = new QROptions( array(
			'eccLevel'         => EccLevel::M,
			'outputBase64'     => false,
			'addQuietzone'     => true,
			'quietzoneSize'    => 2,
			'scale'            => 1,
			'drawLightModules' => false,
		) );

		$qrcode = new QRCode( $options );

		return $qrcode->render( $data );
	}
}
