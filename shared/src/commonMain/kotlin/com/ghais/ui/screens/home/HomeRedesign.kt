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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * Home redesign — "Noir Lavender Grid".
 *
 * Reference synthesis (user set):
 * - AirPods lavender set: deep plum hero with display headline + pill CTA,
 *   2-col tinted grid, pill filter chips, search + avatar top, price-pill CTA.
 * - Lawyer dark set: near-black canvas, stats row, stacked deck, lavender
 *   selected states, rounded bottom dock.
 * - Discover set: 2-col image grid with scrim overlay + heart action,
 *   full-bleed detail with vertical actions.
 *
 * Roles are strict: black canvas stays; lavender is selected/CTA/hero only.
 */

// ---------------------------------------------------------------------------
// Filter chips — Ref1 "All / AirPods / Accessories" pill row.
// Each chip navigates (no dead controls): All shows everything, the rest
// jump to their dedicated surface.
// ---------------------------------------------------------------------------
data class HomeChip(val id: String, val label: String)

val HomeChips = listOf(
    HomeChip("all", "All"),
    HomeChip("reciters", "Qaris"),
    HomeChip("surahs", "Surahs"),
    HomeChip("moods", "Moods"),
)

@Composable
fun HomeFilterChips(
    selectedId: String,
    onSelect: (HomeChip) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(HomeChips, key = { it.id }) { chip ->
            val selected = chip.id == selectedId
            Box(
                modifier = Modifier
                    .clip(GhaisShapes.pill)
                    .background(
                        if (selected) GhaisNoir.lavenderFill() else GhaisNoir.cardFillSoft()
                    )
                    .border(
                        1.dp,
                        if (selected) Color.Transparent else GhaisNoir.BorderCard,
                        GhaisShapes.pill
                    )
                    .noirClickable { onSelect(chip) }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chip.label,
                    color = if (selected) GhaisNoir.LavenderInk else GhaisNoir.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Plum hero — audio-first resume card: eyebrow pill, entry title, subtitle,
// slim resume progress, then Play pill + circular artwork.
// ---------------------------------------------------------------------------
@Composable
fun HomePlumHero(
    eyebrow: String,
    title: String,
    subtitle: String,
    coverUrl: String,
    monogram: String,
    ctaLabel: String,
    onPlay: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
) {
    val clamped = progress.coerceIn(0f, 1f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(GhaisNoir.plumFill(), GhaisShapes.cardLarge)
            .border(1.dp, GhaisNoir.Lavender.copy(alpha = 0.35f), GhaisShapes.cardLarge)
            .topSpecular(inset = 30.dp)
            .noirClickable(onClick = onOpen)
            .padding(20.dp)
    ) {
        // Eyebrow pill — "Jump back in".
        Box(
            modifier = Modifier
                .clip(GhaisShapes.pill)
                .background(Color.White.copy(alpha = 0.16f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = eyebrow,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 30.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 13.sp,
            maxLines = 2
        )
        if (clamped > 0f) {
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(GhaisShapes.pill)
                    .background(Color.White.copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(clamped)
                        .height(4.dp)
                        .clip(GhaisShapes.pill)
                        .background(Color.White)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(GhaisShapes.pill)
                    .background(GhaisNoir.lavenderFill())
                    .noirClickable(onClick = onPlay)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = GhaisNoir.LavenderInk,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = ctaLabel,
                    color = GhaisNoir.LavenderInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.weight(1f))
            NoirArtworkWell(
                coverUrl = coverUrl,
                monogram = monogram,
                shape = CircleShape,
                size = 96.dp,
                monogramSize = 30.sp,
                ring = true,
                scrimAlpha = 0f
            )
        }
    }
}
