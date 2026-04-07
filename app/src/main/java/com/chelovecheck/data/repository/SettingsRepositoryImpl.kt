package com.chelovecheck.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chelovecheck.domain.model.AfterScanAction
import com.chelovecheck.domain.model.AppLanguage
import com.chelovecheck.domain.model.AccentColor
import com.chelovecheck.domain.model.ColorSource
import com.chelovecheck.domain.model.DisplayCurrency
import com.chelovecheck.domain.model.LogLevel
import com.chelovecheck.domain.model.MapProvider
import com.chelovecheck.domain.model.ReceiptGroupMode
import com.chelovecheck.domain.model.ReceiptListSortOrder
import com.chelovecheck.domain.model.ThemeMode
import com.chelovecheck.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val context: Context,
) : SettingsRepository {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val languageKey = stringPreferencesKey("language")
    private val logLevelKey = stringPreferencesKey("log_level")
    private val afterScanKey = stringPreferencesKey("after_scan_action")
    private val colorSourceKey = stringPreferencesKey("color_source")
    private val accentColorKey = stringPreferencesKey("accent_color")
    private val mapProviderKey = stringPreferencesKey("map_provider")
    private val hapticsEnabledKey = booleanPreferencesKey("haptics_enabled")
    private val analyticsPendingPromptKey = booleanPreferencesKey("analytics_pending_prompt_enabled")
    private val displayCurrencyKey = stringPreferencesKey("display_currency")
    private val receiptListSortOrderKey = stringPreferencesKey("receipt_list_sort_order")
    private val receiptGroupModeKey = stringPreferencesKey("receipt_group_mode")

    override val themeMode: Flow<ThemeMode> = context.dataStore.data
        .map { prefs ->
            prefs[themeKey]?.let { raw ->
                runCatching { ThemeMode.valueOf(raw) }.getOrNull()
            } ?: ThemeMode.SYSTEM
        }

    override val language: Flow<AppLanguage> = context.dataStore.data
        .map { prefs ->
            prefs[languageKey]?.let { raw ->
                when (raw) {
                    "EN" -> AppLanguage.ENGLISH
                    "RU" -> AppLanguage.RUSSIAN
                    "KK" -> AppLanguage.KAZAKH
                    else -> runCatching { AppLanguage.valueOf(raw) }.getOrNull()
                }
            } ?: AppLanguage.SYSTEM
        }

    override val logLevel: Flow<LogLevel> = context.dataStore.data
        .map { prefs ->
            prefs[logLevelKey]?.let { raw ->
                runCatching { LogLevel.valueOf(raw) }.getOrNull()
            } ?: LogLevel.ERROR
        }

    override val afterScanAction: Flow<AfterScanAction> = context.dataStore.data
        .map { prefs ->
            prefs[afterScanKey]?.let { raw ->
                runCatching { AfterScanAction.valueOf(raw) }.getOrNull()
            } ?: AfterScanAction.OPEN_RECEIPT
        }

    override val colorSource: Flow<ColorSource> = context.dataStore.data
        .map { prefs ->
            prefs[colorSourceKey]?.let { raw ->
                runCatching { ColorSource.valueOf(raw) }.getOrNull()
            } ?: ColorSource.DYNAMIC
        }

    override val accentColor: Flow<AccentColor> = context.dataStore.data
        .map { prefs ->
            prefs[accentColorKey]?.let { raw ->
                runCatching { AccentColor.valueOf(raw) }.getOrNull()
            } ?: AccentColor.PURPLE
        }

    override val mapProvider: Flow<MapProvider> = context.dataStore.data
        .map { prefs ->
            prefs[mapProviderKey]?.let { raw ->
                runCatching { MapProvider.valueOf(raw) }.getOrNull()
            } ?: MapProvider.GOOGLE
        }

    override val hapticsEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[hapticsEnabledKey] ?: true }

    override val analyticsPendingPromptEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[analyticsPendingPromptKey] ?: true }

    override val displayCurrency: Flow<DisplayCurrency> = context.dataStore.data
        .map { prefs ->
            prefs[displayCurrencyKey]?.let { raw ->
                runCatching { DisplayCurrency.valueOf(raw) }.getOrNull()
            } ?: DisplayCurrency.KZT
        }

    override val receiptListSortOrder: Flow<ReceiptListSortOrder> = context.dataStore.data
        .map { prefs ->
            prefs[receiptListSortOrderKey]?.let { raw ->
                runCatching { ReceiptListSortOrder.valueOf(raw) }.getOrNull()
            } ?: ReceiptListSortOrder.DEFAULT
        }

    override val receiptGroupMode: Flow<ReceiptGroupMode> = context.dataStore.data
        .map { prefs ->
            prefs[receiptGroupModeKey]?.let { raw ->
                runCatching { ReceiptGroupMode.valueOf(raw) }.getOrNull()
            } ?: ReceiptGroupMode.NONE
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[themeKey] = mode.name
        }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[languageKey] = language.name
        }
    }

    override suspend fun setLogLevel(level: LogLevel) {
        context.dataStore.edit { prefs ->
            prefs[logLevelKey] = level.name
        }
    }

    override suspend fun setAfterScanAction(action: AfterScanAction) {
        context.dataStore.edit { prefs ->
            prefs[afterScanKey] = action.name
        }
    }

    override suspend fun setColorSource(source: ColorSource) {
        context.dataStore.edit { prefs ->
            prefs[colorSourceKey] = source.name
        }
    }

    override suspend fun setAccentColor(color: AccentColor) {
        context.dataStore.edit { prefs ->
            prefs[accentColorKey] = color.name
        }
    }

    override suspend fun setMapProvider(provider: MapProvider) {
        context.dataStore.edit { prefs ->
            prefs[mapProviderKey] = provider.name
        }
    }

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[hapticsEnabledKey] = enabled
        }
    }

    override suspend fun setAnalyticsPendingPromptEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[analyticsPendingPromptKey] = enabled
        }
    }

    override suspend fun setDisplayCurrency(currency: DisplayCurrency) {
        context.dataStore.edit { prefs ->
            prefs[displayCurrencyKey] = currency.name
        }
    }

    override suspend fun setReceiptListSortOrder(order: ReceiptListSortOrder) {
        context.dataStore.edit { prefs ->
            prefs[receiptListSortOrderKey] = order.name
        }
    }

    override suspend fun setReceiptGroupMode(mode: ReceiptGroupMode) {
        context.dataStore.edit { prefs ->
            prefs[receiptGroupModeKey] = mode.name
        }
    }
}
