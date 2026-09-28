package com.example.aidocumentorganizer.ui.components

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

const val DAY_MS = 24L * 60 * 60 * 1000
const val EXPIRY_WARNING_DAYS = 30

fun formatDate(timestamp: Long): String =
    try {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    } catch (e: Exception) {
        ""
    }

// Expiry dates are UTC-midnight values, so format them in UTC to avoid the
// date shifting by a day in some time zones.
fun formatExpiry(timestamp: Long): String =
    try {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(timestamp))
    } catch (e: Exception) {
        ""
    }

fun formatFileSize(path: String): String? {

    if (path.isBlank()) return null

    val file = File(path)
    if (!file.exists()) return null

    val bytes = file.length()

    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    }
}

fun daysUntil(expiry: Long, now: Long = System.currentTimeMillis()): Int =
    Math.ceil((expiry - now).toDouble() / DAY_MS).toInt()

fun isExpiringSoon(expiry: Long?, now: Long = System.currentTimeMillis()): Boolean =
    expiry != null &&
            expiry >= now - DAY_MS &&
            expiry <= now + EXPIRY_WARNING_DAYS * DAY_MS
