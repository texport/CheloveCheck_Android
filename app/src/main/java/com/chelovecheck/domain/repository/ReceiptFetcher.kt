package com.chelovecheck.domain.repository

import com.chelovecheck.domain.model.Receipt

interface ReceiptFetcher {
    suspend fun fetchReceiptByUrl(url: String): Receipt

    suspend fun fetchReceiptWithCaptchaToken(url: String, captchaToken: String): Receipt
}
