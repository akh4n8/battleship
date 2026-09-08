package com.ak.battleship.utils

import kotlinx.browser.window

actual class SettingsManager {
    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        val str = window.localStorage.getItem(key)
        return if (str != null) str.toBoolean() else defaultValue
    }
    
    actual fun setBoolean(key: String, value: Boolean) {
        window.localStorage.setItem(key, value.toString())
    }

    actual fun getString(key: String, defaultValue: String): String {
        return window.localStorage.getItem(key) ?: defaultValue
    }

    actual fun setString(key: String, value: String) {
        window.localStorage.setItem(key, value)
    }
}

actual fun createSettingsManager(context: Any?): SettingsManager {
    return SettingsManager()
}
