package com.quranify.ui.screens.reciters

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.quranify.data.repository.QuranDataRepository
import com.quranify.data.seed.QuranData
import com.quranify.domain.model.TrackItem
import com.quranify.player.AudioEngine
import com.quranify.ui.navigation.LocalRootNavigator

private val PureBlack = Color(0xFF000000)

class RecitersScreen : Tab {
    override val options: TabOptions
        @Composable
        get() = remember {
            TabOptions(
                index = 1u,
                title = "Reciters",
                icon = null
            )
        }

    @Composable
    override fun Content() {
        // Push onto the ROOT navigator: the local one here is a TabNavigator,
        // which can only render Tabs (pushing a plain Screen crashes in CurrentTab).
        val rootNavigator = LocalRootNavigator.current
            ?: LocalNavigator.current?.parent
            ?: LocalNavigator.current
        var searchQuery by remember { mutableStateOf("") }

        val groups = remember(searchQuery) {
            QuranData.RECITERS
                .filter {
                    searchQuery.isBlank() ||
                        it.nameEn.contains(searchQuery, ignoreCase = true) ||
                        it.nameAr.contains(searchQuery, ignoreCase = true) ||
                        it.country.contains(searchQuery, ignoreCase = true)
                }
                .groupBy { it.country.ifBlank { "Other" } }
                .entries
                .sortedByDescending { (_, reciters) -> reciters.size }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack)
        ) {
            RecitersHero(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                countText = "${QuranData.RECITERS.size} reciters • ${groups.size} nations"
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 112.dp)
            ) {
                groups.forEach { (nation, reciters) ->
                    item(key = "reel_$nation") {
                        NationReelBlock(
                            nation = nation,
                            reciters = reciters,
                            photoFor = ::photoForSlug,
                            onSeeAll = { rootNavigator?.push(RegionRecitersScreen(nation)) },
                            onReciter = { slug -> rootNavigator?.push(ReciterProfileScreen(slug)) },
                            onPlayReciter = { reciter ->
                                val surahs = QuranDataRepository.getSurahsForReciter(reciter)
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
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
