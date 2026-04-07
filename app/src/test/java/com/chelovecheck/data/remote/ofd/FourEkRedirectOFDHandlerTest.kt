package com.chelovecheck.data.remote.ofd

import com.chelovecheck.domain.model.AppError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FourEkRedirectOFDHandlerTest {
    @Test
    fun transtelecomLink_isResolvedToCanonicalTranstelecomUrl() {
        val input = "https://4ek.kz/?o=transtelecom&i=339472456003&f=620504311750&s=15954.0&t=20250808T144938"
        val resolved = FourEkRedirectResolver.resolve(input)
        assertEquals(Provider.TRANSTELECOM, resolved.provider)
        assertEquals(
            "https://ofd1.kz/t/?t=20250808T144938&i=339472456003&f=620504311750&s=15954.0",
            resolved.url,
        )
    }

    @Test
    fun oofdLink_isResolvedToConsumerOofd() {
        val input = "https://4ek.kz/?o=oofd&i=100&f=200&t=20250101T000001"
        val resolved = FourEkRedirectResolver.resolve(input)
        assertEquals(Provider.KAZAKHTELECOM, resolved.provider)
        assertEquals("https://consumer.oofd.kz?t=20250101T000001&i=100&f=200", resolved.url)
    }

    @Test
    fun missingRequiredParams_throwsMissingParameters() {
        val thrown = runCatching {
            FourEkRedirectResolver.resolve("https://4ek.kz/?o=transtelecom&i=1")
        }.exceptionOrNull()
        assertTrue(thrown is AppError.MissingParameters)
    }
}
