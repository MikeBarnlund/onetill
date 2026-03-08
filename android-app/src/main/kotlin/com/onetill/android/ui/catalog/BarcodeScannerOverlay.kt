package com.onetill.android.ui.catalog

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.barcode.common.Barcode
import com.onetill.android.ui.components.BarcodeScannerView
import com.onetill.android.ui.components.CloseIcon
import com.onetill.android.ui.components.ScanFrameOverlay
import com.onetill.android.ui.theme.OneTillTheme

private const val BARCODE_FORMATS =
    Barcode.FORMAT_EAN_13 or Barcode.FORMAT_EAN_8 or
        Barcode.FORMAT_UPC_A or Barcode.FORMAT_UPC_E or
        Barcode.FORMAT_CODE_128 or Barcode.FORMAT_CODE_39 or
        Barcode.FORMAT_CODE_93 or Barcode.FORMAT_ITF or
        Barcode.FORMAT_QR_CODE

@Composable
fun BarcodeScannerOverlay(
    visible: Boolean,
    onBarcodeScanned: (String) -> Unit,
    onClose: () -> Unit,
) {
    val colors = OneTillTheme.colors

    var hasPermission by remember { mutableStateOf<Boolean?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(visible) {
        if (visible && hasPermission == null) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { -it },
        exit = slideOutVertically { -it },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Dark backdrop — tap to close
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClose,
                    ),
            )

            // Camera viewfinder — top half
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .align(Alignment.TopCenter),
                contentAlignment = Alignment.Center,
            ) {
                if (hasPermission == true) {
                    BarcodeScannerView(
                        onBarcodeScanned = onBarcodeScanned,
                        modifier = Modifier.fillMaxSize(),
                        formats = BARCODE_FORMATS,
                        continuous = true,
                    )

                    ScanFrameOverlay(
                        color = colors.accent,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                    )
                } else if (hasPermission == false) {
                    Text(
                        text = "Camera access required",
                        fontSize = 14.sp,
                        color = Color.White,
                    )
                }
            }

            // Close button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(36.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .clip(CircleShape)
                    .clickable(onClick = onClose)
                    .semantics { contentDescription = "Close scanner" },
                contentAlignment = Alignment.Center,
            ) {
                CloseIcon(
                    color = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }

            // Hint text
            Text(
                text = "Scan a product barcode",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
            )
        }
    }
}
