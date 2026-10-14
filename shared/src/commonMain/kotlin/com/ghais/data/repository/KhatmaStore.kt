package com.ghais.data.repository

import com.ghais.data.seed.PageJuzIndex
import com.ghais.data.seed.QuranData
import com.ghais.domain.model.KhatmaPlan
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.ceil
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

    /**
     * Convenience creator for the khatma create-plan flow (name + start
     * surah:ayah + target days): mints an id, stamps start to now, and
     * persists via [add] (same normalization/ordering). The start ayah is
     * clamped to the surah's ayah count so the cursor is always playable.
     */
    fun create(
        title: String,
        startSurahId: Int = 1,
        startAyahNo: Int = 1,
        targetDays: Int = 30,
    ): KhatmaPlan {
        val now = Clock.System.now().toEpochMilliseconds()
        val surah = startSurahId.coerceIn(1, 114)
        val ayah = startAyahNo.coerceIn(1, khatmaSurahAyahCount(surah).coerceAtLeast(1))
        return add(
            KhatmaPlan(
                id = newId(),
                title = title.ifBlank { "My Khatma" },
                targetDays = targetDays.coerceAtLeast(1),
                currentSurahId = surah,
                currentAyahNo = ayah,
                streak = 0,
                startDateMs = now,
                lastProgressMs = 0L,
                totalAyahsRead = 0,
            )
        )
    }

    /**
     * [create] variant taking an absolute target date: targetDays is the
     * ceiling of whole days from [nowMs] to [targetDateMs] (min 1, so a
     * past date collapses to a single-day plan — see the past-target
     * clamping in [khatmaTodayPortion]).
     */
    fun createForTargetDate(
        title: String,
        startSurahId: Int = 1,
        startAyahNo: Int = 1,
        targetDateMs: Long,
        nowMs: Long = Clock.System.now().toEpochMilliseconds(),
    ): KhatmaPlan {
        val days = ((targetDateMs - nowMs + KHATMA_DAY_MS - 1) / KHATMA_DAY_MS).toInt()
        return create(title, startSurahId, startAyahNo, days.coerceAtLeast(1))
    }

    /**
     * Records a finished daily portion: advances the cursor to the portion
     * end and adds its ayah count. Thin wrapper over [recordProgress] so
     * the screen has a single call site for portion completion.
     */
    fun recordPortionDone(planId: String, portion: KhatmaPortion) {
        recordProgress(planId, portion.endSurahId, portion.endAyahNo, portion.ayahCount)
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

// ---------------------------------------------------------------------------
// Pure khatma math (no store access; unit-testable without the Settings
// singleton). The daily portion is an AYAH-COUNT share, not a page slice:
//
// PageJuzIndex has no reverse lookup (page -> ayah range; pageOf is ayah ->
// page only), so "pages/day -> ayahs" cannot be resolved exactly. Instead a
// plan's remaining ayahs are split evenly across its remaining days
// (remainder to the earliest days), and page counts (via
// PageJuzIndex.pageCount) are used only for the pages/day preview and the
// ETA date. Playback follows the same granularity: full-surah MP3s, so a
// portion spanning N surahs enqueues those N surah tracks from the plan's
// reciter (mirroring ReciterProfileScreen's allTracks shape).
// ---------------------------------------------------------------------------

/** Length of an epoch day; single source for all khatma date math. */
const val KHATMA_DAY_MS = 86_400_000L

/** Total ayahs in [QuranData.SURAHS] (6236 for the bundled seed). */
fun khatmaTotalAyahs(): Int = QuranData.SURAHS.sumOf { it.ayahsCount }

/** Ayah count of [surahId]; 0 when out of bounds. */
fun khatmaSurahAyahCount(surahId: Int): Int =
    if (surahId in 1..114 && surahId <= QuranData.SURAHS.size) {
        QuranData.SURAHS[surahId - 1].ayahsCount
    } else {
        0
    }

/** 0-based canonical ayah index, or -1 when out of bounds. */
fun khatmaGlobalIndex(surahId: Int, ayahNo: Int): Int {
    val count = khatmaSurahAyahCount(surahId)
    if (count == 0 || ayahNo !in 1..count) return -1
    var idx = 0
    for (s in 1 until surahId) idx += khatmaSurahAyahCount(s)
    return idx + (ayahNo - 1)
}

/** (surahId, ayahNo) at 0-based canonical [index], or null when out of bounds. */
fun khatmaAyahAtGlobal(index: Int): Pair<Int, Int>? {
    if (index < 0 || index >= khatmaTotalAyahs()) return null
    var rest = index
    for (surah in QuranData.SURAHS) {
        if (rest < surah.ayahsCount) return surah.id to (rest + 1)
        rest -= surah.ayahsCount
    }
    return null
}

/**
 * Ayah count in the closed range [fromSurah]:[fromAyah]..[toSurah]:[toAyah].
 * Returns 0 when either endpoint is out of bounds or the range is
 * empty/reversed (same contract as [PageJuzIndex.pageCount]).
 */
fun khatmaAyahCountBetween(fromSurah: Int, fromAyah: Int, toSurah: Int, toAyah: Int): Int {
    val a = khatmaGlobalIndex(fromSurah, fromAyah)
    val b = khatmaGlobalIndex(toSurah, toAyah)
    if (a < 0 || b < 0 || b < a) return 0
    return b - a + 1
}

/** Ayah after ([surahId],[ayahNo]); null at the end of the Quran or on bad input. */
fun khatmaNextAyah(surahId: Int, ayahNo: Int): Pair<Int, Int>? {
    val count = khatmaSurahAyahCount(surahId)
    if (count == 0 || ayahNo !in 1..count) return null
    if (ayahNo < count) return surahId to (ayahNo + 1)
    if (surahId < 114) return (surahId + 1) to 1
    return null
}

/** Retreat [steps] ayahs backwards from ([surahId],[ayahNo]), clamped at 1:1. */
fun khatmaRetreatOrFirst(surahId: Int, ayahNo: Int, steps: Int): Pair<Int, Int> {
    val idx = khatmaGlobalIndex(surahId, ayahNo)
    if (idx < 0) return 1 to 1
    val rewound = (idx - steps.coerceAtLeast(0)).coerceAtLeast(0)
    return khatmaAyahAtGlobal(rewound) ?: (1 to 1)
}

/** End of a [span]-ayah portion starting at ([fromSurah],[fromAyah]); null on bad input. */
fun khatmaAdvanceOrNull(fromSurah: Int, fromAyah: Int, span: Int): Pair<Int, Int>? {
    if (span <= 0) return null
    val idx = khatmaGlobalIndex(fromSurah, fromAyah)
    if (idx < 0) return null
    return khatmaAyahAtGlobal(idx + span - 1)
}

/** Whole days elapsed since the plan start at [nowMs] (floor, min 0). */
fun khatmaElapsedDays(plan: KhatmaPlan, nowMs: Long): Int =
    maxOf(0, ((nowMs - plan.startDateMs) / KHATMA_DAY_MS).toInt())

/** Whole days left in the plan at [nowMs] (min 0; 0 = target reached/passed). */
fun khatmaDaysLeft(plan: KhatmaPlan, nowMs: Long): Int =
    maxOf(0, plan.targetDays.coerceAtLeast(1) - khatmaElapsedDays(plan, nowMs))

/**
 * Next-to-read cursor: the start position when never progressed, else the
 * ayah after the last-read one. Null when the plan is finished or the
 * stored cursor is out of bounds.
 */
fun khatmaCursorOf(plan: KhatmaPlan): Pair<Int, Int>? {
    if (plan.lastProgressMs <= 0L) {
        return if (khatmaGlobalIndex(plan.currentSurahId, plan.currentAyahNo) >= 0) {
            plan.currentSurahId to plan.currentAyahNo
        } else {
            null
        }
    }
    return khatmaNextAyah(plan.currentSurahId, plan.currentAyahNo)
}

/** Ayahs still unread from the cursor to the end of the Quran. */
fun khatmaRemainingAyahs(plan: KhatmaPlan): Int {
    val cursor = khatmaCursorOf(plan) ?: return 0
    val lastCount = khatmaSurahAyahCount(114)
    if (lastCount == 0) return 0
    return khatmaAyahCountBetween(cursor.first, cursor.second, 114, lastCount)
}

/**
 * Plan-aware progress fraction: read ayahs over (read + remaining).
 * The model's `progressPercentage` divides by a hardcoded full-Quran 6236,
 * which overstates progress for plans that start mid-Quran; the screen uses
 * this instead. Returns 1 when nothing remains.
 */
fun khatmaPlanProgress(plan: KhatmaPlan): Float {
    val remaining = khatmaRemainingAyahs(plan)
    val done = plan.totalAyahsRead.coerceAtLeast(0)
    val total = done + remaining
    if (total <= 0) return 1f
    return (done.toFloat() / total.toFloat()).coerceIn(0f, 1f)
}

/** True when the plan's last progress fell on the same epoch day as [nowMs]. */
fun khatmaDoneToday(plan: KhatmaPlan, nowMs: Long): Boolean =
    plan.lastProgressMs > 0L && khatmaIsSameEpochDay(plan.lastProgressMs, nowMs)

/** True when nothing remains to read. */
fun khatmaIsComplete(plan: KhatmaPlan): Boolean = khatmaRemainingAyahs(plan) <= 0

fun khatmaIsSameEpochDay(aMs: Long, bMs: Long): Boolean {
    if (aMs <= 0L || bMs <= 0L) return false
    return aMs / KHATMA_DAY_MS == bMs / KHATMA_DAY_MS
}

/**
 * Even per-day split of [totalAyahs] over [days] days: every day gets
 * `total / days`, and the `total % days` leftover ayahs go one each to the
 * earliest days. A non-positive [days] clamps to a single day (the whole
 * remainder is due at once); zero [totalAyahs] yields no days.
 */
fun khatmaPortionSizes(totalAyahs: Int, days: Int): List<Int> {
    val total = totalAyahs.coerceAtLeast(0)
    if (total == 0) return emptyList()
    val n = if (days <= 0) 1 else days
    val base = total / n
    val rem = total % n
    return List(n) { i -> base + if (i < rem) 1 else 0 }
}

data class KhatmaPortion(
    val startSurahId: Int,
    val startAyahNo: Int,
    val endSurahId: Int,
    val endAyahNo: Int,
    val ayahCount: Int,
)

/**
 * Today's portion for [plan] at [nowMs]: the remaining ayahs split evenly
 * over the remaining days ([khatmaPortionSizes]), with today taking the
 * earliest (largest) share. Past-target clamping: when the target date has
 * passed (0 days left) the whole remainder is due today. Null when the plan
 * is complete.
 */
fun khatmaTodayPortion(plan: KhatmaPlan, nowMs: Long): KhatmaPortion? {
    val cursor = khatmaCursorOf(plan) ?: return null
    val remaining = khatmaRemainingAyahs(plan)
    if (remaining <= 0) return null
    val daysLeft = khatmaDaysLeft(plan, nowMs).coerceAtLeast(1)
    val todaySize = khatmaPortionSizes(remaining, daysLeft).firstOrNull()?.coerceAtLeast(1)
        ?: remaining
    val end = khatmaAdvanceOrNull(cursor.first, cursor.second, todaySize) ?: return null
    return KhatmaPortion(cursor.first, cursor.second, end.first, end.second, todaySize)
}

/** Distinct Madinah pages from the cursor to the end of the Quran. */
fun khatmaRemainingPages(plan: KhatmaPlan): Int {
    val cursor = khatmaCursorOf(plan) ?: return 0
    val lastCount = khatmaSurahAyahCount(114)
    if (lastCount == 0) return 0
    return PageJuzIndex.pageCount(cursor.first, cursor.second, 114, lastCount)
}

/**
 * Pages/day for the plan: total pages spread evenly over the target days
 * (ceiling, min 1). The plan stores no explicit start, so it is
 * reconstructed by rewinding the cursor by [KhatmaPlan.totalAyahsRead];
 * for a never-progressed plan the cursor IS the start.
 */
fun khatmaPagesPerDay(plan: KhatmaPlan): Int {
    val cursor = khatmaCursorOf(plan)
    val start = if (cursor == null) {
        1 to 1
    } else {
        khatmaRetreatOrFirst(cursor.first, cursor.second, plan.totalAyahsRead.coerceAtLeast(0))
    }
    val lastCount = khatmaSurahAyahCount(114)
    if (lastCount == 0) return 0
    val totalPages = PageJuzIndex.pageCount(start.first, start.second, 114, lastCount)
    if (totalPages <= 0) return 0
    val days = plan.targetDays.coerceAtLeast(1)
    return ((totalPages + days - 1) / days).coerceAtLeast(1)
}

/**
 * ETA as epoch millis: now plus the ceiling of (remaining pages /
 * pages-per-day) days. A finished plan is due now.
 */
fun khatmaEtaDateMs(plan: KhatmaPlan, nowMs: Long): Long {
    if (khatmaIsComplete(plan)) return nowMs
    val pagesPerDay = khatmaPagesPerDay(plan).coerceAtLeast(1)
    val remaining = khatmaRemainingPages(plan)
    val etaDays = (remaining + pagesPerDay - 1) / pagesPerDay
    return nowMs + etaDays * KHATMA_DAY_MS
}

private val KHATMA_MONTH_SHORT = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

/** "Est. Oct 15" label for [etaMs] (UTC, proleptic Gregorian). */
fun khatmaEtaLabel(etaMs: Long): String {
    val day = if (etaMs >= 0) {
        etaMs / KHATMA_DAY_MS
    } else {
        (etaMs - KHATMA_DAY_MS + 1) / KHATMA_DAY_MS
    }
    val (_, month, dayOfMonth) = khatmaCivilFromDays(day)
    return "Est. ${KHATMA_MONTH_SHORT[month - 1]} $dayOfMonth"
}

/** Short "Oct 15" form of [khatmaEtaLabel] for stat boxes. */
fun khatmaShortDate(etaMs: Long): String = khatmaEtaLabel(etaMs).removePrefix("Est. ")

/** Proleptic Gregorian (year, month, day) for days since 1970-01-01. */
private fun khatmaCivilFromDays(z: Long): Triple<Int, Int, Int> {
    val shifted = z + 719_468
    val era = if (shifted >= 0) shifted / 146_097 else (shifted - 146_096) / 146_097
    val dayOfEra = (shifted - era * 146_097).toInt() // [0, 146096]
    val yearOfEra = (dayOfEra - dayOfEra / 1_460 + dayOfEra / 36_524 - dayOfEra / 146_096) / 365
    var year = yearOfEra + era.toInt() * 400
    val dayOfYear0 = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
    val monthPrime = (5 * dayOfYear0 + 2) / 153
    val day = dayOfYear0 - (153 * monthPrime + 2) / 5 + 1
    val month = if (monthPrime < 10) monthPrime + 3 else monthPrime - 9
    if (month <= 2) year += 1
    return Triple(year, month, day)
}

// ---------------------------------------------------------------------------
// Pace helpers (AGENT A10, additive only). Pure plan-vs-schedule math, no
// store access; unit-testable without the Settings singleton. Expected
// reading at [todayMs] is the plan-aware total (read + remaining) spread
// evenly over the target days; a full day's share of tolerance either way
// keeps the status gentle — only a day or more off-schedule flips it.
// ---------------------------------------------------------------------------

/** Journey pace of a plan against its daily schedule. */
enum class PaceStatus {
    AHEAD,
    ON_TRACK,
    BEHIND,
}

/**
 * Pace of [plan] at [todayMs]: compares ayahs read against the even-split
 * expectation for the elapsed days (clamped to the plan total past-target).
 * A day's share or more ahead reads [PaceStatus.AHEAD] (finishing early
 * included); a day's share or more short reads [PaceStatus.BEHIND];
 * anything within a day reads [PaceStatus.ON_TRACK]. A plan with no
 * readable total is [PaceStatus.ON_TRACK].
 */
fun paceStatus(plan: KhatmaPlan, todayMs: Long): PaceStatus {
    val done = plan.totalAyahsRead.coerceAtLeast(0)
    val total = done + khatmaRemainingAyahs(plan)
    if (total <= 0) return PaceStatus.ON_TRACK
    val target = plan.targetDays.coerceAtLeast(1)
    val elapsed = khatmaElapsedDays(plan, todayMs)
    val expected =
        (total.toDouble() * elapsed.toDouble() / target.toDouble()).coerceIn(0.0, total.toDouble())
    val dayShare = total.toDouble() / target.toDouble()
    val diff = done.toDouble() - expected
    return when {
        diff >= dayShare -> PaceStatus.AHEAD
        diff <= -dayShare -> PaceStatus.BEHIND
        else -> PaceStatus.ON_TRACK
    }
}

/**
 * Madinah pages needed to catch back up to schedule for [plan] at
 * [todayMs]: the ayah deficit converted via the plan's global
 * pages-per-ayah ratio (plan start reconstructed by rewinding the cursor,
 * same approach as [khatmaPagesPerDay]; ceiling, min 1). Returns 0 unless
 * [paceStatus] is [PaceStatus.BEHIND].
 */
fun catchUpPages(plan: KhatmaPlan, todayMs: Long): Int {
    if (paceStatus(plan, todayMs) != PaceStatus.BEHIND) return 0
    val done = plan.totalAyahsRead.coerceAtLeast(0)
    val total = done + khatmaRemainingAyahs(plan)
    if (total <= 0) return 0
    val target = plan.targetDays.coerceAtLeast(1)
    val elapsed = khatmaElapsedDays(plan, todayMs)
    val expected =
        (total.toDouble() * elapsed.toDouble() / target.toDouble()).coerceIn(0.0, total.toDouble())
    val deficit = (expected - done.toDouble()).coerceAtLeast(0.0)
    if (deficit <= 0.0) return 0
    val cursor = khatmaCursorOf(plan)
    val start = if (cursor == null) {
        1 to 1
    } else {
        khatmaRetreatOrFirst(cursor.first, cursor.second, done)
    }
    val lastCount = khatmaSurahAyahCount(114)
    val totalPages = if (lastCount == 0) {
        0
    } else {
        PageJuzIndex.pageCount(start.first, start.second, 114, lastCount)
    }
    if (totalPages <= 0) return 1
    return ceil(deficit * totalPages.toDouble() / total.toDouble()).toInt().coerceAtLeast(1)
}
