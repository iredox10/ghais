package com.ghais.personal

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * SignalStore: persisted append-only log of per-reciter implicit-feedback
 * signals ([ReciterSignal]), the raw material ranking agents score via
 * [scoreSignals].
 *
 * Conventions mirror [com.ghais.data.repository.FollowStore] (multiplatform
 * Settings + JSON-encoded list, owner-namespaced keys, every I/O call in
 * try/catch, silent no-op on failure):
 * - keys are owner-namespaced (`<ownerId>::ghais_reciter_signals_v1` when
 *   bound, bare while signed out as `"local"`); [setOwner] rebinds and
 *   reloads, keeping each account's signals separate.
 * - append-only, capped at [MAX_SIGNALS] (newest 500 win; oldest pruned on
 *   write, load, and import).
 * - blank slugs are dropped on every write path.
 *
 * ## Test seam
 * [settingsOverride] is the injection hook the sibling stores lack: set it to
 * an in-memory `Settings` in tests to assert persistence roundtrips, or leave
 * it null (default) for the real `Settings()` backend. On the unit-test
 * classpath the no-arg `Settings()` factory throws, so with no override every
 * load/save is a silent no-op and only in-memory [signals] transitions are
 * observable — same limitation as `FollowStoreTest`/`OnboardingStoreTest`.
 */
object SignalStore {
    /** Hard cap on stored signals; newest win, oldest pruned. */
    const val MAX_SIGNALS = 500

    private const val KEY_SIGNALS = "ghais_reciter_signals_v1"

    /** The unbound / pre-auth owner. Keys collapse to the bare base key for it. */
    const val LOCAL_OWNER = "local"

    /**
     * Injectable Settings backend. Null (default) = the real no-arg
     * `Settings()`. Tests may assign an in-memory instance; assign null again
     * to restore the default. Read on every I/O call, so it can be swapped at
     * any time; never throws.
     */
    var settingsOverride: Settings? = null

    private val defaultSettings: Settings? by lazy {
        try {
            Settings()
        } catch (_: Exception) {
            null
        }
    }

