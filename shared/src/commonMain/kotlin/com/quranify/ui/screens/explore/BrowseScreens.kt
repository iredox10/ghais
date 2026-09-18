package com.quranify.ui.screens.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.quranify.data.seed.QuranData
import com.quranify.ui.screens.surah.SurahDetailScreen
import com.quranify.ui.theme.QuranifyColors

object SurahsScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("All Surahs", color = QuranifyColors.TextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = QuranifyColors.TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = QuranifyColors.Background)
                )
            },
            containerColor = QuranifyColors.Background
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(QuranData.SURAHS) { surah ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navigator.push(SurahDetailScreen(surah.id)) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(QuranifyColors.Card, shape = MaterialTheme.shapes.small),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = surah.id.toString(),
                                color = QuranifyColors.Primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(surah.nameEn, color = QuranifyColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("${surah.revelationType} • ${surah.ayahsCount} Ayahs", color = QuranifyColors.TextSecondary, fontSize = 12.sp)
                        }
                        Text(surah.nameAr, color = QuranifyColors.Primary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

object JuzBrowserScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Juz Browser", color = QuranifyColors.TextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = QuranifyColors.TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = QuranifyColors.Background)
                )
            },
            containerColor = QuranifyColors.Background
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Juz Browser Coming Soon", color = QuranifyColors.TextSecondary)
            }
        }
    }
}
