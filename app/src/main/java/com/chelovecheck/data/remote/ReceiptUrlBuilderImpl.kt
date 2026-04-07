package com.chelovecheck.data.remote

import com.chelovecheck.domain.model.AppError
import com.chelovecheck.domain.model.Ofd
import com.chelovecheck.domain.model.ReceiptQuery
import com.chelovecheck.domain.repository.ReceiptUrlBuilder
import javax.inject.Inject

class ReceiptUrlBuilderImpl @Inject constructor() : ReceiptUrlBuilder {
    override fun buildUrl(query: ReceiptQuery): String {
        return when (query.ofd) {
            Ofd.KAZAKHTELECOM -> buildWithParams(
                host = "https://consumer.oofd.kz",
                t = query.t,
                i = query.i,
                f = query.f,
                s = query.s,
                requireSum = true,
            )
            Ofd.KOFD -> buildWithParams(
                host = "https://consumer.kofd.kz",
                t = query.t,
                i = query.i,
                f = query.f,
                s = query.s,
                requireSum = false,
            )
            Ofd.WOFD -> buildWithParams(
                host = "https://consumer.wofd.kz",
                t = query.t,
                i = query.i,
                f = query.f,
                s = query.s,
                requireSum = true,
            )
            Ofd.KASPI -> {
                val extTranId = query.i?.trim().orEmpty()
                val saleDateRaw = query.t?.trim().orEmpty()
                if (extTranId.isBlank() || saleDateRaw.isBlank()) {
                    throw AppError.MissingParameters
                }
                val saleDate = saleDateRaw
                    .replace("T", " ")
                    .replace("+", "%2B")
                "https://receipt.kaspi.kz/web?extTranId=$extTranId&sale_date=$saleDate"
            }
            Ofd.TRANSTELECOM -> {
                val url = query.url?.trim().orEmpty()
                if (url.isBlank()) {
                    throw AppError.MissingParameters
                }
                url
            }
        }
    }

    private fun buildWithParams(
        host: String,
        t: String?,
        i: String?,
        f: String?,
        s: String?,
        requireSum: Boolean,
    ): String {
        val timestamp = t?.trim().orEmpty()
        val ticket = i?.trim().orEmpty()
        val fiscal = f?.trim().orEmpty()
        val sum = s?.trim().orEmpty().replace(",", ".")

        if (timestamp.isBlank() || ticket.isBlank() || fiscal.isBlank()) {
            throw AppError.MissingParameters
        }

        if (requireSum && sum.isBlank()) {
            throw AppError.MissingParameters
        }

        val params = buildList {
            add("t=$timestamp")
            add("i=$ticket")
            add("f=$fiscal")
            if (sum.isNotBlank()) add("s=$sum")
        }

        return "$host?${params.joinToString("&")}"
    }
}
