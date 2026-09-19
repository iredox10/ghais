package com.quranify.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import coil3.compose.AsyncImage
import com.quranify.data.seed.QuranData
import com.quranify.data.seed.StitchAssets
import com.quranify.domain.model.RoutineModeType
import com.quranify.domain.model.TrackItem
import com.quranify.player.AudioEngine
import com.quranify.ui.navigation.LocalRootNavigator
import com.quranify.ui.screens.curated.AllCuratedPlaylistsScreen
import com.quranify.ui.screens.library.FavoritesScreen
import com.quranify.ui.screens.mood.RoutineModeScreen
import com.quranify.ui.screens.reciters.RecitersScreen
import com.quranify.ui.screens.reciters.ReciterProfileScreen
import com.quranify.ui.screens.search.SearchScreen
import com.quranify.ui.screens.settings.SettingsScreen
import com.quranify.ui.screens.surah.SurahDetailScreen

// Reference-design tokens: pure black + frosted glass + blue accent
private val PureBlack = Color(0xFF000000)
private val DarkCard = Color(0xFF1C1C1E)
private val GlassBg = Color.White.copy(alpha = 0.09f)
private val GlassBorder = Color.White.copy(alpha = 0.14f)
private val LinkBlue = Color(0xFF4C8DFF)
private val BadgeGreen = Color(0xFF30D158)
private val MutedGrey = Color(0xFF9A9AA0)

object HomeScreen : Tab {
    override val options: TabOptions
        @Composable
        get() = remember {
            TabOptions(
                index = 0u,
                title = "Home",
                icon = null
            )
        }

    @Composable
    override fun Content() {
        val rootNavigator = LocalRootNavigator.current ?: LocalNavigator.current?.parent ?: LocalNavigator.current
        val tabNavigator = LocalTabNavigator.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 112.dp)
            ) {
                item { HomeTopBarReplica(
                    onPremiumClick = { tabNavigator?.let { it.current = SettingsScreen } },
                    onStatsClick = { rootNavigator?.push(SearchScreen()) }
                ) }
                item { FeaturedHero(onClick = { rootNavigator?.push(ReciterProfileScreen("mishary")) }) }
                item {
                    SectionHeader(
                        title = "Playlists",
                        onSeeAll = { tabNavigator?.let { it.current = com.quranify.ui.screens.library.LibraryScreen } }
                    )
                }
                item {
                    PlaylistsRow(
                        onFavourites = { rootNavigator?.push(FavoritesScreen) },
                        onFocusWork = { rootNavigator?.push(AllCuratedPlaylistsScreen()) },
                        onNightSleep = { rootNavigator?.push(RoutineModeScreen(RoutineModeType.SLEEP)) }
                    )
                }
                item {
                    SectionHeader(
                        title = "Newly added",
                        onSeeAll = { rootNavigator?.push(RecitersScreen()) }
                    )
                }
                item {
                    NewlyAddedRow(onReciter = { slug -> rootNavigator?.push(ReciterProfileScreen(slug)) })
                }
                item { ListeningStatsStrip() }
                item { SectionHeader(title = "Continue listening", onSeeAll = null) }
                item {
                    ContinueListeningRow(onPlay = { title ->
                        AudioEngine.playTrack(
                            TrackItem(
                                reciterSlug = "alafasy",
                                reciterName = "Mishary Rashid Alafasy",
                                surahId = 67,
                                surahNameEn = title,
                                surahNameAr = "الملك",
                                ayahNo = 1,
                                audioUrl = "https://everyayah.com/data/Alafasy_64kbps/067001.mp3",
                                durationMs = 0L
                            )
                        )
                    })
                }
                item { SectionHeader(title = "Listen by routine", onSeeAll = null) }
                item {
                    RoutineRow(onMode = { mode -> rootNavigator?.push(RoutineModeScreen(mode)) })
                }
                item { SectionHeader(title = "Top surahs", onSeeAll = null) }
                items(QuranData.SURAHS.take(5)) { surah ->
                    TopSurahRow(
                        number = surah.id,
                        nameEn = surah.nameEn,
                        nameAr = surah.nameAr,
                        meta = "${surah.ayahsCount} Ayahs • ${surah.revelationType}",
                        onClick = { rootNavigator?.push(SurahDetailScreen(surah.id)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeTopBarReplica(onPremiumClick: () -> Unit, onStatsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Frosted "Premium" pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(GlassBg)
                .border(1.dp, GlassBorder, RoundedCornerShape(50))
                .clickable(onClick = onPremiumClick)
                .padding(horizontal = 18.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Premium", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        Text(text = "Home", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)

        // Circular dark stats button
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xFF1C1C1E))
                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                .clickable(onClick = onStatsClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.BarChart,
                contentDescription = "Listening stats",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun FeaturedHero(onClick: () -> Unit) {
    val hero = StitchAssets.VerifiedReciters.first()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 10.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = hero.photoUrl,
            contentDescription = hero.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(190.dp)
                .clip(CircleShape)
                .background(DarkCard)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Mishary Rashid Alafasy",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SectionHeader(title: String, onSeeAll: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        if (onSeeAll != null) {
            Text(
                text = "See all",
                color = LinkBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onSeeAll)
            )
        }
    }
}

@Composable
private fun PlaylistsRow(onFavourites: () -> Unit, onFocusWork: () -> Unit, onNightSleep: () -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Favourites: dark card, giant white star, bottom label
            Box(
                modifier = Modifier
                    .size(width = 160.dp, height = 190.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCard)
                    .clickable(onClick = onFavourites)
                    .padding(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Favourites",
                    tint = Color.White,
                    modifier = Modifier
                        .size(96.dp)
                        .align(Alignment.Center)
                )
                Text(
                    text = "Favourites",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
        }
        item {
            // Focus & Work: blue gradient + concentric rings
            Box(
                modifier = Modifier
                    .size(width = 160.dp, height = 190.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF2E7CF6), Color(0xFF0A1F44))
                        )
                    )
                    .clickable(onClick = onFocusWork)
            ) {
                ConcentricRings(modifier = Modifier.align(Alignment.TopCenter))
                Text(
                    text = "Focus & Work",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                )
            }
        }
        item {
            // Night & Sleep: deep indigo gradient + moon
            Box(
                modifier = Modifier
                    .size(width = 160.dp, height = 190.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF3B2E7C), Color(0xFF0B0B18))
                        )
                    )
                    .clickable(onClick = onNightSleep)
                    .padding(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Bedtime,
                    contentDescription = "Night and Sleep",
                    tint = Color.White,
                    modifier = Modifier
                        .size(84.dp)
                        .align(Alignment.Center)
                )
                Text(
                    text = "Night & Sleep",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
        }
    }
}

