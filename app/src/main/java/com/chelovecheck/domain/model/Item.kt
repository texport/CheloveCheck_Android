package com.chelovecheck.domain.model

data class Item(
    val id: Long = 0,
    val barcode: String?,
    val codeMark: String?,
    val name: String,
    val count: Double,
    val price: Double,
    val unit: UnitOfMeasurement,
    val sum: Double,
    val taxType: String?,
    val taxSum: Double?,
)
