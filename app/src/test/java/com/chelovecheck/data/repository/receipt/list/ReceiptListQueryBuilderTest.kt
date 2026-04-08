package com.chelovecheck.data.repository.receipt.list

import com.chelovecheck.domain.model.ReceiptFilter
import com.chelovecheck.domain.model.ReceiptListSortOrder
import com.chelovecheck.domain.model.ReceiptOwnershipFilter
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptListQueryBuilderTest {
    private val builder = ReceiptListQueryBuilder()

    @Test
    fun buildWhereForList_usesFtsWhenEnabled() {
        val (where, args) = builder.buildWhereForList(
            receiptsAlias = "r",
            filter = ReceiptFilter.All,
            searchQuery = "milk",
            useFtsForItemNames = true,
            ownership = ReceiptOwnershipFilter.All,
        )
        assertTrue(where.contains("items_fts MATCH"))
        assertTrue(args.isNotEmpty())
    }

    @Test
    fun buildWhereForList_usesLikeWhenFtsDisabled() {
        val (where, _) = builder.buildWhereForList(
            receiptsAlias = "r",
            filter = ReceiptFilter.All,
            searchQuery = "milk",
            useFtsForItemNames = false,
            ownership = ReceiptOwnershipFilter.All,
        )
        assertTrue(where.contains("items.name LIKE ?"))
        assertTrue(!where.contains("items_fts MATCH"))
    }

    @Test
    fun orderByClause_containsStableFiscalSignTieBreak() {
        val orderBy = builder.orderByClause(ReceiptListSortOrder.AMOUNT_ASC)
        assertTrue(orderBy.contains("fiscalSign"))
    }
}
