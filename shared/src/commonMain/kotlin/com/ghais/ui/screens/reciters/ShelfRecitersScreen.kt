package com.ghais.ui.screens.reciters

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ghais.data.repository.FollowStore
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.rememberBrowseReciters
import com.ghais.domain.model.Reciter
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.NoirHeaderSearch
import com.ghais.ui.components.noir.NoirNavBar
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.screens.home.NoirArtworkWell
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * Full-list screen opened from "See all" on Reciter shelves.
 * - Default (`showRankedMosaic = false`): 2-column `ReciterReelCard` grid.
 * - Ranked mosaic (`showRankedMosaic = true`): Top 3 podium (#2, #1, #3) at top
 *   followed by a 3-column circular mosaic for #4+.
 */
data class ShelfRecitersScreen(
    val title: String,
    val subtitle: String,
    val slugs: List<String>,
    val showRankedMosaic: Boolean = false,
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var searchQuery by remember { mutableStateOf("") }
        var searchExpanded by remember { mutableStateOf(false) }
        val followedSlugs by FollowStore.followedSlugs.collectAsState()

        val browseReciters = rememberBrowseReciters()
        val reciters = remember(slugs, browseReciters) {
            val bySlug = browseReciters.associateBy { it.slug }
            slugs.mapNotNull { bySlug[it] }
        }
        val filtered = remember(reciters, searchQuery) {
            if (searchQuery.isBlank()) reciters
            else reciters.filter {
                it.nameEn.contains(searchQuery, ignoreCase = true) ||
                    it.nameAr.contains(searchQuery, ignoreCase = true)
            }
        }

        NoirScreenRoot {
            Column(modifier = Modifier.fillMaxSize()) {
                NoirNavBar(
                    title = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    onBack = { navigator.pop() },
                    backIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    actions = {
                        NoirHeaderSearch(
                            expanded = searchExpanded,
                            onExpandedChange = { expanded ->
                                searchExpanded = expanded
                                if (!expanded) searchQuery = ""
                            },
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholder = "Search reciters…"
                        )
                    },
                    titleAlignLeft = true
                )

                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        color = GhaisNoir.TextTertiary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                if (showRankedMosaic) {
                    val showPodium = searchQuery.isBlank() && filtered.size >= 3
                    val podium = if (showPodium) filtered.take(3) else emptyList()
                    val rest = if (showPodium) filtered.drop(3) else filtered
                    val rankOffset = if (showPodium) 4 else 1

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        item(key = "mosaic_header_$title", span = { GridItemSpan(3) }) {
                            NoirSectionHeader(
                                label = "Reciters • ${filtered.size}",
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        if (showPodium) {
                            item(key = "mosaic_podium", span = { GridItemSpan(3) }) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp, bottom = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    // 2nd • 1st • 3rd podium order
                                    podium.getOrNull(1)?.let { second ->
                                        RankedMosaicTile(
                                            reciter = second,
                                            rank = 2,
                                            circleSize = 86.dp,
                                            modifier = Modifier.weight(1f),
                                            onClick = { navigator.push(ReciterProfileScreen(second.slug)) }
                                        )
                                    }
                                    podium.getOrNull(0)?.let { first ->
                                        RankedMosaicTile(
                                            reciter = first,
                                            rank = 1,
                                            circleSize = 108.dp,
                                            modifier = Modifier
                                                .weight(1.1f)
                                                .padding(bottom = 14.dp),
                                            onClick = { navigator.push(ReciterProfileScreen(first.slug)) }
                                        )
                                    }
                                    podium.getOrNull(2)?.let { third ->
                                        RankedMosaicTile(
                                            reciter = third,
                                            rank = 3,
                                            circleSize = 86.dp,
                                            modifier = Modifier.weight(1f),
                                            onClick = { navigator.push(ReciterProfileScreen(third.slug)) }
                                        )
                                    }
                                }
                            }
                        }
                        itemsIndexed(items = rest, key = { _, it -> it.catalogKey() }) { idx, reciter ->
                            RankedMosaicTile(
                                reciter = reciter,
                                rank = rankOffset + idx,
                                circleSize = 84.dp,
                                onClick = { navigator.push(ReciterProfileScreen(reciter.slug)) }
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(key = "shelf_header_$title", span = { GridItemSpan(2) }) {
                            NoirSectionHeader(
                                label = "Reciters • ${filtered.size}",
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        items(items = filtered, key = { it.catalogKey() }) { reciter ->
                            val photo = rememberCloudPhoto(reciter.slug) ?: reciter.imageUrl
                            ReciterReelCard(
                                reciter = reciter,
                                photoUrl = photo,
                                onClick = { navigator.push(ReciterProfileScreen(reciter.slug)) },
                                onPlayClick = {
                                    val surahs = QuranDataRepository.getSurahsForReciter(reciter)
                                        .filter { reciter.isSurahAvailable(it.id) }
                                    val tracks = surahs.map { surah ->
                                        TrackItem(
                                            reciterSlug = reciter.slug,
                                            reciterName = reciter.nameEn,
                                            surahId = surah.id,
                                            surahNameEn = surah.nameEn,
                                            surahNameAr = surah.nameAr,
                                            ayahNo = 0,
                                            audioUrl = reciter.getFullSurahUrl(surah.id),
                                            textUthmani = "",
                                            durationMs = surah.ayahsCount * 15_000L
                                        )
                                    }
                                    if (tracks.isNotEmpty()) {
                                        AudioEngine.playQueue(tracks, startIndex = 0)
                                        navigator.push(NowPlayingScreen())
                                    }
                                },
                                isFollowing = reciter.slug in followedSlugs,
                                onFollowClick = { FollowStore.toggle(reciter.slug) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RankedMosaicTile(
    reciter: Reciter,
    rank: Int,
    circleSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val photo = rememberCloudPhoto(reciter.slug) ?: reciter.imageUrl
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.noirClickable(onClick)
    ) {
        Box(
            modifier = Modifier
                .clip(GhaisShapes.pill)
                .background(
                    brush = if (rank == 1) GhaisNoir.lavenderFill()
                    else Brush.verticalGradient(listOf(GhaisNoir.Fill2, GhaisNoir.Fill2)),
                    shape = GhaisShapes.pill
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "#$rank",
                color = if (rank == 1) GhaisNoir.LavenderInk else GhaisNoir.TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(8.dp))
        NoirArtworkWell(
            coverUrl = photo.orEmpty(),
            monogram = reciter.nameEn.firstOrNull()?.uppercase() ?: "Q",
            shape = CircleShape,
            size = circleSize,
            monogramSize = if (rank == 1) 34.sp else 28.sp,
            ring = rank == 1,
            grayscale = false,
            scrimAlpha = 0f
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = reciter.nameEn,
            color = GhaisNoir.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp
        )
    }
}
