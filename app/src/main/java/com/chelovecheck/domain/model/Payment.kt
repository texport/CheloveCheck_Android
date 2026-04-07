package com.chelovecheck.domain.model

data class Payment(
    val type: PaymentType,
    val sum: Double,
)
