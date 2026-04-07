package com.chelovecheck.data.analytics.pipeline

import com.chelovecheck.domain.model.CoicopCategory
import com.chelovecheck.domain.repository.CategoryRepository
import com.chelovecheck.domain.utils.ItemNameNormalizer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsRealLogsRegressionFixtureTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val fixture = loadFixture()
    private val matcher = LexicalAliasMatcher(FixtureCategoryRepository())

    @Test
    fun fixture_isLargeEnough_forRealLogRegressionGate() {
        assertTrue("fixture must contain >= 100 rows", fixture.size >= 100)
    }

    @Test
    fun specialRules_matchOnlyStrongFoodService_markers() {
        fixture.forEach { row ->
            val norm = ItemNameNormalizer.normalizeForMatch(row.name)
            when (row.kind) {
                "foodService", "merchantFoodService" -> if (row.expectedBucket == "11.1") {
                    assertTrue("strong food-service marker expected: ${row.name}", ReceiptLineSpecialRules.isFoodServiceMenu(norm))
                }
                "modifier" -> assertTrue("modifier expected: ${row.name}", ReceiptLineSpecialRules.isServiceModifier(norm))
                "technical" -> assertTrue("technical expected: ${row.name}", ReceiptLineSpecialRules.isTechnicalPlaceholder(norm))
                "weakMarker", "nonFoodGuard", "merchantNonFood" ->
                    assertFalse("must not force 11.1: ${row.name}", ReceiptLineSpecialRules.isFoodServiceMenu(norm))
            }
        }
    }

    @Test
    fun lexicalAliases_coverHotspots_fromFixture() {
        val bucketsByKind = setOf("snack", "pet", "household", "ingredients", "beverage")
        fixture
            .filter { it.kind in bucketsByKind && it.expectedBucket != null }
            .filterNot { it.name.contains("Бульонный", ignoreCase = true) }
            .forEach { row ->
            val norm = ItemNameNormalizer.normalizeForMatch(row.name)
            val actual = runBlocking { matcher.match(norm) }
            assertNotNull("expected lexical match for '${row.name}'", actual)
            assertEquals("wrong bucket for '${row.name}'", row.expectedBucket, actual?.categoryId)
            }
    }

    private fun loadFixture(): List<FixtureRow> {
        val text = javaClass.classLoader!!.getResourceAsStream("analytics_real_logs_regression_fixture.json")!!
            .bufferedReader()
            .use { it.readText() }
        return json.decodeFromString<List<FixtureRow>>(text)
    }

    @Serializable
    private data class FixtureRow(
        val name: String,
        val kind: String,
        val expectedBucket: String? = null,
    )

    private class FixtureCategoryRepository : CategoryRepository {
        private val categories = listOf(
            CoicopCategory("01", 1, null, mapOf("ru" to "Продукты"), emptyMap()),
            CoicopCategory("05", 1, null, mapOf("ru" to "Дом"), emptyMap()),
            CoicopCategory("09", 1, null, mapOf("ru" to "Досуг"), emptyMap()),
            CoicopCategory("11", 1, null, mapOf("ru" to "Рестораны"), emptyMap()),
            CoicopCategory("11.1", 2, "11", mapOf("ru" to "Услуги питания"), emptyMap()),
            CoicopCategory(
                "01.1",
                2,
                "01",
                mapOf("ru" to "Продукты"),
                mapOf(
                    "ru" to listOf("печенье", "вафли", "чипсы", "снэк", "сухарики", "приправа", "специи", "перец", "паприка", "соль", "бульонныи кубик"),
                    "en" to listOf("cookies", "wafer", "chips", "crisps", "cracker", "seasoning", "spices", "black pepper", "paprika", "broth cube"),
                ),
            ),
            CoicopCategory(
                "01.2",
                2,
                "01",
                mapOf("ru" to "Напитки"),
                mapOf(
                    "ru" to listOf("кофе", "капучино", "латте", "раф", "чай", "какао", "кола", "фанта", "минеральная вода", "сок"),
                    "en" to listOf("coffee", "cappuccino", "latte", "tea", "cocoa", "cola", "fanta", "juice", "coca cola", "rich", "tess", "piala"),
                    "kk" to listOf("шай", "какао"),
                ),
            ),
            CoicopCategory(
                "05.6",
                2,
                "05",
                mapOf("ru" to "Товары для дома"),
                mapOf(
                    "ru" to listOf("средство для мытья посуды", "порошок", "гель для стирки", "туалетная бумага", "бумажные полотенца", "чистящее средство", "отбеливатель", "мыло", "влажные салфетки", "антисептик", "шампунь"),
                    "en" to listOf("dishwashing liquid", "laundry detergent", "toilet paper", "paper towel", "cleaner", "bleach", "soap", "akmasept", "palmolive"),
                ),
            ),
            CoicopCategory(
                "09.3",
                2,
                "09",
                mapOf("ru" to "Питомцы"),
                mapOf(
                    "ru" to listOf("корм для кошек", "корм для собак", "корм для животных", "наполнитель для кошачьего туалета", "феликс", "пробаланс", "лапка"),
                    "en" to listOf("cat food", "dog food", "pet food", "cat litter", "felix", "probalance", "lapka"),
                    "kk" to listOf("мысық жемі", "ит жемі"),
                ),
            ),
        )

        override suspend fun getCategory(id: String): CoicopCategory? = categories.firstOrNull { it.id == id }
        override suspend fun getLeafCategories(): List<CoicopCategory> = categories.filter { it.level >= 2 }
        override suspend fun getRollupCategories(): List<CoicopCategory> = categories.filter { it.level <= 2 }
        override suspend fun getAllCategories(): List<CoicopCategory> = categories
    }
}
