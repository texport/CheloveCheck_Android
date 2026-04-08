package com.chelovecheck.shared.datacore

object SharedOfdSanitizer {
    private const val MAX_LOG_BODY = 512

    fun sanitizeUrl(url: String): String = url.substringBefore("?")

    fun sanitizeBodyPreview(body: String): String {
        val compact = body.replace(Regex("\\s+"), " ").trim()
        return if (compact.length <= MAX_LOG_BODY) compact else compact.take(MAX_LOG_BODY) + "..."
    }
}
