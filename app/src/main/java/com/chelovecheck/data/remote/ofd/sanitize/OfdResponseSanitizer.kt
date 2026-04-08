package com.chelovecheck.data.remote.ofd.sanitize

import com.chelovecheck.shared.datacore.SharedOfdSanitizer

internal object OfdResponseSanitizer {
    fun sanitizeUrl(url: String): String = SharedOfdSanitizer.sanitizeUrl(url)

    fun sanitizeBodyPreview(body: String): String = SharedOfdSanitizer.sanitizeBodyPreview(body)
}
