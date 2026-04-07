package com.chelovecheck.data.analytics.pipeline

import com.chelovecheck.domain.model.CoicopCategory
import com.chelovecheck.domain.repository.CategoryRepository
import com.chelovecheck.domain.utils.ItemNameNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LexicalAliasMatcherSyntheticDatasetTest {
    private val repo = FakeCategoryRepository()
    private val matcher = LexicalAliasMatcher(repo)
    private val chunkSize = 10_000
    private val chunksPerDataset = 10
    private val datasetCount = 10
    private val totalSizePerDataset = chunkSize * chunksPerDataset
    private val uniquePerChunk = 5_500
    private val repeatPerChunk = 2_500
    private val semanticPerChunk = 2_000
    private val datasetConfigs = (1..datasetCount).map { id -> DatasetConfig(id = id, seed = 1_000 * id + 42) }

    @Test
    fun mixedLanguageRegressionCases_areResolvedToExpectedBuckets() = runBlocking {
        val cases = listOf(
            "milk сүт 1л fresh" to "01.1",
            "bus билет жол жүру card" to "07.3",
            "jeans шалбар denim classic" to "03.1",
            "нан bread wholegrain" to "01.1",
            "такси metro жол жүру pass" to "07.3",
            "рубашка shirt көйлек xl" to "03.1",
            "apple алма milk био" to "01.1",
        )
        for ((raw, expected) in cases) {
            val normalized = ItemNameNormalizer.normalizeForMatch(raw)
            val actual = matcher.match(normalized)?.categoryId
            assertEquals("case=$raw", expected, actual)
        }
    }

    @Test fun dataset_01_100k() = runBlocking { assertDataset(datasetConfigs[0]) }
    @Test fun dataset_02_100k() = runBlocking { assertDataset(datasetConfigs[1]) }
    @Test fun dataset_03_100k() = runBlocking { assertDataset(datasetConfigs[2]) }
    @Test fun dataset_04_100k() = runBlocking { assertDataset(datasetConfigs[3]) }
    @Test fun dataset_05_100k() = runBlocking { assertDataset(datasetConfigs[4]) }
    @Test fun dataset_06_100k() = runBlocking { assertDataset(datasetConfigs[5]) }
    @Test fun dataset_07_100k() = runBlocking { assertDataset(datasetConfigs[6]) }
    @Test fun dataset_08_100k() = runBlocking { assertDataset(datasetConfigs[7]) }
    @Test fun dataset_09_100k() = runBlocking { assertDataset(datasetConfigs[8]) }
    @Test fun dataset_10_100k() = runBlocking { assertDataset(datasetConfigs[9]) }

    @Test
    fun allDatasets_aggregateSummary_1m() = runBlocking {
        val all = datasetConfigs.map { runDataset(it) }
        val avgAcc = all.map { it.accuracy }.average()
        val minAcc = all.minOf { it.accuracy }
        val avgRepeat = all.map { it.repeatConsistencyStableShare }.average()
        val minRepeat = all.minOf { it.repeatConsistencyStableShare }
        val avgSemantic = all.map { it.semanticConsistencyStableShare }.average()
        val minSemantic = all.minOf { it.semanticConsistencyStableShare }
        val mergedConfusions = LinkedHashMap<String, Int>()
        all.forEach { m ->
            m.confusions.forEach { (k, v) -> mergedConfusions[k] = (mergedConfusions[k] ?: 0) + v }
        }
        val topConfusions = mergedConfusions.entries
            .sortedByDescending { it.value }
            .take(12)
            .joinToString { "${it.key}:${it.value}" }
        println(
            "Synthetic 10x100k summary: avgAcc=$avgAcc minAcc=$minAcc avgRepeat=$avgRepeat " +
                "minRepeat=$minRepeat avgSemantic=$avgSemantic minSemantic=$minSemantic topConfusions=[$topConfusions]",
        )
        assertTrue("minAcc=$minAcc", minAcc >= 0.95)
        assertTrue("minRepeat=$minRepeat", minRepeat >= 0.95)
    }

    private data class DatasetConfig(val id: Int, val seed: Int)

    private data class DatasetMetrics(
        val datasetId: Int,
        val total: Int,
        val correct: Int,
        val accuracy: Double,
        val uniqueTotal: Int,
        val repeatTotal: Int,
        val semanticTotal: Int,
        val repeatConsistencyStableShare: Double,
        val semanticConsistencyStableShare: Double,
        val confusions: Map<String, Int>,
    )

    private data class ChunkMetrics(
        val total: Int,
        val correct: Int,
        val accuracy: Double,
        val confusions: Map<String, Int>,
        val uniqueCount: Int,
        val repeatCount: Int,
        val semanticCount: Int,
        val repeatConsistency: Map<String, Double>,
        val semanticConsistency: Map<String, Double>,
    )

    private suspend fun assertDataset(config: DatasetConfig) {
        val m = runDataset(config)
        println(
            "Dataset ${config.id}: total=${m.total} acc=${m.accuracy} repeatStable=${m.repeatConsistencyStableShare} " +
                "semanticStable=${m.semanticConsistencyStableShare}",
        )
        assertEquals(totalSizePerDataset, m.total)
        assertEquals((totalSizePerDataset * 0.55).toInt(), m.uniqueTotal)
        assertEquals((totalSizePerDataset * 0.25).toInt(), m.repeatTotal)
        assertEquals((totalSizePerDataset * 0.20).toInt(), m.semanticTotal)
        assertTrue("dataset=${config.id} acc=${m.accuracy}", m.accuracy >= 0.95)
        assertTrue("dataset=${config.id} repeat=${m.repeatConsistencyStableShare}", m.repeatConsistencyStableShare >= 0.95)
    }

    private suspend fun runDataset(config: DatasetConfig): DatasetMetrics {
        val chunks = (0 until chunksPerDataset).map { chunkIndex ->
            val seed = config.seed + chunkIndex * 9_973
            runChunk(chunkIndex = chunkIndex, seed = seed)
        }
        val total = chunks.sumOf { it.total }
        val correct = chunks.sumOf { it.correct }
        val accuracy = correct.toDouble() / total.toDouble()
        val uniqueTotal = chunks.sumOf { it.uniqueCount }
        val repeatTotal = chunks.sumOf { it.repeatCount }
        val semanticTotal = chunks.sumOf { it.semanticCount }
        val repeatConsistencyStableShare = stableShare(chunks.flatMap { it.repeatConsistency.values })
        val semanticConsistencyStableShare = stableShare(chunks.flatMap { it.semanticConsistency.values })
        val confusions = LinkedHashMap<String, Int>()
        chunks.forEach { ch ->
            ch.confusions.forEach { (k, v) -> confusions[k] = (confusions[k] ?: 0) + v }
        }
        return DatasetMetrics(
            datasetId = config.id,
            total = total,
            correct = correct,
            accuracy = accuracy,
            uniqueTotal = uniqueTotal,
            repeatTotal = repeatTotal,
            semanticTotal = semanticTotal,
            repeatConsistencyStableShare = repeatConsistencyStableShare,
            semanticConsistencyStableShare = semanticConsistencyStableShare,
            confusions = confusions,
        )
    }

    private suspend fun runChunk(chunkIndex: Int, seed: Int): ChunkMetrics = withContext(Dispatchers.Default) {
        val random = Random(seed)
        val rows = buildChunkRows(random, chunkIndex)
        var correct = 0
        val confusions = LinkedHashMap<String, Int>()
        val repeatGroups = LinkedHashMap<String, MutableList<String>>()
        val semanticGroups = LinkedHashMap<String, MutableList<String>>()

        rows.forEach { row ->
            val predicted = matcher.match(row.normalized)?.categoryId ?: "__null__"
            if (predicted == row.expectedCategoryId) {
                correct++
            } else {
                val key = "${row.expectedCategoryId}->${predicted}"
                confusions[key] = (confusions[key] ?: 0) + 1
            }
            if (row.kind == RowKind.REPEAT) {
                repeatGroups.getOrPut(row.baseNormalizedKey) { mutableListOf() }.add(predicted)
            }
            if (row.kind == RowKind.SEMANTIC) {
                semanticGroups.getOrPut(row.clusterId) { mutableListOf() }.add(predicted)
            }
        }
        val accuracy = correct.toDouble() / chunkSize.toDouble()
        val repeatConsistency = repeatGroups.mapValues { (_, predictions) -> majorityShare(predictions) }
        val semanticConsistency = semanticGroups.mapValues { (_, predictions) -> majorityShare(predictions) }
        println("Synthetic chunk ${chunkIndex + 1}/$chunksPerDataset: acc=$accuracy correct=$correct total=$chunkSize")
        ChunkMetrics(
            total = chunkSize,
            correct = correct,
            accuracy = accuracy,
            confusions = confusions,
            uniqueCount = rows.count { it.kind == RowKind.UNIQUE },
            repeatCount = rows.count { it.kind == RowKind.REPEAT },
            semanticCount = rows.count { it.kind == RowKind.SEMANTIC },
            repeatConsistency = repeatConsistency,
            semanticConsistency = semanticConsistency,
        )
    }

    private data class DatasetRow(
        val kind: RowKind,
        val normalized: String,
        val expectedCategoryId: String,
        val baseNormalizedKey: String,
        val clusterId: String,
    )

    private enum class RowKind { UNIQUE, REPEAT, SEMANTIC }

    private data class Lexicon(
        val categoryId: String,
        val primary: List<String>,
        val equivalents: List<List<String>>,
    )

    private fun buildChunkRows(random: Random, chunkIndex: Int): List<DatasetRow> {
        val lexicons = listOf(
            Lexicon(
                categoryId = "01.1",
                primary = listOf("milk", "bread", "apple"),
                equivalents = listOf(
                    listOf("milk", "молоко", "сүт"),
                    listOf("bread", "хлеб", "нан"),
                    listOf("apple", "яблоко", "алма"),
                ),
            ),
            Lexicon(
                categoryId = "07.3",
                primary = listOf("bus", "taxi", "metro"),
                equivalents = listOf(
                    listOf("bus", "автобус", "жол жүру"),
                    listOf("taxi", "такси", "taxi"),
                    listOf("metro", "метро", "metro"),
                ),
            ),
            Lexicon(
                categoryId = "03.1",
                primary = listOf("shirt", "jeans", "dress"),
                equivalents = listOf(
                    listOf("shirt", "рубашка", "көйлек"),
                    listOf("jeans", "джинсы", "шалбар"),
                    listOf("dress", "платье", "көйлек"),
                ),
            ),
        )
        val brands = listOf("magnum", "small", "dostar", "global", "arzan", "freshmart")
        val qty = listOf("1л", "500г", "2шт", "250 мл", "0.75l", "xl")
        val noise = listOf("promo", "скидка", "акция", "fresh", "new", "2026")

        val rows = mutableListOf<DatasetRow>()

        // 55% unique
        repeat(uniquePerChunk) { i ->
            val l = lexicons[(i + chunkIndex) % lexicons.size]
            val t1 = l.primary[i % l.primary.size]
            val t2 = l.primary[(i + 1) % l.primary.size]
            val raw = "$t1 $t2 ${brands[(i + chunkIndex) % brands.size]} ${qty[i % qty.size]} ${noise[(i * 3) % noise.size]} u${chunkIndex}_$i"
            rows += buildRow(RowKind.UNIQUE, raw, l.categoryId, "u-$chunkIndex-$i", "unique-$chunkIndex-$i")
        }

        // 25% exact repeats (same normalized key repeated)
        val repeatFamilies = 500
        val repeatsPerFamily = repeatPerChunk / repeatFamilies // 5
        repeat(repeatFamilies) { f ->
            val l = lexicons[(f + chunkIndex) % lexicons.size]
            val base = "${l.primary[f % l.primary.size]} ${l.primary[(f + 2) % l.primary.size]} ${brands[f % brands.size]}"
            repeat(repeatsPerFamily) { r ->
                rows += buildRow(RowKind.REPEAT, base, l.categoryId, base, "repeat-$chunkIndex-$f")
            }
        }

        // 20% semantic variants (same cluster, different surface forms)
        val semanticFamilies = 400
        val semanticPerFamily = semanticPerChunk / semanticFamilies // 5
        repeat(semanticFamilies) { f ->
            val l = lexicons[(f + 2 * chunkIndex) % lexicons.size]
            val eq1 = l.equivalents[f % l.equivalents.size]
            val eq2 = l.equivalents[(f + 1) % l.equivalents.size]
            repeat(semanticPerFamily) { s ->
                val v1 = eq1[(s + f) % eq1.size]
                val v2 = eq2[(s + 1) % eq2.size]
                val raw = "$v1 $v2 ${brands[(f + s) % brands.size]} ${qty[(f + s) % qty.size]} ${noise[(f + s) % noise.size]}"
                rows += buildRow(
                    RowKind.SEMANTIC,
                    raw,
                    l.categoryId,
                    ItemNameNormalizer.normalizeForMatch("${eq1.first()} ${eq2.first()}"),
                    "semantic-$chunkIndex-$f",
                )
            }
        }
        assertEquals(chunkSize, rows.size)
        return rows.shuffled(random)
    }

    private fun buildRow(
        kind: RowKind,
        raw: String,
        expectedCategoryId: String,
        baseNormalizedKey: String,
        clusterId: String,
    ): DatasetRow {
        val normalized = ItemNameNormalizer.normalizeForMatch(raw)
        return DatasetRow(
            kind = kind,
            normalized = normalized,
            expectedCategoryId = expectedCategoryId,
            baseNormalizedKey = baseNormalizedKey,
            clusterId = clusterId,
        )
    }

    private fun majorityShare(values: List<String>): Double {
        if (values.isEmpty()) return 0.0
        val max = values.groupingBy { it }.eachCount().maxOf { it.value }
        return max.toDouble() / values.size.toDouble()
    }

    private fun stableShare(shares: List<Double>): Double {
        if (shares.isEmpty()) return 0.0
        val stable = shares.count { it >= 0.95 }
        return stable.toDouble() / shares.size.toDouble()
    }

    private class FakeCategoryRepository : CategoryRepository {
        private val categories: List<CoicopCategory> = listOf(
            CoicopCategory("01", 1, null, mapOf("ru" to "Продукты"), emptyMap()),
            CoicopCategory("03", 1, null, mapOf("ru" to "Одежда"), emptyMap()),
            CoicopCategory("07", 1, null, mapOf("ru" to "Транспорт"), emptyMap()),
            CoicopCategory(
                "01.1",
                2,
                "01",
                names = mapOf("ru" to "Продукты питания", "en" to "Food", "kk" to "Азық-түлік"),
                aliases = mapOf(
                    "ru" to listOf("молоко", "хлеб", "яблоко", "нан"),
                    "en" to listOf("milk", "bread", "apple"),
                    "kk" to listOf("сүт", "нан", "алма"),
                ),
            ),
            CoicopCategory(
                "03.1",
                2,
                "03",
                names = mapOf("ru" to "Одежда", "en" to "Clothing", "kk" to "Киім"),
                aliases = mapOf(
                    "ru" to listOf("рубашка", "джинсы"),
                    "en" to listOf("shirt", "jeans"),
                    "kk" to listOf("көйлек", "шалбар"),
                ),
            ),
            CoicopCategory(
                "07.3",
                2,
                "07",
                names = mapOf("ru" to "Пассажирский транспорт", "en" to "Passenger transport", "kk" to "Жолаушы көлігі"),
                aliases = mapOf(
                    "ru" to listOf("автобус", "метро", "такси"),
                    "en" to listOf("bus", "metro", "taxi"),
                    "kk" to listOf("жол жүру"),
                ),
            ),
        )

        override suspend fun getCategory(id: String): CoicopCategory? = categories.firstOrNull { it.id == id }
        override suspend fun getLeafCategories(): List<CoicopCategory> = categories.filter { it.level >= 2 }
        override suspend fun getRollupCategories(): List<CoicopCategory> = categories.filter { it.level <= 2 }
        override suspend fun getAllCategories(): List<CoicopCategory> = categories
    }
}
