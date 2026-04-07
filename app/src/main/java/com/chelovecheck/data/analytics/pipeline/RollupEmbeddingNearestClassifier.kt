package com.chelovecheck.data.analytics.pipeline

import com.chelovecheck.data.analytics.EmbeddingCacheStore
import com.chelovecheck.data.analytics.OnnxSentenceEmbedderProvider
import com.chelovecheck.domain.logging.AppLogger
import com.chelovecheck.domain.model.AnalyticsLoadStage
import com.chelovecheck.domain.model.CategoryCandidate
import com.chelovecheck.domain.model.CategoryPrediction
import com.chelovecheck.domain.model.CoicopCategory
import com.chelovecheck.domain.repository.AnalyticsProgressReporter
import com.chelovecheck.domain.repository.CategoryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.sqrt
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Single embedding index over rollup categories (level 1–2) only.
 *
 * Anchor texts combine **ru / en / kk** names and aliases from COICOP plus **synthetic code-mixed** strings
 * (several languages in one line) so centroid vectors align with typical KZ receipt lines that mix scripts
 * and languages in a single product title.
 */
@Singleton
class RollupEmbeddingNearestClassifier @Inject constructor(
    private val repository: CategoryRepository,
    private val cacheStore: EmbeddingCacheStore,
    private val progressReporter: AnalyticsProgressReporter,
    private val embedderProvider: OnnxSentenceEmbedderProvider,
    private val logger: AppLogger,
) {
    private val mutex = Mutex()
    private var embeddings: Map<String, FloatArray>? = null
    private var categoryMap: Map<String, CoicopCategory>? = null

    suspend fun classify(normalizedName: String): CategoryPrediction {
        ensureLoaded()
        val embedding = embedderProvider.stage1.embed(normalizedName)
        val scores = embeddings.orEmpty().map { (id, vector) ->
            CategoryCandidate(id, dot(embedding, vector))
        }
        val sorted = scores.sortedByDescending { it.score }
        val top = sorted.firstOrNull()
        val second = sorted.drop(1).firstOrNull()
        val confidence = top?.score ?: 0f
        val isCertain = top != null && confidence >= THRESHOLD &&
            (second == null || confidence - second.score >= MARGIN)
        val top3 = sorted.take(3).joinToString { "${it.categoryId}:${"%.3f".format(it.score.toDouble())}" }
        logger.debug(TAG, "rollup_embed top=${top?.categoryId}:${top?.score} certain=$isCertain top3=$top3")
        return CategoryPrediction(
            categoryId = top?.categoryId,
            confidence = confidence,
            candidates = sorted.take(MAX_CANDIDATES),
            isCertain = isCertain,
        )
    }

    private suspend fun ensureLoaded() {
        if (embeddings != null) return
        mutex.withLock {
            if (embeddings != null) return
            cacheStore.read(CACHE_KEY)?.let { cached ->
                embeddings = cached
                categoryMap = repository.getAllCategories().associateBy { it.id }
                return
            }
            progressReporter.report(AnalyticsLoadStage.BUILDING_INDEX)
            val all = repository.getRollupCategories()
            categoryMap = repository.getAllCategories().associateBy { it.id }
            val map = LinkedHashMap<String, FloatArray>(all.size)
            for (category in all) {
                val texts = buildTexts(category)
                val vectors = texts.map { embedderProvider.stage1.embed(it) }
                map[category.id] = average(vectors)
            }
            embeddings = map
            cacheStore.write(CACHE_KEY, map)
        }
    }

    private fun buildTexts(category: CoicopCategory): List<String> {
        val categoryMap = this.categoryMap.orEmpty()
        val parent = category.parentId?.let { categoryMap[it] }
        val grandParent = parent?.parentId?.let { categoryMap[it] }
        val languages = setOf("ru", "en", "kk")
        val result = mutableListOf<String>()
        for (lang in languages) {
            val path = listOfNotNull(
                grandParent?.names?.get(lang),
                parent?.names?.get(lang),
                category.names[lang],
            )
            if (path.isNotEmpty()) {
                result.add(path.joinToString(" > "))
            }
            category.names[lang]?.let { result.add(it) }
            category.aliases[lang].orEmpty().forEach { result.add(it) }
        }
        val ru = category.names["ru"]
        val en = category.names["en"]
        val kk = category.names["kk"]
        val mixLeaf = listOfNotNull(ru, en, kk).map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (mixLeaf.size >= 2) {
            result.add(mixLeaf.joinToString(" · "))
            result.add(mixLeaf.joinToString(" "))
        }
        val mixPath = listOfNotNull(
            grandParent?.names["ru"],
            parent?.names["en"],
            category.names["kk"],
        ).map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (mixPath.size >= 2) {
            result.add(mixPath.joinToString(" > "))
        }
        if (result.isEmpty()) {
            result.add(category.id)
        }
        return result.distinct()
    }

    private fun average(vectors: List<FloatArray>): FloatArray {
        if (vectors.isEmpty()) return FloatArray(0)
        val size = vectors.first().size
        val sum = FloatArray(size)
        for (vector in vectors) {
            val len = minOf(size, vector.size)
            for (i in 0 until len) {
                sum[i] += vector[i]
            }
        }
        val count = max(1, vectors.size)
        for (i in sum.indices) sum[i] /= count
        return normalize(sum)
    }

    private fun dot(a: FloatArray, b: FloatArray): Float {
        val size = minOf(a.size, b.size)
        var sum = 0f
        for (i in 0 until size) sum += a[i] * b[i]
        return sum
    }

    private fun normalize(vector: FloatArray): FloatArray {
        var sum = 0f
        for (v in vector) sum += v * v
        val norm = sqrt(sum)
        if (norm > 0f) {
            for (i in vector.indices) vector[i] /= norm
        }
        return vector
    }

    companion object {
        private const val TAG = "RollupEmbed"
        private const val CACHE_KEY = "rollup_centroids_v11"
        private const val THRESHOLD = 0.70f
        private const val MARGIN = 0.12f
        private const val MAX_CANDIDATES = 6
    }
}
