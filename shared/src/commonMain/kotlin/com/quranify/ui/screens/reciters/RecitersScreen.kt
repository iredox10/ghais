package com.quranify.ui.screens.reciters

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

// Mock data models for compilation
data class Reciter(
    val slug: String,
    val nameEn: String,
    val nameAr: String,
    val style: String,
    val riwayah: String,
    val tempo: String,
    val photoUrl: String? = null
)

data class Surah(
    val number: Int,
    val nameEn: String,
    val nameAr: String,
    val ayahs: Int
)

object QuranData {
    val RECITERS = listOf(
        Reciter("mishary", "Mishary Rashid Alafasy", "مشاري راشد العفاسي", "Murattal", "Hafs", "Normal"),
        Reciter("abdulbasit", "AbdulBaset AbdulSamad", "عبد الباسط عبد الصمد", "Mujawwad", "Hafs", "Slow"),
        Reciter("husary", "Mahmoud Khalil Al-Husary", "محمود خليل الحصري", "Murattal", "Hafs", "Slow"),
        Reciter("shuraim", "Saud Al-Shuraim", "سعود الشريم", "Taraweeh", "Hafs", "Fast")
    )
    
    val SURAHS = (1..114).map { 
        Surah(it, "Surah $it", "سورة $it", 7)
    }
}

class RecitersScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var searchQuery by remember { mutableStateOf("") }
        var selectedFilter by remember { mutableStateOf("All") }
        
        val filters = listOf("All", "Murattal (مرتل)", "Mujawwad (مجود)", "Taraweeh (تراويح)", "Slow Tempo", "Fast Tempo")
        
        val filteredReciters = remember(searchQuery, selectedFilter) {
            QuranData.RECITERS.filter {
                (selectedFilter == "All" || it.style.contains(selectedFilter, true) || it.tempo.contains(selectedFilter, true)) &&
                (it.nameEn.contains(searchQuery, true) || it.nameAr.contains(searchQuery, true))
            }
        }

        Scaffold(
            containerColor = Color(0xFF0A0A0F)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { },
                    active = false,
                    onActiveChange = { },
                    placeholder = { Text("Search reciters...", color = Color(0xFF8A8A96)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF8A8A96)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = SearchBarDefaults.colors(
                        containerColor = Color(0xFF141419),
                    )
                ) {}
                
                LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filters) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0xFF141419),
                                labelColor = Color(0xFFF0EDE6),
                                selectedContainerColor = Color(0xFFD4A853),
                                selectedLabelColor = Color(0xFF0A0A0F)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedFilter == filter,
                                borderColor = Color(0xFFD4A853)
                            )
                        )
                    }
                }
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredReciters) { reciter ->
                        ReciterCard(reciter = reciter, onClick = {
                            navigator.push(ReciterProfileScreen(reciter.slug))
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun ReciterCard(reciter: Reciter, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C24)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF141419)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = reciter.nameEn.take(1),
                    color = Color(0xFFD4A853),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = reciter.nameAr,
                color = Color(0xFFF0EDE6),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = reciter.nameEn,
                color = Color(0xFF8A8A96),
                style = MaterialTheme.typography.bodySmall
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF4A8C6F)
                ) {
                    Text(reciter.riwayah, style = MaterialTheme.typography.bodySmall, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFD4A853)
                ) {
                    Text(reciter.style, style = MaterialTheme.typography.bodySmall, color = Color.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = { /* Follow action */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF141419), contentColor = Color(0xFFD4A853)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Follow")
            }
        }
    }
}
