package com.chelovecheck.domain.analytics

/**
 * Heuristics for noisy receipt lines (deposits, vouchers, weight markers) used only in analytics bucketing.
 * Normalized name: [com.chelovecheck.domain.utils.ItemNameNormalizer.normalizeForMatch].
 */
object ReceiptLineEdgeCases {

    private val giftOrVoucher = Regex(
        """(?i)(подарочн\S*\s+карт|gift\s*card|voucher|сертификат|ваучер|coupon|купон)""",
    )
    private val depositOrBail = Regex(
        """(?i)(депозит|залог\s+тары|залог|tare\s+deposit|container\s+deposit)""",
    )
    private val returnInName = Regex(
        """(?i)(^|\s)(возврат|return\s+of|storno|сторно)(\s|$)""",
    )

    fun looksLikeGiftCardOrVoucher(normalized: String): Boolean =
        normalized.isNotBlank() && giftOrVoucher.containsMatchIn(normalized)

    fun looksLikeDepositOrContainerFee(normalized: String): Boolean =
        normalized.isNotBlank() && depositOrBail.containsMatchIn(normalized)

    fun looksLikeExplicitReturnLine(normalized: String): Boolean =
        normalized.isNotBlank() && returnInName.containsMatchIn(normalized)
}
