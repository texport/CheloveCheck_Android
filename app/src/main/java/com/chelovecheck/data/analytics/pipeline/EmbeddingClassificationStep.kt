package com.chelovecheck.data.analytics.pipeline

import com.chelovecheck.domain.model.CategoryPrediction
import com.chelovecheck.domain.model.CoicopCategory

/**
 * Step 2.2 (plan): lexical + rollup embedding merge path.
 */
internal class EmbeddingClassificationStep(
    private val lexicalMatcher: LexicalAliasMatcher,
    private val rollupEmbedding: RollupEmbeddingNearestClassifier,
    private val postProcessor: CategoryPredictionPostProcessor,
) {
    suspend fun classifyFromLexicalOrNull(
        normalized: String,
        categories: Map<String, CoicopCategory>,
    ): LexicalClassificationResult? {
        val lexical = lexicalMatcher.match(normalized) ?: return null
        val merged = if (lexical.isCertain) {
            lexical
        } else {
            val rolled = rollupEmbedding.classify(normalized)
            postProcessor.mergeLexicalAndEmbedding(lexical, rolled, categories)
        }
        val source = if (lexical.isCertain) "lexical_certain" else "lexical_plus_embedding"
        return LexicalClassificationResult(merged, source)
    }

    suspend fun classifyEmbeddingOnly(normalized: String): CategoryPrediction =
        rollupEmbedding.classify(normalized)

    data class LexicalClassificationResult(
        val prediction: CategoryPrediction,
        val source: String,
    )
}
