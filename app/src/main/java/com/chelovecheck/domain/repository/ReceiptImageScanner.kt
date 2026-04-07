package com.chelovecheck.domain.repository

import com.chelovecheck.domain.model.ReceiptScanResult

interface ReceiptImageScanner {
    suspend fun scan(bytes: ByteArray): ReceiptScanResult?
}
