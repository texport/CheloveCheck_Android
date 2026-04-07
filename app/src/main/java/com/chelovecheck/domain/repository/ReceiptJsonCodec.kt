package com.chelovecheck.domain.repository

import com.chelovecheck.domain.model.Receipt

/**
 * JSON round-trip for [Receipt] lists (export/import).
 *
 * **Categories:** item-level COICOP overrides and on-device prediction cache are stored in Room, not in this JSON.
 * Changing `analytics_retail_groups.json` does not affect imported receipts until analytics runs again.
 */
interface ReceiptJsonCodec {
    fun encode(receipts: List<Receipt>): String
    fun decode(json: String): List<Receipt>
}
