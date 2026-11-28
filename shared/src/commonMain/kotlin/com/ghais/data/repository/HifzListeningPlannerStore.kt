package com.ghais.data.repository

import com.ghais.data.seed.EveryAyahReciters
import com.ghais.domain.model.TrackItem
import com.russhwolf.settings.Settings
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A user-configured Hifz listening plan:
 * - [reciterSlugs]: one or more Qaris chosen for listening
 * - [surahIds]: one or more Surahs to listen to
 * - [timesPerSession]: how many times to repeat each surah per listening session
 * - [sessionsPerDay]: how many sessions per day the user commits to
 * - [durationDays]: total number of days for the plan
 * - [reminderEnabled] / [reminderHour] / [reminderMinute]: daily reminder that
 *   fires only when today's [sessionsPerDay] target has NOT been completed yet.
 */
@Serializable
data class HifzListeningPlan(
    val id: String,
    val title: String = "",
    val reciterSlugs: List<String> = emptyList(),
    val surahIds: List<Int> = emptyList(),
    val timesPerSession: Int = 3,
    val sessionsPerDay: Int = 2,
    val durationDays: Int = 7,
    val startEpochDay: Long = 0L,
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val completedSessionsByDay: Map<Long, Int> = emptyMap(),
    val enabled: Boolean = true,
    val autoPlayOnSchedule: Boolean = false,
    val escalateReminder: Boolean = true,
) {
    fun sessionsDoneOn(epochDay: Long): Int =
        completedSessionsByDay[epochDay] ?: 0

    fun isDayCompleted(epochDay: Long): Boolean =
        sessionsDoneOn(epochDay) >= sessionsPerDay.coerceAtLeast(1)

    fun dailyProgressFraction(epochDay: Long): Float =
        (sessionsDoneOn(epochDay).toFloat() / sessionsPerDay.coerceAtLeast(1)).coerceIn(0f, 1f)

    fun streakDays(epochDay: Long): Int {
        var day = when {
            isDayCompleted(epochDay) -> epochDay
            isDayCompleted(epochDay - 1L) -> epochDay - 1L
            else -> return 0
        }
        var streak = 0
        while (isDayCompleted(day)) {
            streak++
            day--
        }
        return streak
    }

    fun bestStreakDays(): Int {
        val completedDays = completedSessionsByDay.keys
            .filter { isDayCompleted(it) }
            .sorted()
        if (completedDays.isEmpty()) return 0
        var best = 1
        var current = 1
        for (i in 1 until completedDays.size) {
            if (completedDays[i] == completedDays[i - 1] + 1L) {
                current++
                if (current > best) best = current
            } else {
                current = 1
            }
        }
        return best
    }

    fun estimatedMinutesPerSession(): Int {
        val totalSeconds = surahIds.sumOf { id ->
            (QuranDataRepository.getSurahById(id)?.ayahsCount ?: 7) * 7 * timesPerSession.coerceAtLeast(1)
        }
        return (totalSeconds / 60).coerceAtLeast(1)
    }

    fun isWithinDuration(epochDay: Long): Boolean {
        val elapsed = epochDay - startEpochDay
        return elapsed >= 0L && elapsed < durationDays.coerceAtLeast(1)
    }

    fun currentDayNumber(epochDay: Long): Int {
        val elapsed = (epochDay - startEpochDay).toInt() + 1
        return elapsed.coerceIn(1, durationDays.coerceAtLeast(1))
    }

    fun completedDaysCount(): Int {
        val goal = sessionsPerDay.coerceAtLeast(1)
        return completedSessionsByDay.count { (_, count) -> count >= goal }
    }

    fun isReminderDue(epochDay: Long): Boolean =
        enabled && reminderEnabled && isWithinDuration(epochDay) && !isDayCompleted(epochDay)

    fun displayTitle(surahNameFor: (Int) -> String): String {
        val trimmed = title.trim()
        if (trimmed.isNotEmpty()) return trimmed
        val firstSurah = surahIds.firstOrNull()?.let(surahNameFor) ?: "Quran"
        val extra = if (surahIds.size > 1) " +${surahIds.size - 1}" else ""
        return "$firstSurah$extra • ${timesPerSession}× · $sessionsPerDay/day"
    }
}

/**
 * Persisted store for Hifz listening plans and daily session progress.
 *
 * Syncs each plan's incomplete-day reminder to [SchedulesStore] under the
 * reserved id prefix [SCHEDULE_ID_PREFIX] (`autoStart = false`), so exact OS
 * alarms reuse the existing [ScheduleEngine] + [SchedulePlayback] pipeline.
 */
object HifzListeningPlannerStore {
    const val SCHEDULE_ID_PREFIX = "hifz-plan-"
    const val ESCALATION_SCHEDULE_ID_PREFIX = "hifz-plan-esc-"
    private const val KEY_PLANS = "ghais_hifz_listening_plans"

