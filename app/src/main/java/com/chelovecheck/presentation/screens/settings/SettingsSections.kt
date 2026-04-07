package com.chelovecheck.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chelovecheck.R
import com.chelovecheck.domain.model.AfterScanAction
import com.chelovecheck.domain.model.ColorSource

@Composable
internal fun SettingsGeneralSection(
    themeLabel: String,
    colorSourceLabel: String,
    accentColorLabel: String,
    languageLabel: String,
    mapProviderLabel: String,
    displayCurrencyLabel: String,
    diagnosticsLabel: String,
    colorSource: ColorSource,
    onThemeClick: () -> Unit,
    onColorSourceClick: () -> Unit,
    onAccentClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onMapProviderClick: () -> Unit,
    onDisplayCurrencyClick: () -> Unit,
    onDiagnosticsClick: () -> Unit,
) {
    SettingsChoiceCard(
        title = stringResource(R.string.settings_theme),
        value = themeLabel,
        icon = Icons.Outlined.Palette,
        onClick = onThemeClick,
    )
    SettingsChoiceCard(
        title = stringResource(R.string.settings_color_source),
        value = colorSourceLabel,
        icon = Icons.Outlined.Palette,
        onClick = onColorSourceClick,
    )
    SettingsChoiceCard(
        title = stringResource(R.string.settings_accent_color),
        value = accentColorLabel,
        icon = Icons.Outlined.Palette,
        enabled = colorSource == ColorSource.STATIC,
        onClick = onAccentClick,
    )
    SettingsChoiceCard(
        title = stringResource(R.string.settings_language),
        value = languageLabel,
        icon = Icons.Outlined.Translate,
        onClick = onLanguageClick,
    )
    SettingsChoiceCard(
        title = stringResource(R.string.settings_map_provider),
        value = mapProviderLabel,
        icon = Icons.Outlined.Map,
        onClick = onMapProviderClick,
    )
    SettingsChoiceCard(
        title = stringResource(R.string.settings_display_currency),
        value = displayCurrencyLabel,
        icon = Icons.Outlined.Payments,
        onClick = onDisplayCurrencyClick,
    )
    SettingsChoiceCard(
        title = stringResource(R.string.settings_diagnostics),
        value = diagnosticsLabel,
        icon = Icons.Outlined.BugReport,
        onClick = onDiagnosticsClick,
    )
}

@Composable
internal fun SettingsBehaviorSection(
    hapticsEnabled: Boolean,
    analyticsPendingPromptEnabled: Boolean,
    afterScanAction: AfterScanAction,
    onHapticsChanged: (Boolean) -> Unit,
    onAnalyticsPromptChanged: (Boolean) -> Unit,
    onAfterScanChanged: (AfterScanAction) -> Unit,
) {
    SettingsSwitchCard(
        title = stringResource(R.string.settings_haptics),
        subtitle = stringResource(R.string.settings_haptics_subtitle),
        icon = Icons.Outlined.Vibration,
        checked = hapticsEnabled,
        onCheckedChange = onHapticsChanged,
    )
    SettingsSwitchCard(
        title = stringResource(R.string.settings_analytics_pending_prompt),
        subtitle = stringResource(R.string.settings_analytics_pending_prompt_subtitle),
        icon = Icons.Outlined.Insights,
        checked = analyticsPendingPromptEnabled,
        onCheckedChange = onAnalyticsPromptChanged,
    )
    val opensReceipt = afterScanAction == AfterScanAction.OPEN_RECEIPT
    SettingsSwitchCard(
        title = stringResource(R.string.settings_after_scan),
        subtitle = if (opensReceipt) {
            stringResource(R.string.settings_after_scan_open)
        } else {
            stringResource(R.string.settings_after_scan_message)
        },
        icon = Icons.Outlined.QrCodeScanner,
        checked = opensReceipt,
        onCheckedChange = { checked ->
            onAfterScanChanged(
                if (checked) AfterScanAction.OPEN_RECEIPT else AfterScanAction.SHOW_MESSAGE,
            )
        },
    )
}

@Composable
internal fun SettingsReceiptsSection(
    onExport: () -> Unit,
    onExportCsv: () -> Unit,
    onImport: () -> Unit,
    onDeleteAll: () -> Unit,
) {
    SettingsCard(
        title = stringResource(R.string.settings_receipts),
        icon = Icons.Outlined.FileDownload,
    ) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_export_title)) },
            supportingContent = { Text(stringResource(R.string.settings_export_subtitle)) },
            leadingContent = { Icon(Icons.Outlined.FileDownload, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            trailingContent = {
                TextButton(onClick = onExport) {
                    Text(stringResource(R.string.action_export))
                }
            },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_export_csv_title)) },
            supportingContent = { Text(stringResource(R.string.settings_export_csv_subtitle)) },
            leadingContent = { Icon(Icons.Outlined.FileDownload, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            trailingContent = {
                TextButton(onClick = onExportCsv) {
                    Text(stringResource(R.string.export_csv))
                }
            },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_import_title)) },
            supportingContent = { Text(stringResource(R.string.settings_import_subtitle)) },
            leadingContent = { Icon(Icons.Outlined.FileUpload, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            trailingContent = {
                TextButton(onClick = onImport) {
                    Text(stringResource(R.string.action_import))
                }
            },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_delete_all_title)) },
            supportingContent = { Text(stringResource(R.string.settings_delete_all_subtitle)) },
            leadingContent = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            trailingContent = {
                TextButton(onClick = onDeleteAll) {
                    Text(stringResource(R.string.action_delete))
                }
            },
        )
    }
}

@Composable
internal fun SettingsExchangeRatesCard(onRefresh: () -> Unit) {
    SettingsCard(
        title = stringResource(R.string.settings_exchange_rates),
        icon = Icons.Outlined.Sync,
    ) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_refresh_rates)) },
            supportingContent = { Text(stringResource(R.string.settings_refresh_rates_subtitle)) },
            leadingContent = { Icon(Icons.Outlined.Sync, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            trailingContent = {
                TextButton(onClick = onRefresh) {
                    Text(stringResource(R.string.action_refresh))
                }
            },
        )
    }
}

@Composable
internal fun SettingsLinksSection() {
    SettingsCard(title = stringResource(R.string.settings_links)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LinkButton(
                label = stringResource(R.string.settings_github),
                url = "https://github.com/texport/CheloveCheck",
            )
            LinkButton(
                label = stringResource(R.string.settings_telegram),
                url = "https://t.me/chelovecheck_com",
            )
        }
    }
}
