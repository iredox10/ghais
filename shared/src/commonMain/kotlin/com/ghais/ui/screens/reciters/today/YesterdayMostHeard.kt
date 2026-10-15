package com.ghais.ui.screens.reciters.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.domain.model.Reciter
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.screens.reciters.NoirReciterAvatar
import com.ghais.ui.screens.reciters.photoForSlug
import com.ghais.ui.screens.reciters.rememberCloudPhoto
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * Yesterday's most heard: compact ranked rows (rank + avatar + plays +
 * play pill), NOT hero cards. Ghost-weight by design — the deck entry above
 * owns the chrome. Falls back to [VoiceOfDayCard] when [entries] is empty
 * but [fallbackReciter] resolves; renders nothing when both are missing.
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
        entries.forEachIndexed { index, entry ->
            YesterdayHeardRow(
                rank = index + 1,
                entry = entry,
                activelyPlaying = playingSlug == entry.slug && isPlaying,
                onOpenProfile = { onOpenProfile(entry.slug) },
                onPlay = { onPlay(entry.slug) },
            )
            if (index < entries.size - 1) Spacer(Modifier.height(10.dp))
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

/** Row subtitle: global entries have no local count (stays server-side). */
private fun playsSubtitle(plays: Int?): String {
    if (plays == null) return "Most heard yesterday"
    if (plays == 1) return "1 play yesterday"
    return "$plays plays yesterday"
}

@Composable
private fun YesterdayHeardRow(
    rank: Int,
    entry: HeardEntry,
    activelyPlaying: Boolean,
    onOpenProfile: () -> Unit,
    onPlay: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GhaisShapes.row)
            .background(GhaisNoir.cardFillSoft(), GhaisShapes.row)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.row)
            .topSpecular(inset = 22.dp)
            .noirClickable(onClick = onOpenProfile)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$rank",
            color = GhaisNoir.TextTertiary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(20.dp)
        )
        NoirReciterAvatar(
            photoUrl = photoForSlug(entry.slug) ?: rememberCloudPhoto(entry.slug),
            nameEn = entry.nameEn,
            size = 44.dp,
            shape = CircleShape,
            monogramSize = 18.sp,
            ring = false,
            slug = entry.slug,
            grayscale = true,
            scrimAlpha = 0f
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.nameEn,
                color = GhaisNoir.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = playsSubtitle(entry.plays),
                color = GhaisNoir.TextTertiary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .then(
                    if (activelyPlaying) Modifier.background(GhaisNoir.chromeFill())
                    else Modifier.background(Color.Black.copy(alpha = 0.55f))
                )
                .border(
                    1.dp,
                    if (activelyPlaying) Color.White.copy(alpha = 0.35f)
                    else GhaisNoir.BorderCard,
                    CircleShape
                )
                .noirClickable(onClick = onPlay),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (activelyPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (activelyPlaying) "Pause" else "Play",
                tint = if (activelyPlaying) GhaisNoir.OnChrome else GhaisNoir.TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
