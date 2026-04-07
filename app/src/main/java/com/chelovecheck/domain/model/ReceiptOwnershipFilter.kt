package com.chelovecheck.domain.model

/**
 * Additional list filter for receipt ownership (favorites / pins).
 * Combined with [ReceiptFilter] period via repository query (AND).
 */
enum class ReceiptOwnershipFilter {
    All,
    FavoritesOnly,
    PinnedOnly,
}
