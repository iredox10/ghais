package com.ghais.ui.screens.memorization

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.ghais.data.seed.EveryAyahReciter
import com.ghais.data.seed.EveryAyahReciters
import com.ghais.player.AudioEngine
import com.ghais.player.QuranDownloads
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSegmentedProgress
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.screens.reciters.NoirReciterAvatar
import com.ghais.ui.screens.reciters.photoForSlug
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography

object MemorizationScreen : Tab {

    override val options: TabOptions
        @Composable
        get() = remember {
            TabOptions(
                index = 2u,
                title = "Hifz",
                icon = null
            )
        }

    @Composable
    override fun Content() {
        val rootNavigator = LocalRootNavigator.current
            ?: LocalNavigator.current?.parent
            ?: LocalNavigator.current

        var searchQuery by remember { mutableStateOf("") }

        val reciters = remember(searchQuery) {
            if (searchQuery.isBlank()) {
                EveryAyahReciters.ALL
            } else {
                val q = searchQuery.trim().lowercase()
                EveryAyahReciters.ALL.filter { reciter ->
                    reciter.nameEn.lowercase().contains(q) ||
                        reciter.nameAr.contains(q) ||
                        reciter.country.lowercase().contains(q) ||
                        reciter.style.lowercase().contains(q)
                }
            }
        }

        val currentTrack by AudioEngine.currentTrack.collectAsState()
        val isAyahMode by AudioEngine.isAyahMode.collectAsState()

        val downloaded by QuranDownloads.downloadedKeys.collectAsState()
        val dlProgress by QuranDownloads.progress.collectAsState()
        val failedKeys by QuranDownloads.failedKeys.collectAsState()

        fun playReciterAyahMode(reciter: EveryAyahReciter, surahId: Int = 1) {
            AudioEngine.playSurahInAyahMode(
                surahId = surahId,
                reciterSlug = reciter.slug,
                startAyahNo = 1
            )
            rootNavigator?.push(NowPlayingScreen())
        }

        NoirScreenRoot {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 112.dp)
            ) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Hifz",
                                style = GhaisTypography.displayEditorialBold,
                                color = GhaisNoir.TextPrimary
                            )
                            Text(
                                text = "${reciters.size} reciters",
                                color = GhaisNoir.TextTertiary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(GhaisShapes.pill)
                                .background(GhaisNoir.Fill2)
                                .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Search",
                                    tint = GhaisNoir.TextTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = GhaisNoir.TextPrimary,
                                        fontSize = 14.sp
                                    ),
                                    cursorBrush = SolidColor(Color.White),
                                    decorationBox = { innerTextField ->
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "Search reciters...",
                                                color = GhaisNoir.TextTertiary,
                                                fontSize = 13.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                    }
                }

                if (reciters.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No reciters found for \"$searchQuery\"",
                                color = GhaisNoir.TextTertiary,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(
                        items = reciters,
                        key = { it.slug }
                    ) { reciter ->
                        val isPlayingThisReciter = currentTrack?.reciterSlug == reciter.slug && isAyahMode

                        val downloadKey = "${reciter.slug}/1"
                        val isDownloaded = downloadKey in downloaded
                        val rawProgress: Float? = dlProgress[downloadKey]
                        val reciterProgress: Float? = if (isDownloaded) null else rawProgress
                        val isDownloading = reciterProgress != null
                        val isFailed = !isDownloaded && !isDownloading && downloadKey in failedKeys
                        val starterAudioUrl = reciter.toReciter().getFullSurahUrl(1)

                        MemorizationReciterCard(
                            reciter = reciter,
                            isPlaying = isPlayingThisReciter,
                            isDownloaded = isDownloaded,
                            downloadProgress = reciterProgress,
                            isDownloadFailed = isFailed,
                            onDownloadClick = {
                                if (isDownloaded) return@MemorizationReciterCard
                                if (dlProgress.containsKey(downloadKey)) return@MemorizationReciterCard
                                QuranDownloads.download(reciter.slug, 1, starterAudioUrl)
                            },
                            onPlayClick = { playReciterAyahMode(reciter, 1) },
                            onCardClick = { rootNavigator?.push(MemorizationReciterScreen(reciter.slug)) }
                        )

                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MemorizationReciterCard(
    reciter: EveryAyahReciter,
    isPlaying: Boolean,
    isDownloaded: Boolean = false,
    downloadProgress: Float? = null,
    isDownloadFailed: Boolean = false,
    onDownloadClick: () -> Unit = {},
    onPlayClick: () -> Unit,
    onCardClick: () -> Unit
) {
    val isDownloading = downloadProgress != null
    NoirCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onCardClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NoirReciterAvatar(
                    photoUrl = photoForSlug(reciter.slug),
                    nameEn = reciter.nameEn,
                    size = 48.dp,
                    shape = RoundedCornerShape(14.dp),
                    ring = isPlaying
                )

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reciter.nameEn,
                        color = if (isPlaying) Color.White else GhaisNoir.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = reciter.nameAr,
                        style = GhaisTypography.arabicBody,
                        fontFamily = GhaisTypography.quranFont,
                        fontSize = 14.sp,
                        color = GhaisNoir.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = "${reciter.style} • ${reciter.tempo}",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                when {
                    isDownloaded -> {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(GhaisNoir.chromeFill())
                                .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Downloaded for offline",
                                tint = GhaisNoir.OnChrome,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    isDownloading -> {
                        Box(
                            modifier = Modifier.size(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = "Downloading",
                                tint = GhaisNoir.TextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    isDownloadFailed -> {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(GhaisNoir.wellFill())
                                .border(1.dp, GhaisNoir.SpecularTop, CircleShape)
                                .noirClickable { onDownloadClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = "Download failed — tap to retry",
                                tint = GhaisNoir.TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(GhaisNoir.Fill2)
                                .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                                .noirClickable { onDownloadClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = "Download Al-Fatihah for offline",
                                tint = GhaisNoir.TextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) Color.White else GhaisNoir.Fill2)
                        .border(
                            1.dp,
                            if (isPlaying) Color.White else GhaisNoir.BorderCard,
                            CircleShape
                        )
                        .noirClickable { onPlayClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play ${reciter.nameEn} in Ayah Mode",
                        tint = if (isPlaying) Color.Black else GhaisNoir.TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            val p = downloadProgress
            if (!isDownloaded && p != null && p in 0f..1f) {
                Spacer(modifier = Modifier.height(8.dp))
                NoirSegmentedProgress(
                    progress = p,
                    trackHeight = 3.dp
                )
            }
        }
    }
}
