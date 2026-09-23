package com.ghais.ui.screens.surah

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.ghais.data.repository.HifzMasteryStore
import com.ghais.data.repository.MasteryStatus
import com.ghais.data.repository.QuranAyahRepository
import com.ghais.data.seed.QuranData
import com.ghais.domain.model.Ayah
import com.ghais.domain.model.HifzRange
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.components.noir.GhostPillButton
import com.ghais.ui.components.noir.NoirHeroCard
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography

/**
 * Strict Noir Glass Surah detail — true-black canvas (glow zone -> #050506 +
 * ambient glow + film grain), grayscale hero plate, and ayah rows in the
 * NoirListRow recipe (number in engraved well, chrome/ghost affordances).
 * Zero hue — state reads through fill elevation, chromium, weight, opacity.
 *
 * Arabic/uthmani text stays at 100% white on dark wells — never dimmed.
 * Signatures, playback/queue/favorite logic and navigation are untouched.
 */
data class SurahDetailScreen(val surahId: Int) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val rootNavigator = LocalRootNavigator.current ?: navigator.parent ?: navigator
        val surah = remember(surahId) {
            QuranData.SURAHS.find { it.id == surahId } ?: QuranData.SURAHS.first()
        }

        var selectedReciter by remember { mutableStateOf(QuranData.RECITERS.first()) }
        var expandedReciterMenu by remember { mutableStateOf(false) }
        var showRangePlaySheet by remember { mutableStateOf(false) }
        var masteryVersion by remember { mutableStateOf(0) }

        // Generate ayahs for this surah with authentic Uthmani text
        val ayahs = remember(surahId) {
            (1..surah.ayahsCount).map { ayahNo ->
                Ayah(
                    surahId = surah.id,
                    ayahNo = ayahNo,
                    textUthmani = QuranAyahRepository.getAyahImmediate(surah.id, ayahNo).textUthmani
                )
            }
        }

        // currentTrack drives MiniPlayer visibility (MainScreen AnimatedVisibility) and highlight.
        val currentTrack by AudioEngine.currentTrack.collectAsState()
        val isEnginePlaying by AudioEngine.isPlaying.collectAsState()
        val hifzRange by AudioEngine.hifzRange.collectAsState()
        val isRangeActiveForSurah = hifzRange?.surahId == surah.id

        fun buildAyahTrack(ayah: Ayah): TrackItem = TrackItem(
            reciterSlug = selectedReciter.slug,
            reciterName = selectedReciter.nameEn,
            surahId = surah.id,
            surahNameEn = surah.nameEn,
            surahNameAr = surah.nameAr,
            ayahNo = ayah.ayahNo,
            audioUrl = selectedReciter.getAyahAudioUrl(surah.id, ayah.ayahNo),
            textUthmani = ayah.textUthmani
        )

        /** Same ayah currently loaded (regardless of play/pause or reciter switch) -> highlight. */
        fun isAyahActive(ayahNo: Int): Boolean {
            val t = currentTrack ?: return false
            return t.surahId == surah.id && t.ayahNo == ayahNo
        }

        /** Same ayah loaded from the selected reciter AND engine reports playing -> pause icon. */
        fun isAyahPlaying(ayahNo: Int): Boolean {
            val t = currentTrack ?: return false
            return t.surahId == surah.id && t.ayahNo == ayahNo &&
                t.reciterSlug == selectedReciter.slug && isEnginePlaying
        }

        /** Per-ayah toggle: pause if playing; resume if paused; else play surah in ayah mode at selected ayah. */
        fun onAyahToggle(ayah: Ayah, index: Int) {
            if (isAyahPlaying(ayah.ayahNo)) {
                AudioEngine.pause()
                return
            }
            val t = currentTrack
            if (t != null && t.surahId == surah.id && t.ayahNo == ayah.ayahNo &&
                t.reciterSlug == selectedReciter.slug
            ) {
                AudioEngine.resume()
                return
            }
            AudioEngine.playSurahInAyahMode(
                surahId = surah.id,
                reciterSlug = selectedReciter.slug,
                startAyahNo = ayah.ayahNo
            )
            rootNavigator.push(NowPlayingScreen())
        }

        val favorites by FavoritesStore.favoriteTracks.collectAsState()

        NoirScreenRoot {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = surah.nameEn,
                                    color = GhaisNoir.TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${surah.revelationType} • ${surah.ayahsCount} Ayahs",
                                    color = GhaisNoir.TextTertiary,
                                    fontSize = 12.sp
                                )
                            }
                        },
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
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = GhaisNoir.TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { showRangePlaySheet = true },
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isRangeActiveForSurah) GhaisNoir.chromeFill() else GhaisNoir.wellFill())
                                        .border(
                                            1.dp,
                                            if (isRangeActiveForSurah) Color.White.copy(alpha = 0.4f) else GhaisNoir.BorderCard,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Repeat,
                                        contentDescription = "Range Loop Mode",
                                        tint = if (isRangeActiveForSurah) GhaisNoir.OnChrome else GhaisNoir.TextPrimary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                            Text(
                                text = surah.nameAr,
                                color = GhaisNoir.TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    // Editorial header + grayscale hero plate.
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "SURAH ${surah.id} • ${surah.revelationType.uppercase()}",
                                color = GhaisNoir.TextTertiary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = surah.nameEn,
                                style = GhaisTypography.displayEditorialBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            SurahNoirHeroPlate(
                                surahNameAr = surah.nameAr,
                                revelationType = surah.revelationType,
                                ayahsCount = surah.ayahsCount,
                                reciterName = selectedReciter.nameEn,
                                expandedReciterMenu = expandedReciterMenu,
                                onExpandReciterMenu = { expandedReciterMenu = true },
                                onDismissReciterMenu = { expandedReciterMenu = false },
                                onSelectReciter = {
                                    selectedReciter = it
                                    expandedReciterMenu = false
                                },
                                onPlayAll = {
                                    AudioEngine.playSurahInAyahMode(
                                        surahId = surah.id,
                                        reciterSlug = selectedReciter.slug,
                                        startAyahNo = 1
                                    )
                                    rootNavigator.push(NowPlayingScreen())
                                },
                                onRangePlay = { showRangePlaySheet = true },
                                isRangeActive = isRangeActiveForSurah,
                                activeRange = if (isRangeActiveForSurah) hifzRange else null,
                                onAddToPlaylist = { /* Add to Playlist */ }
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            NoirSectionHeader(
                                label = "Verses",
                                actionLabel = "${surah.ayahsCount} Ayahs",
                                onAction = {}
                            )
                        }
                    }

                    // Bismillah plate — uthmani at 100% white on an engraved well.
                    if (surahId != 9) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .clip(GhaisShapes.row)
                                    .background(GhaisNoir.insetFill())
                                    .border(1.dp, GhaisNoir.InsetBorder, GhaisShapes.row)
                                    .padding(horizontal = 20.dp, vertical = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                    color = GhaisNoir.TextPrimary,
                                    fontSize = 26.sp,
                                    lineHeight = 48.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    itemsIndexed(ayahs) { index, ayah ->
                        val mastery = remember(masteryVersion, ayah.ayahNo) {
                            HifzMasteryStore.getStatus(surah.id, ayah.ayahNo)
                        }
                        AyahRow(
                            ayah = ayah,
                            // Highlight syncs via currentTrack.surahId/ayahNo/reciterSlug;
                            // icon reflects live play state.
                            isActive = isAyahActive(ayah.ayahNo),
                            isPlaying = isAyahPlaying(ayah.ayahNo),
                            isFavorite = favorites.any { it.audioUrl == selectedReciter.getAyahAudioUrl(surah.id, ayah.ayahNo) },
                            masteryStatus = mastery,
                            onMasteryClick = {
                                HifzMasteryStore.cycleStatus(surah.id, ayah.ayahNo)
                                masteryVersion++
                            },
                            onPlayClick = { onAyahToggle(ayah, index) },
                            onFavoriteClick = {
                                FavoritesStore.toggle(buildAyahTrack(ayah))
                            },
                            onShareClick = { /* Share */ }
                        )
                    }
                }
            }

            if (showRangePlaySheet) {
                RangePlaySheet(
                    surahId = surah.id,
                    surahNameEn = surah.nameEn,
                    totalAyahs = surah.ayahsCount,
                    currentRange = if (isRangeActiveForSurah) hifzRange else null,
                    onDismiss = { showRangePlaySheet = false },
                    onStartRange = { startAyah, endAyah, loops ->
                        AudioEngine.setHifzRange(surah.id, startAyah, endAyah, loops)
                        AudioEngine.playSurahInAyahMode(
                            surahId = surah.id,
                            reciterSlug = selectedReciter.slug,
                            startAyahNo = startAyah
                        )
                        showRangePlaySheet = false
                        rootNavigator.push(NowPlayingScreen())
                    },
                    onClearRange = {
                        AudioEngine.clearHifzRange()
                        showRangePlaySheet = false
                    }
                )
            }
        }
    }
}

