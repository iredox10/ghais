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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ -> }

    private var askedOnce = false

    // Caller-owned scope for deferred startup work: SupervisorJob so one
    // failing child can never cancel the others or crash the launch.
    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // One bad bridge init must never kill onCreate outright (the app "closes
    // by itself") nor skip the inits after it: every risky init runs here.
    private inline fun guardInit(tag: String, block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            try {
                android.util.Log.e("MainActivity", "$tag init failed", t)
            } catch (_: Exception) {
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ExoPlayer builders touch audio service + codecs: can throw on
        // devices with broken media stacks — must not kill the launch.
        guardInit("PlayerBridge") { com.ghais.player.PlayerBridge.init(this) }
        guardInit("AmbientPlayerBridge") { com.ghais.player.AmbientPlayerBridge.init(this) }
        // Also warms AmbientVideoArt from app assets: I/O + decoder probing.
        guardInit("AmbientVideoBridge") { com.ghais.player.AmbientVideoBridge.init(this) }
        guardInit("VoiceRecorderBridge") { com.ghais.player.VoiceRecorderBridge.init(this) }
        guardInit("ScheduleEngine") { com.ghais.data.repository.ScheduleEngine.init(this) }
        // Re-arm alarms on every launch so schedules programmed with older
        // APIs migrate to alarm-clock delivery (background FGS exemption).
        try {
            com.ghais.data.repository.ScheduleEngine.refresh(
                com.ghais.data.repository.SchedulesStore.schedules.value
            )
        } catch (_: Exception) { }
        // SharedPreferences read on the main thread: small but I/O — a
        // corrupted prefs file throws instead of returning defaults.
        guardInit("AuthRepository") { com.ghais.data.auth.AuthRepository.init(this) }
        guardInit("ActivityHolder") { com.ghais.data.auth.ActivityHolder.register(this) }
        // Starts the listening-push heartbeat loop + wires cloud fetchers.
        guardInit("SyncEngine") { com.ghais.data.sync.SyncEngine.init(this) }
        // registerNetworkCallback can throw SecurityException on devices
        // without the network-state permission / OEM-skewed services.
        guardInit("NetworkMonitor") { com.ghais.data.sync.NetworkMonitor.init(this) }
        guardInit("ShareSheet") { com.ghais.data.share.ShareSheet.init(this) }
        // Download-index restore walks filesDir (disk I/O): off the main
        // thread so a large/cold cache can't ANR the launch. Lookups degrade
        // to streaming until the index lands, so deferring is safe.
        startupScope.launch(Dispatchers.IO) {
            guardInit("QuranDownloads") { QuranDownloads.init(this@MainActivity) }
        }
        // Any play/resume boots the foreground MediaSession service so audio
        // survives background + shows system notification controls.
        com.ghais.player.PlayerBridge.onPlayRequested = {
            // May fire while the activity is stopped/backgrounded (alarm,
            // headset resume): launcher + FGS start both throw there.
            try {
                requestNotificationPermission()
                QuranPlaybackService.start(this@MainActivity)
            } catch (_: Exception) { }
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
        // Malformed deep links / a wedged OAuth handshake must never take
        // down the activity: the redirect completes a Deferred that may
        // already be settled.
        try {
            com.ghais.data.auth.GoogleWebAuth.onRedirect(intent.data)
        } catch (_: Exception) { }
    }

    override fun onResume() {
        super.onResume()
        // Resolves a pending browser OAuth handshake as cancelled when the
        // user backed out of the tab without completing the redirect.
        try {
            com.ghais.data.auth.GoogleWebAuth.onReturnedToApp()
        } catch (_: Exception) { }
        if (!askedOnce) {
            askedOnce = true
            requestNotificationPermission()
        }
    }

    override fun onDestroy() {
        try {
            startupScope.cancel()
        } catch (_: Exception) { }
        super.onDestroy()
    }

    private fun requestNotificationPermission() {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) return
            // Launching after onSaveInstanceState / while finishing throws
            // IllegalStateException (caught below, but skip the no-op first).
            if (isFinishing || isDestroyed) return
            try {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } catch (_: Exception) { }
        } catch (_: Exception) { }
    }
}
