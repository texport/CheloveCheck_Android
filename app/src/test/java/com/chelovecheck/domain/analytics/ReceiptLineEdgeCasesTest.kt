package com.chelovecheck.domain.analytics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptLineEdgeCasesTest {

    @Test
    fun giftCardDetection() {
        assertTrue(ReceiptLineEdgeCases.looksLikeGiftCardOrVoucher("подарочная карта 5000"))
        assertTrue(ReceiptLineEdgeCases.looksLikeGiftCardOrVoucher("gift card reload"))
        assertFalse(ReceiptLineEdgeCases.looksLikeGiftCardOrVoucher("молоко"))
    }

    @Test
    fun depositDetection() {
        assertTrue(ReceiptLineEdgeCases.looksLikeDepositOrContainerFee("залог тары"))
        assertFalse(ReceiptLineEdgeCases.looksLikeDepositOrContainerFee("вода 1.5л"))
    }

    @Test
    fun returnLineDetection() {
        assertTrue(ReceiptLineEdgeCases.looksLikeExplicitReturnLine("возврат товара"))
        assertFalse(ReceiptLineEdgeCases.looksLikeExplicitReturnLine("хлеб"))
    }
}
