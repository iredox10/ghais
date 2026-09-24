package com.ghais.data.repository

/**
 * Pure, unit-testable helpers over per-day listening totals.
 *
 * The [days] map is keyed by epoch day (UTC millis / 86_400_000) with values
 * in seconds. No app-specific imports; plain Kotlin only.
 */
enum class StatsRange(val label: String) {
    TODAY("Today"),
    WEEK("This week"),
    DAYS_30("30 days"),
    MONTHS_3("3 months"),
    YEAR("This year"),
}

const val MILLIS_PER_DAY: Long = 86_400_000L

private val WEEKDAY_SHORT = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val MONTH_SHORT = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

/** UTC epoch day for the given wall-clock millis (floor division). */
fun epochDayFromMillis(millis: Long): Long =
    if (millis >= 0) millis / MILLIS_PER_DAY
    else (millis - MILLIS_PER_DAY + 1) / MILLIS_PER_DAY

/** Number of calendar days covered by [range], including today. */
fun rangeWindowDays(range: StatsRange): Int = when (range) {
    StatsRange.TODAY -> 1
    StatsRange.WEEK -> 7
    StatsRange.DAYS_30 -> 30
    StatsRange.MONTHS_3 -> 91 // 13 whole weeks, aligns with weekly buckets
    StatsRange.YEAR -> 365
}

/** First epoch day included in [range] ending on [todayEpochDay]. */
fun rangeStartDay(range: StatsRange, todayEpochDay: Long): Long =
    todayEpochDay - rangeWindowDays(range) + 1

/** Total seconds listened within [range]. Missing days count as 0. */
fun rangeTotal(
    range: StatsRange,
    days: Map<Long, Long>,
    todayEpochDay: Long,
): Long {
    val start = rangeStartDay(range, todayEpochDay)
    var total = 0L
    var day = start
    while (day <= todayEpochDay) {
        total += days[day] ?: 0L
        day++
    }
    return total
}

/** Mean seconds per calendar day over the [range] window (zero-filled). */
fun rangeAverage(
    range: StatsRange,
    days: Map<Long, Long>,
    todayEpochDay: Long,
): Double = rangeTotal(range, days, todayEpochDay).toDouble() / rangeWindowDays(range)

/**
 * Best day in [range] as [Pair] of (epochDay, seconds), or null when no
 * day in the window has a positive total.
 */
fun bestDay(
    range: StatsRange,
    days: Map<Long, Long>,
    todayEpochDay: Long,
): Pair<Long, Long>? {
    val start = rangeStartDay(range, todayEpochDay)
    var best: Pair<Long, Long>? = null
    var day = start
    while (day <= todayEpochDay) {
        val seconds = days[day] ?: 0L
        if (seconds > 0 && (best == null || seconds > best.second)) {
            best = day to seconds
        }
        day++
    }
    return best
}

/**
 * Chart bars for [range] as (label, seconds), oldest first.
 *
 * - [StatsRange.TODAY]: single "Today" bar.
 * - [StatsRange.WEEK] / [StatsRange.DAYS_30]: one bar per day labelled
 *   with the weekday short name ("Mon").
 * - [StatsRange.MONTHS_3] / [StatsRange.YEAR]: 7-day sums ending today;
 *   a bucket containing the 1st of a month is labelled with the month
 *   short name ("Jan"), otherwise "W\<weekOfYear\>" (e.g. "W12").
 *   Capped at the most recent 30 bars.
 */
fun barsForRange(
    range: StatsRange,
    days: Map<Long, Long>,
    todayEpochDay: Long,
): List<Pair<String, Long>> {
    if (range == StatsRange.TODAY) {
        return listOf("Today" to (days[todayEpochDay] ?: 0L))
    }
    if (range == StatsRange.WEEK || range == StatsRange.DAYS_30) {
        val start = rangeStartDay(range, todayEpochDay)
        val bars = ArrayList<Pair<String, Long>>(rangeWindowDays(range))
        var day = start
        while (day <= todayEpochDay) {
            bars.add(weekdayShort(day) to (days[day] ?: 0L))
            day++
        }
        return bars
    }
    // MONTHS_3 / YEAR: weekly sums, most recent week ends today.
    val windowDays = rangeWindowDays(range)
    val weekCount = (windowDays + 6) / 7
    val bars = ArrayList<Pair<String, Long>>(weekCount)
    var w = weekCount - 1
    while (w >= 0) {
        val weekEnd = todayEpochDay - w * 7
        val weekStart = weekEnd - 6
        var sum = 0L
        var day = weekStart
        while (day <= weekEnd) {
            sum += days[day] ?: 0L
            day++
        }
        bars.add(weeklyLabel(weekStart, weekEnd) to sum)
        w--
    }
    return if (bars.size > 30) bars.takeLast(30) else bars
}

/**
 * Compact duration: "45m", "3h 20m", "3h", "2d 4h", "2d".
 * Durations under a minute render as seconds ("30s").
 */
fun formatDuration(totalSeconds: Long): String {
    if (totalSeconds < 60) return "${totalSeconds.coerceAtLeast(0)}s"
    val minutes = totalSeconds / 60
    if (minutes < 60) return "${minutes}m"
    val hours = minutes / 60
    if (hours < 24) {
        val rem = minutes % 60
        return if (rem == 0L) "${hours}h" else "${hours}h ${rem}m"
    }
    val days = hours / 24
    val remHours = hours % 24
    return if (remHours == 0L) "${days}d" else "${days}d ${remHours}h"
}

// --- Date math (pure, no date library) ---

/** Weekday short name for an epoch day; 1970-01-01 was a Thursday. */
private fun weekdayShort(epochDay: Long): String {
    val mondayBased = ((epochDay + 3) % 7 + 7) % 7
    return WEEKDAY_SHORT[mondayBased.toInt()]
}

/** Label for a 7-day bucket: month name when it spans a month start, else W<weekOfYear>. */
private fun weeklyLabel(weekStart: Long, weekEnd: Long): String {
    var day = weekStart
    while (day <= weekEnd) {
        val (_, month, dayOfMonth) = civilFromDays(day)
        if (dayOfMonth == 1) return MONTH_SHORT[month - 1]
        day++
    }
    val (year, _, _) = civilFromDays(weekStart)
    return "W${weekOfYear(weekStart, year)}"
}

/** 1-based week-of-year: week 1 = days 1..7 of the year. */
private fun weekOfYear(epochDay: Long, year: Int): Int =
    (dayOfYear(epochDay, year) - 1) / 7 + 1

private fun dayOfYear(epochDay: Long, year: Int): Int {
    val (_, month, day) = civilFromDays(epochDay)
    var doy = day
    var m = 1
    while (m < month) {
        doy += daysInMonth(year, m)
        m++
    }
    return doy
}

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    else -> if (isLeapYear(year)) 29 else 28
}

private fun isLeapYear(year: Int): Boolean =
    year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

/** Proleptic Gregorian (year, month, day) for days since 1970-01-01. */
private fun civilFromDays(z: Long): Triple<Int, Int, Int> {
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
