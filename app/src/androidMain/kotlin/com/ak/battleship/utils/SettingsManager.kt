package com.ak.battleship.utils

import android.content.Context

actual class SettingsManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("battleship_prefs", Context.MODE_PRIVATE)
    
    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }
    
    actual fun setBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    actual fun getString(key: String, defaultValue: String): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    actual fun setString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
}

actual fun createSettingsManager(context: Any?): SettingsManager {
    return SettingsManager(context as Context)
}
