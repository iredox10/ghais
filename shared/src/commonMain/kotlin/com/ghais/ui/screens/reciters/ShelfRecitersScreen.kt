package com.ghais.ui.screens.reciters

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/** Full-list screen opened from "See all" on personalized Reciter shelves. */
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
        val currentTrack by AudioEngine.currentTrack.collectAsState()
        val isPlaying by AudioEngine.isPlaying.collectAsState()
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

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item(key = "shelf_header_$title") {
                        NoirSectionHeader(
                            label = "Reciters • ${filtered.size}",
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(items = filtered, key = { it.catalogKey() }) { reciter ->
                        val isCurrentReciter = currentTrack?.reciterSlug == reciter.slug
                        val activelyPlaying = isCurrentReciter && isPlaying
                        val isFollowed = reciter.slug in followedSlugs

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(GhaisShapes.row)
                                .background(
                                    if (activelyPlaying) GhaisNoir.cardFillActive()
                                    else GhaisNoir.cardFillSoft()
                                )
                                .border(
                                    1.dp,
                                    if (activelyPlaying) GhaisNoir.SpecularTop else GhaisNoir.BorderCard,
                                    GhaisShapes.row
                                )
                                .topSpecular(inset = 22.dp)
                                .noirClickable { navigator.push(ReciterProfileScreen(reciter.slug)) }
                                .padding(12.dp)
                        ) {
                            NoirReciterArtwork(
                                photoUrl = reciter.imageUrl,
                                nameEn = reciter.nameEn,
                                size = 60.dp,
                                shape = RoundedCornerShape(18.dp),
                                slug = reciter.slug
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = reciter.nameEn,
                                    color = GhaisNoir.TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${reciter.riwayah} • ${reciter.style}",
                                    color = GhaisNoir.TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (reciter.nameAr.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = reciter.nameAr,
                                        color = GhaisNoir.TextSecondary,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(GhaisNoir.wellFill())
                                    .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                                    .noirClickable { FollowStore.toggle(reciter.slug) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isFollowed) Icons.Default.Check else Icons.Default.PersonAdd,
                                    contentDescription = if (isFollowed) "Following" else "Follow",
                                    tint = if (isFollowed) GhaisNoir.TextPrimary else GhaisNoir.TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (activelyPlaying) GhaisNoir.chromeFill() else GhaisNoir.wellFill())
                                    .border(
                                        1.dp,
                                        if (activelyPlaying) GhaisNoir.SpecularTop else GhaisNoir.BorderCard,
                                        CircleShape
                                    )
                                    .noirClickable {
                                        if (activelyPlaying) {
                                            AudioEngine.togglePlayPause()
                                        } else {
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
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (activelyPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (activelyPlaying) "Pause" else "Play",
                                    tint = if (activelyPlaying) androidx.compose.ui.graphics.Color(0xFF050506) else GhaisNoir.TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
