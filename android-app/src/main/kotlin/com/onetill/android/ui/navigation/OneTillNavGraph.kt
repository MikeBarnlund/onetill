package com.onetill.android.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.onetill.android.input.IdleEventBus
import com.onetill.android.ui.lock.LockScreen
import com.onetill.android.ui.lock.LockViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.onetill.android.di.loadPostWizardModules
import com.onetill.android.ui.cart.CartScreen
import com.onetill.android.ui.catalog.CatalogScreen
import com.onetill.android.ui.checkout.CashPaymentModal
import com.onetill.android.ui.complete.OrderCompleteScreen
import com.onetill.android.ui.receipt.ReceiptEmailScreen
import com.onetill.android.ui.orders.DailySummaryScreen
import com.onetill.android.ui.orders.OrderHistoryScreen
import com.onetill.android.ui.scanner.QrPairingViewModel
import com.onetill.android.ui.scanner.QrScannerScreen
import com.onetill.android.ui.settings.SettingsScreen
import com.onetill.android.ui.setup.SetupWizardScreen
import com.onetill.shared.data.local.LocalDataSource
import com.onetill.android.stripe.StripeTerminalManager
import com.onetill.shared.sync.SyncOrchestrator
import org.koin.compose.koinInject

object Routes {
    const val SETUP = "setup"
    const val CATALOG = "catalog"
    const val CART = "cart"
    const val CASH_PAYMENT = "cash_payment"
    const val RECEIPT_EMAIL = "receipt_email/{orderId}/{amount}/{method}?change={change}"
    const val ORDER_COMPLETE = "order_complete/{amount}/{method}?change={change}&email={email}"
    const val ORDER_HISTORY = "order_history"
    const val DAILY_SUMMARY = "daily_summary"
    const val QR_SCAN = "qr_scan"
    const val SETTINGS = "settings"

    fun receiptEmail(orderId: Long, amount: String, method: String, change: String? = null): String {
        val base = "receipt_email/$orderId/$amount/$method"
        return if (change != null) "$base?change=$change" else base
    }

    fun orderComplete(amount: String, method: String, change: String? = null, email: String? = null): String {
        val base = "order_complete/$amount/$method"
        val params = buildList {
            if (change != null) add("change=$change")
            if (email != null) add("email=$email")
        }
        return if (params.isNotEmpty()) "$base?${params.joinToString("&")}" else base
    }
}

private const val SLIDE_DURATION = 300
private const val MODAL_DURATION = 250

private val slideEasing = FastOutSlowInEasing