/**
 * Grayscale hero plate (mirrors the reciter-profile hero): surah identity +
 * ghost meta chips + chrome Play All + ghost reciter/add actions.
 * Presentation only — all callbacks preserve the original screen logic.
 */
@Composable
private fun SurahNoirHeroPlate(
    surahNameAr: String,
    revelationType: String,
    ayahsCount: Int,
    reciterName: String,
    expandedReciterMenu: Boolean,
    onExpandReciterMenu: () -> Unit,
    onDismissReciterMenu: () -> Unit,
    onSelectReciter: (com.ghais.domain.model.Reciter) -> Unit,
    onPlayAll: () -> Unit,
    onRangePlay: () -> Unit,
    isRangeActive: Boolean = false,
    activeRange: HifzRange? = null,
    onAddToPlaylist: () -> Unit
) {
    NoirHeroCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SURAH",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = surahNameAr,
                        color = GhaisNoir.TextPrimary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "$revelationType • $ayahsCount Ayahs",
                        color = GhaisNoir.TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(GhaisShapes.well)
                        .background(GhaisNoir.wellFill())
                        .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.well)
                        .noirClickable(onClick = onAddToPlaylist),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add to Playlist",
                        tint = GhaisNoir.TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NoirMetaChip(text = revelationType)
                NoirMetaChip(text = "$ayahsCount Ayahs")
                NoirMetaChip(text = reciterName.split(" ").take(2).joinToString(" "))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ChromePillButton(
                        text = "Play All",
                        onClick = onPlayAll,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Filled.PlayArrow
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    GhostPillButton(
                        text = if (isRangeActive && activeRange != null) "Loop ${activeRange.startAyah}–${activeRange.endAyah}" else "Range Loop",
                        onClick = onRangePlay,
                        modifier = Modifier.fillMaxWidth(),
                        active = isRangeActive
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Reciter selector as a ghost pill with a noir dropdown.
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.Fill2)
                        .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
                        .noirClickable(onClick = onExpandReciterMenu)
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = reciterName.split(" ").take(2).joinToString(" "),
                        color = GhaisNoir.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.ExpandMore,
                        contentDescription = "Select reciter",
                        tint = GhaisNoir.TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                DropdownMenu(
                    expanded = expandedReciterMenu,
                    onDismissRequest = onDismissReciterMenu,
                    modifier = Modifier.background(Color(0xFF141416))
                ) {
                    com.ghais.data.seed.QuranData.RECITERS.forEach { reciter ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    reciter.nameEn,
                                    color = if (reciter.nameEn == reciterName) GhaisNoir.TextPrimary else GhaisNoir.TextSecondary,
                                    fontWeight = if (reciter.nameEn == reciterName) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = { onSelectReciter(reciter) }
                        )
                    }
                }
            }
        }
    }
}

