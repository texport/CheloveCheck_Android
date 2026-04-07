package com.chelovecheck.domain.export

/**
 * Export / backup formats. JSON and CSV are implemented as use cases; batch PDF export is planned
 * (single-receipt share PDF exists on the receipt screen).
 */
enum class ReceiptExportFormat {
    JSON,
    CSV,
    PDF,
}
