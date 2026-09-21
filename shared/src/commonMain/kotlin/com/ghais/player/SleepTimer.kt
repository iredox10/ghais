package com.ghais.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class StopCondition {
    MINUTES, END_OF_AYAH, END_OF_SURAH, END_OF_QUEUE
}

data class SleepTimerState(
    val isActive: Boolean = false,
    val remainingSeconds: Long = 0,
    val stopCondition: StopCondition = StopCondition.MINUTES,
    val fadeOutEnabled: Boolean = true
) {
    val volumeMultiplier: Float
        get() = if (isActive && fadeOutEnabled && remainingSeconds <= 30 && stopCondition == StopCondition.MINUTES) {
            remainingSeconds.toFloat() / 30f
        } else {
            1f
        }
}

object SleepTimer {
    private val _state = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default)
    private var timerJob: Job? = null

    // Callback to actually stop the player. The audio engine should set this.
    var onStopPlayer: (() -> Unit)? = null

    fun startTimer(durationMinutes: Int, condition: StopCondition = StopCondition.MINUTES, fadeOut: Boolean = true) {
        cancelTimer()
        val durationSeconds = durationMinutes * 60L
        _state.update { 
            it.copy(
                isActive = true,
                remainingSeconds = durationSeconds,
                stopCondition = condition,
                fadeOutEnabled = fadeOut
            )
        }
        
        if (condition == StopCondition.MINUTES) {
            timerJob = scope.launch {
                while (_state.value.remainingSeconds > 0) {
                    delay(1000)
                    _state.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }
                }
                stopPlayer()
            }
        }
    }

    fun startAtAyahBoundary(fadeOut: Boolean = true) {
        startTimer(0, StopCondition.END_OF_AYAH, fadeOut)
    }

    fun startAtSurahBoundary(fadeOut: Boolean = true) {
        startTimer(0, StopCondition.END_OF_SURAH, fadeOut)
    }
    
    fun startAtQueueBoundary(fadeOut: Boolean = true) {
        startTimer(0, StopCondition.END_OF_QUEUE, fadeOut)
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _state.update { it.copy(isActive = false, remainingSeconds = 0) }
    }

    fun onAyahEnded() {
        if (_state.value.isActive && _state.value.stopCondition == StopCondition.END_OF_AYAH) {
            stopPlayer()
        }
    }

    fun onSurahEnded() {
        if (_state.value.isActive && _state.value.stopCondition == StopCondition.END_OF_SURAH) {
            stopPlayer()
        }
    }
    
    fun onQueueEnded() {
        if (_state.value.isActive && _state.value.stopCondition == StopCondition.END_OF_QUEUE) {
            stopPlayer()
        }
    }

    private fun stopPlayer() {
        cancelTimer()
        onStopPlayer?.invoke()
    }
}
