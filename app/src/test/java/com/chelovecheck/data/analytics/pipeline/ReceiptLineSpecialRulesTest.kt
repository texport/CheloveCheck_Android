package com.chelovecheck.data.analytics.pipeline

import com.chelovecheck.domain.utils.ItemNameNormalizer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptLineSpecialRulesTest {
    @Test
    fun foodService_andModifiers_areDetected_fromRealLogs() {
        val foodService = listOf(
            "Воппер",
            "Обед Биф Ролл",
            "Цезарь с цыпленком и гренками бриошь",
            "Хачапури по-имеретински",
            "Обед Двойн.Вопп",
            "Крылышки Кинг 8",
        )
        val modifiers = listOf(
            "Без льда",
            "Без экстра соуса",
            "Увелич. комбо",
        )
        val technical = listOf(
            "Позиция",
            "Товар",
        )

        foodService.forEach { raw ->
            val norm = ItemNameNormalizer.normalizeForMatch(raw)
            assertTrue("foodService not detected: $raw", ReceiptLineSpecialRules.isFoodServiceMenu(norm))
        }
        modifiers.forEach { raw ->
            val norm = ItemNameNormalizer.normalizeForMatch(raw)
            assertTrue("modifier not detected: $raw", ReceiptLineSpecialRules.isServiceModifier(norm))
        }
        technical.forEach { raw ->
            val norm = ItemNameNormalizer.normalizeForMatch(raw)
            assertTrue("technical placeholder not detected: $raw", ReceiptLineSpecialRules.isTechnicalPlaceholder(norm))
        }
    }

    @Test
    fun weakDrinkOrSnackMarkers_doNotForceFoodService() {
        val shouldNotMatch = listOf(
            "Раф соленая карамель",
            "Глинтвейн 900мл",
            "Кола 0.5",
            "Защелка дверная магнитная графит",
        )
        shouldNotMatch.forEach { raw ->
            val norm = ItemNameNormalizer.normalizeForMatch(raw)
            assertFalse("should not be forced into food service: $raw", ReceiptLineSpecialRules.isFoodServiceMenu(norm))
        }
    }
}
