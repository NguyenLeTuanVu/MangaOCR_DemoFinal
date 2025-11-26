package com.example.mangaocr_demon.utils

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "manga_reader_settings"

        // Keys
        private const val KEY_THEME = "theme"
        private const val KEY_READING_MODE = "reading_mode"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_BRIGHTNESS = "brightness"

        // Default values
        const val THEME_DARK = "dark"
        const val THEME_LIGHT = "light"
        const val THEME_SEPIA = "sepia"

        const val MODE_HORIZONTAL = "horizontal"
        const val MODE_VERTICAL = "vertical"
        const val MODE_WEBTOON = "webtoon"
    }

    // Theme
    var theme: String
        get() = prefs.getString(KEY_THEME, THEME_DARK) ?: THEME_DARK
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    // Reading Mode
    var readingMode: String
        get() = prefs.getString(KEY_READING_MODE, MODE_HORIZONTAL) ?: MODE_HORIZONTAL
        set(value) = prefs.edit().putString(KEY_READING_MODE, value).apply()

    // Keep Screen On
    var keepScreenOn: Boolean
        get() = prefs.getBoolean(KEY_KEEP_SCREEN_ON, true)
        set(value) = prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, value).apply()

    // Brightness (0-100)
    var brightness: Int
        get() = prefs.getInt(KEY_BRIGHTNESS, -1) // -1 = system default
        set(value) = prefs.edit().putInt(KEY_BRIGHTNESS, value).apply()

    // Get theme background color
    fun getThemeBackgroundColor(): Int {
        return when (theme) {
            THEME_LIGHT -> 0xFFFFFFFF.toInt() // White
            THEME_SEPIA -> 0xFFF4ECD8.toInt() // Sepia
            else -> 0xFF000000.toInt() // Black (Dark)
        }
    }

    // Get theme text color
    fun getThemeTextColor(): Int {
        return when (theme) {
            THEME_LIGHT, THEME_SEPIA -> 0xFF000000.toInt() // Black text
            else -> 0xFFFFFFFF.toInt() // White text
        }
    }
}