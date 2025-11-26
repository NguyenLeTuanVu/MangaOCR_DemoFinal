package com.example.mangaocr_demon

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.mangaocr_demon.databinding.ActivityLoginBinding
import com.example.mangaocr_demon.ui.GoogleAuthManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleAuthManager: GoogleAuthManager

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)

                // Lưu thông tin user
                saveUserInfo(
                    email = account.email,
                    name = account.displayName,
                    photoUrl = account.photoUrl?.toString()
                )

                // Trả kết quả về SettingsFragment
                setResult(Activity.RESULT_OK)
                finish()

                showToast("Đăng nhập thành công!")
            } catch (e: ApiException) {
                hideLoading()
                showToast("Đăng nhập thất bại: ${e.message}")
            }
        } else {
            hideLoading()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        googleAuthManager = GoogleAuthManager(this)

        setupViews()
    }

    private fun setupViews() {
        // Nút đóng
        binding.btnClose.setOnClickListener {
            finish()
        }

        // Nút đăng nhập Google
        binding.btnGoogleSignIn.setOnClickListener {
            showLoading()
            signInLauncher.launch(googleAuthManager.getSignInIntent())
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnGoogleSignIn.isEnabled = false
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.btnGoogleSignIn.isEnabled = true
    }

    private fun saveUserInfo(email: String?, name: String?, photoUrl: String?) {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        prefs.edit().apply {
            putString("user_email", email)
            putString("user_name", name)
            putString("user_photo", photoUrl)
            putBoolean("is_logged_in", true)
            putLong("login_time", System.currentTimeMillis())
            apply()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}