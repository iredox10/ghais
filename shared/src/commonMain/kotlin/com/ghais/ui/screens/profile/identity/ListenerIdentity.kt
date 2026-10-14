package com.ghais.ui.screens.profile.identity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import kotlin.time.Clock

/**
 * AGENT A11 — Listener identity.
 *
 * Pure core: [buildIdentityMemberSince] + [listenerTitle]. UI: [IdentityCard].
 * New file only — ProfileScreen wires it via the insertion snippet in the
 * agent report.
 *
 * Date math is dependency-free (no kotlinx-datetime on this classpath):
 * month/year derive from epoch days via the same proleptic-Gregorian
 * algorithm `KhatmaStore` uses (`khatmaCivilFromDays`), UTC.
 */

private const val DAY_MS = 86_400_000L

private val IDENTITY_MONTH_SHORT = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

/**
 * "Member since Oct 2024" for [firstLaunchMs] (epoch millis, UTC).
 *
 * - Non-positive input (unknown install time) → "New here".
 * - Future input (clock skew) clamps to [nowMs]'s month.
 * - Pure apart from the [nowMs] default; pass both args in tests.
 */
fun buildIdentityMemberSince(
    firstLaunchMs: Long,
    nowMs: Long = Clock.System.now().toEpochMilliseconds()
): String {
    if (firstLaunchMs <= 0L) return "New here"
    val clamped = minOf(firstLaunchMs, nowMs.coerceAtLeast(0L))
    val day = clamped / DAY_MS
    val (year, month, _) = identityCivilFromDays(day)
    if (month !in 1..12) return "Member since $year"
    return "Member since ${IDENTITY_MONTH_SHORT[month - 1]} $year"
}

private fun identityCivilFromDays(z: Long): Triple<Int, Int, Int> {
    val shifted = z + 719_468
    val era = if (shifted >= 0) shifted / 146_097 else (shifted - 146_096) / 146_097
    val dayOfEra = (shifted - era * 146_097).toInt()
    val yearOfEra = (dayOfEra - dayOfEra / 1_460 + dayOfEra / 36_524 - dayOfEra / 146_096) / 365
    var year = yearOfEra + era.toInt() * 400
    val dayOfYear0 = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
    val monthPrime = (5 * dayOfYear0 + 2) / 153
    val day = dayOfYear0 - (153 * monthPrime + 2) / 5 + 1
    val month = if (monthPrime < 10) monthPrime + 3 else monthPrime - 9
    if (month <= 2) year += 1
    return Triple(year, month, day)
}

/**
 * Listener title from aggregate stats heuristics.
 *
 * - 0 minutes → "New listener"
 * - >= 10 distinct reciters → "Voice explorer"
 * - >= 1000 minutes → "Murattal soul" (loyal depth listener)
 * - >= 120 minutes → "Devoted listener"
 * - else → "Seeking soul"
 *
 * "Night listener" is intentionally NOT returned here: night-ness needs a
 * time-of-day signal, which these two params cannot carry. Use the
 * three-arg overload when a night-session count is available.
 * Pure, never throws (negatives clamp to 0).
 */
fun listenerTitle(uniqueReciters: Int, totalMinutes: Long): String {
    val voices = uniqueReciters.coerceAtLeast(0)
    val minutes = totalMinutes.coerceAtLeast(0L)
    if (minutes <= 0L) return "New listener"
    if (voices >= 10) return "Voice explorer"
    if (minutes >= 1000L) return "Murattal soul"
    if (minutes >= 120L) return "Devoted listener"
    return "Seeking soul"
}

/**
 * Night-aware overload: returns "Night listener" when at least [nightSessions]
 * qualifying 22:00–04:00 sessions are recorded AND the base title would
 * otherwise be depth/loyalty-flavoured (minutes >= 120, voices < 10).
 * Otherwise delegates to the two-arg heuristic. Pure.
 */
fun listenerTitle(uniqueReciters: Int, totalMinutes: Long, nightSessions: Int): String {
    if (nightSessions.coerceAtLeast(0) >= 5 &&
        totalMinutes.coerceAtLeast(0L) >= 120L &&
        uniqueReciters.coerceAtLeast(0) < 10
    ) {
        return "Night listener"
    }
    return listenerTitle(uniqueReciters, totalMinutes)
}

/**
 * Noir identity card: monogram well + display name + title chip +
 * member-since caption. Tap opens the edit-profile dialog via [onClick].
 */
@Composable
fun IdentityCard(
    displayName: String,
    title: String,
    memberSince: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(GhaisShapes.cardNoir)
            .background(GhaisNoir.cardFillSoft())
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.cardNoir)
            .topSpecular()
            .noirClickable(onClick)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(GhaisNoir.sheen())
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(GhaisNoir.wellFill())
                    .border(1.dp, GhaisNoir.BorderCard, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = GhaisNoir.TextSecondary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    color = GhaisNoir.TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                    maxLines = 1
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.Fill2)
                        .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title.uppercase(),
                        color = GhaisNoir.TextSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.9.sp,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = memberSince,
                    color = GhaisNoir.TextTertiary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }
}
