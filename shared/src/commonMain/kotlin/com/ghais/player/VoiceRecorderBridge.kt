package com.ghais.player

import kotlinx.coroutines.flow.StateFlow

/**
 * Cross-platform bridge for recording and playing back user voice recitation per Ayah.
 *
 * commonMain declares the expect contract:
 * - androidMain implements recording via MediaRecorder (AAC/M4A) and playback via MediaPlayer.
 * - iosMain provides a compiling stub for future AVAudioRecorder wiring.
 */
expect object VoiceRecorderBridge {
    /**
     * Observable state: true if recording is currently active.
     */
    val isRecording: StateFlow<Boolean>

    /**
     * Elapsed duration of the active recording session in milliseconds.
     * Emits 0 when not recording.
     */
    val recordingDurationMs: StateFlow<Long>

    /**
     * Observable state: true if a recorded ayah audio is currently playing.
     */
    val isPlayingRecording: StateFlow<Boolean>

    /**
     * Starts recording voice for the specified Ayah.
     * Saved to context cache directory as `ayah_rec_{surahId}_{ayahNo}.m4a`.
     *
     * @param surahId Surah number (1-114).
     * @param ayahNo Ayah number within the Surah.
     * @return True if recording started successfully, false if permission is missing or initialization failed.
     */
    fun startRecording(surahId: Int, ayahNo: Int): Boolean

    /**
     * Stops the current recording session and finalizes the audio file.
     *
     * @return The absolute file path of the saved audio, or null if recording failed or was empty.
     */
    fun stopRecording(): String?

    /**
     * Returns the local file path if a recording exists for the given Surah and Ayah, or null.
     */
    fun getRecordingPath(surahId: Int, ayahNo: Int): String?

    /**
     * Returns true if a valid recording exists on disk for the given Surah and Ayah.
     */
    fun hasRecording(surahId: Int, ayahNo: Int): Boolean

    /**
     * Deletes any existing voice recording for the given Surah and Ayah.
     * Stops playback or active recording if target matches.
     */
    fun deleteRecording(surahId: Int, ayahNo: Int)

    /**
     * Starts playback of a recorded audio file from the specified local file path.
     */
    fun playRecording(path: String)

    /**
     * Stops any active playback of user recordings.
     */
    fun stopPlayback()
}
