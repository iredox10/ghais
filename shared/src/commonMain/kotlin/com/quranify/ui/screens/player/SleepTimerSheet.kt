package com.quranify.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranify.player.SleepTimer
import com.quranify.player.StopCondition

private val Background = Color(0xFF0A0A0F)
private val SurfaceColor = Color(0xFF141419)
private val CardColor = Color(0xFF1C1C24)
private val Primary = Color(0xFFD4A853)
private val TextPrimary = Color(0xFFF0EDE6)
private val TextSecondary = Color(0xFF8A8A96)
private val ErrorColor = Color(0xFFCF6679)

@Composable
fun SleepTimerSheet() {
    val state by SleepTimer.state.collectAsState()

    Surface(
        color = SurfaceColor,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp, 4.dp)
                    .clip(CircleShape)
                    .background(TextSecondary.copy(alpha = 0.5f))
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Sleep Timer",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (state.isActive) {
                ActiveTimerView(
                    remainingSeconds = state.remainingSeconds,
                    condition = state.stopCondition,
                    onCancel = { SleepTimer.cancelTimer() }
                )
            } else {
                TimerSetupView()
            }
        }
    }
}

@Composable
private fun ActiveTimerView(
    remainingSeconds: Long,
    condition: StopCondition,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (condition == StopCondition.MINUTES) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(120.dp),
                    color = Primary,
                    trackColor = CardColor,
                    strokeWidth = 4.dp
                )
                
                val hours = remainingSeconds / 3600
                val minutes = (remainingSeconds % 3600) / 60
                val seconds = remainingSeconds % 60
                
                val timeString = if (hours > 0) {
                    "${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
                } else {
                    "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
                }
                
                Text(
                    text = timeString,
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            val statusText = when(condition) {
                StopCondition.END_OF_AYAH -> "Stopping after current Ayah"
                StopCondition.END_OF_SURAH -> "Stopping after current Surah"
                StopCondition.END_OF_QUEUE -> "Stopping after Queue ends"
                else -> ""
            }
            Text(
                text = statusText,
                color = Primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onCancel,
            colors = ButtonDefaults.buttonColors(containerColor = CardColor, contentColor = ErrorColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Cancel Timer",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TimerSetupView() {
    var fadeOutEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Duration",
            color = TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresetChip(text = "15m", onClick = { SleepTimer.startTimer(15, fadeOut = fadeOutEnabled) }, modifier = Modifier.weight(1f))
            PresetChip(text = "30m", onClick = { SleepTimer.startTimer(30, fadeOut = fadeOutEnabled) }, modifier = Modifier.weight(1f))
            PresetChip(text = "45m", onClick = { SleepTimer.startTimer(45, fadeOut = fadeOutEnabled) }, modifier = Modifier.weight(1f))
            PresetChip(text = "1h", onClick = { SleepTimer.startTimer(60, fadeOut = fadeOutEnabled) }, modifier = Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Boundaries",
            color = TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        BoundaryOption(
            title = "End of current Ayah",
            onClick = { SleepTimer.startAtAyahBoundary(fadeOut = fadeOutEnabled) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        BoundaryOption(
            title = "End of Surah",
            onClick = { SleepTimer.startAtSurahBoundary(fadeOut = fadeOutEnabled) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        BoundaryOption(
            title = "End of Queue",
            onClick = { SleepTimer.startAtQueueBoundary(fadeOut = fadeOutEnabled) }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardColor, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Gentle Fade-out",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Slowly lower volume over the last 30 seconds",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            
            Switch(
                checked = fadeOutEnabled,
                onCheckedChange = { fadeOutEnabled = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Background,
                    checkedTrackColor = Primary,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = SurfaceColor
                )
            )
        }
    }
}

@Composable
private fun PresetChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(CardColor)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Primary,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun BoundaryOption(
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardColor)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
