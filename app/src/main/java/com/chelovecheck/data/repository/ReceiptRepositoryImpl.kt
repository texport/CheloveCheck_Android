package com.chelovecheck.data.repository

import androidx.sqlite.db.SimpleSQLiteQuery
import com.chelovecheck.data.local.ReceiptDao
import com.chelovecheck.data.repository.receipt.list.ReceiptListQueryBuilder
import com.chelovecheck.data.repository.receipt.list.ReceiptListSearchStrategy
import com.chelovecheck.data.repository.receipt.list.ReceiptWriteService
import com.chelovecheck.data.mapper.toDomain
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
import javax.inject.Inject

class ReceiptRepositoryImpl @Inject constructor(
    private val receiptDao: ReceiptDao,
    private val receiptsChangeTracker: ReceiptsChangeTracker,
) : ReceiptRepository {
    private val queryBuilder = ReceiptListQueryBuilder()
    private val writeService = ReceiptWriteService(receiptDao, receiptsChangeTracker)
    private val searchStrategy = ReceiptListSearchStrategy(::getReceiptListPageInternal)

    override suspend fun saveReceipt(receipt: Receipt) {
        writeService.saveReceipt(receipt)
    }

    override suspend fun saveReceipts(receipts: List<Receipt>): SaveManyResult {
        val existing = receiptDao.getAllFiscalSigns().toMutableSet()
        val imported = mutableListOf<Receipt>()
        val skipped = mutableListOf<Receipt>()

        receipts.forEach { receipt ->
            if (existing.contains(receipt.fiscalSign)) {
                skipped.add(receipt)
            } else {
                writeService.saveReceipt(receipt)
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
        val (whereSql, whereArgs) = queryBuilder.buildWhere(filter, searchQuery)
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
        return searchStrategy.load(filter, searchQuery, cursor, limit, ownership, sortOrder)
    }

    override suspend fun setReceiptFavorite(fiscalSign: String, favorite: Boolean) {
        writeService.setReceiptFavorite(fiscalSign, favorite)
    }

    override suspend fun setReceiptPinned(fiscalSign: String, pinned: Boolean) {
        writeService.setReceiptPinned(fiscalSign, pinned)
    }

    override suspend fun replaceReceiptFromFetch(receipt: Receipt) {
        writeService.replaceReceiptFromFetch(receipt)
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
        val (whereSql, whereArgs) = queryBuilder.buildWhereForList(
            receiptsAlias = "r",
            filter = filter,
            searchQuery = searchQuery,
            useFtsForItemNames = useFtsForItemNames,
            ownership = ownership,
        )
        val orderBy = queryBuilder.orderByClause(sortOrder)
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
                append(queryBuilder.cursorPredicateSql(sortOrder))
            }
            append(" ORDER BY ")
            append(orderBy)
            append(" LIMIT ?")
        }
        val args = mutableListOf<Any>()
        args.addAll(whereArgs)
        if (cursor != null) {
            args.addAll(queryBuilder.cursorPredicateArgs(sortOrder, cursor))
        }
        args.add(limit)
        val query = SimpleSQLiteQuery(sql, args.toTypedArray())
        return receiptDao.getReceiptListRows(query).map { it.toSummary() }
    }

    override suspend fun getAllReceipts(): List<Receipt> {
        return receiptDao.getAllReceipts().map { it.toDomain() }
    }

    override suspend fun getReceipt(fiscalSign: String): Receipt? {
        return receiptDao.getReceipt(fiscalSign)?.toDomain()
    }

    override suspend fun deleteAllReceipts() {
        writeService.deleteAllReceipts()
    }

    override suspend fun deleteReceipt(fiscalSign: String) {
        writeService.deleteReceipt(fiscalSign)
    }

    override suspend fun updateReceiptAddress(fiscalSign: String, address: String) {
        writeService.updateReceiptAddress(fiscalSign, address)
    }

    override suspend fun updateReceiptItems(fiscalSign: String, items: List<Item>) {
        writeService.updateReceiptItems(fiscalSign, items)
    }

    override suspend fun countReceipts(): Int {
        return receiptDao.countReceipts()
    }

}
