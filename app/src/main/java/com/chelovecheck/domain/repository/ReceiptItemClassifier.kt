package com.chelovecheck.domain.repository

import com.chelovecheck.domain.model.CategoryPrediction
import com.chelovecheck.domain.model.RetailClassificationContext

interface ReceiptItemClassifier {
    suspend fun classify(
        name: String,
        retailContext: RetailClassificationContext? = null,
    ): CategoryPrediction
}
