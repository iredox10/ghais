package com.ghais.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS actual: stub implementation for voice recording and playback.
 * Real iOS recording requires AVAudioRecorder, AVAudioSession category Record,
 * and NSMicrophoneUsageDescription in Info.plist.
 */
actual object VoiceRecorderBridge {
    private val _isRecording = MutableStateFlow(false)
    actual val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    actual val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

    private val _isPlayingRecording = MutableStateFlow(false)
    actual val isPlayingRecording: StateFlow<Boolean> = _isPlayingRecording.asStateFlow()

    actual fun startRecording(surahId: Int, ayahNo: Int): Boolean {
        // Stub for iOS
        return false
    }

    actual fun stopRecording(): String? {
        _isRecording.value = false
        _recordingDurationMs.value = 0L
        return null
    }

    actual fun getRecordingPath(surahId: Int, ayahNo: Int): String? {
        return null
    }

    actual fun hasRecording(surahId: Int, ayahNo: Int): Boolean {
        return false
    }

    actual fun deleteRecording(surahId: Int, ayahNo: Int) {
        // Stub for iOS
    }

    actual fun playRecording(path: String) {
        // Stub for iOS
    }

    actual fun stopPlayback() {
        _isPlayingRecording.value = false
    }
}
