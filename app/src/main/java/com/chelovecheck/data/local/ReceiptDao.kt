package com.chelovecheck.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface ReceiptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: ReceiptEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItems(items: List<ItemEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPayments(payments: List<PaymentEntity>)

    @Transaction
    suspend fun insertReceiptWithRelations(
        receipt: ReceiptEntity,
        items: List<ItemEntity>,
        payments: List<PaymentEntity>,
    ) {
        insertReceipt(receipt)
        if (items.isNotEmpty()) insertItems(items)
        if (payments.isNotEmpty()) insertPayments(payments)
    }

    @Transaction
    @Query("SELECT * FROM receipts WHERE fiscalSign = :fiscalSign")
    suspend fun getReceipt(fiscalSign: String): ReceiptWithRelations?

    @Transaction
    @Query("SELECT * FROM receipts ORDER BY dateTimeEpochMillis DESC")
    suspend fun getAllReceipts(): List<ReceiptWithRelations>

    @Transaction
    @RawQuery(observedEntities = [ReceiptEntity::class, ItemEntity::class, PaymentEntity::class])
    suspend fun getReceipts(query: SupportSQLiteQuery): List<ReceiptWithRelations>

    @RawQuery(observedEntities = [ReceiptEntity::class, ItemEntity::class])
    suspend fun getReceiptListRows(query: SupportSQLiteQuery): List<ReceiptWithItemCount>

    @Query("SELECT COUNT(*) FROM receipts")
    suspend fun countReceipts(): Int

    @Query("DELETE FROM receipts")
    suspend fun deleteAll()

    @Query("DELETE FROM receipts WHERE fiscalSign = :fiscalSign")
    suspend fun deleteByFiscalSign(fiscalSign: String)

    @Query("SELECT fiscalSign FROM receipts")
    suspend fun getAllFiscalSigns(): List<String>

    @Query("DELETE FROM items WHERE receiptFiscalSign = :fiscalSign")
    suspend fun deleteItemsByReceipt(fiscalSign: String)

    @Query("UPDATE receipts SET companyAddress = :address WHERE fiscalSign = :fiscalSign")
    suspend fun updateReceiptAddress(fiscalSign: String, address: String)

    @Query("UPDATE receipts SET totalSum = :totalSum WHERE fiscalSign = :fiscalSign")
    suspend fun updateReceiptTotal(fiscalSign: String, totalSum: Double)

    @Transaction
    suspend fun replaceItemsForReceipt(fiscalSign: String, items: List<ItemEntity>) {
        deleteItemsByReceipt(fiscalSign)
        if (items.isNotEmpty()) insertItems(items)
    }

    @Query("DELETE FROM payments WHERE receiptFiscalSign = :fiscalSign")
    suspend fun deletePaymentsByReceipt(fiscalSign: String)

    @Query("UPDATE receipts SET isFavorite = :favorite WHERE fiscalSign = :fiscalSign")
    suspend fun setFavorite(fiscalSign: String, favorite: Boolean)

    @Query("UPDATE receipts SET isPinned = :pinned WHERE fiscalSign = :fiscalSign")
    suspend fun setPinned(fiscalSign: String, pinned: Boolean)

    @Transaction
    suspend fun replaceReceiptFromFetch(
        receipt: ReceiptEntity,
        items: List<ItemEntity>,
        payments: List<PaymentEntity>,
    ) {
        deleteItemsByReceipt(receipt.fiscalSign)
        deletePaymentsByReceipt(receipt.fiscalSign)
        insertReceipt(receipt)
        if (items.isNotEmpty()) insertItems(items)
        if (payments.isNotEmpty()) insertPayments(payments)
    }
}
