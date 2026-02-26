package com.onetill.android.ui.setup

import androidx.compose.animation.Crossfade
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onetill.android.ui.components.BottomActionBar
import com.onetill.android.ui.components.OneTillButton
import com.onetill.android.ui.components.OneTillTextField
import com.onetill.android.ui.theme.OneTillTheme

@Composable
fun SetupWizardScreen(
    onSetupComplete: () -> Unit,
    viewModel: SetupViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    Crossfade(targetState = state.currentStep, label = "setup_step") { step ->
        when (step) {
            SetupStep.Welcome -> WelcomeStep(
                onGetStarted = { viewModel.onGetStarted() },
            )
            SetupStep.StoreConnection -> StoreConnectionStep(
                siteUrl = state.siteUrl,
                consumerKey = state.consumerKey,
                consumerSecret = state.consumerSecret,
                isConnecting = state.isConnecting,
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

@Composable
private fun WelcomeStep(onGetStarted: () -> Unit) {
    val dimens = OneTillTheme.dimens

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = dimens.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Logo placeholder
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "OT",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }

            Spacer(modifier = Modifier.height(dimens.xl))

            Text(
                text = "Connect your WooCommerce store",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(dimens.md))

            Text(
                text = "Sell in person. Stay in sync.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        BottomActionBar {
            OneTillButton(
                text = "Get Started",
                onClick = onGetStarted,
            )
        }
    }
}

@Composable
private fun StoreConnectionStep(
    siteUrl: String,
    consumerKey: String,
    consumerSecret: String,
    isConnecting: Boolean,
    connectionError: String?,
    onSiteUrlChange: (String) -> Unit,
    onConsumerKeyChange: (String) -> Unit,
    onConsumerSecretChange: (String) -> Unit,
    onConnect: () -> Unit,
) {
    val dimens = OneTillTheme.dimens

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.lg, vertical = dimens.xxl),
            verticalArrangement = Arrangement.spacedBy(dimens.lg),
        ) {
            Text(
                text = "Connect Your Store",
                style = MaterialTheme.typography.headlineMedium,
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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Text(
                text = "Find your API keys in WooCommerce \u2192 Settings \u2192 Advanced \u2192 REST API",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        BottomActionBar {
            OneTillButton(
                text = if (isConnecting) "Connecting..." else "Connect",
                onClick = onConnect,
                enabled = !isConnecting && siteUrl.isNotBlank() &&
                    consumerKey.isNotBlank() && consumerSecret.isNotBlank(),
            )
        }
    }
}

@Composable
private fun CatalogSyncStep(progress: SyncProgress?) {
    val dimens = OneTillTheme.dimens
    val colors = OneTillTheme.colors

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (progress != null && progress.isComplete) {
            // Completion state
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(colors.success, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "\u2713",
                    style = MaterialTheme.typography.displayMedium,
                    color = colors.onSemantic,
                )
            }
            Spacer(modifier = Modifier.height(dimens.lg))
            Text(
                text = "All set!",
                style = MaterialTheme.typography.headlineMedium,
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp,
            )
            Spacer(modifier = Modifier.height(dimens.lg))
            Text(
                text = "Syncing your catalog...",
                style = MaterialTheme.typography.titleMedium,
            )
            if (progress != null) {
                Spacer(modifier = Modifier.height(dimens.sm))
                Text(
                    text = "${progress.current} of ${progress.total} products",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReadyStep(
    productsSynced: Int,
    registerName: String,
    onRegisterNameChange: (String) -> Unit,
    onStartSelling: () -> Unit,
) {
    val dimens = OneTillTheme.dimens
    val colors = OneTillTheme.colors

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = dimens.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(colors.success, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "\u2713",
                    style = MaterialTheme.typography.displayMedium,
                    color = colors.onSemantic,
                )
            }

            Spacer(modifier = Modifier.height(dimens.xl))

            Text(
                text = "You're ready to sell",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(dimens.md))

            Text(
                text = "$productsSynced products synced",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(dimens.xl))

            OneTillTextField(
                value = registerName,
                onValueChange = onRegisterNameChange,
                label = "Register Name",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        BottomActionBar {
            OneTillButton(
                text = "Start Selling",
                onClick = onStartSelling,
            )
        }
    }
}
