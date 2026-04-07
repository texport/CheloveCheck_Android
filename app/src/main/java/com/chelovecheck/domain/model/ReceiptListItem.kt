package com.chelovecheck.domain.model

/**
 * Row shown on the checks list (summary + resolved store display name).
 */
data class ReceiptListItem(
    val summary: ReceiptListSummary,
    val displayName: String,
)
