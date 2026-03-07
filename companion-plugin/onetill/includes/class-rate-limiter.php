<?php
/**
 * Rate limiter using WordPress transients.
 *
 * Provides per-device, per-endpoint-group rate limiting for all
 * OneTill REST API endpoints.
 *
 * @package OneTill
 */

namespace OneTill;

defined( 'ABSPATH' ) || exit;

/**
 * Class Rate_Limiter
 */
class Rate_Limiter {

	/**
	 * Get rate limits per endpoint group.
	 *
	 * @return array
	 */
	private static function get_limits() {
		return array(
			'read'      => array( 'max' => 120, 'window' => MINUTE_IN_SECONDS ),
			'write'     => array( 'max' => 60,  'window' => MINUTE_IN_SECONDS ),
			'heartbeat' => array( 'max' => 2,   'window' => MINUTE_IN_SECONDS ),
		);
	}

	/**
	 * Check if a request is within the rate limit.
	 *
	 * @param string $device_id      The device identifier.
	 * @param string $endpoint_group One of: read, write, heartbeat.
	 * @return bool True if allowed, false if rate limited.
	 */
	public static function check( $device_id, $endpoint_group ) {
		$limits = self::get_limits();

		if ( empty( $device_id ) || ! isset( $limits[ $endpoint_group ] ) ) {
			return true;
		}

		$limit  = $limits[ $endpoint_group ];
		$key    = 'onetill_rate_' . md5( $device_id ) . '_' . $endpoint_group;
		$count  = (int) get_transient( $key );

		if ( $count >= $limit['max'] ) {
			return false;
		}

		set_transient( $key, $count + 1, $limit['window'] );

		return true;
	}

	/**
	 * Return a rate-limited WP_Error response.
	 *
	 * @return \WP_Error
	 */
	public static function error() {
		return new \WP_Error(
			'onetill_rate_limited',
			__( 'Rate limit exceeded. Please try again later.', 'onetill' ),
			array( 'status' => 429 )
		);
	}
}
