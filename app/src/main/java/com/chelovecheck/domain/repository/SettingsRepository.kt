package com.chelovecheck.domain.repository

import com.chelovecheck.domain.model.AfterScanAction
import com.chelovecheck.domain.model.AppLanguage
import com.chelovecheck.domain.model.AccentColor
import com.chelovecheck.domain.model.ColorSource
import com.chelovecheck.domain.model.LogLevel
import com.chelovecheck.domain.model.ItemTranslationLanguage
import com.chelovecheck.domain.model.DisplayCurrency
import com.chelovecheck.domain.model.MapProvider
import com.chelovecheck.domain.model.ReceiptGroupMode
import com.chelovecheck.domain.model.ReceiptListSortOrder
import com.chelovecheck.domain.model.ThemeMode
import com.chelovecheck.domain.model.TranslationProvider
import com.chelovecheck.domain.model.TranslationProviderConfig
import kotlinx.coroutines.flow.Flow

/**
 * Пользовательские настройки: в реализации `data` — DataStore и (для API-ключей) защищённое хранилище платформы.
 *
 * **Инварианты**
 * - Каждый `Flow` отражает актуальные настройки; все подписчики получают одинаковые обновления.
 * - В [translationProviderConfig] должен попадать провайдер, выбранный через [setTranslationProvider] (в хранилище — имя enum).
 * - Ключи API перевода не отдаются через обычные строковые преференсы; используется защищённое хранилище.
 *
 * **Ошибки** — [com.chelovecheck.domain.model.AppError] только там, где suspend-вызов может явно завершиться сбоем;
 * большинство чтений идут через Flow с откатом к значениям по умолчанию при битых данных.
 */
interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    val language: Flow<AppLanguage>
    val itemTranslationLanguage: Flow<ItemTranslationLanguage>
    val translationProviderConfig: Flow<TranslationProviderConfig>
    val logLevel: Flow<LogLevel>
    val afterScanAction: Flow<AfterScanAction>
    val colorSource: Flow<ColorSource>
    val accentColor: Flow<AccentColor>
    val mapProvider: Flow<MapProvider>
    val hapticsEnabled: Flow<Boolean>
    val analyticsPendingPromptEnabled: Flow<Boolean>
    val displayCurrency: Flow<DisplayCurrency>
    val receiptListSortOrder: Flow<ReceiptListSortOrder>
    val receiptGroupMode: Flow<ReceiptGroupMode>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setItemTranslationLanguage(language: ItemTranslationLanguage)
    suspend fun setTranslationProvider(provider: TranslationProvider)
    suspend fun setLibreTranslateEndpoint(endpoint: String)
    suspend fun setOpenAiApiKey(apiKey: String)
    suspend fun setGeminiApiKey(apiKey: String)
    suspend fun setOpenAiModel(model: String)
    suspend fun setGeminiModel(model: String)
    suspend fun setLogLevel(level: LogLevel)
    suspend fun setAfterScanAction(action: AfterScanAction)
    suspend fun setColorSource(source: ColorSource)
    suspend fun setAccentColor(color: AccentColor)
    suspend fun setMapProvider(provider: MapProvider)
    suspend fun setHapticsEnabled(enabled: Boolean)
    suspend fun setAnalyticsPendingPromptEnabled(enabled: Boolean)
    suspend fun setDisplayCurrency(currency: DisplayCurrency)
    suspend fun setReceiptListSortOrder(order: ReceiptListSortOrder)
    suspend fun setReceiptGroupMode(mode: ReceiptGroupMode)
}
