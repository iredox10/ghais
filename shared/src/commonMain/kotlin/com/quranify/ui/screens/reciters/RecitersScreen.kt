package com.quranify.ui.screens.reciters

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.quranify.data.seed.QuranData

private val PureBlack = Color(0xFF000000)

// Preferred display order for nation groups
private val NationOrder = listOf("Saudi Arabia", "Egypt", "Kuwait")

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
        val navigator = LocalNavigator.currentOrThrow
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
                .toSortedMap(compareBy { NationOrder.indexOf(it).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE })
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
                            onSeeAll = { navigator.push(RegionRecitersScreen(nation)) },
                            onReciter = { slug -> navigator.push(ReciterProfileScreen(slug)) }
                        )
                    }
                }
            }
        }
    }
}
