package com.chelovecheck.data.remote.ofd.handlers

import com.chelovecheck.data.remote.ofd.debug.OfdDebugLog
import com.chelovecheck.data.remote.ofd.http.OfdHttpExecutor
import com.chelovecheck.data.remote.ofd.http.OfdHttpHeaders
import com.chelovecheck.data.remote.ofd.http.OfdReceiptFetchHttpPolicy
import com.chelovecheck.data.remote.ofd.parse.OfdParsingCommons
import com.chelovecheck.data.remote.ofd.parse.TranstelecomTicketInstantParser
import com.chelovecheck.data.remote.ofd.sanitize.OfdResponseSanitizer
import android.net.Uri
import com.chelovecheck.data.remote.HttpClient
import com.chelovecheck.domain.model.AppError
import com.chelovecheck.domain.model.Item
import com.chelovecheck.domain.model.Ofd
import com.chelovecheck.domain.model.OperationType
import com.chelovecheck.domain.model.Payment
import com.chelovecheck.domain.model.PaymentType
import com.chelovecheck.domain.model.Receipt
import com.chelovecheck.domain.model.UnitOfMeasurement
import com.chelovecheck.data.telemetry.DataLayerReason
import com.chelovecheck.data.telemetry.DataTelemetry
import com.chelovecheck.domain.logging.AppLogger
import javax.inject.Inject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

