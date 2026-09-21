package com.ghais.data.repository

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * OnboardingStore: persisted onboarding state.
 *
 * Mirrors [FavoritesStore]/[FollowStore] persistence approach (multiplatform
 * Settings + primitives + StateFlow). Load all in init; mutations update
 * flow + persist synchronously. Pure common code.
 */
object OnboardingStore {
    const val KEY_SEEN = "quranify_onboarding_seen"
    private const val KEY_STEP = "quranify_onboarding_step"
    private const val KEY_GOAL = "quranify_onboarding_goal"
    private const val KEY_DAILY_GOAL_MINUTES = "quranify_daily_goal_minutes"

    private const val DEFAULT_DAILY_GOAL_MINUTES = 15

    private var ownerId: String = "local"

    private fun key(base: String): String =
        if (ownerId == "local") base else "$ownerId::$base"

    private val settings: Settings by lazy { Settings() }

    private val _seen = MutableStateFlow(false)
    val seen: StateFlow<Boolean> = _seen.asStateFlow()

    private val _step = MutableStateFlow(0)
    val step: StateFlow<Int> = _step.asStateFlow()

    private val _goal = MutableStateFlow<String?>(null)
    val goal: StateFlow<String?> = _goal.asStateFlow()

    private val _dailyGoalMinutes = MutableStateFlow(DEFAULT_DAILY_GOAL_MINUTES)
    val dailyGoalMinutes: StateFlow<Int> = _dailyGoalMinutes.asStateFlow()

    init {
        load()
    }

    fun setOwner(ownerId: String) {
        if (ownerId == this.ownerId) return
        this.ownerId = ownerId
        loadPersonal()
    }

    fun setStep(index: Int) {
        val coerced = index.coerceAtLeast(0)
        _step.value = coerced
        try {
            settings.putInt(KEY_STEP, coerced)
        } catch (_: Exception) {
        }
    }

    fun setGoal(goalId: String) {
        _goal.value = goalId
        try {
            settings.putString(key(KEY_GOAL), goalId)
        } catch (_: Exception) {
        }
    }

    fun setDailyGoalMinutes(minutes: Int) {
        val coerced = minutes.coerceIn(5, 180)
        _dailyGoalMinutes.value = coerced
        try {
            settings.putInt(key(KEY_DAILY_GOAL_MINUTES), coerced)
        } catch (_: Exception) {
        }
    }

    fun complete() {
        _seen.value = true
        _step.value = 0
        try {
            settings.putBoolean(KEY_SEEN, true)
            settings.putInt(KEY_STEP, 0)
        } catch (_: Exception) {
        }
    }

    fun resetForDebug() {
        restartForNewUser()
    }

    /**
     * Replays the flow for a freshly created account: a new user always sees
     * onboarding even on a device where it was completed before.
     */
    fun restartForNewUser() {
        _seen.value = false
        _step.value = 0
        try {
            settings.putBoolean(KEY_SEEN, false)
            settings.putInt(KEY_STEP, 0)
        } catch (_: Exception) {
        }
    }

    private fun load() {
        try {
            _seen.value = settings.getBoolean(KEY_SEEN, false)
        } catch (_: Exception) {
            _seen.value = false
        }
        try {
            _step.value = settings.getInt(KEY_STEP, 0).coerceAtLeast(0)
        } catch (_: Exception) {
            _step.value = 0
        }
        try {
            val rawGoal = settings.getString(key(KEY_GOAL), "")
            _goal.value = rawGoal.ifBlank { null }
        } catch (_: Exception) {
            _goal.value = null
        }
        try {
            _dailyGoalMinutes.value =
                settings.getInt(key(KEY_DAILY_GOAL_MINUTES), DEFAULT_DAILY_GOAL_MINUTES).coerceIn(5, 180)
        } catch (_: Exception) {
            _dailyGoalMinutes.value = DEFAULT_DAILY_GOAL_MINUTES
        }
    }

    private fun loadPersonal() {
        try {
            val rawGoal = settings.getString(key(KEY_GOAL), "")
            _goal.value = rawGoal.ifBlank { null }
        } catch (_: Exception) {
            _goal.value = null
        }
        try {
            _dailyGoalMinutes.value =
                settings.getInt(key(KEY_DAILY_GOAL_MINUTES), DEFAULT_DAILY_GOAL_MINUTES).coerceIn(5, 180)
        } catch (_: Exception) {
            _dailyGoalMinutes.value = DEFAULT_DAILY_GOAL_MINUTES
        }
    }
}
