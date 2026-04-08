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

class KaspiOFDHandlerTest {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val logger = object : AppLogger {
        override fun debug(tag: String, message: String, throwable: Throwable?) = Unit
        override fun error(tag: String, message: String, throwable: Throwable?) = Unit
    }

    @Test
    fun fetchReceipt_successfullyParsesKaspiPayload() = runBlocking {
        val fixture = readResource("kaspi_receipt_fixture.html")
        val httpClient = HttpClient(
            OkHttpClient.Builder()
                .addInterceptor(
                    staticResponseInterceptor(
                        code = 200,
                        body = fixture,
                        headers = mapOf("Content-Type" to "text/html; charset=utf-8"),
                    )
                )
                .build()
        )
        val handler = KaspiOFDHandler(httpClient, json, logger)

        val receipt = handler.fetchReceipt(
            "https://receipt.kaspi.kz/web?extTranId=KKM38539310&sale_date=2026-04-01%2015%3A29%3A21.226779"
        )

        assertEquals(Ofd.KASPI, receipt.ofd)
        assertEquals("ИП DBY", receipt.companyName)
        assertEquals("870712300992", receipt.iinBin)
        assertEquals("847501129269", receipt.fiscalSign)
        assertEquals(1, receipt.items.size)
        assertEquals(5676.0, receipt.totalSum, 0.001)
    }

    @Test
    fun fetchReceipt_404MapsToReceiptNotFound() = runBlocking {
        val httpClient = HttpClient(
            OkHttpClient.Builder()
                .addInterceptor(staticResponseInterceptor(code = 404, body = "not found"))
                .build()
        )
        val handler = KaspiOFDHandler(httpClient, json, logger)

        val error = runCatching {
            handler.fetchReceipt("https://receipt.kaspi.kz/web?extTranId=1&sale_date=2026-01-01")
        }.exceptionOrNull()

        assertTrue(error is AppError.ReceiptNotFound)
    }

    @Test
    fun fetchReceipt_invalidPayloadMapsToParsingError() = runBlocking {
        val httpClient = HttpClient(
            OkHttpClient.Builder()
                .addInterceptor(
                    staticResponseInterceptor(
                        code = 200,
                        body = "<html><body>no nuxt data</body></html>",
                        headers = mapOf("Content-Type" to "text/html"),
                    )
                )
                .build()
        )
        val handler = KaspiOFDHandler(httpClient, json, logger)

        val error = runCatching {
            handler.fetchReceipt("https://receipt.kaspi.kz/web?extTranId=1&sale_date=2026-01-01")
        }.exceptionOrNull()

        assertTrue(error is AppError.ParsingError)
    }

    @Test
    fun fetchReceipt_resolvesIndexedItemNumbersFromNuxtData() = runBlocking {
        val indexedHtml = """
            <!doctype html><html><body>
            <script type="application/json" id="__NUXT_DATA__">
            [
              {"meta":"ok"},
              {
                "extTranId":"KKM38539310",
                "title":"ИП DBY",
                "amount":"5 676",
                "saleDate":"2026-04-01 15:29:21.226000",
                "orderType":"SELL",
                "payParameters":[
                  {"name":"ИИН/БИН продавца","value":"870712300992"},
                  {"name":"РНМ","value":"010103817780"},
                  {"name":"ЗНМ","value":"KK0053255091"},
                  {"name":"ФП","value":"847501129269"},
                  {"name":"Оплачено","value":"Картой"}
                ],
                "cartItems":[{"item_name":5,"item_price":2,"quantity":3,"unit_name":4,"sum":2}]
              },
              5676,
              1,
              "шт.",
              "Зал"
            ]
            </script>
            </body></html>
        """.trimIndent()

        val httpClient = HttpClient(
            OkHttpClient.Builder()
                .addInterceptor(
                    staticResponseInterceptor(
                        code = 200,
                        body = indexedHtml,
                        headers = mapOf("Content-Type" to "text/html; charset=utf-8"),
                    )
                )
                .build()
        )
        val handler = KaspiOFDHandler(httpClient, json, logger)

        val receipt = handler.fetchReceipt(
            "https://receipt.kaspi.kz/web?extTranId=KKM38539310&sale_date=2026-04-01%2015%3A29%3A21.226779"
        )

        assertEquals(1.0, receipt.items.first().count, 0.001)
        assertEquals(5676.0, receipt.items.first().price, 0.001)
        assertEquals(5676.0, receipt.items.first().sum, 0.001)
    }

    private fun staticResponseInterceptor(
        code: Int,
        body: String,
        headers: Map<String, String> = emptyMap(),
    ): Interceptor = Interceptor { chain ->
        Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("stub")
            .headers(headers.toHeaders())
            .body(body.toResponseBody("text/html".toMediaType()))
            .build()
    }

    private fun readResource(name: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream(name)
            ?: error("Resource not found: $name")
        return stream.use { it.readBytes().toString(StandardCharsets.UTF_8) }
    }
}
