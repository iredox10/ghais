package com.ghais.ui.screens.reciters.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.repository.ChartsRepository
import com.ghais.data.repository.QuranDataRepository
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.screens.home.NoirArtworkWell
import com.ghais.ui.screens.reciters.art.ReciterArtwork
import com.ghais.ui.screens.reciters.monogramForReciter
import com.ghais.ui.screens.reciters.photoForSlug
import com.ghais.ui.screens.reciters.rememberCloudPhoto
import com.ghais.ui.theme.GhaisNoir

/**
 * Agent A6 — Top Qaris rail for the Reciters tab.
 *
 * Same recipe as Home's `HomeTopQarisShelf` (see HomeDiscovery.kt):
 * `snapshot.topQaris7d` slugs resolved through the nullable
 * [QuranDataRepository.getBrowseReciterBySlug] (unresolvable slugs skipped,
 * never borrowed), refreshed once per screen entry. Renders NOTHING —
 * including its own spacing — when the snapshot is null/unfetched or no slug
 * resolves (offline, empty charts), so the host layout never gaps.
 *
 * Artwork falls back to the deterministic [ReciterArtwork] when neither the
 * cloud nor the local [photoForSlug] has a portrait.
 */
@Composable
fun TopQarisRail(
    onReciter: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) { ChartsRepository.refresh() }
    val snapshot by ChartsRepository.snapshot.collectAsState()
    val reciters = remember(snapshot) {
        try {
            snapshot?.topQaris7d.orEmpty()
                .mapNotNull { QuranDataRepository.getBrowseReciterBySlug(it) }
                .distinctBy { it.slug }
        } catch (_: Exception) {
            emptyList()
        }
    }
    if (reciters.isEmpty()) return

    Column(modifier = modifier) {
        NoirSectionHeader(label = "Top Qaris this week")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(reciters, key = { it.slug }) { reciter ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(96.dp)
                        .noirClickable { onReciter(reciter.slug) }
                ) {
                    val photo = rememberCloudPhoto(reciter.slug)
                    val local = remember(reciter.slug) { photoForSlug(reciter.slug) }
                    val artUrl = photo ?: local
                    if (artUrl != null) {
                        NoirArtworkWell(
                            coverUrl = artUrl,
                            monogram = monogramForReciter(reciter.nameEn),
                            shape = CircleShape,
                            size = 84.dp,
                            monogramSize = 28.sp,
                            ring = true,
                            grayscale = false,
                            scrimAlpha = 0f
                        )
                    } else {
                        ReciterArtwork(
                            slug = reciter.slug,
                            size = 84.dp,
                            shape = CircleShape,
                            monogram = monogramForReciter(reciter.nameEn)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = reciter.nameEn,
                        color = GhaisNoir.TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
