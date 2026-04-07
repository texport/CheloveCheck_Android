package com.chelovecheck.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chelovecheck.R
import com.chelovecheck.domain.model.AccentColor
import com.chelovecheck.domain.model.AppLanguage
import com.chelovecheck.domain.model.ColorSource
import com.chelovecheck.domain.model.DisplayCurrency
import com.chelovecheck.domain.model.LogLevel
import com.chelovecheck.domain.model.MapProvider
import com.chelovecheck.domain.model.ThemeMode
import com.chelovecheck.presentation.screens.settings.ChoiceDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import com.chelovecheck.presentation.screens.settings.SettingsBehaviorSection
import com.chelovecheck.presentation.screens.settings.SettingsGeneralSection
import com.chelovecheck.presentation.screens.settings.SettingsLinksSection
import com.chelovecheck.presentation.screens.settings.SettingsChoiceCard
import com.chelovecheck.presentation.components.M3MaxWidthColumn
import com.chelovecheck.presentation.strings.accentColorLabel
import com.chelovecheck.presentation.strings.colorSourceLabel
import com.chelovecheck.presentation.strings.displayCurrencyLabel
import com.chelovecheck.presentation.strings.languageLabel
import com.chelovecheck.presentation.strings.logLevelLabel
import com.chelovecheck.presentation.strings.mapProviderLabel
import com.chelovecheck.presentation.strings.themeLabel
import com.chelovecheck.presentation.utils.rememberHapticPerformer
import com.chelovecheck.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenDataBackup: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val haptics = rememberHapticPerformer()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val logLevel by viewModel.logLevel.collectAsStateWithLifecycle()
    val afterScanAction by viewModel.afterScanAction.collectAsStateWithLifecycle()
    val colorSource by viewModel.colorSource.collectAsStateWithLifecycle()
    val accentColor by viewModel.accentColor.collectAsStateWithLifecycle()
    val mapProvider by viewModel.mapProvider.collectAsStateWithLifecycle()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsStateWithLifecycle()
    val analyticsPendingPromptEnabled by viewModel.analyticsPendingPromptEnabled.collectAsStateWithLifecycle()
    val displayCurrency by viewModel.displayCurrency.collectAsStateWithLifecycle()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showLogDialog by remember { mutableStateOf(false) }
    var showColorSourceDialog by remember { mutableStateOf(false) }
    var showAccentDialog by remember { mutableStateOf(false) }
    var showMapProviderDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.title_settings)) }) },
    ) { padding ->
        val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
        val contentPadding = PaddingValues(
            start = padding.calculateStartPadding(layoutDirection),
            top = padding.calculateTopPadding(),
            end = padding.calculateEndPadding(layoutDirection),
            bottom = 0.dp,
        )
        M3MaxWidthColumn(
            modifier = Modifier
                .padding(contentPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
            SettingsGeneralSection(
                themeLabel = themeLabel(themeMode),
                colorSourceLabel = colorSourceLabel(colorSource),
                accentColorLabel = accentColorLabel(accentColor),
                languageLabel = languageLabel(language),
                mapProviderLabel = mapProviderLabel(mapProvider),
                displayCurrencyLabel = displayCurrencyLabel(displayCurrency),
                diagnosticsLabel = logLevelLabel(logLevel),
                colorSource = colorSource,
                onThemeClick = {
                    haptics(HapticFeedbackType.GestureThresholdActivate)
                    showThemeDialog = true
                },
                onColorSourceClick = {
                    haptics(HapticFeedbackType.GestureThresholdActivate)
                    showColorSourceDialog = true
                },
                onAccentClick = {
                    haptics(HapticFeedbackType.GestureThresholdActivate)
                    showAccentDialog = true
                },
                onLanguageClick = {
                    haptics(HapticFeedbackType.GestureThresholdActivate)
                    showLanguageDialog = true
                },
                onMapProviderClick = {
                    haptics(HapticFeedbackType.GestureThresholdActivate)
                    showMapProviderDialog = true
                },
                onDisplayCurrencyClick = {
                    haptics(HapticFeedbackType.GestureThresholdActivate)
                    showCurrencyDialog = true
                },
                onDiagnosticsClick = {
                    haptics(HapticFeedbackType.GestureThresholdActivate)
                    showLogDialog = true
                },
            )

            SettingsChoiceCard(
                title = stringResource(R.string.settings_data_backup_title),
                value = stringResource(R.string.settings_data_backup_nav_hint),
                icon = Icons.Outlined.FileDownload,
                onClick = {
                    haptics(HapticFeedbackType.GestureThresholdActivate)
                    onOpenDataBackup()
                },
            )

            SettingsBehaviorSection(
                hapticsEnabled = hapticsEnabled,
                analyticsPendingPromptEnabled = analyticsPendingPromptEnabled,
                afterScanAction = afterScanAction,
                onHapticsChanged = {
                    haptics(HapticFeedbackType.GestureThresholdActivate)
                    viewModel.setHapticsEnabled(it)
                },
                onAnalyticsPromptChanged = {
                    haptics(HapticFeedbackType.GestureThresholdActivate)
                    viewModel.setAnalyticsPendingPromptEnabled(it)
                },
                onAfterScanChanged = {
                    haptics(HapticFeedbackType.GestureThresholdActivate)
                    viewModel.setAfterScanAction(it)
                },
            )

            SettingsLinksSection()
            Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }

    if (showThemeDialog) {
        ChoiceDialog(
            title = stringResource(R.string.settings_theme),
            options = ThemeMode.values().toList(),
            selected = themeMode,
            label = { themeLabel(it) },
            onSelected = {
                haptics(HapticFeedbackType.GestureThresholdActivate)
                viewModel.setThemeMode(it)
            },
            onDismiss = {
                haptics(HapticFeedbackType.GestureThresholdActivate)
                showThemeDialog = false
            },
        )
    }

    if (showLanguageDialog) {
        val languages = listOf(
            AppLanguage.SYSTEM,
            AppLanguage.RUSSIAN,
            AppLanguage.ENGLISH,
            AppLanguage.KAZAKH,
        )
        ChoiceDialog(
            title = stringResource(R.string.settings_language),
            options = languages,
            selected = language,
            label = { languageLabel(it) },
            onSelected = {
                haptics(HapticFeedbackType.GestureThresholdActivate)
                viewModel.setLanguage(it)
            },
            onDismiss = {
                haptics(HapticFeedbackType.GestureThresholdActivate)
                showLanguageDialog = false
            },
        )
    }

    if (showLogDialog) {
        val levels = listOf(LogLevel.OFF, LogLevel.ERROR, LogLevel.DEBUG)
        ChoiceDialog(
            title = stringResource(R.string.settings_diagnostics),
            options = levels,
            selected = logLevel,
            label = { logLevelLabel(it) },
            onSelected = {
                haptics(HapticFeedbackType.GestureThresholdActivate)
                viewModel.setLogLevel(it)
            },
            onDismiss = {
                haptics(HapticFeedbackType.GestureThresholdActivate)
                showLogDialog = false
            },
        )
    }

    if (showColorSourceDialog) {
        val sources = listOf(ColorSource.DYNAMIC, ColorSource.STATIC)
        ChoiceDialog(
            title = stringResource(R.string.settings_color_source),
            options = sources,
            selected = colorSource,
            label = { colorSourceLabel(it) },
            onSelected = {
                haptics(HapticFeedbackType.GestureThresholdActivate)
                viewModel.setColorSource(it)
            },
            onDismiss = {
                haptics(HapticFeedbackType.GestureThresholdActivate)
                showColorSourceDialog = false
            },
        )
    }

    if (showAccentDialog) {
        val accents = listOf(
            AccentColor.PURPLE,
            AccentColor.BLUE,
            AccentColor.GREEN,
            AccentColor.ORANGE,
            AccentColor.RED,
        )
        ChoiceDialog(
            title = stringResource(R.string.settings_accent_color),
            options = accents,
            selected = accentColor,
            label = { accentColorLabel(it) },
            onSelected = {
                haptics(HapticFeedbackType.GestureThresholdActivate)
                viewModel.setAccentColor(it)
            },
            onDismiss = {
                haptics(HapticFeedbackType.GestureThresholdActivate)
                showAccentDialog = false
            },
        )
    }

    if (showMapProviderDialog) {
        val providers = listOf(MapProvider.GOOGLE, MapProvider.YANDEX, MapProvider.TWO_GIS)
        ChoiceDialog(
            title = stringResource(R.string.settings_map_provider),
            options = providers,
            selected = mapProvider,
            label = { mapProviderLabel(it) },
            onSelected = {
                haptics(HapticFeedbackType.GestureThresholdActivate)
                viewModel.setMapProvider(it)
            },
            onDismiss = {
                haptics(HapticFeedbackType.GestureThresholdActivate)
                showMapProviderDialog = false
            },
        )
    }

    if (showCurrencyDialog) {
        val currencies = DisplayCurrency.entries
        ChoiceDialog(
            title = stringResource(R.string.settings_display_currency),
            options = currencies,
            selected = displayCurrency,
            label = { displayCurrencyLabel(it) },
            onSelected = {
                haptics(HapticFeedbackType.GestureThresholdActivate)
                viewModel.setDisplayCurrency(it)
            },
            onDismiss = {
                haptics(HapticFeedbackType.GestureThresholdActivate)
                showCurrencyDialog = false
            },
        )
    }
}
