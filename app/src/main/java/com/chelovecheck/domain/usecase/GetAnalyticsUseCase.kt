package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.analytics.ReceiptLineEdgeCases
import com.chelovecheck.domain.analytics.RetailDisplayGroupResolver
import com.chelovecheck.domain.analytics.ServiceLineHeuristic
import com.chelovecheck.domain.analytics.aggregateUniqueItemBuckets
import com.chelovecheck.domain.analytics.inPeriod
import com.chelovecheck.domain.model.AnalyticsSummary
import com.chelovecheck.domain.model.CategoryItemTotal
import com.chelovecheck.domain.model.CategoryTotal
import com.chelovecheck.domain.model.CategoryIds
import com.chelovecheck.domain.model.PendingCategoryItem
import com.chelovecheck.domain.model.MerchantTotal
import com.chelovecheck.domain.model.PaymentTotal
import com.chelovecheck.domain.model.PaymentType
import com.chelovecheck.domain.model.CategoryPrediction
import com.chelovecheck.domain.model.OperationType
import com.chelovecheck.domain.model.AnalyticsLoadStage
import com.chelovecheck.domain.model.analyticsSourceName
import com.chelovecheck.domain.model.RetailClassificationContext
import com.chelovecheck.domain.logging.AppLogger
import com.chelovecheck.domain.repository.AnalyticsProgressReporter
import com.chelovecheck.domain.repository.CategoryRepository
import com.chelovecheck.domain.repository.ReceiptItemClassifier
import com.chelovecheck.domain.repository.ReceiptRepository
import com.chelovecheck.domain.repository.RetailDisplayGroupsRepository
import com.chelovecheck.domain.repository.RetailRepository
import com.chelovecheck.domain.rollup.coicopAnalyticsBucketId
import com.chelovecheck.domain.utils.ItemNameNormalizer
import javax.inject.Inject
import java.time.Instant
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetAnalyticsUseCase @Inject constructor(
    private val receiptRepository: ReceiptRepository,
    private val retailRepository: RetailRepository,
    private val categoryRepository: CategoryRepository,
    private val retailDisplayGroupsRepository: RetailDisplayGroupsRepository,
    private val classifier: ReceiptItemClassifier,
    private val progressReporter: AnalyticsProgressReporter,
    private val logger: AppLogger,
) {
    suspend operator fun invoke(from: Instant? = null, to: Instant? = null): AnalyticsSummary {
        return withContext(Dispatchers.Default) {
            val start = System.currentTimeMillis()
            try {
                val receipts = receiptRepository.getAllReceipts().inPeriod(from, to)
                if (receipts.isEmpty()) {
                    return@withContext AnalyticsSummary(
                        totalSpent = 0.0,
                        receiptsCount = 0,
                        averageReceipt = 0.0,
                        categoryTotals = emptyList(),
                        categoryItems = emptyMap(),
                        leafTotalsByRollup = emptyMap(),
                        paymentTotals = emptyList(),
                        topMerchants = emptyList(),
                    )
                }

                val resolver = retailDisplayGroupsRepository.getResolver()
                val categoriesById = categoryRepository.getAllCategories().associateBy { it.id }
                val uncategorizedDisplay = resolver.displayGroupForRollup(CategoryIds.UNCATEGORIZED)

                val categoryTotals = mutableMapOf<String, Double>()
                val categoryItems = mutableMapOf<String, MutableMap<String, ItemAggregate>>()
                val leafAccumulator = mutableMapOf<String, MutableMap<String, Double>>()
                val merchantTotals = mutableMapOf<String, Double>()
                val paymentTotals = mutableMapOf<PaymentType, Double>()
                val pendingItems = LinkedHashMap<String, PendingCategoryItem>()
                var total = 0.0
                var lineItemCount = 0

                progressReporter.report(AnalyticsLoadStage.ANALYZING_RECEIPTS)
                val networkByReceipt = receipts.associateWith { receipt ->
                    retailRepository.getNetworkName(receipt.iinBin)?.takeIf { it.isNotBlank() }
                        ?: receipt.companyName.takeIf { it.isNotBlank() }
                        ?: "__unknown__"
                }
                val buckets = aggregateUniqueItemBuckets(receipts) { receipt ->
                    networkByReceipt.getValue(receipt)
                }
                val totalUnique = buckets.size
                progressReporter.reportProgress(0, totalUnique.coerceAtLeast(1))
                var processedUnique = 0
                var lastProgressUpdate = 0L
                val progressIntervalMs = 250L

                val predictionsByKey = LinkedHashMap<String, CategoryPrediction>()
                for (bucket in buckets) {
                    val retailContext = RetailClassificationContext(
                        networkName = bucket.networkKey.takeUnless { it == "__unknown__" },
                        bin = null,
                    )
                    val prediction = classifier.classify(bucket.sampleSourceName, retailContext)
                    predictionsByKey[bucket.normalizedKey] = prediction
                    processedUnique++
                    val now = System.currentTimeMillis()
                    if (processedUnique == totalUnique || now - lastProgressUpdate >= progressIntervalMs) {
                        progressReporter.reportProgress(processedUnique, totalUnique)
                        lastProgressUpdate = now
                    }
                    if (!prediction.isCertain) {
                        pendingItems.putIfAbsent(
                            bucket.normalizedKey,
                            PendingCategoryItem(
                                sourceItemName = bucket.sampleSourceName,
                                displayItemName = bucket.sampleDisplayName,
                                candidates = prediction.candidates,
                                networkKey = bucket.networkKey.takeUnless { it == "__unknown__" },
                            ),
                        )
                    }
                }

                for (receipt in receipts) {
                    val receiptTotal = receipt.totalSum
                    total += receiptTotal
                    val merchantKey = retailRepository.getNetworkName(receipt.iinBin)
                        ?.takeIf { it.isNotBlank() }
                        ?: receipt.companyName.ifBlank { "—" }
                    merchantTotals[merchantKey] = (merchantTotals[merchantKey] ?: 0.0) + receiptTotal

                    if (receipt.items.isEmpty()) {
                        categoryTotals[uncategorizedDisplay] =
                            (categoryTotals[uncategorizedDisplay] ?: 0.0) + receiptTotal
                    } else {
                        for (item in receipt.items) {
                            lineItemCount++
                            val sourceName = item.analyticsSourceName()
                            val key = ItemNameNormalizer.normalizeForMatch(sourceName).ifBlank {
                                sourceName.trim()
                            }
                            val networkKey = networkByReceipt.getValue(receipt)
                            val compositeKey = "$key|$networkKey"
                            val prediction = predictionsByKey.getValue(compositeKey)
                            val amount = if (item.sum > 0.0) item.sum else item.price * max(item.count, 1.0)
                            val rawId = detailCategoryId(prediction)
                            val rollupId = coicopAnalyticsBucketId(rawId, categoriesById)

                            val isReturnReceipt = receipt.typeOperation == OperationType.BUY_RETURN ||
                                receipt.typeOperation == OperationType.SELL_RETURN
                            val displayId: String = when {
                                isReturnReceipt -> RetailDisplayGroupResolver.ADJUSTMENTS_GROUP_ID
                                amount <= 0.0 -> RetailDisplayGroupResolver.ADJUSTMENTS_GROUP_ID
                                ReceiptLineEdgeCases.looksLikeExplicitReturnLine(key) ->
                                    RetailDisplayGroupResolver.ADJUSTMENTS_GROUP_ID
                                ReceiptLineEdgeCases.looksLikeGiftCardOrVoucher(key) -> "retail_misc_services"
                                ReceiptLineEdgeCases.looksLikeDepositOrContainerFee(key) -> uncategorizedDisplay
                                ServiceLineHeuristic.matchesNormalized(key) ->
                                    RetailDisplayGroupResolver.SERVICES_GROUP_ID
                                else -> resolver.displayGroupForRollup(rollupId)
                            }

                            categoryTotals[displayId] = (categoryTotals[displayId] ?: 0.0) + amount
                            val leafRollup = if (amount <= 0.0) CategoryIds.UNCATEGORIZED else rollupId
                            val leafMap = leafAccumulator.getOrPut(displayId) { LinkedHashMap() }
                            leafMap[leafRollup] = (leafMap[leafRollup] ?: 0.0) + amount

                            val itemKey = key.ifBlank { sourceName.trim() }
                            val categoryBucket = categoryItems.getOrPut(displayId) { LinkedHashMap() }
                            val aggregate = categoryBucket.getOrPut(itemKey) {
                                ItemAggregate(
                                    sourceName = sourceName,
                                    displayName = item.name,
                                )
                            }
                            aggregate.amount += amount
                            aggregate.count += max(item.count, 1.0).roundToInt()
                            if (item.name.length > aggregate.displayName.length) {
                                aggregate.displayName = item.name
                            }
                        }
                    }
                    receipt.totalType.forEach { payment ->
                        paymentTotals[payment.type] = (paymentTotals[payment.type] ?: 0.0) + payment.sum
                    }
                }

                val safeTotal = if (total <= 0.0) 1.0 else total
                val categoryTotalsList = categoryTotals.entries
                    .sortedByDescending { it.value }
                    .map { (category, amount) ->
                        CategoryTotal(
                            categoryId = category,
                            amount = amount,
                            share = (amount / safeTotal).toFloat(),
                        )
                    }
                val categoryBreakdown = categoryItems.mapValues { (_, items) ->
                    items.values
                        .sortedByDescending { it.amount }
                        .map { aggregate ->
                            CategoryItemTotal(
                                sourceItemName = aggregate.sourceName,
                                displayItemName = aggregate.displayName,
                                amount = aggregate.amount,
                                count = aggregate.count,
                            )
                        }
                }
                val leafTotalsByRollup = leafAccumulator.mapValues { (_, inner) ->
                    val sumInner = inner.values.sum().takeIf { it > 0.0 } ?: 1.0
                    inner.entries
                        .sortedByDescending { it.value }
                        .map { (rollupId, amt) ->
                            CategoryTotal(
                                categoryId = rollupId,
                                amount = amt,
                                share = (amt / sumInner).toFloat(),
                            )
                        }
                }
                val merchants = merchantTotals.entries
                    .sortedByDescending { it.value }
                    .take(5)
                    .map { (name, amount) -> MerchantTotal(name = name, amount = amount) }

                val payments = paymentTotals.entries
                    .sortedByDescending { it.value }
                    .map { (type, amount) ->
                        PaymentTotal(
                            type = type,
                            amount = amount,
                            share = (amount / safeTotal).toFloat(),
                        )
                    }

                logger.debug(
                    "AnalyticsUseCase",
                    "analytics done receipts=${receipts.size} lines=$lineItemCount unique=$totalUnique categories=${categoryTotalsList.size} pending=${pendingItems.size} " +
                        "unknownNetwork=${networkByReceipt.values.count { it == "__unknown__" }} " +
                        "durationMs=${System.currentTimeMillis() - start}",
                )

                AnalyticsSummary(
                    totalSpent = total,
                    receiptsCount = receipts.size,
                    averageReceipt = total / receipts.size.coerceAtLeast(1),
                    categoryTotals = categoryTotalsList,
                    categoryItems = categoryBreakdown,
                    leafTotalsByRollup = leafTotalsByRollup,
                    paymentTotals = payments,
                    topMerchants = merchants,
                    pendingItems = pendingItems.values.toList(),
                )
            } finally {
                progressReporter.clear()
            }
        }
    }

    private data class ItemAggregate(
        val sourceName: String,
        var displayName: String,
        var amount: Double = 0.0,
        var count: Int = 0,
    )

    /**
     * Leaf or rollup COICOP id before [coicopAnalyticsBucketId] (preferred L2 rollup for analytics).
     */
    private fun detailCategoryId(prediction: CategoryPrediction): String {
        val direct = prediction.categoryId
        if (prediction.isCertain) {
            return direct ?: CategoryIds.UNCATEGORIZED
        }
        if (direct != null) return direct
        return prediction.candidates.firstOrNull()?.categoryId ?: CategoryIds.UNCATEGORIZED
    }
}
