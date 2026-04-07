package com.chelovecheck.domain.repository

import com.chelovecheck.domain.model.ReceiptQuery

interface ReceiptUrlBuilder {
    fun buildUrl(query: ReceiptQuery): String
}
