package com.example.mangaocr_demon.ui

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes

class GoogleAuthManager(private val context: Context) {

    private val signInClient: GoogleSignInClient

    companion object {
        private const val TAG = "GoogleAuthManager"
    }

    init {
        try {
            // ✅ Lấy Web Client ID từ google-services.json
            val webClientId = getWebClientId()

            Log.d(TAG, "Initializing with Web Client ID: $webClientId")

            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .requestIdToken(webClientId)
                .requestScopes(
                    Scope(DriveScopes.DRIVE_FILE),
                    Scope(DriveScopes.DRIVE_APPDATA)
                )
                .build()

            signInClient = GoogleSignIn.getClient(context, gso)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing GoogleAuthManager", e)
            throw e
        }
    }

    /**
     * Lấy Web Client ID từ google-services.json
     */
    private fun getWebClientId(): String {
        return try {
            // Tìm resource ID của default_web_client_id
            val resourceId = context.resources.getIdentifier(
                "default_web_client_id",
                "string",
                context.packageName
            )

            if (resourceId != 0) {
                val webClientId = context.getString(resourceId)
                Log.d(TAG, "Found Web Client ID from resources")
                webClientId
            } else {
                // ❌ Không tìm thấy - cần check google-services.json
                val error = """
                    Missing default_web_client_id!
                    
                    SOLUTIONS:
                    1. Make sure google-services.json is in app/ folder
                    2. Rebuild project (Build → Rebuild Project)
                    3. Check if google-services.json has "client_type": 3
                    
                    Or manually add to strings.xml:
                    <string name="default_web_client_id">YOUR_WEB_CLIENT_ID</string>
                """.trimIndent()

                Log.e(TAG, error)
                throw IllegalStateException(error)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Web Client ID", e)
            throw e
        }
    }

    /**
     * Lấy Intent để sign in
     */
    fun getSignInIntent(): Intent {
        return signInClient.signInIntent
    }

    /**
     * Đăng xuất
     */
    fun signOut(onComplete: () -> Unit) {
        signInClient.signOut().addOnCompleteListener {
            Log.d(TAG, "Sign out completed")
            onComplete()
        }
    }

    /**
     * Revoke access (đăng xuất hoàn toàn)
     */
    fun revokeAccess(onComplete: () -> Unit) {
        signInClient.revokeAccess().addOnCompleteListener {
            Log.d(TAG, "Revoke access completed")
            onComplete()
        }
    }

    /**
     * Lấy account đã đăng nhập gần nhất
     */
    fun getLastSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    /**
     * Kiểm tra đã đăng nhập chưa
     */
    fun isSignedIn(): Boolean {
        val account = getLastSignedInAccount()
        return account != null
    }

    /**
     * Kiểm tra có quyền Drive không
     */
    fun hasDrivePermissions(): Boolean {
        val account = getLastSignedInAccount() ?: return false
        return GoogleSignIn.hasPermissions(
            account,
            Scope(DriveScopes.DRIVE_FILE),
            Scope(DriveScopes.DRIVE_APPDATA)
        )
    }
}