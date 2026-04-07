package com.chelovecheck.domain.analytics

import com.chelovecheck.domain.model.Item
import com.chelovecheck.domain.model.Receipt
import com.chelovecheck.domain.utils.ItemNameNormalizer
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * @param normalizedKey Composite key: `normalizedItemName|networkKey` (see [aggregateUniqueItemBuckets]).
 */
data class UniqueItemBucket(
    val normalizedKey: String,
    val networkKey: String,
    var sampleDisplayName: String,
    var totalAmount: Double,
    var totalCount: Int,
)

fun aggregateUniqueItemBuckets(
    receipts: List<Receipt>,
    networkKeyFor: (Receipt) -> String,
): List<UniqueItemBucket> {
    val map = LinkedHashMap<String, UniqueItemBucket>()
    for (receipt in receipts) {
        val networkKey = networkKeyFor(receipt)
        for (item in receipt.items) {
            accumulateItem(map, item, networkKey)
        }
    }
    return map.values.toList()
}

private fun accumulateItem(
    map: LinkedHashMap<String, UniqueItemBucket>,
    item: Item,
    networkKey: String,
) {
    val nameKey = ItemNameNormalizer.normalizeForMatch(item.name).ifBlank { item.name.trim() }
    val compositeKey = "$nameKey|$networkKey"
    val display = item.name.trim()
    val amount = if (item.sum > 0.0) item.sum else item.price * max(item.count, 1.0)
    val count = max(item.count, 1.0).roundToInt()
    val existing = map[compositeKey]
    if (existing == null) {
        map[compositeKey] = UniqueItemBucket(
            normalizedKey = compositeKey,
            networkKey = networkKey,
            sampleDisplayName = display,
            totalAmount = amount,
            totalCount = count,
        )
    } else {
        existing.totalAmount += amount
        existing.totalCount += count
        if (display.length > existing.sampleDisplayName.length) {
            existing.sampleDisplayName = display
        }
    }
}
