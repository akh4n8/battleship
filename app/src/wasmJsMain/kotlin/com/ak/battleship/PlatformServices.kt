package com.ak.battleship

import kotlinx.browser.window

internal fun jsDateNow(): Double = js("Date.now()")
internal fun jsFormatDate(timestamp: Double): String = js("new Date(timestamp).toLocaleString()")
internal fun jsParseDate(dateStr: String): Double = js("Date.parse(dateStr)")

actual object PlatformServices {
    actual fun showToast(context: Any?, message: String) {
        window.alert(message)
    }
    
    actual fun formatTimestamp(timestamp: Long): String {
        return try {
            jsFormatDate(timestamp.toDouble())
        } catch(e: Exception) {
            timestamp.toString()
        }
    }

    actual fun parseTimestamp(timestampRaw: String): Long? {
        return try {
            val parsed = jsParseDate(timestampRaw)
            if (parsed.isNaN()) timestampRaw.toLongOrNull() else parsed.toLong()
        } catch(e: Exception) {
            timestampRaw.toLongOrNull()
        }
    }

    actual fun getCurrentTimeMillis(): Long {
        return try {
            jsDateNow().toLong()
        } catch(e: Exception) {
            0L
        }
    }
    
    actual suspend fun readUriText(context: Any?, uri: Any?): String? {
        return null
    }
}
