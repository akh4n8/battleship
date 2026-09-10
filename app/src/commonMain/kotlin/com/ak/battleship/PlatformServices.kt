package com.ak.battleship

expect object PlatformServices {
    fun showToast(context: Any?, message: String)
    fun formatTimestamp(timestamp: Long): String
    fun parseTimestamp(timestampRaw: String): Long?
    fun getCurrentTimeMillis(): Long
    fun generateUUID(): String
    suspend fun readUriText(context: Any?, uri: Any?): String?
}
