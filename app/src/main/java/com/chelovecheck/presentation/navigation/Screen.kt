package com.chelovecheck.presentation.navigation

import android.util.Base64
import android.util.Log
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    data object Checks : Screen("checks")
    data object Analytics : Screen("analytics")
    data object Scan : Screen("scan")
    data object Receipt : Screen("receipt/{fiscalSign}/{highlightKey}") {
        fun create(fiscalSign: String, searchHighlight: String = ""): String {
            val key = if (searchHighlight.isBlank()) {
                "-"
            } else {
                Base64.encodeToString(
                    searchHighlight.toByteArray(StandardCharsets.UTF_8),
                    Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
                )
            }
            Log.d(
                "ReceiptNav",
                "create route: fiscalSign=$fiscalSign query='${searchHighlight.take(80)}' encodedKey='$key'",
            )
            return "receipt/$fiscalSign/$key"
        }
    }

    /** Route arg: Base64URL-no-padding UTF-8 [com.chelovecheck.domain.utils.ItemNameNormalizer.normalizeForMatch] key. */
    data object Product : Screen("product/{encodedKey}") {
        fun create(normalizedItemKey: String): String {
            val enc = Base64.encodeToString(
                normalizedItemKey.toByteArray(StandardCharsets.UTF_8),
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
            )
            return "product/$enc"
        }
    }
    data object Settings : Screen("settings")

    data object ExchangeRates : Screen("exchange_rates")

    data object DataBackup : Screen("data_backup")

    /** Route argument `encodedUrl`: Base64 URL-safe encoded receipt URL. */
    data object OfdCaptchaVerification : Screen("ofd_captcha/{encodedUrl}") {
        fun create(encodedUrl: String) = "ofd_captcha/$encodedUrl"
    }
}
