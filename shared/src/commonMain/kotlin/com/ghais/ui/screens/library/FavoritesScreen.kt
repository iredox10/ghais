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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ghais.data.repository.FavoritesStore
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirHeroCard
import com.ghais.ui.components.noir.NoirListRow
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.screens.home.NoirStatChip
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisTypography

object FavoritesScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val favorites by FavoritesStore.favoriteTracks.collectAsState()

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
                                text = "COLLECTIONS • SAVED",
                                color = GhaisNoir.TextTertiary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your",
                                style = GhaisTypography.displayEditorial,
                                maxLines = 1
                            )
                            Text(
                                text = "Favorites",
                                style = GhaisTypography.displayEditorialBold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            NoirHeroCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconWell(
                                            icon = Icons.Default.Favorite,
                                            size = 64.dp,
                                            iconSize = 28.dp,
                                            contentDescription = "Favorites"
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "SAVED",
                                                color = GhaisNoir.TextTertiary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = 1.2.sp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = if (favorites.isEmpty()) "Your saved recitations"
                                                else "${favorites.size} saved recitation${if (favorites.size == 1) "" else "s"}",
                                                color = GhaisNoir.TextPrimary,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = "Tap the heart on any track to save it here.",
                                                color = GhaisNoir.TextSecondary,
                                                fontSize = 12.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        NoirStatChip(
                                            text = if (favorites.isEmpty()) "0 saved"
                                            else "${favorites.size} saved"
                                        )
                                    }

                                    if (favorites.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        ChromePillButton(
                                            text = "Play All",
                                            onClick = { AudioEngine.playQueue(favorites) },
                                            modifier = Modifier.fillMaxWidth(),
                                            leadingIcon = Icons.Default.PlayArrow
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            if (favorites.isNotEmpty()) {
                                NoirSectionHeader(
                                    label = "Saved recitations",
                                    actionLabel = "${favorites.size} tracks",
                                    onAction = {}
                                )
                            }
                        }
                    }

                    if (favorites.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                NoirHeroCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        IconWell(
                                            icon = Icons.Default.Favorite,
                                            size = 64.dp,
                                            iconSize = 28.dp,
                                            contentDescription = null,
                                            tint = GhaisNoir.TextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text(
                                            text = "No favorites yet",
                                            color = GhaisNoir.TextPrimary,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Tap the heart on any track to save it here.",
                                            color = GhaisNoir.TextSecondary,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        itemsIndexed(favorites) { index, track ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 5.dp)
                            ) {
                                NoirListRow(
                                    title = track.surahNameEn,
                                    subtitle = track.reciterName,
                                    icon = Icons.Default.Favorite,
                                    chevron = false,
                                    onClick = { AudioEngine.playQueue(favorites, index) },
                                    trailing = {
                                        FavoriteRowTrailing(
                                            onPlay = { AudioEngine.playQueue(favorites, index) },
                                            onRemove = { FavoritesStore.remove(track) }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.FavoriteRowTrailing(
    onPlay: () -> Unit,
    onRemove: () -> Unit
) {
    IconButton(onClick = onRemove, modifier = Modifier.size(34.dp)) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Remove from favorites",
            tint = GhaisNoir.TextTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
    Spacer(modifier = Modifier.width(6.dp))
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(GhaisNoir.wellFill(), CircleShape)
            .border(1.dp, GhaisNoir.BorderCard, CircleShape)
            .noirClickable(onPlay),
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
fun FavoriteTrackRow(
    track: TrackItem,
    onPlay: () -> Unit = {},
    onRemove: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
    ) {
        NoirListRow(
            title = track.surahNameEn,
            subtitle = track.reciterName,
            icon = Icons.Default.Favorite,
            chevron = false,
            onClick = onPlay,
            trailing = {
                FavoriteRowTrailing(onPlay = onPlay, onRemove = onRemove)
            }
        )
    }
}
