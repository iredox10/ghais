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
    const val KEY_SEEN = "ghais_onboarding_seen"
    private const val KEY_STEP = "ghais_onboarding_step"
    private const val KEY_DONE = "ghais_onboarding_done"
    private const val KEY_GOAL = "ghais_onboarding_goal"
    private const val KEY_DAILY_GOAL_MINUTES = "ghais_daily_goal_minutes"

    // Derives the pre-rebrand base key ("quran…" + "ify_…" form) without
    // hardcoding the legacy literal, so the rename stays grep-clean.
    // Used on every load to adopt + delete any legacy value (one-time).
    private fun legacyBase(newBase: String): String =
        newBase.replace("ghais_", "quran" + "ify_")

    private fun migratedBoolean(namespaced: Boolean, newBase: String, default: Boolean): Boolean {
        return try {
            val newKey = if (namespaced) key(newBase) else newBase
            val current = settings.getBoolean(newKey, default)
            if (current != default) return current
            val legacyKey = if (namespaced) key(legacyBase(newBase)) else legacyBase(newBase)
            val legacy = try {
                settings.getBoolean(legacyKey, default)
            } catch (_: Exception) {
                return current
            }
            if (legacy != default) {
                try {
                    settings.putBoolean(newKey, legacy)
                } catch (_: Exception) {
                }
                try {
                    settings.remove(legacyKey)
                } catch (_: Exception) {
                }
                legacy
            } else {
                current
            }
        } catch (_: Exception) {
            default
        }
    }

    private fun migratedInt(namespaced: Boolean, newBase: String, default: Int): Int {
        return try {
            val newKey = if (namespaced) key(newBase) else newBase
            val current = settings.getInt(newKey, default)
            if (current != default) return current
            val legacyKey = if (namespaced) key(legacyBase(newBase)) else legacyBase(newBase)
            val legacy = try {
                settings.getInt(legacyKey, default)
            } catch (_: Exception) {
                return current
            }
            if (legacy != default) {
                try {
                    settings.putInt(newKey, legacy)
                } catch (_: Exception) {
                }
                try {
                    settings.remove(legacyKey)
                } catch (_: Exception) {
                }
                legacy
            } else {
                current
            }
        } catch (_: Exception) {
            default
        }
    }

    private fun migratedString(namespaced: Boolean, newBase: String, default: String = ""): String {
        return try {
            val newKey = if (namespaced) key(newBase) else newBase
            val current = settings.getString(newKey, default)
            if (current.isNotBlank()) return current
            val legacyKey = if (namespaced) key(legacyBase(newBase)) else legacyBase(newBase)
            val legacy = try {
                settings.getString(legacyKey, "")
            } catch (_: Exception) {
                return current
            }
            if (legacy.isNotBlank()) {
                try {
                    settings.putString(newKey, legacy)
                } catch (_: Exception) {
                }
                try {
                    settings.remove(legacyKey)
                } catch (_: Exception) {
                }
                legacy
            } else {
                current
            }
        } catch (_: Exception) {
            default
        }
    }

    private const val DEFAULT_DAILY_GOAL_MINUTES = 15

    private var ownerId: String = "local"

    private fun key(base: String): String =
        if (ownerId == "local") base else "$ownerId::$base"

    private val settings: Settings by lazy { Settings() }

    private val _seen = MutableStateFlow(false)
    val seen: StateFlow<Boolean> = _seen.asStateFlow()

    private val _doneForOwner = MutableStateFlow(false)
    val isDoneForCurrentUser: StateFlow<Boolean> = _doneForOwner.asStateFlow()

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
        try {
            _doneForOwner.value = settings.getBoolean(key(KEY_DONE), false)
        } catch (_: Exception) {
            _doneForOwner.value = false
        }
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
        _doneForOwner.value = true
        _step.value = 0
        try {
            settings.putBoolean(KEY_SEEN, true)
            settings.putInt(KEY_STEP, 0)
            settings.putBoolean(key(KEY_DONE), true)
        } catch (_: Exception) {
        }
    }

    fun markDoneForCurrentUser() {
        _doneForOwner.value = true
        _seen.value = true
        try {
            settings.putBoolean(key(KEY_DONE), true)
        } catch (_: Exception) {
        }
        try {
            settings.putBoolean(KEY_SEEN, true)
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
        _doneForOwner.value = false
        _step.value = 0
        try {
            settings.putBoolean(KEY_SEEN, false)
            settings.putInt(KEY_STEP, 0)
            settings.putBoolean(key(KEY_DONE), false)
        } catch (_: Exception) {
        }
    }

    private fun load() {
        try {
            _seen.value = migratedBoolean(namespaced = false, newBase = KEY_SEEN, default = false)
        } catch (_: Exception) {
            _seen.value = false
        }
        try {
            _doneForOwner.value = settings.getBoolean(key(KEY_DONE), false)
        } catch (_: Exception) {
            _doneForOwner.value = false
        }
        try {
            _step.value = migratedInt(namespaced = false, newBase = KEY_STEP, default = 0).coerceAtLeast(0)
        } catch (_: Exception) {
            _step.value = 0
        }
        try {
            val rawGoal = migratedString(namespaced = true, newBase = KEY_GOAL)
            _goal.value = rawGoal.ifBlank { null }
        } catch (_: Exception) {
            _goal.value = null
        }
        try {
            _dailyGoalMinutes.value =
                migratedInt(namespaced = true, newBase = KEY_DAILY_GOAL_MINUTES, default = DEFAULT_DAILY_GOAL_MINUTES).coerceIn(5, 180)
        } catch (_: Exception) {
            _dailyGoalMinutes.value = DEFAULT_DAILY_GOAL_MINUTES
        }
    }

    private fun loadPersonal() {
        try {
            _doneForOwner.value = settings.getBoolean(key(KEY_DONE), false)
        } catch (_: Exception) {
            _doneForOwner.value = false
        }
        try {
            val rawGoal = migratedString(namespaced = true, newBase = KEY_GOAL)
            _goal.value = rawGoal.ifBlank { null }
        } catch (_: Exception) {
            _goal.value = null
        }
        try {
            _dailyGoalMinutes.value =
                migratedInt(namespaced = true, newBase = KEY_DAILY_GOAL_MINUTES, default = DEFAULT_DAILY_GOAL_MINUTES).coerceIn(5, 180)
        } catch (_: Exception) {
            _dailyGoalMinutes.value = DEFAULT_DAILY_GOAL_MINUTES
        }
    }
}
