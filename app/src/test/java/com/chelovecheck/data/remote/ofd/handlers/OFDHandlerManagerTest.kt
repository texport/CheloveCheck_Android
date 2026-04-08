package com.chelovecheck.data.remote.ofd.handlers

import com.chelovecheck.domain.model.Receipt
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class OFDHandlerManagerTest {
    private val wofdHandler = NoopHandler()
    private val kaspiHandler = NoopHandler()
    private val manager = OFDHandlerManager(
        mapOf(
            "consumer.wofd.kz" to wofdHandler,
            "cabinet.wofd.kz" to wofdHandler,
            "wofd.kz" to wofdHandler,
            "receipt.kaspi.kz" to kaspiHandler,
        )
    )

    @Test
    fun consumerHost_resolvesToWofdHandler() {
        val resolved = manager.handlerForHost("consumer.wofd.kz")
        assertNotNull(resolved)
        assertSame(wofdHandler, resolved)
    }

    @Test
    fun subdomainHost_resolvesToWofdHandlerBySuffix() {
        val resolved = manager.handlerForHost("api.consumer.wofd.kz")
        assertNotNull(resolved)
        assertSame(wofdHandler, resolved)
    }

    @Test
    fun unsupportedHost_returnsNull() {
        val resolved = manager.handlerForHost("example.com")
        assertNull(resolved)
    }

    @Test
    fun kaspiHost_resolvesToKaspiHandler() {
        val resolved = manager.handlerForHost("receipt.kaspi.kz")
        assertNotNull(resolved)
        assertSame(kaspiHandler, resolved)
    }

    private class NoopHandler : OFDHandler {
        override suspend fun fetchReceipt(url: String): Receipt {
            error("Should not be called in routing tests")
        }
    }
}
