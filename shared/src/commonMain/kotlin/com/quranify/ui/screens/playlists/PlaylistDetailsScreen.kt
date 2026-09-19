package com.quranify.ui.screens.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.quranify.data.seed.QuranData
import com.quranify.domain.model.TrackItem
import com.quranify.player.AudioEngine
import com.quranify.ui.navigation.LocalRootNavigator
import com.quranify.ui.screens.player.NowPlayingScreen
import com.quranify.ui.screens.surah.SurahDetailScreen

private val MutedGrey = Color(0xFF9A9AA0)

/**
 * One playlist's own screen: cover art, name, description,
 * then its surahs ready to play.
 */
data class PlaylistDetailsScreen(val playlistId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val rootNavigator = LocalRootNavigator.current ?: navigator.parent ?: navigator
        val playlist = remember(playlistId) { moodPlaylistById(playlistId) }
        val reciter = remember { QuranData.RECITERS.first() }
        val surahs = remember(playlist) {
            playlist.surahIds.mapNotNull { id -> QuranData.SURAHS.firstOrNull { it.id == id } }
        }

        fun buildTracks(): List<TrackItem> = surahs.flatMap { surah ->
            (1..surah.ayahsCount).map { ayahNo ->
                TrackItem(
                    reciterSlug = reciter.slug,
                    reciterName = reciter.nameEn,
                    surahId = surah.id,
                    surahNameEn = surah.nameEn,
                    surahNameAr = surah.nameAr,
                    ayahNo = ayahNo,
                    audioUrl = reciter.getAyahAudioUrl(surah.id, ayahNo)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentPadding = PaddingValues(bottom = 112.dp)
        ) {
            // Nav row
            item {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp, top = 8.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable { navigator.pop() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            // Cover art
            item {
                PlaylistCover(
                    art = playlist.art,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(220.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
            }
            // Name + description
            item {
                Text(
                    text = playlist.title,
                    color = Color.White,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            item {
                Text(
                    text = playlist.description,
                    color = MutedGrey,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            item {
                Text(
                    text = "${surahs.size} surahs • ${reciter.nameEn}",
                    color = MutedGrey,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            // Play-all + shuffle
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White)
                            .clickable {
                                val tracks = buildTracks()
                                if (tracks.isNotEmpty()) {
                                    AudioEngine.playQueue(tracks, startIndex = 0)
                                    rootNavigator.push(NowPlayingScreen())
                                }
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play all",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Play All", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                            .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                            .clickable {
                                val tracks = buildTracks().shuffled()
                                if (tracks.isNotEmpty()) {
                                    AudioEngine.playQueue(tracks, startIndex = 0)
                                    rootNavigator.push(NowPlayingScreen())
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shuffle,
                            contentDescription = "Shuffle",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
            // Surahs header
            item {
                Text(
                    text = "Surahs in this playlist",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
            // Surah rows
            itemsIndexed(surahs, key = { _, s -> "pl_${playlist.id}_${s.id}" }) { index, surah ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navigator.push(SurahDetailScreen(surah.id)) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.09f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = surah.id.toString(),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = surah.nameEn,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${surah.ayahsCount} Ayahs • ${surah.revelationType}",
                                color = MutedGrey,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(text = surah.nameAr, color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.10f))
                                .clickable {
                                    val tracks = buildTracks()
                                    val start = tracks.indexOfFirst { it.surahId == surah.id }
                                        .takeIf { it >= 0 } ?: 0
                                    if (tracks.isNotEmpty()) {
                                        AudioEngine.playQueue(tracks, startIndex = start)
                                        rootNavigator.push(NowPlayingScreen())
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play ${surah.nameEn}",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (index < surahs.lastIndex) {
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.07f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 62.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            item { Spacer(modifier = Modifier.height(4.dp)) }
        }
    }
}
