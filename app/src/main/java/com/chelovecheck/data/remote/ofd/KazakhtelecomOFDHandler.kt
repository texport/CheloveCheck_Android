package com.chelovecheck.data.remote.ofd

import com.chelovecheck.data.remote.HttpClient
import com.chelovecheck.domain.model.AppError
import com.chelovecheck.domain.model.Item
import com.chelovecheck.domain.model.Ofd
import com.chelovecheck.domain.model.OperationType
import com.chelovecheck.domain.model.Payment
import com.chelovecheck.domain.model.PaymentType
import com.chelovecheck.domain.model.Receipt
import com.chelovecheck.domain.model.UnitOfMeasurement
import com.chelovecheck.domain.logging.AppLogger
import java.net.URLEncoder
import java.net.URLDecoder
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatterBuilder
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class KazakhtelecomOFDHandler @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
    private val logger: AppLogger,
) : OFDHandler {
    private data class Endpoint(val baseUrl: String, val supportsUrlParam: Boolean)

    private companion object {
        val ENDPOINTS = listOf(
            Endpoint(
                baseUrl = "https://consumer.oofd.kz/api/consumer-proxy/api/tickets/get-by-url",
                supportsUrlParam = false,
            ),
            Endpoint(
                baseUrl = "https://consumer.oofd.kz/api/tickets/get-by-url",
                supportsUrlParam = true,
            ),
        )
        const val USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/97.0.4692.71 Safari/537.36"
        const val MAX_RESPONSE_SIZE_BYTES = 10 * 1024 * 1024
        const val TAG = "KazakhtelecomOFD"
        const val LOG_CHUNK_SIZE = 3000
    }

    override suspend fun fetchReceipt(url: String): Receipt {
        logDebug("fetchReceipt start, initialUrl=$url")
        val apiUrls = buildApiUrls(url)
        logDebug("apiUrls=${apiUrls.joinToString(" | ")}")
        var lastError: AppError? = null

        for (apiUrl in apiUrls) {
            logDebug("requesting apiUrl=$apiUrl")
            val response = try {
                httpClient.get(
                    apiUrl,
                    headers = mapOf(
                        "User-Agent" to USER_AGENT,
                        "Accept" to "application/json",
                    ),
                )
            } catch (error: Throwable) {
                logger.error(TAG, "request failed", error)
                lastError = AppError.NetworkError(error)
                continue
            }
            logDebug("response code=${response.code}")
            logDebug("response headers=${response.headers}")

            if (response.code != 200) {
                logDebug("response non-200: ${response.code}")
                logLong("response body", response.body)
                lastError = when (response.code) {
                    400 -> AppError.InvalidQrCode
                    404 -> AppError.ReceiptNotFound
                    else -> AppError.NetworkError(IllegalStateException("HTTP ${response.code}"))
                }
                continue
            }

            val contentType = response.headers["Content-Type"].orEmpty()
            if (contentType.isNotBlank() && !contentType.contains("application/json")) {
                logDebug("invalid content-type=$contentType")
                lastError = AppError.ReceiptNotFound
                continue
            }

            val bodyBytes = response.body.toByteArray()
            logDebug("response body size=${bodyBytes.size} bytes")
            logLong("response body", response.body)
            if (bodyBytes.size > MAX_RESPONSE_SIZE_BYTES) {
                logDebug("response too large: ${bodyBytes.size} > $MAX_RESPONSE_SIZE_BYTES")
                lastError = AppError.ParsingError()
                continue
            }

            val parsed = runCatching {
                val jsonObject = json.parseToJsonElement(response.body).jsonObject
                parseReceipt(jsonObject, url)
            }.getOrElse { error ->
                logger.error(TAG, "parse error", error)
                lastError = (error as? AppError) ?: AppError.Unknown(error)
                null
            }

            if (parsed != null) {
                logDebug("parsed receipt summary: org=${parsed.companyName}, bin=${parsed.iinBin}, items=${parsed.items.size}, payments=${parsed.totalType.size}, total=${parsed.totalSum}")
                logLong("parsed receipt", parsed.toString())
                return parsed
            }
        }

        throw lastError ?: AppError.ReceiptNotFound
    }

    private fun buildApiUrls(initialUrl: String): List<String> {
        val urls = mutableListOf<String>()
        val params = extractParams(initialUrl)
        logDebug("extracted params=$params")
        val encodedUrl = URLEncoder.encode(initialUrl, "UTF-8")
        ENDPOINTS.forEach { endpoint ->
            if (params.isNotEmpty()) {
                urls.add(buildUrlWithParams(endpoint.baseUrl, params))
            }
            if (endpoint.supportsUrlParam) {
                urls.add("${endpoint.baseUrl}?url=$encodedUrl")
            }
        }

        return urls.distinct()
    }

    private fun extractParams(initialUrl: String): Map<String, String?> {
        val query = when {
            initialUrl.contains("?") -> initialUrl.substringAfter("?").substringBefore("#")
            initialUrl.contains("=") -> initialUrl.substringBefore("#")
            else -> ""
        }

        if (query.isBlank()) return emptyMap()

        val map = mutableMapOf<String, String?>()
        query.split("&").forEach { part ->
            if (part.isBlank()) return@forEach
            val pieces = part.split("=", limit = 2)
            val key = decode(pieces[0]) ?: return@forEach
            val value = when {
                pieces.size == 1 -> null
                pieces[1].isEmpty() -> ""
                else -> decode(pieces[1])
            }
            map[key] = value
        }
        return map
    }

    private fun buildUrlWithParams(baseUrl: String, params: Map<String, String?>): String {
        val queryParts = listOf(
            "t" to params["t"],
            "i" to params["i"],
            "f" to params["f"],
            "s" to params["s"],
        ).map { (key, value) ->
            val encodedKey = URLEncoder.encode(key, "UTF-8")
            val encodedValue = value?.let { URLEncoder.encode(it, "UTF-8") }
            if (encodedValue == null) {
                encodedKey
            } else {
                "$encodedKey=$encodedValue"
            }
        }

        return "$baseUrl?${queryParts.joinToString("&")}"
    }

    private fun decode(value: String): String? {
        return runCatching { URLDecoder.decode(value, "UTF-8") }.getOrNull()
    }

    private fun logDebug(message: String) {
        logger.debug(TAG, message)
    }

    private fun logLong(prefix: String, value: String) {
        if (value.isBlank()) {
            logDebug("$prefix: <empty>")
            return
        }
        logDebug("$prefix length=${value.length}")
        var index = 0
        while (index < value.length) {
            val end = (index + LOG_CHUNK_SIZE).coerceAtMost(value.length)
            logger.debug(TAG, "$prefix[$index..$end]: ${value.substring(index, end)}")
            index = end
        }
    }

    private fun parseReceipt(jsonObject: JsonObject, url: String): Receipt {
        val ticket = jsonObject["ticket"]?.jsonObject
            ?: throw AppError.ParsingError()

        val itemsJson = ticket["items"]?.jsonArray
            ?: throw AppError.ParsingError()

        val orgTitle = jsonObject["orgTitle"].stringValue()
        val orgId = jsonObject["orgId"].stringValue()
        val orgAddress = jsonObject["retailPlaceAddress"].stringValue()
        val kkmSerialNumber = jsonObject["kkmSerialNumber"].stringValue()
        val kkmFnsId = jsonObject["kkmFnsId"].stringValue()
        val fiscalId = ticket["fiscalId"].stringValue()
        val operationTypeId = ticket["operationType"].intValue()
        val totalSum = ticket["totalSum"].doubleValue()

        val operationType = OperationType.fromId(operationTypeId)
            ?: throw AppError.ParsingError()

        val transactionDate = ticket["transactionDate"].stringValue()
        val dateTime = parseDate(transactionDate)

        val paymentsJson = ticket["payments"]?.jsonArray.orEmpty()
        val payments = paymentsJson.mapNotNull { paymentJson ->
            val paymentObject = paymentJson.jsonObject
            val paymentType = paymentObject["paymentType"].stringValue().lowercase()
            val sum = paymentObject["sum"].doubleValue()
            val mappedType = when (paymentType.trim()) {
                "card" -> PaymentType.CARD
                "cash" -> PaymentType.CASH
                "mobile" -> PaymentType.MOBILE
                else -> null
            }
            mappedType?.let { Payment(type = it, sum = sum) }
        }

        val items = itemsJson.mapNotNull { itemJson ->
            val itemObject = itemJson.jsonObject
            val commodity = itemObject["commodity"]?.jsonObject ?: return@mapNotNull null
            val rawName = commodity["name"].stringValue()
            val quantity = commodity["quantity"].doubleValue()
            val price = commodity["price"].doubleValue()
            val sum = commodity["sum"].doubleValue()

            val name = rawName.replace(Regex("[\r\n]"), "")
            val barcode = commodity["barcode"]?.jsonPrimitive?.content
            val exciseStamp = commodity["exciseStamp"]?.jsonPrimitive?.content
            val measureUnitCode = commodity["measureUnitCode"]?.jsonPrimitive?.content
            val unit = UnitOfMeasurement.from(measureUnitCode)

            val taxData = commodity["taxes"]?.jsonArray?.firstOrNull()?.jsonObject
            val taxType = taxData?.get("layout")?.jsonObject?.get("rate")?.jsonPrimitive?.doubleOrNull
            val taxSum = taxData?.get("sum")?.jsonPrimitive?.doubleOrNull

            Item(
                barcode = barcode,
                codeMark = exciseStamp,
                name = name,
                count = quantity,
                price = price,
                unit = unit,
                sum = sum,
                taxType = taxType?.toString(),
                taxSum = taxSum,
            )
        }

        val taxes = jsonObject["taxes"]?.jsonArray?.firstOrNull()?.jsonObject
        val taxesType = taxes?.get("rate")?.jsonPrimitive?.doubleOrNull?.toString()
        val taxesSum = taxes?.get("sum")?.jsonPrimitive?.doubleOrNull

        val takenSum = ticket["takenSum"]?.jsonPrimitive?.doubleOrNull
        val changeSum = ticket["changeSum"]?.jsonPrimitive?.doubleOrNull

        return Receipt(
            companyName = orgTitle,
            certificateVat = null,
            iinBin = orgId,
            companyAddress = orgAddress,
            serialNumber = kkmSerialNumber,
            kgdId = kkmFnsId,
            dateTime = dateTime,
            fiscalSign = fiscalId,
            ofd = Ofd.KAZAKHTELECOM,
            typeOperation = operationType,
            items = items,
            url = url,
            taxesType = taxesType,
            taxesSum = taxesSum,
            taken = takenSum,
            change = changeSum,
            totalType = payments,
            totalSum = totalSum,
        )
    }

    private fun parseDate(value: String): Instant {
        val formatter = DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .optionalStart()
            .appendPattern(".SSS")
            .optionalEnd()
            .optionalStart()
            .appendOffsetId()
            .optionalEnd()
            .toFormatter()

        return runCatching { OffsetDateTime.parse(value, formatter).toInstant() }
            .getOrElse {
                val localDate = LocalDateTime.parse(value, formatter)
                localDate.atZone(ZoneId.of("Asia/Almaty")).toInstant()
            }
    }

    private fun JsonObject.stringValue(key: String): String {
        return this[key].stringValue()
    }

    private fun JsonObject.intValue(key: String): Int {
        return this[key].intValue()
    }

    private fun JsonObject.doubleValue(key: String): Double {
        return this[key].doubleValue()
    }

    private fun kotlinx.serialization.json.JsonElement?.stringValue(): String {
        val primitive = this?.jsonPrimitive
        return primitive?.content ?: ""
    }

    private fun kotlinx.serialization.json.JsonElement?.intValue(): Int {
        val primitive = this?.jsonPrimitive
        return primitive?.intOrNull ?: 0
    }

    private fun kotlinx.serialization.json.JsonElement?.doubleValue(): Double {
        val primitive = this?.jsonPrimitive
        return primitive?.doubleOrNull ?: 0.0
    }
}
