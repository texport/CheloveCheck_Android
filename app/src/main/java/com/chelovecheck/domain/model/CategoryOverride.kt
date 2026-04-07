package com.chelovecheck.domain.model

data class CategoryOverride(
    val id: Long = 0,
    val itemName: String,
    val categoryId: String,
    val embedding: FloatArray,
)
