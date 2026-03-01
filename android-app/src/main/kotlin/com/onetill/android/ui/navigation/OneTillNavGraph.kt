package com.onetill.android.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.onetill.android.ui.cart.CartScreen
import com.onetill.android.ui.catalog.CatalogScreen
import com.onetill.android.ui.checkout.CashPaymentModal
import com.onetill.android.ui.checkout.CheckoutScreen
import com.onetill.android.ui.complete.OrderCompleteScreen
import com.onetill.android.ui.orders.DailySummaryScreen
import com.onetill.android.ui.orders.OrderHistoryScreen
import com.onetill.android.ui.setup.SetupWizardScreen

object Routes {
    const val SETUP = "setup"
    const val CATALOG = "catalog"
    const val CART = "cart"
    const val CHECKOUT = "checkout"
    const val CASH_PAYMENT = "cash_payment"
    const val ORDER_COMPLETE = "order_complete/{amount}/{method}"
    const val ORDER_HISTORY = "order_history"
    const val DAILY_SUMMARY = "daily_summary"

    fun orderComplete(amount: String, method: String) = "order_complete/$amount/$method"
}

private const val SLIDE_DURATION = 200
private const val MODAL_DURATION = 250

@Composable
fun OneTillNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.SETUP,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                tween(SLIDE_DURATION),
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                tween(SLIDE_DURATION),
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                tween(SLIDE_DURATION),
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                tween(SLIDE_DURATION),
            )
        },
    ) {
        composable(Routes.SETUP) {
            SetupWizardScreen(
                onSetupComplete = {
                    navController.navigate(Routes.CATALOG) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.CATALOG) {
            CatalogScreen(
                onNavigateToCart = { navController.navigate(Routes.CART) },
                onNavigateToOrders = { navController.navigate(Routes.ORDER_HISTORY) },
                onNavigateToSummary = { navController.navigate(Routes.DAILY_SUMMARY) },
                onNavigateToSettings = { /* Settings screen not yet built */ },
            )
        }

        composable(Routes.CART) {
            CartScreen(
                onBack = { navController.popBackStack() },
                onCheckout = { navController.navigate(Routes.CHECKOUT) },
            )
        }

        composable(Routes.CHECKOUT) {
            CheckoutScreen(
                onBack = { navController.popBackStack() },
                onCashPayment = { navController.navigate(Routes.CASH_PAYMENT) },
                onCardPaymentComplete = { amount ->
                    navController.navigate(Routes.orderComplete(amount, "Card")) {
                        popUpTo(Routes.CATALOG)
                    }
                },
            )
        }

        composable(
            Routes.CASH_PAYMENT,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Up,
                    tween(MODAL_DURATION),
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Down,
                    tween(MODAL_DURATION),
                )
            },
        ) {
            CashPaymentModal(
                onClose = { navController.popBackStack() },
                onPaymentComplete = { amount ->
                    navController.navigate(Routes.orderComplete(amount, "Cash")) {
                        popUpTo(Routes.CATALOG)
                    }
                },
            )
        }

        composable(Routes.ORDER_COMPLETE) { backStackEntry ->
            val amount = backStackEntry.arguments?.getString("amount") ?: "$0.00"
            val method = backStackEntry.arguments?.getString("method") ?: "Card"
            OrderCompleteScreen(
                amount = amount,
                paymentMethod = method,
                onNewSale = {
                    navController.navigate(Routes.CATALOG) {
                        popUpTo(Routes.CATALOG) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.ORDER_HISTORY) {
            OrderHistoryScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.DAILY_SUMMARY) {
            DailySummaryScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
