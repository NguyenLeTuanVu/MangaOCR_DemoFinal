package com.example.mangaocr_demon

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.mangaocr_demon.ui.SettingsManager
import com.example.mangaocr_demon.databinding.ActivityMainBinding
import com.example.mangaocr_demon.ui.history.BookcaseFragment
import androidx.core.view.WindowCompat
import com.example.mangaocr_demon.HomeFragment
import com.example.mangaocr_demon.HistoryFragment
import com.example.mangaocr_demon.SettingsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var settingsManager: SettingsManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsManager = SettingsManager(this)

        applyTheme()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }


        // Fragment mặc định là Home
        replaceFragment(HomeFragment())
        binding.bottomNav.selectedItemId = R.id.nav_home // đánh dấu Home là active

        // Lắng nghe chọn item BottomNavigationView
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> replaceFragment(HomeFragment())
                R.id.nav_bookcase -> replaceFragment(BookcaseFragment())
                R.id.nav_history -> replaceFragment(HistoryFragment())
                R.id.nav_settings -> replaceFragment(SettingsFragment())
            }
            true
        }
    }

    private fun applyTheme() {
        val backgroundColor = settingsManager.getThemeBackgroundColor()

        // Apply to window
        window.decorView.setBackgroundColor(backgroundColor)

        // Optional: Apply to status bar
        when (settingsManager.theme) {
            SettingsManager.THEME_DARK -> {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = false
                }
            }
            else -> {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = true
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        applyTheme()
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
