package com.chelovecheck.data.remote.ofd.handlers

import com.chelovecheck.domain.model.AppError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FourEkRedirectResolverTest {
    @Test
    fun resolve_transtelecom_buildsCanonicalHost() {
        val url =
            "https://4ek.kz/?o=transtelecom&i=id&f=fn&t=token&s=1"
        val r = FourEkRedirectResolver.resolve(url)
        assertEquals(Provider.TRANSTELECOM, r.provider)
        assertTrue(r.url.startsWith("https://ofd1.kz/t/?"))
        assertTrue(r.url.contains("t=token"))
    }

    @Test
    fun resolve_throwsWhenIncomplete() {
        val err = runCatching { FourEkRedirectResolver.resolve("https://4ek.kz/?o=transtelecom") }.exceptionOrNull()
        assertTrue(err is AppError.MissingParameters)
    }
}
