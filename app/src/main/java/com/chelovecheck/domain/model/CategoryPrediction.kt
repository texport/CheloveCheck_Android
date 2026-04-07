package com.chelovecheck.domain.model

data class CategoryCandidate(
    val categoryId: String,
    val score: Float,
)

data class CategoryPrediction(
    val categoryId: String?,
    val confidence: Float,
    val candidates: List<CategoryCandidate>,
    val isCertain: Boolean,
)
