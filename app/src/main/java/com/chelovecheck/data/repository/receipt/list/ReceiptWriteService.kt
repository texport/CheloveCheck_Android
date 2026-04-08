package com.chelovecheck.data.repository.receipt.list

import com.chelovecheck.data.local.ReceiptDao
import com.chelovecheck.data.mapper.toEntity
import com.chelovecheck.domain.model.Item
import com.chelovecheck.domain.model.Receipt
import com.chelovecheck.domain.repository.ReceiptsChangeTracker

internal class ReceiptWriteService(
    private val receiptDao: ReceiptDao,
    private val receiptsChangeTracker: ReceiptsChangeTracker,
) {
    suspend fun saveReceipt(receipt: Receipt) {
        val entity = receipt.toEntity()
        val items = receipt.items.mapIndexed { index, item ->
            item.toEntity(receipt.fiscalSign, index)
        }
        val payments = receipt.totalType.map { it.toEntity(receipt.fiscalSign) }
        receiptDao.insertReceiptWithRelations(entity, items, payments)
        receiptsChangeTracker.notifyChanged()
    }

    suspend fun setReceiptFavorite(fiscalSign: String, favorite: Boolean) {
        receiptDao.setFavorite(fiscalSign, favorite)
        receiptsChangeTracker.notifyChanged()
    }

    suspend fun setReceiptPinned(fiscalSign: String, pinned: Boolean) {
        receiptDao.setPinned(fiscalSign, pinned)
        receiptsChangeTracker.notifyChanged()
    }

    suspend fun replaceReceiptFromFetch(receipt: Receipt) {
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

    suspend fun deleteAllReceipts() {
        receiptDao.deleteAll()
        receiptsChangeTracker.notifyChanged()
    }

    suspend fun deleteReceipt(fiscalSign: String) {
        receiptDao.deleteByFiscalSign(fiscalSign)
        receiptsChangeTracker.notifyChanged()
    }

    suspend fun updateReceiptAddress(fiscalSign: String, address: String) {
        receiptDao.updateReceiptAddress(fiscalSign, address)
        receiptsChangeTracker.notifyChanged()
    }

    suspend fun updateReceiptItems(fiscalSign: String, items: List<Item>) {
        val entities = items.mapIndexed { index, item ->
            item.toEntity(fiscalSign, index)
        }
        receiptDao.replaceItemsForReceipt(fiscalSign, entities)
        val total = items.sumOf { it.sum }
        receiptDao.updateReceiptTotal(fiscalSign, total)
        receiptsChangeTracker.notifyChanged()
    }
}
