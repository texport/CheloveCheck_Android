package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.AccentColor
import com.chelovecheck.domain.model.AfterScanAction
import com.chelovecheck.domain.model.AppLanguage
import com.chelovecheck.domain.model.ColorSource
import com.chelovecheck.domain.model.DisplayCurrency
import com.chelovecheck.domain.model.Item
import com.chelovecheck.domain.model.ItemTranslationLanguage
import com.chelovecheck.domain.model.LogLevel
import com.chelovecheck.domain.model.MapProvider
import com.chelovecheck.domain.model.Ofd
import com.chelovecheck.domain.model.OperationType
import com.chelovecheck.domain.model.Payment
import com.chelovecheck.domain.model.PaymentType
import com.chelovecheck.domain.model.Receipt
import com.chelovecheck.domain.model.ReceiptGroupMode
import com.chelovecheck.domain.model.ReceiptListSortOrder
import com.chelovecheck.domain.model.ThemeMode
import com.chelovecheck.domain.model.TranslationProvider
import com.chelovecheck.domain.model.TranslationProviderConfig
import com.chelovecheck.domain.model.UnitOfMeasurement
import com.chelovecheck.domain.logging.AppLogger
import com.chelovecheck.domain.repository.ItemTranslationRepository
import com.chelovecheck.domain.repository.SettingsRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TranslateReceiptItemsUseCaseTest {
    @Test
    fun ofdSourceModeLeavesNamesUntouched() = runBlocking {
        val settings = FakeSettingsRepository(itemLang = ItemTranslationLanguage.OFD_SOURCE)
        val translator = FakeTranslator(mapOf("Milk" to "Сүт"))
        val useCase = TranslateReceiptItemsUseCase(settings, translator, NoOpLogger)

        val translated = useCase(sampleReceipt("Milk"))

        assertEquals("Milk", translated.items.first().name)
    }

    @Test
    fun translatesNamesUsingSelectedLanguage() = runBlocking {
        val settings = FakeSettingsRepository(itemLang = ItemTranslationLanguage.KAZAKH)
        val translator = FakeTranslator(mapOf("Milk" to "Сүт"))
        val useCase = TranslateReceiptItemsUseCase(settings, translator, NoOpLogger)

        val translated = useCase(sampleReceipt("Milk"))

        assertEquals("Сүт", translated.items.first().name)
    }

    @Test
    fun fallsBackToSourceWhenTranslatorFails() = runBlocking {
        val settings = FakeSettingsRepository(itemLang = ItemTranslationLanguage.RUSSIAN)
        val translator = object : ItemTranslationRepository {
            override suspend fun translateNames(names: List<String>, targetLanguageTag: String): Map<String, String> {
                error("boom")
            }
        }
        val useCase = TranslateReceiptItemsUseCase(settings, translator, NoOpLogger)

        val translated = useCase(sampleReceipt("Milk"))

        assertEquals("Milk", translated.items.first().name)
    }

    private fun sampleReceipt(itemName: String): Receipt {
        return Receipt(
            companyName = "Shop",
            certificateVat = null,
            iinBin = "123",
            companyAddress = "Addr",
            serialNumber = "sn",
            kgdId = "kgd",
            dateTime = Instant.now(),
            fiscalSign = "fp",
            ofd = Ofd.KASPI,
            typeOperation = OperationType.BUY,
            items = listOf(
                Item(
                    barcode = null,
                    codeMark = null,
                    name = itemName,
                    count = 1.0,
                    price = 100.0,
                    unit = UnitOfMeasurement.PIECE,
                    sum = 100.0,
                    taxType = null,
                    taxSum = null,
                ),
            ),
            url = "",
            taxesType = null,
            taxesSum = null,
            taken = null,
            change = null,
            totalType = listOf(Payment(PaymentType.CARD, 100.0)),
            totalSum = 100.0,
        )
    }

    private class FakeTranslator(
        private val map: Map<String, String>,
    ) : ItemTranslationRepository {
        override suspend fun translateNames(names: List<String>, targetLanguageTag: String): Map<String, String> = map
    }

    private class FakeSettingsRepository(
        itemLang: ItemTranslationLanguage,
    ) : SettingsRepository {
        override val themeMode: Flow<ThemeMode> = MutableStateFlow(ThemeMode.SYSTEM)
        override val language: Flow<AppLanguage> = MutableStateFlow(AppLanguage.SYSTEM)
        override val itemTranslationLanguage: Flow<ItemTranslationLanguage> = MutableStateFlow(itemLang)
        override val translationProviderConfig: Flow<TranslationProviderConfig> =
            MutableStateFlow(TranslationProviderConfig(provider = TranslationProvider.GOOGLE_TRANSLATE))
        override val logLevel: Flow<LogLevel> = MutableStateFlow(LogLevel.ERROR)
        override val afterScanAction: Flow<AfterScanAction> = MutableStateFlow(AfterScanAction.OPEN_RECEIPT)
        override val colorSource: Flow<ColorSource> = MutableStateFlow(ColorSource.DYNAMIC)
        override val accentColor: Flow<AccentColor> = MutableStateFlow(AccentColor.PURPLE)
        override val mapProvider: Flow<MapProvider> = MutableStateFlow(MapProvider.GOOGLE)
        override val hapticsEnabled: Flow<Boolean> = MutableStateFlow(true)
        override val analyticsPendingPromptEnabled: Flow<Boolean> = MutableStateFlow(true)
        override val displayCurrency: Flow<DisplayCurrency> = MutableStateFlow(DisplayCurrency.KZT)
        override val receiptListSortOrder: Flow<ReceiptListSortOrder> = MutableStateFlow(ReceiptListSortOrder.DEFAULT)
        override val receiptGroupMode: Flow<ReceiptGroupMode> = MutableStateFlow(ReceiptGroupMode.NONE)

        override suspend fun setThemeMode(mode: ThemeMode) = Unit
        override suspend fun setLanguage(language: AppLanguage) = Unit
        override suspend fun setItemTranslationLanguage(language: ItemTranslationLanguage) = Unit
        override suspend fun setTranslationProvider(provider: TranslationProvider) = Unit
        override suspend fun setLibreTranslateEndpoint(endpoint: String) = Unit
        override suspend fun setOpenAiApiKey(apiKey: String) = Unit
        override suspend fun setGeminiApiKey(apiKey: String) = Unit
        override suspend fun setOpenAiModel(model: String) = Unit
        override suspend fun setGeminiModel(model: String) = Unit
        override suspend fun setLogLevel(level: LogLevel) = Unit
        override suspend fun setAfterScanAction(action: AfterScanAction) = Unit
        override suspend fun setColorSource(source: ColorSource) = Unit
        override suspend fun setAccentColor(color: AccentColor) = Unit
        override suspend fun setMapProvider(provider: MapProvider) = Unit
        override suspend fun setHapticsEnabled(enabled: Boolean) = Unit
        override suspend fun setAnalyticsPendingPromptEnabled(enabled: Boolean) = Unit
        override suspend fun setDisplayCurrency(currency: DisplayCurrency) = Unit
        override suspend fun setReceiptListSortOrder(order: ReceiptListSortOrder) = Unit
        override suspend fun setReceiptGroupMode(mode: ReceiptGroupMode) = Unit
    }

    private object NoOpLogger : AppLogger {
        override fun debug(tag: String, message: String, throwable: Throwable?) = Unit
        override fun error(tag: String, message: String, throwable: Throwable?) = Unit
    }
}
