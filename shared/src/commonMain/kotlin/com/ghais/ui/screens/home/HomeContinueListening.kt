package com.ghais.ui.screens.home

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.UserUsageRepository
import com.ghais.data.seed.JumpBackInItem
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.components.noir.NoirHeroCard
import com.ghais.ui.components.noir.NoirSegmentedProgress
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.screens.reciters.rememberCloudPhoto
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * Phase 4 — "Continue listening".
 *
 * Bento split: the most recent entry becomes a hero plate (artwork + progress
 * meter + chromium Resume), the remainder becomes a rail of compact plates.
 * The live row is signalled by fill elevation, a brighter specular border and a
 * chromium disc — never by the old emerald accent or animated colour borders.
 */
@Composable
fun HomeContinueListeningRow(onPlay: (JumpBackInItem) -> Unit) {
    val history by UserUsageRepository.history.collectAsState()
    val currentTrack by AudioEngine.currentTrack.collectAsState()
    val isEnginePlaying by AudioEngine.isPlaying.collectAsState()

    // Latest-first at read: hero = single newest, rail follows newest-first.
    // sortedByDescending is stable, so timestamp ties keep store (MRU-first) order.
    val recent = remember(history) { history.sortedByDescending { it.lastPlayedTimestampMs }.take(7) }

    if (recent.isEmpty()) {
        NoirCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Nothing queued yet",
                    color = GhaisNoir.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Play any surah and your place will be kept here.",
                    color = GhaisNoir.TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
        return
    }

    val hero = recent.first()
    val rail = recent.drop(1)
    val heroActive = isLive(hero, currentTrack?.surahId, currentTrack?.reciterSlug)
    val heroPhoto = rememberCloudPhoto(hero.reciterSlug)

    NoirHeroCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (heroPhoto != null) {
                    NoirArtworkWell(
                        coverUrl = heroPhoto,
                        monogram = reciterMonogramOf(hero),
                        shape = CircleShape,
                        size = 76.dp,
                        monogramSize = 26.sp,
                        ring = true,
                        grayscale = false,
                        scrimAlpha = 0f,
                        errorFallbackUrl = hero.coverUrl
                    )
                } else {
                    NoirArtworkWell(
                        coverUrl = hero.coverUrl,
                        monogram = monogramOf(hero),
                        shape = RoundedCornerShape(20.dp),
                        size = 76.dp,
                        monogramSize = 26.sp,
                        ring = heroActive
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LAST PLAYED",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = hero.title,
                        color = GhaisNoir.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = hero.subtitle,
                        color = if (heroActive) GhaisNoir.TextPrimary else GhaisNoir.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (heroActive) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            NoirSegmentedProgress(progress = progressOf(hero), trackHeight = 8.dp)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${(progressOf(hero) * 100).toInt()}% through",
                    color = GhaisNoir.TextTertiary,
                    fontSize = 11.sp
                )
                val remaining = hero.durationMs - hero.positionMs
                if (hero.durationMs > 0L && remaining > 0L) {
                    Text(
                        text = "${formatClock(remaining)} left",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            ChromePillButton(
                text = if (heroActive && isEnginePlaying) "Now playing" else "Resume",
                onClick = { onPlay(hero) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = if (heroActive && isEnginePlaying) {
                    Icons.Filled.Pause
                } else {
                    Icons.Filled.PlayArrow
                }
            )
        }
    }

    if (rail.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(
                items = rail,
                key = { index, item -> "${item.reciterSlug}-${item.surahId}#$index" }
            ) { _, item ->
                val active = isLive(item, currentTrack?.surahId, currentTrack?.reciterSlug)
                NoirRecentPlate(
                    item = item,
                    active = active,
                    playing = active && isEnginePlaying,
                    onPlay = { onPlay(item) }
                )
            }
        }
    }
}

/** Compact rail plate: artwork, dual text, engraved progress, state disc. */
@Composable
private fun NoirRecentPlate(
    item: JumpBackInItem,
    active: Boolean,
    playing: Boolean,
    onPlay: () -> Unit
) {
    val reciterPhoto = rememberCloudPhoto(item.reciterSlug)

    Row(
        modifier = Modifier
            .width(272.dp)
            .background(
                if (active) GhaisNoir.cardFillActive() else GhaisNoir.cardFillSoft(),
                GhaisShapes.row
            )
            .border(
                1.dp,
                if (active) GhaisNoir.SpecularTop else GhaisNoir.BorderCard,
                GhaisShapes.row
            )
            .topSpecular(inset = 22.dp)
            .noirClickable(onPlay)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (reciterPhoto != null) {
            NoirArtworkWell(
                coverUrl = reciterPhoto,
                monogram = reciterMonogramOf(item),
                shape = CircleShape,
                size = 52.dp,
                monogramSize = 18.sp,
                ring = true,
                grayscale = false,
                scrimAlpha = 0f,
                errorFallbackUrl = item.coverUrl
            )
        } else {
            NoirArtworkWell(
                coverUrl = item.coverUrl,
                monogram = monogramOf(item),
                shape = RoundedCornerShape(14.dp),
                size = 52.dp,
                monogramSize = 18.sp,
                ring = active
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = GhaisNoir.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = item.subtitle,
                color = GhaisNoir.TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            NoirSegmentedProgress(progress = progressOf(item), trackHeight = 4.dp)
        }
        Spacer(Modifier.width(10.dp))
        // Chromium disc while live, ghost well otherwise — fill carries the state.
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    if (playing) GhaisNoir.chromeFill() else GhaisNoir.wellFill(),
                    GhaisShapes.well
                )
                .border(
                    1.dp,
                    if (playing) Color.White.copy(alpha = 0.4f) else GhaisNoir.BorderCard,
                    GhaisShapes.well
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (playing) "Playing" else "Resume",
                tint = if (playing) GhaisNoir.OnChrome else GhaisNoir.TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun progressOf(item: JumpBackInItem): Float = item.progress.coerceIn(0f, 1f)

private fun monogramOf(item: JumpBackInItem): String =
    item.title.firstOrNull()?.uppercase() ?: "Q"

private fun reciterMonogramOf(item: JumpBackInItem): String =
    QuranDataRepository.getReciterBySlug(item.reciterSlug).nameEn.firstOrNull()?.uppercase() ?: "Q"

private fun isLive(item: JumpBackInItem, surahId: Int?, reciterSlug: String?): Boolean =
    surahId != null && reciterSlug != null &&
        item.surahId == surahId && item.reciterSlug == reciterSlug

/** mm:ss read-out for the progress labels. */
private fun formatClock(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${if (seconds < 10) "0$seconds" else "$seconds"}"
}
