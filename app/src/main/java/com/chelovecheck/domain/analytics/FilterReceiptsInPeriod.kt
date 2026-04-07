package com.chelovecheck.domain.analytics

import com.chelovecheck.domain.model.Receipt
import java.time.Instant

fun List<Receipt>.inPeriod(from: Instant?, to: Instant?): List<Receipt> {
    return filter { receipt ->
        val time = receipt.dateTime
        val afterStart = from?.let { !time.isBefore(it) } ?: true
        val beforeEnd = to?.let { !time.isAfter(it) } ?: true
        afterStart && beforeEnd
    }
}
