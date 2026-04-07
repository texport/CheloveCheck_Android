package com.chelovecheck.domain.export

import com.chelovecheck.domain.model.Receipt

object ReceiptCsvFormatter {
    fun formatAll(receipts: List<Receipt>): String {
        val header = "fiscalSign,dateTime,company,totalKzt,itemsCount"
        val lines = receipts.map { r ->
            val esc = { s: String -> "\"${s.replace("\"", "\"\"")}\"" }
            listOf(
                r.fiscalSign,
                r.dateTime.toString(),
                esc(r.companyName),
                r.totalSum.toString(),
                r.items.size.toString(),
            ).joinToString(",")
        }
        return (listOf(header) + lines).joinToString("\n")
    }
}
