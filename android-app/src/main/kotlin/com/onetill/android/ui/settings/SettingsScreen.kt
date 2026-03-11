package com.onetill.android.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onetill.android.ui.components.ButtonVariant
import com.onetill.android.ui.components.AppStatusBar
import com.onetill.android.ui.components.HeaderNavAction
import com.onetill.android.ui.components.OneTillButton
import com.onetill.android.ui.components.OneTillTextField
import com.onetill.android.ui.components.ScreenHeader
import com.onetill.android.ui.theme.OneTillTheme
import com.onetill.android.ui.theme.Success
import com.onetill.android.ui.theme.screenGradientBackground
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .screenGradientBackground(),
    ) {
        AppStatusBar()

        ScreenHeader(
            title = "Settings",
            navAction = HeaderNavAction.Back,
            onNavAction = onBack,
        )

        // Scrollable form
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.lg, vertical = dimens.lg),
            verticalArrangement = Arrangement.spacedBy(dimens.lg),
        ) {
            Text(
                text = "Store Connection",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
            )

            if (state.currentStoreUrl.isNotBlank()) {
                Text(
                    text = "Connected to ${state.currentStoreUrl}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = colors.textSecondary,
                )
            }

            OneTillTextField(
                value = state.siteUrl,
                onValueChange = { viewModel.onSiteUrlChange(it) },
                label = "Store URL",
                placeholder = "mystore.com",
            )

            OneTillTextField(
                value = state.consumerKey,
                onValueChange = { viewModel.onConsumerKeyChange(it) },
                label = "Consumer Key",
                placeholder = "ck_...",
            )

            OneTillTextField(
                value = state.consumerSecret,
                onValueChange = { viewModel.onConsumerSecretChange(it) },
                label = "Consumer Secret",
                placeholder = "cs_...",
            )

            if (state.connectionError != null) {
                Text(
                    text = state.connectionError!!,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = colors.error,
                )
            }

            if (state.syncError != null) {
                Text(
                    text = state.syncError!!,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = colors.error,
                )
            }

            Text(
                text = "Find your API keys in WooCommerce \u2192 Settings \u2192 Advanced \u2192 REST API",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textTertiary,
            )

            if (state.isConnected && !state.isSyncing) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(dimens.sm))
                    SettingsCheckmark()
                    Spacer(modifier = Modifier.height(dimens.sm))
                    Text(
                        text = "Connected & synced",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.success,
                    )
                }
            }

            if (state.isSyncing) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(dimens.sm))
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = colors.accent,
                        strokeWidth = 3.dp,
                    )
                    Spacer(modifier = Modifier.height(dimens.sm))
                    Text(
                        text = "Syncing products...",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary,
                    )
                }
            }
        }

        // Bottom CTA
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = dimens.lg, end = dimens.lg, top = 10.dp, bottom = 14.dp),
        ) {
            OneTillButton(
                text = when {
                    state.isSyncing -> "Syncing..."
                    state.isConnecting -> "Connecting..."
                    else -> "Save & Sync"
                },
                onClick = { viewModel.onSaveAndSync() },
                enabled = state.hasChanges &&
                    !state.isConnecting &&
                    !state.isSyncing &&
                    state.siteUrl.isNotBlank() &&
                    state.consumerKey.isNotBlank() &&
                    state.consumerSecret.isNotBlank(),
            )
        }
    }
}

@Composable
private fun SettingsCheckmark() {
    val colors = OneTillTheme.colors

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(36.dp)
            .background(colors.success, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
            val w = size.width
            val h = size.height
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.222f, h * 0.5f)
                lineTo(w * 0.417f, h * 0.722f)
                lineTo(w * 0.778f, h * 0.306f)
            }
            drawPath(
                path = path,
                color = Color.White,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round,
                ),
            )
        }
    }
}
