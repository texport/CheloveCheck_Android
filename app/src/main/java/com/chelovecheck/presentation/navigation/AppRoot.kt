package com.chelovecheck.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.chelovecheck.presentation.utils.rememberHapticPerformer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.remember
import com.chelovecheck.R
import com.chelovecheck.presentation.config.FeatureFlags
import com.chelovecheck.presentation.screens.ChecksScreen
import com.chelovecheck.presentation.screens.AnalyticsScreen
import com.chelovecheck.presentation.screens.ProductScreen
import com.chelovecheck.presentation.screens.ReceiptScreen
import com.chelovecheck.presentation.screens.OfdCaptchaScreen
import com.chelovecheck.presentation.screens.ScanScreen
import com.chelovecheck.presentation.screens.SettingsScreen
import com.chelovecheck.presentation.screens.settings.ExchangeRatesScreen
import com.chelovecheck.presentation.screens.settings.DataBackupScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.chelovecheck.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun AppRoot() {
    val haptics = rememberHapticPerformer()
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    var checksReselectSignal by remember { mutableIntStateOf(0) }
    val activity = LocalContext.current as Activity
    val windowSizeClass = calculateWindowSizeClass(activity)
    val isExpandedLayout = FeatureFlags.adaptiveNavigationRailEnabled &&
        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

    val showBottomBar = currentRoute == Screen.Checks.route ||
        currentRoute == Screen.Analytics.route ||
        currentRoute == Screen.Settings.route
    val showFab = currentRoute == Screen.Checks.route

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar && !isExpandedLayout) {
                AppBottomBar(
                    navController = navController,
                    currentDestination = currentDestination,
                    onChecksReselected = { checksReselectSignal++ },
                )
            }
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = {
                        haptics(HapticFeedbackType.GestureThresholdActivate)
                        navController.navigate(Screen.Scan.route)
                    },
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.action_add_receipt))
                }
            }
        },
    ) { padding ->
        if (isExpandedLayout) {
            Row(modifier = Modifier.fillMaxWidth().padding(padding)) {
                AppNavigationRail(
                    navController = navController,
                    currentDestination = currentDestination,
                    onChecksReselected = { checksReselectSignal++ },
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(84.dp),
                )
                AppNavHost(
                    navController = navController,
                    checksReselectSignal = checksReselectSignal,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            AppNavHost(
                navController = navController,
                checksReselectSignal = checksReselectSignal,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    checksReselectSignal: Int,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Checks.route,
        modifier = modifier,
    ) {
        composable(Screen.Checks.route) {
            ChecksScreen(
                scrollToTopSignal = checksReselectSignal,
                onOpenReceipt = { fiscalSign, searchQuery ->
                    navController.navigate(Screen.Receipt.create(fiscalSign, searchQuery))
                },
            )
        }
        composable(Screen.Analytics.route) {
            AnalyticsScreen()
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
            ReceiptScreen(
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

@Composable
private fun AppBottomBar(
    navController: NavHostController,
    currentDestination: androidx.navigation.NavDestination?,
    onChecksReselected: () -> Unit,
) {
    val haptics = rememberHapticPerformer()
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        val items = listOf(
            Screen.Checks to Icons.Outlined.Receipt,
            Screen.Analytics to Icons.Outlined.Insights,
            Screen.Settings to Icons.Outlined.Settings,
        )

        items.forEach { (screen, icon) ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            val label = when (screen) {
                Screen.Checks -> stringResource(R.string.nav_receipts)
                Screen.Analytics -> stringResource(R.string.nav_analytics)
                Screen.Settings -> stringResource(R.string.nav_settings)
                else -> stringResource(R.string.title_receipts)
            }
            NavigationBarItem(
                selected = selected,
                onClick = {
                    haptics(HapticFeedbackType.GestureThresholdActivate)
                    if (selected && screen == Screen.Checks) {
                        onChecksReselected()
                    }
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(icon, contentDescription = label) },
                label = {
                    Text(
                        text = if (screen == Screen.Checks) {
                            stringResource(R.string.title_receipts)
                        } else if (screen == Screen.Analytics) {
                            stringResource(R.string.title_analytics)
                        } else {
                            stringResource(R.string.title_settings)
                        },
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                colors = itemColors,
            )
        }
    }
}

@Composable
private fun AppNavigationRail(
    navController: NavHostController,
    currentDestination: androidx.navigation.NavDestination?,
    onChecksReselected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHapticPerformer()
    val items = listOf(
        Screen.Checks to Icons.Outlined.Receipt,
        Screen.Analytics to Icons.Outlined.Insights,
        Screen.Settings to Icons.Outlined.Settings,
    )
    NavigationRail(modifier = modifier, containerColor = MaterialTheme.colorScheme.surface) {
        items.forEach { (screen, icon) ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            val label = when (screen) {
                Screen.Checks -> stringResource(R.string.title_receipts)
                Screen.Analytics -> stringResource(R.string.title_analytics)
                Screen.Settings -> stringResource(R.string.title_settings)
                else -> stringResource(R.string.title_receipts)
            }
            NavigationRailItem(
                selected = selected,
                onClick = {
                    haptics(HapticFeedbackType.GestureThresholdActivate)
                    if (selected && screen == Screen.Checks) {
                        onChecksReselected()
                    }
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(imageVector = icon, contentDescription = label) },
                label = { Text(label) },
            )
        }
    }
}