@Composable
private fun ConcentricRings(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 160.dp, height = 130.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        listOf(130.dp, 104.dp, 78.dp, 52.dp).forEach { size ->
            Box(
                modifier = Modifier
                    .size(size)
                    .offset(y = (-18).dp)
                    .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
            )
        }
    }
}

@Composable
private fun NewlyAddedRow(onReciter: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(StitchAssets.VerifiedReciters) { reciter ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(88.dp)
                    .clickable { onReciter(reciter.slug) }
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    AsyncImage(
                        model = reciter.photoUrl,
                        contentDescription = reciter.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(DarkCard)
                    )
                    // Green NEW badge
                    Box(
                        modifier = Modifier
                            .offset(x = 4.dp, y = (-4).dp)
                            .clip(RoundedCornerShape(50))
                            .background(BadgeGreen)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(text = "NEW", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = reciter.name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun ListeningStatsStrip() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatCell(value = "9", label = "days streak")
        StatCell(value = "13", label = "min today")
        StatCell(value = QuranData.RECITERS.size.toString(), label = "reciters")
    }
}

@Composable
private fun StatCell(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, color = MutedGrey, fontSize = 12.sp)
    }
}

@Composable
private fun ContinueListeningRow(onPlay: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(StitchAssets.JumpBackInItems) { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .width(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(16.dp))
                    .clickable { onPlay(item.title) }
                    .padding(10.dp)
            ) {
                AsyncImage(
                    model = item.coverUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCard)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.subtitle,
                        color = MutedGrey,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.14f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(item.progress.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(LinkBlue, RoundedCornerShape(50))
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Resume",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

private data class RoutineCard(val label: String, val icon: ImageVector, val mode: RoutineModeType)

@Composable
private fun RoutineRow(onMode: (RoutineModeType) -> Unit) {
    val cards = listOf(
        RoutineCard("Study", Icons.Filled.MenuBook, RoutineModeType.STUDY),
        RoutineCard("Work", Icons.Filled.Work, RoutineModeType.WORK),
        RoutineCard("Sleep", Icons.Filled.Bedtime, RoutineModeType.SLEEP)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        cards.forEach { card ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(16.dp))
                    .clickable { onMode(card.mode) }
                    .padding(vertical = 16.dp)
            ) {
                Icon(imageVector = card.icon, contentDescription = card.label, tint = Color.White, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = card.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun TopSurahRow(number: Int, nameEn: String, nameAr: String, meta: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.09f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = number.toString(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = nameEn, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = meta, color = MutedGrey, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(text = nameAr, color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = "Open", tint = Color.White, modifier = Modifier.size(24.dp))
    }
}
