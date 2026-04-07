package com.chelovecheck.domain.model

import java.time.LocalDate

sealed class ReceiptFilter {
    data object All : ReceiptFilter()
    data object Today : ReceiptFilter()
    data object LastWeek : ReceiptFilter()
    data object LastMonth : ReceiptFilter()
    data class ByDate(val date: LocalDate) : ReceiptFilter()
}
