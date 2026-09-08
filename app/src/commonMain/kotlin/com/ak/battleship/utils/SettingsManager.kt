package com.ak.battleship.utils

expect class SettingsManager {
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun setBoolean(key: String, value: Boolean)
    fun getString(key: String, defaultValue: String): String
    fun setString(key: String, value: String)
}

expect fun createSettingsManager(context: Any?): SettingsManager
