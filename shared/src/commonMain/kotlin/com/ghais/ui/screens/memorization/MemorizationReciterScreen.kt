package com.ghais.ui.screens.memorization

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ghais.data.repository.HifzMasteryStore
import com.ghais.data.repository.MasteryStatus
import com.ghais.data.repository.POPULAR_PORTIONS
import com.ghais.data.repository.PortionsStore
import com.ghais.data.repository.QuranAyahRepository
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.QuranPortion
import com.ghais.data.seed.EveryAyahReciters
import com.ghais.domain.model.Reciter
import com.ghais.domain.model.Surah
import com.ghais.player.AudioEngine
import com.ghais.player.DownloadKeys
import com.ghais.player.PlayerBackHandler
import com.ghais.player.QuranDownloads
import com.ghais.ui.components.noir.GhostPillButton
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirListRow
import com.ghais.ui.components.noir.NoirNavBar
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSegmentedProgress
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.screens.reciters.CreatePortionDialog
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography
import com.ghais.ui.util.bidiIsolate

data class MemorizationReciterScreen(val reciterSlug: String) : Screen {

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
        val masteryMap by HifzMasteryStore.masteryMap.collectAsState()
        val scope = rememberCoroutineScope()

        var surahQuery by remember { mutableStateOf("") }
        val customPortions by PortionsStore.customPortions.collectAsState()
        var selectedPortion by remember { mutableStateOf<QuranPortion?>(null) }
        var showCreatePortion by remember { mutableStateOf(false) }

        val allPortions = remember(customPortions) {
            customPortions + POPULAR_PORTIONS
        }

        val visibleSurahs = remember(allSurahs, surahQuery, selectedPortion) {
            val portionIds = selectedPortion?.surahIds?.toSet()
            val base = if (portionIds == null) allSurahs else allSurahs.filter { it.id in portionIds }
            val q = surahQuery.trim().lowercase()
            if (q.isEmpty()) base
            else base.filter { s ->
                s.nameEn.lowercase().contains(q) ||
                    s.nameAr.contains(q) ||
                    s.id.toString() == q
            }
        }

        // Precompute bundle keys once per reciter instead of allocating ~6,350
        // strings on every download progress tick.
        val bundleMap = remember(reciter.slug, allSurahs) {
            allSurahs.associate { it.id to DownloadKeys.bundleKeys(reciter.slug, it.id, it.ayahsCount) }
        }
        val downloadedCount = remember(bundleMap, downloaded) {
            allSurahs.count { surah ->
                bundleMap[surah.id].orEmpty().all { it in downloaded }
            }
        }
        val allDone = allSurahs.isNotEmpty() && downloadedCount == allSurahs.size
        val downloadingCount = remember(bundleMap, downloaded, dlProgress) {
            allSurahs.count { surah ->
                val keys = bundleMap[surah.id].orEmpty()
                !keys.all { it in downloaded } && keys.any { it in dlProgress }
            }
        }

        fun playSurahInAyahMode(surahId: Int) {
            AudioEngine.playSurahInAyahMode(
                surahId = surahId,
                reciterSlug = reciter.slug,
                startAyahNo = 1
            )
            rootNavigator.push(NowPlayingScreen())
        }

