package com.quranify.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranify.player.AmbientChannel
import com.quranify.player.AmbientMixer

val quranifyBackground = Color(0xFF0A0A0F)
val quranifySurface = Color(0xFF141419)
val quranifyCard = Color(0xFF1C1C24)
val quranifyPrimary = Color(0xFFD4A853)
val quranifySecondary = Color(0xFF4A8C6F)
val quranifyTextPrimary = Color(0xFFF0EDE6)
val quranifyTextSecondary = Color(0xFF8A8A96)
val quranifyError = Color(0xFFCF6679)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmbientMixerSheet(
    mixer: AmbientMixer,
    onDismissRequest: () -> Unit
) {
    val channels by mixer.channels.collectAsState()
    val masterVolume by mixer.masterAmbientVolume.collectAsState()
    val quranVolume by mixer.quranVolume.collectAsState()
    val isMuted by mixer.isMuted.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = quranifySurface,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(quranifyTextSecondary.copy(alpha = 0.3f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ambient Sounds",
                    color = quranifyTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onDismissRequest) {
                    Text("Done", color = quranifyPrimary, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Volumes
            Text(text = "Master Mix", color = quranifyTextSecondary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Quran", color = quranifyTextPrimary, fontSize = 14.sp, modifier = Modifier.weight(0.2f))
                Slider(
                    value = quranVolume,
                    onValueChange = { mixer.setQuranVolume(it) },
                    modifier = Modifier.weight(0.6f),
                    colors = SliderDefaults.colors(
                        thumbColor = quranifyPrimary,
                        activeTrackColor = quranifyPrimary,
                        inactiveTrackColor = quranifyCard
                    )
                )
                Text("${(quranVolume * 100).toInt()}%", color = quranifyTextSecondary, fontSize = 12.sp, modifier = Modifier.weight(0.2f), textAlign = TextAlign.End)
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ambient", color = quranifyTextPrimary, fontSize = 14.sp, modifier = Modifier.weight(0.2f))
                Slider(
                    value = masterVolume,
                    onValueChange = { mixer.setMasterAmbientVolume(it) },
                    modifier = Modifier.weight(0.6f),
                    colors = SliderDefaults.colors(
                        thumbColor = quranifySecondary,
                        activeTrackColor = quranifySecondary,
                        inactiveTrackColor = quranifyCard
                    )
                )
                Text("${(masterVolume * 100).toInt()}%", color = quranifyTextSecondary, fontSize = 12.sp, modifier = Modifier.weight(0.2f), textAlign = TextAlign.End)
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Presets
            Text(text = "Presets", color = quranifyTextSecondary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PresetChip("Study Focus", onClick = { mixer.applyPreset("Study Focus") })
                PresetChip("Deep Sleep", onClick = { mixer.applyPreset("Deep Sleep") })
                PresetChip("Nature Calm", onClick = { mixer.applyPreset("Nature Calm") })
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Channels
            Text(text = "Sounds", color = quranifyTextSecondary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(channels) { channel ->
                    AmbientChannelItem(
                        channel = channel,
                        onToggle = { mixer.toggleChannel(channel.type) },
                        onVolumeChange = { volume -> mixer.setVolume(channel.type, volume) }
                    )
                }
            }
        }
    }
}

@Composable
fun PresetChip(name: String, onClick: () -> Unit) {
    Surface(
        color = quranifyCard,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = name,
            color = quranifyTextPrimary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun AmbientChannelItem(
    channel: AmbientChannel,
    onToggle: () -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    val bgColor = if (channel.isEnabled) quranifySecondary.copy(alpha = 0.15f) else quranifyCard
    val iconColor = if (channel.isEnabled) quranifySecondary else quranifyTextSecondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onToggle)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (channel.isEnabled) quranifySecondary.copy(alpha = 0.2f) else quranifySurface),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = channel.type.name.take(1),
                color = iconColor,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel.type.name.replace(Regex("([a-z])([A-Z]+)"), "$1 $2"),
                color = if (channel.isEnabled) quranifyTextPrimary else quranifyTextSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            
            if (channel.isEnabled) {
                Slider(
                    value = channel.volume,
                    onValueChange = onVolumeChange,
                    modifier = Modifier.height(24.dp).padding(top = 4.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = quranifySecondary,
                        activeTrackColor = quranifySecondary,
                        inactiveTrackColor = quranifySurface
                    )
                )
            }
        }
    }
}
