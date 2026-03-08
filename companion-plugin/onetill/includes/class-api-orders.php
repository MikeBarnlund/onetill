<?php
/**
 * Order REST API endpoints.
 *
 * Handles:
 * - POST /onetill/v1/orders           — Create order (with idempotency)
 * - POST /onetill/v1/orders/batch     — Batch create orders (offline sync)
 * - GET  /onetill/v1/orders           — List device orders
 * - POST /onetill/v1/orders/{id}/refund — Full order refund
 *
 * @package OneTill
 */

namespace OneTill;

defined( 'ABSPATH' ) || exit;

/**
 * Class API_Orders
 */
class API_Orders {

	/**
	 * REST API namespace.
	 *
	 * @var string
	 */
	private const NAMESPACE = 'onetill/v1';

	/**
	 * Register REST API routes.
	 */
	public function register_routes() {
		register_rest_route(
			self::NAMESPACE,
			'/orders',
			array(
				array(
					'methods'             => 'POST',
					'callback'            => array( $this, 'create_order' ),
					'permission_callback' => array( $this, 'check_permissions' ),
				),
				array(
					'methods'             => 'GET',
					'callback'            => array( $this, 'get_orders' ),
					'permission_callback' => array( $this, 'check_permissions' ),
					'args'                => array(
						'page'       => array(
							'default'           => 1,
							'sanitize_callback' => 'absint',
						),
						'per_page'   => array(
							'default'           => 20,
							'sanitize_callback' => 'absint',
						),
						'device_id'  => array(
							'sanitize_callback' => 'sanitize_text_field',
						),
						'date_after' => array(
							'sanitize_callback' => 'sanitize_text_field',
						),
					),
				),
			)
		);

		register_rest_route(
			self::NAMESPACE,
			'/orders/batch',
			array(
				'methods'             => 'POST',
				'callback'            => array( $this, 'create_orders_batch' ),
				'permission_callback' => array( $this, 'check_permissions' ),
			)
		);

		register_rest_route(
			self::NAMESPACE,
			'/orders/(?P<id>\d+)/refund',
			array(
				'methods'             => 'POST',
				'callback'            => array( $this, 'refund_order' ),
				'permission_callback' => array( $this, 'check_permissions' ),
				'args'                => array(
					'id' => array(
						'required'          => true,
						'sanitize_callback' => 'absint',
					),
				),
			)
		);
	}

	/**
	 * Check that the request has valid WooCommerce API credentials.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return bool|\WP_Error
	 */
	public function check_permissions( $request ) {
		return Authenticator::check( $request );
	}

