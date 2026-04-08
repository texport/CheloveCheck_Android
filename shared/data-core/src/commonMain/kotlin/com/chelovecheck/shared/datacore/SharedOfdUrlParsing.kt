package com.chelovecheck.shared.datacore

/**
 * KMP-safe query parsing for OFD URLs (no `java.net.URI`).
 */
object SharedOfdUrlParsing {
    fun parseQueryParams(url: String): Map<String, String> {
        val qStart = url.indexOf('?')
        if (qStart < 0 || qStart >= url.length - 1) return emptyMap()
        val q = url.substring(qStart + 1)
        if (q.isEmpty()) return emptyMap()
        val pairs = q.split("&").mapNotNull { part ->
            val idx = part.indexOf('=')
            if (idx <= 0) null else {
                decodeQueryComponent(part.substring(0, idx)) to decodeQueryComponent(part.substring(idx + 1))
            }
        }
        return LinkedHashMap<String, String>().apply { pairs.forEach { (k, v) -> put(k, v) } }
    }

    private fun decodeQueryComponent(s: String): String {
        if (!s.contains('%')) return s.replace('+', ' ')
        val out = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            when {
                s[i] == '+' -> {
                    out.append(' ')
                    i++
                }
                s[i] == '%' && i + 2 < s.length -> {
                    val hex = s.substring(i + 1, i + 3)
                    val code = hex.toIntOrNull(16)
                    if (code != null) {
                        out.append(code.toChar())
                        i += 3
                    } else {
                        out.append(s[i])
                        i++
                    }
                }
                else -> {
                    out.append(s[i])
                    i++
                }
            }
        }
        return out.toString()
    }
}
