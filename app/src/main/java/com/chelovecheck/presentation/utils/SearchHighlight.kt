package com.chelovecheck.presentation.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

fun buildSearchHighlightedText(
    text: String,
    query: String?,
    highlightBackground: Color,
    highlightForeground: Color,
): AnnotatedString {
    val q = query?.trim().orEmpty()
    if (q.isEmpty()) return AnnotatedString(text)
    val lower = text.lowercase()
    val ql = q.lowercase()
    return buildAnnotatedString {
        var start = 0
        while (true) {
            val idx = lower.indexOf(ql, start)
            if (idx < 0) {
                append(text.substring(start))
                break
            }
            append(text.substring(start, idx))
            pushStyle(
                SpanStyle(
                    background = highlightBackground,
                    color = highlightForeground,
                ),
            )
            append(text.substring(idx, idx + q.length))
            pop()
            start = idx + q.length
        }
    }
}
