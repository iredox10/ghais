package com.ghais.ui.screens.player

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val queue by AudioEngine.queue.collectAsState()
    val currentIndex by AudioEngine.currentIndex.collectAsState()
    val currentTrack by AudioEngine.currentTrack.collectAsState()
    val engineDurationMs by AudioEngine.durationMs.collectAsState()

    val surfaceColor = Color(0xFF141419)
    val cardColor = Color(0xFF1C1C24)
    val primaryColor = Color(0xFFD4A853)
    val textPrimaryColor = Color(0xFFF0EDE6)
    val textSecondaryColor = Color(0xFF8A8A96)

    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    fun animateDismiss() {
        coroutineScope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                onDismiss()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = surfaceColor,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Up Next",
                        color = textPrimaryColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (queue.isNotEmpty()) {
                        val posText = if (currentIndex in queue.indices) {
                            "Track ${currentIndex + 1} of ${queue.size}"
                        } else {
                            "${queue.size} tracks"
                        }
                        Text(
                            text = posText,
                            color = primaryColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { AudioEngine.clear() },
                        enabled = queue.isNotEmpty()
                    ) {
                        Text(
                            text = "Clear Queue",
                            color = if (queue.isNotEmpty()) primaryColor else textSecondaryColor.copy(alpha = 0.4f)
                        )
                    }
                    IconButton(onClick = { animateDismiss() }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Close",
                            tint = textSecondaryColor
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Empty Queue",
                            tint = textSecondaryColor.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "Queue is empty",
                            color = textPrimaryColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Play a surah or playlist to see the queue",
                            color = textSecondaryColor,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(
                        items = queue,
                        key = { index, item -> "${item.audioUrl}_${item.surahId}_$index" }
                    ) { index, item ->
                        val isActive = index == currentIndex || (currentIndex == -1 && currentTrack?.audioUrl == item.audioUrl)

                        val itemDuration = if (isActive && engineDurationMs > 0L) {
                            engineDurationMs
                        } else {
                            item.durationMs
                        }
                        val formattedDuration = if (itemDuration > 0L) formatQueueDuration(itemDuration) else ""

                        val surahTitle = when {
                            item.surahId > 0 && item.surahNameEn.isNotBlank() -> "${item.surahId}. ${item.surahNameEn}"
                            item.surahNameEn.isNotBlank() -> item.surahNameEn
                            item.surahId > 0 -> "Surah ${item.surahId}"
                            else -> "Unknown Surah"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isActive) primaryColor.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { AudioEngine.skipToIndex(index) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isActive) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = "Currently Playing",
                                        tint = primaryColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Text(
                                        text = "${index + 1}",
                                        color = textSecondaryColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = surahTitle,
                                        color = if (isActive) primaryColor else textPrimaryColor,
                                        fontSize = 15.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (item.surahNameAr.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = item.surahNameAr,
                                            color = if (isActive) primaryColor.copy(alpha = 0.85f) else textSecondaryColor.copy(alpha = 0.8f),
                                            fontSize = 14.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.reciterName.ifBlank { "Quran Recitation" },
                                        color = textSecondaryColor,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (formattedDuration.isNotEmpty()) {
                                        Text(
                                            text = " • $formattedDuration",
                                            color = if (isActive) primaryColor.copy(alpha = 0.75f) else textSecondaryColor,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick = { AudioEngine.removeFromQueue(index) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Remove from queue",
                                    tint = textSecondaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun formatQueueDuration(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}
