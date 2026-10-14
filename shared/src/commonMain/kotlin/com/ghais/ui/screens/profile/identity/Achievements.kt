package com.ghais.ui.screens.profile.identity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * AGENT A11 — Achievements.
 *
 * Pure core: [ACHIEVEMENTS] defs + [AchievementStats] + [unlockedAchievements]
 * + [isNightHour]. UI: [AchievementsShelf].
 * New file only — ProfileScreen wires it via the insertion snippet in the
 * agent report.
 *
 * [AchievementStats] holds PRIMITIVES ONLY (no repository/store types), so the
 * unlock math stays unit-testable on the JVM commonTest classpath and callers
 * map from `UserUsageRepository.stats` / `KhatmaStore` / `HifzMasteryStore`
 * without this file depending on any of them.
 */

// ---------------------------------------------------------------------------
// Tiers + defs
// ---------------------------------------------------------------------------

enum class AchievementTier { EASY, MEDIUM, RARE, HARD }

data class AchievementDef(
    val id: String,
    val title: String,
    val desc: String,
    val tier: AchievementTier
)

const val ACH_FIRST_LISTEN = "first-listen"
const val ACH_SEVEN_DAY_DEVOTION = "seven-day-devotion"
const val ACH_TEN_VOICES = "ten-voices"
const val ACH_NIGHT_SESSION = "night-session"
const val ACH_FIRST_KHATMA = "first-khatma"
const val ACH_THOUSAND_MINUTES = "thousand-minutes"
const val ACH_SURAH_MASTERED = "surah-mastered"

val ACHIEVEMENTS: List<AchievementDef> = listOf(
    AchievementDef(
        id = ACH_FIRST_LISTEN,
        title = "First Light",
        desc = "Complete your first listen",
        tier = AchievementTier.EASY
    ),
    AchievementDef(
        id = ACH_SEVEN_DAY_DEVOTION,
        title = "Seven-Day Devotion",
        desc = "Listen 7 days in a row",
        tier = AchievementTier.MEDIUM
    ),
    AchievementDef(
        id = ACH_TEN_VOICES,
        title = "Ten Voices",
        desc = "Listen to 10 distinct reciters",
        tier = AchievementTier.RARE
    ),
    AchievementDef(
        id = ACH_NIGHT_SESSION,
        title = "Night Vigil",
        desc = "Listen between 22:00 and 04:00",
        tier = AchievementTier.MEDIUM
    ),
    AchievementDef(
        id = ACH_FIRST_KHATMA,
        title = "First Khatma",
        desc = "Complete a Quran completion plan",
        tier = AchievementTier.HARD
    ),
    AchievementDef(
        id = ACH_THOUSAND_MINUTES,
        title = "Thousand Minutes",
        desc = "Listen for 1000 minutes all-time",
        tier = AchievementTier.HARD
    ),
    AchievementDef(
        id = ACH_SURAH_MASTERED,
        title = "Surah Mastered",
        desc = "Master any surah by heart",
        tier = AchievementTier.RARE
    )
)

// ---------------------------------------------------------------------------
// Pure unlock math
// ---------------------------------------------------------------------------

/**
 * Primitive-only snapshot for [unlockedAchievements].
 *
 * @param hasListened any qualifying listen ever recorded.
 * @param daysStreak consecutive-day streak (maps to `UserListeningStats.daysStreak`).
 * @param uniqueReciters distinct reciter count.
 * @param sessionHour24 hour-of-day (0..23) of a qualifying session for the
 *   night achievement; any value outside 0..23 (e.g. -1 = unknown) locks it.
 * @param khatmaCompleted completed khatma plans (param — maps to KhatmaStore).
 * @param totalMinutes lifetime listening minutes.
 * @param masteredSurahs mastered surah count (param — maps to HifzMasteryStore).
 */
data class AchievementStats(
    val hasListened: Boolean = false,
    val daysStreak: Int = 0,
    val uniqueReciters: Int = 0,
    val sessionHour24: Int = -1,
    val khatmaCompleted: Int = 0,
    val totalMinutes: Long = 0L,
    val masteredSurahs: Int = 0
)

/**
 * True for hours in the 22:00–04:00 night window (start-inclusive,
 * end-exclusive): 22, 23, 0, 1, 2, 3. Hour 4 (04:xx) is OUTSIDE — the window
 * ends at 04:00 sharp. Pure.
 */
fun isNightHour(hour24: Int): Boolean =
    hour24 == 22 || hour24 == 23 || hour24 in 0..3

