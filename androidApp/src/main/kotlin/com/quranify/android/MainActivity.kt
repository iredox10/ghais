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
import com.quranify.player.QuranDownloads

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.quranify.player.PlayerBridge.init(this)
        com.quranify.player.AmbientPlayerBridge.init(this)
        com.quranify.player.AmbientVideoBridge.init(this)
        QuranDownloads.init(this)
        com.quranify.data.repository.ScheduleEngine.init(this)
        // TEMP-DEBUG schedule hook (revert).
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                val cal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MINUTE, 2) }
                com.quranify.data.repository.SchedulesStore.add(
                    com.quranify.data.repository.RecitationSchedule(
                        id = "hook-test",
                        hour = cal.get(java.util.Calendar.HOUR_OF_DAY),
                        minute = cal.get(java.util.Calendar.MINUTE),
                        reciterSlug = "mishary",
                        fromSurah = 112, toSurah = 114,
                        durationMin = null, enabled = true
                    )
                )
                android.util.Log.d("SchedHook", "test schedule added")
            } catch (e: Exception) {
                android.util.Log.e("SchedHook", "hook failed", e)
            }
        }, 4000)
        // Any play/resume boots the foreground MediaSession service so audio
        // survives background + shows system notification controls.
        com.quranify.player.PlayerBridge.onPlayRequested = {
            requestNotificationPermission()
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) return
        try {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } catch (_: Exception) { }
    }
}
