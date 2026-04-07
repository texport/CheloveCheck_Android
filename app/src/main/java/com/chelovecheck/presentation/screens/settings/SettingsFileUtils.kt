package com.chelovecheck.presentation.screens.settings

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset

internal fun readTextFromUri(
    context: Context,
    uri: Uri,
    charset: Charset = Charsets.UTF_8,
): String {
    context.contentResolver.openInputStream(uri).use { stream ->
        if (stream != null) {
            return BufferedReader(InputStreamReader(stream, charset)).readText()
        }
    }
    return ""
}

internal fun writeTextToUri(
    context: Context,
    uri: Uri,
    content: String,
    charset: Charset = Charsets.UTF_8,
) {
    context.contentResolver.openOutputStream(uri).use { stream ->
        if (stream != null) {
            stream.write(content.toByteArray(charset))
        }
    }
}