	/**
	 * Create a single order.
	 *
	 * Uses wc_create_order() for HPOS compatibility. Checks idempotency key
	 * to prevent duplicate orders on retry. Stores POS metadata as order meta:
	 * _onetill_device_id, _onetill_device_name, _onetill_payment_method,
	 * _onetill_transaction_id, _onetill_card_brand, _onetill_card_last4.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response|\WP_Error
	 */
	public function create_order( $request ) {
		$body = $request->get_json_params();

		// Validate required fields.
		$idempotency_key = isset( $body['idempotency_key'] ) ? sanitize_text_field( $body['idempotency_key'] ) : '';
		$line_items      = isset( $body['line_items'] ) ? $body['line_items'] : array();
		$payment         = isset( $body['payment'] ) ? $body['payment'] : array();
		$device_id       = isset( $body['device_id'] ) ? sanitize_text_field( $body['device_id'] ) : '';
		$device_name     = isset( $body['device_name'] ) ? sanitize_text_field( $body['device_name'] ) : '';

		if ( empty( $idempotency_key ) ) {
			return new \WP_Error( 'missing_idempotency_key', 'idempotency_key is required.', array( 'status' => 400 ) );
		}

		if ( empty( $line_items ) || ! is_array( $line_items ) ) {
			return new \WP_Error( 'missing_line_items', 'line_items array is required.', array( 'status' => 400 ) );
		}

		if ( empty( $payment ) || empty( $payment['method'] ) || empty( $payment['amount_paid'] ) ) {
			return new \WP_Error( 'missing_payment', 'payment.method and payment.amount_paid are required.', array( 'status' => 400 ) );
		}

		if ( empty( $device_id ) || empty( $device_name ) ) {
			return new \WP_Error( 'missing_device', 'device_id and device_name are required.', array( 'status' => 400 ) );
		}

		// 1. Idempotency check.
		$cached = $this->check_idempotency( $idempotency_key );
		if ( null !== $cached ) {
			return new \WP_REST_Response( $cached, 200 );
		}

		// 2. Create order via wc_create_order() (HPOS-compatible).
		$order = wc_create_order();
		if ( is_wp_error( $order ) ) {
			return $order;
		}

		// 3. Add line items.
		foreach ( $line_items as $item ) {
			$product_id   = isset( $item['product_id'] ) ? absint( $item['product_id'] ) : 0;
			$variation_id = ! empty( $item['variation_id'] ) ? absint( $item['variation_id'] ) : 0;
			$quantity     = isset( $item['quantity'] ) ? absint( $item['quantity'] ) : 1;
			$price        = isset( $item['price'] ) ? $item['price'] : '0';
			$name         = isset( $item['name'] ) ? sanitize_text_field( $item['name'] ) : '';

			$product = wc_get_product( $variation_id ? $variation_id : $product_id );

			$line_item = new \WC_Order_Item_Product();
			$line_item->set_quantity( $quantity );

			if ( $product ) {
				$line_item->set_product( $product );
			} else {
				// Offline order — product may no longer exist.
				$line_item->set_name( $name );
				$line_item->set_product_id( $product_id );
				if ( $variation_id ) {
					$line_item->set_variation_id( $variation_id );
				}
			}

			// Use the price from the POS (may differ from current catalog in offline mode).
			$line_item->set_subtotal( wc_format_decimal( (float) $price * $quantity ) );
			$line_item->set_total( wc_format_decimal( (float) $price * $quantity ) );

			$order->add_item( $line_item );
		}

		// Apply coupons.
		$coupon_codes = isset( $body['coupon_codes'] ) ? $body['coupon_codes'] : array();
		foreach ( $coupon_codes as $code ) {
			$order->apply_coupon( sanitize_text_field( $code ) );
		}

		// Set customer if provided.
		$customer_id = isset( $body['customer_id'] ) ? absint( $body['customer_id'] ) : 0;
		if ( $customer_id ) {
			$order->set_customer_id( $customer_id );
		}

		$customer_email = isset( $body['customer_email'] ) ? sanitize_email( $body['customer_email'] ) : '';
		if ( $customer_email ) {
			$order->set_billing_email( $customer_email );
		}

		// Calculate taxes and totals via WooCommerce's tax engine.
		$order->calculate_taxes();
		$order->calculate_totals( false );

		// 4. Set payment method.
		$payment_method = sanitize_text_field( $payment['method'] );
		$order->set_payment_method( 'onetill_pos' );
		$order->set_payment_method_title( $this->get_payment_method_title( $payment_method ) );

		// 5. Store POS metadata (HPOS-compatible via update_meta_data).
		$order->update_meta_data( '_onetill_source', 'onetill_pos' );
		$order->update_meta_data( '_onetill_device_id', $device_id );
		$order->update_meta_data( '_onetill_device_name', $device_name );
		$order->update_meta_data( '_onetill_payment_method', $payment_method );
		$order->update_meta_data( '_onetill_idempotency_key', $idempotency_key );

		if ( ! empty( $payment['transaction_id'] ) ) {
			$order->update_meta_data( '_onetill_stripe_id', sanitize_text_field( $payment['transaction_id'] ) );
			$order->set_transaction_id( sanitize_text_field( $payment['transaction_id'] ) );
		}

		if ( ! empty( $payment['card_brand'] ) ) {
			$order->update_meta_data( '_onetill_card_brand', sanitize_text_field( $payment['card_brand'] ) );
		}

		if ( ! empty( $payment['card_last4'] ) ) {
			$order->update_meta_data( '_onetill_card_last4', sanitize_text_field( $payment['card_last4'] ) );
		}

		$note = isset( $body['note'] ) ? sanitize_text_field( $body['note'] ) : '';
		if ( $note ) {
			$order->update_meta_data( '_onetill_note', $note );
			$order->add_order_note( $note );
		}

		// Set status to completed (POS orders are paid at point of sale).
		$order->set_status( 'completed' );
		$order->save();

		// 6. Decrement stock for each line item.
		foreach ( $line_items as $item ) {
			$product_id   = isset( $item['product_id'] ) ? absint( $item['product_id'] ) : 0;
			$variation_id = ! empty( $item['variation_id'] ) ? absint( $item['variation_id'] ) : null;
			$quantity     = isset( $item['quantity'] ) ? absint( $item['quantity'] ) : 1;

			$stock_target_id = $this->resolve_stock_target( $product_id, $variation_id );
			$target_product  = wc_get_product( $stock_target_id );

			if ( $target_product && $target_product->get_manage_stock() ) {
				wc_update_product_stock( $stock_target_id, $quantity, 'decrease' );
			}
		}

		// Build the response.
		$response_data = $this->build_create_response( $order );

		// 7. Store idempotency key.
		$this->store_idempotency( $idempotency_key, $order->get_id(), wp_json_encode( $response_data ) );

		// 8. Trigger digital receipt email if customer_email is set.
		if ( $customer_email ) {
			\WC()->mailer()->get_emails()['WC_Email_Customer_Completed_Order']->trigger( $order->get_id(), $order );
		}

		return new \WP_REST_Response( $response_data, 201 );
	}

