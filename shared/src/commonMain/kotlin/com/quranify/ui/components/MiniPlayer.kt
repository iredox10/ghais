package com.quranify.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import com.quranify.player.AudioEngine
import com.quranify.ui.screens.player.NowPlayingScreen
import com.quranify.ui.theme.QuranifyColors

@Composable
fun MiniPlayer(modifier: Modifier = Modifier) {
    val currentTrack by AudioEngine.currentTrack.collectAsState()
    val isPlaying by AudioEngine.isPlaying.collectAsState()
    val progress by AudioEngine.progress.collectAsState()
    
    val track = currentTrack ?: return
    val navigator = LocalNavigator.current

    // Animated progress
    val animatedProgress by animateFloatAsState(targetValue = progress)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .background(
                color = QuranifyColors.CardElevated,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = QuranifyColors.Primary.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable { 
                navigator?.parent?.push(NowPlayingScreen()) ?: navigator?.push(NowPlayingScreen()) 
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    change.consume()
                    if (dragAmount > 20) {
                        AudioEngine.clear()
                    }
                }
            }
    ) {
        Column {
            // Linear progress indicator at the very top
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = QuranifyColors.Primary,
                trackColor = QuranifyColors.Surface
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Rounded cover thumbnail with Surah number
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(QuranifyColors.Surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = track.surahId.toString(),
                        color = QuranifyColors.Primary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Center: Surah English & Arabic name + Ayah number
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = track.surahNameEn,
                            color = QuranifyColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = track.surahNameAr,
                            color = QuranifyColors.Primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Ayah ${track.ayahNo} • ${track.reciterName}",
                        color = QuranifyColors.TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Right: Quick Play/Pause button, Next Ayah button, and Ambient sound active indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ambient indicator
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(QuranifyColors.Secondary)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(onClick = { AudioEngine.togglePlayPause() }) {
                        Text(
                            text = if (isPlaying) "⏸" else "▶", 
                            color = QuranifyColors.TextPrimary,
                            fontSize = 18.sp
                        )
                    }
                    IconButton(onClick = { AudioEngine.nextAyah() }) {
                        Text(
                            text = "⏭", 
                            color = QuranifyColors.TextPrimary,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}
