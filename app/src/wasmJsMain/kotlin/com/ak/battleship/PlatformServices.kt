package com.ak.battleship

import kotlinx.browser.window

internal fun jsDateNow(): Double = js("Date.now()")
internal fun jsFormatDate(timestamp: Double): String = js("""
    if (!timestamp || timestamp <= 0) return '';
    return new Date(timestamp).toISOString().replace(/\.\d{3}Z$/, 'Z');
""")
internal fun jsParseDate(dateStr: String): Double = js("""
    if (!dateStr || dateStr.trim() === '') return NaN;
    var cleaned = dateStr.trim().replace(/^"/, '').replace(/"$/, '').trim();
    var d = Date.parse(cleaned);
    if (!isNaN(d)) return d;
    if (cleaned.indexOf(', ') !== -1) {
        var parts = cleaned.split(', ');
        var d2 = Date.parse(parts[0] + ' ' + parts[1]);
        if (!isNaN(d2)) return d2;
    }
    return NaN;
""")
internal fun jsGenerateUUID(): String = js("crypto.randomUUID()")
internal fun jsFallbackUUID(): String = js("\"wasm-\" + Date.now() + \"-\" + Math.random()")

actual object PlatformServices {
    actual fun showToast(context: Any?, message: String) {
        window.alert(message)
    }
    
    actual fun formatTimestamp(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        return try {
            jsFormatDate(timestamp.toDouble())
        } catch(e: Exception) {
            ""
        }
    }

    actual fun parseTimestamp(timestampRaw: String): Long? {
        val clean = timestampRaw.trim().removeSurrounding("\"").trim()
        if (clean.isEmpty()) return null
        return try {
            val parsed = jsParseDate(clean)
            if (parsed.isNaN()) clean.toLongOrNull() else parsed.toLong()
        } catch(e: Exception) {
            clean.toLongOrNull()
        }
    }

    actual fun getCurrentTimeMillis(): Long {
        return try {
            jsDateNow().toLong()
        } catch(e: Exception) {
            0L
        }
    }
    
    actual fun generateUUID(): String { return try { jsGenerateUUID() } catch(e: Exception) { jsFallbackUUID() } }

    actual suspend fun readUriText(context: Any?, uri: Any?): String? {
        return null
    }
}
