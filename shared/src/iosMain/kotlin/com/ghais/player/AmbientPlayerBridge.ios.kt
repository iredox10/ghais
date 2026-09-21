@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.ghais.player

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.currentItem
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.volume
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.temporaryDirectory
import platform.Foundation.writeToFile

/**
 * iOS actual: a second AVPlayer looping the ambient file underneath the
 * main Quran player. Loops via AVPlayerItemDidPlayToEndTimeNotification
 * (seek-to-zero + play). Bytes come from shared composeResources, cached
 * in NSTemporaryDirectory on first use.
 */
actual object AmbientPlayerBridge {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var player: AVPlayer? = null
    private var endObserver: Any? = null
    private var preparedKey: String? = null
    private var pendingVolume: Float = 0.4f

    private fun ensurePlayer(): AVPlayer {
        var p = player
        if (p == null) {
            p = AVPlayer.playerWithPlayerItem(null)
            p.volume = pendingVolume
            player = p
        }
        return p
    }

    private fun observeLoop(item: AVPlayerItem) {
        try {
            endObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        } catch (_: Exception) { }
        endObserver = null
        try {
            endObserver = NSNotificationCenter.defaultCenter.addObserverForName(
                AVPlayerItemDidPlayToEndTimeNotification,
                item,
                null
            ) {
                try {
                    player?.seekToTime(CMTimeMakeWithSeconds(0.0, 600))
                    player?.play()
                } catch (_: Exception) { }
            }
        } catch (_: Exception) { }
    }

    actual fun playAmbient(assetKey: String) {
        scope.launch {
            try {
                if (preparedKey == assetKey) {
                    player?.play()
                    return@launch
                }
                val fileUrl = ensureCachedFile(assetKey) ?: return@launch
                val p = ensurePlayer()
                val item = AVPlayerItem.playerItemWithURL(fileUrl)
                observeLoop(item)
                p.replaceCurrentItemWithPlayerItem(item)
                p.volume = pendingVolume
                p.play()
                preparedKey = assetKey
            } catch (_: Exception) { }
        }
    }

    actual fun pauseAmbient() {
        try { player?.pause() } catch (_: Exception) { }
    }

    actual fun resumeAmbient() {
        try {
            if (preparedKey != null) player?.play()
        } catch (_: Exception) { }
    }

    actual fun stopAmbient() {
        try {
            player?.pause()
            player?.replaceCurrentItemWithPlayerItem(null)
        } catch (_: Exception) { }
        try {
            endObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        } catch (_: Exception) { }
        endObserver = null
        preparedKey = null
    }

    actual fun setAmbientVolume(volume: Float) {
        pendingVolume = volume.coerceIn(0f, 1f)
        try { player?.volume = pendingVolume } catch (_: Exception) { }
    }

    private suspend fun ensureCachedFile(assetKey: String): NSURL? {
        return try {
            val bytes = ghais.shared.generated.resources.Res
                .readBytes("files/ambient/$assetKey.mp3")
            if (bytes.isEmpty()) return null
            val tmp = NSFileManager.defaultManager.temporaryDirectory.path
                ?: return null
            val path = "$tmp/quranify_ambient_$assetKey.mp3"
            val data: NSData = memScoped {
                NSData.create(
                    bytes = bytes.usePinned { it.addressOf(0) as CPointer<ByteVar> },
                    length = bytes.size.toULong()
                )
            }
            val ok = data.writeToFile(path, true)
            if (!ok) return null
            NSURL.fileURLWithPath(path)
        } catch (_: Exception) {
            null
        }
    }
}
