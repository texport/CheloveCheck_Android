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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class JusanOFDHandler @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
    private val logger: AppLogger,
) : OFDHandler {
    private companion object {
        const val USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/97.0.4692.71 Safari/537.36"
        const val TAG = "JusanOFD"
        const val LOG_CHUNK_SIZE = 3000
    }

    override suspend fun fetchReceipt(url: String): Receipt {
        logDebug("fetchReceipt start, initialUrl=$url")
        val uri = URI(url)
        val query = uri.query.orEmpty()
        val params = query.split("&")
            .mapNotNull {
                val parts = it.split("=")
                if (parts.size == 2) parts[0] to parts[1] else null
            }
            .toMap()

        logDebug("extracted params=$params")

        val ticketNumber = params["i"] ?: throw AppError.InvalidQrCode
        val registrationNumber = params["f"] ?: throw AppError.InvalidQrCode
        val rawDate = params["t"] ?: throw AppError.InvalidQrCode

        val ticketDate = convertDateFormat(rawDate)
        val apiUrl = "https://cabinet.kofd.kz/api/tickets?registrationNumber=$registrationNumber&ticketNumber=$ticketNumber&ticketDate=$ticketDate"
        logDebug("apiUrl=$apiUrl")

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
            throw AppError.NetworkError(error)
        }
        logDebug("response code=${response.code}")
        logDebug("response headers=${response.headers}")
        logLong("response body", response.body)
        if (response.code == 404) {
            throw AppError.ReceiptNotFound
        }
        if (response.code != 200) {
            throw AppError.NetworkError(IllegalStateException("HTTP ${response.code}"))
        }

        val jsonObject = json.parseToJsonElement(response.body).jsonObject
        val receipt = runCatching { convertToReceipt(jsonObject, url) }
            .getOrElse { error ->
                logger.error(TAG, "parse error", error)
                throw error
            }
        logDebug("parsed receipt summary: org=${receipt.companyName}, bin=${receipt.iinBin}, items=${receipt.items.size}, payments=${receipt.totalType.size}, total=${receipt.totalSum}")
        logLong("parsed receipt", receipt.toString())
        return receipt
    }

    private fun convertDateFormat(dateStr: String): String {
        val inputFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
        val outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return runCatching {
            val date = LocalDateTime.parse(dateStr, inputFormatter)
            date.format(outputFormatter)
        }.getOrElse { dateStr }
    }

    private fun convertToReceipt(jsonObject: kotlinx.serialization.json.JsonObject, url: String): Receipt {
        val data = jsonObject["data"]?.jsonObject
            ?: throw AppError.ParsingError()
        val ticketArray = data["ticket"]?.jsonArray
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

        val items = extractItems(lines)
        val totals = extractTotals(lines)

        return Receipt(
            companyName = companyName,
            certificateVat = null,
            iinBin = iinBin,
            companyAddress = "",
            serialNumber = serialNumber,
            kgdId = kgdId,
            dateTime = dateTime,
            fiscalSign = fiscalSign,
            ofd = Ofd.KOFD,
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

    private fun extractCompanyName(lines: List<String>): String {
        val companyLines = mutableListOf<String>()
        for (text in lines) {
            if (text.contains("БСН/БИН") || text.contains("ИИН")) break
            companyLines.add(text)
        }
        return companyLines.joinToString(" ").trim()
    }

    private fun extractIinBin(lines: List<String>): String {
        for (text in lines) {
            if (text.contains("БСН/БИН") || text.contains("ИИН")) {
                return text.replace("БСН/БИН", "")
                    .replace("ИИН", "")
                    .trim()
            }
        }
        return "Неизвестно"
    }

    private fun extractTypeOperation(lines: List<String>): OperationType {
        var foundIinBin = false
        for (text in lines) {
            if (foundIinBin) {
                val trimmed = text.trim()
                if (trimmed.isEmpty()) continue
                return when (trimmed) {
                    "Продажа" -> OperationType.SELL
                    "Возврат", "Возврат продажи" -> OperationType.SELL_RETURN
                    "Покупка" -> OperationType.BUY
                    "Возврат покупки" -> OperationType.BUY_RETURN
                    else -> OperationType.SELL
                }
            }
            if (text.contains("БСН/БИН") || text.contains("ИИН")) {
                foundIinBin = true
            }
        }
        return OperationType.SELL
    }

    private fun extractFiscalSign(lines: List<String>): String {
        for (text in lines) {
            if (text.contains("ФИСКАЛЬНЫЙ ПРИЗНАК")) {
                return text.split(":").lastOrNull()?.trim().orEmpty()
            }
        }
        return "Неизвестно"
    }

    private fun extractDateTime(lines: List<String>): Instant {
        val dateKeywords = listOf("Время", "Дата", "ДАТА", "ВРЕМЯ")
        for (text in lines) {
            if (dateKeywords.any { text.contains(it) }) {
                val rawDate = text.split(":").drop(1).joinToString(":").trim()
                return convertDate(rawDate)
            }
        }
        return Instant.now()
    }

    private fun extractSerialNumber(lines: List<String>): String {
        for (text in lines) {
            if (text.contains("КЗН/ЗНМ")) {
                val afterKzn = text.substringAfter("КЗН/ЗНМ")
                return if (afterKzn.contains("КСН/ИНК")) {
                    afterKzn.substringBefore("КСН/ИНК").trim()
                } else {
                    afterKzn.trim()
                }
            }
        }
        return "Неизвестно"
    }

    private fun extractKgdId(lines: List<String>): String {
        for (text in lines) {
            if (text.contains("КТН/РНМ")) {
                return text.substringAfter("КТН/РНМ").trim()
            }
        }
        return "Неизвестно"
    }

    private fun extractItems(lines: List<String>): List<Item> {
        val itemsRawData = extractRawItemsData(lines)
        val structuredItems = extractItemsAndDiscounts(itemsRawData)
        return structuredItems.map { data ->
            val name = data[0]
            val countPriceSumText = data[1]
            val taxText = data[2]
            parseItem(name, countPriceSumText, taxText)
        }
    }

    private fun extractRawItemsData(lines: List<String>): List<String> {
        val itemsRawData = mutableListOf<String>()
        var index = 0
        while (index < lines.size) {
            if (lines[index].contains("***********************************************")) {
                index++
                break
            }
            index++
        }

        while (index < lines.size) {
            val text = lines[index]
            if (text == "------------------------------------------------") {
                index++
                break
            }
            itemsRawData.add(text)
            index++
        }
        return itemsRawData
    }

    private fun extractItemsAndDiscounts(itemsRawData: List<String>): List<List<String>> {
        val structuredItems = mutableListOf<List<String>>()
        var index = 0

        while (index < itemsRawData.size) {
            val firstLine = itemsRawData[index].trim()
            if (firstLine.contains("ЖЕҢІЛДІК/СКИДКА") || firstLine.contains("НАЦЕНКА")) {
                break
            }

            val itemNameLines = mutableListOf<String>()
            itemNameLines.add(itemsRawData[index].trim())
            index++

            while (index < itemsRawData.size) {
                val line = itemsRawData[index].trim()
                if (isQuantityPriceSumLine(line)) break
                itemNameLines.add(line)
                index++
            }

            val cleanedName = cleanItemName(itemNameLines.joinToString(" "))
            val countPriceSumText = if (index < itemsRawData.size) itemsRawData[index] else ""
            index++

            var taxText = ""
            if (index < itemsRawData.size) {
                val taxLine = itemsRawData[index].trim()
                if (taxLine.contains("НДС")) {
                    taxText = taxLine
                    index++
                }
            }

            structuredItems.add(listOf(cleanedName, countPriceSumText, taxText))
        }

        return structuredItems
    }

    private fun isQuantityPriceSumLine(text: String): Boolean {
        val trimmed = text.trim()
        return trimmed.contains("x") && trimmed.contains("₸") && trimmed.firstOrNull()?.isDigit() == true
    }

    private fun cleanItemName(name: String): String {
        return name.replace("\\", "")
            .replace("\"", "")
            .replace("|", "")
            .replace("*", "")
            .replace("_", "")
            .replace("~", "")
            .trim()
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
        val totalsRawData = mutableListOf<String>()
        var index = 0

        while (index < lines.size) {
            if (lines[index] == "------------------------------------------------") {
                index++
                break
            }
            index++
        }

        while (index < lines.size) {
            val text = lines[index]
            if (text == "------------------------------------------------") {
                index++
                break
            }
            totalsRawData.add(text)
            index++
        }

        val taken = totalsRawData.getOrNull(0)?.let { extractTaken(it) }
        val payments = totalsRawData.getOrNull(1)?.let { extractPayments(it) } ?: emptyList()
        val change = totalsRawData.getOrNull(2)?.let { extractChange(it) }
        val taxesType = totalsRawData.getOrNull(5)?.let { "НДС" }
        val taxesSum = totalsRawData.getOrNull(5)?.let { extractTaxesSum(it) }
        val totalSum = totalsRawData.getOrNull(6)?.let { extractTotalSum(it) } ?: 0.0

        return Totals(taken, payments, change, taxesType, taxesSum, totalSum)
    }

    private fun extractPayments(text: String): List<Payment> {
        val parts = text.split(":")
        if (parts.size != 2) return emptyList()

        val typeString = parts[0].trim()
        val sumText = parts[1].trim()
        val sum = extractAmount(sumText)

        val type = when {
            typeString.contains("Банковская карта") -> PaymentType.CARD
            typeString.contains("Наличные") -> PaymentType.CASH
            typeString.contains("Мобильные платежи") -> PaymentType.MOBILE
            else -> PaymentType.CARD
        }

        return listOf(Payment(type = type, sum = sum))
    }

    private fun extractAmount(text: String): Double {
        val cleaned = text.replace("₸", "")
            .replace("\u00A0", "")
            .replace(" ", "")
            .replace(",", ".")
            .trim()
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    private fun extractTaken(text: String): Double {
        return extractSpecificAmount(text, "Төленген сома/Сумма оплаты")
    }

    private fun extractChange(text: String): Double {
        return extractSpecificAmount(text, "Қайтарым сомасы/Сумма сдачи")
    }

    private fun extractTaxesSum(text: String): Double {
        return extractSpecificAmount(text, "ҚҚС сомасы/Сумма НДС")
    }

    private fun extractTotalSum(text: String): Double {
        return extractSpecificAmount(text, ":")
    }

    private fun extractSpecificAmount(text: String, keyword: String): Double {
        val index = text.indexOf(keyword)
        if (index == -1) return 0.0
        val amountText = text.substring(index + keyword.length)
            .replace("₸", "")
            .replace("\u00A0", "")
            .replace(" ", "")
            .replace(",", ".")
            .trim()
        return amountText.toDoubleOrNull() ?: 0.0
    }

    private fun convertDate(dateStr: String): Instant {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
        return runCatching {
            val localDate = LocalDateTime.parse(dateStr, formatter)
            localDate.atZone(ZoneId.of("Asia/Almaty")).toInstant()
        }.getOrElse { Instant.now() }
    }

    private fun parseItem(name: String, countPriceSumText: String, taxText: String): Item {
        val countPattern = Regex("^([\\d.,]+)\\s*\\(")
        val unitPattern = Regex("\\((.*?)\\)")
        val pricePattern = Regex("x\\s*([\\d\\s]+,\\d+)₸")
        val sumPattern = Regex("=\\s*([\\d\\s]+,\\d+)₸")

        val cleanedText = countPriceSumText.replace("\u00A0", " ")

        val countText = countPattern.find(cleanedText)?.groupValues?.get(1)
            ?.replace(",", ".") ?: "0.0"
        val count = countText.toDoubleOrNull() ?: 0.0

        val unitText = unitPattern.find(cleanedText)?.groupValues?.get(1) ?: "шт"
        val priceText = pricePattern.find(cleanedText)?.groupValues?.get(1)
            ?.replace(" ", "")
            ?.replace(",", ".") ?: "0.0"
        val sumText = sumPattern.find(cleanedText)?.groupValues?.get(1)
            ?.replace(" ", "")
            ?.replace(",", ".") ?: "0.0"

        val price = priceText.toDoubleOrNull() ?: 0.0
        val sum = sumText.toDoubleOrNull() ?: 0.0
        val unit = UnitOfMeasurement.from(unitText)
        val taxSum = extractAmount(taxText.replace("НДС", "").trim())

        return Item(
            barcode = null,
            codeMark = null,
            name = name,
            count = count,
            price = price,
            unit = unit,
            sum = sum,
            taxType = if (taxText.isBlank()) null else "НДС",
            taxSum = taxSum,
        )
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
}
