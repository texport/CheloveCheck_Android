package com.chelovecheck.domain.model

data class AnalyticsSummary(
    val totalSpent: Double,
    val receiptsCount: Int,
    val averageReceipt: Double,
    val categoryTotals: List<CategoryTotal>,
    val categoryItems: Map<String, List<CategoryItemTotal>> = emptyMap(),
    /** Spending by finer COICOP node within each rollup group (rollup id -> breakdown). */
    val leafTotalsByRollup: Map<String, List<CategoryTotal>> = emptyMap(),
    val paymentTotals: List<PaymentTotal>,
    val topMerchants: List<MerchantTotal>,
    val pendingItems: List<PendingCategoryItem> = emptyList(),
)

data class CategoryTotal(
    val categoryId: String,
    val amount: Double,
    val share: Float,
)

data class CategoryItemTotal(
    val itemName: String,
    val amount: Double,
    val count: Int,
)

data class MerchantTotal(
    val name: String,
    val amount: Double,
)

data class PaymentTotal(
    val type: PaymentType,
    val amount: Double,
    val share: Float,
)

data class PendingCategoryItem(
    val itemName: String,
    val candidates: List<CategoryCandidate>,
    /** Retail network name from BIN when distinct from [itemName] bucket; null if unknown. */
    val networkKey: String? = null,
)
