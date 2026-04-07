package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.AppError
import com.chelovecheck.domain.model.Receipt
import com.chelovecheck.domain.repository.ReceiptFetcher
import java.net.URL
import javax.inject.Inject

class FetchReceiptWithCaptchaTokenUseCase @Inject constructor(
    private val receiptFetcher: ReceiptFetcher,
) {
    suspend operator fun invoke(url: String, captchaToken: String): Receipt {
        val normalized = if (url.startsWith("http://")) {
            url.replaceFirst("http://", "https://")
        } else {
            url
        }
        try {
            URL(normalized)
        } catch (error: Exception) {
            throw AppError.InvalidQrCode
        }
        return receiptFetcher.fetchReceiptWithCaptchaToken(normalized, captchaToken)
    }
}
