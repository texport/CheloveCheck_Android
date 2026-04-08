package com.chelovecheck.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

data class HttpResponse(
    val code: Int,
    val body: String,
    val headers: Headers,
)

class HttpClient @Inject constructor(
    private val client: OkHttpClient,
) {
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): HttpResponse {
        return withContext(Dispatchers.IO) {
            val requestBuilder = Request.Builder().url(url)
            headers.forEach { (key, value) -> requestBuilder.addHeader(key, value) }

            client.newCall(requestBuilder.build()).execute().use { response ->
                val body = response.body.string()
                HttpResponse(
                    code = response.code,
                    body = body,
                    headers = response.headers,
                )
            }
        }
    }

    suspend fun postJson(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
    ): HttpResponse {
        return withContext(Dispatchers.IO) {
            val requestBuilder = Request.Builder()
                .url(url)
                .post(body.toRequestBody("application/json".toMediaType()))
            headers.forEach { (key, value) -> requestBuilder.addHeader(key, value) }
            client.newCall(requestBuilder.build()).execute().use { response ->
                val responseBody = response.body.string()
                HttpResponse(
                    code = response.code,
                    body = responseBody,
                    headers = response.headers,
                )
            }
        }
    }

    suspend fun postFormUrlEncoded(
        url: String,
        fields: Map<String, String>,
        headers: Map<String, String> = emptyMap(),
    ): HttpResponse {
        return withContext(Dispatchers.IO) {
            val formBody = FormBody.Builder().apply {
                fields.forEach { (key, value) -> add(key, value) }
            }.build()
            val requestBuilder = Request.Builder()
                .url(url)
                .post(formBody)
            headers.forEach { (key, value) -> requestBuilder.addHeader(key, value) }
            client.newCall(requestBuilder.build()).execute().use { response ->
                val responseBody = response.body.string()
                HttpResponse(
                    code = response.code,
                    body = responseBody,
                    headers = response.headers,
                )
            }
        }
    }
}
