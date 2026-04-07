package com.chelovecheck.domain.analytics

/**
 * Detects likely **service** lines on receipts (vs goods) from the normalized product title.
 * Used only for analytics bucketing into [RetailDisplayGroupResolver.SERVICES_GROUP_ID].
 */
object ServiceLineHeuristic {
    private val pattern = Regex(
        """(?i)(услуг|аренд|комисс|доставк|мойк|ремонт|абон|подписк|сервис|пошлин|страхов|лиценз|парковк|химчист|стирк|уборк|монтаж|настройк|диагност|консультац|қызмет|жалға|қызмет көрсету|service|rental|commission|delivery|wash|repair|subscription|insurance|parking|cleaning|installation)""",
    )

    fun matchesNormalized(normalized: String): Boolean =
        normalized.isNotBlank() && pattern.containsMatchIn(normalized)
}
