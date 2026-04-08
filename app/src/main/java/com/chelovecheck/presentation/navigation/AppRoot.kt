package com.chelovecheck.presentation.navigation

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chelovecheck.R
import com.chelovecheck.presentation.adaptive.AdaptiveLayoutPolicy
import com.chelovecheck.presentation.adaptive.rememberAdaptiveLayoutPolicy
import com.chelovecheck.presentation.config.FeatureFlags
import com.chelovecheck.presentation.screens.AnalyticsScreen
import com.chelovecheck.presentation.screens.ChecksScreen
import com.chelovecheck.presentation.screens.OfdCaptchaScreen
import com.chelovecheck.presentation.screens.ProductScreen
import com.chelovecheck.presentation.screens.ReceiptScreen
import com.chelovecheck.presentation.screens.ScanScreen
import com.chelovecheck.presentation.screens.SettingsScreen
import com.chelovecheck.presentation.screens.settings.DataBackupScreen
import com.chelovecheck.presentation.screens.settings.ExchangeRatesScreen
import com.chelovecheck.presentation.utils.rememberHapticPerformer
import com.chelovecheck.presentation.viewmodel.SettingsViewModel

@Composable
fun AppRoot() {
    val haptics = rememberHapticPerformer()
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    var checksReselectSignal by remember { mutableIntStateOf(0) }
    val adaptivePolicy = rememberAdaptiveLayoutPolicy()
    val isExpandedLayout = FeatureFlags.adaptiveNavigationRailEnabled && adaptivePolicy.useNavigationRail
    val density = LocalDensity.current
    val hingeSpacer = with(density) { (adaptivePolicy.hingeBounds?.width ?: 0f).toDp() }

    val showNavigation = currentDestination?.hierarchy?.any {
        it.route == Screen.Checks.route ||
            it.route == Screen.Analytics.route ||
            it.route == Screen.Settings.route
    } == true
    val fabBottomPadding = if (showNavigation && !isExpandedLayout) 88.dp else 16.dp

    Box(modifier = Modifier.fillMaxSize()) {
        if (showNavigation) {
            NavigationSuiteScaffold(
                navigationSuiteItems = {
                    item(
                        icon = { Icon(Icons.Outlined.Receipt, contentDescription = null) },
                        label = { Text(stringResource(R.string.title_receipts)) },
                        selected = currentDestination?.hierarchy?.any { it.route == Screen.Checks.route } == true,
                        onClick = {
                            haptics(HapticFeedbackType.GestureThresholdActivate)
                            if (currentDestination?.hierarchy?.any { it.route == Screen.Checks.route } == true) {
                                checksReselectSignal++
                            }
                            navController.navigate(Screen.Checks.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                    item(
                        icon = { Icon(Icons.Outlined.Insights, contentDescription = null) },
                        label = { Text(stringResource(R.string.title_analytics)) },
                        selected = currentDestination?.hierarchy?.any { it.route == Screen.Analytics.route } == true,
                        onClick = {
                            haptics(HapticFeedbackType.GestureThresholdActivate)
                            navController.navigate(Screen.Analytics.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                    item(
                        icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                        label = { Text(stringResource(R.string.title_settings)) },
                        selected = currentDestination?.hierarchy?.any { it.route == Screen.Settings.route } == true,
                        onClick = {
                            haptics(HapticFeedbackType.GestureThresholdActivate)
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                },
                layoutType = if (isExpandedLayout) NavigationSuiteType.NavigationRail else NavigationSuiteType.NavigationBar,
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    if (isExpandedLayout && hingeSpacer > 0.dp) {
                        Spacer(modifier = Modifier.width(hingeSpacer))
                    }
                    AppNavHost(
                        navController = navController,
                        checksReselectSignal = checksReselectSignal,
                        adaptivePolicy = adaptivePolicy,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth()) {
                if (isExpandedLayout && hingeSpacer > 0.dp) {
                    Spacer(modifier = Modifier.width(hingeSpacer))
                }
                AppNavHost(
                    navController = navController,
                    checksReselectSignal = checksReselectSignal,
                    adaptivePolicy = adaptivePolicy,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (currentRoute == Screen.Checks.route) {
            FloatingActionButton(
                onClick = {
                    haptics(HapticFeedbackType.GestureThresholdActivate)
                    navController.navigate(Screen.Scan.route)
                },
                modifier = Modifier
                    .navigationBarsPadding()
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = fabBottomPadding),
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.action_add_receipt))
            }
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    checksReselectSignal: Int,
    adaptivePolicy: AdaptiveLayoutPolicy,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Checks.route,
        modifier = modifier,
    ) {
        composable(Screen.Checks.route) {
            ChecksScreen(
                adaptivePolicy = adaptivePolicy,
                scrollToTopSignal = checksReselectSignal,
                onOpenReceipt = { fiscalSign, searchQuery ->
                    navController.navigate(Screen.Receipt.create(fiscalSign, searchQuery))
                },
            )
        }
        composable(Screen.Analytics.route) {
            AnalyticsScreen(adaptivePolicy = adaptivePolicy)
        }
        composable(Screen.Scan.route) {
            ScanScreen(
                onReceiptSaved = { fiscalSign ->
                    navController.navigate(Screen.Receipt.create(fiscalSign))
                },
                onClose = {
                    navController.popBackStack()
                },
                onNavigateToOfdCaptcha = { encodedUrl ->
                    navController.navigate(Screen.OfdCaptchaVerification.create(encodedUrl))
                },
            )
        }
        composable(
            route = Screen.OfdCaptchaVerification.route,
            arguments = listOf(
                navArgument("encodedUrl") { type = NavType.StringType },
            ),
        ) {
            OfdCaptchaScreen(
                onClose = { navController.popBackStack() },
                onReceiptSaved = { fiscalSign ->
                    navController.popBackStack()
                    navController.navigate(Screen.Receipt.create(fiscalSign))
                },
            )
        }
        composable(
            route = Screen.Receipt.route,
            arguments = listOf(
                navArgument("fiscalSign") { type = NavType.StringType },
                navArgument("highlightKey") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val fiscalSign = backStackEntry.arguments?.getString("fiscalSign").orEmpty()
            val highlightKey = backStackEntry.arguments?.getString("highlightKey").orEmpty()
            Log.d(
                "ReceiptNav",
                "receipt destination: fiscalSign=$fiscalSign highlightKey='$highlightKey'",
            )
            ReceiptScreen(
                adaptivePolicy = adaptivePolicy,
                fiscalSign = fiscalSign,
                highlightKey = highlightKey,
                onClose = { navController.popBackStack() },
                onOpenProduct = { normalizedKey ->
                    navController.navigate(Screen.Product.create(normalizedKey))
                },
            )
        }
        composable(
            route = Screen.Product.route,
            arguments = listOf(
                navArgument("encodedKey") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val encodedKey = backStackEntry.arguments?.getString("encodedKey").orEmpty()
            ProductScreen(
                encodedKey = encodedKey,
                onClose = { navController.popBackStack() },
                onOpenReceipt = { fiscalSign ->
                    navController.navigate(Screen.Receipt.create(fiscalSign)) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onOpenDataBackup = { navController.navigate(Screen.DataBackup.route) },
            )
        }
        composable(Screen.ExchangeRates.route) {
            ExchangeRatesScreen(onClose = { navController.popBackStack() })
        }
        composable(Screen.DataBackup.route) {
            val parentEntry = remember(navController) {
                navController.getBackStackEntry(Screen.Settings.route)
            }
            DataBackupScreen(
                viewModel = hiltViewModel<SettingsViewModel>(parentEntry),
                onClose = { navController.popBackStack() },
            )
        }
    }
}
