package com.chelovecheck.data.vision

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.chelovecheck.domain.model.ReceiptScanResult
import com.chelovecheck.domain.repository.ReceiptImageScanner
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.ByteArrayInputStream
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class ReceiptImageScannerImpl @Inject constructor() : ReceiptImageScanner {
    override suspend fun scan(bytes: ByteArray): ReceiptScanResult? {
        val bitmap = decodeBitmap(bytes) ?: return null
        val image = InputImage.fromBitmap(bitmap, 0)

        val barcodeUrl = scanBarcode(image)
        if (!barcodeUrl.isNullOrBlank()) {
            return ReceiptScanResult.Url(barcodeUrl)
        }

        val text = recognizeText(image)
        val url = extractUrlFromText(text)
        if (!url.isNullOrBlank()) {
            return ReceiptScanResult.Url(url)
        }

        val manual = extractManualData(text)
        return manual
    }

    private suspend fun scanBarcode(image: InputImage): String? {
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
        val result = scanner.process(image).await()
        return result.firstOrNull()?.rawValue
    }

    private suspend fun recognizeText(image: InputImage): String {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val result = recognizer.process(image).await()
        return result.text.orEmpty()
    }

    private fun extractUrlFromText(text: String): String? {
        val normalized = normalizeText(text)
        val urlMatch = Regex("(https?://[^\\s]+)").find(normalized)?.value
        if (!urlMatch.isNullOrBlank()) {
            return urlMatch.trim().trimEnd(',', '.', ';', ')')
        }

        val hostMatch = Regex(
            "(consumer\\.(oofd|kofd|wofd)\\.kz[^\\s]*|cabinet\\.wofd\\.kz[^\\s]*|receipt\\.kaspi\\.kz[^\\s]*|ofd1\\.kz[^\\s]*)",
            RegexOption.IGNORE_CASE
        )
            .find(normalized)
            ?.value
        if (!hostMatch.isNullOrBlank()) {
            val trimmed = hostMatch.trim().trimEnd(',', '.', ';', ')')
            return if (trimmed.startsWith("http")) trimmed else "https://$trimmed"
        }

        val timestamp = matchParam(normalized, "t")
        val ticket = matchParam(normalized, "i")
        val fiscal = matchParam(normalized, "f")
        val sum = matchParam(normalized, "s")
        if (timestamp != null && ticket != null && fiscal != null) {
            val host = if (normalized.contains("kofd", ignoreCase = true)) {
                "https://consumer.kofd.kz"
            } else {
                "https://consumer.oofd.kz"
            }
            val params = buildList {
                add("t=$timestamp")
                add("i=$ticket")
                add("f=$fiscal")
                if (!sum.isNullOrBlank()) add("s=$sum")
            }
            return "$host?${params.joinToString("&")}"
        }

        return null
    }

    private fun extractManualData(text: String): ReceiptScanResult.Manual? {
        val normalized = normalizeText(text)
        val t = matchParam(normalized, "t") ?: extractDateTime(normalized)
        val i = matchParam(normalized, "i") ?: extractFiscalSign(normalized) ?: extractDocumentNumber(normalized)
        val f = matchParam(normalized, "f") ?: extractRegistrationNumber(normalized)
        val s = matchParam(normalized, "s") ?: extractTotal(normalized)

        if (t.isNullOrBlank() || i.isNullOrBlank() || f.isNullOrBlank()) return null
        return ReceiptScanResult.Manual(t = t, i = i, f = f, s = s)
    }

    private fun matchParam(text: String, name: String): String? {
        val regex = Regex("\\b$name\\s*=\\s*([0-9A-Za-z.,:-]+)")
        return regex.find(text)?.groupValues?.getOrNull(1)
    }

    private fun extractDateTime(text: String): String? {
        val isoPattern = Regex("\\b(20\\d{2})[-./]?(\\d{2})[-./]?(\\d{2})[ T]?(\\d{2})[:.]?(\\d{2})(?:[:.]?(\\d{2}))?\\b")
        val isoMatch = isoPattern.find(text)
        if (isoMatch != null) {
            val year = isoMatch.groupValues[1]
            val month = isoMatch.groupValues[2]
            val day = isoMatch.groupValues[3]
            val hour = isoMatch.groupValues[4]
            val minute = isoMatch.groupValues[5]
            val second = isoMatch.groupValues.getOrNull(6)?.ifBlank { "00" } ?: "00"
            return "${year}${month}${day}T${hour}${minute}${second}"
        }

        val ruPattern = Regex("\\b(\\d{2})[.\\-/](\\d{2})[.\\-/](20\\d{2})\\s*(\\d{2})[:.]?(\\d{2})(?:[:.]?(\\d{2}))?\\b")
        val ruMatch = ruPattern.find(text)
        if (ruMatch != null) {
            val day = ruMatch.groupValues[1]
            val month = ruMatch.groupValues[2]
            val year = ruMatch.groupValues[3]
            val hour = ruMatch.groupValues[4]
            val minute = ruMatch.groupValues[5]
            val second = ruMatch.groupValues.getOrNull(6)?.ifBlank { "00" } ?: "00"
            return "${year}${month}${day}T${hour}${minute}${second}"
        }

        return null
    }

    private fun extractDocumentNumber(text: String): String? {
        val regex = Regex("(?i)(фд|фискальн(?:ый|ого)?\\s*документ|документ\\s*№|чек\\s*№|№\\s*чека)\\s*[:№#-]?\\s*(\\d{4,})")
        return regex.find(text)?.groupValues?.getOrNull(2)
    }

    private fun extractRegistrationNumber(text: String): String? {
        val regex = Regex("(?i)(рнм|рнк|ккт|ккм|рег(?:истрационн(?:ый|ого))?\\s*номер|код\\s*ккм)\\s*[:№#-]?\\s*(\\d{6,})")
        return regex.find(text)?.groupValues?.getOrNull(2)
    }

    private fun extractFiscalSign(text: String): String? {
        val regex = Regex("(?i)(фп|фискальн(?:ый|ого)?\\s*признак|фиск\\.?\\s*признак|fiscal\\s*sign)\\s*[:№#-]?\\s*(\\d{6,})")
        return regex.find(text)?.groupValues?.getOrNull(2)
    }

    private fun extractTotal(text: String): String? {
        val regex = Regex("(?i)(итог|итого|total|к\\s*оплате)\\s*[:№#-]?\\s*([0-9\\s]+[.,][0-9]{2})")
        val match = regex.find(text)?.groupValues?.getOrNull(2)?.replace(" ", "")
        if (!match.isNullOrBlank()) {
            return match.replace(',', '.')
        }

        val alt = Regex("\\b([0-9\\s]+[.,][0-9]{2})\\s*(тг|тенге|kzt)\\b", RegexOption.IGNORE_CASE)
        return alt.find(text)?.groupValues?.getOrNull(1)?.replace(" ", "")?.replace(',', '.')
    }

    private fun normalizeText(text: String): String {
        return text.replace("\n", " ").replace("\r", " ")
    }

    private fun decodeBitmap(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
        val sampleSize = calculateInSampleSize(maxSide, MAX_BITMAP_SIDE)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        val exif = runCatching { ExifInterface(ByteArrayInputStream(bytes)) }.getOrNull()
        val rotation = when (exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
        if (rotation == 0) return bitmap

        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun calculateInSampleSize(sourceMaxSide: Int, targetMaxSide: Int): Int {
        if (sourceMaxSide <= 0 || sourceMaxSide <= targetMaxSide) return 1
        var sample = 1
        var current = sourceMaxSide
        while (current > targetMaxSide) {
            sample *= 2
            current /= 2
        }
        return sample.coerceAtLeast(1)
    }

    private suspend fun <T> Task<T>.await(): T {
        return suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { continuation.resume(it) }
            addOnFailureListener { continuation.resumeWithException(it) }
            addOnCanceledListener { continuation.cancel() }
        }
    }
}

private const val MAX_BITMAP_SIDE = 2048
