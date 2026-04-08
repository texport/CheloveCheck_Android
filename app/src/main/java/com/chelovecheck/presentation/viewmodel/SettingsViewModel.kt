package com.chelovecheck.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelovecheck.domain.model.AccentColor
import com.chelovecheck.domain.model.AfterScanAction
import com.chelovecheck.domain.model.AppLanguage
import com.chelovecheck.domain.model.ColorSource
import com.chelovecheck.domain.model.DisplayCurrency
import com.chelovecheck.domain.model.ItemTranslationLanguage
import com.chelovecheck.domain.model.Receipt
import com.chelovecheck.domain.model.LogLevel
import com.chelovecheck.domain.model.MapProvider
import com.chelovecheck.domain.model.ThemeMode
import com.chelovecheck.domain.repository.SaveManyResult
import com.chelovecheck.domain.usecase.DeleteAllReceiptsUseCase
import com.chelovecheck.domain.usecase.ExportReceiptsCsvUseCase
import com.chelovecheck.domain.usecase.ExportReceiptsUseCase
import com.chelovecheck.domain.usecase.ImportReceiptsUseCase
import com.chelovecheck.domain.usecase.ObserveDisplayCurrencyUseCase
import com.chelovecheck.domain.usecase.RefreshExchangeRatesUseCase
import com.chelovecheck.domain.usecase.ObserveAccentColorUseCase
import com.chelovecheck.domain.usecase.ObserveLanguageUseCase
import com.chelovecheck.domain.usecase.ObserveAfterScanActionUseCase
import com.chelovecheck.domain.usecase.ObserveColorSourceUseCase
import com.chelovecheck.domain.usecase.ObserveAnalyticsPendingPromptUseCase
import com.chelovecheck.domain.usecase.ObserveHapticsEnabledUseCase
import com.chelovecheck.domain.usecase.ObserveLogLevelUseCase
import com.chelovecheck.domain.usecase.ObserveMapProviderUseCase
import com.chelovecheck.domain.usecase.ObserveThemeModeUseCase
import com.chelovecheck.domain.usecase.SetAccentColorUseCase
import com.chelovecheck.domain.usecase.SetDisplayCurrencyUseCase
import com.chelovecheck.domain.usecase.SetLanguageUseCase
import com.chelovecheck.domain.usecase.SetItemTranslationLanguageUseCase
import com.chelovecheck.domain.usecase.SetAfterScanActionUseCase
import com.chelovecheck.domain.usecase.SetAnalyticsPendingPromptUseCase
import com.chelovecheck.domain.usecase.SetColorSourceUseCase
import com.chelovecheck.domain.usecase.SetHapticsEnabledUseCase
import com.chelovecheck.domain.usecase.SetLogLevelUseCase
import com.chelovecheck.domain.usecase.SetMapProviderUseCase
import com.chelovecheck.domain.usecase.ReplaceImportedDuplicatesUseCase
import com.chelovecheck.domain.usecase.SetThemeModeUseCase
import com.chelovecheck.domain.usecase.ObserveItemTranslationLanguageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeThemeModeUseCase: ObserveThemeModeUseCase,
    observeLanguageUseCase: ObserveLanguageUseCase,
    observeLogLevelUseCase: ObserveLogLevelUseCase,
    observeAfterScanActionUseCase: ObserveAfterScanActionUseCase,
    observeColorSourceUseCase: ObserveColorSourceUseCase,
    observeAccentColorUseCase: ObserveAccentColorUseCase,
    observeMapProviderUseCase: ObserveMapProviderUseCase,
    observeHapticsEnabledUseCase: ObserveHapticsEnabledUseCase,
    observeAnalyticsPendingPromptUseCase: ObserveAnalyticsPendingPromptUseCase,
    observeDisplayCurrencyUseCase: ObserveDisplayCurrencyUseCase,
    observeItemTranslationLanguageUseCase: ObserveItemTranslationLanguageUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val setLanguageUseCase: SetLanguageUseCase,
    private val setLogLevelUseCase: SetLogLevelUseCase,
    private val setAfterScanActionUseCase: SetAfterScanActionUseCase,
    private val setColorSourceUseCase: SetColorSourceUseCase,
    private val setAccentColorUseCase: SetAccentColorUseCase,
    private val setMapProviderUseCase: SetMapProviderUseCase,
    private val setHapticsEnabledUseCase: SetHapticsEnabledUseCase,
    private val setAnalyticsPendingPromptUseCase: SetAnalyticsPendingPromptUseCase,
    private val setDisplayCurrencyUseCase: SetDisplayCurrencyUseCase,
    private val setItemTranslationLanguageUseCase: SetItemTranslationLanguageUseCase,
    private val exportReceiptsUseCase: ExportReceiptsUseCase,
    private val exportReceiptsCsvUseCase: ExportReceiptsCsvUseCase,
    private val importReceiptsUseCase: ImportReceiptsUseCase,
    private val replaceImportedDuplicatesUseCase: ReplaceImportedDuplicatesUseCase,
    private val refreshExchangeRatesUseCase: RefreshExchangeRatesUseCase,
    private val deleteAllReceiptsUseCase: DeleteAllReceiptsUseCase,
) : ViewModel() {
    private val _events = MutableSharedFlow<SettingsUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SettingsUiEvent> = _events.asSharedFlow()

    val themeMode: StateFlow<ThemeMode> = observeThemeModeUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val language: StateFlow<AppLanguage> = observeLanguageUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppLanguage.SYSTEM)

    val logLevel: StateFlow<LogLevel> = observeLogLevelUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, LogLevel.ERROR)

    val afterScanAction: StateFlow<AfterScanAction> = observeAfterScanActionUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AfterScanAction.OPEN_RECEIPT)

    val colorSource: StateFlow<ColorSource> = observeColorSourceUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ColorSource.DYNAMIC)

    val accentColor: StateFlow<AccentColor> = observeAccentColorUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AccentColor.PURPLE)

    val mapProvider: StateFlow<MapProvider> = observeMapProviderUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, MapProvider.GOOGLE)

    val hapticsEnabled: StateFlow<Boolean> = observeHapticsEnabledUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val analyticsPendingPromptEnabled: StateFlow<Boolean> = observeAnalyticsPendingPromptUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val displayCurrency: StateFlow<DisplayCurrency> = observeDisplayCurrencyUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, DisplayCurrency.KZT)

    val itemTranslationLanguage: StateFlow<ItemTranslationLanguage> = observeItemTranslationLanguageUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ItemTranslationLanguage.OFD_SOURCE)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            setThemeModeUseCase(mode)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            setLanguageUseCase(language)
        }
    }

    fun setLogLevel(level: LogLevel) {
        viewModelScope.launch {
            setLogLevelUseCase(level)
        }
    }

    fun setAfterScanAction(action: AfterScanAction) {
        viewModelScope.launch {
            setAfterScanActionUseCase(action)
        }
    }

    fun setColorSource(source: ColorSource) {
        viewModelScope.launch {
            setColorSourceUseCase(source)
        }
    }

    fun setAccentColor(color: AccentColor) {
        viewModelScope.launch {
            setAccentColorUseCase(color)
        }
    }

    fun setMapProvider(provider: MapProvider) {
        viewModelScope.launch {
            setMapProviderUseCase(provider)
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            setHapticsEnabledUseCase(enabled)
        }
    }

    fun setAnalyticsPendingPromptEnabled(enabled: Boolean) {
        viewModelScope.launch {
            setAnalyticsPendingPromptUseCase(enabled)
        }
    }

    fun setDisplayCurrency(currency: DisplayCurrency) {
        viewModelScope.launch {
            setDisplayCurrencyUseCase(currency)
        }
    }

    fun setItemTranslationLanguage(language: ItemTranslationLanguage) {
        viewModelScope.launch {
            setItemTranslationLanguageUseCase(language)
        }
    }

    suspend fun exportJson(): String = exportReceiptsUseCase()

    suspend fun exportCsv(): String = exportReceiptsCsvUseCase()

    fun notifyExportSucceeded() {
        _events.tryEmit(SettingsUiEvent.ExportSucceeded)
    }

    fun notifyExportFailed() {
        _events.tryEmit(SettingsUiEvent.ExportFailed)
    }

    fun notifyImportFailed() {
        _events.tryEmit(SettingsUiEvent.ImportFailed)
    }

    fun importJson(json: String) {
        viewModelScope.launch {
            runCatching { importReceiptsUseCase(json) }
                .onSuccess { result ->
                    if (result.skipped.isNotEmpty()) {
                        _events.emit(SettingsUiEvent.ImportDuplicatesPending(result))
                    } else {
                        _events.emit(SettingsUiEvent.ImportFinished(result))
                    }
                }
                .onFailure {
                    _events.emit(SettingsUiEvent.ImportFailed)
                }
        }
    }

    fun replaceImportDuplicates(skippedReceipts: List<Receipt>) {
        viewModelScope.launch {
            runCatching { replaceImportedDuplicatesUseCase(skippedReceipts) }
                .onSuccess {
                    _events.emit(
                        SettingsUiEvent.ImportFinished(
                            SaveManyResult(imported = skippedReceipts, skipped = emptyList()),
                        ),
                    )
                }
                .onFailure {
                    _events.emit(SettingsUiEvent.ImportFailed)
                }
        }
    }

    fun deleteAllReceipts() {
        viewModelScope.launch {
            deleteAllReceiptsUseCase()
            _events.emit(SettingsUiEvent.DeleteAllFinished)
        }
    }

    fun refreshExchangeRates() {
        viewModelScope.launch {
            val ok = refreshExchangeRatesUseCase()
            _events.emit(
                if (ok) SettingsUiEvent.ExchangeRatesUpdated else SettingsUiEvent.ExchangeRatesUpdateFailed,
            )
        }
    }
}

sealed interface SettingsUiEvent {
    data object ExportSucceeded : SettingsUiEvent
    data object ExportFailed : SettingsUiEvent
    data class ImportFinished(val result: SaveManyResult) : SettingsUiEvent
    /** Import succeeded but some fiscal signs already existed; user may replace them. */
    data class ImportDuplicatesPending(val result: SaveManyResult) : SettingsUiEvent
    data object ImportFailed : SettingsUiEvent
    data object DeleteAllFinished : SettingsUiEvent
    data object ExchangeRatesUpdated : SettingsUiEvent
    data object ExchangeRatesUpdateFailed : SettingsUiEvent
}
