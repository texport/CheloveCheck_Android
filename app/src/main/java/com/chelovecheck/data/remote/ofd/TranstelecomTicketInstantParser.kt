package com.chelovecheck.data.remote.ofd

import com.chelovecheck.domain.model.AppError
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Parses receipt date/time from Transtelecom OFD ticket HTML text and/or the compact `t` query param
 * (e.g. `20250303T114419`).
 */
internal object TranstelecomTicketInstantParser {
    private val zone: ZoneId = ZoneId.of("Asia/Almaty")

    private val ruLocale: Locale = Locale.forLanguageTag("ru-RU")

    private val formatters: List<Pair<String, DateTimeFormatter>> = listOf(
        "dd MM yyyy, HH:mm" to DateTimeFormatter.ofPattern("dd MM yyyy, HH:mm", ruLocale),
        "dd.MM.yyyy HH:mm" to DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", ruLocale),
        "dd.MM.yyyy, HH:mm" to DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm", ruLocale),
        "dd.MM.yyyy HH:mm:ss" to DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss", ruLocale),
    )

    fun parse(dateTimeText: String, url: String): Instant {
        val trimmed = dateTimeText.trim()
        val normalized = convertMonthNamesToNumbers(trimmed)
        val parseErrors = mutableListOf<String>()

        if (normalized.isNotBlank()) {
            for ((pattern, formatter) in formatters) {
                runCatching {
                    val local = LocalDateTime.parse(normalized, formatter)
                    return local.atZone(zone).toInstant()
                }.onFailure { e ->
                    val msg = when (e) {
                        is DateTimeParseException -> e.message ?: e::class.simpleName.orEmpty()
                        else -> e.message ?: e::class.simpleName.orEmpty()
                    }
                    parseErrors.add("$pattern: $msg")
                }
            }
        }

        parseFromUrlParameterT(url)?.let { return it }

        val tParam = queryParameter(url, "t").orEmpty().take(32)
        throw AppError.ParsingError(
            details = buildString {
                append("Transtelecom date: ")
                append("raw='${trimmed.take(120)}' ")
                append("normalized='${normalized.take(120)}' ")
                append("t=$tParam ")
                if (parseErrors.isNotEmpty()) {
                    append("attempts=[${parseErrors.take(3).joinToString("; ")}]")
                }
            },
        )
    }

    /**
     * `t=yyyyMMdd'T'HHmmss` (e.g. 20250303T114419) or `t=yyyyMMddHHmmss` (14 digits).
     */
    internal fun parseFromUrlParameterT(url: String): Instant? {
        val raw = queryParameter(url, "t")?.trim().orEmpty()
        if (raw.isEmpty()) return null
        val withT = Regex("^(\\d{8})T(\\d{6})$").find(raw)
        val d: String
        val time: String
        if (withT != null) {
            d = withT.groupValues[1]
            time = withT.groupValues[2]
        } else {
            val compact = Regex("^(\\d{14})$").find(raw) ?: return null
            val v = compact.value
            d = v.substring(0, 8)
            time = v.substring(8, 14)
        }
        return runCatching {
            val year = d.substring(0, 4).toInt()
            val month = d.substring(4, 6).toInt()
            val day = d.substring(6, 8).toInt()
            val hour = time.substring(0, 2).toInt()
            val minute = time.substring(2, 4).toInt()
            val second = time.substring(4, 6).toInt()
            val ld = LocalDate.of(year, month, day)
            val lt = LocalTime.of(hour, minute, second)
            ld.atTime(lt).atZone(zone).toInstant()
        }.getOrNull()
    }

    private fun queryParameter(url: String, name: String): String? {
        val rawQuery = runCatching { URI.create(url).rawQuery }.getOrNull() ?: return null
        for (part in rawQuery.split('&')) {
            val eq = part.indexOf('=')
            if (eq <= 0) continue
            val key = URLDecoder.decode(part.substring(0, eq), StandardCharsets.UTF_8)
            if (key != name) continue
            val value = part.substring(eq + 1)
            return URLDecoder.decode(value, StandardCharsets.UTF_8)
        }
        return null
    }

    internal fun convertMonthNamesToNumbers(dateText: String): String {
        val monthMapping = buildMonthMappingSortedLongestFirst()
        var converted = dateText
        for ((variant, number) in monthMapping) {
            converted = converted.replace(Regex(Regex.escape(variant), RegexOption.IGNORE_CASE), number)
        }
        return converted
    }

    private fun buildMonthMappingSortedLongestFirst(): List<Pair<String, String>> {
        val map = mutableMapOf<String, String>()
        fun putAll(pairs: List<Pair<String, String>>) {
            pairs.forEach { (k, v) -> map[k] = v }
        }
        // Russian
        putAll(
            listOf(
                "январь" to "01", "января" to "01", "янв" to "01", "янв." to "01",
                "февраль" to "02", "февраля" to "02", "фев" to "02", "фев." to "02",
                "март" to "03", "марта" to "03", "мар" to "03", "мар." to "03",
                "апрель" to "04", "апреля" to "04", "апр" to "04", "апр." to "04",
                "май" to "05", "мая" to "05",
                "июнь" to "06", "июня" to "06", "июн" to "06", "июн." to "06",
                "июль" to "07", "июля" to "07", "июл" to "07", "июл." to "07",
                "август" to "08", "августа" to "08", "авг" to "08", "авг." to "08",
                "сентябрь" to "09", "сентября" to "09", "сент" to "09", "сент." to "09",
                "октябрь" to "10", "октября" to "10", "окт" to "10", "окт." to "10",
                "ноябрь" to "11", "ноября" to "11", "нояб" to "11", "нояб." to "11",
                "декабрь" to "12", "декабря" to "12", "дек" to "12", "дек." to "12",
            ),
        )
        // Kazakh (full month names; longer keys must win over short prefixes)
        putAll(
            listOf(
                "қаңтар" to "01", "ақпан" to "02", "наурыз" to "03",
                "сәуір" to "04", "сеуір" to "04",
                "мамыр" to "05", "маусым" to "06", "шілде" to "07", "тамыз" to "08",
                "қыркүйек" to "09", "қазан" to "10", "қараша" to "11", "желтоқсан" to "12",
            ),
        )
        return map.entries
            .map { it.key to it.value }
            .sortedByDescending { it.first.length }
    }
}
