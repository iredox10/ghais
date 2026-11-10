package com.ghais.ui.screens.stats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.delay
import kotlin.time.Clock

/** Stored buckets are UTC epoch days; refresh while visible across midnight. */
@Composable
internal fun rememberStatsEpochDay(): Long {
    val day by produceState(Clock.System.now().toEpochMilliseconds() / 86_400_000L) {
        while (true) {
            value = Clock.System.now().toEpochMilliseconds() / 86_400_000L
            delay(1_000)
        }
    }
    return day
}

/** Remote data excludes this device, so positive totals are additive. */
internal fun combinedStatsDays(local: Map<Long, Long>, remote: Map<Long, Long>): Map<Long, Long> =
    (local.keys + remote.keys).associateWith { day ->
        val own = (local[day] ?: 0L).coerceAtLeast(0L)
        val other = (remote[day] ?: 0L).coerceAtLeast(0L)
        if (Long.MAX_VALUE - own < other) Long.MAX_VALUE else own + other
    }
