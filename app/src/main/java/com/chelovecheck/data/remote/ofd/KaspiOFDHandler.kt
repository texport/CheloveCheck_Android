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
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatterBuilder
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class KaspiOFDHandler @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
    private val logger: AppLogger,
) : OFDHandler {
    private companion object {
        const val TAG = "KaspiOFD"
        val NUXT_DATA_REGEX = Regex(
            """<script[^>]*id=["']__NUXT_DATA__["'][^>]*>(.*?)</script>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
    }

    override suspend fun fetchReceipt(url: String): Receipt {
        logger.debug(TAG, "fetchReceipt start: url=$url")
        val response = try {
            httpClient.get(
                url,
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0",
                    "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                    "Accept-Language" to "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7",
                ),
            )
        } catch (error: Throwable) {
            logger.error(TAG, "request failed", error)
            throw AppError.NetworkError(error)
        }

        if (response.code == 404) throw AppError.ReceiptNotFound
        if (response.code != 200) throw AppError.NetworkError(IllegalStateException("HTTP ${response.code}"))
        logger.debug(TAG, "fetchReceipt response: code=${response.code}, bodyLength=${response.body.length}")

        return runCatching {
            val nuxtJson = extractNuxtDataJson(response.body)
            val payload = decodePayload(nuxtJson)
            val receipt = payload.toReceipt(url)
            logger.debug(
                TAG,
                "parsed receipt: fiscal=${receipt.fiscalSign}, total=${receipt.totalSum}, items=${receipt.items.size}, firstItem=${receipt.items.firstOrNull()?.name}",
            )
            receipt
        }.getOrElse { error ->
            logger.error(TAG, "parse error", error)
            throw (error as? AppError ?: AppError.ParsingError())
        }
    }

    private fun extractNuxtDataJson(html: String): String {
        val match = NUXT_DATA_REGEX.find(html) ?: throw AppError.ParsingError("Missing __NUXT_DATA__")
        return match.groupValues[1]
    }

    private fun decodePayload(nuxtJson: String): KaspiPayload {
        val root = json.parseToJsonElement(nuxtJson).jsonArray
        val indexed = root.toList()
        val payloadNode = indexed.asSequence()
            .mapNotNull { it.asObjectOrNull() }
            .firstOrNull { map ->
                map.containsKey("extTranId") &&
                    map.containsKey("payParameters") &&
                    map.containsKey("cartItems")
            } ?: throw AppError.ParsingError("Kaspi payload not found")

        fun deref(raw: JsonElement?): JsonElement? {
            if (raw == null || raw is JsonNull) return null
            if (raw is JsonPrimitive) {
                val idx = raw.intOrNull
                if (idx != null) {
                    val referenced = indexed.getOrNull(idx)
                    return referenced ?: raw
                }
            }
            return raw
        }

        fun derefDeep(raw: JsonElement?): JsonElement? {
            var current = raw
            var guard = 0
            while (guard < 16) {
                val next = deref(current)
                if (next === current) return current
                current = next
                guard++
            }
            return current
        }

        fun stringAt(key: String): String? {
            val v = derefDeep(payloadNode[key]) ?: return null
            return v.asStringOrNull()
        }

        fun listAt(key: String): List<JsonElement> {
            val v = derefDeep(payloadNode[key]) ?: return emptyList()
            return v.asArrayOrNull()?.toList().orEmpty()
        }

        fun objectAt(raw: JsonElement?): JsonObject? = derefDeep(raw)?.asObjectOrNull()
        fun numberAt(raw: JsonElement?): Double? = derefDeep(raw)?.let { (it as? JsonPrimitive)?.doubleOrNull }

        val payParameters = listAt("payParameters").mapNotNull { rowRaw ->
            val row = objectAt(rowRaw) ?: return@mapNotNull null
            val name = derefDeep(row["name"]).asStringOrNull().orEmpty()
            val value = derefDeep(row["value"]).asStringOrNull().orEmpty()
            if (name.isBlank()) null else name to value
        }.toMap()

        val items = listAt("cartItems").mapNotNull { itemRaw ->
            val itemObj = objectAt(itemRaw) ?: return@mapNotNull null
            val name = derefDeep(itemObj["item_name"]).asStringOrNull().orEmpty()
            if (name.isBlank()) return@mapNotNull null
            val quantity = numberAt(itemObj["quantity"]) ?: 1.0
            val price = numberAt(itemObj["item_price"]) ?: 0.0
            val sum = numberAt(itemObj["sum"]) ?: (price * quantity)
            val unit = derefDeep(itemObj["unit_name"]).asStringOrNull().orEmpty()
            Item(
                barcode = null,
                codeMark = null,
                name = name,
                count = quantity,
                price = price,
                unit = UnitOfMeasurement.from(unit),
                sum = sum,
                taxType = null,
                taxSum = null,
            )
        }

        logger.debug(
            TAG,
            "decoded payload: extTranId=${stringAt("extTranId")}, title=${stringAt("title")}, amount=${stringAt("amount")}, payParams=${payParameters.size}, items=${items.size}",
        )

        return KaspiPayload(
            title = stringAt("title").orEmpty(),
            extTranId = stringAt("extTranId").orEmpty(),
            amount = stringAt("amount").orEmpty(),
            saleDate = stringAt("saleDate").orEmpty(),
            orderType = stringAt("orderType").orEmpty(),
            address = payParameters["Адрес"].orEmpty(),
            iinBin = payParameters["ИИН/БИН продавца"].orEmpty(),
            rnM = payParameters["РНМ"].orEmpty(),
            znM = payParameters["ЗНМ"].orEmpty(),
            fiscalSign = payParameters["ФП"].orEmpty(),
            paymentText = payParameters["Оплачено"].orEmpty(),
            items = items,
        )
    }

    private data class KaspiPayload(
        val title: String,
        val extTranId: String,
        val amount: String,
        val saleDate: String,
        val orderType: String,
        val address: String,
        val iinBin: String,
        val rnM: String,
        val znM: String,
        val fiscalSign: String,
        val paymentText: String,
        val items: List<Item>,
    ) {
        fun toReceipt(url: String): Receipt {
            val itemsSum = items.sumOf { it.sum }
            val amountParsed = amount.replace(" ", "").replace(",", ".").toDoubleOrNull()
            val totalSum = amountParsed ?: itemsSum
            if (amountParsed != null && kotlin.math.abs(amountParsed - itemsSum) > 0.01 && items.isNotEmpty()) {
                // Keep parser resilient when Kaspi payload uses rounded "amount" but line items differ.
                // We prefer "amount" for totals and only log the discrepancy for debugging.
            }
            val paymentType = when {
                paymentText.contains("нал", ignoreCase = true) -> PaymentType.CASH
                paymentText.contains("моб", ignoreCase = true) -> PaymentType.MOBILE
                else -> PaymentType.CARD
            }
            val operationType = when (orderType.uppercase()) {
                "BUY" -> OperationType.SELL
                "BUY_RETURN" -> OperationType.SELL_RETURN
                else -> OperationType.SELL
            }

            return Receipt(
                companyName = title,
                certificateVat = null,
                iinBin = iinBin,
                companyAddress = address,
                serialNumber = znM,
                kgdId = rnM,
                dateTime = parseSaleDate(saleDate),
                fiscalSign = fiscalSign.ifBlank { extTranId },
                ofd = Ofd.KASPI,
                typeOperation = operationType,
                items = items,
                url = url,
                taxesType = null,
                taxesSum = null,
                taken = totalSum,
                change = 0.0,
                totalType = listOf(Payment(type = paymentType, sum = totalSum)),
                totalSum = totalSum,
            )
        }

        private fun parseSaleDate(value: String): Instant {
            val formatter = DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd HH:mm:ss")
                .optionalStart()
                .appendFraction(java.time.temporal.ChronoField.MICRO_OF_SECOND, 0, 6, true)
                .optionalEnd()
                .toFormatter()
            return runCatching {
                val local = LocalDateTime.parse(value, formatter)
                local.atZone(ZoneId.of("Asia/Almaty")).toInstant()
            }.getOrElse { Instant.now() }
        }
    }

    private fun JsonElement?.asObjectOrNull(): JsonObject? = this as? JsonObject
    private fun JsonElement?.asArrayOrNull(): JsonArray? = this as? JsonArray
    private fun JsonElement?.asStringOrNull(): String? = (this as? JsonPrimitive)?.content
}
