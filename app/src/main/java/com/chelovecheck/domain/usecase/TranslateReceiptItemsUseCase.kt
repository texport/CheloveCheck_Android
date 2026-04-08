package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.ItemTranslationLanguage
import com.chelovecheck.domain.model.Receipt
import com.chelovecheck.domain.logging.AppLogger
import com.chelovecheck.domain.repository.ItemTranslationRepository
import com.chelovecheck.domain.repository.SettingsRepository
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class TranslateReceiptItemsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val translationRepository: ItemTranslationRepository,
    private val logger: AppLogger,
) {
    suspend operator fun invoke(receipt: Receipt): Receipt {
        val languageMode = settingsRepository.itemTranslationLanguage.first()
        val targetTag = resolveTargetTag(languageMode)
        if (targetTag == null) {
            val restored = restoreOriginalNames(receipt)
            logger.debug("TranslateReceiptItems", "OFD_SOURCE mode: restored original names")
            return restored
        }
        val uniqueNames = receipt.items.map { it.name.trim() }.filter { it.isNotEmpty() }.distinct()
        if (uniqueNames.isEmpty()) {
            logger.debug("TranslateReceiptItems", "Skipped: no item names to translate")
            return receipt
        }
        logger.debug(
            "TranslateReceiptItems",
            "Start translation: target=$targetTag, uniqueItems=${uniqueNames.size}",
        )
        val translated = runCatching {
            translationRepository.translateNames(uniqueNames, targetTag)
        }.onFailure { error ->
            logger.error("TranslateReceiptItems", "Translation failed; fallback to source names", error)
        }.getOrDefault(emptyMap())
        if (translated.isEmpty()) {
            logger.debug("TranslateReceiptItems", "No translated items returned; fallback to source names")
            return receipt
        }
        logger.debug(
            "TranslateReceiptItems",
            "Translation done: translated=${translated.size}, source=${uniqueNames.size}",
        )
        return receipt.copy(
            items = receipt.items.map { item ->
                val mapped = translated[item.name.trim()]
                if (mapped.isNullOrBlank()) {
                    item
                } else {
                    item.copy(
                        name = mapped,
                        originalName = item.originalName ?: item.name,
                    )
                }
            },
        )
    }

    private fun restoreOriginalNames(receipt: Receipt): Receipt {
        return receipt.copy(
            items = receipt.items.map { item ->
                val original = item.originalName
                if (original.isNullOrBlank()) item else item.copy(name = original)
            },
        )
    }

    private fun resolveTargetTag(language: ItemTranslationLanguage): String? {
        return when (language) {
            ItemTranslationLanguage.OFD_SOURCE -> null
            ItemTranslationLanguage.SYSTEM -> {
                when (Locale.getDefault().language.lowercase(Locale.ROOT)) {
                    "ru" -> "ru"
                    "kk" -> "kk"
                    "en" -> "en"
                    else -> "en"
                }
            }
            ItemTranslationLanguage.ENGLISH -> "en"
            ItemTranslationLanguage.RUSSIAN -> "ru"
            ItemTranslationLanguage.KAZAKH -> "kk"
        }
    }
}
