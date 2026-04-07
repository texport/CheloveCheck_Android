package com.chelovecheck.presentation.navigation

import android.util.Base64
import kotlin.text.Charsets

object OfdCaptchaNav {
    fun encodeUrlForNav(url: String): String {
        return Base64.encodeToString(
            url.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
    }

    fun decodeUrlFromNav(encoded: String): String {
        return String(
            Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_PADDING),
            Charsets.UTF_8,
        )
    }
}
