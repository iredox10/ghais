package com.quranify.ui.screens.mood

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen

import com.quranify.domain.model.RoutineModeType

data class RoutineModeScreen(val modeType: RoutineModeType) : Screen {
    @Composable
    override fun Content() {
        val modeColor = when (modeType) {
            RoutineModeType.STUDY -> Color(0xFF0F52BA) // Sapphire
            RoutineModeType.WORK -> Color(0xFF50C878) // Emerald
            RoutineModeType.SLEEP -> Color(0xFF9966CC) // Amethyst
        }

        val modeTitle = when (modeType) {
            RoutineModeType.STUDY -> "Study Mode"
            RoutineModeType.WORK -> "Deep Work"
            RoutineModeType.SLEEP -> "Restful Sleep"
        }

        val modeSubtitle = when (modeType) {
            RoutineModeType.STUDY -> "Focus-oriented recitation with gentle ambient sound"
            RoutineModeType.WORK -> "Clarity & deep focus with steady tempo recitations"
            RoutineModeType.SLEEP -> "Calming slow recitations to ease your mind"
        }

        val curatedSurahs = when (modeType) {
            RoutineModeType.STUDY -> listOf("Surah Al-Kahf", "Surah Taha", "Surah Ar-Rahman")
            RoutineModeType.WORK -> listOf("Surah Yaseen", "Surah Al-Waqi'ah", "Surah Al-Mulk")
            RoutineModeType.SLEEP -> listOf("Surah Al-Mulk", "Surah As-Sajdah", "Surah Yaseen")
        }
        
        Scaffold(
            containerColor = Color(0xFF0A0A0F)
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                item {
                    ModeHeader(modeTitle, modeSubtitle, modeColor)
                }
                
                item {
                    StartSessionButton(modeColor)
                }
                
                item {
                    CuratedSurahsShelf(curatedSurahs, modeColor)
                }
                
                item {
                    AmbientSoundControls(modeColor)
                }
            }
        }
    }
}

@Composable
fun ModeHeader(title: String, subtitle: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(color.copy(alpha = 0.3f), Color.Transparent)
                )
            ),
        contentAlignment = Alignment.BottomStart
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = title,
                color = Color(0xFFF0EDE6),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                color = Color(0xFF8A8A96),
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun StartSessionButton(color: Color) {
    Button(
        onClick = { /* Start Session */ },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = "Start", tint = Color.White)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Start Session", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun CuratedSurahsShelf(surahs: List<String>, color: Color) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(
            text = "Curated For You",
            color = Color(0xFFF0EDE6),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(surahs) { surah ->
                Card(
                    modifier = Modifier
                        .width(140.dp)
                        .height(100.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C24)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = surah,
                            color = Color(0xFFF0EDE6),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AmbientSoundControls(color: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C24)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = "Ambient Sounds", tint = color)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Live Ambient Sounds",
                    color = Color(0xFFF0EDE6),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            AmbientSlider("Rain Volume", color)
            Spacer(modifier = Modifier.height(8.dp))
            AmbientSlider("Wind Volume", color)
        }
    }
}

@Composable
fun AmbientSlider(label: String, color: Color) {
    var sliderPosition by remember { mutableStateOf(0.5f) }
    Column {
        Text(text = label, color = Color(0xFF8A8A96), fontSize = 14.sp)
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = Color(0xFF141419)
            )
        )
    }
}
