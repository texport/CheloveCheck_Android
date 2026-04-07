package com.chelovecheck.data.analytics.pipeline

import com.chelovecheck.domain.model.CategoryCandidate
import com.chelovecheck.domain.model.CategoryPrediction
import com.chelovecheck.domain.repository.CategoryRepository
import com.chelovecheck.domain.rollup.coicopAnalyticsBucketId
import com.chelovecheck.domain.utils.ItemNameNormalizer
import kotlin.math.max
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Longest-substring match against COICOP names and aliases (**ru, en, kk** from loaded COICOP data).
 * Receipt lines are often code-mixed; matching uses normalized tokens from every available language.
 */
@Singleton
class LexicalAliasMatcher @Inject constructor(
    private val categoryRepository: CategoryRepository,
) {
    private val mutex = Mutex()
    private var entries: List<LexicalEntry>? = null

    private data class LexicalEntry(val pattern: String, val categoryId: String)

    suspend fun match(normalizedItemName: String): CategoryPrediction? {
        ensureEntries()
        val entries = this.entries ?: return null
        if (normalizedItemName.isBlank()) return null
        val scores = LinkedHashMap<String, Float>()
        for (entry in entries) {
            if (entry.pattern.isBlank()) continue
            val matched = when {
                normalizedItemName.contains(entry.pattern) -> entry.pattern.length.toFloat()
                entry.pattern.contains(normalizedItemName) && normalizedItemName.length >= 3 ->
                    normalizedItemName.length.toFloat()
                else -> 0f
            }
            if (matched <= 0f) continue
            val prev = scores[entry.categoryId] ?: 0f
            if (matched > prev) scores[entry.categoryId] = matched
        }
        if (scores.isEmpty()) return null
        val map = categoryRepository.getAllCategories().associateBy { it.id }
        val bucketScores = LinkedHashMap<String, Float>()
        for ((catId, score) in scores) {
            val b = coicopAnalyticsBucketId(catId, map)
            bucketScores[b] = max(bucketScores[b] ?: 0f, score)
        }
        val sorted = bucketScores.entries.sortedByDescending { it.value }
        val adjusted = applyDrinkTieBreak(sorted, normalizedItemName)
        val top = adjusted[0]
        val second = adjusted.getOrNull(1)
        val margin = if (second != null) top.value - second.value else top.value
        val isCertain = top.value >= MIN_SCORE_CERTAIN && margin >= MIN_MARGIN
        val candidates = adjusted.take(MAX_CANDIDATES).map { CategoryCandidate(it.key, it.value / 20f) }
        return CategoryPrediction(
            categoryId = top.key,
            confidence = (top.value / 20f).coerceIn(0f, 1f),
            candidates = candidates,
            isCertain = isCertain,
        )
    }

    private suspend fun ensureEntries() {
        if (entries != null) return
        mutex.withLock {
            if (entries != null) return
            val all = categoryRepository.getAllCategories()
            val list = mutableListOf<LexicalEntry>()
            for (cat in all) {
                // Level-1 division titles (e.g. long COICOP headings) cause false substring matches.
                if (cat.level >= 2) {
                    for (name in cat.names.values) {
                        val p = ItemNameNormalizer.normalizeForMatch(name)
                        if (p.length >= MIN_PATTERN_LEN) list.add(LexicalEntry(p, cat.id))
                    }
                }
                for (aliases in cat.aliases.values) {
                    for (a in aliases) {
                        val p = ItemNameNormalizer.normalizeForMatch(a)
                        if (p.length >= MIN_PATTERN_LEN) list.add(LexicalEntry(p, cat.id))
                    }
                }
            }
            entries = list.sortedByDescending { it.pattern.length }
        }
    }

    private fun applyDrinkTieBreak(
        sorted: List<Map.Entry<String, Float>>,
        normalizedItemName: String,
    ): List<Map.Entry<String, Float>> {
        if (sorted.isEmpty()) return sorted
        if (!drinkToken.containsMatchIn(normalizedItemName)) return sorted
        val drinkEntry = sorted.firstOrNull { it.key == "01.2" } ?: return sorted
        val top = sorted.first()
        if (top.key == "01.2") return sorted
        if (top.value - drinkEntry.value > DRINK_TIE_BREAK_MAX_GAP) return sorted
        return listOf(drinkEntry) + sorted.filterNot { it.key == "01.2" }
    }

    companion object {
        private const val MIN_PATTERN_LEN = 3
        private const val MIN_SCORE_CERTAIN = 4f
        // Slightly lower margin keeps strong lexical matches from being downgraded on mixed-language lines.
        private const val MIN_MARGIN = 1.0f
        private const val MAX_CANDIDATES = 6
        private const val DRINK_TIE_BREAK_MAX_GAP = 1.1f
        private val drinkToken = Regex(
            """(?i)(coca[\s-]?cola|cola|fanta|sprite|сок|шырын|нектар|чай|кофе|какао|rich|tess|пиала|мин\s?вода)""",
        )
    }
}
