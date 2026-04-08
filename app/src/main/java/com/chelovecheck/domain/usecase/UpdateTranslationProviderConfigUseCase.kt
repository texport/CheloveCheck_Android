package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.repository.SettingsRepository
import javax.inject.Inject

class UpdateTranslationProviderConfigUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend fun setLibreEndpoint(endpoint: String) = repository.setLibreTranslateEndpoint(endpoint)
    suspend fun setOpenAiApiKey(apiKey: String) = repository.setOpenAiApiKey(apiKey)
    suspend fun setGeminiApiKey(apiKey: String) = repository.setGeminiApiKey(apiKey)
    suspend fun setOpenAiModel(model: String) = repository.setOpenAiModel(model)
    suspend fun setGeminiModel(model: String) = repository.setGeminiModel(model)
}
