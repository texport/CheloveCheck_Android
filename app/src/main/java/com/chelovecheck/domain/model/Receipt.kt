package com.chelovecheck.domain.model

import java.time.Instant

data class Receipt(
    val companyName: String,
    val certificateVat: String?,
    val iinBin: String,
    val companyAddress: String,
    val serialNumber: String,
    val kgdId: String,
    val dateTime: Instant,
    val fiscalSign: String,
    val ofd: Ofd,
    val typeOperation: OperationType,
    val items: List<Item>,
    val url: String,
    val taxesType: String?,
    val taxesSum: Double?,
    val taken: Double?,
    val change: Double?,
    val totalType: List<Payment>,
    val totalSum: Double,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
)
