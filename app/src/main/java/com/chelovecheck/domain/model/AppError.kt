package com.chelovecheck.domain.model

sealed class AppError(
    override val cause: Throwable? = null,
) : Exception(cause) {
    data class Unknown(override val cause: Throwable? = null) : AppError(cause)
    data object InvalidQrCode : AppError()
    data object UnsupportedDomain : AppError()
    data class FailedToSaveReceipt(override val cause: Throwable? = null) : AppError(cause)
    data class NetworkError(override val cause: Throwable? = null) : AppError(cause)
    data class DatabaseError(override val cause: Throwable? = null) : AppError(cause)
    data class ParsingError(val details: String? = null) : AppError()
    data object CameraAccessDenied : AppError()
    data object CameraUnavailable : AppError()
    data class CameraError(override val cause: Throwable? = null) : AppError(cause)
    data class PdfError(override val cause: Throwable? = null) : AppError(cause)
    data object MissingParameters : AppError()
    data object PhotoNotRecognized : AppError()
    data object ReceiptNotFound : AppError()
    /**
     * OFD returned a captcha page or blocked automatic fetch; [receiptUrl] is the same URL the user scanned
     * (open in WebView or browser to complete verification, then retry with token in-app if supported).
     */
    data class ReceiptRequiresOfdVerification(val receiptUrl: String) : AppError()
    data class ReceiptAlreadyExists(val fiscalSign: String) : AppError()
    data class SslError(override val cause: Throwable? = null) : AppError(cause)
}
