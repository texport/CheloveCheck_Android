package com.chelovecheck.data.analytics.pipeline

import com.chelovecheck.data.analytics.ItemCategoryCacheStore
import com.chelovecheck.domain.model.CategoryPrediction
import com.chelovecheck.domain.model.RetailClassificationContext
import com.chelovecheck.domain.logging.AppLogger
import com.chelovecheck.domain.repository.CategoryRepository
import com.chelovecheck.domain.repository.ReceiptItemClassifier
import com.chelovecheck.domain.repository.RetailCategoryHintRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class CategoryPredictionPipeline @Inject constructor(
    private val overrideMatcher: CategoryOverrideMatcher,
    private val lexicalMatcher: LexicalAliasMatcher,
    private val rollupEmbedding: RollupEmbeddingNearestClassifier,
    private val predictionCacheStore: ItemCategoryCacheStore,
    private val categoryRepository: CategoryRepository,
    private val retailHintRepository: RetailCategoryHintRepository,
    private val postProcessor: CategoryPredictionPostProcessor,
    private val logger: AppLogger,
) : ReceiptItemClassifier {
    private val memoryMutex = Mutex()
    private val predictionMemory = LinkedHashMap<String, CategoryPrediction>(MEMORY_CACHE_SIZE, 0.75f, true)
    private val inputNormalizationStep = InputNormalizationStep()
    private val ruleBasedPredictionStep = RuleBasedPredictionStep(overrideMatcher)
    private val overrideResolutionStep = OverrideResolutionStep(ruleBasedPredictionStep)
    private val embeddingClassificationStep = EmbeddingClassificationStep(lexicalMatcher, rollupEmbedding, postProcessor)
    private val resultAssemblyStep = ResultAssemblyStep(
        postProcessor,
        RetailPriorAdjustmentStep(postProcessor),
        ConfidencePolicyStep(postProcessor),
    )

    override suspend fun classify(
        name: String,
        retailContext: RetailClassificationContext?,
    ): CategoryPrediction {
        val input = inputNormalizationStep.run(name, retailContext)
        val normalized = input.normalizedName
        val displayName = input.displayName
        val key = input.cacheKey
        return withContext(Dispatchers.Default) {
            if (normalized.isBlank()) {
                return@withContext emptyPrediction()
            }
            val categoriesMap = categoryRepository.getAllCategories().associateBy { it.id }
            val retailHint = retailContext?.networkName?.let { retailHintRepository.getHint(it) }
            logger.debug(TAG, "start raw='$displayName' norm='$normalized' key='$key'")
            overrideResolutionStep.resolve(normalized, retailHint)?.let { rule ->
                return@withContext finalizeAndCache(
                    key = key,
                    normalizedName = normalized,
                    raw = rule.prediction,
                    retailContext = retailContext,
                    hint = retailHint,
                    skipRetailPrior = rule.skipRetailPrior,
                    source = rule.source,
                )
            }
            memoryMutex.withLock { predictionMemory[key] }?.let { return@withContext it }
            predictionCacheStore.get(key)?.let { persisted ->
                val out = postProcessor.clampPrediction(persisted)
                cacheMemory(key, out)
                logger.debug(
                    TAG,
                    "final key=$key source=persisted_cache category=${out.categoryId} certain=${out.isCertain} " +
                        "confidence=${out.confidence} candidates=${formatTopCandidates(out)}",
                )
                return@withContext out
            }
            embeddingClassificationStep.classifyFromLexicalOrNull(normalized, categoriesMap)?.let { lex ->
                return@withContext finalizeAndCache(
                    key = key,
                    normalizedName = normalized,
                    raw = lex.prediction,
                    retailContext = retailContext,
                    hint = retailHint,
                    skipRetailPrior = false,
                    source = lex.source,
                )
            }
            val rolled = embeddingClassificationStep.classifyEmbeddingOnly(normalized)
            finalizeAndCache(
                key = key,
                normalizedName = normalized,
                raw = rolled,
                retailContext = retailContext,
                hint = retailHint,
                skipRetailPrior = false,
                source = "embedding_only",
            )
        }
    }

    suspend fun invalidateMemoryForNormalizedName(normalized: String) {
        if (normalized.isBlank()) return
        memoryMutex.withLock {
            val prefix = "$normalized|"
            val toRemove = predictionMemory.keys.filter { k ->
                k == normalized || k.startsWith(prefix)
            }
            toRemove.forEach { predictionMemory.remove(it) }
        }
    }

    private suspend fun finalizeAndCache(
        key: String,
        normalizedName: String,
        raw: CategoryPrediction,
        retailContext: RetailClassificationContext?,
        hint: com.chelovecheck.domain.model.RetailCategoryHint?,
        skipRetailPrior: Boolean,
        source: String,
    ): CategoryPrediction {
        val categoriesMap = categoryRepository.getAllCategories().associateBy { it.id }
        val assembled = resultAssemblyStep.assemble(
            raw = raw,
            normalizedName = normalizedName,
            retailContext = retailContext,
            hint = hint,
            skipRetailPrior = skipRetailPrior,
            categories = categoriesMap,
        )
        val out = assembled.output
        cachePrediction(key, out)
        predictionCacheStore.put(key, out)
        logger.debug(
            TAG,
            "final key=$key source=$source network=${retailContext?.networkName ?: "__unknown__"} " +
                "hintApplied=${!skipRetailPrior && retailContext != null && hint != null} " +
                "prePrior=${formatTopCandidates(assembled.prePrior)} postPrior=${formatTopCandidates(assembled.domainAdjusted)} " +
                "category=${out.categoryId} certain=${out.isCertain} confidence=${out.confidence} " +
                "gateReason=${assembled.gateReason ?: "pass"}",
        )
        return out
    }

    private fun emptyPrediction() = CategoryPrediction(
        categoryId = null,
        confidence = 0f,
        candidates = emptyList(),
        isCertain = false,
    )

    private suspend fun cachePrediction(key: String, prediction: CategoryPrediction) {
        memoryMutex.withLock {
            predictionMemory[key] = prediction
            if (predictionMemory.size > MEMORY_CACHE_SIZE) {
                val it = predictionMemory.entries.iterator()
                if (it.hasNext()) it.next()
                it.remove()
            }
        }
    }

    private suspend fun cacheMemory(key: String, prediction: CategoryPrediction) {
        memoryMutex.withLock {
            predictionMemory[key] = prediction
        }
    }

    companion object {
        private const val TAG = "CategoryPipeline"
        private const val MEMORY_CACHE_SIZE = 2048

        fun predictionCacheKey(normalizedName: String, retailContext: RetailClassificationContext?): String {
            val net = retailContext?.networkName?.takeIf { it.isNotBlank() } ?: "__unknown__"
            return "$normalizedName|$net"
        }

        private fun formatTopCandidates(prediction: CategoryPrediction): String {
            return prediction.candidates.take(3).joinToString { "${it.categoryId}:${"%.3f".format(it.score.toDouble())}" }
        }
    }
}
