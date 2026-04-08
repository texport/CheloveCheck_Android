package com.chelovecheck.domain.repository

interface ItemTranslationRepository {
    suspend fun translateNames(
        names: List<String>,
        targetLanguageTag: String,
    ): Map<String, String>
}
