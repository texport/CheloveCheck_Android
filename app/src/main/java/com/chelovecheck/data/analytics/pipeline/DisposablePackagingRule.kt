package com.chelovecheck.data.analytics.pipeline

/**
 * One-off plastic bags and similar lines should not be classified as food.
 * Normalized name (see [com.chelovecheck.domain.utils.ItemNameNormalizer.normalizeForMatch]).
 */
object DisposablePackagingRule {
    private val pattern = Regex(
        """(?i)(^|.*\s)(пакет(\s+майк\w*)?|пакет\s+с\s+ручк|одноразов\w*\s+упаковк|пакет\s+п\w+|пакет\s*№|plastic\s+bag|t-?shirt\s+bag|сумка\s+пакет)($|\s.*)""",
    )

    fun matchesNormalized(normalized: String): Boolean =
        normalized.isNotBlank() && pattern.containsMatchIn(normalized)
}
