package com.chelovecheck.data.remote.ofd.debug

import com.chelovecheck.domain.logging.AppLogger

/**
 * Chunked debug logging for large payloads — single policy for handlers that still need full dumps.
 */
internal object OfdDebugLog {
    private const val CHUNK_SIZE = 3000

    fun chunkedDebug(logger: AppLogger, tag: String, prefix: String, value: String) {
        if (value.isBlank()) {
            logger.debug(tag, "$prefix: <empty>")
            return
        }
        logger.debug(tag, "$prefix length=${value.length}")
        var index = 0
        while (index < value.length) {
            val end = (index + CHUNK_SIZE).coerceAtMost(value.length)
            logger.debug(tag, "$prefix[$index..$end]: ${value.substring(index, end)}")
            index = end
        }
    }
}
