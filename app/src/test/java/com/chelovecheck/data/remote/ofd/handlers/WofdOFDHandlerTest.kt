package com.chelovecheck.data.remote.ofd.handlers

import com.chelovecheck.data.remote.HttpClient
import com.chelovecheck.domain.logging.AppLogger
import com.chelovecheck.domain.model.AppError
import com.chelovecheck.domain.model.Ofd
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
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

class WofdOFDHandlerTest {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val logger = object : AppLogger {
        override fun debug(tag: String, message: String, throwable: Throwable?) = Unit
        override fun error(tag: String, message: String, throwable: Throwable?) = Unit
    }

    @Test
    fun fetchReceipt_successfullyParsesWofdPayload() = runBlocking {
        val fixture = readResource("wofd_receipt_fixture.json")
        val httpClient = HttpClient(
            OkHttpClient.Builder()
                .addInterceptor(
                    staticResponseInterceptor(
                        code = 200,
                        body = fixture,
                        headers = mapOf("Content-Type" to "application/json; charset=utf-8"),
                    )
                )
                .build()
        )
        val handler = WofdOFDHandler(httpClient, json, logger)

        val receipt = handler.fetchReceipt("https://consumer.wofd.kz?i=1434240585587&f=620304364326&s=13320.00&t=20251016T182415")

        assertEquals(Ofd.WOFD, receipt.ofd)
        assertEquals("ТОО \"IVI EXPRESS\"", receipt.companyName)
        assertEquals("250340011472", receipt.iinBin)
        assertEquals("1434240585587", receipt.fiscalSign)
        assertEquals(2, receipt.items.size)
        assertEquals(13320.0, receipt.totalSum, 0.001)
    }

    @Test
    fun fetchReceipt_404MapsToReceiptNotFound() = runBlocking {
        val httpClient = HttpClient(
            OkHttpClient.Builder()
                .addInterceptor(staticResponseInterceptor(code = 404, body = """{"error":"not found"}"""))
                .build()
        )
        val handler = WofdOFDHandler(httpClient, json, logger)

        val error = runCatching {
            handler.fetchReceipt("https://consumer.wofd.kz?i=1&f=2&t=20250101T000000")
        }.exceptionOrNull()

        assertTrue(error is AppError.ReceiptNotFound)
    }

    @Test
    fun fetchReceipt_invalidJsonMapsToParsingError() = runBlocking {
        val httpClient = HttpClient(
            OkHttpClient.Builder()
                .addInterceptor(
                    staticResponseInterceptor(
                        code = 200,
                        body = """{"unexpected":"shape"}""",
                        headers = mapOf("Content-Type" to "application/json"),
                    )
                )
                .build()
        )
        val handler = WofdOFDHandler(httpClient, json, logger)

        val error = runCatching {
            handler.fetchReceipt("https://cabinet.wofd.kz/consumer/?i=1&f=2&t=20250101T000000")
        }.exceptionOrNull()

        assertTrue(error is AppError.ParsingError)
    }

    private fun staticResponseInterceptor(
        code: Int,
        body: String,
        headers: Map<String, String> = emptyMap(),
    ): Interceptor = Interceptor { chain ->
        val request = chain.request()
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("stub")
            .headers(headers.toHeaders())
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
    }

    private fun readResource(name: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream(name)
            ?: error("Resource not found: $name")
        return stream.use { it.readBytes().toString(StandardCharsets.UTF_8) }
    }
}