	/**
	 * Get a human-readable payment method title.
	 *
	 * @param string $method The payment method key.
	 * @return string
	 */
	private function get_payment_method_title( $method ) {
		$titles = array(
			'stripe_terminal' => 'Stripe Terminal (POS)',
			'cash'            => 'Cash (POS)',
			'card_manual'     => 'Manual Card Entry (POS)',
		);
		return isset( $titles[ $method ] ) ? $titles[ $method ] : 'POS Payment';
	}

	/**
	 * Build the create order response payload.
	 *
	 * @param \WC_Order $order The order.
	 * @return array
	 */
	private function build_create_response( $order ) {
		$line_items = array();
		foreach ( $order->get_items() as $item ) {
			$line_items[] = array(
				'product_id'   => $item->get_product_id(),
				'variation_id' => $item->get_variation_id() ?: null,
				'name'         => $item->get_name(),
				'quantity'     => $item->get_quantity(),
				'subtotal'     => wc_format_decimal( $item->get_subtotal(), 2 ),
				'total'        => wc_format_decimal( $item->get_total(), 2 ),
				'tax'          => wc_format_decimal( $item->get_total_tax(), 2 ),
			);
		}

		return array(
			'success' => true,
			'order'   => array(
				'id'             => $order->get_id(),
				'number'         => $order->get_order_number(),
				'status'         => $order->get_status(),
				'total'          => wc_format_decimal( $order->get_total(), 2 ),
				'tax_total'      => wc_format_decimal( $order->get_total_tax(), 2 ),
				'discount_total' => wc_format_decimal( $order->get_discount_total(), 2 ),
				'created_at'     => $order->get_date_created() ? $order->get_date_created()->format( 'Y-m-d\TH:i:s\Z' ) : '',
				'line_items'     => $line_items,
			),
		);
	}

	/**
	 * Batch create orders (offline sync).
	 *
	 * Processes up to 50 orders sequentially. Each order is independently
	 * idempotent — already-created orders return from cache.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response|\WP_Error
	 */
	public function create_orders_batch( $request ) {
		$body   = $request->get_json_params();
		$orders = isset( $body['orders'] ) ? $body['orders'] : array();

		if ( empty( $orders ) || ! is_array( $orders ) ) {
			return new \WP_Error( 'missing_orders', 'orders array is required.', array( 'status' => 400 ) );
		}

		if ( count( $orders ) > 50 ) {
			return new \WP_Error( 'batch_too_large', 'Maximum 50 orders per batch request.', array( 'status' => 400 ) );
		}

		$results = array();
		$created = 0;
		$failed  = 0;

		// Process sequentially for correct stock decrements.
		foreach ( $orders as $order_data ) {
			$idempotency_key = isset( $order_data['idempotency_key'] ) ? sanitize_text_field( $order_data['idempotency_key'] ) : '';

			// Build a fake WP_REST_Request to reuse create_order().
			$sub_request = new \WP_REST_Request( 'POST', '/onetill/v1/orders' );
			$sub_request->set_body( wp_json_encode( $order_data ) );
			$sub_request->set_header( 'Content-Type', 'application/json' );

			$result = $this->create_order( $sub_request );

			if ( is_wp_error( $result ) ) {
				++$failed;
				$results[] = array(
					'idempotency_key' => $idempotency_key,
					'success'         => false,
					'error'           => $result->get_error_code(),
					'message'         => $result->get_error_message(),
				);
			} elseif ( $result instanceof \WP_REST_Response ) {
				$data = $result->get_data();
				if ( ! empty( $data['success'] ) ) {
					++$created;
					$results[] = array(
						'idempotency_key' => $idempotency_key,
						'success'         => true,
						'order_id'        => $data['order']['id'],
					);
				} else {
					++$failed;
					$results[] = array(
						'idempotency_key' => $idempotency_key,
						'success'         => false,
						'error'           => 'unknown_error',
						'message'         => 'Order creation returned unexpected response.',
					);
				}
			}
		}

		return new \WP_REST_Response( array(
			'results' => $results,
			'created' => $created,
			'failed'  => $failed,
		), 200 );
	}

