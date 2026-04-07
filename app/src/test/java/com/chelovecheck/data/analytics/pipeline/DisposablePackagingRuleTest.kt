package com.chelovecheck.data.analytics.pipeline

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DisposablePackagingRuleTest {

    @Test
    fun matchesCommonBagPhrases() {
        assertTrue(DisposablePackagingRule.matchesNormalized("пакет майка"))
        assertTrue(DisposablePackagingRule.matchesNormalized("пакет с ручками №1"))
        assertTrue(DisposablePackagingRule.matchesNormalized("plastic bag small"))
    }

    @Test
    fun doesNotMatchRegularFood() {
        assertFalse(DisposablePackagingRule.matchesNormalized("молоко 3.2%"))
        assertFalse(DisposablePackagingRule.matchesNormalized("хлеб белый"))
    }
}
