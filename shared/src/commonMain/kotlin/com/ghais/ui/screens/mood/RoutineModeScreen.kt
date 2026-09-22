package com.ghais.ui.screens.mood

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ghais.domain.model.RoutineModeType
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography

data class RoutineModeScreen(val modeType: RoutineModeType) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

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

        val modeIcon: ImageVector = when (modeType) {
            RoutineModeType.STUDY -> Icons.AutoMirrored.Filled.MenuBook
            RoutineModeType.WORK -> Icons.Filled.Work
            RoutineModeType.SLEEP -> Icons.Filled.Bedtime
        }

        val curatedSurahs = when (modeType) {
            RoutineModeType.STUDY -> listOf("Surah Al-Kahf", "Surah Taha", "Surah Ar-Rahman")
            RoutineModeType.WORK -> listOf("Surah Yaseen", "Surah Al-Waqi'ah", "Surah Al-Mulk")
            RoutineModeType.SLEEP -> listOf("Surah Al-Mulk", "Surah As-Sajdah", "Surah Yaseen")
        }

        NoirScreenRoot {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(GhaisNoir.Fill2)
                                .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                                .noirClickable { navigator.pop() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = GhaisNoir.TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = modeTitle,
                            style = GhaisTypography.titleMedium.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = GhaisNoir.TextPrimary
                            )
                        )
                    }
                }

                item {
                    ModeHeader(
                        title = modeTitle,
                        subtitle = modeSubtitle,
                        color = Color.White,
                        icon = modeIcon
                    )
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        StartSessionButton(color = Color.White)
                    }
                }

                item {
                    CuratedSurahsShelf(curatedSurahs, Color.White)
                }

                item {
                    AmbientSoundControls(Color.White)
                }
            }
        }
    }
}

/**
 * Noir hero: glass plate + clay icon-well + ghost badge + sheen. Zero hue.
 * [color] retained for signature compatibility and intentionally ignored.
 */
@Composable
fun ModeHeader(title: String, subtitle: String, color: Color, icon: ImageVector = Icons.Filled.PlayArrow) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(GhaisShapes.cardLarge)
            .background(GhaisNoir.cardFill())
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.cardLarge)
            .topSpecular(inset = 30.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(GhaisNoir.sheen())
        )
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconWell(icon = icon, size = 52.dp, iconSize = 24.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.Fill2)
                        .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "ROUTINE MODE",
                        style = GhaisTypography.bodyMedium.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = GhaisNoir.TextTertiary
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = GhaisTypography.titleMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = GhaisNoir.TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = GhaisTypography.bodyMedium.copy(
                    fontSize = 14.sp,
                    color = GhaisNoir.TextSecondary
                )
            )
        }
    }
}

/**
 * Chrome primary CTA. [color] retained for compatibility, ignored (zero hue).
 */
@Composable
fun StartSessionButton(color: Color) {
    ChromePillButton(
        text = "Start Routine Session",
        onClick = { /* Start Session */ },
        leadingIcon = Icons.Default.PlayArrow,
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Bento shelf: identical glass plates with clay wells (home routine pattern).
 * [color] retained for compatibility, ignored.
 */
@Composable
fun CuratedSurahsShelf(surahs: List<String>, color: Color) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        NoirSectionHeader(
            label = "Curated for you",
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(surahs) { surah ->
                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .height(132.dp)
                        .clip(GhaisShapes.cardNoir)
                        .background(GhaisNoir.cardFillSoft())
                        .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.cardNoir)
                        .topSpecular(inset = 24.dp)
                        .padding(14.dp)
                ) {
                    IconWell(
                        icon = Icons.Default.PlayArrow,
                        size = 40.dp,
                        iconSize = 20.dp,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                    Text(
                        text = surah,
                        style = GhaisTypography.titleMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GhaisNoir.TextPrimary
                        ),
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                }
            }
        }
    }
}

/**
 * Ambient panel: noir card + engraved chrome sliders. [color] ignored.
 */
@Composable
fun AmbientSoundControls(color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(GhaisShapes.cardNoir)
            .background(GhaisNoir.cardFillSoft())
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.cardNoir)
            .topSpecular(inset = 26.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconWell(icon = Icons.Default.Settings, size = 40.dp, iconSize = 20.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Live Ambient Sounds",
                        style = GhaisTypography.titleMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = GhaisNoir.TextPrimary
                        )
                    )
                    Text(
                        text = "Blend calming background nature sounds",
                        style = GhaisTypography.bodyMedium.copy(
                            fontSize = 12.sp,
                            color = GhaisNoir.TextSecondary
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            AmbientSlider("Rain Volume", Color.White)
            Spacer(modifier = Modifier.height(12.dp))
            AmbientSlider("Wind Volume", Color.White)
        }
    }
}

/**
 * Monochrome slider: chrome thumb + white active track on engraved rail.
 * [color] retained for compatibility, ignored.
 */
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
                style = GhaisTypography.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = GhaisNoir.TextSecondary
                )
            )
            Text(
                text = "${(sliderPosition * 100).toInt()}%",
                style = GhaisTypography.bodyMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GhaisNoir.TextPrimary
                )
            )
        }
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = GhaisNoir.Fill4
            )
        )
    }
}
