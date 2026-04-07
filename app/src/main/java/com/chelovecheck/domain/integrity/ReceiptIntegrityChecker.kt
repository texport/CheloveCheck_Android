package com.chelovecheck.domain.integrity

import com.chelovecheck.domain.model.Receipt
import kotlin.math.abs

/**
 * Lightweight structural checks: line totals vs receipt total (KISS).
 */
object ReceiptIntegrityChecker {

    fun check(receipt: Receipt): List<String> {
        val issues = mutableListOf<String>()
        val itemsSum = receipt.items.sumOf { it.sum }
        val paymentsSum = receipt.totalType.sumOf { it.sum }
        val tol = 1.0
        if (abs(itemsSum - receipt.totalSum) > tol) {
            issues.add("items_sum_mismatch")
        }
        if (abs(paymentsSum - receipt.totalSum) > tol) {
            issues.add("payments_sum_mismatch")
        }
        return issues
    }
}
