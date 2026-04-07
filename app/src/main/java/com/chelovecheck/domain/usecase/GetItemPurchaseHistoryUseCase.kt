package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.ItemPurchaseRow
import com.chelovecheck.domain.repository.ReceiptRepository
import com.chelovecheck.domain.utils.ItemNameNormalizer
import javax.inject.Inject

/**
 * Finds recent purchases of items with the same normalized name (for product history / deep analytics).
 */
class GetItemPurchaseHistoryUseCase @Inject constructor(
    private val repository: ReceiptRepository,
) {
    suspend operator fun invoke(itemName: String, limit: Int = 50): List<ItemPurchaseRow> {
        val key = ItemNameNormalizer.normalizeForMatch(itemName)
        if (key.isBlank()) return emptyList()
        val receipts = repository.getAllReceipts()
        val rows = mutableListOf<ItemPurchaseRow>()
        for (r in receipts) {
            for (item in r.items) {
                if (ItemNameNormalizer.normalizeForMatch(item.name) == key) {
                    rows.add(
                        ItemPurchaseRow(
                            fiscalSign = r.fiscalSign,
                            companyName = r.companyName,
                            dateTimeEpochMillis = r.dateTime.toEpochMilli(),
                            itemName = item.name,
                            sum = item.sum,
                            quantity = item.count,
                        ),
                    )
                }
            }
        }
        return rows.sortedByDescending { it.dateTimeEpochMillis }.take(limit)
    }
}
