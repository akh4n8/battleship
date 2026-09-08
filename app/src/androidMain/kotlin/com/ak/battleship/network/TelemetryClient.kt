package com.ak.battleship.network

import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

actual class TelemetryClient actual constructor() {
    actual fun sendMatchData(url: String, anonKey: String, payload: String) {
        if (url.isBlank() || anonKey.isBlank()) return
        thread {
            try {
                val endpoint = URL("$url/rest/v1/game_telemetry")
                val conn = endpoint.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("apikey", anonKey)
                conn.setRequestProperty("Authorization", "Bearer $anonKey")
                conn.doOutput = true
                conn.outputStream.use { os ->
                    val input = payload.toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }
                val code = conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
