package com.ghais.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.repository.FollowStore
import com.ghais.data.repository.ReciterCloudCache
import com.ghais.data.repository.UserUsageRepository
import com.ghais.data.repository.prettifySlug
import com.ghais.data.repository.resolveFollowedQari
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.screens.reciters.rememberCloudPhoto
import com.ghais.ui.theme.GhaisNoir
import kotlin.time.Clock

/**
 * Stories rail for followed qaris + discover tile.
 *
 * Portraits render in natural colour as plain circles (no ring); the trailing
 * tile jumps to discovery.
 */
@Composable
fun HomeFollowedRow(
    onReciter: (String) -> Unit,
    onDiscover: () -> Unit,
) {
    val followedSlugs by FollowStore.followedSlugs.collectAsState()
    // Re-resolve on catalog sync so an admin-uploaded reciter shows up here.
    val cloudCatalog by ReciterCloudCache.cloudReciters.collectAsState()
    val resolved = remember(followedSlugs, cloudCatalog) {
        // Keep an unresolvable slug rather than dropping it. mapNotNull here
        // made the whole section claim "No Qari followed yet" while
        // FollowStore still held the record. The fallback name comes from the
        // slug, so it never borrows another reciter's identity.
        followedSlugs.map { slug ->
            val qari = resolveFollowedQari(slug)
            Triple(
                qari?.reciter?.slug ?: slug,
                qari?.reciter?.nameEn ?: slug.prettifySlug(),
                qari?.photoUrl.orEmpty()
            )
        // Two followed slugs can resolve to the SAME reciter (mishary +
        // alafasy are the same person), which would both duplicate the row
        // and collide on the LazyRow key below.
        }.distinctBy { it.first }
    }

    if (resolved.isEmpty()) {
        NoirCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No Qari followed yet",
                    color = GhaisNoir.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Follow a Qari from their profile and they will appear here.",
                    color = GhaisNoir.TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
        return
    }

    // Stories rail: circular portraits (no ring) + a discover tile that
    // jumps to the Reciters tab. Tapping a story opens the profile.
    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(resolved, key = { it.first }) { (slug, name, photoUrl) ->
            // Cloud portrait first: the admin panel owns reciter photos, and
            // followed qaris outside the small bundled roster have no static
            // image — without this they rendered as bare monograms.
            val cloudPhoto = rememberCloudPhoto(slug)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(76.dp)
                    .noirClickable { onReciter(slug) }
            ) {
                // Border color removed per user request — plain circular photo.
                NoirArtworkWell(
                    coverUrl = cloudPhoto ?: photoUrl,
                    monogram = name.firstOrNull()?.uppercase() ?: "Q",
                    shape = CircleShape,
                    size = 72.dp,
                    monogramSize = 24.sp,
                    ring = false,
                    grayscale = false,
                    scrimAlpha = 0f
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = name,
                    color = GhaisNoir.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        item(key = "discover") {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(76.dp)
                    .noirClickable(onClick = onDiscover)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(GhaisNoir.wellFill())
                        .border(1.dp, GhaisNoir.BorderCard, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = GhaisNoir.Lavender,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Discover",
                    color = GhaisNoir.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * "Your stats" as activity rings + a 7-day chart (2026 standard).
 *
 * Rings read breadth (streak vs 7 days, surahs vs the 114, reciters heard);
 * the bars read REAL per-day minutes from [UserUsageRepository.dailySeconds]
 * keyed by epoch day — never seeded, never padded. Silent days render flat,
 * which is the honest empty state.
 */

@Composable
fun HomeStatsCard() {
    val stats by UserUsageRepository.stats.collectAsState()
    val dailySeconds by UserUsageRepository.dailySeconds.collectAsState()

    val todayEpochDay = remember {
        Clock.System.now().toEpochMilliseconds() / 86_400_000L
    }
    // Last 7 epoch days, oldest → newest: (epochDay, minutes, isToday).
    val week = remember(dailySeconds, todayEpochDay) {
        (6 downTo 0).map { back ->
            val day = todayEpochDay - back
            val minutes = ((dailySeconds[day] ?: 0L) / 60L).toInt().coerceAtLeast(0)
            Triple(day, minutes, back == 0)
        }
    }
    val maxMinutes = (week.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)

    NoirCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatRings(
                    streakFrac = (stats.daysStreak.coerceAtMost(7) / 7f).coerceIn(0f, 1f),
                    surahFrac = (stats.uniqueSurahsCount.coerceAtMost(114) / 114f).coerceIn(0f, 1f),
                    qariFrac = (stats.uniqueRecitersCount.coerceAtMost(10) / 10f).coerceIn(0f, 1f),
                    modifier = Modifier.size(112.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatLegendRow(
                        dot = GhaisNoir.Lavender,
                        label = if (stats.daysStreak == 1) "day streak" else "days streak",
                        value = stats.daysStreak.toString()
                    )
                    StatLegendRow(
                        dot = Color.White.copy(alpha = 0.85f),
                        label = "surahs heard",
                        value = "${stats.uniqueSurahsCount}/114"
                    )
                    StatLegendRow(
                        dot = Color.White.copy(alpha = 0.4f),
                        label = if (stats.uniqueRecitersCount == 1) "qari" else "qaris",
                        value = stats.uniqueRecitersCount.toString()
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            // 7-day minutes chart — flat bars are silent days, never gaps.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                week.forEach { (day, minutes, isToday) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(width = 24.dp, height = 64.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(
                                        if (minutes <= 0) 4.dp
                                        else (8 + (56 * minutes / maxMinutes)).dp
                                    )
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        brush = if (isToday) GhaisNoir.lavenderFill()
                                        else Brush.verticalGradient(
                                            listOf(GhaisNoir.Fill4, GhaisNoir.Fill4)
                                        )
                                    )
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (isToday) "${minutes}m" else dayLetterOf(day),
                            color = if (isToday) GhaisNoir.Lavender else GhaisNoir.TextTertiary,
                            fontSize = 10.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

/** Three concentric progress rings: streak / surahs / qaris. */
@Composable
private fun StatRings(
    streakFrac: Float,
    surahFrac: Float,
    qariFrac: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawArc(
            color = Color.White.copy(alpha = 0.12f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
        )
        val rings = listOf(
            streakFrac to Color(0xFFC9BFF2),
            surahFrac to Color.White.copy(alpha = 0.85f),
            qariFrac to Color.White.copy(alpha = 0.4f)
        )
        var inset = (5 + 7).dp.toPx()
        rings.forEach { (frac, color) ->
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 359.9f * frac,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
            inset += 13.dp.toPx()
        }
    }
}

@Composable
private fun StatLegendRow(dot: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dot)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            color = GhaisNoir.TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            color = GhaisNoir.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Mon..Sun letter for an epoch-day key. Epoch day 0 was a Thursday. */
private fun dayLetterOf(epochDay: Long): String {
    val idx = (((epochDay + 3) % 7) + 7) % 7
    return listOf("M", "T", "W", "T", "F", "S", "S")[idx.toInt()]
}
