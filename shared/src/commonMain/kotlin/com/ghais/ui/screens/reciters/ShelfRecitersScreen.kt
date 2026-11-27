package com.ghais.ui.screens.reciters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ghais.data.repository.FollowStore
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.rememberBrowseReciters
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.NoirHeaderSearch
import com.ghais.ui.components.noir.NoirNavBar
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.theme.GhaisNoir

/** Full-list 2-column grid screen opened from "See all" on Reciter shelves. */
data class ShelfRecitersScreen(
    val title: String,
    val subtitle: String,
    val slugs: List<String>,
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
