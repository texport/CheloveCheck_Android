package com.chelovecheck.presentation.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.chelovecheck.domain.model.MapProvider
import java.util.Locale

data class MapLaunchResult(
    val query: String,
    val primaryUri: Uri?,
    val fallbackUri: Uri?,
    val success: Boolean,
    val usedFallback: Boolean,
    val failureReason: String?,
    val usedPackage: String?,
)

fun openMap(
    context: Context,
    provider: MapProvider,
    query: String,
): MapLaunchResult {
    val trimmed = query.trim()
    if (trimmed.isBlank()) {
        return MapLaunchResult(
            query = query,
            primaryUri = null,
            fallbackUri = null,
            success = false,
            usedFallback = false,
            failureReason = "empty_query",
            usedPackage = null,
        )
    }

    val encoded = Uri.encode(trimmed)
    val (primaryUri, fallbackUri, primaryPackage) = buildUris(provider, encoded)
    val primaryLaunched = tryLaunch(context, primaryUri, primaryPackage)
    if (primaryLaunched) {
        return MapLaunchResult(
            query = trimmed,
            primaryUri = primaryUri,
            fallbackUri = fallbackUri,
            success = true,
            usedFallback = false,
            failureReason = null,
            usedPackage = primaryPackage,
        )
    }

    val fallbackLaunched = tryLaunch(context, fallbackUri, null)
    return MapLaunchResult(
        query = trimmed,
        primaryUri = primaryUri,
        fallbackUri = fallbackUri,
        success = fallbackLaunched,
        usedFallback = fallbackLaunched,
        failureReason = if (fallbackLaunched) null else "no_handler",
        usedPackage = if (fallbackLaunched) null else primaryPackage,
    )
}

private fun buildUris(provider: MapProvider, encodedQuery: String): Triple<Uri, Uri, String?> {
    val country = Locale.getDefault().country.uppercase(Locale.ROOT)
    return when (provider) {
        MapProvider.GOOGLE -> {
            Triple(
                Uri.parse("geo:0,0?q=$encodedQuery"),
                Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedQuery"),
                "com.google.android.apps.maps",
            )
        }
        MapProvider.YANDEX -> {
            val host = if (country == "KZ") "yandex.kz" else "yandex.com"
            Triple(
                Uri.parse("yandexmaps://maps.yandex.ru/?text=$encodedQuery"),
                Uri.parse("https://$host/maps/?text=$encodedQuery"),
                "ru.yandex.yandexmaps",
            )
        }
        MapProvider.TWO_GIS -> {
            val host = if (country == "KZ") "2gis.kz" else "2gis.ru"
            Triple(
                Uri.parse("https://$host/search/$encodedQuery"),
                Uri.parse("https://$host/search/$encodedQuery"),
                "ru.dublgis.dgismobile",
            )
        }
    }
}

private fun tryLaunch(context: Context, uri: Uri, packageName: String?): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, uri)
    if (packageName != null) {
        intent.setPackage(packageName)
    }
    if (context !is Activity) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return runCatching {
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}
