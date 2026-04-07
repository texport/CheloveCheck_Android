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
import java.net.URI
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class WofdOFDHandler @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
    private val logger: AppLogger,
) : OFDHandler {
    private companion object {
        const val USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/97.0.4692.71 Safari/537.36"
        const val TAG = "WofdOFD"
        const val API_BASE_URL = "https://cabinet.wofd.kz/api/tickets"
    }

    override suspend fun fetchReceipt(url: String): Receipt {
        val params = parseQueryParams(url)
        val ticketNumber = params["i"] ?: throw AppError.InvalidQrCode
        val registrationNumber = params["f"] ?: throw AppError.InvalidQrCode
        val rawDate = params["t"] ?: throw AppError.InvalidQrCode
        val ticketDate = convertDateFormat(rawDate)
        val apiUrl = "$API_BASE_URL?registrationNumber=$registrationNumber&ticketNumber=$ticketNumber&ticketDate=$ticketDate"

        val response = try {
            httpClient.get(
                apiUrl,
                headers = mapOf(
                    "User-Agent" to USER_AGENT,
                    "Accept" to "application/json",
                    "Referer" to "https://cabinet.wofd.kz/consumer",
                ),
            )
        } catch (error: Throwable) {
            logger.error(TAG, "request failed", error)
            throw AppError.NetworkError(error)
        }

        if (response.code == 404) throw AppError.ReceiptNotFound
        if (response.code != 200) {
            throw AppError.NetworkError(IllegalStateException("HTTP ${response.code}"))
        }

        val receipt = runCatching {
            val jsonObject = json.parseToJsonElement(response.body).jsonObject
            convertToReceipt(jsonObject, url)
        }.getOrElse { error ->
            logger.error(TAG, "parse error", error)
            throw (error as? AppError ?: AppError.Unknown(error))
        }

        return receipt
    }

    private fun convertToReceipt(jsonObject: kotlinx.serialization.json.JsonObject, url: String): Receipt {
        val found = jsonObject["found"]?.jsonPrimitive?.intOrNull
            ?: if (jsonObject["ticket"] == null) throw AppError.ParsingError() else 1
        if (found == 0) throw AppError.ReceiptNotFound
        val ticketArray = jsonObject["ticket"]?.jsonArray
            ?: throw AppError.ParsingError()
        val lines = ticketArray.mapNotNull { entry ->
            entry.jsonObject["text"]?.jsonPrimitive?.content
        }

        val companyName = extractCompanyName(lines)
        val iinBin = extractIinBin(lines)
        val typeOperation = extractTypeOperation(lines)
        val fiscalSign = extractFiscalSign(lines)
        val dateTime = extractDateTime(lines)
        val serialNumber = extractSerialNumber(lines)
        val kgdId = extractKgdId(lines)
        val companyAddress = extractAddress(lines)
        val items = extractItems(lines)
        val totals = extractTotals(lines)

        return Receipt(
            companyName = companyName,
            certificateVat = null,
            iinBin = iinBin,
            companyAddress = companyAddress,
            serialNumber = serialNumber,
            kgdId = kgdId,
            dateTime = dateTime,
            fiscalSign = fiscalSign,
            ofd = Ofd.WOFD,
            typeOperation = typeOperation,
            items = items,
            url = url,
            taxesType = totals.taxesType,
            taxesSum = totals.taxesSum,
            taken = totals.taken,
            change = totals.change,
            totalType = totals.payments,
            totalSum = totals.totalSum,
        )
    }

    private fun parseQueryParams(url: String): Map<String, String> {
        return runCatching {
            val uri = URI(url)
            uri.query.orEmpty()
                .split("&")
                .mapNotNull { part ->
                    val pieces = part.split("=", limit = 2)
                    if (pieces.size == 2) pieces[0] to pieces[1] else null
                }
                .toMap()
        }.getOrElse { emptyMap() }
    }

    private fun convertDateFormat(dateStr: String): String {
        val inputFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
        val outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return runCatching {
            val date = LocalDateTime.parse(dateStr, inputFormatter)
            date.format(outputFormatter)
        }
            .getOrElse {
                val date = LocalDateTime.parse(dateStr, inputFormatter)
                date.format(outputFormatter)
            }
    }

    private fun extractCompanyName(lines: List<String>): String {
        return lines.firstOrNull { it.contains("ТОО") || it.contains("ИП") }
            ?.trim()
            .orEmpty()
    }

    private fun extractIinBin(lines: List<String>): String {
        val raw = lines.firstOrNull { it.contains("БИН") || it.contains("ИИН") }.orEmpty()
        return raw.replace("БСН/БИН", "").replace("ИИН", "").trim()
    }

    private fun extractTypeOperation(lines: List<String>): OperationType {
        val line = lines.firstOrNull { it.contains("Продажа") || it.contains("Сату") || it.contains("Возврат") }.orEmpty()
        val normalized = line.lowercase()
        return when {
            normalized.contains("возврат продажи") || normalized.contains("сатуды қайтару") -> OperationType.SELL_RETURN
            normalized.contains("возврат покупки") || normalized.contains("сатып алуды қайтару") -> OperationType.BUY_RETURN
            normalized.contains("покупка") || normalized.contains("сатып алу") -> OperationType.BUY
            else -> OperationType.SELL
        }
    }

    private fun extractFiscalSign(lines: List<String>): String {
        val fiscalLine = lines.firstOrNull { it.contains("Фискальный признак") }.orEmpty()
        return fiscalLine.substringAfter(":").trim()
    }

    private fun extractDateTime(lines: List<String>): Instant {
        val dateLine = lines.firstOrNull { it.contains("ВРЕМЯ:") || it.contains("УАҚЫТЫ/ВРЕМЯ:") }.orEmpty()
        val rawDate = dateLine.substringAfter(":").trim()
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
        return runCatching {
            val localDate = LocalDateTime.parse(rawDate, formatter)
            localDate.atZone(ZoneId.of("Asia/Almaty")).toInstant()
        }.getOrElse { Instant.now() }
    }

    private fun extractSerialNumber(lines: List<String>): String {
        val line = lines.firstOrNull { it.contains("КЗН/ЗНМ") }.orEmpty()
        val afterKzn = line.substringAfter("КЗН/ЗНМ").trim()
        return afterKzn.substringBefore("КСН/ИНК").trim()
    }

    private fun extractKgdId(lines: List<String>): String {
        val line = lines.firstOrNull { it.contains("КТН/РНМ") }.orEmpty()
        return line.substringAfter("КТН/РНМ").trim()
    }

    private fun extractAddress(lines: List<String>): String {
        val separatorIndexes = lines.withIndex().filter { it.value.trim() == "------------------------------------------------" }.map { it.index }
        if (separatorIndexes.isEmpty()) return ""
        val start = separatorIndexes.last() + 1
        if (start >= lines.size) return ""
        val addressLines = lines.drop(start)
            .takeWhile { line -> !line.contains("ФДО:") && !line.contains("ОФД:") && !line.contains("consumer.wofd.kz") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
        return addressLines.joinToString(" ")
    }

    private fun extractItems(lines: List<String>): List<Item> {
        val start = lines.indexOfFirst { it.contains("***********************************************") }
        val end = lines.indexOfFirst { it.trim() == "------------------------------------------------" }
        if (start == -1 || end == -1 || end <= start) return emptyList()
        val itemsRaw = lines.subList(start + 1, end)
        val items = mutableListOf<Item>()
        var index = 0
        while (index < itemsRaw.size) {
            val nameLine = itemsRaw[index].trim()
            val next = itemsRaw.getOrNull(index + 1)?.trim().orEmpty()
            if (nameLine.isBlank() || !next.contains("x") || !next.contains("=")) {
                index++
                continue
            }
            val item = parseItem(nameLine, next)
            if (item != null) items += item
            index += 2
        }
        return items
    }

    private fun parseItem(name: String, countPriceSumText: String): Item? {
        val countPattern = Regex("^([\\d.,]+)\\s*\\(")
        val unitPattern = Regex("\\((.*?)\\)")
        val pricePattern = Regex("x\\s*([\\d\\s\\u00A0]+,\\d+)₸")
        val sumPattern = Regex("=\\s*([\\d\\s\\u00A0]+,\\d+)₸")
        val count = countPattern.find(countPriceSumText)?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull() ?: return null
        val unitText = unitPattern.find(countPriceSumText)?.groupValues?.get(1).orEmpty()
        val price = pricePattern.find(countPriceSumText)?.groupValues?.get(1)
            ?.replace("\u00A0", "")
            ?.replace(" ", "")
            ?.replace(",", ".")
            ?.toDoubleOrNull() ?: return null
        val sum = sumPattern.find(countPriceSumText)?.groupValues?.get(1)
            ?.replace("\u00A0", "")
            ?.replace(" ", "")
            ?.replace(",", ".")
            ?.toDoubleOrNull() ?: return null
        return Item(
            barcode = null,
            codeMark = null,
            name = name,
            count = count,
            price = price,
            unit = UnitOfMeasurement.from(unitText),
            sum = sum,
            taxType = null,
            taxSum = null,
        )
    }

    private data class Totals(
        val taken: Double?,
        val payments: List<Payment>,
        val change: Double?,
        val taxesType: String?,
        val taxesSum: Double?,
        val totalSum: Double,
    )

    private fun extractTotals(lines: List<String>): Totals {
        val totalIndex = lines.indexOfFirst { it.contains("ИТОГО") || it.contains("БАРЛЫҒЫ") }
        val totalLine = if (totalIndex >= 0) lines[totalIndex] else ""
        val totalSum = extractTrailingAmount(totalLine)
            ?: lines.getOrNull(totalIndex + 1)?.let { extractTrailingAmount(it) }
            ?: 0.0
        val paymentLine = lines.firstOrNull { it.contains("Банковская карта") || it.contains("Наличные") || it.contains("Мобильные") }.orEmpty()
        val paymentAmount = extractTrailingAmount(paymentLine) ?: totalSum
        val paymentType = when {
            paymentLine.contains("Наличные", ignoreCase = true) -> PaymentType.CASH
            paymentLine.contains("Мобиль", ignoreCase = true) -> PaymentType.MOBILE
            else -> PaymentType.CARD
        }
        val changeLine = lines.firstOrNull { it.contains("Сумма сдачи") || it.contains("қайтарым") }.orEmpty()
        val change = extractTrailingAmount(changeLine)
        val taxesLine = lines.firstOrNull { it.contains("НДС") || it.contains("ҚҚС") }.orEmpty()
        val taxesSum = extractTrailingAmount(taxesLine)
        return Totals(
            taken = null,
            payments = listOf(Payment(type = paymentType, sum = paymentAmount)),
            change = change,
            taxesType = if (taxesLine.isBlank()) null else "НДС",
            taxesSum = taxesSum,
            totalSum = totalSum,
        )
    }

    private fun extractTrailingAmount(text: String): Double? {
        val match = Regex("([\\d\\s\\u00A0]+,\\d+)₸").findAll(text).lastOrNull()?.groupValues?.get(1) ?: return null
        return match.replace("\u00A0", "").replace(" ", "").replace(",", ".").toDoubleOrNull()
    }
}