/**
 * Ids of unlocked achievements for [stats]. Unknown ids are never returned;
 * locked achievements are simply absent. Pure, never throws.
 */
fun unlockedAchievements(stats: AchievementStats): Set<String> {
    val out = LinkedHashSet<String>()
    if (stats.hasListened) out.add(ACH_FIRST_LISTEN)
    if (stats.daysStreak.coerceAtLeast(0) >= 7) out.add(ACH_SEVEN_DAY_DEVOTION)
    if (stats.uniqueReciters.coerceAtLeast(0) >= 10) out.add(ACH_TEN_VOICES)
    if (isNightHour(stats.sessionHour24)) out.add(ACH_NIGHT_SESSION)
    if (stats.khatmaCompleted.coerceAtLeast(0) >= 1) out.add(ACH_FIRST_KHATMA)
    if (stats.totalMinutes.coerceAtLeast(0L) >= 1000L) out.add(ACH_THOUSAND_MINUTES)
    if (stats.masteredSurahs.coerceAtLeast(0) >= 1) out.add(ACH_SURAH_MASTERED)
    return out
}

// ---------------------------------------------------------------------------
// UI
// ---------------------------------------------------------------------------

private fun achievementIcon(id: String): ImageVector = when (id) {
    ACH_FIRST_LISTEN -> Icons.Filled.Headphones
    ACH_SEVEN_DAY_DEVOTION -> Icons.Filled.LocalFireDepartment
    ACH_TEN_VOICES -> Icons.Filled.RecordVoiceOver
    ACH_NIGHT_SESSION -> Icons.Filled.Bedtime
    ACH_FIRST_KHATMA -> Icons.Filled.MenuBook
    ACH_THOUSAND_MINUTES -> Icons.Filled.Timer
    ACH_SURAH_MASTERED -> Icons.Filled.EmojiEvents
    else -> Icons.Filled.EmojiEvents
}

private fun tierLabel(tier: AchievementTier): String = when (tier) {
    AchievementTier.EASY -> "EASY"
    AchievementTier.MEDIUM -> "MEDIUM"
    AchievementTier.RARE -> "RARE"
    AchievementTier.HARD -> "HARD"
}

/**
 * Noir achievements shelf: horizontal rail of locked/unlocked badges.
 * Locked = dimmed well + tertiary copy; unlocked = chrome play-well +
 * primary copy. Optional [onShare] (e.g. ShareSheet) receives the tapped
 * UNLOCKED def; locked taps are inert.
 */
@Composable
fun AchievementsShelf(
    unlocked: Set<String>,
    modifier: Modifier = Modifier,
    onShare: ((AchievementDef) -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        NoirSectionHeader(label = "Achievements • ${unlocked.size}/${ACHIEVEMENTS.size}")
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(ACHIEVEMENTS, key = { it.id }) { def ->
                val isUnlocked = def.id in unlocked
                AchievementBadge(
                    def = def,
                    unlocked = isUnlocked,
                    onClick = if (isUnlocked && onShare != null) ({ onShare(def) }) else null
                )
            }
        }
    }
}

@Composable
private fun AchievementBadge(
    def: AchievementDef,
    unlocked: Boolean,
    onClick: (() -> Unit)?
) {
    val cardModifier = Modifier
        .width(140.dp)
        .clip(GhaisShapes.cardNoir)
        .background(GhaisNoir.cardFillSoft())
        .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.cardNoir)
        .topSpecular(inset = 18.dp)
        .then(if (onClick != null) Modifier.noirClickable(onClick) else Modifier)
        .then(if (!unlocked) Modifier.alpha(0.55f) else Modifier)

    Box(modifier = cardModifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(GhaisNoir.sheen())
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (unlocked) GhaisNoir.chromeFill()
                            else GhaisNoir.wellFill()
                        )
                        .border(1.dp, GhaisNoir.BorderCard, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = achievementIcon(def.id),
                        contentDescription = null,
                        tint = if (unlocked) GhaisNoir.OnChrome else GhaisNoir.TextTertiary,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.Fill2)
                        .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tierLabel(def.tier),
                        color = GhaisNoir.TextTertiary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        maxLines = 1
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = def.title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (unlocked) GhaisNoir.TextPrimary else GhaisNoir.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (unlocked) def.desc else "Locked • ${def.desc}",
                fontSize = 11.5.sp,
                color = GhaisNoir.TextTertiary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
