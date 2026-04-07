package com.chelovecheck.domain.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ItemNameNormalizerTest {

    @Test
    fun preprocessStripsAngleTags() {
        assertEquals("Молоко", ItemNameNormalizer.preprocessReceiptLineName("<A>Молоко"))
        assertEquals("Хлеб", ItemNameNormalizer.preprocessReceiptLineName("</tag>Хлеб"))
        assertEquals("допосле", ItemNameNormalizer.preprocessReceiptLineName("до<A>после").trim())
    }

    @Test
    fun preprocessStripsLeadingNumberingRepeatedly() {
        assertEquals("Молоко", ItemNameNormalizer.preprocessReceiptLineName("1. Молоко"))
        assertEquals("Молоко", ItemNameNormalizer.preprocessReceiptLineName("2) Молоко"))
        assertEquals("Молоко", ItemNameNormalizer.preprocessReceiptLineName("3 - Молоко"))
        assertEquals("Молоко", ItemNameNormalizer.preprocessReceiptLineName("№5 Молоко"))
        assertEquals("Молоко", ItemNameNormalizer.preprocessReceiptLineName("1. 2. Молоко"))
    }

    @Test
    fun preprocessNormalizesNewlines() {
        assertEquals("a b", ItemNameNormalizer.preprocessReceiptLineName("a\nb"))
    }

    @Test
    fun cleanDisplayNameAndNormalizeForMatchSharePreprocess() {
        val raw = "<A>1. Хлеб ×2"
        val cleaned = ItemNameNormalizer.cleanDisplayName(raw)
        assertEquals("Хлеб", cleaned)
        val norm = ItemNameNormalizer.normalizeForMatch(raw)
        assertEquals("хлеб", norm)
    }

    @Test
    fun normalizeForMatchDoesNotSeeAngleNoise() {
        val n = ItemNameNormalizer.normalizeForMatch("<A>молоко 3.2%")
        assertEquals("молоко", n)
    }
}
