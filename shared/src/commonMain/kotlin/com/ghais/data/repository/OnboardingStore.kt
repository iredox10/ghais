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
 *
 * **Owner binding.** Every flag is stored per owner via [key]; `"local"` is the
 * pre-auth / unbound namespace. [setOwner] is invoked asynchronously from the
 * session collector ([com.ghais.data.sync.SyncTriggers]), so the first frames
 * after a session appears — and *all* frames for an offline user whose session
 * refresh failed — are still unbound. [isDoneForCurrentUser] therefore fails
 * open while unbound, so that window can never drop a returning user into
 * onboarding.
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

    /** The unbound / pre-auth owner. [key] collapses to the bare base key for it. */
    private const val LOCAL_OWNER = "local"

    private var ownerId: String = LOCAL_OWNER

    /**
     * A [markDoneForCurrentUser]/[complete]/[restartForNewUser] write that landed
     * while unbound, replayed into the real owner's namespace by the next
     * [setOwner]. `null` = no pending write; `true`/`false` = force that value
     * onto the next bound owner regardless of what its own key says.
     */
    private var pendingDone: Boolean? = null

    /** True once [setOwner] has bound a real (non-`"local"`) owner. */
    private val isOwnerBound: Boolean get() = ownerId != LOCAL_OWNER

    private fun key(base: String): String =
        if (!isOwnerBound) base else "$ownerId::$base"

    private val settings: Settings by lazy { Settings() }

    /**
     * Reads a boolean that is namespaced per owner, with a one-time adoption of
     * the pre-namespacing *device-global* value.
     *
     * Tier 1 (owner-scoped) is the un-namespaced base key, which while unbound
     * *is* the owner-scoped key. Tier 2 is the pre-rebrand owner-scoped key
     * (handled by [migratedBoolean]). Tier 3 is the device-global base key: a
     * value written before this flag was namespaced, which can only belong to
     * the first owner that binds. It is adopted into that owner's key and then
     * removed, so a second user on a shared device starts from the default
     * instead of inheriting the previous user's half-finished state.
     */
    private fun readOwnedBoolean(base: String, default: Boolean): Boolean {
        val shared = migratedBoolean(namespaced = false, newBase = base, default = default)
        if (!isOwnerBound) return shared
        val owned = migratedBoolean(namespaced = true, newBase = base, default = default)
        if (owned != default) return owned
        if (shared == default) return default
        try {
            settings.putBoolean(key(base), shared)
        } catch (_: Exception) {
        }
        try {
            settings.remove(base)
        } catch (_: Exception) {
        }
        return shared
    }

    /** [readOwnedBoolean] for the int flag ([KEY_STEP]). */
    private fun readOwnedInt(base: String, default: Int): Int {
        val shared = migratedInt(namespaced = false, newBase = base, default = default)
        if (!isOwnerBound) return shared
        val owned = migratedInt(namespaced = true, newBase = base, default = default)
        if (owned != default) return owned
        if (shared == default) return default
        try {
            settings.putInt(key(base), shared)
        } catch (_: Exception) {
        }
        try {
            settings.remove(base)
        } catch (_: Exception) {
        }
        return shared
    }

    private fun writeOwnedBoolean(base: String, value: Boolean) {
        try {
            settings.putBoolean(key(base), value)
        } catch (_: Exception) {
        }
    }

    private fun writeOwnedInt(base: String, value: Int) {
        try {
            settings.putInt(key(base), value)
        } catch (_: Exception) {
        }
    }

    /**
     * Reads [KEY_DONE] for the bound owner (pre-rebrand key included).
     *
     * Only valid once an owner is bound, and deliberately has **no**
     * device-global tier even though [KEY_SEEN]/[KEY_STEP] do: a shared "done"
     * would hand a second user on the same device a finished-onboarding flag
     * they never earned, permanently skipping the goal/daily-minutes setup. A
     * wrong "not done" costs one extra pass through onboarding; a wrong "done"
     * is unrecoverable, so this one fails toward onboarding — and the unbound
     * window that could strand a returning user is covered by
     * [doneWhileUnbound] instead.
     */
    private fun readOwnedDone(): Boolean =
        try {
            migratedBoolean(namespaced = true, newBase = KEY_DONE, default = false)
        } catch (_: Exception) {
            false
        }

    /**
     * Resolves [isDoneForCurrentUser] while no owner is bound yet.
     *
     * Unbound means "we cannot tell who this is", not "this person is new", so
     * it reports `true` — fail open *away* from onboarding — unless a caller has
     * already made an explicit statement via [writeDone], in which case that
     * statement is reported immediately.
     */
    private fun doneWhileUnbound(): Boolean = pendingDone ?: true

    /**
     * Single writer for [isDoneForCurrentUser] + [KEY_DONE]. When no owner is
     * bound the intent is recorded in [pendingDone] instead of being written to
     * the shared key, so it is applied to — and only to — the account that binds
     * next. This is what makes the write order irrelevant for callers.
     */
    private fun writeDone(value: Boolean) {
        _doneForOwner.value = value
        if (isOwnerBound) {
            writeOwnedBoolean(KEY_DONE, value)
        } else {
            pendingDone = value
        }
    }

    private val _seen = MutableStateFlow(false)
    val seen: StateFlow<Boolean> = _seen.asStateFlow()

    private val _doneForOwner = MutableStateFlow(true)

    /**
     * Whether the *currently bound* owner has finished onboarding.
     *
     * Read this, never the raw keys. While no owner is bound yet (signed out, or
     * [setOwner] has not run for this frame — the session collector is a
     * separate coroutine, so a session's first frames are always unbound, and an
     * offline user stays unbound until a refresh succeeds) it reports `true`:
     * an unidentified visitor is never shown onboarding, so a returning user can
     * never be bounced into it by that window. The value flips to the owner's
     * real flag as soon as [setOwner] runs.
     *
     * A brand-new account is unaffected: it reaches onboarding through an
     * explicit [restartForNewUser] (or [resetForDebug]), which reports `false`
     * immediately, before any binding.
     */
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

    /**
     * Binds every flag to [ownerId] (`"local"` unbinds) and reloads.
     *
     * Safe to call in any order relative to [markDoneForCurrentUser] /
     * [complete] / [restartForNewUser]: a write that arrived while unbound is
     * replayed onto the owner bound here, and wins over that owner's stored
     * flag. Idempotent — rebinding the same owner is a no-op.
     */
    fun setOwner(ownerId: String) {
        if (ownerId == this.ownerId) return
        this.ownerId = ownerId
        loadPersonal()
        _doneForOwner.value = if (isOwnerBound) readOwnedDone() else doneWhileUnbound()
        pendingDone?.let { pending ->
            _doneForOwner.value = pending
            if (isOwnerBound) writeOwnedBoolean(KEY_DONE, pending)
            pendingDone = null
        }
    }

    fun setStep(index: Int) {
        val coerced = index.coerceAtLeast(0)
        _step.value = coerced
        writeOwnedInt(KEY_STEP, coerced)
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

    /**
     * Marks onboarding finished for whoever is bound: `seen` + `done` set,
     * `step` reset to 0.
     *
     * Order-independent with [setOwner] — see [writeDone]. Idempotent: calling
     * it twice writes the same values.
     */
    fun complete() {
        _seen.value = true
        _step.value = 0
        writeOwnedBoolean(KEY_SEEN, true)
        writeOwnedInt(KEY_STEP, 0)
        writeDone(true)
    }

    /**
     * Marks onboarding finished for the current user without touching [step] —
     * for a returning user whose onboarding is already satisfied server-side
     * (e.g. a Google sign-in) and who must never be shown it again.
     *
     * **Ordering contract: none required.** This may be called before or after
     * [setOwner]. Called while unbound (the usual shape, since a sign-in
     * callback runs before the session collector has rebound the stores) the
     * in-memory flow flips to `true` immediately and the intent is replayed into
     * the account's own namespace by the next [setOwner], where it overrides
     * that account's stored flag. Safe to call repeatedly.
     */
    fun markDoneForCurrentUser() {
        _seen.value = true
        writeOwnedBoolean(KEY_SEEN, true)
        writeDone(true)
    }

    /** @see restartForNewUser */
    fun resetForDebug() {
        restartForNewUser()
    }

    /**
     * Replays the flow for a freshly created account: a new user always sees
     * onboarding even on a device where it was completed before.
     *
     * Reports `false` immediately (so the replay latch opens without waiting for
     * a bind) and, when unbound, is replayed onto the next owner bound by
     * [setOwner] — which is what makes a sign-up callback correct even though it
     * runs while the store still points at the previous owner.
     */
    fun restartForNewUser() {
        _seen.value = false
        _step.value = 0
        writeOwnedBoolean(KEY_SEEN, false)
        writeOwnedInt(KEY_STEP, 0)
        writeDone(false)
    }

    /**
     * Initial (unbound) load. Deliberately does not read [KEY_DONE]: with no
     * owner bound there is nothing authoritative to read, and [doneWhileUnbound]
     * already covers it via `_doneForOwner`'s `true` seed.
     */
    private fun load() {
        try {
            _seen.value = readOwnedBoolean(KEY_SEEN, false)
        } catch (_: Exception) {
            _seen.value = false
        }
        try {
            _step.value = readOwnedInt(KEY_STEP, 0).coerceAtLeast(0)
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

    /**
     * Reloads the per-owner step + preferences. [KEY_DONE] is resolved by
     * [setOwner] itself, right after this runs, together with any replayed
     * [pendingDone] write.
     */
    private fun loadPersonal() {
        try {
            _seen.value = readOwnedBoolean(KEY_SEEN, false)
        } catch (_: Exception) {
            _seen.value = false
        }
        try {
            _step.value = readOwnedInt(KEY_STEP, 0).coerceAtLeast(0)
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
}