class TranstelecomOFDHandler @Inject constructor(
    private val httpClient: HttpClient,
    private val logger: AppLogger,
) : OFDHandler {
    private val httpExecutor = OfdHttpExecutor(httpClient, logger)

    private companion object {
        const val TAG = "TranstelecomOFD"
    }

    override suspend fun fetchReceipt(url: String): Receipt {
        logDebug("fetchReceipt start, url=${OfdResponseSanitizer.sanitizeUrl(url)}")
        val response = httpExecutor.getTransportOrThrow(TAG, url, defaultHtmlHeaders())
        logDebug("response headers=${response.headers}")
        logLong("response body", response.body)
        OfdReceiptFetchHttpPolicy.throwIfBadForReceiptFetch(response.code)

        val body = response.body
        if (looksLikeTicketHtml(body)) {
            return parseTicketDocument(Jsoup.parse(body), url)
        }
        if (isCaptchaLandingPage(body)) {
            return fetchAfterCaptchaOrThrow(url)
        }
        return runCatching { parseTicketDocument(Jsoup.parse(body), url) }
            .getOrElse { error ->
                DataTelemetry.logParserFailure(
                    logger,
                    TAG,
                    DataLayerReason.OFD_PARSE,
                    error,
                )
                if (isCaptchaLandingPage(body) || looksLikeCaptchaMarkers(body)) {
                    throw AppError.ReceiptRequiresOfdVerification(url)
                }
                throw error
            }
    }

    private suspend fun fetchAfterCaptchaOrThrow(originalUrl: String): Receipt {
        val fallbackUrl = buildFragmentFallbackUrl(originalUrl)
        logDebug("captcha landing detected, trying XHR fragment fallback: $fallbackUrl")
        val retry = try {
            httpExecutor.getTransportOrThrow(TAG, fallbackUrl, xhrFragmentHeaders())
        } catch (error: Throwable) {
            logger.error(TAG, "fragment fallback request failed", error)
            throw AppError.ReceiptRequiresOfdVerification(originalUrl)
        }
        logDebug("fallback response code=${retry.code}")
        logLong("fallback body", retry.body)
        OfdReceiptFetchHttpPolicy.throwIfBadForReceiptFetch(retry.code)
        if (looksLikeTicketHtml(retry.body)) {
            return parseTicketDocument(Jsoup.parse(retry.body), originalUrl)
        }
        if (isCaptchaLandingPage(retry.body) || looksLikeCaptchaMarkers(retry.body)) {
            throw AppError.ReceiptRequiresOfdVerification(originalUrl)
        }
        return runCatching { parseTicketDocument(Jsoup.parse(retry.body), originalUrl) }
            .getOrElse { error ->
                DataTelemetry.logParserFailure(
                    logger,
                    TAG,
                    DataLayerReason.OFD_PARSE,
                    error,
                )
                throw AppError.ReceiptRequiresOfdVerification(originalUrl)
            }
    }

    override suspend fun fetchReceiptWithCaptchaToken(url: String, captchaToken: String): Receipt {
        logDebug("fetchReceiptWithCaptchaToken url=$url")
        val built = buildUrlWithCaptchaToken(url, captchaToken)
        val response = httpExecutor.getTransportOrThrow(TAG, built, xhrFragmentHeaders())
        logLong("captcha token response body", response.body)
        OfdReceiptFetchHttpPolicy.throwIfBadForReceiptFetch(response.code)
        if (!looksLikeTicketHtml(response.body)) {
            if (isCaptchaLandingPage(response.body)) {
                throw AppError.ReceiptRequiresOfdVerification(url)
            }
            throw AppError.ParsingError()
        }
        return parseTicketDocument(Jsoup.parse(response.body), url)
    }

    private fun defaultHtmlHeaders(): Map<String, String> = mapOf("Accept" to "text/html")

    private fun xhrFragmentHeaders(): Map<String, String> = mapOf(
        "Accept" to "text/html",
        "X-Requested-With" to "XMLHttpRequest",
    )

    private fun buildFragmentFallbackUrl(url: String): String {
        return Uri.parse(url).buildUpon()
            .appendQueryParameter("fragment", "1")
            .appendQueryParameter("captcha_skip", "1")
            .build()
            .toString()
    }

    private fun buildUrlWithCaptchaToken(url: String, captchaToken: String): String {
        return Uri.parse(url).buildUpon()
            .appendQueryParameter("fragment", "1")
            .appendQueryParameter("captcha_skip", "1")
            .appendQueryParameter("captcha_token", captchaToken)
            .build()
            .toString()
    }

    private fun looksLikeTicketHtml(html: String): Boolean {
        return html.contains("ticket_header", ignoreCase = true) ||
            html.contains("ready_ticket__items_list", ignoreCase = true)
    }

    private fun looksLikeCaptchaMarkers(html: String): Boolean {
        val lower = html.lowercase()
        return lower.contains("g-recaptcha") ||
            lower.contains("google.com/recaptcha") ||
            lower.contains("data-sitekey") ||
            lower.contains("ticketcaptcha") ||
            lower.contains("recaptcha")
    }

    private fun isCaptchaLandingPage(html: String): Boolean {
        if (looksLikeTicketHtml(html)) return false
        val lower = html.lowercase()
        if (looksLikeCaptchaMarkers(html)) return true
        return lower.contains("captcha") && lower.contains("ofd")
    }

    private fun parseTicketDocument(document: Document, url: String): Receipt {
        return runCatching { convertDocumentToReceipt(document, url) }
            .getOrElse { error ->
                logger.error(TAG, "convert document failed", error)
                throw error
            }
    }

    private fun convertDocumentToReceipt(document: Document, url: String): Receipt {
        val companyName = document.selectFirst("div.ticket_header > div > span")?.text().orEmpty()
        val companyAddress = document.select("div.ticket_header > div:contains(Адрес) > span").text()
        val iinBin = document.select("div.ticket_header > div:contains(БИН) > span").text()
        val serialNumber = document.select("div.ticket_header > div:contains(ЗНМ) > span").text()
        val kgdId = document.select("div.ticket_header > div:contains(РНМ) > span").text()
        val fiscalSign = document.select("div.ticket_footer > div:contains(Фискальный признак) > span").text()

        val dateTimeText = document.select("div.ticket_header > div:contains(Дата и время) > span").text()
        val dateTime = TranstelecomTicketInstantParser.parse(dateTimeText, url)

        val typeOperation = extractOperationType(document)
        val items = parseItems(document.select("ol.ready_ticket__items_list > li"))
        val totalType = extractTotalType(document)
        val totalSum = document.select("div.total_sum > div > b > span").text()
            .replace(" ", "")
            .replace(",", ".")
            .toDoubleOrNull() ?: 0.0
        val change = extractChange(document)

        return Receipt(
            companyName = companyName,
            certificateVat = null,
            iinBin = iinBin,
            companyAddress = companyAddress,
            serialNumber = serialNumber,
            kgdId = kgdId,
            dateTime = dateTime,
            fiscalSign = fiscalSign,
            ofd = Ofd.TRANSTELECOM,
            typeOperation = typeOperation,
            items = items,
            url = url,
            taxesType = null,
            taxesSum = null,
            taken = null,
            change = change,
            totalType = totalType,
            totalSum = totalSum,
        )
    }

    private fun extractOperationType(document: Document): OperationType {
        val operationTypeText = document
            .select("div.ticket_header > div:contains(Кассалық чек / Кассовый чек) > span")
            .text()
            .trim()

        val operationType = operationTypeText.split("/").lastOrNull()?.trim().orEmpty()
        val cleaned = operationType.lowercase()

        val buyKeywords = listOf("покупка", "сатып алу", "purchase", "купить", "buy")
        val buyReturnKeywords = listOf("возврат покупки", "сатып алуды қайтару", "purchase return", "refund", "return")
        val sellKeywords = listOf("продажа", "сату", "sale", "sell")
        val sellReturnKeywords = listOf("возврат продажи", "сатуды қайтару", "sale return", "return sale")

        return when {
            buyKeywords.any { cleaned.contains(it) } -> OperationType.BUY
            buyReturnKeywords.any { cleaned.contains(it) } -> OperationType.BUY_RETURN
            sellKeywords.any { cleaned.contains(it) } -> OperationType.SELL
            sellReturnKeywords.any { cleaned.contains(it) } -> OperationType.SELL_RETURN
            else -> throw AppError.ParsingError()
        }
    }

    private fun parseItems(elements: Elements): List<Item> {
        return elements.mapNotNull { element ->
            val nameSpan = element.selectFirst("span.wb-all") ?: return@mapNotNull null
            var name = nameSpan.text().trim()
            var barcode: String? = null

            val barcodeMatch = Regex("^(\\d{13}|\\d{8})").find(name)
            if (barcodeMatch != null) {
                barcode = barcodeMatch.value
                name = name.replace(barcodeMatch.value, "").trim()
            }

            val itemInfoDiv = element.selectFirst("div.ready_ticket__item")
                ?: throw AppError.ParsingError()

            itemInfoDiv.select("b").remove()
            val itemInfo = itemInfoDiv.text()

            val xIndex = itemInfo.indexOf('x')
            val equalsIndex = itemInfo.indexOf('=')

            if (xIndex == -1 || equalsIndex == -1) {
                throw AppError.ParsingError()
            }

            val priceText = itemInfo.substring(0, xIndex).replace(" ", "").trim()
            val countAndUnit = itemInfo.substring(xIndex + 1, equalsIndex).trim()

            val components = countAndUnit.split(" ").filter { it.isNotBlank() }
            var countText = ""
            var unitText = ""

            if (components.size >= 2) {
                val last = components.last()
                if (last.all { it.isLetter() }) {
                    unitText = last
                    countText = components.dropLast(1).joinToString(" ")
                } else {
                    countText = components.first()
                    unitText = components.drop(1).joinToString(" ")
                }
            } else if (components.isNotEmpty()) {
                countText = components.first()
            }

            val unit = UnitOfMeasurement.from(unitText)
            countText = countText.replace(",", ".").replace(" ", "")
            val count = countText.toDoubleOrNull()
                ?: throw AppError.ParsingError()

            val sumStart = equalsIndex + 1
            val sumEnd = itemInfo.indexOf("ҚҚС", startIndex = sumStart).takeIf { it > 0 } ?: itemInfo.length
            val sumText = itemInfo.substring(sumStart, sumEnd)
                .replace(",", ".")
                .replace(" ", "")
                .trim()

            val (taxType, taxSum) = if (itemInfo.contains("ҚҚС")) {
                extractTaxInfo(itemInfo)
            } else {
                null to null
            }

            val price = priceText.toDoubleOrNull()
                ?: throw AppError.ParsingError()
            val sum = sumText.toDoubleOrNull()
                ?: throw AppError.ParsingError()

            Item(
                barcode = barcode,
                codeMark = null,
                name = name,
                count = count,
                price = price,
                unit = unit,
                sum = sum,
                taxType = taxType,
                taxSum = taxSum,
            )
        }
    }

    private fun extractTaxInfo(itemInfo: String): Pair<String?, Double?> {
        val taxStart = itemInfo.indexOf("ҚҚС")
        if (taxStart == -1) return null to null

        val taxInfo = itemInfo.substring(taxStart + 3).trim()
        val parts = taxInfo.split(":")
        val taxType = parts.firstOrNull()?.trim()
        val taxSum = parts.lastOrNull()?.trim()
            ?.replace(" ", "")
            ?.replace(",", ".")
            ?.toDoubleOrNull()
        return taxType to taxSum
    }

    private fun extractTotalType(document: Document): List<Payment> {
        val paymentTypeMapping = mapOf(
            PaymentType.CASH to listOf("нал", "наличные", "cash"),
            PaymentType.CARD to listOf("карт", "карта", "bank", "card"),
            PaymentType.MOBILE to listOf("моб", "мобильные", "mobile"),
        )

        val totalSumDiv = document.selectFirst("div.total_sum")
            ?: throw AppError.ParsingError()
        val totalTypeUl = totalSumDiv.selectFirst("ul.list-unstyled")
            ?: throw AppError.ParsingError()

        return totalTypeUl.select("li").map { listItem ->
            val listItemText = listItem.text().trim()
            val colonIndex = listItemText.indexOf(':')
            if (colonIndex == -1) {
                throw AppError.ParsingError()
            }

            val paymentTypeText = listItemText.substring(0, colonIndex).trim()
            val paymentAmountText = listItemText.substring(colonIndex + 1)
                .replace(",", ".")
                .replace(" ", "")
                .trim()

            val paymentAmount = paymentAmountText.toDoubleOrNull()
                ?: throw AppError.ParsingError()

            val paymentType = paymentTypeMapping.entries.firstOrNull { entry ->
                entry.value.any { paymentTypeText.lowercase().contains(it) }
            }?.key ?: paymentTypeText.toIntOrNull()?.let { PaymentType.fromId(it) }
            ?: throw AppError.ParsingError()

            Payment(type = paymentType, sum = paymentAmount)
        }
    }

    private fun extractChange(document: Document): Double? {
        val totalSumDiv = document.selectFirst("div.total_sum") ?: return null
        val changeDiv = totalSumDiv.selectFirst("div:contains(Тапсыру /  Сдача:)") ?: return null

        val changeText = changeDiv.text()
            .replace(",", ".")
            .replace(" ", "")
            .trim()

        val colonIndex = changeText.indexOf(':')
        if (colonIndex == -1) return null

        val amountText = changeText.substring(colonIndex + 1).trim()
        return amountText.toDoubleOrNull()
    }

    private fun logDebug(message: String) {
        logger.debug(TAG, message)
    }

    private fun logLong(prefix: String, value: String) {
        if (value.isBlank()) {
            logDebug("$prefix: <empty>")
            return
        }
        val preview = OfdResponseSanitizer.sanitizeBodyPreview(value)
        logDebug("$prefix length=${value.length} preview=$preview")
    }
}
