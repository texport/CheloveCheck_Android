package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.CategoryPrediction
import com.chelovecheck.domain.repository.ReceiptItemClassifier
import javax.inject.Inject

class GetCategoryCandidatesUseCase @Inject constructor(
    private val classifier: ReceiptItemClassifier,
) {
    suspend operator fun invoke(name: String): CategoryPrediction {
        return classifier.classify(name)
    }
}
