package com.ghais.ui.screens.library

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
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
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * AGENT A11 — Library resurrection rail ("Hear what you missed").
 *
 * Pure core: [findUnplayed]. UI: [ResurrectionRail] + [ResurrectionCard].
 * New file only — LibraryScreen wires it via the insertion snippet in the
 * agent report (no edits made here to existing files).
 *
 * Identity model: saved/played ids are opaque caller-owned keys
 * (e.g. `UserPlaylists.trackKeyOf` = `"<reciterSlug>:<surahId>:<ayahNo>"`).
 * This file never resolves artwork or catalog entries — the caller maps an
 * id to a display label via [resolveLabel] or passes pre-resolved labels.
 */

/**
 * Returns up to [limit] ids from [savedIds] (order-preserved, de-duplicated)
 * that do not appear in [playHistory].
 *
 * - Blank ids are dropped (never surfaced as cards).
 * - [limit] <= 0 yields an empty list; negative limits never throw.
 * - Pure: no I/O, no clock, no platform APIs — safe under commonTest.
 */
fun findUnplayed(
    savedIds: List<String>,
    playHistory: Collection<String>,
    limit: Int = 8
): List<String> {
    if (limit <= 0 || savedIds.isEmpty()) return emptyList()
    val played = try {
        playHistory.toSet()
    } catch (_: Exception) {
        emptySet()
    }
    val out = LinkedHashSet<String>(minOf(limit, savedIds.size).coerceAtLeast(1))
    for (raw in savedIds) {
        val id = raw.trim()
        if (id.isEmpty()) continue
        if (id in played) continue
        if (!out.add(id)) continue
        if (out.size >= limit) break
    }
    return out.toList()
}

private val ResurrectionCardShape = RoundedCornerShape(22.dp)

/**
 * Noir resurrection rail. Hidden (emits nothing) when [unplayedIds] is empty.
 *
 * @param unplayedIds ids to surface, already in display order
 *   (typically `findUnplayed(...)` output).
 * @param onPlay one-tap play callback receiving the tapped id.
 * @param resolveLabel maps an id to a human label; defaults to the raw id.
 */
@Composable
fun ResurrectionRail(
    unplayedIds: List<String>,
    onPlay: (String) -> Unit,
    modifier: Modifier = Modifier,
    resolveLabel: (String) -> String = { it }
) {
    if (unplayedIds.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        NoirSectionHeader(label = "Hear what you missed • ${unplayedIds.size}")
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(unplayedIds, key = { it }) { id ->
                ResurrectionCard(
                    id = id,
                    label = resolveLabel(id),
                    onPlay = { onPlay(id) }
                )
            }
        }
    }
}

@Composable
private fun ResurrectionCard(
    id: String,
    label: String,
    onPlay: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(148.dp)
            .clip(ResurrectionCardShape)
            .background(GhaisNoir.cardFillSoft())
            .border(1.dp, GhaisNoir.BorderCard, ResurrectionCardShape)
            .topSpecular(inset = 20.dp)
            .noirClickable(onPlay)
    ) {
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
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GhaisNoir.wellFill())
                        .border(1.dp, GhaisNoir.BorderCard, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = GhaisNoir.TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GhaisNoir.chromeFill())
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play $id",
                        tint = GhaisNoir.OnChrome,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp,
                color = GhaisNoir.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Saved • Unplayed",
                fontSize = 11.5.sp,
                color = GhaisNoir.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
