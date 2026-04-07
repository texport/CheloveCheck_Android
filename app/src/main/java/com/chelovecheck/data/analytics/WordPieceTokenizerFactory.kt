package com.chelovecheck.data.analytics

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Singleton
class WordPieceTokenizerFactory @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private var cached: WordPieceTokenizer? = null

    fun get(): WordPieceTokenizer {
        val existing = cached
        if (existing != null) return existing
        val vocab = loadVocab()
        val config = loadConfig()
        return WordPieceTokenizer(vocab, config).also { cached = it }
    }

    private fun loadVocab(): Map<String, Int> {
        val lines = context.assets.open("ml/distiluse-base-multilingual-cased-v2/vocab.txt")
            .bufferedReader()
            .use { it.readLines() }
        val map = HashMap<String, Int>(lines.size)
        lines.forEachIndexed { index, token ->
            map[token] = index
        }
        return map
    }

    private fun loadConfig(): TokenizerConfig {
        val payload = context.assets.open("ml/distiluse-base-multilingual-cased-v2/tokenizer_config.json")
            .bufferedReader()
            .use { it.readText() }
        val dto = json.decodeFromString(TokenizerConfigDto.serializer(), payload)
        return TokenizerConfig(
            doLowerCase = dto.doLowerCase ?: false,
            clsToken = dto.clsToken ?: "[CLS]",
            sepToken = dto.sepToken ?: "[SEP]",
            padToken = dto.padToken ?: "[PAD]",
            unkToken = dto.unkToken ?: "[UNK]",
            maxLength = dto.modelMaxLength?.coerceAtMost(64) ?: 64,
        )
    }
}
