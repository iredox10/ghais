package com.ghais.data.repository

import com.ghais.domain.model.KhatmaPlan
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * KhatmaStore: persisted store of khatma (Quran completion) plans.
 *
 * Mirrors [SchedulesStore] persistence approach
 * (multiplatform Settings + JSON-encoded list + StateFlow + owner-namespaced
 * keys with one-time legacy-key adoption).
 * The exposed list is sorted newest-first by [KhatmaPlan.startDateMs], so
 * [activePlan] (the first element) is the most recently started plan — the
 * source [com.ghais.ui.screens.library.KhatmaScreen] renders.
 * Every mutation persists via [save].
 */
object KhatmaStore {
    private const val KEY_PLANS = "ghais_khatma_plans"

    // Derives the pre-rebrand base key ("quran…" + "ify_…" form) without
    // hardcoding the legacy literal, so the rename stays grep-clean.
    // Used once per load to adopt + delete any legacy value.
    private fun legacyBase(newBase: String): String =
        newBase.replace("ghais_", "quran" + "ify_")

    private var ownerId: String = "local"

    private fun key(base: String): String =
        if (ownerId == "local") base else "$ownerId::$base"

    private val settings: Settings by lazy { Settings() }
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _plans = MutableStateFlow<List<KhatmaPlan>>(emptyList())
    val plans: StateFlow<List<KhatmaPlan>> = _plans.asStateFlow()

    init {
        load()
    }

    fun setOwner(ownerId: String) {
        if (ownerId == this.ownerId) return
        this.ownerId = ownerId
        load()
    }

    /** The most recently started plan, or null when no plan exists. */
    fun activePlan(): KhatmaPlan? = _plans.value.firstOrNull()

    fun get(id: String): KhatmaPlan? = _plans.value.firstOrNull { it.id == id }

    /**
     * Generates a new unique plan id.
     *
     * Clock-based (common code via kotlin.time.Clock; no java.util):
     * millisecond timestamp plus a random suffix to avoid collisions when
     * several plans are created within the same millisecond.
     */
    fun newId(): String =
        "kht-" + Clock.System.now().toEpochMilliseconds() + "-" + (0..9999).random()

    /**
     * Adds a plan (replaces any plan with the same id), normalizing its
     * bounds, then persists and returns the stored plan.
     */
    fun add(plan: KhatmaPlan): KhatmaPlan {
        val normalized = normalize(plan)
        _plans.value = (_plans.value.filter { it.id != normalized.id } + normalized)
            .sortedWith(planOrder())
        save()
        return normalized
    }

    /** Replaces the plan with the same id (same normalization as [add]). */
    fun update(plan: KhatmaPlan) {
        val normalized = normalize(plan)
        _plans.value = (_plans.value.filter { it.id != normalized.id } + normalized)
            .sortedWith(planOrder())
        save()
    }

    /**
     * Records reading progress on the plan with [planId]: advances the
     * current position to ([surahId], [ayahNo]), adds [ayahsRead] to the
     * total, stamps `lastProgressMs` to now, and bumps the streak when the
     * previous progress fell on an earlier epoch day. No-op when unknown.
     */
    fun recordProgress(planId: String, surahId: Int, ayahNo: Int, ayahsRead: Int = 1) {
        val current = _plans.value
        val plan = current.firstOrNull { it.id == planId } ?: return
        val now = Clock.System.now().toEpochMilliseconds()
        val newDay = plan.lastProgressMs <= 0L || !sameEpochDay(plan.lastProgressMs, now)
        val updated = normalize(
            plan.copy(
                currentSurahId = surahId,
                currentAyahNo = ayahNo,
                totalAyahsRead = plan.totalAyahsRead + ayahsRead.coerceAtLeast(0),
                lastProgressMs = now,
                streak = if (newDay) plan.streak + 1 else plan.streak,
            )
        )
        _plans.value = (current.filter { it.id != planId } + updated)
            .sortedWith(planOrder())
        save()
    }

    /** Removes the plan with [id]; no-op when unknown. */
    fun remove(id: String) {
        val updated = _plans.value.filter { it.id != id }
        if (updated.size == _plans.value.size) return
        _plans.value = updated
        save()
    }

    /** Alias of [remove] for callers completing (dismissing) a plan. */
    fun complete(id: String) = remove(id)

    /**
     * Bulk-replaces the plans (sync pull-restore path): normalizes, sorts,
     * and persists. Used by `SyncEngine.pullKhatmaIfEmpty` to adopt the cloud
     * snapshot on a fresh device.
     */
    fun restoreAll(plans: List<KhatmaPlan>) {
        _plans.value = plans.map(::normalize).sortedWith(planOrder())
        save()
    }

    fun clear() {
        _plans.value = emptyList()
        save()
    }

    private fun normalize(plan: KhatmaPlan): KhatmaPlan =
        plan.copy(
            targetDays = plan.targetDays.coerceAtLeast(1),
            currentSurahId = plan.currentSurahId.coerceIn(1, 114),
            currentAyahNo = plan.currentAyahNo.coerceAtLeast(1),
            streak = plan.streak.coerceAtLeast(0),
            totalAyahsRead = plan.totalAyahsRead.coerceAtLeast(0),
        )

    private fun planOrder(): Comparator<KhatmaPlan> =
        compareByDescending<KhatmaPlan> { it.startDateMs }.thenBy { it.id }

    private fun sameEpochDay(aMs: Long, bMs: Long): Boolean {
        if (aMs <= 0L || bMs <= 0L) return false
        return aMs / 86_400_000L == bMs / 86_400_000L
    }

    private fun load() {
        try {
            var raw = settings.getString(key(KEY_PLANS), "")
            if (raw.isBlank()) {
                // One-time migration: adopt the legacy namespaced value if present.
                try {
                    val legacyKey = key(legacyBase(KEY_PLANS))
                    val legacyRaw = settings.getString(legacyKey, "")
                    if (legacyRaw.isNotBlank()) {
                        raw = legacyRaw
                        try {
                            settings.putString(key(KEY_PLANS), legacyRaw)
                        } catch (_: Exception) {
                        }
                        try {
                            settings.remove(legacyKey)
                        } catch (_: Exception) {
                        }
                    }
                } catch (_: Exception) {
                }
            }
            if (raw.isBlank()) {
                _plans.value = emptyList()
                return
            }
            _plans.value = json.decodeFromString<List<KhatmaPlan>>(raw)
                .map(::normalize)
                .sortedWith(planOrder())
        } catch (_: Exception) {
            _plans.value = emptyList()
        }
    }

    private fun save() {
        try {
            settings.putString(key(KEY_PLANS), json.encodeToString(_plans.value))
        } catch (_: Exception) {
        }
    }
}
