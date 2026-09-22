package com.ghais.ui.screens.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.theme.GhaisNoir

/**
 * Volume-mixer card on the Now Playing screen, in strict Noir Glass monochrome.
 *
 * A [NoirCard] (glass gradient fill + ghost border + top specular) with two
 * labeled rows: "Quran" and "Background sound". Sliders are pure monochrome —
 * white active track, low-alpha-white engraved inactive track, white thumb —
 * zero hue. Side icons read [GhaisNoir.TextPrimary].
 *
 * Behavior preserved:
 * - Quran row: left icon toggles mute/restore (restores last non-zero volume,
 *   default 1f); right icon sets 1f.
 * - Background row: side icons are decorative with no tap handling, so ambient
 *   volume only changes via the slider.
 */
@Composable
fun NowPlayingVolumePanel(
    quranVolume: Float,
    onQuranVolumeChange: (Float) -> Unit,
    ambientVolume: Float,
    onAmbientVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Last non-zero Quran volume used to restore after un-muting. Defaults to 1f.
    var lastNonZeroQuranVolume by remember { mutableStateOf(1f) }
    LaunchedEffect(quranVolume) {
        if (quranVolume > 0f) {
            lastNonZeroQuranVolume = quranVolume
        }
    }

    val sliderColors = SliderDefaults.colors(
        thumbColor = GhaisNoir.TextPrimary,
        activeTrackColor = GhaisNoir.TextPrimary,
        inactiveTrackColor = GhaisNoir.Fill4
    )

    NoirCard(modifier = modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Quran volume section.
            Column {
                Text(
                    text = "Quran",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = GhaisNoir.TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (quranVolume > 0f) {
                                onQuranVolumeChange(0f)
                            } else {
                                onQuranVolumeChange(lastNonZeroQuranVolume)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VolumeOff,
                            contentDescription = "Mute or unmute Quran",
                            tint = GhaisNoir.TextPrimary
                        )
                    }
                    Slider(
                        value = quranVolume.coerceIn(0f, 1f),
                        onValueChange = onQuranVolumeChange,
                        valueRange = 0f..1f,
                        colors = sliderColors,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onQuranVolumeChange(1f) }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VolumeUp,
                            contentDescription = "Set Quran volume to maximum",
                            tint = GhaisNoir.TextPrimary
                        )
                    }
                }
            }

            // Background sound section.
            Column {
                Text(
                    text = "Background sound",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = GhaisNoir.TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = GhaisNoir.TextPrimary
                        )
                    }
                    Slider(
                        value = ambientVolume.coerceIn(0f, 1f),
                        onValueChange = onAmbientVolumeChange,
                        valueRange = 0f..1f,
                        colors = sliderColors,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QueueMusic,
                            contentDescription = null,
                            tint = GhaisNoir.TextPrimary
                        )
                    }
                }
            }
        }
    }
}
