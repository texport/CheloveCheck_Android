package com.chelovecheck.data.remote.ofd.http

/**
 * Shared OFD HTTP headers to avoid copy-paste across provider handlers (DRY).
 */
internal object OfdHttpHeaders {
    /** Same UA string previously duplicated in Jusan / Wofd / Kazakhtelecom handlers. */
    const val BROWSER_CHROME_MAC =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/97.0.4692.71 Safari/537.36"

    fun jsonGetBrowser(extra: Map<String, String> = emptyMap()): Map<String, String> = buildMap {
        put("User-Agent", BROWSER_CHROME_MAC)
        put("Accept", "application/json")
        putAll(extra)
    }
}
