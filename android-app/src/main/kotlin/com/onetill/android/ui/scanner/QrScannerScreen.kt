package com.onetill.android.ui.scanner

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onetill.android.ui.components.AppStatusBar
import com.onetill.android.ui.components.ButtonVariant
import com.onetill.android.ui.components.HeaderNavAction
import com.onetill.android.ui.components.OneTillButton
import com.onetill.android.ui.components.ScreenHeader
import com.onetill.android.ui.components.BarcodeScannerView
import com.onetill.android.ui.components.ScanFrameOverlay
import com.onetill.android.ui.theme.OneTillTheme
import com.onetill.android.ui.theme.screenGradientBackground

@Composable
fun QrScannerScreen(
    isProcessing: Boolean,
    error: String?,
    onQrScanned: (String) -> Unit,
    onManualEntry: () -> Unit,
    onRetry: () -> Unit,
    showBackButton: Boolean = false,
    onBack: (() -> Unit)? = null,
) {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens

    var scanKey by remember { mutableStateOf(0) }
    var hasPermission by remember { mutableStateOf<Boolean?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .screenGradientBackground(),
    ) {
        // Only show status bar in post-setup context (SyncOrchestrator not available during setup)
        if (onBack != null) {
            AppStatusBar()
        }

        // Header
        if (onBack != null) {
            ScreenHeader(
                title = "Scan QR Code",
                navAction = HeaderNavAction.Back,
                onNavAction = onBack,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.lg, vertical = if (onBack != null) 0.dp else dimens.md),
        ) {
            if (onBack == null) {
                Text(
                    text = "Scan QR Code",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = "Find the QR code in your WordPress admin under OneTill settings",
                fontSize = 13.sp,
                color = colors.textSecondary,
            )
        }

        // Camera preview area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = dimens.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (hasPermission) {
                true -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!isProcessing) {
                            BarcodeScannerView(
                                onBarcodeScanned = onQrScanned,
                                modifier = Modifier.fillMaxSize(),
                                formats = com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE,
                                continuous = false,
                                scanKey = scanKey,
                            )
                        }

                        // Corner bracket overlay
                        ScanFrameOverlay(
                            color = colors.accent,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                        )

                        // Processing overlay
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = colors.accent,
                                strokeWidth = 4.dp,
                            )
                        }
                    }

                    // Error display
                    if (error != null) {
                        Spacer(modifier = Modifier.height(dimens.md))
                        Text(
                            text = error,
                            fontSize = 13.sp,
                            color = colors.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(dimens.sm))
                        OneTillButton(
                            text = "Try Again",
                            onClick = {
                                scanKey++
                                onRetry()
                            },
                            variant = ButtonVariant.Ghost,
                        )
                    }
                }
                false -> {
                    // Permission denied state
                    Text(
                        text = "Camera access is required to scan QR codes",
                        fontSize = 14.sp,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(dimens.md))
                    OneTillButton(
                        text = "Grant Camera Access",
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    )
                }
                null -> {
                    // Waiting for permission result
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = colors.accent,
                        strokeWidth = 4.dp,
                    )
                }
            }
        }

        // Bottom: Manual entry fallback
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = dimens.lg, end = dimens.lg, top = 10.dp, bottom = 14.dp),
        ) {
            OneTillButton(
                text = "Enter details manually",
                onClick = onManualEntry,
                variant = ButtonVariant.Ghost,
            )
        }
    }
}
