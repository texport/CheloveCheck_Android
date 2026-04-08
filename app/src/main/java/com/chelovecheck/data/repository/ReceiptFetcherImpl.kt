package com.chelovecheck.data.repository

import com.chelovecheck.data.remote.ofd.handlers.OFDHandlerManager
import com.chelovecheck.domain.model.AppError
import com.chelovecheck.domain.model.Receipt
import com.chelovecheck.domain.repository.ReceiptFetcher
import java.net.URI
import javax.inject.Inject

class ReceiptFetcherImpl @Inject constructor(
    private val handlerManager: OFDHandlerManager,
) : ReceiptFetcher {
    override suspend fun fetchReceiptByUrl(url: String): Receipt {
        val host = try {
            URI(url).host
        } catch (error: Exception) {
            throw AppError.InvalidQrCode
        }

        val handler = handlerManager.handlerForHost(host)
            ?: throw AppError.UnsupportedDomain

        return handler.fetchReceipt(url)
    }

    override suspend fun fetchReceiptWithCaptchaToken(url: String, captchaToken: String): Receipt {
        val host = try {
            URI(url).host
        } catch (error: Exception) {
            throw AppError.InvalidQrCode
        }

        val handler = handlerManager.handlerForHost(host)
            ?: throw AppError.UnsupportedDomain

        return handler.fetchReceiptWithCaptchaToken(url, captchaToken)
    }
}
