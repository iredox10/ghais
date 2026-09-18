package com.quranify.ui.screens.mood

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.quranify.domain.model.RoutineModeType
import com.quranify.ui.theme.QuranifyColors
import com.quranify.ui.theme.QuranifyTypography

private val SheetContainer = Color(0xFF181E20)
private val SheetBorder = Color(0x204EDEA3)
private val LiquidEmerald = Color(0xFF4EDEA3)
private val CardBackground = Color(0xFF1F2527)
private val CardBorder = Color(0x184EDEA3)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8E989C)

data class RoutineModeScreen(val modeType: RoutineModeType) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val modeColor = when (modeType) {
            RoutineModeType.STUDY -> Color(0xFF4EDEA3) // Vivid Liquid Emerald
            RoutineModeType.WORK -> Color(0xFF10B981)  // Forest Emerald
            RoutineModeType.SLEEP -> Color(0xFF85F8C4) // Mint Glow Emerald
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
            containerColor = QuranifyColors.Background,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = modeTitle,
                        style = QuranifyTypography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 32.dp)
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
            .height(180.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(color.copy(alpha = 0.22f), Color.Transparent)
                )
            ),
        contentAlignment = Alignment.BottomStart
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(LiquidEmerald.copy(alpha = 0.15f))
                    .border(1.dp, LiquidEmerald.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "ROUTINE MODE",
                    style = QuranifyTypography.bodyMedium.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = LiquidEmerald
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = QuranifyTypography.titleMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = QuranifyTypography.bodyMedium.copy(
                    fontSize = 14.sp,
                    color = TextSecondary
                )
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
            .height(54.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = LiquidEmerald,
            contentColor = Color(0xFF003824)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Start",
            tint = Color(0xFF003824)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Start Routine Session",
            style = QuranifyTypography.titleMedium.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF003824)
            )
        )
    }
}

@Composable
fun CuratedSurahsShelf(surahs: List<String>, color: Color) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(LiquidEmerald)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "CURATED FOR YOU",
                style = QuranifyTypography.bodyMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextSecondary
                )
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(surahs) { surah ->
                Card(
                    modifier = Modifier
                        .width(160.dp)
                        .height(96.dp)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LiquidEmerald.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = LiquidEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = surah,
                                style = QuranifyTypography.titleMedium.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            )
                        }
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
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .border(1.dp, SheetBorder, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = SheetContainer),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(LiquidEmerald.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Ambient Sounds",
                        tint = LiquidEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Live Ambient Sounds",
                        style = QuranifyTypography.titleMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Blend calming background nature sounds",
                        style = QuranifyTypography.bodyMedium.copy(
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            AmbientSlider("Rain Volume", LiquidEmerald)
            Spacer(modifier = Modifier.height(12.dp))
            AmbientSlider("Wind Volume", LiquidEmerald)
        }
    }
}

@Composable
fun AmbientSlider(label: String, color: Color) {
    var sliderPosition by remember { mutableStateOf(0.5f) }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = QuranifyTypography.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            )
            Text(
                text = "${(sliderPosition * 100).toInt()}%",
                style = QuranifyTypography.bodyMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
        }
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = Color(0xFF141819)
            )
        )
    }
}
