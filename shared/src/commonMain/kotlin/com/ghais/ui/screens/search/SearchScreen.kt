package com.ghais.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.seed.QuranData
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.screens.reciters.ReciterProfileScreen
import com.ghais.ui.screens.surah.SurahDetailScreen
import com.ghais.ui.theme.QuranifyColors

class SearchScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val rootNavigator = LocalRootNavigator.current ?: navigator.parent ?: navigator
        var query by remember { mutableStateOf("") }
        var selectedFilter by remember { mutableStateOf("All") }
        val filters = listOf("All", "Surahs", "Reciters", "Ayahs")

        val searchResults = remember(query) { SearchEngine.search(query) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            placeholder = { Text("Search...", color = QuranifyColors.TextSecondary) },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { query = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = QuranifyColors.TextSecondary)
                                    }
                                } else {
                                    IconButton(onClick = { /* Mic action */ }) {
                                        Icon(Icons.Default.Mic, contentDescription = "Mic", tint = QuranifyColors.TextSecondary)
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = QuranifyColors.Primary,
                                unfocusedBorderColor = QuranifyColors.Card,
                                focusedContainerColor = QuranifyColors.Card,
                                unfocusedContainerColor = QuranifyColors.Card,
                                focusedTextColor = QuranifyColors.TextPrimary,
                                unfocusedTextColor = QuranifyColors.TextPrimary
                            ),
                            shape = RoundedCornerShape(25.dp),
                            singleLine = true
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = QuranifyColors.TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = QuranifyColors.Background)
                )
            },
            containerColor = QuranifyColors.Background
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    filters.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = QuranifyColors.Primary,
                                selectedLabelColor = QuranifyColors.Background,
                                containerColor = QuranifyColors.Card,
                                labelColor = QuranifyColors.TextPrimary
                            ),
                            border = null,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (query.isEmpty()) {
                    Text("Recent Searches", color = QuranifyColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No recent searches.", color = QuranifyColors.TextSecondary)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        val ayahRef = searchResults.ayahReference
                        if (ayahRef != null && (selectedFilter == "All" || selectedFilter == "Ayahs")) {
                            item {
                                AyahQuickPlayCard(
                                    ayahRef = ayahRef,
                                    onClick = { navigator.push(SurahDetailScreen(ayahRef.surahId)) },
                                    onPlay = {
                                        val reciter = AudioEngine.currentTrack.value?.let {
                                            QuranDataRepository.getReciterBySlug(it.reciterSlug)
                                        } ?: QuranDataRepository.getFallbackReciter()
                                        val allSurahs = QuranDataRepository.getSurahs()
                                        val allTracks = allSurahs.map { s ->
                                            TrackItem(
                                                reciterSlug = reciter.slug,
                                                reciterName = reciter.nameEn,
                                                surahId = s.id,
                                                surahNameEn = s.nameEn,
                                                surahNameAr = s.nameAr,
                                                ayahNo = 0,
                                                audioUrl = reciter.getFullSurahUrl(s.id),
                                                durationMs = s.ayahsCount * 15_000L
                                            )
                                        }
                                        val startIndex = allTracks.indexOfFirst { it.surahId == ayahRef.surahId }.coerceAtLeast(0)
                                        AudioEngine.playQueue(allTracks, startIndex = startIndex)
                                        rootNavigator.push(NowPlayingScreen())
                                    }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        if (searchResults.surahs.isNotEmpty() && (selectedFilter == "All" || selectedFilter == "Surahs")) {
                            item {
                                Text("Surahs", color = QuranifyColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                            }
                            items(searchResults.surahs) { surah ->
                                val playSurah = {
                                    val reciter = AudioEngine.currentTrack.value?.let {
                                        QuranDataRepository.getReciterBySlug(it.reciterSlug)
                                    } ?: QuranDataRepository.getFallbackReciter()
                                    val allSurahs = QuranDataRepository.getSurahs()
                                    val allTracks = allSurahs.map { s ->
                                        TrackItem(
                                            reciterSlug = reciter.slug,
                                            reciterName = reciter.nameEn,
                                            surahId = s.id,
                                            surahNameEn = s.nameEn,
                                            surahNameAr = s.nameAr,
                                            ayahNo = 0,
                                            audioUrl = reciter.getFullSurahUrl(s.id),
                                            durationMs = s.ayahsCount * 15_000L
                                        )
                                    }
                                    val startIndex = allTracks.indexOfFirst { it.surahId == surah.id }.coerceAtLeast(0)
                                    AudioEngine.playQueue(allTracks, startIndex = startIndex)
                                    rootNavigator.push(NowPlayingScreen())
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { playSurah() }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { playSurah() },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.PlayArrow,
                                            contentDescription = "Play Surah ${surah.nameEn}",
                                            tint = QuranifyColors.Primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(surah.nameEn, color = QuranifyColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                                        Text("Surah ${surah.id} • ${surah.ayahsCount} Ayahs", color = QuranifyColors.TextSecondary, fontSize = 12.sp)
                                    }
                                    Text(surah.nameAr, color = QuranifyColors.Primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (searchResults.reciters.isNotEmpty() && (selectedFilter == "All" || selectedFilter == "Reciters")) {
                            item {
                                Text("Reciters", color = QuranifyColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                            }
                            items(searchResults.reciters) { reciter ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { navigator.push(ReciterProfileScreen(reciter.slug)) }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            val allSurahs = QuranDataRepository.getSurahs()
                                            val allTracks = allSurahs.map { s ->
                                                TrackItem(
                                                    reciterSlug = reciter.slug,
                                                    reciterName = reciter.nameEn,
                                                    surahId = s.id,
                                                    surahNameEn = s.nameEn,
                                                    surahNameAr = s.nameAr,
                                                    ayahNo = 0,
                                                    audioUrl = reciter.getFullSurahUrl(s.id),
                                                    durationMs = s.ayahsCount * 15_000L
                                                )
                                            }
                                            AudioEngine.playQueue(allTracks, startIndex = 0)
                                            rootNavigator.push(NowPlayingScreen())
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.PlayArrow,
                                            contentDescription = "Play reciter ${reciter.nameEn}",
                                            tint = QuranifyColors.Primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(reciter.nameEn, color = QuranifyColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                                        Text(reciter.style.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }, color = QuranifyColors.TextSecondary, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AyahQuickPlayCard(
    ayahRef: AyahReference,
    onClick: () -> Unit = {},
    onPlay: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = QuranifyColors.Card),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val surah = QuranData.SURAHS.find { it.id == ayahRef.surahId }
            Column(modifier = Modifier.weight(1f)) {
                Text("Ayah Reference Found", color = QuranifyColors.Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("${surah?.nameEn} - Ayah ${ayahRef.ayahNo}", color = QuranifyColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .background(QuranifyColors.Primary, shape = RoundedCornerShape(24.dp))
                    .size(48.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play Ayah", tint = QuranifyColors.Background)
            }
        }
    }
}
