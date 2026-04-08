package com.chelovecheck.data.remote.ofd.parse

import com.chelovecheck.shared.datacore.SharedOfdUrlParsing
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal object OfdParsingCommons {
    fun parseQueryParams(url: String): Map<String, String> = SharedOfdUrlParsing.parseQueryParams(url)

    fun convertQrDateToApiDate(dateStr: String): String {
        val inputFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
        val outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return runCatching {
            val date = LocalDateTime.parse(dateStr, inputFormatter)
            date.format(outputFormatter)
        }.getOrElse { dateStr }
    }

    fun parseDateTimeOrNow(rawDate: String, pattern: String, zoneId: String = "Asia/Almaty"): java.time.Instant {
        val formatter = DateTimeFormatter.ofPattern(pattern)
        return runCatching {
            val localDate = LocalDateTime.parse(rawDate, formatter)
            localDate.atZone(ZoneId.of(zoneId)).toInstant()
        }.getOrElse { java.time.Instant.now() }
    }
}
