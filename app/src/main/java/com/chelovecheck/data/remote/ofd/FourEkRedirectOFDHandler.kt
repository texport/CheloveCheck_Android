package com.chelovecheck.data.remote.ofd

import com.chelovecheck.domain.model.AppError
import com.chelovecheck.domain.model.Receipt
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

/**
 * Handles short links like `https://4ek.kz/?o=transtelecom&i=...&f=...&s=...&t=...`
 * and delegates to a concrete OFD handler after building a canonical OFD URL.
 */
class FourEkRedirectOFDHandler @Inject constructor(
    private val transtelecomHandler: TranstelecomOFDHandler,
    private val kazakhtelecomHandler: KazakhtelecomOFDHandler,
    private val jusanHandler: JusanOFDHandler,
) : OFDHandler {

    override suspend fun fetchReceipt(url: String): Receipt {
        val resolved = FourEkRedirectResolver.resolve(url)
        return when (resolved.provider) {
            Provider.TRANSTELECOM -> transtelecomHandler.fetchReceipt(resolved.url)
            Provider.KAZAKHTELECOM -> kazakhtelecomHandler.fetchReceipt(resolved.url)
            Provider.JUSAN -> jusanHandler.fetchReceipt(resolved.url)
        }
    }

    override suspend fun fetchReceiptWithCaptchaToken(url: String, captchaToken: String): Receipt {
        val resolved = FourEkRedirectResolver.resolve(url)
        return when (resolved.provider) {
            Provider.TRANSTELECOM -> transtelecomHandler.fetchReceiptWithCaptchaToken(resolved.url, captchaToken)
            Provider.KAZAKHTELECOM -> kazakhtelecomHandler.fetchReceiptWithCaptchaToken(resolved.url, captchaToken)
            Provider.JUSAN -> jusanHandler.fetchReceiptWithCaptchaToken(resolved.url, captchaToken)
        }
    }
}

internal enum class Provider { TRANSTELECOM, KAZAKHTELECOM, JUSAN }

internal object FourEkRedirectResolver {
    fun resolve(rawUrl: String): ResolvedTarget {
        val uri = runCatching { URI.create(rawUrl) }.getOrElse { throw AppError.InvalidQrCode }
        val params = parseQuery(uri.rawQuery)
        val provider = params["o"]?.lowercase().orEmpty()
        val i = params["i"]?.trim().orEmpty()
        val f = params["f"]?.trim().orEmpty()
        val t = params["t"]?.trim().orEmpty()
        val s = params["s"]?.trim().orEmpty()
        if (provider.isBlank() || i.isBlank() || f.isBlank() || t.isBlank()) throw AppError.MissingParameters
        return when (provider) {
            "transtelecom" -> ResolvedTarget(Provider.TRANSTELECOM, buildCanonicalUrl("https://ofd1.kz/t/", t, i, f, s))
            "kazakhtelecom", "oofd" ->
                ResolvedTarget(Provider.KAZAKHTELECOM, buildCanonicalUrl("https://consumer.oofd.kz", t, i, f, s))
            "kofd", "jusan" ->
                ResolvedTarget(Provider.JUSAN, buildCanonicalUrl("https://consumer.kofd.kz", t, i, f, s))
            else -> throw AppError.UnsupportedDomain
        }
    }

    private fun buildCanonicalUrl(host: String, t: String, i: String, f: String, s: String): String {
        val parts = buildList {
            add("t=$t")
            add("i=$i")
            add("f=$f")
            if (s.isNotBlank()) add("s=${s.replace(",", ".")}")
        }
        return "$host?${parts.joinToString("&")}"
    }

    private fun parseQuery(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrBlank()) return emptyMap()
        return rawQuery
            .split("&")
            .mapNotNull { part ->
                val idx = part.indexOf("=")
                if (idx <= 0) return@mapNotNull null
                val key = URLDecoder.decode(part.substring(0, idx), StandardCharsets.UTF_8)
                val value = URLDecoder.decode(part.substring(idx + 1), StandardCharsets.UTF_8)
                key to value
            }
            .toMap()
    }
}

internal data class ResolvedTarget(
    val provider: Provider,
    val url: String,
)
