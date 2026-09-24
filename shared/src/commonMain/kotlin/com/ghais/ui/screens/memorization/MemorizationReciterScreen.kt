package com.ghais.ui.screens.memorization

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.seed.EveryAyahReciters
import com.ghais.domain.model.Reciter
import com.ghais.domain.model.Surah
import com.ghais.player.AudioEngine
import com.ghais.player.QuranDownloads
import com.ghais.ui.components.noir.GhostPillButton
import com.ghais.ui.components.noir.NoirListRow
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSegmentedProgress
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisTypography

data class MemorizationReciterScreen(val reciterSlug: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val rootNavigator = LocalRootNavigator.current ?: navigator.parent ?: navigator

        val reciter: Reciter = remember(reciterSlug) {
            QuranDataRepository.getReciterBySlug(reciterSlug)
        }
        val everyAyahMeta = remember(reciterSlug) {
            EveryAyahReciters.findBySlug(reciterSlug)
        }
        val allSurahs: List<Surah> = remember {
            QuranDataRepository.getSurahs()
        }

        val currentTrack by AudioEngine.currentTrack.collectAsState()
        val isAyahMode by AudioEngine.isAyahMode.collectAsState()

        val downloaded by QuranDownloads.downloadedKeys.collectAsState()
        val dlProgress by QuranDownloads.progress.collectAsState()
        val failedKeys by QuranDownloads.failedKeys.collectAsState()

        val allKeys = remember(reciter.slug, allSurahs) {
            allSurahs.map { "${reciter.slug}/${it.id}" }
        }
        val downloadedCount = remember(reciter.slug, allSurahs, downloaded) {
            allSurahs.count { "${reciter.slug}/${it.id}" in downloaded }
        }
        val allDone = allKeys.isNotEmpty() && allKeys.all { it in downloaded }
        val downloadingCount = allKeys.count { it in dlProgress && it !in downloaded }

        fun playSurahInAyahMode(surahId: Int) {
            AudioEngine.playSurahInAyahMode(
                surahId = surahId,
                reciterSlug = reciter.slug,
                startAyahNo = 1
            )
            rootNavigator.push(NowPlayingScreen())
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
                                        .clip(CircleShape)
                                        .background(GhaisNoir.Fill2)
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
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp)
                ) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Memorize with",
                                style = GhaisTypography.displayEditorial,
                                maxLines = 1
                            )
                            Text(
                                text = reciter.nameEn,
                                style = GhaisTypography.displayEditorialBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = reciter.nameAr,
                                style = GhaisTypography.arabicBody,
                                fontFamily = GhaisTypography.quranFont,
                                fontSize = 20.sp,
                                color = GhaisNoir.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${everyAyahMeta?.style ?: reciter.style} • ${everyAyahMeta?.tempo ?: "Steady"}",
                                color = GhaisNoir.TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (!allDone) {
                                Spacer(modifier = Modifier.height(12.dp))
                                GhostPillButton(
                                    text = when {
                                        downloadingCount > 0 -> "Downloading ($downloadingCount/${allSurahs.size})"
                                        downloadedCount > 0 -> "Download ${allSurahs.size - downloadedCount} remaining"
                                        else -> "Download all"
                                    },
                                    onClick = {
                                        allSurahs.forEach { surah ->
                                            val key = "${reciter.slug}/${surah.id}"
                                            if (key !in downloaded && !dlProgress.containsKey(key) && key !in failedKeys) {
                                                QuranDownloads.download(
                                                    reciter.slug,
                                                    surah.id,
                                                    reciter.getFullSurahUrl(surah.id)
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    items(
                        items = allSurahs,
                        key = { "${reciter.slug}/${it.id}" }
                    ) { surah ->
                        val isPlayingThisSurah = currentTrack?.surahId == surah.id &&
                            currentTrack?.reciterSlug == reciter.slug &&
                            isAyahMode

                        // Precedence: downloaded > downloading > failed > idle.
                        val downloadKey = "${reciter.slug}/${surah.id}"
                        val isDownloaded = downloadKey in downloaded
                        val rawProgress: Float? = dlProgress[downloadKey]
                        val surahProgress: Float? = if (isDownloaded) null else rawProgress
                        val isDownloading = surahProgress != null
                        val isFailed = !isDownloaded && !isDownloading && downloadKey in failedKeys
                        val audioUrl = reciter.getFullSurahUrl(surah.id)

                        Column(modifier = Modifier.fillMaxWidth()) {
                            NoirListRow(
                                title = "${surah.id}. ${surah.nameEn}",
                                subtitle = "${surah.nameAr} • ${surah.ayahsCount} Ayahs • ${surah.revelationType}",
                                icon = Icons.AutoMirrored.Filled.MenuBook,
                                chevron = false,
                                onClick = { playSurahInAyahMode(surah.id) },
                                trailing = {
                                    HifzSurahTrailing(
                                        isPlayingThisSurah = isPlayingThisSurah,
                                        surahNameEn = surah.nameEn,
                                        isDownloaded = isDownloaded,
                                        isDownloading = isDownloading,
                                        isDownloadFailed = isFailed,
                                        onDownloadClick = {
                                            if (isDownloaded) return@HifzSurahTrailing
                                            if (dlProgress.containsKey(downloadKey)) return@HifzSurahTrailing
                                            QuranDownloads.download(reciter.slug, surah.id, audioUrl)
                                        },
                                        onPlayClick = { playSurahInAyahMode(surah.id) }
                                    )
                                }
                            )

                            val p = surahProgress
                            if (!isDownloaded && p != null && p in 0f..1f) {
                                Spacer(modifier = Modifier.height(6.dp))
                                NoirSegmentedProgress(
                                    progress = p,
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    trackHeight = 4.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.HifzSurahTrailing(
    isPlayingThisSurah: Boolean,
    surahNameEn: String,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    isDownloadFailed: Boolean,
    onDownloadClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    if (isDownloaded) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = "Downloaded",
            tint = GhaisNoir.TextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
    } else {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .noirClickable {
                    if (!isDownloading) onDownloadClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = if (isDownloadFailed) "Download failed — tap to retry" else "Download",
                tint = if (isDownloadFailed) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
    }

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (isPlayingThisSurah) Color.White else GhaisNoir.Fill2)
            .border(
                1.dp,
                if (isPlayingThisSurah) Color.White else GhaisNoir.BorderCard,
                CircleShape
            )
            .noirClickable { onPlayClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = "Play Surah $surahNameEn",
            tint = if (isPlayingThisSurah) Color.Black else GhaisNoir.TextPrimary,
            modifier = Modifier.size(18.dp)
        )
    }
}