        NoirScreenRoot {
            Column(modifier = Modifier.fillMaxSize()) {
                NoirNavBar(
                    title = displayReciterName(reciter),
                    backIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    onBack = { navigator.pop() },
                    titleAlignLeft = true
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp)
                ) {
                    item {
                        // Compact hero: the full "(Teacher)" suffix lives in
                        // the meta line below, so the name reads clean once
                        // instead of three times on one screen.
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Memorize with",
                                color = GhaisNoir.TextTertiary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = displayReciterName(reciter),
                                color = GhaisNoir.TextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 28.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = reciter.nameAr,
                                fontFamily = GhaisTypography.quranFont,
                                fontSize = 18.sp,
                                lineHeight = 30.sp,
                                letterSpacing = 0.sp,
                                color = GhaisNoir.TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${everyAyahMeta?.style ?: reciter.style} • ${everyAyahMeta?.tempo ?: "Steady"}",
                                color = GhaisNoir.TextTertiary,
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
                                        val targets = visibleSurahs
                                        scope.launch(Dispatchers.Default) {
                                            targets.forEach { surah ->
                                                val keys = bundleMap[surah.id].orEmpty()
                                                if (keys.all { it in downloaded }) return@forEach
                                                val ayahUrls = (1..surah.ayahsCount).associateWith { ayahNo ->
                                                    reciter.getAyahAudioUrl(surah.id, ayahNo)
                                                }
                                                QuranDownloads.downloadSurahBundle(
                                                    reciter.slug,
                                                    surah.id,
                                                    reciter.getFullSurahUrl(surah.id),
                                                    ayahUrls
                                                )
                                                QuranAyahRepository.prefetchSurah(surah.id)
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .clip(GhaisShapes.pill)
                                    .background(GhaisNoir.insetFill())
                                    .border(1.dp, GhaisNoir.InsetBorder, GhaisShapes.pill)
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = "Search surahs",
                                        tint = GhaisNoir.TextTertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    BasicTextField(
                                        value = surahQuery,
                                        onValueChange = { surahQuery = it },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = GhaisNoir.TextPrimary,
                                            fontSize = 13.sp
                                        ),
                                        cursorBrush = SolidColor(Color.White),
                                        decorationBox = { innerTextField ->
                                            if (surahQuery.isEmpty()) {
                                                Text(
                                                    text = "Search surah...",
                                                    color = GhaisNoir.TextTertiary,
                                                    fontSize = 13.sp
                                                )
                                            }
                                            innerTextField()
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item(key = "portion-all") {
                                    val selected = selectedPortion == null
                                    Box(
                                        modifier = Modifier
                                            .clip(GhaisShapes.pill)
                                            .then(
                                                if (selected) Modifier.background(GhaisNoir.lavenderFill())
                                                else Modifier
                                                    .background(GhaisNoir.Fill2)
                                                    .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
                                            )
                                            .noirClickable { selectedPortion = null }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "All",
                                            color = if (selected) GhaisNoir.LavenderInk else GhaisNoir.TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                items(allPortions, key = { it.id }) { portion ->
                                    val selected = selectedPortion?.id == portion.id
                                    Box(
                                        modifier = Modifier
                                            .clip(GhaisShapes.pill)
                                            .then(
                                                if (selected) Modifier.background(GhaisNoir.lavenderFill())
                                                else Modifier
                                                    .background(GhaisNoir.Fill2)
                                                    .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
                                            )
                                            .noirClickable {
                                                selectedPortion = if (selected) null else portion
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = portion.name,
                                            color = if (selected) GhaisNoir.LavenderInk else GhaisNoir.TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                item(key = "portion-create") {
                                    Box(
                                        modifier = Modifier
                                            .clip(GhaisShapes.pill)
                                            .background(GhaisNoir.wellFill())
                                            .border(1.dp, GhaisNoir.SpecularTop, GhaisShapes.pill)
                                            .noirClickable { showCreatePortion = true }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "+ Create",
                                            color = GhaisNoir.TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    items(
                        items = visibleSurahs,
                        key = { "${reciter.slug}/${it.id}" }
                    ) { surah ->
                        val isPlayingThisSurah = currentTrack?.surahId == surah.id &&
                            currentTrack?.reciterSlug == reciter.slug &&
                            isAyahMode

                        val bundleKeys = bundleMap[surah.id].orEmpty()
                        val isDownloaded = bundleKeys.all { it in downloaded }
                        val isDownloading = !isDownloaded &&
                            bundleKeys.any { it in dlProgress }
                        val isFailed = !isDownloaded && !isDownloading &&
                            bundleKeys.any { it in failedKeys }
                        val bundleProgress: Float? = if (!isDownloading) null else
                            bundleKeys.count { it in downloaded }.toFloat() / bundleKeys.size.toFloat()
                        val audioUrl = reciter.getFullSurahUrl(surah.id)

                        Column(modifier = Modifier.fillMaxWidth()) {
                            NoirListRow(
                                title = "${surah.id}. ${surah.nameEn}",
                                subtitle = "${bidiIsolate(surah.nameAr)} • ${surah.ayahsCount} Ayahs • ${surah.revelationType}",
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
                                            // Guard: finished bundles stay finished.
                                            // Partial/failed bundles resume — the engine
                                            // skips keys already done or in flight.
                                            if (isDownloaded) return@HifzSurahTrailing
                                            val ayahUrls = (1..surah.ayahsCount).associateWith { ayahNo ->
                                                reciter.getAyahAudioUrl(surah.id, ayahNo)
                                            }
                                            QuranDownloads.downloadSurahBundle(
                                                reciter.slug, surah.id, audioUrl, ayahUrls
                                            )
                                            QuranAyahRepository.prefetchSurah(surah.id)
                                        },
                                        onPlayClick = { playSurahInAyahMode(surah.id) }
                                    )
                                }
                            )

                            val p = bundleProgress
                            if (isDownloading && p != null && p in 0f..1f) {
                                Spacer(modifier = Modifier.height(6.dp))
                                NoirSegmentedProgress(
                                    progress = p,
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    trackHeight = 4.dp
                                )
                            }

                            val surahMastered = masteryMap.count { (coord, st) ->
                                coord.first == surah.id && st == MasteryStatus.MASTERED
                            }
                            val surahWeak = masteryMap.count { (coord, st) ->
                                coord.first == surah.id && st == MasteryStatus.REVIEW_NEEDED
                            }
                            val practicedCount = surahMastered + surahWeak
                            if (practicedCount > 0) {
                                val surahLearning = (surah.ayahsCount - practicedCount).coerceAtLeast(0)
                                Spacer(modifier = Modifier.height(5.dp))
                                SurahMasteryHeatmapBar(
                                    masteredCount = surahMastered,
                                    weakCount = surahWeak,
                                    learningCount = surahLearning,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }

            if (showCreatePortion) {
                CreatePortionDialog(
                    onDismiss = { showCreatePortion = false },
                    onCreate = { name, surahIds ->
                        PortionsStore.addCustom(
                            name = name,
                            surahIds = surahIds,
                            reciterSlug = reciter.slug,
                        )
                        showCreatePortion = false
                    },
                )
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

/**
 * Display name without a trailing "(Teacher)"-style role suffix.
 *
 * The reciter's role already appears in the hero meta line ("Teacher
 * (Mu'allim) • Slow & Measured"), so repeating the parenthetical in the
 * collapsed bar title AND the hero name made the header read like a
 * form field. Strip only a SUFFIX parenthetical — mid-name parentheses
 * (rare) stay untouched. Falls back to the raw name if stripping
 * would leave nothing.
 */
internal fun displayReciterName(reciter: Reciter): String =
    reciter.nameEn.replace(Regex("\\s*\\([^)]*\\)\\s*$"), "").trim()
        .ifBlank { reciter.nameEn }

@Composable
private fun SurahMasteryHeatmapBar(
    masteredCount: Int,
    weakCount: Int,
    learningCount: Int,
    modifier: Modifier = Modifier,
) {
    val total = (masteredCount + weakCount + learningCount).coerceAtLeast(1)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(GhaisShapes.pill)
                .background(GhaisNoir.Fill1)
        ) {
            if (masteredCount > 0) {
                Box(
                    modifier = Modifier
                        .weight(masteredCount.toFloat() / total.toFloat())
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.85f))
                )
            }
            if (weakCount > 0) {
                Box(
                    modifier = Modifier
                        .weight(weakCount.toFloat() / total.toFloat())
                        .height(4.dp)
                        .background(GhaisNoir.Lavender)
                )
            }
            if (learningCount > 0) {
                Box(
                    modifier = Modifier
                        .weight(learningCount.toFloat() / total.toFloat())
                        .height(4.dp)
                        .background(GhaisNoir.Fill2)
                )
            }
        }
        Text(
            text = "${masteredCount}M · ${weakCount}W · ${learningCount}L",
            color = GhaisNoir.TextTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
