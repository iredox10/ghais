package com.ghais.hifz

import com.ghais.data.repository.MasteryStatus
import com.ghais.data.seed.QuranData

enum class HifzSessionTier {
    SABAQ,
    SABQI,
    MANZIL,
}

data class HifzSessionPlan(
    val tier: HifzSessionTier,
    val title: String,
    val subtitle: String,
    val targets: List<Pair<Int, Int>>,
)

private const val SABAQ_MAX_AYAHS = 7

/**
 * Pure 3-tier SRS session planner for daily Hifz:
 * - [HifzSessionTier.SABAQ]: next unpracticed portion (Al-Fatihah first, then Juz Amma, then remaining surahs).
 * - [HifzSessionTier.SABQI]: recently practiced / consolidating ayahs not yet due today.
 * - [HifzSessionTier.MANZIL]: weak ayahs and ayahs whose due day is on or before [todayEpochDay].
 */
fun buildSrsSessions(
    masteryMap: Map<Pair<Int, Int>, MasteryStatus>,
    dueDayMap: Map<Pair<Int, Int>, Long>,
    weakAyahs: List<Pair<Int, Int>>,
    todayEpochDay: Long,
): List<HifzSessionPlan> {
    val sabaq = buildSabaqPlan(masteryMap)
    val manzil = buildManzilPlan(masteryMap, dueDayMap, weakAyahs, todayEpochDay)
    val manzilSet = manzil.targets.toSet()
    val sabqi = buildSabqiPlan(masteryMap, dueDayMap, manzilSet, todayEpochDay)
    return listOf(sabaq, sabqi, manzil)
}

private fun buildSabaqPlan(
    masteryMap: Map<Pair<Int, Int>, MasteryStatus>,
): HifzSessionPlan {
    val surahById = QuranData.SURAHS.associateBy { it.id }
    // Progression order: Al-Fatihah (1), then Juz Amma from An-Nas (114) up to An-Naba (78), then 2..77.
    val progressionOrder = buildList {
        add(1)
        for (id in 114 downTo 78) add(id)
        for (id in 2..77) add(id)
    }

    for (surahId in progressionOrder) {
        val surah = surahById[surahId] ?: continue
        val unpracticed = (1..surah.ayahsCount)
            .map { surahId to it }
            .filter { coord ->
                masteryMap[coord] == null
            }
            .take(SABAQ_MAX_AYAHS)

        if (unpracticed.isNotEmpty()) {
            return HifzSessionPlan(
                tier = HifzSessionTier.SABAQ,
                title = "Sabaq",
                subtitle = "${surah.nameEn} • ${unpracticed.size} new",
                targets = unpracticed,
            )
        }
    }

    return HifzSessionPlan(
        tier = HifzSessionTier.SABAQ,
        title = "Sabaq",
        subtitle = "All caught up",
        targets = emptyList(),
    )
}

private fun buildSabqiPlan(
    masteryMap: Map<Pair<Int, Int>, MasteryStatus>,
    dueDayMap: Map<Pair<Int, Int>, Long>,
    manzilSet: Set<Pair<Int, Int>>,
    todayEpochDay: Long,
): HifzSessionPlan {
    val targets = masteryMap.entries
        .filter { (coord, status) ->
            if (coord in manzilSet) return@filter false
            when (status) {
                MasteryStatus.NEW -> true
                MasteryStatus.REVIEW_NEEDED, MasteryStatus.MASTERED -> {
                    val dueDay = dueDayMap[coord]
                    dueDay != null && dueDay > todayEpochDay
                }
            }
        }
        .map { it.key }
        .sortedWith(compareBy({ it.first }, { it.second }))

    val subtitle = if (targets.isEmpty()) {
        "No recent ayahs"
    } else {
        "${targets.size} consolidating"
    }

    return HifzSessionPlan(
        tier = HifzSessionTier.SABQI,
        title = "Sabqi",
        subtitle = subtitle,
        targets = targets,
    )
}

private fun buildManzilPlan(
    masteryMap: Map<Pair<Int, Int>, MasteryStatus>,
    dueDayMap: Map<Pair<Int, Int>, Long>,
    weakAyahs: List<Pair<Int, Int>>,
    todayEpochDay: Long,
): HifzSessionPlan {
    val seen = LinkedHashSet<Pair<Int, Int>>()
    for (coord in weakAyahs) {
        seen.add(coord)
    }

    val dueFromMap = dueDayMap.entries
        .filter { (_, dueDay) -> dueDay <= todayEpochDay }
        .sortedWith(compareBy({ it.value }, { it.key.first }, { it.key.second }))
        .map { it.key }

    for (coord in dueFromMap) {
        seen.add(coord)
    }

    // Tracked non-NEW ayahs without an explicit dueDay are treated as due today (matches HifzMasteryStore.dueToday).
    val legacyDue = masteryMap.entries
        .filter { (coord, status) ->
            status != MasteryStatus.NEW && coord !in dueDayMap
        }
        .sortedWith(compareBy({ it.key.first }, { it.key.second }))
        .map { it.key }

    for (coord in legacyDue) {
        seen.add(coord)
    }

    val targets = seen.toList()
    val subtitle = if (targets.isEmpty()) {
        "Nothing due today"
    } else {
        "${targets.size} due review"
    }

    return HifzSessionPlan(
        tier = HifzSessionTier.MANZIL,
        title = "Manzil",
        subtitle = subtitle,
        targets = targets,
    )
}
