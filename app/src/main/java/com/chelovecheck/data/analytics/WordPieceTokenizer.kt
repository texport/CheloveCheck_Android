package com.chelovecheck.data.analytics

import java.text.Normalizer

data class TokenizerConfig(
    val doLowerCase: Boolean,
    val clsToken: String,
    val sepToken: String,
    val padToken: String,
    val unkToken: String,
    val maxLength: Int,
)

data class TokenizerResult(
    val inputIds: LongArray,
    val attentionMask: LongArray,
    val tokenTypeIds: LongArray? = null,
)

class WordPieceTokenizer(
    private val vocab: Map<String, Int>,
    private val config: TokenizerConfig,
) {
    private val clsId = vocab[config.clsToken] ?: 101
    private val sepId = vocab[config.sepToken] ?: 102
    private val padId = vocab[config.padToken] ?: 0
    private val unkId = vocab[config.unkToken] ?: 100

    fun encode(text: String, maxLength: Int = config.maxLength): TokenizerResult {
        val tokens = tokenize(text)
        val wordPieceTokens = tokens.flatMap { wordPiece(it) }

        val limited = wordPieceTokens.take(maxLength - 2)
        val inputIds = LongArray(maxLength) { padId.toLong() }
        val attentionMask = LongArray(maxLength) { 0L }

        var index = 0
        inputIds[index] = clsId.toLong()
        attentionMask[index] = 1L
        index += 1

        for (token in limited) {
            if (index >= maxLength - 1) break
            inputIds[index] = (vocab[token] ?: unkId).toLong()
            attentionMask[index] = 1L
            index += 1
        }

        if (index < maxLength) {
            inputIds[index] = sepId.toLong()
            attentionMask[index] = 1L
        }

        return TokenizerResult(inputIds = inputIds, attentionMask = attentionMask)
    }

    fun encodePair(
        premise: String,
        hypothesis: String,
        maxLength: Int = config.maxLength,
    ): TokenizerResult {
        val tokensA = tokenize(premise).flatMap { wordPiece(it) }.toMutableList()
        val tokensB = tokenize(hypothesis).flatMap { wordPiece(it) }.toMutableList()

        while (tokensA.size + tokensB.size > maxLength - 3) {
            if (tokensA.size >= tokensB.size && tokensA.isNotEmpty()) {
                tokensA.removeAt(tokensA.lastIndex)
            } else if (tokensB.isNotEmpty()) {
                tokensB.removeAt(tokensB.lastIndex)
            } else {
                break
            }
        }

        val inputIds = LongArray(maxLength) { padId.toLong() }
        val attentionMask = LongArray(maxLength) { 0L }
        val tokenTypeIds = LongArray(maxLength) { 0L }

        var index = 0
        inputIds[index] = clsId.toLong()
        attentionMask[index] = 1L
        tokenTypeIds[index] = 0L
        index += 1

        for (token in tokensA) {
            if (index >= maxLength - 1) break
            inputIds[index] = (vocab[token] ?: unkId).toLong()
            attentionMask[index] = 1L
            tokenTypeIds[index] = 0L
            index += 1
        }

        if (index < maxLength) {
            inputIds[index] = sepId.toLong()
            attentionMask[index] = 1L
            tokenTypeIds[index] = 0L
            index += 1
        }

        for (token in tokensB) {
            if (index >= maxLength - 1) break
            inputIds[index] = (vocab[token] ?: unkId).toLong()
            attentionMask[index] = 1L
            tokenTypeIds[index] = 1L
            index += 1
        }

        if (index < maxLength) {
            inputIds[index] = sepId.toLong()
            attentionMask[index] = 1L
            tokenTypeIds[index] = 1L
        }

        return TokenizerResult(
            inputIds = inputIds,
            attentionMask = attentionMask,
            tokenTypeIds = tokenTypeIds,
        )
    }

    private fun tokenize(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val cleaned = cleanText(text)
        val tokens = mutableListOf<String>()
        val buffer = StringBuilder()
        fun flush() {
            if (buffer.isNotEmpty()) {
                tokens.add(buffer.toString())
                buffer.clear()
            }
        }

        for (ch in cleaned) {
            when {
                ch.isWhitespace() -> flush()
                isPunctuation(ch) -> {
                    flush()
                    tokens.add(ch.toString())
                }
                else -> buffer.append(ch)
            }
        }
        flush()

        return tokens.map { token ->
            val normalized = if (config.doLowerCase) token.lowercase() else token
            if (config.doLowerCase) stripAccents(normalized) else normalized
        }
    }

    private fun cleanText(text: String): String {
        val builder = StringBuilder()
        for (ch in text) {
            if (ch == '\u0000' || ch == '\uFFFD' || Character.isISOControl(ch)) continue
            builder.append(ch)
        }
        return builder.toString()
    }

    private fun stripAccents(text: String): String {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
        val builder = StringBuilder()
        for (ch in normalized) {
            if (Character.getType(ch) != Character.NON_SPACING_MARK.toInt()) {
                builder.append(ch)
            }
        }
        return builder.toString()
    }

    private fun isPunctuation(ch: Char): Boolean {
        val type = Character.getType(ch)
        return type == Character.CONNECTOR_PUNCTUATION.toInt() ||
            type == Character.DASH_PUNCTUATION.toInt() ||
            type == Character.START_PUNCTUATION.toInt() ||
            type == Character.END_PUNCTUATION.toInt() ||
            type == Character.OTHER_PUNCTUATION.toInt() ||
            type == Character.MATH_SYMBOL.toInt() ||
            type == Character.CURRENCY_SYMBOL.toInt() ||
            type == Character.MODIFIER_SYMBOL.toInt() ||
            type == Character.OTHER_SYMBOL.toInt()
    }

    private fun wordPiece(token: String): List<String> {
        if (token.isEmpty()) return emptyList()
        if (vocab.containsKey(token)) return listOf(token)

        val chars = token.toCharArray()
        val subTokens = mutableListOf<String>()
        var start = 0
        while (start < chars.size) {
            var end = chars.size
            var found = false
            while (start < end) {
                val piece = String(chars, start, end - start)
                val candidate = if (start == 0) piece else "##$piece"
                if (vocab.containsKey(candidate)) {
                    subTokens.add(candidate)
                    start = end
                    found = true
                    break
                }
                end -= 1
            }
            if (!found) {
                return listOf(config.unkToken)
            }
        }
        return subTokens
    }
}