    data class PlanCompletionEvent(
        val planId: String,
        val planTitle: String,
        val sessionsDoneToday: Int,
        val sessionsPerDay: Int,
        val dayCompleted: Boolean,
        val streak: Int,
        val timestampMs: Long,
    )

    private var ownerId: String = "local"
    private val settings: Settings by lazy { Settings() }
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _plans = MutableStateFlow<List<HifzListeningPlan>>(emptyList())
    val plans: StateFlow<List<HifzListeningPlan>> = _plans.asStateFlow()

    private val _lastCompletionEvent = MutableStateFlow<PlanCompletionEvent?>(null)
    val lastCompletionEvent: StateFlow<PlanCompletionEvent?> = _lastCompletionEvent.asStateFlow()

    init {
        load()
    }

    fun dismissCompletionEvent() {
        _lastCompletionEvent.value = null
    }

    private fun key(base: String): String =
        if (ownerId == "local") base else "$ownerId::$base"

    fun currentEpochDay(): Long =
        Clock.System.now().toEpochMilliseconds() / 86_400_000L

    fun scheduleIdFor(planId: String): String = "$SCHEDULE_ID_PREFIX$planId"

    fun escalationScheduleIdFor(planId: String): String = "$ESCALATION_SCHEDULE_ID_PREFIX$planId"

    fun isEscalationScheduleId(scheduleId: String): Boolean =
        scheduleId.startsWith(ESCALATION_SCHEDULE_ID_PREFIX)

    fun planIdFromScheduleId(scheduleId: String): String? = when {
        scheduleId.startsWith(ESCALATION_SCHEDULE_ID_PREFIX) ->
            scheduleId.removePrefix(ESCALATION_SCHEDULE_ID_PREFIX)
        scheduleId.startsWith(SCHEDULE_ID_PREFIX) ->
            scheduleId.removePrefix(SCHEDULE_ID_PREFIX)
        else -> null
    }?.takeIf { it.isNotBlank() }

    fun setOwner(ownerId: String) {
        if (ownerId == this.ownerId) return
        this.ownerId = ownerId
        load()
    }

    fun newId(): String =
        "hp-" + Clock.System.now().toEpochMilliseconds() + "-" + (0..9999).random()

    fun getPlan(planId: String): HifzListeningPlan? =
        _plans.value.firstOrNull { it.id == planId }

    fun savePlan(plan: HifzListeningPlan): HifzListeningPlan {
        val normalized = normalize(plan)
        val updated = _plans.value.filterNot { it.id == normalized.id } + normalized
        persist(updated.sortedByDescending { it.startEpochDay })
        syncScheduleAlarm(normalized)
        return normalized
    }

    fun deletePlan(planId: String) {
        val updated = _plans.value.filterNot { it.id == planId }
        if (updated.size != _plans.value.size) {
            persist(updated)
        }
        try {
            SchedulesStore.remove(scheduleIdFor(planId))
            SchedulesStore.remove(escalationScheduleIdFor(planId))
        } catch (_: Exception) {
        }
    }

    fun toggleEnabled(planId: String, enabled: Boolean) {
        val existing = getPlan(planId) ?: return
        savePlan(existing.copy(enabled = enabled))
    }

    fun recordSessionCompleted(
        planId: String,
        epochDay: Long = currentEpochDay(),
    ) {
        val existing = getPlan(planId) ?: return
        val nextMap = existing.completedSessionsByDay.toMutableMap()
        val current = nextMap[epochDay] ?: 0
        nextMap[epochDay] = current + 1
        val updatedPlan = existing.copy(completedSessionsByDay = nextMap)
        val updated = _plans.value.map { if (it.id == planId) updatedPlan else it }
        persist(updated)
        val planTitle = updatedPlan.displayTitle { id ->
            QuranDataRepository.getSurahById(id)?.nameEn ?: "Surah $id"
        }
        _lastCompletionEvent.value = PlanCompletionEvent(
            planId = planId,
            planTitle = planTitle,
            sessionsDoneToday = updatedPlan.sessionsDoneOn(epochDay),
            sessionsPerDay = updatedPlan.sessionsPerDay,
            dayCompleted = updatedPlan.isDayCompleted(epochDay),
            streak = updatedPlan.streakDays(epochDay),
            timestampMs = Clock.System.now().toEpochMilliseconds(),
        )
    }

    fun setDaySessions(
        planId: String,
        count: Int,
        epochDay: Long = currentEpochDay(),
    ) {
        val existing = getPlan(planId) ?: return
        val nextMap = existing.completedSessionsByDay.toMutableMap()
        val clamped = count.coerceAtLeast(0)
        if (clamped == 0) nextMap.remove(epochDay) else nextMap[epochDay] = clamped
        val updatedPlan = existing.copy(completedSessionsByDay = nextMap)
        val updated = _plans.value.map { if (it.id == planId) updatedPlan else it }
        persist(updated)
    }

