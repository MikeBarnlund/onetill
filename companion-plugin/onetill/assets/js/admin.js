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

	// -- User Management --

	function openUserModal(title, userId, firstName, lastName, pinRequired) {
		$('#onetill-user-modal-title').text(title);
		$('#onetill-user-id').val(userId || '');
		$('#onetill-first-name').val(firstName || '');
		$('#onetill-last-name').val(lastName || '');
		$('#onetill-pin').val('');
		$('#onetill-user-form-error').hide();

		// PIN is required for new users, optional for edits.
		var $pin = $('#onetill-pin');
		if (pinRequired) {
			$pin.attr('required', 'required');
			$('#onetill-pin-hint').text('');
		} else {
			$pin.removeAttr('required');
			$('#onetill-pin-hint').text(onetillAdmin.i18n.pinLeaveBlank || '');
		}

		$('#onetill-user-modal').fadeIn(200);
		$('#onetill-first-name').focus();
	}

	function closeUserModal() {
		$('#onetill-user-modal').fadeOut(200);
	}

	function showUserError(message) {
		$('#onetill-user-form-error').text(message).show();
	}

	function saveUser(e) {
		e.preventDefault();

		var userId    = $('#onetill-user-id').val();
		var firstName = $.trim($('#onetill-first-name').val());
		var lastName  = $.trim($('#onetill-last-name').val());
		var pin       = $.trim($('#onetill-pin').val());
		var isEdit    = !!userId;

		$('#onetill-user-form-error').hide();

		if (!firstName || !lastName) {
			showUserError(onetillAdmin.i18n.nameRequired || 'First and last name are required.');
			return;
		}

		if (!isEdit && !pin) {
			showUserError(onetillAdmin.i18n.pinRequired || 'PIN is required.');
			return;
		}

		if (pin && !/^\d{4}$/.test(pin)) {
			showUserError(onetillAdmin.i18n.pinFormat || 'PIN must be exactly 4 digits.');
			return;
		}

		var $saveBtn = $('#onetill-user-save-btn');
		$saveBtn.prop('disabled', true).text(onetillAdmin.i18n.saving || 'Saving...');

		var data = {
			action: isEdit ? 'onetill_update_user' : 'onetill_create_user',
			nonce: onetillAdmin.nonce,
			first_name: firstName,
			last_name: lastName,
			pin: pin
		};

		if (isEdit) {
			data.user_id = userId;
		}

		$.ajax({
			url: onetillAdmin.ajaxUrl,
			method: 'POST',
			data: data,
			success: function (response) {
				if (response.success) {
					closeUserModal();
					location.reload();
				} else {
					showUserError(response.data ? response.data.message : onetillAdmin.i18n.userError);
				}
			},
			error: function (xhr) {
				var msg = onetillAdmin.i18n.userError || 'Something went wrong.';
				if (xhr.responseJSON && xhr.responseJSON.data && xhr.responseJSON.data.message) {
					msg = xhr.responseJSON.data.message;
				}
				showUserError(msg);
			},
			complete: function () {
				$saveBtn.prop('disabled', false).text(onetillAdmin.i18n.save || 'Save');
			}
		});
	}

	function deleteUser(userId, userName, $row) {
		$.ajax({
			url: onetillAdmin.ajaxUrl,
			method: 'POST',
			data: {
				action: 'onetill_delete_user',
				nonce: onetillAdmin.nonce,
				user_id: userId
			},
			success: function (response) {
				if (response.success) {
					$row.fadeOut(300, function () {
						$row.remove();
						if ($('#onetill-users-tbody tr').length === 0) {
							$('.onetill-users-table').hide();
							$('#onetill-users-empty').show();
						}
					});
				} else {
					alert(response.data ? response.data.message : onetillAdmin.i18n.userError);
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

		// -- User Management Bindings --

		// Add User button.
		$('#onetill-add-user-btn').on('click', function () {
			openUserModal(
				onetillAdmin.i18n.addUser || 'Add User',
				'', '', '', true
			);
		});

		// Edit User button.
		$(document).on('click', '.onetill-edit-user-btn', function () {
			openUserModal(
				onetillAdmin.i18n.editUser || 'Edit User',
				$(this).data('user-id'),
				$(this).data('first-name'),
				$(this).data('last-name'),
				false
			);
		});

		// Delete User button.
		$(document).on('click', '.onetill-delete-user-btn', function () {
			var userId = $(this).data('user-id');
			var userName = $(this).data('user-name');
			var $row = $(this).closest('tr');

			if (confirm(onetillAdmin.i18n.deleteConfirm
				? onetillAdmin.i18n.deleteConfirm.replace('%s', userName)
				: 'Are you sure you want to delete ' + userName + '?'
			)) {
				deleteUser(userId, userName, $row);
			}
		});

		// User modal close.
		$('#onetill-user-modal-close, #onetill-user-cancel-btn').on('click', closeUserModal);

		// Click outside user modal to close.
		$('#onetill-user-modal').on('click', function (e) {
			if ($(e.target).is('.onetill-modal-overlay')) {
				closeUserModal();
			}
		});

		// ESC key to close user modal.
		$(document).on('keydown', function (e) {
			if (e.key === 'Escape' && $('#onetill-user-modal').is(':visible')) {
				closeUserModal();
			}
		});

		// User form submission.
		$('#onetill-user-form').on('submit', saveUser);
	});
})(jQuery);
