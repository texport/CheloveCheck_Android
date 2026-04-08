package com.chelovecheck.data.repository

import com.chelovecheck.data.remote.ofd.handlers.OFDHandler
import com.chelovecheck.data.remote.ofd.handlers.OFDHandlerManager
import com.chelovecheck.domain.model.AppError
import com.chelovecheck.domain.model.Ofd
import com.chelovecheck.domain.model.OperationType
import com.chelovecheck.domain.model.Receipt
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptFetcherImplTest {
    @Test
    fun fetchReceiptByUrl_wofdHostUsesMappedHandler() = runBlocking {
        val expected = stubReceipt()
        val handler = object : OFDHandler {
            override suspend fun fetchReceipt(url: String): Receipt = expected
        }
        val fetcher = ReceiptFetcherImpl(
            OFDHandlerManager(
                mapOf(
                    "consumer.wofd.kz" to handler,
                    "cabinet.wofd.kz" to handler,
                    "wofd.kz" to handler,
                )
            )
        )

        val receipt = fetcher.fetchReceiptByUrl("https://consumer.wofd.kz?i=1&f=2&s=3&t=20250101T000000")

        assertEquals(expected, receipt)
    }

    @Test
    fun fetchReceiptByUrl_unknownHostThrowsUnsupportedDomain() = runBlocking {
        val fetcher = ReceiptFetcherImpl(OFDHandlerManager(emptyMap()))

        val error = runCatching {
            fetcher.fetchReceiptByUrl("https://consumer.wofd.kz?i=1&f=2&s=3&t=20250101T000000")
        }.exceptionOrNull()

        assertTrue(error is AppError.UnsupportedDomain)
    }

    private fun stubReceipt(): Receipt {
        return Receipt(
            companyName = "WOFD",
            certificateVat = null,
            iinBin = "123",
            companyAddress = "address",
            serialNumber = "sn",
            kgdId = "kgd",
            dateTime = Instant.EPOCH,
            fiscalSign = "fs",
            ofd = Ofd.WOFD,
            typeOperation = OperationType.SELL,
            items = emptyList(),
            url = "https://consumer.wofd.kz",
            taxesType = null,
            taxesSum = null,
            taken = null,
            change = null,
            totalType = emptyList(),
            totalSum = 0.0,
        )
    }
}
