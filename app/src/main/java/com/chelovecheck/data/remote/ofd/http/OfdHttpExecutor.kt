package com.chelovecheck.data.remote.ofd.http

import com.chelovecheck.data.remote.ofd.sanitize.OfdResponseSanitizer
import com.chelovecheck.data.remote.HttpClient
import com.chelovecheck.data.remote.HttpResponse
import com.chelovecheck.domain.logging.AppLogger
import com.chelovecheck.domain.model.AppError

internal class OfdHttpExecutor(
    private val httpClient: HttpClient,
    private val logger: AppLogger,
) {
    /**
     * Successful transport only; status code may be non-200 (caller uses [OfdReceiptFetchHttpPolicy] or custom rules).
     */
    suspend fun getTransportOrThrow(
        tag: String,
        url: String,
        headers: Map<String, String> = emptyMap(),
    ): HttpResponse {
        val safeUrl = OfdResponseSanitizer.sanitizeUrl(url)
        val response = try {
            httpClient.get(url = url, headers = headers)
        } catch (error: Throwable) {
            logger.error(tag, "request failed: url=$safeUrl", error)
            throw AppError.NetworkError(error)
        }
        logger.debug(tag, "response: code=${response.code}, url=$safeUrl, bodyLength=${response.body.length}")
        return response
    }

    suspend fun getOrThrow(
        tag: String,
        url: String,
        headers: Map<String, String> = emptyMap(),
    ): HttpResponse {
        val response = getTransportOrThrow(tag, url, headers)
        if (response.code == 404) throw AppError.ReceiptNotFound
        if (response.code != 200) throw AppError.NetworkError(IllegalStateException("HTTP ${response.code}"))
        logger.debug(tag, "response ok: code=${response.code}")
        return response
    }
}
