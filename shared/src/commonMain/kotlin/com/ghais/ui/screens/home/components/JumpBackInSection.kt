package com.ghais.ui.screens.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghais.data.seed.StitchAssets
import com.ghais.ui.theme.QuranifyColors

@Composable
fun JumpBackInSection(
    onCardClick: (String) -> Unit = {}
) {
    val items = StitchAssets.JumpBackInItems

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Jump Back In",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                // Pulsing dot
                val infiniteTransition = rememberInfiniteTransition()
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .alpha(alpha)
                        .background(QuranifyColors.TrendingPurple, CircleShape)
                )
            }
            
            Text(
                text = "History",
                fontSize = 12.sp,
                color = QuranifyColors.TextSecondary,
                modifier = Modifier.clickable { /* Handle click */ }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 2x2 Grid of Cards
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val item0 = items.getOrNull(0)
                JumpBackInCard(
                    modifier = Modifier.weight(1f),
                    title = item0?.title ?: "Al-Mulk",
                    subtitle = item0?.subtitle ?: "Ayah 14 • 4:12 left",
                    subtitleColor = QuranifyColors.TextSecondary,
                    progress = item0?.progress ?: 0.67f,
                    coverUrl = item0?.coverUrl ?: "",
                    onClick = { onCardClick("Al-Mulk") }
                )
                val item1 = items.getOrNull(1)
                JumpBackInCard(
                    modifier = Modifier.weight(1f),
                    title = item1?.title ?: "Al-Kahf",
                    subtitle = item1?.subtitle ?: "Friday Sunnah",
                    subtitleColor = QuranifyColors.NeonLilac,
                    progress = item1?.progress ?: 0.25f,
                    coverUrl = item1?.coverUrl ?: "",
                    onClick = { onCardClick("Al-Kahf") }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val item2 = items.getOrNull(2)
                JumpBackInCard(
                    modifier = Modifier.weight(1f),
                    title = item2?.title ?: "Surah Yaseen",
                    subtitle = item2?.subtitle ?: "Mishary Alafasy",
                    subtitleColor = QuranifyColors.TextSecondary,
                    progress = item2?.progress ?: 0.80f,
                    coverUrl = item2?.coverUrl ?: "",
                    onClick = { onCardClick("Yaseen") }
                )
                val item3 = items.getOrNull(3)
                JumpBackInCard(
                    modifier = Modifier.weight(1f),
                    title = item3?.title ?: "Tahajjud Peace",
                    subtitle = item3?.subtitle ?: "Heart Softeners",
                    subtitleColor = QuranifyColors.TextSecondary,
                    progress = item3?.progress ?: 0.50f,
                    coverUrl = item3?.coverUrl ?: "",
                    onClick = { onCardClick("Tahajjud") }
                )
            }
        }
    }
}

@Composable
fun JumpBackInCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    subtitleColor: Color = QuranifyColors.TextSecondary,
    progress: Float,
    progressBrush: Brush = Brush.horizontalGradient(
        listOf(QuranifyColors.ElectricViolet, QuranifyColors.TrendingPurple)
    ),
    coverUrl: String,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(QuranifyColors.ObsidianCard)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: 48.dp square cover image with subtle border
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(QuranifyColors.SurfaceHigh)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (coverUrl.isNotEmpty()) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = title.firstOrNull()?.toString() ?: "",
                    color = QuranifyColors.TrendingPurple,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.width(10.dp))
        
        // Right: Column with title, subtitle, and progress bar
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Text(
                text = subtitle,
                color = subtitleColor,
                fontSize = 11.sp,
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Mini progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(progressBrush, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}
