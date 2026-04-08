package com.chelovecheck.data.repository.receipt.list

import com.chelovecheck.domain.model.ReceiptListCursor
import com.chelovecheck.domain.model.ReceiptListSortOrder

/**
 * Declarative SQL fragments for receipt list ordering and keyset pagination per [ReceiptListSortOrder].
 */
internal object ReceiptListSortSqlStrategy {

    fun orderByClause(sort: ReceiptListSortOrder): String = when (sort) {
        ReceiptListSortOrder.DEFAULT ->
            "r.isPinned DESC, r.isFavorite DESC, r.dateTimeEpochMillis DESC, r.fiscalSign DESC"
        ReceiptListSortOrder.DATE_NEWEST ->
            "r.isPinned DESC, r.dateTimeEpochMillis DESC, r.fiscalSign DESC"
        ReceiptListSortOrder.DATE_OLDEST ->
            "r.isPinned DESC, r.dateTimeEpochMillis ASC, r.fiscalSign ASC"
        ReceiptListSortOrder.AMOUNT_DESC ->
            "r.isPinned DESC, r.totalSum DESC, r.fiscalSign DESC"
        ReceiptListSortOrder.AMOUNT_ASC ->
            "r.isPinned DESC, r.totalSum ASC, r.fiscalSign ASC"
        ReceiptListSortOrder.MERCHANT_AZ ->
            "r.isPinned DESC, r.companyName COLLATE NOCASE ASC, r.fiscalSign ASC"
    }

    fun cursorPredicateSql(sort: ReceiptListSortOrder): String = when (sort) {
        ReceiptListSortOrder.DEFAULT ->
            """
            (
                (r.isPinned < ?) OR
                (r.isPinned = ? AND r.isFavorite < ?) OR
                (r.isPinned = ? AND r.isFavorite = ? AND r.dateTimeEpochMillis < ?) OR
                (r.isPinned = ? AND r.isFavorite = ? AND r.dateTimeEpochMillis = ? AND r.fiscalSign < ?)
            )
            """.trimIndent()
        ReceiptListSortOrder.DATE_NEWEST ->
            """
            (
                (r.isPinned < ?) OR
                (r.isPinned = ? AND r.dateTimeEpochMillis < ?) OR
                (r.isPinned = ? AND r.dateTimeEpochMillis = ? AND r.fiscalSign < ?)
            )
            """.trimIndent()
        ReceiptListSortOrder.DATE_OLDEST ->
            """
            (
                (? = 1 AND r.isPinned = 1 AND (r.dateTimeEpochMillis > ? OR (r.dateTimeEpochMillis = ? AND r.fiscalSign > ?))) OR
                (? = 1 AND r.isPinned = 0) OR
                (? = 0 AND r.isPinned = 0 AND (r.dateTimeEpochMillis > ? OR (r.dateTimeEpochMillis = ? AND r.fiscalSign > ?)))
            )
            """.trimIndent()
        ReceiptListSortOrder.AMOUNT_DESC ->
            """
            (
                (r.isPinned < ?) OR
                (r.isPinned = ? AND r.totalSum < ?) OR
                (r.isPinned = ? AND r.totalSum = ? AND r.fiscalSign < ?)
            )
            """.trimIndent()
        ReceiptListSortOrder.AMOUNT_ASC ->
            """
            (
                (? = 1 AND r.isPinned = 1 AND (r.totalSum > ? OR (r.totalSum = ? AND r.fiscalSign > ?))) OR
                (? = 1 AND r.isPinned = 0) OR
                (? = 0 AND r.isPinned = 0 AND (r.totalSum > ? OR (r.totalSum = ? AND r.fiscalSign > ?)))
            )
            """.trimIndent()
        ReceiptListSortOrder.MERCHANT_AZ ->
            """
            (
                (? = 1 AND r.isPinned = 1 AND (r.companyName COLLATE NOCASE > ? OR (r.companyName COLLATE NOCASE = ? AND r.fiscalSign > ?))) OR
                (? = 1 AND r.isPinned = 0) OR
                (? = 0 AND r.isPinned = 0 AND (r.companyName COLLATE NOCASE > ? OR (r.companyName COLLATE NOCASE = ? AND r.fiscalSign > ?)))
            )
            """.trimIndent()
    }

    fun cursorPredicateArgs(sort: ReceiptListSortOrder, cursor: ReceiptListCursor): List<Any> {
        val cp = cursor.isPinned.asSqlInt()
        val cf = cursor.isFavorite.asSqlInt()
        return when (sort) {
            ReceiptListSortOrder.DEFAULT -> listOf(
                cp, cp, cf, cp, cf, cursor.dateTimeEpochMillis,
                cp, cf, cursor.dateTimeEpochMillis, cursor.fiscalSign,
            )
            ReceiptListSortOrder.DATE_NEWEST -> listOf(
                cp, cp, cursor.dateTimeEpochMillis, cp, cursor.dateTimeEpochMillis, cursor.fiscalSign,
            )
            ReceiptListSortOrder.DATE_OLDEST -> listOf(
                cp, cursor.dateTimeEpochMillis, cursor.dateTimeEpochMillis, cursor.fiscalSign,
                cp, cp, cursor.dateTimeEpochMillis, cursor.dateTimeEpochMillis, cursor.fiscalSign,
            )
            ReceiptListSortOrder.AMOUNT_DESC -> listOf(
                cp, cp, cursor.totalSum, cp, cursor.totalSum, cursor.fiscalSign,
            )
            ReceiptListSortOrder.AMOUNT_ASC -> listOf(
                cp, cursor.totalSum, cursor.totalSum, cursor.fiscalSign,
                cp, cp, cursor.totalSum, cursor.totalSum, cursor.fiscalSign,
            )
            ReceiptListSortOrder.MERCHANT_AZ -> listOf(
                cp, cursor.companyName, cursor.companyName, cursor.fiscalSign,
                cp, cp, cursor.companyName, cursor.companyName, cursor.fiscalSign,
            )
        }
    }

    private fun Boolean.asSqlInt(): Int = if (this) 1 else 0
}
