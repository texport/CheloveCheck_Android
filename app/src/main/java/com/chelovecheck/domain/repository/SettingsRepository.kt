package com.chelovecheck.domain.repository

import com.chelovecheck.domain.model.AfterScanAction
import com.chelovecheck.domain.model.AppLanguage
import com.chelovecheck.domain.model.AccentColor
import com.chelovecheck.domain.model.ColorSource
import com.chelovecheck.domain.model.LogLevel
import com.chelovecheck.domain.model.DisplayCurrency
import com.chelovecheck.domain.model.MapProvider
import com.chelovecheck.domain.model.ReceiptGroupMode
import com.chelovecheck.domain.model.ReceiptListSortOrder
import com.chelovecheck.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    val language: Flow<AppLanguage>
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
