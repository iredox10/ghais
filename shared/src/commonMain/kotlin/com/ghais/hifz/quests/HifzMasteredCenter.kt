package com.ghais.hifz.quests

import com.ghais.data.repository.HifzMasteryStore
import com.ghais.flags.HookFlags

/**
 * Central mastery-mutation payoff: the single call site for "an ayah just
 * became MASTERED".
 *
 * Contract: the caller mutates the store first ([HifzMasteryStore.cycleStatus]
 * / `setStatus`), passing whether the surah was already complete BEFORE the
 * mutation. The center awards [HifzEvent.AYAH_MASTERED] XP, plus
 * [HifzEvent.SURAH_DONE] when this mutation completes the surah, and reports
 * level-ups + just-completed flags for toasts/dialogs. XP is gated on
 * [HookFlags.achievementsEnabled]; the ceremony flag on
 * [HookFlags.ceremoniesEnabled]. Never throws.
 */
data class MasteredOutcome(
    val xpAwarded: Int,
    val leveledUpTo: HifzXpLevel?,
    val surahJustCompleted: Boolean,
)

fun onAyahMastered(surahId: Int, wasCompleteBefore: Boolean): MasteredOutcome = try {
    var awarded = 0
    var levelBefore: HifzXpLevel? = null
    if (HookFlags.achievementsEnabled) {
        try {
            levelBefore = hifzXpLevel(HifzXpStore.totalXp.value)
        } catch (_: Exception) {
        }
        awarded += try {
            HifzXpStore.addXp(HifzEvent.AYAH_MASTERED)
        } catch (_: Exception) {
            0
        }
    }
    var justCompleted = false
    if (!wasCompleteBefore) {
        val nowComplete = try {
            HifzMasteryStore.getSurahMasteredPercentage(surahId) == 1f
        } catch (_: Exception) {
            false
        }
        if (nowComplete) {
            justCompleted = HookFlags.ceremoniesEnabled
            if (HookFlags.achievementsEnabled) {
                awarded += try {
                    HifzXpStore.addXp(HifzEvent.SURAH_DONE)
                } catch (_: Exception) {
                    0
                }
            }
        }
    }
    val levelAfter = try {
        hifzXpLevel(HifzXpStore.totalXp.value)
    } catch (_: Exception) {
        null
    }
    val leveledUp = if (levelBefore != null && levelAfter != null && levelAfter.minXp > levelBefore.minXp) {
        levelAfter
    } else {
        null
    }
    MasteredOutcome(xpAwarded = awarded, leveledUpTo = leveledUp, surahJustCompleted = justCompleted)
} catch (_: Exception) {
    MasteredOutcome(xpAwarded = 0, leveledUpTo = null, surahJustCompleted = false)
}
