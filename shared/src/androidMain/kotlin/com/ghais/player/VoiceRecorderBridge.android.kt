package com.ghais.player

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Android actual: Voice recording using [MediaRecorder] (AAC audio in M4A container)
 * and playback using [MediaPlayer].
 *
 * Recordings are stored in the application cache directory:
 * `<cacheDir>/ayah_rec_{surah}_{ayah}.m4a`
 */
actual object VoiceRecorderBridge {
    private const val TAG = "VoiceRecorderBridge"

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    @Volatile
    private var appContext: Context? = null

    private val _isRecording = MutableStateFlow(false)
    actual val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    actual val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

    private val _isPlayingRecording = MutableStateFlow(false)
    actual val isPlayingRecording: StateFlow<Boolean> = _isPlayingRecording.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null
    private var timerJob: Job? = null

    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingPath: String? = null

    private val lock = Any()

    /**
     * Must be called once during app startup (e.g. MainActivity.onCreate)
     * so cache directory and system services are accessible.
     */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun getRecordingFile(surahId: Int, ayahNo: Int): File? {
        val ctx = appContext ?: return null
        return File(ctx.cacheDir, "ayah_rec_${surahId}_${ayahNo}.m4a")
    }

    actual fun startRecording(surahId: Int, ayahNo: Int): Boolean {
        synchronized(lock) {
            val context = appContext ?: run {
                Log.e(TAG, "startRecording: appContext is null. Call VoiceRecorderBridge.init(context) first.")
                return false
            }

            if (context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "startRecording: RECORD_AUDIO permission not granted")
                return false
            }

            // Stop any ongoing recording cleanly
            if (_isRecording.value) {
                stopRecordingInternal()
            }

            // Stop any voice playback before recording
            stopPlaybackInternal()

            try {
                val cacheDir = context.cacheDir
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }

                val file = File(cacheDir, "ayah_rec_${surahId}_${ayahNo}.m4a")
                if (file.exists()) {
                    file.delete()
                }

                val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }

                recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                recorder.setAudioEncodingBitRate(128_000)
                recorder.setAudioSamplingRate(44_100)
                recorder.setOutputFile(file.absolutePath)
                recorder.prepare()
                recorder.start()

                mediaRecorder = recorder
                currentRecordingFile = file
                _isRecording.value = true
                _recordingDurationMs.value = 0L

                startTimer()
                return true
            } catch (e: Exception) {
                Log.e(TAG, "startRecording failed for surah=$surahId, ayah=$ayahNo", e)
                cleanupRecorder()
                _isRecording.value = false
                _recordingDurationMs.value = 0L
                return false
            }
        }
    }

    actual fun stopRecording(): String? {
        synchronized(lock) {
            return stopRecordingInternal()
        }
    }

    private fun stopRecordingInternal(): String? {
        if (!_isRecording.value && mediaRecorder == null) {
            return null
        }

        stopTimer()
        _isRecording.value = false

        val rec = mediaRecorder
        val file = currentRecordingFile
        mediaRecorder = null
        currentRecordingFile = null

        var success = false
        if (rec != null) {
            try {
                rec.stop()
                success = true
            } catch (e: Exception) {
                Log.e(TAG, "MediaRecorder.stop() failed (recording may have been too brief)", e)
            } finally {
                try { rec.reset() } catch (_: Exception) {}
                try { rec.release() } catch (_: Exception) {}
            }
        }

        return if (success && file != null && file.exists() && file.length() > 0L) {
            file.absolutePath
        } else {
            file?.delete()
            null
        }
    }

    actual fun getRecordingPath(surahId: Int, ayahNo: Int): String? {
        val file = getRecordingFile(surahId, ayahNo) ?: return null
        return if (file.exists() && file.length() > 0L) file.absolutePath else null
    }

    actual fun hasRecording(surahId: Int, ayahNo: Int): Boolean {
        return getRecordingPath(surahId, ayahNo) != null
    }

    actual fun deleteRecording(surahId: Int, ayahNo: Int) {
        synchronized(lock) {
            val file = getRecordingFile(surahId, ayahNo) ?: return
            val filePath = file.absolutePath

            if (currentPlayingPath == filePath) {
                stopPlaybackInternal()
            }

            if (currentRecordingFile?.absolutePath == filePath) {
                cleanupRecorder()
                stopTimer()
                _isRecording.value = false
                _recordingDurationMs.value = 0L
            }

            if (file.exists()) {
                try {
                    file.delete()
                } catch (e: Exception) {
                    Log.e(TAG, "deleteRecording failed for $filePath", e)
                }
            }
        }
    }

    actual fun playRecording(path: String) {
        synchronized(lock) {
            val file = File(path)
            if (!file.exists() || file.length() == 0L) {
                Log.w(TAG, "playRecording: file missing or empty: $path")
                return
            }

            if (_isRecording.value) {
                stopRecordingInternal()
            }

            stopPlaybackInternal()

            try {
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(path)
                    setOnCompletionListener {
                        synchronized(lock) {
                            stopPlaybackInternal()
                        }
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra path=$path")
                        synchronized(lock) {
                            stopPlaybackInternal()
                        }
                        true
                    }
                    prepare()
                    start()
                }
                mediaPlayer = player
                currentPlayingPath = path
                _isPlayingRecording.value = true
            } catch (e: Exception) {
                Log.e(TAG, "playRecording failed for $path", e)
                stopPlaybackInternal()
            }
        }
    }

    actual fun stopPlayback() {
        synchronized(lock) {
            stopPlaybackInternal()
        }
    }

    private fun stopPlaybackInternal() {
        val player = mediaPlayer
        mediaPlayer = null
        currentPlayingPath = null
        _isPlayingRecording.value = false
        if (player != null) {
            try {
                if (player.isPlaying) {
                    player.stop()
                }
            } catch (_: Exception) {}
            try { player.reset() } catch (_: Exception) {}
            try { player.release() } catch (_: Exception) {}
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        val startTime = System.currentTimeMillis()
        timerJob = scope.launch {
            while (_isRecording.value) {
                _recordingDurationMs.value = (System.currentTimeMillis() - startTime).coerceAtLeast(0L)
                delay(50)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun cleanupRecorder() {
        val rec = mediaRecorder
        mediaRecorder = null
        val file = currentRecordingFile
        currentRecordingFile = null
        if (rec != null) {
            try { rec.reset() } catch (_: Exception) {}
            try { rec.release() } catch (_: Exception) {}
        }
        file?.delete()
    }
}
