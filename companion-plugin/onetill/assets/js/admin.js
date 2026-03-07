/**
 * OneTill admin scripts.
 *
 * Handles QR pairing flow (initiate, poll, success/expired) and
 * device disconnect actions on the dashboard page.
 *
 * @package OneTill
 */

/* global jQuery, onetillAdmin */

(function ($) {
	'use strict';

	var pollInterval = null;
	var countdownInterval = null;
	var currentToken = null;

	/**
	 * Show a modal state, hiding all others.
	 */
	function showState(stateId) {
		$('.onetill-pairing-state').hide();
		$('#onetill-state-' + stateId).show();
	}

	/**
	 * Open the pairing modal.
	 */
	function openModal() {
		$('#onetill-pairing-modal').fadeIn(200);
	}

	/**
	 * Close the pairing modal and clean up.
	 */
	function closeModal() {
		$('#onetill-pairing-modal').fadeOut(200);
		stopPolling();
	}

	/**
	 * Stop polling and countdown intervals.
	 */
	function stopPolling() {
		if (pollInterval) {
			clearInterval(pollInterval);
			pollInterval = null;
		}
		if (countdownInterval) {
			clearInterval(countdownInterval);
			countdownInterval = null;
		}
		currentToken = null;
	}

	/**
	 * Initiate pairing — request QR code from server.
	 */
	function initiatePairing() {
		stopPolling();
		showState('loading');
		openModal();

		$.ajax({
			url: onetillAdmin.ajaxUrl,
			method: 'POST',
			data: {
				action: 'onetill_initiate_pairing',
				nonce: onetillAdmin.nonce
			},
			success: function (response) {
				if (response.success && response.data) {
					currentToken = response.data.token;

					// Insert QR SVG.
					$('#onetill-qr-container').html(response.data.qr_svg);

					// Start countdown.
					var expiresAt = new Date(response.data.expires_at).getTime();
					startCountdown(expiresAt);

					// Show QR state.
					showState('qr');

					// Start polling for completion.
					startPolling();
				} else {
					showError(response.data ? response.data.message : onetillAdmin.i18n.pairingError);
				}
			},
			error: function (xhr) {
				var msg = onetillAdmin.i18n.pairingError;
				if (xhr.responseJSON && xhr.responseJSON.data && xhr.responseJSON.data.message) {
					msg = xhr.responseJSON.data.message;
				}
				showError(msg);
			}
		});
	}

	/**
	 * Start polling for pairing status every 2 seconds.
	 */
	function startPolling() {
		pollInterval = setInterval(function () {
			if (!currentToken) {
				stopPolling();
				return;
			}

			$.ajax({
				url: onetillAdmin.ajaxUrl,
				method: 'GET',
				data: {
					action: 'onetill_check_pairing_status',
					nonce: onetillAdmin.nonce,
					token: currentToken
				},
				success: function (response) {
					if (!response.success || !response.data) {
						return;
					}

					var status = response.data.status;

					if (status === 'complete') {
						stopPolling();
						$('#onetill-paired-device-name').text(
							response.data.device ? response.data.device.name : ''
						);
						showState('success');
					} else if (status === 'expired') {
						stopPolling();
						showState('expired');
					}
					// 'pending' — keep polling.
				}
			});
		}, 2000);
	}

	/**
	 * Start the expiry countdown timer.
	 */
	function startCountdown(expiresAt) {
		if (countdownInterval) {
			clearInterval(countdownInterval);
		}

		function updateCountdown() {
			var remaining = Math.max(0, Math.floor((expiresAt - Date.now()) / 1000));
			var minutes = Math.floor(remaining / 60);
			var seconds = remaining % 60;
			$('#onetill-countdown').text(
				minutes + ':' + (seconds < 10 ? '0' : '') + seconds
			);

			if (remaining <= 0) {
				clearInterval(countdownInterval);
				countdownInterval = null;
			}
		}

		updateCountdown();
		countdownInterval = setInterval(updateCountdown, 1000);
	}

	/**
	 * Show the error state.
	 */
	function showError(message) {
		$('#onetill-error-message').text(message);
		showState('error');
	}

	/**
	 * Disconnect a device.
	 */
	function disconnectDevice(deviceId, $row) {
		$.ajax({
			url: onetillAdmin.ajaxUrl,
			method: 'POST',
			data: {
				action: 'onetill_disconnect_device',
				nonce: onetillAdmin.nonce,
				device_id: deviceId
			},
			success: function (response) {
				if (response.success) {
					$row.fadeOut(300, function () {
						$row.remove();
						// If no more devices, reload to show empty state.
						if ($('.onetill-devices-table tbody tr').length === 0) {
							location.reload();
						}
					});
				} else {
					alert(response.data ? response.data.message : onetillAdmin.i18n.pairingError);
				}
			}
		});
	}

	// -- Event Bindings --

	$(document).ready(function () {
		// Pair New Device button.
		$('#onetill-pair-btn').on('click', function () {
			initiatePairing();
		});

		// Modal close.
		$('#onetill-modal-close').on('click', closeModal);

		// Click outside modal to close.
		$('#onetill-pairing-modal').on('click', function (e) {
			if ($(e.target).is('.onetill-modal-overlay')) {
				closeModal();
			}
		});

		// ESC key to close.
		$(document).on('keydown', function (e) {
			if (e.key === 'Escape' && $('#onetill-pairing-modal').is(':visible')) {
				closeModal();
			}
		});

		// Done button (after success).
		$('#onetill-done-btn').on('click', function () {
			closeModal();
			location.reload();
		});

		// Retry buttons.
		$('#onetill-retry-btn, #onetill-error-retry-btn').on('click', function () {
			initiatePairing();
		});

		// Disconnect device.
		$(document).on('click', '.onetill-disconnect-btn', function () {
			var deviceId = $(this).data('device-id');
			var deviceName = $(this).data('device-name');
			var $row = $(this).closest('tr');

			if (confirm(onetillAdmin.i18n.disconnectConfirm)) {
				disconnectDevice(deviceId, $row);
			}
		});
	});
})(jQuery);
