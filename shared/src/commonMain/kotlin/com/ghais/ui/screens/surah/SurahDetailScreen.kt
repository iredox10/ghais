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
import com.ghais.data.seed.QuranData
import com.ghais.domain.model.Ayah
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.ChromePillButton
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

        // Generate ayahs for this surah
        val ayahs = remember(surahId) {
            (1..surah.ayahsCount).map { ayahNo ->
                Ayah(
                    surahId = surah.id,
                    ayahNo = ayahNo,
                    textUthmani = if (ayahNo == 1 && surah.id == 1) "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ" else "آية رقم $ayahNo من سورة ${surah.nameAr}"
                )
            }
        }

        // currentTrack drives MiniPlayer visibility (MainScreen AnimatedVisibility) and highlight.
        val currentTrack by AudioEngine.currentTrack.collectAsState()
        val isEnginePlaying by AudioEngine.isPlaying.collectAsState()

        // Track builder: per-ayah URL for the currently selected reciter.
        fun buildTracks(): List<TrackItem> = ayahs.map { ayah ->
            TrackItem(
                reciterSlug = selectedReciter.slug,
                reciterName = selectedReciter.nameEn,
                surahId = surah.id,
                surahNameEn = surah.nameEn,
                surahNameAr = surah.nameAr,
                ayahNo = ayah.ayahNo,
                audioUrl = selectedReciter.getAyahAudioUrl(surah.id, ayah.ayahNo),
                textUthmani = ayah.textUthmani
            )
        }

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

        /** Per-ayah toggle: pause if playing; switch reciter if slug differs; resume if paused; else play queue. */
        fun onAyahToggle(ayah: Ayah, index: Int) {
            if (isAyahPlaying(ayah.ayahNo)) {
                AudioEngine.pause()
                return
            }
            val t = currentTrack
            if (t != null && t.surahId == surah.id && t.ayahNo == ayah.ayahNo &&
                t.reciterSlug != selectedReciter.slug
            ) {
                // Reciter switched while this ayah is loaded -> restart same ayah with new reciter.
                AudioEngine.playQueue(buildTracks(), index)
                rootNavigator.push(NowPlayingScreen())
                return
            }
            if (isAyahActive(ayah.ayahNo)) {
                AudioEngine.resume()
                return
            }
            AudioEngine.playQueue(buildTracks(), index)
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
                                    AudioEngine.playQueue(buildTracks(), 0)
                                    rootNavigator.push(NowPlayingScreen())
                                },
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
                        AyahRow(
                            ayah = ayah,
                            // Highlight syncs via currentTrack.surahId/ayahNo/reciterSlug;
                            // icon reflects live play state.
                            isActive = isAyahActive(ayah.ayahNo),
                            isPlaying = isAyahPlaying(ayah.ayahNo),
                            isFavorite = favorites.any { it.audioUrl == selectedReciter.getAyahAudioUrl(surah.id, ayah.ayahNo) },
                            onPlayClick = { onAyahToggle(ayah, index) },
                            onFavoriteClick = {
                                FavoritesStore.toggle(buildTracks()[index])
                            },
                            onShareClick = { /* Share */ }
                        )
                    }
                }
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

            ChromePillButton(
                text = "Play All",
                onClick = onPlayAll,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = Icons.Filled.PlayArrow
            )

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
