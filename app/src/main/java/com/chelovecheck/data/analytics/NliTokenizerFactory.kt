package com.chelovecheck.data.analytics

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.contentOrNull

@Singleton
class NliTokenizerFactory @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private var cached: UnigramTokenizer? = null

    fun get(): UnigramTokenizer {
        val existing = cached
        if (existing != null) return existing
        val tokenizerJson = context.assets.open(TOKENIZER_JSON).bufferedReader().use { it.readText() }
        val configJson = context.assets.open(TOKENIZER_CONFIG).bufferedReader().use { it.readText() }
        val root = json.parseToJsonElement(tokenizerJson).jsonObject
        val model = root["model"]?.jsonObject ?: error("tokenizer.json missing model")
        val vocab = model["vocab"]?.jsonArray ?: error("tokenizer.json missing vocab")
        val unkId = model["unk_id"]?.jsonPrimitive?.int ?: 0
        val tokenEntries = parseVocab(vocab)
        val tokenIdByText = tokenEntries.associate { it.text to it.id }
        val tokenScores = DoubleArray(tokenEntries.size) { index -> tokenEntries[index].score }
        val config = parseConfig(configJson, tokenIdByText, unkId)
        val tokensByFirstChar = buildTokenIndex(tokenEntries, config)
        return UnigramTokenizer(tokensByFirstChar, tokenIdByText, tokenScores, config).also { cached = it }
    }

    private fun parseVocab(vocab: JsonArray): List<UnigramTokenizer.TokenEntry> {
        val list = ArrayList<UnigramTokenizer.TokenEntry>(vocab.size)
        vocab.forEachIndexed { index, element ->
            val entry = element.jsonArray
            val token = entry[0].jsonPrimitive.contentOrNull ?: ""
            val score = entry[1].jsonPrimitive.contentOrNull?.toDoubleOrNull() ?: 0.0
            list.add(UnigramTokenizer.TokenEntry(token, index, score))
        }
        return list
    }

    private fun parseConfig(
        payload: String,
        tokenIdByText: Map<String, Int>,
        unkId: Int,
    ): UnigramTokenizerConfig {
        val root = json.parseToJsonElement(payload).jsonObject
        val maxLength = root["model_max_length"]?.jsonPrimitive?.int ?: 512
        val bosToken = root["bos_token"]?.jsonPrimitive?.contentOrNull ?: "<s>"
        val eosToken = root["eos_token"]?.jsonPrimitive?.contentOrNull ?: "</s>"
        val padToken = root["pad_token"]?.jsonPrimitive?.contentOrNull ?: "<pad>"
        val bosId = tokenIdByText[bosToken] ?: 0
        val eosId = tokenIdByText[eosToken] ?: 2
        val padId = tokenIdByText[padToken] ?: 1
        return UnigramTokenizerConfig(
            bosId = bosId,
            eosId = eosId,
            unkId = unkId,
            padId = padId,
            maxLength = maxLength,
        )
    }

    private fun buildTokenIndex(
        tokens: List<UnigramTokenizer.TokenEntry>,
        config: UnigramTokenizerConfig,
    ): Map<Char, List<UnigramTokenizer.TokenEntry>> {
        val skip = setOf(config.bosId, config.eosId, config.padId, config.unkId)
        return tokens
            .asSequence()
            .filter { it.id !in skip && it.text.isNotEmpty() }
            .groupBy { it.text[0] }
            .mapValues { (_, value) -> value.sortedByDescending { it.text.length } }
    }

    private companion object {
        private const val TOKENIZER_JSON = "ml/nli/tokenizer.json"
        private const val TOKENIZER_CONFIG = "ml/nli/tokenizer_config.json"
    }
}
