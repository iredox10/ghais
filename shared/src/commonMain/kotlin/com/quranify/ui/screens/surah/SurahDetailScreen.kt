package com.quranify.ui.screens.surah

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.quranify.data.repository.FavoritesStore
import com.quranify.data.seed.QuranData
import com.quranify.domain.model.Ayah
import com.quranify.domain.model.TrackItem
import com.quranify.player.AudioEngine
import com.quranify.ui.navigation.LocalRootNavigator
import com.quranify.ui.screens.player.NowPlayingScreen
import com.quranify.ui.theme.QuranifyColors

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

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = surah.nameEn,
                                color = QuranifyColors.TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${surah.revelationType} • ${surah.ayahsCount} Ayahs",
                                color = QuranifyColors.TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = QuranifyColors.TextPrimary)
                        }
                    },
                    actions = {
                        Text(
                            text = surah.nameAr,
                            color = QuranifyColors.Primary,
                            fontSize = 22.sp,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = QuranifyColors.Background)
                )
            },
            containerColor = QuranifyColors.Background
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                // Actions Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reciter Dropdown
                    Box {
                        AssistChip(
                            onClick = { expandedReciterMenu = true },
                            label = { Text(selectedReciter.nameEn.split(" ").take(2).joinToString(" "), color = QuranifyColors.TextPrimary) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = QuranifyColors.Surface),
                            border = BorderStroke(1.dp, QuranifyColors.Primary)
                        )
                        DropdownMenu(
                            expanded = expandedReciterMenu,
                            onDismissRequest = { expandedReciterMenu = false },
                            modifier = Modifier.background(QuranifyColors.Surface)
                        ) {
                            QuranData.RECITERS.forEach { reciter ->
                                DropdownMenuItem(
                                    text = { Text(reciter.nameEn, color = QuranifyColors.TextPrimary) },
                                    onClick = {
                                        selectedReciter = reciter
                                        expandedReciterMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Row {
                        IconButton(onClick = { /* Add to Playlist */ }) {
                            Icon(Icons.Filled.Add, contentDescription = "Add to Playlist", tint = QuranifyColors.Primary)
                        }
                        Button(
                            onClick = {
                                AudioEngine.playQueue(buildTracks(), 0)
                                rootNavigator.push(NowPlayingScreen())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = QuranifyColors.Primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Play All", color = QuranifyColors.Background, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    // Bismillah Header
                    if (surahId != 9) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                    color = QuranifyColors.TextPrimary,
                                    fontSize = 26.sp,
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
                        HorizontalDivider(color = QuranifyColors.Surface, thickness = 1.dp)
                    }
                }
            }
        }
    }
}
