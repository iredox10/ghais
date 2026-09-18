package com.quranify.ui.screens.reciters

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
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

data class ReciterProfileScreen(val reciterSlug: String) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val reciter = remember(reciterSlug) {
            QuranData.RECITERS.find { it.slug == reciterSlug }
        }
        val surahs = remember { QuranData.SURAHS }

        if (reciter == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Reciter not found", color = Color(0xFFCF6679))
            }
            return
        }

        Scaffold(
            containerColor = Color(0xFF0A0A0F),
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFFF0EDE6))
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
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1C1C24)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = reciter.nameEn.take(1),
                                color = Color(0xFFD4A853),
                                style = MaterialTheme.typography.displaySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = reciter.nameAr,
                            color = Color(0xFFF0EDE6),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = reciter.nameEn,
                            color = Color(0xFF8A8A96),
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF4A8C6F)
                            ) {
                                Text(reciter.style, style = MaterialTheme.typography.bodySmall, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFD4A853)
                            ) {
                                Text(reciter.tempo, style = MaterialTheme.typography.bodySmall, color = Color.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = { /* Start Radio */ },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4A853), contentColor = Color(0xFF0A0A0F))
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Radio")
                            }
                            Button(
                                onClick = { /* Shuffle Play */ },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF141419), contentColor = Color(0xFFF0EDE6))
                            ) {
                                Icon(Icons.Default.Shuffle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Shuffle Play")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { /* Follow/Liked */ },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C24), contentColor = Color(0xFFD4A853))
                        ) {
                            Text("Follow Reciter")
                        }
                    }
                    
                    Text(
                        text = "Discography",
                        color = Color(0xFFF0EDE6),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(surahs) { surah ->
                    SurahListItem(surah = surah)
                }
            }
        }
    }
}

@Composable
fun SurahListItem(surah: Surah) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Play surah */ }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1C1C24)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = surah.number.toString(),
                color = Color(0xFF8A8A96),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = surah.nameAr,
                color = Color(0xFFF0EDE6),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${surah.nameEn} • ${surah.ayahs} Ayahs",
                color = Color(0xFF8A8A96),
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        IconButton(onClick = { /* Download */ }) {
            Icon(Icons.Default.Download, contentDescription = "Download", tint = Color(0xFF8A8A96))
        }
        IconButton(onClick = { /* Play */ }) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color(0xFFD4A853))
        }
    }
}
