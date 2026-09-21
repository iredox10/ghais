package com.ghais.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.ghais.App
import com.ghais.player.QuranDownloads
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.ghais.player.PlayerBridge.init(this)
        com.ghais.player.AmbientPlayerBridge.init(this)
        com.ghais.player.AmbientVideoBridge.init(this)
        QuranDownloads.init(this)
        com.ghais.data.repository.ScheduleEngine.init(this)
        com.ghais.data.auth.AuthRepository.init(this)
        // Any play/resume boots the foreground MediaSession service so audio
        // survives background + shows system notification controls.
        com.ghais.player.PlayerBridge.onPlayRequested = {
            requestNotificationPermission()
            QuranPlaybackService.start(this@MainActivity)
        }
        // Do NOT warm-start the FGS here: starting a foreground service with an
        // idle player posts no notification within 10s ->
        // ForegroundServiceDidNotStartInTimeException (app "closes by itself").
        requestNotificationPermission()
        enableEdgeToEdge()
        handleOAuthRedirect(intent)
        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthRedirect(intent)
    }

    /** Appwrite Google-OAuth return: appwrite-callback-*?userId=..&secret=.. */
    private fun handleOAuthRedirect(intent: android.content.Intent?) {
        val uri = intent?.data ?: return
        val scheme = uri.scheme ?: return
        if (scheme != "appwrite-callback-tikitakatalk" && scheme != "ghais") return
        val userId = uri.getQueryParameter("userId")
        val secret = uri.getQueryParameter("secret")
        if (userId.isNullOrBlank() || secret.isNullOrBlank()) return
        kotlinx.coroutines.MainScope().launch {
            val result = com.ghais.data.auth.AuthRepository.completeGoogleSignIn(userId, secret)
            result.exceptionOrNull()?.let {
                android.util.Log.e("GhaisAuth", "Google sign-in completion failed", it)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) return
        try {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } catch (_: Exception) { }
    }
}
