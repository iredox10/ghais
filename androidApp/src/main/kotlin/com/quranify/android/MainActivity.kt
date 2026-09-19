package com.quranify.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.quranify.App

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.quranify.player.PlayerBridge.init(this)
        com.quranify.player.AmbientPlayerBridge.init(this)
        // Any play/resume boots the foreground MediaSession service so audio
        // survives background + shows system notification controls.
        com.quranify.player.PlayerBridge.onPlayRequested = {
            QuranPlaybackService.start(this@MainActivity)
        }
        // Do NOT warm-start the FGS here: starting a foreground service with an
        // idle player posts no notification within 10s ->
        // ForegroundServiceDidNotStartInTimeException (app "closes by itself").
        requestNotificationPermission()
        enableEdgeToEdge()
        setContent {
            App()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) return
        try {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } catch (_: Exception) { }
    }
}
