package com.ak.battleship

import android.content.Context
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual object PlatformServices {
    actual fun showToast(context: Any?, message: String) {
        val ctx = context as? Context ?: return
        Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
    }
    
    actual fun formatTimestamp(timestamp: Long): String {
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        return dateFormatter.format(Date(timestamp))
    }

    actual fun parseTimestamp(timestampRaw: String): Long? {
        return try {
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            dateFormatter.parse(timestampRaw)?.time
        } catch (e: Exception) {
            null
        }
    }

    actual fun getCurrentTimeMillis(): Long {
        return System.currentTimeMillis()
    }
    
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
