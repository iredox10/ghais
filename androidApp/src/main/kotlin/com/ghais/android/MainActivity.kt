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

    private var askedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.ghais.player.PlayerBridge.init(this)
        com.ghais.player.AmbientPlayerBridge.init(this)
        com.ghais.player.AmbientVideoBridge.init(this)
        com.ghais.player.VoiceRecorderBridge.init(this)
        QuranDownloads.init(this)
        com.ghais.data.repository.ScheduleEngine.init(this)
        com.ghais.data.auth.AuthRepository.init(this)
        com.ghais.data.auth.ActivityHolder.register(this)
        com.ghais.data.sync.SyncEngine.init(this)
        com.ghais.data.sync.NetworkMonitor.init(this)
        com.ghais.data.share.ShareSheet.init(this)
        // Any play/resume boots the foreground MediaSession service so audio
        // survives background + shows system notification controls.
        com.ghais.player.PlayerBridge.onPlayRequested = {
            requestNotificationPermission()
            QuranPlaybackService.start(this@MainActivity)
        }
        // Do NOT warm-start the FGS here: starting a foreground service with an
        // idle player posts no notification within 10s ->
        // ForegroundServiceDidNotStartInTimeException (app "closes by itself").
        enableEdgeToEdge()
        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        com.ghais.data.auth.GoogleWebAuth.onRedirect(intent.data)
    }

    override fun onResume() {
        super.onResume()
        // Resolves a pending browser OAuth handshake as cancelled when the
        // user backed out of the tab without completing the redirect.
        com.ghais.data.auth.GoogleWebAuth.onReturnedToApp()
        if (!askedOnce) {
            askedOnce = true
            requestNotificationPermission()
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
