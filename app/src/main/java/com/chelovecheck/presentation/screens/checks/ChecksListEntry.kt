package com.chelovecheck.presentation.screens.checks

import com.chelovecheck.domain.model.ReceiptGroupMode
import com.chelovecheck.domain.model.ReceiptListItem
import com.chelovecheck.domain.model.ReceiptListSortOrder
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

sealed interface ChecksListEntry {
    val stableKey: String

    data class SectionHeader(
        val sortKey: String,
        val title: String,
    ) : ChecksListEntry {
        override val stableKey: String get() = "header-$sortKey"
    }

    data class ReceiptRow(
        val item: ReceiptListItem,
    ) : ChecksListEntry {
        override val stableKey: String get() = item.summary.fiscalSign
    }
}

fun buildChecksListEntries(
    receipts: List<ReceiptListItem>,
    mode: ReceiptGroupMode,
    sortOrder: ReceiptListSortOrder,
    zone: ZoneId,
    formatDayTitle: (LocalDate) -> String,
    formatMonthTitle: (YearMonth) -> String,
): List<ChecksListEntry> {
    if (mode == ReceiptGroupMode.NONE || receipts.isEmpty()) {
        return receipts.map { ChecksListEntry.ReceiptRow(it) }
    }
    val grouped: Map<String, List<ReceiptListItem>> = when (mode) {
        ReceiptGroupMode.BY_DAY -> receipts.groupBy { receipt ->
            LocalDate.ofInstant(receipt.summary.dateTime, zone).toString()
        }
        ReceiptGroupMode.BY_MONTH -> receipts.groupBy { receipt ->
            val d = LocalDate.ofInstant(receipt.summary.dateTime, zone)
            YearMonth.from(d).toString()
        }
        ReceiptGroupMode.NONE -> error("handled")
    }
    val descending = when (sortOrder) {
        ReceiptListSortOrder.DATE_OLDEST,
        ReceiptListSortOrder.AMOUNT_ASC,
        ReceiptListSortOrder.MERCHANT_AZ,
        -> false
        ReceiptListSortOrder.DEFAULT,
        ReceiptListSortOrder.DATE_NEWEST,
        ReceiptListSortOrder.AMOUNT_DESC,
        -> true
    }
    val sortedKeys = if (descending) grouped.keys.sortedDescending() else grouped.keys.sorted()
    val out = mutableListOf<ChecksListEntry>()
    for (key in sortedKeys) {
        val rows = grouped[key].orEmpty()
        val title = when (mode) {
            ReceiptGroupMode.BY_DAY -> formatDayTitle(LocalDate.parse(key))
            ReceiptGroupMode.BY_MONTH -> formatMonthTitle(YearMonth.parse(key))
            ReceiptGroupMode.NONE -> ""
        }
        out.add(ChecksListEntry.SectionHeader(sortKey = key, title = title))
        rows.forEach { out.add(ChecksListEntry.ReceiptRow(it)) }
    }
    return out
}
