package com.ghais.ui.screens.reciters

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.ghais.data.repository.FollowStore
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.reciterCountryKey
import com.ghais.data.repository.rememberBrowseReciters
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.NoirHeaderSearch
import com.ghais.ui.components.noir.NoirNavBar
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

data class RegionRecitersScreen(val nation: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var searchQuery by remember { mutableStateOf("") }
        val currentTrack by AudioEngine.currentTrack.collectAsState()
        val isPlaying by AudioEngine.isPlaying.collectAsState()
        val followedSlugs by FollowStore.followedSlugs.collectAsState()

        val browseReciters = rememberBrowseReciters()
        val reciters = remember(nation, browseReciters) {
            val wanted = reciterCountryKey(nation)
            browseReciters
                .filter { reciterCountryKey(it.country) == wanted }
                .sortedBy { it.nameEn.trim().lowercase() }
        }
        val filtered = remember(reciters, searchQuery) {
            if (searchQuery.isBlank()) reciters
            else reciters.filter {
                it.nameEn.contains(searchQuery, ignoreCase = true) ||
                    it.nameAr.contains(searchQuery, ignoreCase = true)
            }
        }

        // Header search expansion is screen-owned (ReciterProfileScreen
        // pattern), wired to the same `searchQuery` that drives `filtered`
        // and the in-body engraved pill. Collapse always clears the query,
        // so the list reads unfiltered once the field is hidden.
        var searchExpanded by remember { mutableStateOf(false) }

        NoirScreenRoot {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                NoirNavBar(
                    title = nation,
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

                // Region chip strip — single active chrome pill (selected region).
                // Single-region contract preserved: no multi-region switching.
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item(key = "region_$nation") {
                        RegionChromePill(
                            text = nation,
                            active = true,
                            onClick = {}
                        )
                    }
                    item(key = "region_count") {
                        Box(
                            modifier = Modifier
                                .clip(GhaisShapes.pill)
                                .background(GhaisNoir.Fill2)
                                .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${filtered.size}",
                                color = GhaisNoir.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Engraved search pill (carved-in recessed surface)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.insetFill())
                        .border(1.dp, GhaisNoir.InsetBorder, GhaisShapes.pill)
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = GhaisNoir.TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = GhaisNoir.TextPrimary,
                            fontSize = 14.sp
                        ),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search $nation reciters...",
                                    color = GhaisNoir.TextDisabled,
                                    fontSize = 14.sp
                                )
                            }
                            inner()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp, vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "No reciters found for \"$searchQuery\"" else "No reciters in $nation",
                                color = GhaisNoir.TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Try a different spelling",
                                color = GhaisNoir.TextTertiary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 112.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(key = "header_$nation", span = { GridItemSpan(2) }) {
                            NoirSectionHeader(
                                label = "Reciters • ${filtered.size}",
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        items(
                            items = filtered,
                            key = { it.catalogKey() }
                        ) { reciter ->
                            val photo = rememberCloudPhoto(reciter.slug) ?: photoForSlug(reciter.slug)
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
                                        navigator.push(com.ghais.ui.screens.player.NowPlayingScreen())
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

/**
 * Chrome pill for the region strip — white chrome gradient when active,
 * ghost wash otherwise. Monochrome only.
 */
@Composable
private fun RegionChromePill(
    text: String,
    active: Boolean,
    onClick: () -> Unit
) {
    if (active) {
        Box(
            modifier = Modifier
                .clip(GhaisShapes.pill)
                .background(GhaisNoir.chromeFill())
                .border(1.dp, Color.White.copy(alpha = 0.35f), GhaisShapes.pill)
                .noirClickable(onClick)
                .padding(horizontal = 16.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = GhaisNoir.OnChrome,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        Box(
            modifier = Modifier
                .clip(GhaisShapes.pill)
                .background(GhaisNoir.Fill2)
                .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
                .noirClickable(onClick)
                .padding(horizontal = 16.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = GhaisNoir.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
