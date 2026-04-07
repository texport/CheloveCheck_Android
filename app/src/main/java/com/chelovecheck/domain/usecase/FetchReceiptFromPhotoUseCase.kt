package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.AppError
import com.chelovecheck.domain.model.Receipt
import com.chelovecheck.domain.model.ReceiptScanResult
import com.chelovecheck.domain.repository.ReceiptImageScanner
import javax.inject.Inject

class FetchReceiptFromPhotoUseCase @Inject constructor(
    private val receiptImageScanner: ReceiptImageScanner,
    private val fetchReceiptByUrlUseCase: FetchReceiptByUrlUseCase,
    private val fetchReceiptFromManualUseCase: FetchReceiptFromManualUseCase,
) {
    suspend operator fun invoke(bytes: ByteArray): Receipt {
        val result = receiptImageScanner.scan(bytes) ?: throw AppError.PhotoNotRecognized
        return when (result) {
            is ReceiptScanResult.Url -> fetchReceiptByUrlUseCase(result.url)
            is ReceiptScanResult.Manual -> {
                fetchReceiptFromManualUseCase(
                    t = result.t,
                    i = result.i,
                    f = result.f,
                    s = result.s.orEmpty(),
                )
            }
        }
    }
}
