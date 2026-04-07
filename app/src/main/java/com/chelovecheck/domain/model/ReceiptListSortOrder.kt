package com.chelovecheck.domain.model

/**
 * Receipt list ordering. Pinned receipts stay at the top for every mode.
 */
enum class ReceiptListSortOrder {
    /** Pinned, then favorite, then date (newest), then fiscal sign. */
    DEFAULT,

    /** Pinned, then date newest first. */
    DATE_NEWEST,

    /** Pinned, then date oldest first. */
    DATE_OLDEST,

    /** Pinned, then total amount high to low. */
    AMOUNT_DESC,

    /** Pinned, then total amount low to high. */
    AMOUNT_ASC,

    /** Pinned, then merchant name A–Z. */
    MERCHANT_AZ,
}
