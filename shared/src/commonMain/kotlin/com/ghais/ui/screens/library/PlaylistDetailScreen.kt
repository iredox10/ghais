package com.ghais.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ghais.data.seed.QuranDataRepository
import com.ghais.data.seed.toTrackItem
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.components.noir.GhostPillButton
import com.ghais.ui.components.noir.NoirHeroCard
import com.ghais.ui.components.noir.NoirListRow
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.home.NoirStatChip
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisTypography

data class PlaylistDetailScreen(val playlistId: String) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val rootNavigator = LocalRootNavigator.current ?: navigator.parent ?: navigator

        val playlist = remember(playlistId) {
            QuranDataRepository.getCuratedPlaylistOrDefault(playlistId)
        }
        val tracks = remember(playlist) {
            playlist.tracks.map { it.toTrackItem() }
        }

        NoirScreenRoot {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {},
                        navigationIcon = {
                            IconButton(
                                onClick = { navigator.pop() },
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(GhaisNoir.Fill2, CircleShape)
                                        .border(1.dp, GhaisNoir.BorderCard, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = GhaisNoir.TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "CURATED COLLECTION",
                                color = GhaisNoir.TextTertiary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Listen to",
                                style = GhaisTypography.displayEditorial,
                                maxLines = 1
                            )
                            Text(
                                text = playlist.title,
                                style = GhaisTypography.displayEditorialBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            NoirHeroCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(84.dp)
                                                .background(GhaisNoir.wellFill(), CircleShape)
                                                .border(1.dp, GhaisNoir.SpecularTop, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = playlist.title.take(1).uppercase(),
                                                color = GhaisNoir.TextPrimary,
                                                fontSize = 32.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "COLLECTION",
                                                color = GhaisNoir.TextTertiary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = 1.2.sp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = playlist.title,
                                                color = GhaisNoir.TextPrimary,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = "${tracks.size} tracks • ${playlist.totalDuration}",
                                                color = GhaisNoir.TextSecondary,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        NoirStatChip(text = "${tracks.size} Tracks")
                                        NoirStatChip(text = playlist.totalDuration)
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    ChromePillButton(
                                        text = "Play All",
                                        onClick = {
                                            if (tracks.isNotEmpty()) {
                                                AudioEngine.playQueue(tracks, startIndex = 0)
                                                rootNavigator.push(NowPlayingScreen())
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = tracks.isNotEmpty(),
                                        leadingIcon = Icons.Default.PlayArrow
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        GhostPillButton(
                                            text = "Shuffle",
                                            onClick = {
                                                if (tracks.isNotEmpty()) {
                                                    AudioEngine.playQueue(tracks.shuffled(), startIndex = 0)
                                                    rootNavigator.push(NowPlayingScreen())
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                        GhostPillButton(
                                            text = "Share",
                                            onClick = {},
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            NoirSectionHeader(
                                label = "Tracks",
                                actionLabel = "${tracks.size} recitations",
                                onAction = {}
                            )
                        }
                    }

                    itemsIndexed(items = tracks, key = { index, track -> "${track.surahId}-${track.reciterSlug}#$index" }) { _, track ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 5.dp)
                        ) {
                            NoirListRow(
                                title = "${track.surahNameEn} (${track.surahNameAr})",
                                subtitle = track.reciterName,
                                icon = Icons.Default.MusicNote,
                                chevron = false,
                                onClick = {
                                    val index = tracks.indexOf(track).coerceAtLeast(0)
                                    AudioEngine.playQueue(tracks, startIndex = index)
                                    rootNavigator.push(NowPlayingScreen())
                                },
                                trailing = {
                                    PlaylistRowTrailing(onRemove = {})
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.PlaylistRowTrailing(
    onRemove: () -> Unit
) {
    IconButton(onClick = onRemove, modifier = Modifier.size(34.dp)) {
        Icon(
            imageVector = Icons.Filled.Delete,
            contentDescription = "Remove",
            tint = GhaisNoir.TextTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
    Spacer(modifier = Modifier.width(6.dp))
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(GhaisNoir.wellFill(), CircleShape)
            .border(1.dp, GhaisNoir.BorderCard, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play",
            tint = GhaisNoir.TextPrimary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun PlaylistTrackRow(
    track: TrackItem,
    onTrackClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
    ) {
        NoirListRow(
            title = "${track.surahNameEn} (${track.surahNameAr})",
            subtitle = track.reciterName,
            icon = Icons.Default.MusicNote,
            chevron = false,
            onClick = onTrackClick,
            trailing = {
                PlaylistRowTrailing(onRemove = {})
            }
        )
    }
}
