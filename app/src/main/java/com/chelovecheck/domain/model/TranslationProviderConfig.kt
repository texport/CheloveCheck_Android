package com.chelovecheck.domain.model

data class TranslationProviderConfig(
    val provider: TranslationProvider = TranslationProvider.GOOGLE_TRANSLATE,
    val libreTranslateEndpoint: String = "https://libretranslate.com/translate",
    val openAiApiKey: String = "",
    val geminiApiKey: String = "",
    val openAiModel: String = "gpt-4o-mini",
    val geminiModel: String = "gemini-2.0-flash",
)
