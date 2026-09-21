package com.ghais.ui.screens.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Volume-mixer card shown on the Now Playing screen.
 *
 * A full-width rounded (28dp) translucent steel-blue glass card with two labeled
 * white sliders: "Quran" and "Background sound".
 *
 * Design-token choices:
 * - Card background: vertical gradient from Color(0xFF3E5F86) at 0.75 alpha (top)
 *   to Color(0xFF2C4562) at 0.75 alpha (bottom).
 * - Content padding: 20dp horizontal / 16dp vertical; 12dp spacing between rows.
 * - Labels: muted blue-grey text at ~15sp.
 * - Sliders: white active track, translucent-white inactive track, white thumb.
 *
 * API compromises (for compile safety against the repo's Material3 version):
 * - Sliders use [SliderDefaults.colors] with the default thumb tinted white instead
 *   of custom `thumb`/`track` slots. The custom slot signature changed between M3
 *   1.2.x (`thumb: @Composable () -> Unit`) and 1.3.x
 *   (`thumb: @Composable (SliderState) -> Unit`), and the shared module resolves
 *   Material3 via Compose Multiplatform (not the androidApp androidx BOM), so the
 *   default thumb (circular, white) is used instead of the reference's white
 *   capsule (~28x18dp rounded-full) thumb.
 * - Quran row: left icon is always [Icons.Filled.VolumeOff] (muted speaker with x,
 *   tap toggles mute/restore); right icon is [Icons.Filled.VolumeUp] (tap sets 1f).
 * - Background row: side icons ([Icons.Filled.MusicNote] left,
 *   [Icons.Filled.QueueMusic] right) are decorative with no tap handling, so
 *   ambient volume only changes via the slider. This avoids accidental jumps and
 *   keeps the row a pure display affordance.
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
        thumbColor = Color.White,
        activeTrackColor = Color.White,
        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF3E5F86).copy(alpha = 0.75f),
                        Color(0xFF2C4562).copy(alpha = 0.75f)
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quran volume section.
        Column {
            Text(
                text = "Quran",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MutedBlueGrey
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
                        tint = Color.White
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
                        tint = Color.White
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
                color = MutedBlueGrey
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
                        tint = Color.White
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
                        tint = Color.White
                    )
                }
            }
        }
    }
}

private val MutedBlueGrey = Color(0xFF9DB4CC)
