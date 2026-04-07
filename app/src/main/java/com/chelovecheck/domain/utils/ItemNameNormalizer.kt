package com.chelovecheck.domain.utils

/**
 * Receipt line cleanup: shared pipeline for UI ([cleanDisplayName]) and ML ([normalizeForMatch]).
 */
object ItemNameNormalizer {
    private val multiSpace = Regex("\\s+")
    private val qtySuffix = Regex("\\s*(?:[xх×*]\\s*\\d+|\\d+\\s*[xх×*])\\s*$", RegexOption.IGNORE_CASE)
    private val unitSuffix = Regex(
        "\\s*\\(?\\d+\\s*(?:шт|pcs|pc|уп|упак|уп\\.|pack)\\)?\\s*$",
        RegexOption.IGNORE_CASE,
    )
    private val numberToken = Regex("\\b\\d+(?:[\\.,]\\d+)?\\b")
    private val numberWithUnit = Regex(
        "\\b\\d+(?:[\\.,]\\d+)?\\s*(?:г|кг|л|мл|mg|kg|g|l|ml|шт|pcs|pc|уп|упак|уп\\.|pack|дана|dana)\\b",
        RegexOption.IGNORE_CASE,
    )
    private val qtyToken = Regex("\\b(?:[xх×*]\\s*\\d+|\\d+\\s*[xх×*])\\b", RegexOption.IGNORE_CASE)
    private val nonLetterDigit = Regex("[^\\p{L}\\p{N}]+")
    /** XML/OFD-style fragments e.g. `<A>`, `</tag>`, `<1>`. */
    private val angleTag = Regex("<\\/?[^>\\n]{0,48}>")
    private val leadingPatterns = listOf(
        Regex("""(?i)^\s*\d+[\.)]\s*"""),
        Regex("""(?i)^\s*№\s*\d+\s*"""),
        Regex("""(?i)^\s*\d+\s*[-–—]\s+"""),
    )

    /**
     * Strips non-product noise (tags, list numbering, normalizes whitespace) before display-specific rules.
     */
    fun preprocessReceiptLineName(raw: String): String {
        var text = raw.trim().replace(Regex("[\\r\\n]+"), " ")
        text = angleTag.replace(text, "")
        var prev: String
        do {
            prev = text
            for (p in leadingPatterns) {
                text = p.replaceFirst(text, "")
            }
            text = text.trimStart()
        } while (text != prev && text.isNotEmpty())
        text = multiSpace.replace(text, " ").trim()
        return text
    }

    fun cleanDisplayName(raw: String): String {
        var text = preprocessReceiptLineName(raw)
        text = qtySuffix.replace(text, "")
        text = unitSuffix.replace(text, "")
        return text.trim()
    }

    fun normalizeForMatch(raw: String): String {
        var text = preprocessReceiptLineName(raw).lowercase()
        text = text.replace('×', 'x')
        text = text.replace('*', 'x')
        text = text.replace(nonLetterDigit, " ")
        text = text.replace(qtyToken, " ")
        text = text.replace(numberWithUnit, " ")
        text = text.replace(numberToken, " ")
        text = text.replace(multiSpace, " ").trim()
        return text
    }

    fun normalizeTokens(raw: String): Set<String> {
        val normalized = normalizeForMatch(raw)
        if (normalized.isBlank()) return emptySet()
        return normalized.split(" ").filter { it.isNotBlank() }.toSet()
    }
}
