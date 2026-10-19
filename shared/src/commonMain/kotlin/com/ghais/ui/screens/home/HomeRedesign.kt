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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
 *   2-col tinted grid, search + avatar top, price-pill CTA.
 * - Lawyer dark set: near-black canvas, stats row, stacked deck, lavender
 *   selected states, rounded bottom dock.
 * - Discover set: 2-col image grid with scrim overlay + heart action,
 *   full-bleed detail with vertical actions.
 *
 * Roles are strict: black canvas stays; lavender is selected/CTA/hero only.
 */

// ---------------------------------------------------------------------------
// Plum hero — minimal resume card per user request: "show the qari image then
// the progress bar ... make it minimal. add see all". Qari photo + inline
// title/subtitle/progress sit in one row; progress bar is ALWAYS visible
// (an empty track at progress 0 is intended, not a bug).
// ---------------------------------------------------------------------------
@Composable
fun HomePlumHero(
    eyebrow: String,
    title: String,
    subtitle: String,
    imageUrl: String,          // the QARI photo (was coverUrl)
    monogram: String,
    ctaLabel: String,          // "Play" or "Resume" (caller decides)
    onPlay: () -> Unit,
    onOpen: () -> Unit,
    onSeeAll: () -> Unit,      // NEW — right-aligned history entry point
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
            .padding(16.dp)
    ) {
        // Eyebrow row: "Jump back in" pill left, "See all" right — the user
        // asked for an explicit See all; nested click stops at this control,
        // everything else still opens the card via onOpen.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
            Text(
                text = "See all",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier
                    // semantics, not contentDescription param: Text has none,
                    // and screen readers need a label distinct from "See all".
                    .semantics { contentDescription = "See all history" }
                    .noirClickable(onClick = onSeeAll)
                    .padding(start = 8.dp) // touch target breathing room
            )
        }
        Spacer(Modifier.height(8.dp))
        // Single content row: qari image first, then text + progress — the
        // exact order the user described, replacing the stacked display title.
        Row(verticalAlignment = Alignment.CenterVertically) {
            NoirArtworkWell(
                coverUrl = imageUrl,
                monogram = monogram,
                shape = CircleShape,
                size = 56.dp,
                monogramSize = 18.sp, // scaled to the 56dp well
                ring = false,
                scrimAlpha = 0f
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                // Always-visible slim progress: an empty track at 0 keeps the
                // image → bar rhythm stable instead of popping in later.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(GhaisShapes.pill)
                        .background(Color.White.copy(alpha = 0.25f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(clamped)
                            .height(3.dp)
                            .clip(GhaisShapes.pill)
                            .background(Color.White)
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        // Compact play pill, left-aligned — no trailing artwork well anymore,
        // so the weight spacer goes too.
        Row(
            modifier = Modifier
                .clip(GhaisShapes.pill)
                .background(GhaisNoir.lavenderFill())
                .noirClickable(onClick = onPlay)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = GhaisNoir.LavenderInk,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = ctaLabel,
                color = GhaisNoir.LavenderInk,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