	/**
	 * Get orders created by a specific device.
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response|\WP_Error
	 */
	public function get_orders( $request ) {
		$page     = $request->get_param( 'page' ) ?: 1;
		$per_page = $request->get_param( 'per_page' ) ?: 20;
		$per_page = min( $per_page, 50 );

		$args = array(
			'limit'    => $per_page,
			'page'     => $page,
			'orderby'  => 'date',
			'order'    => 'DESC',
			'meta_key'   => '_onetill_source',
			'meta_value' => 'onetill_pos',
		);

		$date_after = $request->get_param( 'date_after' );
		if ( ! empty( $date_after ) ) {
			$args['date_created'] = '>' . strtotime( $date_after );
		}

		$device_id = $request->get_param( 'device_id' );
		if ( ! empty( $device_id ) ) {
			$args['meta_query'] = array(
				'relation' => 'AND',
				array(
					'key'   => '_onetill_source',
					'value' => 'onetill_pos',
				),
				array(
					'key'   => '_onetill_device_id',
					'value' => $device_id,
				),
			);
			unset( $args['meta_key'], $args['meta_value'] );
		}

		$orders = wc_get_orders( $args );

		// Get total count for pagination.
		$count_args           = $args;
		$count_args['limit']  = -1;
		$count_args['page']   = 1;
		$count_args['return'] = 'ids';
		$total = count( wc_get_orders( $count_args ) );
		$pages = (int) ceil( $total / $per_page );

		$data = array();
		foreach ( $orders as $wc_order ) {
			$data[] = $this->format_order_summary( $wc_order );
		}

		return new \WP_REST_Response( array(
			'orders'      => $data,
			'total'       => $total,
			'page'        => (int) $page,
			'total_pages' => $pages,
		), 200 );
	}

	/**
	 * Format a WC_Order as a summary for the order list endpoint.
	 *
	 * @param \WC_Order $wc_order The WooCommerce order.
	 * @return array
	 */
	private function format_order_summary( $wc_order ) {
		$item_count = 0;
		foreach ( $wc_order->get_items() as $item ) {
			$item_count += $item->get_quantity();
		}

		$customer_name = trim( $wc_order->get_billing_first_name() . ' ' . $wc_order->get_billing_last_name() );

		return array(
			'id'             => $wc_order->get_id(),
			'number'         => $wc_order->get_order_number(),
			'status'         => $wc_order->get_status(),
			'total'          => wc_format_decimal( $wc_order->get_total(), 2 ),
			'payment_method' => $wc_order->get_meta( '_onetill_payment_method' ) ?: $wc_order->get_payment_method(),
			'card_brand'     => $wc_order->get_meta( '_onetill_card_brand' ) ?: null,
			'card_last4'     => $wc_order->get_meta( '_onetill_card_last4' ) ?: null,
			'customer_name'  => $customer_name ?: null,
			'customer_email' => $wc_order->get_billing_email() ?: null,
			'item_count'     => $item_count,
			'created_at'     => $wc_order->get_date_created() ? $wc_order->get_date_created()->format( 'Y-m-d\TH:i:s\Z' ) : '',
		);
	}

