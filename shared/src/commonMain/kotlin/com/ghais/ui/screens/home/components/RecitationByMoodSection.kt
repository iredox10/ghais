package com.ghais.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.ui.theme.GhaisColors

// Trending Purple & Monochrome Palette Tokens
private val ObsidianCardBg = Color(0xFF121418)
private val ElectricPurple = Color(0xFFA855F7)
private val RoyalViolet = Color(0xFF8B5CF6)
private val DeepPurple = Color(0xFF6D28D9)
private val Amethyst = Color(0xFFC084FC)

@Composable
fun RecitationByMoodSection(
    onMoodClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recitation by Mood",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, ElectricPurple.copy(alpha = 0.30f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "4 Modes",
                    fontSize = 11.sp,
                    color = Color(0xFFD8B4FE),
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(14.dp))
        
        // 2x2 Grid of Mood Tiles
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MoodTile(
                    modifier = Modifier.weight(1f),
                    title = "Gratitude (Shukr)",
                    subtitle = "Surah Ibrahim • Luqman",
                    icon = Icons.Rounded.Face,
                    containerColor = ElectricPurple,
                    onClick = { onMoodClick("gratitude") }
                )
                MoodTile(
                    modifier = Modifier.weight(1f),
                    title = "Anxiety & Relief",
                    subtitle = "Ash-Sharh • Ad-Duha",
                    icon = Icons.Rounded.Favorite,
                    containerColor = RoyalViolet,
                    onClick = { onMoodClick("anxiety") }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MoodTile(
                    modifier = Modifier.weight(1f),
                    title = "Hifz Memorization",
                    subtitle = "Repeat loop mode 3x",
                    icon = Icons.Rounded.Lightbulb,
                    containerColor = DeepPurple,
                    onClick = { onMoodClick("hifz") }
                )
                MoodTile(
                    modifier = Modifier.weight(1f),
                    title = "Qiyam al-Layl",
                    subtitle = "Long slow recitations",
                    icon = Icons.Rounded.DarkMode,
                    containerColor = Amethyst,
                    onClick = { onMoodClick("qiyam") }
                )
            }
        }
    }
}

@Composable
private fun MoodTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianCardBg)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(containerColor.copy(alpha = 0.16f))
                .border(
                    width = 1.dp,
                    color = containerColor.copy(alpha = 0.32f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = GhaisColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