    private fun settingsOrNull(): Settings? = try {
        settingsOverride ?: defaultSettings
    } catch (_: Exception) {
        null
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private var ownerId: String = LOCAL_OWNER

    private fun key(base: String): String =
        if (ownerId == LOCAL_OWNER) base else "$ownerId::$base"

    private val _signals = MutableStateFlow<List<ReciterSignal>>(emptyList())

    /** Live log, oldest-first (append order), capped at [MAX_SIGNALS]. */
    val signals: StateFlow<List<ReciterSignal>> = _signals.asStateFlow()

    init {
        load()
    }

    /**
     * Rebinds the store to [ownerId]'s namespace (`"local"`/blank unbinds) and
     * reloads that owner's log. Each account sees only its own signals; the
     * previous namespace's in-memory log is discarded (its persisted copy
     * stays under its own key). Never throws.
     */
    fun setOwner(ownerId: String) {
        try {
            val normalized = ownerId.ifBlank { LOCAL_OWNER }
            if (normalized == this.ownerId) return
            this.ownerId = normalized
            load()
        } catch (_: Exception) {
        }
    }

    /** Today's epoch day (`epochMs / 86_400_000`), the clock half of [record]. */
    fun currentEpochDay(): Long = try {
        Clock.System.now().toEpochMilliseconds() / 86_400_000L
    } catch (_: Exception) {
        0L
    }

    /**
     * Appends one [kind] signal for [slug]. Blank slugs are dropped;
     * persistence failures are silent (the in-memory log still updates).
     * Never throws.
     */
    fun record(kind: SignalKind, slug: String, epochDay: Long = currentEpochDay()) {
        try {
            val clean = slug.trim()
            if (clean.isEmpty()) return
            val day = epochDay.coerceAtLeast(0L)
            _signals.value = (_signals.value + ReciterSignal(clean, kind, day)).takeLast(MAX_SIGNALS)
            save()
        } catch (_: Exception) {
        }
    }

    /** Convenience helpers — one per [SignalKind], all delegating to [record]. */
    fun recordPlayStarted(slug: String, epochDay: Long = currentEpochDay()) =
        record(SignalKind.PLAY_STARTED, slug, epochDay)

    fun recordPlayCompleted(slug: String, epochDay: Long = currentEpochDay()) =
        record(SignalKind.PLAY_COMPLETED, slug, epochDay)

    fun recordPreview(slug: String, epochDay: Long = currentEpochDay()) =
        record(SignalKind.PREVIEW, slug, epochDay)

    fun recordFollow(slug: String, epochDay: Long = currentEpochDay()) =
        record(SignalKind.FOLLOW, slug, epochDay)

    fun recordSearchOpen(slug: String, epochDay: Long = currentEpochDay()) =
        record(SignalKind.SEARCH_OPEN, slug, epochDay)

    fun recordDownload(slug: String, epochDay: Long = currentEpochDay()) =
        record(SignalKind.DOWNLOAD, slug, epochDay)

    fun recordSkipQuick(slug: String, epochDay: Long = currentEpochDay()) =
        record(SignalKind.SKIP_QUICK, slug, epochDay)

    fun recordUnfollow(slug: String, epochDay: Long = currentEpochDay()) =
        record(SignalKind.UNFOLLOW, slug, epochDay)

    /** Empties the bound owner's log (memory + disk). Never throws. */
    fun clear() {
        try {
            _signals.value = emptyList()
            save()
        } catch (_: Exception) {
            try {
                _signals.value = emptyList()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Scores the current log via [scoreSignals]. Pure read off [signals];
     * never throws (empty map on failure).
     */
    fun scores(
        todayDay: Long = currentEpochDay(),
        halfLifeDays: Int = DEFAULT_HALF_LIFE_DAYS,
    ): Map<String, Float> = try {
        scoreSignals(_signals.value, todayDay, halfLifeDays)
    } catch (_: Exception) {
        emptyMap()
    }

    /**
     * The bound owner's log for a push (copied — callers cannot mutate the
     * live list). Oldest-first, capped at [MAX_SIGNALS].
     */
    fun exportSnapshot(): List<ReciterSignal> = try {
        _signals.value.toList()
    } catch (_: Exception) {
        emptyList()
    }

    /**
     * Replaces the bound owner's log with [snapshot] — the pull direction.
     * Authoritative (not a merge): entries missing from [snapshot] are
     * dropped. Normalized like [load] (blank slugs dropped, oldest pruned
     * past the cap). Never throws.
     */
    fun importSnapshot(snapshot: List<ReciterSignal>) {
        try {
            _signals.value = snapshot
                .map { it.copy(slug = it.slug.trim(), epochDay = it.epochDay.coerceAtLeast(0L)) }
                .filter { it.slug.isNotEmpty() }
                .takeLast(MAX_SIGNALS)
            save()
        } catch (_: Exception) {
        }
    }

    /**
     * Additive merge for cold-start adoption (see `ForYou.adoptPreAuthSignals`):
     * appends [extra] to the live log instead of replacing it. Normalized
     * like [importSnapshot], capped at [MAX_SIGNALS] (newest win).
     *
     * @return adopted (non-blank) count. Never throws.
     */
    fun mergeSnapshot(extra: List<ReciterSignal>): Int = try {
        if (extra.isEmpty()) return 0
        val fresh = extra
            .map { it.copy(slug = it.slug.trim(), epochDay = it.epochDay.coerceAtLeast(0L)) }
            .filter { it.slug.isNotEmpty() }
        if (fresh.isEmpty()) return 0
        _signals.value = (_signals.value + fresh).takeLast(MAX_SIGNALS)
        save()
        fresh.size
    } catch (_: Exception) {
        0
    }

    private fun load() {
        try {
            val settings = settingsOrNull()
            if (settings == null) {
                _signals.value = emptyList()
                return
            }
            val raw = try {
                settings.getString(key(KEY_SIGNALS), "")
            } catch (_: Exception) {
                ""
            }
            if (raw.isBlank()) {
                _signals.value = emptyList()
                return
            }
            val decoded = try {
                json.decodeFromString<List<ReciterSignal>>(raw)
            } catch (_: Exception) {
                _signals.value = emptyList()
                return
            }
            _signals.value = decoded
                .map { it.copy(slug = it.slug.trim(), epochDay = it.epochDay.coerceAtLeast(0L)) }
                .filter { it.slug.isNotEmpty() }
                .takeLast(MAX_SIGNALS)
        } catch (_: Exception) {
            try {
                _signals.value = emptyList()
            } catch (_: Exception) {
            }
        }
    }

    private fun save() {
        try {
            val settings = settingsOrNull() ?: return
            try {
                settings.putString(key(KEY_SIGNALS), json.encodeToString(_signals.value))
            } catch (_: Exception) {
            }
        } catch (_: Exception) {
        }
    }
}