	/**
	 * Full refund an order.
	 *
	 * Creates WooCommerce refund record. The actual Stripe refund is
	 * initiated by the S700 app via Stripe Terminal SDK — the plugin
	 * does NOT touch Stripe directly (no PCI scope).
	 *
	 * @param \WP_REST_Request $request The request.
	 * @return \WP_REST_Response|\WP_Error
	 */
	public function refund_order( $request ) {
		$order_id = absint( $request->get_param( 'id' ) );
		$order    = wc_get_order( $order_id );

		if ( ! $order ) {
			return new \WP_Error( 'order_not_found', 'Order not found.', array( 'status' => 404 ) );
		}

		// Check that this is a OneTill POS order.
		if ( 'onetill_pos' !== $order->get_meta( '_onetill_source' ) ) {
			return new \WP_Error( 'not_pos_order', 'This order was not created by OneTill POS.', array( 'status' => 400 ) );
		}

		// Check the order hasn't already been fully refunded.
		if ( 'refunded' === $order->get_status() ) {
			return new \WP_Error( 'already_refunded', 'This order has already been refunded.', array( 'status' => 400 ) );
		}

		$body    = $request->get_json_params();
		$reason  = isset( $body['reason'] ) ? sanitize_text_field( $body['reason'] ) : '';
		$restock = isset( $body['restock'] ) ? (bool) $body['restock'] : false;

		// Create WooCommerce refund for the full amount.
		$refund = wc_create_refund( array(
			'amount'         => $order->get_total(),
			'reason'         => $reason,
			'order_id'       => $order_id,
			'refund_payment' => false, // Stripe refund is handled by the S700 app via Terminal SDK.
			'restock_items'  => $restock,
		) );

		if ( is_wp_error( $refund ) ) {
			return new \WP_Error( 'refund_failed', $refund->get_error_message(), array( 'status' => 500 ) );
		}

		// If restock requested and wc_create_refund didn't handle it, do it manually.
		if ( $restock ) {
			foreach ( $order->get_items() as $item ) {
				$product_id   = $item->get_product_id();
				$variation_id = $item->get_variation_id() ?: null;
				$quantity     = $item->get_quantity();

				$stock_target_id = $this->resolve_stock_target( $product_id, $variation_id );
				$target_product  = wc_get_product( $stock_target_id );

				if ( $target_product && $target_product->get_manage_stock() ) {
					wc_update_product_stock( $stock_target_id, $quantity, 'increase' );
				}
			}
		}

		return new \WP_REST_Response( array(
			'success' => true,
			'refund'  => array(
				'id'         => $refund->get_id(),
				'amount'     => wc_format_decimal( $refund->get_amount(), 2 ),
				'reason'     => $refund->get_reason(),
				'created_at' => $refund->get_date_created() ? $refund->get_date_created()->format( 'Y-m-d\TH:i:s\Z' ) : '',
			),
		), 200 );
	}

	/**
	 * Check an idempotency key and return cached response if it exists.
	 *
	 * @param string $idempotency_key The key to check.
	 * @return array|null Cached response data or null.
	 */
	private function check_idempotency( $idempotency_key ) {
		global $wpdb;

		$row = $wpdb->get_row(
			$wpdb->prepare(
				"SELECT order_id, response_body FROM {$wpdb->prefix}onetill_idempotency WHERE idempotency_key = %s",
				$idempotency_key
			),
			ARRAY_A
		);

		if ( ! $row ) {
			return null;
		}

		// Return cached response if available.
		if ( ! empty( $row['response_body'] ) ) {
			$cached = json_decode( $row['response_body'], true );
			if ( $cached ) {
				return $cached;
			}
		}

		// Fallback: rebuild response from the order.
		$order = wc_get_order( $row['order_id'] );
		if ( $order ) {
			return $this->build_create_response( $order );
		}

		return null;
	}

	/**
	 * Store an idempotency key with the cached response.
	 *
	 * @param string $idempotency_key The key.
	 * @param int    $order_id        The created order ID.
	 * @param string $response_body   JSON-encoded response body.
	 */
	private function store_idempotency( $idempotency_key, $order_id, $response_body ) {
		global $wpdb;

		$wpdb->insert(
			$wpdb->prefix . 'onetill_idempotency',
			array(
				'idempotency_key' => $idempotency_key,
				'order_id'        => $order_id,
				'response_body'   => $response_body,
				'created_at'      => current_time( 'mysql', true ),
			),
			array( '%s', '%d', '%s', '%s' )
		);
	}

	/**
	 * Clean up expired idempotency keys (older than 24 hours).
	 *
	 * Called by WP-Cron daily.
	 */
	public function cleanup_idempotency() {
		global $wpdb;

		$wpdb->query(
			$wpdb->prepare(
				"DELETE FROM {$wpdb->prefix}onetill_idempotency WHERE created_at < %s",
				gmdate( 'Y-m-d H:i:s', time() - DAY_IN_SECONDS )
			)
		);
	}

	/**
	 * Resolve the stock target for a line item.
	 *
	 * When manage_stock = "parent" on a variation, stock decrements
	 * must target the parent product ID, not the variation ID.
	 *
	 * @param int      $product_id   The product ID.
	 * @param int|null $variation_id The variation ID, if applicable.
	 * @return int The product ID to decrement stock on.
	 */
	private function resolve_stock_target( $product_id, $variation_id ) {
		if ( $variation_id ) {
			$variation = wc_get_product( $variation_id );
			if ( $variation && 'parent' === $variation->get_manage_stock() ) {
				return $variation->get_parent_id();
			}
			return $variation_id;
		}
		return $product_id;
	}
}
