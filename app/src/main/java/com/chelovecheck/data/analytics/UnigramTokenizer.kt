package com.chelovecheck.data.analytics

import java.text.Normalizer
import kotlin.math.max

data class UnigramTokenizerConfig(
    val bosId: Int,
    val eosId: Int,
    val unkId: Int,
    val padId: Int,
    val maxLength: Int,
)

class UnigramTokenizer(
    private val tokensByFirstChar: Map<Char, List<TokenEntry>>,
    private val tokenIdByText: Map<String, Int>,
    private val tokenScores: DoubleArray,
    private val config: UnigramTokenizerConfig,
) {
    data class TokenEntry(
        val text: String,
        val id: Int,
        val score: Double,
    )

    fun encodePair(
        premise: String,
        hypothesis: String,
        maxLength: Int = config.maxLength,
    ): TokenizerResult {
        val tokensA = tokenize(premise)
        val tokensB = tokenize(hypothesis)

        val maxTokens = maxLength - 4
        val truncatedA = tokensA.toMutableList()
        val truncatedB = tokensB.toMutableList()
        while (truncatedA.size + truncatedB.size > maxTokens && (truncatedA.isNotEmpty() || truncatedB.isNotEmpty())) {
            if (truncatedA.size >= truncatedB.size && truncatedA.isNotEmpty()) {
                truncatedA.removeAt(truncatedA.lastIndex)
            } else if (truncatedB.isNotEmpty()) {
                truncatedB.removeAt(truncatedB.lastIndex)
            } else {
                break
            }
        }

        val inputIds = LongArray(maxLength) { config.padId.toLong() }
        val attentionMask = LongArray(maxLength) { 0L }

        var index = 0
        index = writeToken(inputIds, attentionMask, index, config.bosId)
        index = writeTokens(inputIds, attentionMask, index, truncatedA)
        index = writeToken(inputIds, attentionMask, index, config.eosId)
        index = writeToken(inputIds, attentionMask, index, config.eosId)
        index = writeTokens(inputIds, attentionMask, index, truncatedB)
        writeToken(inputIds, attentionMask, index, config.eosId)

        return TokenizerResult(
            inputIds = inputIds,
            attentionMask = attentionMask,
            tokenTypeIds = null,
        )
    }

    private fun writeTokens(
        inputIds: LongArray,
        mask: LongArray,
        start: Int,
        tokens: List<Int>,
    ): Int {
        var index = start
        for (token in tokens) {
            if (index >= inputIds.size) break
            index = writeToken(inputIds, mask, index, token)
        }
        return index
    }

    private fun writeToken(
        inputIds: LongArray,
        mask: LongArray,
        index: Int,
        tokenId: Int,
    ): Int {
        if (index >= inputIds.size) return index
        inputIds[index] = tokenId.toLong()
        mask[index] = 1L
        return index + 1
    }

    private fun tokenize(text: String): List<Int> {
        val normalized = normalize(text)
        if (normalized.isBlank()) return emptyList()
        val parts = normalized.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (parts.isEmpty()) return emptyList()
        val tokens = mutableListOf<Int>()
        for (part in parts) {
            val piece = "▁$part"
            tokens.addAll(encodePiece(piece))
        }
        return tokens
    }

    private fun normalize(text: String): String {
        val cleaned = buildString(text.length) {
            for (ch in text) {
                if (ch == '\u0000' || ch == '\uFFFD' || Character.isISOControl(ch)) continue
                append(ch)
            }
        }
        return Normalizer.normalize(cleaned, Normalizer.Form.NFKC)
    }

    private fun encodePiece(piece: String): List<Int> {
        if (piece.isEmpty()) return emptyList()
        val n = piece.length
        val bestScore = DoubleArray(n + 1) { Double.NEGATIVE_INFINITY }
        val bestPrev = IntArray(n + 1) { -1 }
        val bestToken = IntArray(n + 1) { -1 }
        bestScore[0] = 0.0
        val unkScore = tokenScores[config.unkId].takeIf { it.isFinite() } ?: -10.0

        for (i in 0 until n) {
            val base = bestScore[i]
            if (base == Double.NEGATIVE_INFINITY) continue
            var matched = false
            val list = tokensByFirstChar[piece[i]]
            if (list != null) {
                for (token in list) {
                    if (piece.startsWith(token.text, i)) {
                        val j = i + token.text.length
                        val score = base + token.score
                        if (score > bestScore[j]) {
                            bestScore[j] = score
                            bestPrev[j] = i
                            bestToken[j] = token.id
                        }
                        matched = true
                    }
                }
            }
            if (!matched) {
                val j = i + 1
                val score = base + unkScore
                if (score > bestScore[j]) {
                    bestScore[j] = score
                    bestPrev[j] = i
                    bestToken[j] = config.unkId
                }
            }
        }

        if (bestScore[n] == Double.NEGATIVE_INFINITY) {
            return List(n) { config.unkId }
        }
        val result = ArrayList<Int>(max(1, n / 2))
        var index = n
        while (index > 0) {
            val token = bestToken[index]
            val prev = bestPrev[index]
            if (token < 0 || prev < 0) {
                result.clear()
                result.addAll(List(n) { config.unkId })
                break
            }
            result.add(token)
            index = prev
        }
        result.reverse()
        return result
    }
}
