package com.chelovecheck.presentation.utils

import android.util.Log
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
    val tokens = query
        ?.lowercase()
        ?.split(Regex("\\s+"))
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.distinct()
        .orEmpty()
    if (tokens.isEmpty()) return AnnotatedString(text)
    val lower = text.lowercase()
    val ranges = mutableListOf<IntRange>()
    tokens.forEach { token ->
        var start = 0
        while (start < lower.length) {
            val idx = lower.indexOf(token, start)
            if (idx < 0) break
            ranges += idx until (idx + token.length)
            start = idx + token.length
        }
    }
    if (ranges.isEmpty()) {
        if (text.length <= 120) {
            Log.d(
                "ReceiptSearch",
                "highlight no-match: query='${tokens.joinToString(" ").take(80)}' text='${text.take(120)}'",
            )
        }
        return AnnotatedString(text)
    }
    val mergedRanges = ranges
        .sortedBy { it.first }
        .fold(mutableListOf<IntRange>()) { acc, range ->
            val last = acc.lastOrNull()
            if (last == null) {
                acc += range
            } else if (range.first <= last.last + 1) {
                acc[acc.lastIndex] = last.first..maxOf(last.last, range.last)
            } else {
                acc += range
            }
            acc
        }
    if (text.length <= 120) {
        Log.d(
            "ReceiptSearch",
            "highlight evaluate: query='${tokens.joinToString(" ").take(80)}' text='${text.take(120)}' rawMatches=${ranges.size} mergedMatches=${mergedRanges.size}",
        )
    }
    return buildAnnotatedString {
        var current = 0
        mergedRanges.forEach { range ->
            if (current < range.first) {
                append(text.substring(current, range.first))
            }
            pushStyle(
                SpanStyle(
                    background = highlightBackground,
                    color = highlightForeground,
                ),
            )
            append(text.substring(range.first, range.last + 1))
            pop()
            current = range.last + 1
        }
        if (current < text.length) {
            append(text.substring(current))
        }
    }
}