    /**
     * Called by `SchedulePlayback` when an `autoStart = false` alarm fires.
     * For non-planner schedules, returns `true` (legacy behavior).
     * For `hifz-plan-*` schedules, returns `true` ONLY when the user has NOT
     * completed today's target yet and the plan is still active.
     */
    fun shouldNotifyIncomplete(
        scheduleId: String,
        epochDay: Long = currentEpochDay(),
    ): Boolean {
        val planId = planIdFromScheduleId(scheduleId) ?: return true
        val plan = getPlan(planId) ?: return false
        return plan.isReminderDue(epochDay)
    }

    fun buildSessionTracks(plan: HifzListeningPlan): List<TrackItem> {
        if (plan.reciterSlugs.isEmpty() || plan.surahIds.isEmpty()) return emptyList()
        val repeats = plan.timesPerSession.coerceIn(1, 50)
        val tracks = mutableListOf<TrackItem>()
        for (slug in plan.reciterSlugs) {
            val reciter = QuranDataRepository.getBrowseReciterBySlug(slug)?.takeIf { it.serverUrl.isNotBlank() }
                ?: QuranDataRepository.getReciterBySlug(slug)
            for (surahId in plan.surahIds) {
                val surah = QuranDataRepository.getSurahById(surahId) ?: continue
                if (!reciter.isSurahAvailable(surahId)) continue
                repeat(repeats) {
                    tracks.add(
                        TrackItem(
                            reciterSlug = reciter.slug,
                            reciterName = reciter.nameEn,
                            surahId = surah.id,
                            surahNameEn = surah.nameEn,
                            surahNameAr = surah.nameAr,
                            audioUrl = reciter.getFullSurahUrl(surah.id),
                            durationMs = surah.ayahsCount * 15_000L,
                        )
                    )
                }
            }
        }
        return tracks
    }

    private fun normalize(plan: HifzListeningPlan): HifzListeningPlan {
        val cleanId = plan.id.ifBlank { newId() }
        val cleanStart = if (plan.startEpochDay > 0L) plan.startEpochDay else currentEpochDay()
        return plan.copy(
            id = cleanId,
            title = plan.title.trim(),
            reciterSlugs = plan.reciterSlugs.distinct().filter { it.isNotBlank() },
            surahIds = plan.surahIds.distinct().filter { it in 1..114 },
            timesPerSession = plan.timesPerSession.coerceIn(1, 50),
            sessionsPerDay = plan.sessionsPerDay.coerceIn(1, 20),
            durationDays = plan.durationDays.coerceIn(1, 365),
            startEpochDay = cleanStart,
            reminderHour = plan.reminderHour.coerceIn(0, 23),
            reminderMinute = plan.reminderMinute.coerceIn(0, 59),
        )
    }

    private fun syncScheduleAlarm(plan: HifzListeningPlan) {
        val scheduleId = scheduleIdFor(plan.id)
        val escScheduleId = escalationScheduleIdFor(plan.id)
        try {
            if (!plan.enabled || !plan.reminderEnabled) {
                SchedulesStore.remove(scheduleId)
                SchedulesStore.remove(escScheduleId)
                return
            }
            val reciterSlug = plan.reciterSlugs.firstOrNull() ?: "mishary"
            val fromSurah = plan.surahIds.firstOrNull() ?: 1
            val toSurah = plan.surahIds.lastOrNull() ?: 1
            SchedulesStore.add(
                RecitationSchedule(
                    id = scheduleId,
                    hour = plan.reminderHour,
                    minute = plan.reminderMinute,
                    reciterSlug = reciterSlug,
                    fromSurah = fromSurah,
                    toSurah = toSurah,
                    durationMin = null,
                    autoStart = plan.autoPlayOnSchedule,
                    enabled = true,
                )
            )
            if (plan.escalateReminder) {
                val (escHour, escMinute) = if (plan.reminderHour + 2 <= 22) {
                    (plan.reminderHour + 2) to plan.reminderMinute
                } else {
                    22 to 45
                }
                SchedulesStore.add(
                    RecitationSchedule(
                        id = escScheduleId,
                        hour = escHour,
                        minute = escMinute,
                        reciterSlug = reciterSlug,
                        fromSurah = fromSurah,
                        toSurah = toSurah,
                        durationMin = null,
                        autoStart = false,
                        enabled = true,
                    )
                )
            } else {
                SchedulesStore.remove(escScheduleId)
            }
        } catch (_: Exception) {
        }
    }

    private fun load() {
        val raw = try {
            settings.getStringOrNull(key(KEY_PLANS))
        } catch (_: Exception) {
            null
        }
        if (raw.isNullOrBlank()) {
            _plans.value = emptyList()
            return
        }
        _plans.value = try {
            json.decodeFromString<List<HifzListeningPlan>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persist(list: List<HifzListeningPlan>) {
        _plans.value = list
        try {
            settings.putString(key(KEY_PLANS), json.encodeToString(list))
        } catch (_: Exception) {
        }
    }
}
