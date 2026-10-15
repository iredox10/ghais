package com.ghais.ui.screens.reciters.taste

import com.russhwolf.settings.Settings
import com.ghais.analytics.HookAnalytics
import com.ghais.analytics.HookEvents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * TasteStore: persisted reciter-taste onboarding profile (AGENT A1).
 *
 * Mirrors [com.ghais.data.repository.FollowStore] / `OnboardingStore`
 * persistence (multiplatform Settings + primitives/JSON + StateFlow).
 * Load in init; every mutation updates the flow and persists synchronously.
 * Pure common code. Never throws — Settings failures are swallowed.
 *
 * Owner binding mirrors `FollowStore`: every key is namespaced per owner
 * (`"<owner>::<base>"`, bare base for `"local"`), and [setOwner] reloads.
 */
object TasteStore {
    private const val KEY_PACE = "ghais_taste_pace"
    private const val KEY_MOOD = "ghais_taste_mood"
    private const val KEY_LIKED = "ghais_taste_liked"
    private const val KEY_SKIPPED = "ghais_taste_skipped"
    private const val KEY_COMPLETED = "ghais_taste_completed"

    private const val LOCAL_OWNER = "local"

    private var ownerId: String = LOCAL_OWNER

    private fun key(base: String): String =
        if (ownerId == LOCAL_OWNER) base else "$ownerId::$base"

    private val settings: Settings by lazy { Settings() }
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _taste = MutableStateFlow(TasteProfile())
    val taste: StateFlow<TasteProfile> = _taste.asStateFlow()

    init {
        load()
    }

    /** Binds the store to [ownerId]'s namespace (`"local"` unbinds) and reloads. */
    fun setOwner(ownerId: String) {
        val normalized = ownerId.ifBlank { LOCAL_OWNER }
        if (normalized == this.ownerId) return
        this.ownerId = normalized
        load()
    }

    fun savePace(pace: Pace?) {
        _taste.update { it.copy(pace = pace) }
        save()
    }

    fun saveMood(mood: Mood?) {
        _taste.update { it.copy(mood = mood) }
        save()
    }

    /**
     * Records one A/B voice-test verdict: [winnerSlug] joins [TasteProfile.likedSlugs],
     * [loserSlug] joins [TasteProfile.skippedSlugs]. Blanks are ignored and a
     * slug can never sit in both sets (a re-pick moves it).
     */
    fun recordVoicePick(winnerSlug: String, loserSlug: String) {
        val winner = winnerSlug.trim()
        val loser = loserSlug.trim()
        if (winner.isEmpty() && loser.isEmpty()) return
        _taste.update { current ->
            var liked = current.likedSlugs
            var skipped = current.skippedSlugs
            if (winner.isNotEmpty()) {
                liked = liked + winner
                skipped = skipped - winner
            }
            if (loser.isNotEmpty() && loser != winner) {
                skipped = skipped + loser
                liked = liked - loser
            }
            current.copy(likedSlugs = liked, skippedSlugs = skipped)
        }
        save()
    }

    /** Marks the quiz finished (step 3 done). This is what the first-visit gate reads. */
    fun markCompleted() {
        _taste.update { it.copy(completed = true) }
        save()
        HookAnalytics.track(
            HookEvents.Event.TASTE_COMPLETED,
            mapOf(HookEvents.Prop.SOURCE to "taste_quiz"),
        )
    }

    /** Full replace (e.g. restoring a quiz answer set in one shot). */
    fun save(profile: TasteProfile) {
        _taste.value = profile
        save()
    }

    /** Clears the profile for the currently bound owner only. */
    fun clear() {
        _taste.value = TasteProfile()
        save()
    }

    private fun load() {
        try {
            val pace = try {
                settings.getString(key(KEY_PACE), "").takeIf { it.isNotBlank() }?.let { Pace.valueOf(it) }
            } catch (_: Exception) {
                null
            }
            val mood = try {
                settings.getString(key(KEY_MOOD), "").takeIf { it.isNotBlank() }?.let { Mood.valueOf(it) }
            } catch (_: Exception) {
                null
            }
            val liked = readStringSet(key(KEY_LIKED))
            val skipped = readStringSet(key(KEY_SKIPPED))
            val completed = try {
                settings.getBoolean(key(KEY_COMPLETED), false)
            } catch (_: Exception) {
                false
            }
            _taste.value = TasteProfile(
                pace = pace,
                mood = mood,
                likedSlugs = liked,
                skippedSlugs = skipped,
                completed = completed
            )
        } catch (_: Exception) {
            _taste.value = TasteProfile()
        }
    }

    private fun save() {
        val current = _taste.value
        try {
            if (current.pace == null) settings.remove(key(KEY_PACE))
            else settings.putString(key(KEY_PACE), current.pace.name)
        } catch (_: Exception) {
        }
        try {
            if (current.mood == null) settings.remove(key(KEY_MOOD))
            else settings.putString(key(KEY_MOOD), current.mood.name)
        } catch (_: Exception) {
        }
        try {
            settings.putString(key(KEY_LIKED), json.encodeToString(current.likedSlugs.toList()))
        } catch (_: Exception) {
        }
        try {
            settings.putString(key(KEY_SKIPPED), json.encodeToString(current.skippedSlugs.toList()))
        } catch (_: Exception) {
        }
        try {
            settings.putBoolean(key(KEY_COMPLETED), current.completed)
        } catch (_: Exception) {
        }
    }

    private fun readStringSet(storageKey: String): Set<String> {
        return try {
            val raw = settings.getString(storageKey, "")
            if (raw.isBlank()) emptySet()
            else json.decodeFromString<List<String>>(raw).map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }
}
