package com.quranify.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen

class NowPlayingScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val backgroundColor = Color(0xFF0A0A0F)
        val surfaceColor = Color(0xFF141419)
        val cardColor = Color(0xFF1C1C24)
        val primaryColor = Color(0xFFD4A853) // gold
        val secondaryColor = Color(0xFF4A8C6F)
        val textPrimaryColor = Color(0xFFF0EDE6)
        val textSecondaryColor = Color(0xFF8A8A96)
        
        var isPlaying by remember { mutableStateOf(false) }
        var progress by remember { mutableStateOf(0.45f) }
        var isLiked by remember { mutableStateOf(false) }
        var showQueue by remember { mutableStateOf(false) }
        
        if (showQueue) {
            QueueSheet(onDismiss = { showQueue = false })
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* Minimize */ }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Minimize",
                            tint = textPrimaryColor
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NOW PLAYING",
                            color = textSecondaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Mishary Rashid Alafasy",
                            color = textPrimaryColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = { /* More Options */ }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = textPrimaryColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Center Visual
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .clip(CircleShape)
                        .background(surfaceColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "سورة البقرة",
                        color = primaryColor,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Track Details
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Al-Baqarah",
                        color = textPrimaryColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ayah 255 of 286",
                        color = textSecondaryColor,
                        fontSize = 16.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Current Ayah Arabic Text
                Surface(
                    color = cardColor,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "اللَّهُ لَا إِلَهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ",
                        color = textPrimaryColor,
                        fontSize = 22.sp,
                        modifier = Modifier.padding(24.dp),
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Progress Bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = progress,
                        onValueChange = { progress = it },
                        colors = SliderDefaults.colors(
                            thumbColor = primaryColor,
                            activeTrackColor = primaryColor,
                            inactiveTrackColor = textSecondaryColor.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("02:45", color = textSecondaryColor, fontSize = 12.sp)
                        Text("-01:15", color = textSecondaryColor, fontSize = 12.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { isLiked = !isLiked }) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) primaryColor else textSecondaryColor
                        )
                    }
                    
                    Text("⏮", color = textPrimaryColor, fontSize = 24.sp, modifier = Modifier.clickable { })
                    
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(primaryColor)
                            .clickable { isPlaying = !isPlaying },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = backgroundColor,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    
                    Text("⏭", color = textPrimaryColor, fontSize = 24.sp, modifier = Modifier.clickable { })
                    
                    IconButton(onClick = { showQueue = true }) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Queue",
                            tint = textSecondaryColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Bottom Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* Mixer */ }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ambient Mixer",
                            tint = textSecondaryColor
                        )
                    }
                    Text(
                        text = "1x",
                        color = textSecondaryColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { /* Change Speed */ }
                    )
                    IconButton(onClick = { /* Repeat Mode */ }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Repeat",
                            tint = textSecondaryColor
                        )
                    }
                }
            }
        }
    }
}
