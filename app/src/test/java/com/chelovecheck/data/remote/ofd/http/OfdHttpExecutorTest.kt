package com.chelovecheck.data.remote.ofd.http

import com.chelovecheck.data.remote.HttpClient
import com.chelovecheck.domain.logging.AppLogger
import com.chelovecheck.domain.model.AppError
import kotlinx.coroutines.runBlocking
import okhttp3.Headers.Companion.toHeaders
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfdHttpExecutorTest {
    private val logger = object : AppLogger {
        override fun debug(tag: String, message: String, throwable: Throwable?) = Unit
        override fun error(tag: String, message: String, throwable: Throwable?) = Unit
    }

    @Test
    fun getOrThrow_maps404ToReceiptNotFound() = runBlocking {
        val client = HttpClient(OkHttpClient.Builder().addInterceptor(staticResponse(404, "nf")).build())
        val executor = OfdHttpExecutor(client, logger)
        val error = runCatching { executor.getOrThrow("t", "https://x") }.exceptionOrNull()
        assertTrue(error is AppError.ReceiptNotFound)
    }

    @Test
    fun getOrThrow_returnsResponseFor200() = runBlocking {
        val client = HttpClient(OkHttpClient.Builder().addInterceptor(staticResponse(200, "{\"ok\":1}")).build())
        val executor = OfdHttpExecutor(client, logger)
        val response = executor.getOrThrow("t", "https://x")
        assertEquals(200, response.code)
    }

    @Test
    fun getTransportOrThrow_returnsNon200WithoutThrowing() = runBlocking {
        val client = HttpClient(OkHttpClient.Builder().addInterceptor(staticResponse(503, "busy")).build())
        val executor = OfdHttpExecutor(client, logger)
        val response = executor.getTransportOrThrow("t", "https://x")
        assertEquals(503, response.code)
    }

    private fun staticResponse(code: Int, body: String): Interceptor = Interceptor { chain ->
        Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("stub")
            .headers(emptyMap<String, String>().toHeaders())
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
    }
}