@Composable
fun OneTillNavGraph(
    modifier: Modifier = Modifier,
) {
    val localDataSource: LocalDataSource = koinInject()

    var startDestination by remember { mutableStateOf<String?>(null) }

    // Check if store is already configured (fast SQLite read)
    LaunchedEffect(Unit) {
        val config = localDataSource.getStoreConfig()
        if (config != null) {
            loadPostWizardModules(config)
            // Start background sync and pre-warm Stripe Terminal on app restart
            try {
                val koin = org.koin.core.context.GlobalContext.get()
                koin.get<SyncOrchestrator>().startSync()
                launch { koin.get<StripeTerminalManager>().warmUp() }
            } catch (_: Exception) {
                // Not yet available — will initialize on first use
            }
            startDestination = Routes.CATALOG
        } else {
            startDestination = Routes.SETUP
        }
    }

    val dest = startDestination
    if (dest == null) {
        // Brief loading state while checking config
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    val lockViewModel: LockViewModel = org.koin.androidx.compose.koinViewModel()
    val shouldShowLock by lockViewModel.shouldShowLockScreen.collectAsState()
    val hasUsers by lockViewModel.hasUsers.collectAsState()

    // Idle timer — lock after 60 seconds of no touch events
    LaunchedEffect(hasUsers) {
        if (!hasUsers) return@LaunchedEffect
        IdleEventBus.lastTouchTime.collectLatest {
            delay(60_000L)
            lockViewModel.lock()
        }
    }

    val navController: NavHostController = rememberNavController()

    Box(modifier = Modifier.fillMaxSize()) {
    NavHost(
        navController = navController,
        startDestination = dest,
        modifier = modifier,
        // Default transitions — slide left/right with parallax pop-back
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                tween(SLIDE_DURATION, easing = slideEasing),
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                tween(SLIDE_DURATION, easing = slideEasing),
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                tween(SLIDE_DURATION, easing = slideEasing),
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                tween(SLIDE_DURATION, easing = slideEasing),
            )
        },
    ) {
        // Setup Wizard — first-launch only, replaced by Catalog on completion
        composable(Routes.SETUP) {
            SetupWizardScreen(
                onSetupComplete = {
                    navController.navigate(Routes.CATALOG) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                },
            )
        }

        // Catalog — primary screen, hub for all navigation
        composable(Routes.CATALOG) {
            CatalogScreen(
                onNavigateToCart = { navController.navigate(Routes.CART) },
                onNavigateToOrders = { navController.navigate(Routes.ORDER_HISTORY) },
                onNavigateToSummary = { navController.navigate(Routes.DAILY_SUMMARY) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToQrScan = { navController.navigate(Routes.QR_SCAN) },
            )
        }

        // Cart — includes payment method selection
        composable(Routes.CART) {
            CartScreen(
                onBack = { navController.popBackStack() },
                onCashPayment = { navController.navigate(Routes.CASH_PAYMENT) },
                onCardPaymentComplete = { orderId, amount ->
                    navController.navigate(Routes.receiptEmail(orderId, amount, "Card")) {
                        popUpTo(Routes.CATALOG)
                    }
                },
                onCardPaymentFailed = { /* Error is shown via subtitle on payment card */ },
            )
        }

        // Cash Payment — modal slide up/down (250ms)
        composable(
            Routes.CASH_PAYMENT,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Up,
                    tween(MODAL_DURATION, easing = slideEasing),
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Down,
                    tween(MODAL_DURATION, easing = slideEasing),
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Up,
                    tween(MODAL_DURATION, easing = slideEasing),
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Down,
                    tween(MODAL_DURATION, easing = slideEasing),
                )
            },
        ) {
            CashPaymentModal(
                onClose = { navController.popBackStack() },
                onPaymentComplete = { orderId, amount, changeDue ->
                    navController.navigate(Routes.receiptEmail(orderId, amount, "Cash", changeDue)) {
                        popUpTo(Routes.CATALOG)
                    }
                },
            )
        }

        // Receipt Email — post-payment email capture
        composable(Routes.RECEIPT_EMAIL) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId")?.toLongOrNull() ?: 0L
            val amount = backStackEntry.arguments?.getString("amount") ?: "$0.00"
            val method = backStackEntry.arguments?.getString("method") ?: "Card"
            val changeDue = backStackEntry.arguments?.getString("change")
                ?.takeIf { it != "{change}" }
            ReceiptEmailScreen(
                orderId = orderId,
                onSend = { email ->
                    navController.navigate(Routes.orderComplete(amount, method, changeDue, email)) {
                        popUpTo(Routes.CATALOG)
                    }
                },
                onSkip = {
                    navController.navigate(Routes.orderComplete(amount, method, changeDue)) {
                        popUpTo(Routes.CATALOG)
                    }
                },
            )
        }

        // Payment Complete — fade in/out (celebration screen, no slide)
        composable(
            Routes.ORDER_COMPLETE,
            enterTransition = { fadeIn(tween(200)) },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = { fadeOut(tween(200)) },
        ) { backStackEntry ->
            val amount = backStackEntry.arguments?.getString("amount") ?: "$0.00"
            val method = backStackEntry.arguments?.getString("method") ?: "Card"
            val changeDue = backStackEntry.arguments?.getString("change")
                ?.takeIf { it != "{change}" }
            val receiptEmail = backStackEntry.arguments?.getString("email")
                ?.takeIf { it != "{email}" }
            OrderCompleteScreen(
                amount = amount,
                paymentMethod = method,
                changeDue = changeDue,
                receiptEmail = receiptEmail,
                onNewSale = {
                    navController.navigate(Routes.CATALOG) {
                        popUpTo(Routes.CATALOG) { inclusive = true }
                    }
                },
            )
        }

        // Order History
        composable(Routes.ORDER_HISTORY) {
            OrderHistoryScreen(
                onBack = { navController.popBackStack() },
            )
        }

        // Daily Summary
        composable(Routes.DAILY_SUMMARY) {
            DailySummaryScreen(
                onBack = { navController.popBackStack() },
            )
        }

        // QR Scanner (post-setup re-pairing)
        composable(Routes.QR_SCAN) {
            val viewModel: QrPairingViewModel = org.koin.androidx.compose.koinViewModel()
            val pairingState by viewModel.state.collectAsState()

            // Navigate back to catalog on successful pairing
            LaunchedEffect(pairingState.pairingComplete) {
                if (pairingState.pairingComplete) {
                    navController.navigate(Routes.CATALOG) {
                        popUpTo(Routes.CATALOG) { inclusive = true }
                    }
                }
            }

            QrScannerScreen(
                isProcessing = pairingState.isProcessing,
                error = pairingState.error,
                onQrScanned = { viewModel.onQrScanned(it) },
                onManualEntry = {
                    navController.popBackStack()
                    navController.navigate(Routes.SETTINGS)
                },
                onRetry = { viewModel.onRetry() },
                showBackButton = true,
                onBack = { navController.popBackStack() },
            )
        }

        // Settings
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }

    // Lock screen overlay — renders on top of all screens
    LockScreen(
        visible = shouldShowLock,
        onPinEntered = { pin -> lockViewModel.verifyPin(pin) },
    )
    }
}
