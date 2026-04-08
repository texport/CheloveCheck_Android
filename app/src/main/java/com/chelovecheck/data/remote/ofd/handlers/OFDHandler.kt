package com.chelovecheck.data.remote.ofd.handlers

import com.chelovecheck.domain.model.AppError
import com.chelovecheck.domain.model.Receipt

interface OFDHandler {
    suspend fun fetchReceipt(url: String): Receipt

    suspend fun fetchReceiptWithCaptchaToken(url: String, captchaToken: String): Receipt {
        throw AppError.UnsupportedDomain
    }
}
