package com.chelovecheck.data.repository.receipt.list

import com.chelovecheck.domain.model.ReceiptListCursor
import com.chelovecheck.domain.model.ReceiptListSortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Golden/regression checks for keyset SQL — must stay aligned with [ReceiptListQueryBuilder] delegates. */
class ReceiptListSortSqlStrategyTest {
    @Test
    fun orderByClause_default_matchesGoldenPrefix() {
        val sql = ReceiptListSortSqlStrategy.orderByClause(ReceiptListSortOrder.DEFAULT)
        assertTrue(sql.startsWith("r.isPinned DESC, r.isFavorite DESC"))
        assertTrue(sql.contains("r.fiscalSign DESC"))
    }

    @Test
    fun cursorPredicateArgs_default_argCount_matchesPlaceholderCount() {
        val sql = ReceiptListSortSqlStrategy.cursorPredicateSql(ReceiptListSortOrder.DEFAULT)
        val placeholders = sql.count { it == '?' }
        val cursor = ReceiptListCursor(
            dateTimeEpochMillis = 100L,
            fiscalSign = "abc",
            isPinned = true,
            isFavorite = false,
            totalSum = 0.0,
            companyName = "",
        )
        val args = ReceiptListSortSqlStrategy.cursorPredicateArgs(ReceiptListSortOrder.DEFAULT, cursor)
        assertEquals(placeholders, args.size)
    }
}
