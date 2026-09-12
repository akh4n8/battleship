package com.ak.battleship

import android.content.Context
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

actual object PlatformServices {
    actual fun showToast(context: Any?, message: String) {
        val ctx = context as? Context ?: return
        Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
    }
    
    actual fun formatTimestamp(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return dateFormatter.format(Date(timestamp))
    }

    actual fun parseTimestamp(timestampRaw: String): Long? {
        val clean = timestampRaw.trim().removeSurrounding("\"").trim()
        if (clean.isEmpty()) return null

        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
            SimpleDateFormat("yyyy-MM-dd, hh:mm:ss a", Locale.US),
            SimpleDateFormat("yyyy-MM-dd, HH:mm:ss", Locale.US),
            SimpleDateFormat("MM/dd/yyyy, hh:mm:ss a", Locale.US),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        )
        for (fmt in formats) {
            try {
                val parsed = fmt.parse(clean)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {}
        }
        return clean.toLongOrNull()
    }

    actual fun getCurrentTimeMillis(): Long {
        return System.currentTimeMillis()
    }
    
    actual fun generateUUID(): String = java.util.UUID.randomUUID().toString()

    actual suspend fun readUriText(context: Any?, uri: Any?): String? {
        val ctx = context as? Context ?: return null
        val u = uri as? Uri ?: return null
        return withContext(Dispatchers.IO) {
            try {
                ctx.contentResolver.openInputStream(u)?.use { it.bufferedReader().readText() }
            } catch (e: Exception) {
                null
            }
        }
    }
}
