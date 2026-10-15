package com.ghais.personal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.domain.model.Reciter
import com.ghais.ui.screens.reciters.ReciterReelCard
import com.ghais.ui.theme.GhaisNoir

/**
 * AGENT P3 — Noir horizontal rail rendering one personalized [Shelf].
 *
 * Header mirrors the nation-reel treatment (22sp extrabold title + 13sp
 * why-[Shelf.subtitle]), cards reuse the public [ReciterReelCard] — the same
 * Noir glass card (soft fill, ghost border, top specular, play + follow
 * actions) the reciter reels use. (The `NoirReciterCard` in RecitersScreen is
 * private to that file, so it is not reusable here.)
 *
 * Horizontal padding is intentionally minimal (2.dp): the host LazyColumn
 * (RecitersScreen) already pads the screen edges, matching `NoirNationReel`.
 * Renders nothing when none of [shelf.slugs] resolve in [reciterBySlug].
 */
@Composable
fun ShelfRow(
    shelf: Shelf,
    reciterBySlug: Map<String, Reciter>,
    photoFor: (String) -> String?,
    followedSlugs: Set<String>,
    onReciter: (String) -> Unit,
    onPlayReciter: (Reciter) -> Unit = {},
    onToggleFollow: (String) -> Unit = {},
    followerCountFor: (String) -> Long? = { null },
) {
    val cards = remember(shelf, reciterBySlug) {
        shelf.slugs.mapNotNull { reciterBySlug[it] }
    }
    if (cards.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = shelf.title,
            color = GhaisNoir.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = shelf.subtitle,
            color = GhaisNoir.TextSecondary,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(items = cards, key = { it.catalogKey() }) { reciter ->
                ReciterReelCard(
                    reciter = reciter,
                    photoUrl = photoFor(reciter.slug),
                    onClick = { onReciter(reciter.slug) },
                    onPlayClick = { onPlayReciter(reciter) },
                    isFollowing = reciter.slug in followedSlugs,
                    onFollowClick = { onToggleFollow(reciter.slug) },
                    followerCount = followerCountFor(reciter.slug),
                )
            }
        }
    }
}
