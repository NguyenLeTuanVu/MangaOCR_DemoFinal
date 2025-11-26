package com.example.mangaocr_demon.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.ContextCompat
import com.example.mangaocr_demon.R

class SettingsManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    companion object {
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SEPIA = "sepia"

        const val MODE_HORIZONTAL = "horizontal"
        const val MODE_VERTICAL = "vertical"
        const val MODE_WEBTOON = "webtoon"
    }

    var theme: String
        get() = prefs.getString("theme", THEME_LIGHT) ?: THEME_LIGHT
        set(value) = prefs.edit().putString("theme", value).apply()

    var readingMode: String
        get() = prefs.getString("reading_mode", MODE_HORIZONTAL) ?: MODE_HORIZONTAL
        set(value) = prefs.edit().putString("reading_mode", value).apply()

    var keepScreenOn: Boolean
        get() = prefs.getBoolean("keep_screen_on", false)
        set(value) = prefs.edit().putBoolean("keep_screen_on", value).apply()

    var brightness: Int
        get() = prefs.getInt("brightness", -1)   // -1 nghĩa là mặc định
        set(value) = prefs.edit().putInt("brightness", value).apply()

    // 🟩 Thêm hàm này để sửa lỗi "Unresolved reference"
    fun getThemeBackgroundColor(): Int {
        return when (theme) {
            THEME_DARK -> ContextCompat.getColor(context, R.color.background_dark)
            THEME_SEPIA -> ContextCompat.getColor(context, R.color.background_sepia)
            else -> ContextCompat.getColor(context, R.color.background_light)
        }
    }

    fun getThemeTextColor(): Int {
        return when (theme) {
            THEME_DARK -> ContextCompat.getColor(context, R.color.text_light) // chữ sáng cho nền tối
            THEME_SEPIA -> ContextCompat.getColor(context, R.color.text_dark)  // chữ nâu đậm
            else -> ContextCompat.getColor(context, R.color.text_dark)         // chữ đen cho nền sáng
        }
    }

}
