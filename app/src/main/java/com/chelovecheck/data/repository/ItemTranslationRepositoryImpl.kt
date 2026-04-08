package com.chelovecheck.data.repository

import com.chelovecheck.data.remote.HttpClient
import com.chelovecheck.domain.logging.AppLogger
import com.chelovecheck.domain.repository.ItemTranslationRepository
import com.chelovecheck.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

@Singleton
class ItemTranslationRepositoryImpl @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val httpClient: HttpClient,
    private val json: Json,
    private val logger: AppLogger,
) : ItemTranslationRepository {
    private val cache = mutableMapOf<String, String>()

    override suspend fun translateNames(names: List<String>, targetLanguageTag: String): Map<String, String> {
        val config = settingsRepository.translationProviderConfig.first()
        logger.debug(
            "ItemTranslationRepo",
            "Provider=${config.provider}, target=$targetLanguageTag, items=${names.size}",
        )
        val deduplicated = names.distinct()
        val results = mutableMapOf<String, String>()
        deduplicated.forEach { name ->
            val cacheKey = "${config.provider}|$targetLanguageTag|$name"
            val cached = cache[cacheKey]
            if (!cached.isNullOrBlank()) {
                results[name] = cached
                return@forEach
            }
            val translated = runCatching {
                translateGoogle(name, targetLanguageTag)
            }.onFailure { error ->
                logger.error("ItemTranslationRepo", "Provider request failed for one item", error)
            }.getOrNull()
            if (!translated.isNullOrBlank()) {
                cache[cacheKey] = translated
                results[name] = translated
            }
        }
        logger.debug("ItemTranslationRepo", "Translated ${results.size}/${deduplicated.size} unique items")
        return results
    }

    private suspend fun translateGoogle(text: String, target: String): String? {
        val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$target&dt=t&q=${java.net.URLEncoder.encode(text, "UTF-8")}"
        val response = httpClient.get(url)
        if (response.code !in 200..299) {
            logger.error("ItemTranslationRepo", "GoogleTranslate HTTP ${response.code}")
            return null
        }
        val parsed = json.parseToJsonElement(response.body).jsonArray
        return parsed.firstOrNull()?.jsonArray?.firstOrNull()?.jsonArray?.firstOrNull()?.jsonPrimitive?.contentOrNull
    }

}
