package com.chelovecheck.data.repository.receipt.list

import com.chelovecheck.domain.model.ReceiptFilter
import com.chelovecheck.domain.model.ReceiptListCursor
import com.chelovecheck.domain.model.ReceiptListSortOrder
import com.chelovecheck.domain.model.ReceiptOwnershipFilter
import java.time.LocalDate
import java.time.ZoneId

internal class ReceiptListQueryBuilder {
    fun buildWhere(
        filter: ReceiptFilter,
        searchQuery: String?,
    ): Pair<String, List<Any>> {
        return buildWhereForList(
            receiptsAlias = "",
            filter = filter,
            searchQuery = searchQuery,
            useFtsForItemNames = false,
            ownership = ReceiptOwnershipFilter.All,
        )
    }

    /**
     * @param receiptsAlias empty string for legacy queries without table alias, or e.g. "r" for aliased SQL.
     */
    fun buildWhereForList(
        receiptsAlias: String,
        filter: ReceiptFilter,
        searchQuery: String?,
        useFtsForItemNames: Boolean,
        ownership: ReceiptOwnershipFilter,
    ): Pair<String, List<Any>> {
        val a = if (receiptsAlias.isEmpty()) "" else "$receiptsAlias."
        val clauses = mutableListOf<String>()
        val args = mutableListOf<Any>()

        val dateRange = resolveDateRange(filter)
        if (dateRange != null) {
            clauses.add("${a}dateTimeEpochMillis >= ? AND ${a}dateTimeEpochMillis < ?")
            args.add(dateRange.first)
            args.add(dateRange.second)
        }

        when (ownership) {
            ReceiptOwnershipFilter.FavoritesOnly -> clauses.add("${a}isFavorite = 1")
            ReceiptOwnershipFilter.PinnedOnly -> clauses.add("${a}isPinned = 1")
            ReceiptOwnershipFilter.All -> Unit
        }

        if (!searchQuery.isNullOrBlank()) {
            val trimmed = searchQuery.trim()
            val likeQuery = "%$trimmed%"
            if (useFtsForItemNames) {
                clauses.add(
                    "((${a}companyName LIKE ? OR ${a}fiscalSign LIKE ?) OR ${a}fiscalSign IN (" +
                        "SELECT receiptFiscalSign FROM items_fts WHERE items_fts MATCH ?) OR EXISTS (" +
                        "SELECT 1 FROM items WHERE items.receiptFiscalSign = ${a}fiscalSign " +
                        "AND items.originalName LIKE ?))",
                )
                args.add(likeQuery)
                args.add(likeQuery)
                args.add(ftsMatchQuery(trimmed))
                args.add(likeQuery)
            } else {
                val receiptTable = if (receiptsAlias.isEmpty()) "receipts" else receiptsAlias
                clauses.add(
                    "(${a}companyName LIKE ? OR ${a}fiscalSign LIKE ? OR EXISTS (" +
                        "SELECT 1 FROM items WHERE items.receiptFiscalSign = $receiptTable.fiscalSign " +
                        "AND (items.name LIKE ? OR items.originalName LIKE ?)))",
                )
                args.add(likeQuery)
                args.add(likeQuery)
                args.add(likeQuery)
                args.add(likeQuery)
            }
        }

        return clauses.joinToString(" AND ") to args
    }

    fun orderByClause(sort: ReceiptListSortOrder): String = ReceiptListSortSqlStrategy.orderByClause(sort)

    fun cursorPredicateSql(sort: ReceiptListSortOrder): String = ReceiptListSortSqlStrategy.cursorPredicateSql(sort)

    fun cursorPredicateArgs(sort: ReceiptListSortOrder, cursor: ReceiptListCursor): List<Any> =
        ReceiptListSortSqlStrategy.cursorPredicateArgs(sort, cursor)

    private fun ftsMatchQuery(raw: String): String {
        val tokens = raw.split(Regex("\\s+")).filter { it.length >= 2 }.take(5)
        if (tokens.isEmpty()) {
            return "__nomatch__"
        }
        return tokens.joinToString(" AND ") { token ->
            val escaped = token.replace("\"", "\"\"")
            "name:\"$escaped\"*"
        }
    }

    private fun resolveDateRange(filter: ReceiptFilter): Pair<Long, Long>? {
        val zone = ZoneId.systemDefault()
        val now = LocalDate.now(zone)

        return when (filter) {
            ReceiptFilter.All -> null
            ReceiptFilter.Today -> {
                val start = now.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = now.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                start to end
            }
            ReceiptFilter.LastWeek -> {
                val start = now.minusDays(7).atStartOfDay(zone).toInstant().toEpochMilli()
                val end = now.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                start to end
            }
            ReceiptFilter.LastMonth -> {
                val start = now.minusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val end = now.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                start to end
            }
            is ReceiptFilter.ByDate -> {
                val date = filter.date
                val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                start to end
            }
        }
    }

}
