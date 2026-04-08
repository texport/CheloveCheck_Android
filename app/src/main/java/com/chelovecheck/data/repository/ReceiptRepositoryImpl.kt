package com.chelovecheck.data.repository

import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery
import com.chelovecheck.data.local.ReceiptDao
import com.chelovecheck.data.mapper.toDomain
import com.chelovecheck.data.mapper.toEntity
import com.chelovecheck.data.mapper.toSummary
import com.chelovecheck.domain.model.Item
import com.chelovecheck.domain.model.Receipt
import com.chelovecheck.domain.model.ReceiptFilter
import com.chelovecheck.domain.model.ReceiptListCursor
import com.chelovecheck.domain.model.ReceiptListSortOrder
import com.chelovecheck.domain.model.ReceiptListSummary
import com.chelovecheck.domain.model.ReceiptOwnershipFilter
import com.chelovecheck.domain.repository.ReceiptRepository
import com.chelovecheck.domain.repository.ReceiptsChangeTracker
import com.chelovecheck.domain.repository.SaveManyResult
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class ReceiptRepositoryImpl @Inject constructor(
    private val receiptDao: ReceiptDao,
    private val receiptsChangeTracker: ReceiptsChangeTracker,
) : ReceiptRepository {
    override suspend fun saveReceipt(receipt: Receipt) {
        val entity = receipt.toEntity()
        val items = receipt.items.mapIndexed { index, item ->
            item.toEntity(receipt.fiscalSign, index)
        }
        val payments = receipt.totalType.map { it.toEntity(receipt.fiscalSign) }
        receiptDao.insertReceiptWithRelations(entity, items, payments)
        receiptsChangeTracker.notifyChanged()
    }

    override suspend fun saveReceipts(receipts: List<Receipt>): SaveManyResult {
        val existing = receiptDao.getAllFiscalSigns().toMutableSet()
        val imported = mutableListOf<Receipt>()
        val skipped = mutableListOf<Receipt>()

        receipts.forEach { receipt ->
            if (existing.contains(receipt.fiscalSign)) {
                skipped.add(receipt)
            } else {
                saveReceipt(receipt)
                existing.add(receipt.fiscalSign)
                imported.add(receipt)
            }
        }

        return SaveManyResult(imported = imported, skipped = skipped)
    }

    override suspend fun getReceipts(
        filter: ReceiptFilter,
        searchQuery: String?,
        offset: Int,
        limit: Int,
    ): List<Receipt> {
        val (whereSql, whereArgs) = buildWhere(filter, searchQuery)
        val sql = buildString {
            append("SELECT * FROM receipts")
            if (whereSql.isNotBlank()) {
                append(" WHERE ")
                append(whereSql)
            }
            append(" ORDER BY isPinned DESC, isFavorite DESC, dateTimeEpochMillis DESC")
            append(" LIMIT ? OFFSET ?")
        }

        val args = (whereArgs + listOf(limit, offset)).toTypedArray()
        val query = SimpleSQLiteQuery(sql, args)
        return receiptDao.getReceipts(query).map { it.toDomain() }
    }

    override suspend fun getReceiptListPage(
        filter: ReceiptFilter,
        searchQuery: String?,
        cursor: ReceiptListCursor?,
        limit: Int,
        ownership: ReceiptOwnershipFilter,
        sortOrder: ReceiptListSortOrder,
    ): List<ReceiptListSummary> {
        Log.d(
            "ChecksSearch",
            "list page request: filter=$filter query='${searchQuery?.take(80)}' cursor=${cursor?.fiscalSign ?: "null"} " +
                "limit=$limit ownership=$ownership sort=$sortOrder",
        )
        return try {
            val result = getReceiptListPageInternal(
                filter = filter,
                searchQuery = searchQuery,
                cursor = cursor,
                limit = limit,
                ownership = ownership,
                useFtsForItemNames = true,
                sortOrder = sortOrder,
            )
            Log.d("ChecksSearch", "list page result (fts=true): count=${result.size}")
            result
        } catch (_: Exception) {
            Log.d("ChecksSearch", "list page fallback: fts failed, retry with LIKE")
            val result = getReceiptListPageInternal(
                filter = filter,
                searchQuery = searchQuery,
                cursor = cursor,
                limit = limit,
                ownership = ownership,
                useFtsForItemNames = false,
                sortOrder = sortOrder,
            )
            Log.d("ChecksSearch", "list page result (fts=false): count=${result.size}")
            result
        }
    }

    override suspend fun setReceiptFavorite(fiscalSign: String, favorite: Boolean) {
        receiptDao.setFavorite(fiscalSign, favorite)
        receiptsChangeTracker.notifyChanged()
    }

    override suspend fun setReceiptPinned(fiscalSign: String, pinned: Boolean) {
        receiptDao.setPinned(fiscalSign, pinned)
        receiptsChangeTracker.notifyChanged()
    }

    override suspend fun replaceReceiptFromFetch(receipt: Receipt) {
        val existing = receiptDao.getReceipt(receipt.fiscalSign)?.receipt
        val merged = receipt.copy(
            isFavorite = existing?.isFavorite ?: receipt.isFavorite,
            isPinned = existing?.isPinned ?: receipt.isPinned,
        )
        val entity = merged.toEntity()
        val items = merged.items.mapIndexed { index, item ->
            item.toEntity(receipt.fiscalSign, index)
        }
        val payments = merged.totalType.map { it.toEntity(receipt.fiscalSign) }
        receiptDao.replaceReceiptFromFetch(entity, items, payments)
        receiptsChangeTracker.notifyChanged()
    }

    private suspend fun getReceiptListPageInternal(
        filter: ReceiptFilter,
        searchQuery: String?,
        cursor: ReceiptListCursor?,
        limit: Int,
        ownership: ReceiptOwnershipFilter,
        useFtsForItemNames: Boolean,
        sortOrder: ReceiptListSortOrder,
    ): List<ReceiptListSummary> {
        val (whereSql, whereArgs) = buildWhereForList(
            receiptsAlias = "r",
            filter = filter,
            searchQuery = searchQuery,
            useFtsForItemNames = useFtsForItemNames,
            ownership = ownership,
        )
        val orderBy = orderByClause(sortOrder)
        val sql = buildString {
            append(
                "SELECT r.*, (SELECT COUNT(*) FROM items WHERE receiptFiscalSign = r.fiscalSign) AS itemsCount ",
            )
            append("FROM receipts r WHERE ")
            if (whereSql.isNotBlank()) {
                append(whereSql)
            } else {
                append("1=1")
            }
            if (cursor != null) {
                append(" AND ")
                append(cursorPredicateSql(sortOrder))
            }
            append(" ORDER BY ")
            append(orderBy)
            append(" LIMIT ?")
        }
        val args = mutableListOf<Any>()
        args.addAll(whereArgs)
        if (cursor != null) {
            args.addAll(cursorPredicateArgs(sortOrder, cursor))
        }
        args.add(limit)
        val query = SimpleSQLiteQuery(sql, args.toTypedArray())
        return receiptDao.getReceiptListRows(query).map { it.toSummary() }
    }

    private fun orderByClause(sort: ReceiptListSortOrder): String = when (sort) {
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

    private fun cursorPredicateSql(sort: ReceiptListSortOrder): String = when (sort) {
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

    private fun cursorPredicateArgs(sort: ReceiptListSortOrder, cursor: ReceiptListCursor): List<Any> {
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

    override suspend fun getAllReceipts(): List<Receipt> {
        return receiptDao.getAllReceipts().map { it.toDomain() }
    }

    override suspend fun getReceipt(fiscalSign: String): Receipt? {
        return receiptDao.getReceipt(fiscalSign)?.toDomain()
    }

    override suspend fun deleteAllReceipts() {
        receiptDao.deleteAll()
        receiptsChangeTracker.notifyChanged()
    }

    override suspend fun deleteReceipt(fiscalSign: String) {
        receiptDao.deleteByFiscalSign(fiscalSign)
        receiptsChangeTracker.notifyChanged()
    }

    override suspend fun updateReceiptAddress(fiscalSign: String, address: String) {
        receiptDao.updateReceiptAddress(fiscalSign, address)
        receiptsChangeTracker.notifyChanged()
    }

    override suspend fun updateReceiptItems(fiscalSign: String, items: List<Item>) {
        val entities = items.mapIndexed { index, item ->
            item.toEntity(fiscalSign, index)
        }
        receiptDao.replaceItemsForReceipt(fiscalSign, entities)
        val total = items.sumOf { it.sum }
        receiptDao.updateReceiptTotal(fiscalSign, total)
        receiptsChangeTracker.notifyChanged()
    }

    override suspend fun countReceipts(): Int {
        return receiptDao.countReceipts()
    }

    private fun buildWhere(
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
    private fun buildWhereForList(
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

        Log.d(
            "ChecksSearch",
            "where built: filter=$filter ownership=$ownership fts=$useFtsForItemNames " +
                "query='${searchQuery?.trim()?.take(80)}' clause='${clauses.joinToString(" AND ")}' args=${args.size}",
        )

        return clauses.joinToString(" AND ") to args
    }

    /**
     * Builds an FTS4 [MATCH] pattern for [items_fts] (indexed column [name] only).
     * FTS5-specific `column : "term*"` syntax is not used; Android SQLite often lacks fts5.
     */
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

    private fun Boolean.asSqlInt(): Int = if (this) 1 else 0
}