/** Non-interactive ghost meta chip — informational, zero hue. */
@Composable
private fun NoirMetaChip(text: String) {
    Box(
        modifier = Modifier
            .clip(GhaisShapes.pill)
            .background(GhaisNoir.Fill2)
            .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = GhaisNoir.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Bottom sheet modal for selecting Ayah range bounds and repetition loop count.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangePlaySheet(
    surahId: Int,
    surahNameEn: String,
    totalAyahs: Int,
    currentRange: HifzRange?,
    onDismiss: () -> Unit,
    onStartRange: (start: Int, end: Int, loops: Int) -> Unit,
    onClearRange: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var startAyah by remember { mutableStateOf(currentRange?.startAyah ?: 1) }
    var endAyah by remember { mutableStateOf(currentRange?.endAyah ?: minOf(5, totalAyahs)) }
    var targetLoops by remember { mutableStateOf(currentRange?.targetLoops ?: 3) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0B0B0E),
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(GhaisNoir.Fill4)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Range Loop Mode",
                        color = GhaisNoir.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$surahNameEn • Select verses to memorize & loop",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 12.sp
                    )
                }
                if (currentRange != null) {
                    Box(
                        modifier = Modifier
                            .clip(GhaisShapes.pill)
                            .background(GhaisNoir.Fill2)
                            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
                            .noirClickable(onClick = onClearRange)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Clear Loop",
                            color = GhaisNoir.TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Presets
            Text(
                text = "QUICK PRESETS",
                color = GhaisNoir.TextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(
                    "1–5" to (1 to minOf(5, totalAyahs)),
                    "1–10" to (1 to minOf(10, totalAyahs)),
                    "1–20" to (1 to minOf(20, totalAyahs)),
                    "All" to (1 to totalAyahs)
                )
                presets.forEach { (label, range) ->
                    val isSelected = startAyah == range.first && endAyah == range.second
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(GhaisShapes.pill)
                            .background(if (isSelected) GhaisNoir.chromeFill() else GhaisNoir.wellFill())
                            .border(
                                1.dp,
                                if (isSelected) Color.White.copy(alpha = 0.4f) else GhaisNoir.BorderCard,
                                GhaisShapes.pill
                            )
                            .noirClickable {
                                startAyah = range.first
                                endAyah = range.second
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) GhaisNoir.OnChrome else GhaisNoir.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Start & End Ayah Pickers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Start Ayah picker
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(GhaisShapes.card)
                        .background(GhaisNoir.insetFill())
                        .border(1.dp, GhaisNoir.InsetBorder, GhaisShapes.card)
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "START AYAH",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$startAyah",
                        color = GhaisNoir.TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RangeStepButton(text = "−") {
                            if (startAyah > 1) {
                                startAyah--
                            }
                        }
                        RangeStepButton(text = "+") {
                            if (startAyah < endAyah) {
                                startAyah++
                            }
                        }
                    }
                }

                // End Ayah picker
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(GhaisShapes.card)
                        .background(GhaisNoir.insetFill())
                        .border(1.dp, GhaisNoir.InsetBorder, GhaisShapes.card)
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "END AYAH",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$endAyah",
                        color = GhaisNoir.TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RangeStepButton(text = "−") {
                            if (endAyah > startAyah) {
                                endAyah--
                            }
                        }
                        RangeStepButton(text = "+") {
                            if (endAyah < totalAyahs) {
                                endAyah++
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Repetitions
            Text(
                text = "LOOP REPETITIONS",
                color = GhaisNoir.TextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val loopOptions = listOf(1, 3, 5, 10, -1)
                loopOptions.forEach { loops ->
                    val isSelected = targetLoops == loops
                    val label = if (loops == -1) "∞" else "${loops}x"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(GhaisShapes.pill)
                            .background(if (isSelected) GhaisNoir.chromeFill() else GhaisNoir.wellFill())
                            .border(
                                1.dp,
                                if (isSelected) Color.White.copy(alpha = 0.4f) else GhaisNoir.BorderCard,
                                GhaisShapes.pill
                            )
                            .noirClickable { targetLoops = loops }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) GhaisNoir.OnChrome else GhaisNoir.TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Start button
            ChromePillButton(
                text = "Start Looping ($startAyah – $endAyah)",
                onClick = {
                    onStartRange(startAyah, endAyah, targetLoops)
                },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = Icons.Filled.PlayArrow
            )
        }
    }
}

@Composable
private fun RangeStepButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(GhaisShapes.well)
            .background(GhaisNoir.Fill2)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.well)
            .noirClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = GhaisNoir.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
