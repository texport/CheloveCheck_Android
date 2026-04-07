package com.chelovecheck.presentation.screens.scan

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.chelovecheck.R
import com.chelovecheck.domain.model.AppError
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

internal fun parseManualDateTime(value: String): LocalDateTime? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null
    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    return runCatching { LocalDateTime.parse(trimmed, formatter) }.getOrNull()
}

internal fun formatManualDateTime(date: LocalDate, time: LocalTime): String {
    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    return formatter.format(LocalDateTime.of(date, time.withSecond(0)))
}

internal fun createPhotoUri(context: Context): Uri {
    val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File(imagesDir, "receipt_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

internal fun shouldShowSupport(error: AppError): Boolean {
    return error is AppError.Unknown ||
        error is AppError.UnsupportedDomain ||
        error is AppError.ParsingError ||
        error is AppError.SslError ||
        error is AppError.NetworkError
}

internal fun openSupport(context: Context) {
    val uri = Uri.parse(context.getString(R.string.support_telegram_url))
    val intent = Intent(Intent.ACTION_VIEW, uri)
    context.startActivity(intent)
}

internal fun openReceiptInBrowser(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}

internal fun isCameraPermissionPermanentlyDenied(
    activity: Activity?,
    permission: String,
    hasPermission: Boolean,
    wasRequested: Boolean,
): Boolean {
    if (hasPermission || !wasRequested || activity == null) return false
    return !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
}

internal fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    )
    context.startActivity(intent)
}
