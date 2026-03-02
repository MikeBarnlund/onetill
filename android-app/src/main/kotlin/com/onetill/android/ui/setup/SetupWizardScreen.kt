package com.onetill.android.ui.setup

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onetill.android.R
import com.onetill.android.ui.components.OneTillButton
import com.onetill.android.ui.components.OneTillTextField
import com.onetill.android.ui.theme.OneTillTheme
import com.onetill.android.ui.theme.Success
import com.onetill.android.ui.theme.screenGradient

@Composable
fun SetupWizardScreen(
    onSetupComplete: () -> Unit,
    viewModel: SetupViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    Crossfade(
        targetState = state.currentStep,
        animationSpec = tween(200),
        label = "setup_step",
    ) { step ->
        when (step) {
            SetupStep.Welcome -> WelcomeStep(
                onGetStarted = { viewModel.onGetStarted() },
            )
            SetupStep.StoreConnection -> StoreConnectionStep(
                siteUrl = state.siteUrl,
                consumerKey = state.consumerKey,
                consumerSecret = state.consumerSecret,
                isConnecting = state.isConnecting,
                isConnected = state.isConnected,
                connectionError = state.connectionError,
                onSiteUrlChange = { viewModel.onSiteUrlChange(it) },
                onConsumerKeyChange = { viewModel.onConsumerKeyChange(it) },
                onConsumerSecretChange = { viewModel.onConsumerSecretChange(it) },
                onConnect = { viewModel.onConnect() },
            )
            SetupStep.CatalogSync -> CatalogSyncStep(
                progress = state.syncProgress,
            )
            SetupStep.Ready -> ReadyStep(
                productsSynced = state.productsSynced,
                registerName = state.registerName,
                onRegisterNameChange = { viewModel.onRegisterNameChange(it) },
                onStartSelling = onSetupComplete,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Step 1 — Welcome
// ---------------------------------------------------------------------------

@Composable
private fun WelcomeStep(onGetStarted: () -> Unit) {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens

    Column(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind { drawRect(brush = screenGradient(size.width, size.height)) },
    ) {
        // Centered content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = dimens.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.onetill_logo),
                contentDescription = "OneTill logo",
                modifier = Modifier.size(120.dp),
            )

            Spacer(modifier = Modifier.height(dimens.xl))

            Text(
                text = "Connect your WooCommerce store",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(dimens.sm))

            Text(
                text = "Sell in person. Stay in sync.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }

        // "Get Started" CTA
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = dimens.lg, end = dimens.lg, top = 10.dp, bottom = 14.dp),
        ) {
            OneTillButton(
                text = "Get Started",
                onClick = onGetStarted,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Step 2 — Store Connection
// ---------------------------------------------------------------------------

@Composable
private fun StoreConnectionStep(
    siteUrl: String,
    consumerKey: String,
    consumerSecret: String,
    isConnecting: Boolean,
    isConnected: Boolean,
    connectionError: String?,
    onSiteUrlChange: (String) -> Unit,
    onConsumerKeyChange: (String) -> Unit,
    onConsumerSecretChange: (String) -> Unit,
    onConnect: () -> Unit,
) {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens

    Column(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind { drawRect(brush = screenGradient(size.width, size.height)) },
    ) {
        // Scrollable form
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.lg, vertical = dimens.xxl),
            verticalArrangement = Arrangement.spacedBy(dimens.lg),
        ) {
            Text(
                text = "Connect Your Store",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
            )

            OneTillTextField(
                value = siteUrl,
                onValueChange = onSiteUrlChange,
                label = "Store URL",
                placeholder = "mystore.com",
            )

            OneTillTextField(
                value = consumerKey,
                onValueChange = onConsumerKeyChange,
                label = "Consumer Key",
                placeholder = "ck_...",
            )

            OneTillTextField(
                value = consumerSecret,
                onValueChange = onConsumerSecretChange,
                label = "Consumer Secret",
                placeholder = "cs_...",
            )

            if (connectionError != null) {
                Text(
                    text = connectionError,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = colors.error,
                )
            }

            Text(
                text = "Find your API keys in WooCommerce → Settings → Advanced → REST API",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textTertiary,
            )

            // Connected success indicator
            if (isConnected) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(dimens.md))
                    SetupCheckmark(
                        size = 48.dp,
                        showGlow = false,
                    )
                    Spacer(modifier = Modifier.height(dimens.sm))
                    Text(
                        text = "Connected",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.success,
                    )
                }
            }
        }

        // "Connect" CTA
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = dimens.lg, end = dimens.lg, top = 10.dp, bottom = 14.dp),
        ) {
            OneTillButton(
                text = when {
                    isConnected -> "Connected"
                    isConnecting -> "Connecting..."
                    else -> "Connect"
                },
                onClick = onConnect,
                enabled = !isConnecting && !isConnected &&
                    siteUrl.isNotBlank() && consumerKey.isNotBlank() && consumerSecret.isNotBlank(),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Step 3 — Catalog Sync
// ---------------------------------------------------------------------------

@Composable
private fun CatalogSyncStep(progress: SyncProgress?) {
    val colors = OneTillTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind { drawRect(brush = screenGradient(size.width, size.height)) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (progress != null && progress.isComplete) {
            // Completion — green checkmark with scale-in
            val scale = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = keyframes {
                        durationMillis = 400
                        0f at 0
                        1.1f at 280
                        1f at 400
                    },
                )
            }

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    }
                    .drawBehind {
                        val glowColor = Success.copy(alpha = 0.06f)
                        drawCircle(color = glowColor, radius = size.width / 2f + 20.dp.toPx())
                        drawCircle(color = glowColor, radius = size.width / 2f + 12.dp.toPx())
                        drawCircle(color = glowColor, radius = size.width / 2f + 6.dp.toPx())
                    }
                    .background(colors.success, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                SetupCheckIcon(
                    color = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "All set!",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
            )
        } else {
            // Progress spinner
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = colors.accent,
                strokeWidth = 4.dp,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Syncing your catalog...",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
            )

            if (progress != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${progress.current} of ${progress.total} products",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Step 4 — Ready
// ---------------------------------------------------------------------------

@Composable
private fun ReadyStep(
    productsSynced: Int,
    registerName: String,
    onRegisterNameChange: (String) -> Unit,
    onStartSelling: () -> Unit,
) {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens

    // Checkmark scale-in animation
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = keyframes {
                durationMillis = 400
                0f at 0
                1.1f at 280
                1f at 400
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind { drawRect(brush = screenGradient(size.width, size.height)) },
    ) {
        // Centered content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = dimens.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Green checkmark — 64dp with glow
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    }
                    .drawBehind {
                        val glowColor = Success.copy(alpha = 0.06f)
                        drawCircle(color = glowColor, radius = size.width / 2f + 20.dp.toPx())
                        drawCircle(color = glowColor, radius = size.width / 2f + 12.dp.toPx())
                        drawCircle(color = glowColor, radius = size.width / 2f + 6.dp.toPx())
                    }
                    .background(colors.success, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                SetupCheckIcon(
                    color = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }

            Spacer(modifier = Modifier.height(dimens.xl))

            Text(
                text = "You're ready to sell",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(dimens.sm))

            Text(
                text = "$productsSynced products synced",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textSecondary,
            )

            Spacer(modifier = Modifier.height(dimens.xl))

            OneTillTextField(
                value = registerName,
                onValueChange = onRegisterNameChange,
                label = "Register Name",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // "Start Selling" CTA
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = dimens.lg, end = dimens.lg, top = 10.dp, bottom = 14.dp),
        ) {
            OneTillButton(
                text = "Start Selling",
                onClick = onStartSelling,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Shared — green checkmark with optional glow
// ---------------------------------------------------------------------------

@Composable
private fun SetupCheckmark(
    size: androidx.compose.ui.unit.Dp,
    showGlow: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = OneTillTheme.colors

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (showGlow) {
                    Modifier.drawBehind {
                        val glowColor = Success.copy(alpha = 0.06f)
                        drawCircle(color = glowColor, radius = this.size.width / 2f + 16.dp.toPx())
                        drawCircle(color = glowColor, radius = this.size.width / 2f + 10.dp.toPx())
                        drawCircle(color = glowColor, radius = this.size.width / 2f + 5.dp.toPx())
                    }
                } else {
                    Modifier
                },
            )
            .background(colors.success, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        SetupCheckIcon(
            color = Color.White,
            modifier = Modifier.size(size * 0.44f),
        )
    }
}

@Composable
private fun SetupCheckIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val s = 3.dp.toPx()
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.222f, h * 0.5f)
            lineTo(w * 0.417f, h * 0.722f)
            lineTo(w * 0.778f, h * 0.306f)
        }
        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = s,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round,
            ),
        )
    }
}
