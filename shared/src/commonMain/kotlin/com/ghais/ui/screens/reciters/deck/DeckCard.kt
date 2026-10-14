package com.ghais.ui.screens.reciters.deck

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghais.domain.model.Reciter
// A3-owned API (com.ghais.player.ReciterPreview — do NOT create here):
//   object ReciterPreview {
//     fun previewUrl(reciter: Reciter, ayahRef: String = ...): String?
//     fun playPreview(reciter: Reciter, ayahRef: String = ...): String?
//     fun stopPreview(restore: Boolean = true)
//     val isPreviewing: StateFlow<Boolean>   // any preview active
//     val previewSlug: StateFlow<String?>     // active preview slug, null = idle
//   }
import com.ghais.player.ReciterPreview
import com.ghais.ui.screens.reciters.taste.TasteProfile
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.screens.reciters.monogramForReciter
import com.ghais.ui.screens.reciters.rememberCloudPhoto
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

private val DeckArtShape = RoundedCornerShape(24.dp)

/**
 * AGENT A4 — full-card discovery UI for one deck voice.
 *
 * Noir glass card in the reel language (soft fill + ghost border + top-only
 * specular + sheen): big artwork/monogram, nameEn / nameAr, why-line
 * ("Measured pace · Hafs"), optional taste-match chip, preview toggle and the
 * "3 of 10" progress hint with a segmented meter.
 *
 * Auto-plays the 30s preview on appear via [ReciterPreview] and stops it on
 * dispose, so swiping to the next card (or closing the sheet) never leaves
 * audio running behind the new card. Presentation only — swipe/follow/advance
 * decisions live in [DeckSheet].
 *
 * AGENT P7 — additive v2 reason line under the why-line: [reason] wins when
 * supplied, else [deckReason] when [affinities] is non-empty, else nothing
 * (v1 call sites render exactly as before).
 */
@Composable
fun DeckCard(
    reciter: Reciter,
    position: Int,
    total: Int,
    photoUrl: String?,
    taste: TasteProfile? = null,
    affinities: Map<String, Float> = emptyMap(),
    reason: String? = null,
    modifier: Modifier = Modifier
) {
    val whyLine = remember(reciter) { whyLineFor(reciter) }
    val matchReason = remember(reciter, taste) { matchReasonFor(reciter, taste) }
    val deckReasonLine = remember(reciter, affinities, reason) {
        reason ?: if (affinities.isNotEmpty()) deckReason(reciter, affinities) else null
    }

    // Auto-play the 30s sample when this card appears; stop when it leaves.
    LaunchedEffect(reciter.slug) {
        ReciterPreview.playPreview(reciter)
    }
    DisposableEffect(reciter.slug) {
        onDispose { ReciterPreview.stopPreview() }
    }
    val previewActive by ReciterPreview.isPreviewing.collectAsState()
    val previewSlug by ReciterPreview.previewSlug.collectAsState()
    val previewing = previewActive && previewSlug == reciter.slug

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(GhaisShapes.cardNoir)
            .background(GhaisNoir.cardFillSoft())
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.cardNoir)
            .topSpecular()
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(GhaisNoir.sheen())
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DeckProgressHint(position = position, total = total)
            Spacer(Modifier.height(16.dp))

            DeckArtwork(reciter = reciter, photoUrl = photoUrl)

            Spacer(Modifier.height(14.dp))
            Text(
                text = reciter.nameEn,
                color = GhaisNoir.TextPrimary,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = reciter.nameAr,
                color = GhaisNoir.TextSecondary,
                fontSize = 17.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = whyLine,
                color = GhaisNoir.TextTertiary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (deckReasonLine != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = deckReasonLine,
                    color = GhaisNoir.TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (matchReason != null) {
                Spacer(Modifier.height(8.dp))
                DeckMatchChip(text = matchReason)
            }

            Spacer(Modifier.height(14.dp))
            DeckPreviewRow(
                previewing = previewing,
                onToggle = {
                    if (previewing) ReciterPreview.stopPreview()
                    else ReciterPreview.playPreview(reciter)
                }
            )
        }
    }
}

/** "3 of 10" hint + segmented meter (filled through the current card). */
@Composable
private fun DeckProgressHint(position: Int, total: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today's voices",
                color = GhaisNoir.TextTertiary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${position + 1} of $total",
                color = GhaisNoir.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 0 until total) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(
                            if (i <= position) GhaisNoir.TextSecondary
                            else GhaisNoir.Fill4
                        )
                )
            }
        }
    }
}

/** Big artwork well with cloud upgrade + monogram fallback (reel language). */
@Composable
private fun DeckArtwork(reciter: Reciter, photoUrl: String?) {
    val cloudPhoto = rememberCloudPhoto(reciter.slug)
    val artUrl = cloudPhoto ?: photoUrl
    Box(
        modifier = Modifier
            .size(196.dp)
            .clip(DeckArtShape)
            .background(GhaisNoir.wellFill())
            .border(1.dp, GhaisNoir.BorderCard, DeckArtShape),
        contentAlignment = Alignment.Center
    ) {
        if (artUrl != null) {
            AsyncImage(
                model = artUrl,
                contentDescription = reciter.nameEn,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Bottom scrim keeps the card legible over bright portraits.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.28f)
                            )
                        )
                    )
            )
        } else {
            Text(
                text = monogramForReciter(reciter.nameEn),
                color = GhaisNoir.TextPrimary,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Ghost taste-match chip — informational, no press affordance. */
@Composable
private fun DeckMatchChip(text: String) {
    Box(
        modifier = Modifier
            .clip(GhaisShapes.pill)
            .background(GhaisNoir.Fill2)
            .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = GhaisNoir.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Preview toggle: chrome disc while the 30s sample plays, smoked otherwise. */
@Composable
private fun DeckPreviewRow(previewing: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .then(
                    if (previewing) Modifier.background(GhaisNoir.chromeFill())
                    else Modifier.background(Color.Black.copy(alpha = 0.70f))
                )
                .border(
                    1.dp,
                    if (previewing) Color.White.copy(alpha = 0.35f)
                    else GhaisNoir.BorderCard,
                    CircleShape
                )
                .noirClickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (previewing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (previewing) "Pause preview" else "Play preview",
                tint = if (previewing) GhaisNoir.OnChrome else GhaisNoir.TextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Icon(
            imageVector = Icons.Filled.GraphicEq,
            contentDescription = null,
            tint = if (previewing) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (previewing) "Playing 30s sample…" else "30s preview",
            color = if (previewing) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
