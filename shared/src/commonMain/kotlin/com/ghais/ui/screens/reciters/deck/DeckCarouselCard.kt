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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghais.data.repository.FollowStore
import com.ghais.data.share.ShareSheet
import com.ghais.domain.model.Reciter
import com.ghais.player.ReciterPreview
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.screens.reciters.monogramForReciter
import com.ghais.ui.screens.reciters.rememberCloudPhoto
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

private val CarouselPortraitShape = RoundedCornerShape(24.dp)

/**
 * Carousel card for the "Today's voices" deck (audio-app tile, plum tinted).
 *
 * - Top row: listener count (formatted, or "New voice" when unknown) +
 *   status pill ("Top pick" at affinity >= 0.5, else "New voice") +
 *   36dp share circle ("Listen to {name} on Quranify").
 * - Center 180dp portrait (cloud photo upgrade, monogram fallback).
 * - Name + up to 3 tag pills (riwayah / style / country, non-blank only).
 * - One-line description: affinity percent when known, else an honest
 *   "{style} • {country}" fallback (numbers are never fabricated).
 * - Bottom row: lavender Follow pill + 48dp preview disc.
 *
 * Preview lifecycle mirrors [DeckCard]: play on appear, stop on dispose,
 * except autoplay is gated on [autoPlayPreview] so pre-composed pager
 * neighbours never fight the visible page for audio.
 */
@Composable
fun DeckCarouselCard(
    reciter: Reciter,
    photoUrl: String?,
    affinity: Float?,
    followerCount: Long?,
    isInDeck: Boolean,
    autoPlayPreview: Boolean,
    onFollow: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(reciter.slug, autoPlayPreview) {
        if (autoPlayPreview) ReciterPreview.playPreview(reciter)
    }
    DisposableEffect(reciter.slug) {
        onDispose { ReciterPreview.stopPreview() }
    }
    val previewActive by ReciterPreview.isPreviewing.collectAsState()
    val previewSlug by ReciterPreview.previewSlug.collectAsState()
    val previewing = previewActive && previewSlug == reciter.slug

    val cloudPhoto = rememberCloudPhoto(reciter.slug)
    val artUrl = cloudPhoto ?: photoUrl
    val statusText = if ((affinity ?: 0f) >= 0.5f) "Top pick" else "New voice"
    val isTopPick = statusText == "Top pick"
    val followerText = if (followerCount != null) {
        FollowStore.formatFollowerCount(followerCount)
    } else {
        "New voice"
    }
    val description = remember(reciter, affinity) {
        if (affinity != null) {
            "Matches your taste • ${(affinity * 100).toInt()}%"
        } else {
            listOf(reciter.style.trim(), reciter.country.trim())
                .filter { it.isNotBlank() }
                .joinToString(" • ")
                .ifBlank { whyLineFor(reciter) }
        }
    }
    val tags = remember(reciter) {
        listOf(reciter.riwayah.trim(), reciter.style.trim(), reciter.country.trim())
            .filter { it.isNotBlank() }
            .take(3)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(GhaisShapes.cardNoir)
            .background(GhaisNoir.plumFill())
            .border(1.dp, Color.White.copy(alpha = 0.14f), GhaisShapes.cardNoir)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = GhaisNoir.TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = followerText,
                    color = GhaisNoir.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                DeckStatusPill(text = statusText, highlighted = isTopPick)
                Spacer(Modifier.width(8.dp))
                DeckShareButton(
                    onClick = {
                        ShareSheet.shareText(
                            reciter.nameEn,
                            "Listen to ${reciter.nameEn} on Quranify"
                        )
                    }
                )
            }

            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CarouselPortraitShape)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), CarouselPortraitShape),
                contentAlignment = Alignment.Center
            ) {
                if (artUrl != null) {
                    AsyncImage(
                        model = artUrl,
                        contentDescription = reciter.nameEn,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = monogramForReciter(reciter.nameEn),
                        color = GhaisNoir.TextPrimary,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = reciter.nameEn,
                color = GhaisNoir.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(7.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        6.dp,
                        Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (tag in tags) {
                        DeckTagPill(text = tag)
                    }
                }
            }
            Spacer(Modifier.height(5.dp))
            Text(
                text = description,
                color = GhaisNoir.TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.lavenderFill())
                        .noirClickable(onClick = onFollow)
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isInDeck) "Added ✓" else "Follow",
                        color = GhaisNoir.LavenderInk,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .then(
                            if (previewing) Modifier.background(GhaisNoir.chromeFill())
                            else Modifier.background(Color.Black.copy(alpha = 0.55f))
                        )
                        .border(
                            1.dp,
                            if (previewing) Color.White.copy(alpha = 0.35f)
                            else Color.White.copy(alpha = 0.16f),
                            CircleShape
                        )
                        .noirClickable(onClick = {
                            if (previewing) ReciterPreview.stopPreview()
                            else ReciterPreview.playPreview(reciter)
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (previewing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (previewing) "Pause preview" else "Play preview",
                        tint = if (previewing) GhaisNoir.OnChrome else GhaisNoir.TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/** Status pill: lavender wash for "Top pick", ghost for "New voice". */
@Composable
private fun DeckStatusPill(text: String, highlighted: Boolean) {
    Box(
        modifier = Modifier
            .clip(GhaisShapes.pill)
            .background(
                if (highlighted) GhaisNoir.Lavender.copy(alpha = 0.20f)
                else GhaisNoir.Fill2
            )
            .border(
                1.dp,
                if (highlighted) GhaisNoir.Lavender.copy(alpha = 0.45f)
                else GhaisNoir.BorderCard,
                GhaisShapes.pill
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (highlighted) GhaisNoir.Lavender else GhaisNoir.TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

/** Tinted tag pill (riwayah / style / country), 11sp lavender. */
@Composable
private fun DeckTagPill(text: String) {
    Box(
        modifier = Modifier
            .clip(GhaisShapes.pill)
            .background(GhaisNoir.Lavender.copy(alpha = 0.14f))
            .border(1.dp, GhaisNoir.Lavender.copy(alpha = 0.25f), GhaisShapes.pill)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = GhaisNoir.Lavender,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** 36dp share well (NoirNavButton language, smaller for the card row). */
@Composable
private fun DeckShareButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(GhaisNoir.wellFill())
            .border(1.dp, GhaisNoir.BorderCard, CircleShape)
            .noirClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Share,
            contentDescription = "Share voice",
            tint = GhaisNoir.TextPrimary,
            modifier = Modifier.size(17.dp)
        )
    }
}
