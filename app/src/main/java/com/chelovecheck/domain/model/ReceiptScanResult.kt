package com.chelovecheck.domain.model

sealed interface ReceiptScanResult {
    data class Url(val url: String) : ReceiptScanResult
    data class Manual(
        val t: String,
        val i: String,
        val f: String,
        val s: String?,
    ) : ReceiptScanResult
}
