package com.quranify.ui.screens.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.quranify.ui.navigation.LocalRootNavigator
import com.quranify.ui.screens.explore.components.DarkObsidian
import com.quranify.ui.screens.explore.components.ExploreBentoCard
import com.quranify.ui.screens.explore.components.HairlineSpecularBorder
import com.quranify.ui.screens.explore.components.HairlineSubtleBorder
import com.quranify.ui.screens.explore.components.OffWhiteText
import com.quranify.ui.screens.explore.components.PureBlack
import com.quranify.ui.screens.explore.components.SystemGrey
import com.quranify.ui.screens.explore.components.isWideCard
import com.quranify.ui.screens.playlists.MoodPlaylist
import com.quranify.ui.screens.playlists.MoodPlaylists
import com.quranify.ui.screens.playlists.PlaylistDetailsScreen
import com.quranify.ui.theme.QuranifyColors

/**
 * ExploreScreen:
 *
 * Designed according to:
 * - iOS Human Interface Guidelines (Apple-style UI): SF-scale Large Title, subheadline, generous corner radii,
 *   continuous curves, 48dp touch targets, Apple-style frosted search bar and category filter pills.
 * - Dark Minimalist UI: Pure black background (#000000), dark obsidian layers (#0D0F12 / #141418), soft off-white text.
 * - Glassmorphism: Translucent frosted glass containers, specular hairline borders, top specular reflection.
 * - Modular Cards with Chunky / Oversized Avatars (76dp - 84dp): Halo glowing borders, instant full-surah queue play.
 * - Migrated Focus Mode: Deep Focus & Study, Deep Work & Flow, and Restful Sleep routines prominently featured.
 */
object ExploreScreen : Tab {
    override val options: TabOptions
        @Composable
        get() = remember {
            TabOptions(
                index = 2u,
                title = "Explore",
                icon = null
            )
        }

    private val Categories = listOf(
        "All",
        "Focus",
        "Work",
        "Sleep",
        "Peace",
        "Healing",
        "Night",
        "Barakah",
        "Reflection"
    )

    @Composable
    override fun Content() {
        val rootNavigator = LocalRootNavigator.current ?: LocalNavigator.current?.parent ?: LocalNavigator.current
        var query by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf("All") }

        val filteredCards = remember(query, selectedCategory) {
            MoodPlaylists.filter { playlist ->
                val matchesQuery = query.isBlank() ||
                    playlist.title.contains(query, ignoreCase = true) ||
                    playlist.description.contains(query, ignoreCase = true)

                val matchesCategory = when (selectedCategory) {
                    "All" -> true
                    "Focus" -> playlist.id in listOf("study-focus", "focus-work")
                    "Work" -> playlist.id in listOf("focus-work", "study-focus")
                    "Sleep" -> playlist.id in listOf("sleep-mode", "tahajjud")
                    "Peace" -> playlist.id in listOf("heart-soothing", "most-beautiful")
                    "Healing" -> playlist.id in listOf("duaa-ruqia", "heart-soothing")
                    "Night" -> playlist.id in listOf("tahajjud", "sleep-mode")
                    "Barakah" -> playlist.id in listOf("sunrise-barakah", "favourites")
                    "Reflection" -> playlist.id in listOf("emotional", "most-beautiful")
                    else -> true
                }

                matchesQuery && matchesCategory
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack)
        ) {
            // iOS SF Large Title & Subtitle
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Explore",
                    color = OffWhiteText,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Curated collections, deep focus routines & sacred themes",
                    color = SystemGrey,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // iOS Glassmorphic Search Bar with 48dp minimum touch target
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(
                        width = 0.5.dp,
                        brush = HairlineSpecularBorder,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = SystemGrey,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = OffWhiteText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(QuranifyColors.Primary),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                text = "Search collections, surahs & moods...",
                                color = SystemGrey,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        inner()
                    },
                    modifier = Modifier.weight(1f)
                )

                AnimatedVisibility(
                    visible = query.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(
                        onClick = { query = "" },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear",
                            tint = SystemGrey,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // iOS Category Filter Pills Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(Categories) { category ->
                    val isSelected = category == selectedCategory
                    val chipShape = RoundedCornerShape(12.dp)

                    Box(
                        modifier = Modifier
                            .heightIn(min = 36.dp)
                            .clip(chipShape)
                            .background(
                                if (isSelected) Color(0xFF1F2937).copy(alpha = 0.90f)
                                else Color.White.copy(alpha = 0.05f)
                            )
                            .border(
                                width = if (isSelected) 1.dp else 0.5.dp,
                                brush = if (isSelected) {
                                    Brush.verticalGradient(
                                        listOf(
                                            QuranifyColors.Primary.copy(alpha = 0.70f),
                                            QuranifyColors.Primary.copy(alpha = 0.20f)
                                        )
                                    )
                                } else HairlineSubtleBorder,
                                shape = chipShape
                            )
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) OffWhiteText else SystemGrey
                        )
                    }
                }
            }

            // Bento Grid of Modular Cards with Chunky / Oversized Avatars
            if (filteredCards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(0.5.dp, HairlineSubtleBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SelfImprovement,
                                contentDescription = null,
                                tint = SystemGrey,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No collections found",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OffWhiteText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try adjusting your search query or category filter.",
                            fontSize = 13.sp,
                            color = SystemGrey
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = filteredCards,
                        key = { it.id },
                        span = { playlist ->
                            if (playlist.isWideCard()) GridItemSpan(2) else GridItemSpan(1)
                        }
                    ) { playlist ->
                        ExploreBentoCard(
                            playlist = playlist,
                            onClick = { rootNavigator?.push(PlaylistDetailsScreen(playlist.id)) }
                        )
                    }
                }
            }
        }
    }
}
