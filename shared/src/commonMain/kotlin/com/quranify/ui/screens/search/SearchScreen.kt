package com.quranify.ui.screens.search

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
import com.quranify.data.seed.QuranData
import com.quranify.ui.screens.reciters.ReciterProfileScreen
import com.quranify.ui.screens.surah.SurahDetailScreen
import com.quranify.ui.theme.QuranifyColors

class SearchScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
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
                                AyahQuickPlayCard(ayahRef)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        if (searchResults.surahs.isNotEmpty() && (selectedFilter == "All" || selectedFilter == "Surahs")) {
                            item {
                                Text("Surahs", color = QuranifyColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                            }
                            items(searchResults.surahs) { surah ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { navigator.push(SurahDetailScreen(surah.id)) }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
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
fun AyahQuickPlayCard(ayahRef: AyahReference) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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
                onClick = { /* Play action */ },
                modifier = Modifier
                    .background(QuranifyColors.Primary, shape = RoundedCornerShape(24.dp))
                    .size(48.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = QuranifyColors.Background)
            }
        }
    }
}
