package com.ghais.ui.screens.reciters.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.domain.model.Reciter
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.screens.reciters.NoirReciterAvatar
import com.ghais.ui.screens.reciters.photoForSlug
import com.ghais.ui.screens.reciters.rememberCloudPhoto
import com.ghais.ui.theme.GhaisNoir

/**
 * Yesterday's most heard as a top-three podium (2nd • 1st • 3rd):
 * trophy over the champion, overlapping rank badges, plays lines.
 * Each slot opens that qari's profile. Falls back to [VoiceOfDayCard]
 * when [entries] is empty but [fallbackReciter] resolves; renders
 * nothing when both are missing.
 */
@Composable
fun YesterdayMostHeardSection(
    entries: List<HeardEntry>,
    dateLabel: String = "",
    fallbackReciter: Reciter? = null,
    playingSlug: String? = null,
    isPlaying: Boolean = false,
    onOpenProfile: (String) -> Unit,
    onPlay: (String) -> Unit,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) {
        if (fallbackReciter != null) {
            VoiceOfDayCard(
                reciter = fallbackReciter,
                onOpenProfile = onOpenProfile,
                onOpenPlayer = onOpenPlayer,
                modifier = modifier,
            )
            Spacer(Modifier.height(20.dp))
        }
        return
    }
    Column(modifier = modifier.fillMaxWidth()) {
        NoirSectionHeader(
            label = if (dateLabel.isBlank()) "Yesterday's most heard"
            else "Yesterday's most heard • $dateLabel"
        )
        // Top-three podium (leaderboard-ref): 2nd • 1st • 3rd, winner
        // raised center with a trophy. Each taps through to that qari's
        // profile — the page that shows the qari.
        val top = entries.take(3)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            val order = listOfNotNull(
                top.getOrNull(1)?.let { it to 2 },
                top.getOrNull(0)?.let { it to 1 },
                top.getOrNull(2)?.let { it to 3 }
            )
            order.forEach { (entry, rank) ->
                PodiumPerson(
                    entry = entry,
                    rank = rank,
                    champion = rank == 1,
                    onOpenProfile = { onOpenProfile(entry.slug) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

/** Resolved display row for one yesterday-top slug. */
data class HeardEntry(
    val slug: String,
    val nameEn: String,
    /** Local play count; null for the global chart (counts stay server-side). */
    val plays: Int? = null,
)

/**
 * One podium slot: trophy over the champion, portrait with an overlapping
 * rank badge, name, and a short plays line. The whole slot opens the
 * qari's profile.
 */
@Composable
private fun PodiumPerson(
    entry: HeardEntry,
    rank: Int,
    champion: Boolean,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.noirClickable(onClick = onOpenProfile)
    ) {
        if (champion) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = GhaisNoir.Lavender,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(4.dp))
        }
        Box(contentAlignment = Alignment.Center) {
            NoirReciterAvatar(
                photoUrl = photoForSlug(entry.slug) ?: rememberCloudPhoto(entry.slug),
                nameEn = entry.nameEn,
                size = if (champion) 96.dp else 72.dp,
                shape = CircleShape,
                monogramSize = if (champion) 32.sp else 24.sp,
                ring = champion,
                slug = entry.slug,
                scrimAlpha = 0f
            )
            // Rank badge overlapping the portrait's bottom edge.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 10.dp)
                    .size(if (champion) 32.dp else 28.dp)
                    .clip(CircleShape)
                    .background(
                        brush = if (champion) GhaisNoir.lavenderFill()
                        else Brush.verticalGradient(
                            listOf(GhaisNoir.Fill2, GhaisNoir.Fill2)
                        ),
                        shape = CircleShape
                    )
                    .border(
                        1.dp,
                        if (champion) Color.Transparent else GhaisNoir.BorderCard,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    color = if (champion) GhaisNoir.LavenderInk else GhaisNoir.TextSecondary,
                    fontSize = if (champion) 14.sp else 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = entry.nameEn,
            color = GhaisNoir.TextPrimary,
            fontSize = if (champion) 14.sp else 12.sp,
            fontWeight = if (champion) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = playsShort(entry.plays),
            color = if (champion) GhaisNoir.Lavender else GhaisNoir.TextTertiary,
            fontSize = 11.sp,
            fontWeight = if (champion) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

/** Compact plays copy for the podium: "6 plays" / "Most heard". */
private fun playsShort(plays: Int?): String {
    if (plays == null) return "Most heard"
    if (plays == 1) return "1 play"
    return "$plays plays"
}
