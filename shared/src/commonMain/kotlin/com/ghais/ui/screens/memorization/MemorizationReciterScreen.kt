package com.ghais.ui.screens.memorization

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
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
import com.ghais.player.QuranDownloads
import com.ghais.ui.components.noir.NoirEditorialHeader
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSegmentedProgress
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.screens.reciters.CreatePortionDialog
import com.ghais.ui.screens.reciters.NoirReciterAvatar
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography
import com.ghais.ui.util.bidiIsolate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        val totalMasteredAyahs = remember(masteryMap) {
            masteryMap.values.count { it == MasteryStatus.MASTERED }
        }

        fun playSurahInAyahMode(surahId: Int) {
            AudioEngine.playSurahInAyahMode(
                surahId = surahId,
                reciterSlug = reciter.slug,
                startAyahNo = 1
            )
            rootNavigator.push(NowPlayingScreen())
        }

        val listState = rememberLazyListState()
        val showScrolledTitle = listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 140

        NoirScreenRoot {
            Column(modifier = Modifier.fillMaxSize()) {
                NoirEditorialHeader(
                    eyebrow = if (showScrolledTitle) "Hifz Mode" else "Hifz Studio",
                    title = if (showScrolledTitle) displayReciterName(reciter) else "Recitation & Recall",
                    onBack = { navigator.pop() },
                    backIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    badgeText = "$downloadedCount/${allSurahs.size}",
                    badgeIcon = if (allDone) Icons.Filled.Check else Icons.Filled.Download
                )
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp)
                ) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Spacer(modifier = Modifier.height(4.dp))

                            // Plum Aura Hero Card — portrait, calligraphy, style & offline CTA
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(GhaisNoir.plumFill())
                                    .background(GhaisNoir.sheen())
                                    .border(1.dp, GhaisNoir.SpecularTop.copy(alpha = 0.28f), RoundedCornerShape(24.dp))
                                    .padding(18.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        NoirReciterAvatar(
                                            photoUrl = reciter.imageUrl,
                                            nameEn = reciter.nameEn,
                                            size = 64.dp,
                                            shape = RoundedCornerShape(18.dp),
                                            monogramSize = 22.sp,
                                            ring = true,
                                            slug = reciter.slug,
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "MEMORIZE WITH",
                                                color = GhaisNoir.Lavender,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.2.sp,
                                                maxLines = 1
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = displayReciterName(reciter),
                                                color = GhaisNoir.TextPrimary,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                lineHeight = 24.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = reciter.nameAr,
                                                fontFamily = GhaisTypography.quranFont,
                                                fontSize = 17.sp,
                                                lineHeight = 26.sp,
                                                letterSpacing = 0.sp,
                                                color = GhaisNoir.TextSecondary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Style + tempo tag & mastery summary + Download CTA row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(GhaisShapes.pill)
                                                .background(Color.Black.copy(alpha = 0.24f))
                                                .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            val metaText = buildString {
                                                append(everyAyahMeta?.style ?: reciter.style)
                                                append(" • ")
                                                append(everyAyahMeta?.tempo ?: "Steady")
                                                if (totalMasteredAyahs > 0) {
                                                    append(" • $totalMasteredAyahs mastered")
                                                }
                                            }
                                            Text(
                                                text = metaText,
                                                color = GhaisNoir.TextSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (!allDone) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(GhaisShapes.pill)
                                                    .background(GhaisNoir.lavenderFill())
                                                    .noirClickable {
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
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Download,
                                                        contentDescription = null,
                                                        tint = GhaisNoir.LavenderInk,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = when {
                                                            downloadingCount > 0 -> "$downloadingCount/${allSurahs.size}"
                                                            downloadedCount > 0 -> "Get ${allSurahs.size - downloadedCount}"
                                                            else -> "Download all"
                                                        },
                                                        color = GhaisNoir.LavenderInk,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .clip(GhaisShapes.pill)
                                                    .background(Color.White.copy(alpha = 0.14f))
                                                    .border(1.dp, GhaisNoir.SpecularTop.copy(alpha = 0.3f), GhaisShapes.pill)
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Check,
                                                        contentDescription = null,
                                                        tint = GhaisNoir.Lavender,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = "All offline",
                                                        color = GhaisNoir.TextPrimary,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Engraved search bar with clear button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
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
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(Modifier.width(9.dp))
                                    BasicTextField(
                                        value = surahQuery,
                                        onValueChange = { surahQuery = it },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = GhaisNoir.TextPrimary,
                                            fontSize = 13.sp
                                        ),
                                        cursorBrush = SolidColor(GhaisNoir.Lavender),
                                        decorationBox = { innerTextField ->
                                            if (surahQuery.isEmpty()) {
                                                Text(
                                                    text = "Search surah by name or number...",
                                                    color = GhaisNoir.TextTertiary,
                                                    fontSize = 13.sp
                                                )
                                            }
                                            innerTextField()
                                        }
                                    )
                                    if (surahQuery.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(GhaisNoir.Fill2)
                                                .noirClickable { surahQuery = "" },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Clear search",
                                                tint = GhaisNoir.TextSecondary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Portion filter pills
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
                                                    .background(GhaisNoir.cardFillSoft())
                                                    .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
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
                                                    .background(GhaisNoir.cardFillSoft())
                                                    .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
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
                                            .border(1.dp, GhaisNoir.SpecularTop.copy(alpha = 0.25f), GhaisShapes.pill)
                                            .noirClickable { showCreatePortion = true }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "+ Create",
                                            color = GhaisNoir.Lavender,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
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

                        val surahMastered = masteryMap.count { (coord, st) ->
                            coord.first == surah.id && st == MasteryStatus.MASTERED
                        }
                        val surahWeak = masteryMap.count { (coord, st) ->
                            coord.first == surah.id && st == MasteryStatus.REVIEW_NEEDED
                        }
                        val practicedCount = surahMastered + surahWeak

                        val cardShape = RoundedCornerShape(18.dp)
                        val cardBorder = when {
                            isPlayingThisSurah -> GhaisNoir.Lavender.copy(alpha = 0.50f)
                            practicedCount > 0 -> GhaisNoir.BorderCard
                            else -> GhaisNoir.BorderGhost
                        }
                        val cardBg = when {
                            isPlayingThisSurah -> GhaisNoir.cardFillActive()
                            practicedCount > 0 -> GhaisNoir.tintFill()
                            else -> GhaisNoir.cardFillSoft()
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(cardShape)
                                .background(cardBg)
                                .border(1.dp, cardBorder, cardShape)
                                .noirClickable { playSurahInAyahMode(surah.id) }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Numbered Surah Well
                                val badgeShape = RoundedCornerShape(13.dp)
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(badgeShape)
                                        .then(
                                            if (isPlayingThisSurah) Modifier.background(GhaisNoir.plumFill())
                                            else Modifier
                                                .background(GhaisNoir.wellFill())
                                                .border(1.dp, GhaisNoir.BorderCard, badgeShape)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = surah.id.toString().padStart(2, '0'),
                                        color = if (isPlayingThisSurah) GhaisNoir.Lavender else GhaisNoir.TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Surah title + Arabic calligraphy + metadata
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = surah.nameEn,
                                            color = GhaisNoir.TextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Text(
                                            text = bidiIsolate(surah.nameAr),
                                            fontFamily = GhaisTypography.quranFont,
                                            fontSize = 15.sp,
                                            color = if (isPlayingThisSurah) GhaisNoir.Lavender else GhaisNoir.TextSecondary,
                                            maxLines = 1
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = buildString {
                                            append("${surah.ayahsCount} Ayahs • ${surah.revelationType}")
                                            if (isPlayingThisSurah) append(" • Playing")
                                        },
                                        color = if (isPlayingThisSurah) GhaisNoir.Lavender else GhaisNoir.TextTertiary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Trailing download & play buttons
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    HifzSurahTrailing(
                                        isPlayingThisSurah = isPlayingThisSurah,
                                        surahNameEn = surah.nameEn,
                                        isDownloaded = isDownloaded,
                                        isDownloading = isDownloading,
                                        isDownloadFailed = isFailed,
                                        onDownloadClick = {
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
                            }

                            val p = bundleProgress
                            if (isDownloading && p != null && p in 0f..1f) {
                                Spacer(modifier = Modifier.height(10.dp))
                                NoirSegmentedProgress(
                                    progress = p,
                                    trackHeight = 4.dp
                                )
                            }

                            if (practicedCount > 0) {
                                val surahLearning = (surah.ayahsCount - practicedCount).coerceAtLeast(0)
                                Spacer(modifier = Modifier.height(10.dp))
                                SurahMasteryHeatmapBar(
                                    masteredCount = surahMastered,
                                    weakCount = surahWeak,
                                    learningCount = surahLearning,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
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
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(GhaisNoir.Lavender.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Downloaded",
                tint = GhaisNoir.Lavender,
                modifier = Modifier.size(15.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
    } else {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(GhaisNoir.Fill1)
                .noirClickable {
                    if (!isDownloading) onDownloadClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = if (isDownloadFailed) "Download failed — tap to retry" else "Download",
                tint = if (isDownloadFailed) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .then(
                if (isPlayingThisSurah) Modifier.background(GhaisNoir.lavenderFill())
                else Modifier
                    .background(GhaisNoir.chromeFill())
            )
            .noirClickable { onPlayClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlayingThisSurah) Icons.Filled.GraphicEq else Icons.Filled.PlayArrow,
            contentDescription = "Play Surah $surahNameEn",
            tint = if (isPlayingThisSurah) GhaisNoir.LavenderInk else GhaisNoir.OnChrome,
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
