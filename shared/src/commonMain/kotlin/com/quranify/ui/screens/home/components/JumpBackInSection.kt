package com.quranify.ui.screens.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranify.ui.theme.QuranifyColors
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items

@Composable
fun JumpBackInSection(
    onCardClick: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                    color = QuranifyColors.TextPrimary
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
                        .background(QuranifyColors.Primary, CircleShape)
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
        // We'll use a Column of Rows to achieve a 2x2 grid without fixed height constraints that LazyVerticalGrid might impose.
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
                JumpBackInCard(
                    modifier = Modifier.weight(1f),
                    title = "Surah Al-Mulk",
                    subtitle = "Ayah 14 • 4:12 left",
                    subtitleColor = QuranifyColors.TextSecondary,
                    progress = 0.67f,
                    progressColor = QuranifyColors.Primary,
                    onClick = { onCardClick("Al-Mulk") }
                )
                JumpBackInCard(
                    modifier = Modifier.weight(1f),
                    title = "Al-Kahf",
                    subtitle = "Friday Sunnah",
                    subtitleColor = QuranifyColors.Secondary,
                    progress = 0.25f,
                    progressColor = QuranifyColors.Secondary,
                    onClick = { onCardClick("Al-Kahf") }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                JumpBackInCard(
                    modifier = Modifier.weight(1f),
                    title = "Surah Yaseen",
                    subtitle = "Mishary Alafasy",
                    subtitleColor = QuranifyColors.TextSecondary,
                    progress = 0.80f,
                    progressColor = QuranifyColors.Primary,
                    onClick = { onCardClick("Yaseen") }
                )
                JumpBackInCard(
                    modifier = Modifier.weight(1f),
                    title = "Tahajjud Peace",
                    subtitle = "Heart Softeners",
                    subtitleColor = QuranifyColors.TextSecondary,
                    progress = 0.50f,
                    progressColor = QuranifyColors.Primary,
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
    subtitleColor: Color,
    progress: Float,
    progressColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .background(QuranifyColors.Card, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: 48.dp square cover image
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(QuranifyColors.Surface, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Fallback icon/text
            Text(
                text = title.firstOrNull()?.toString() ?: "",
                color = QuranifyColors.Primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.width(10.dp))
        
        // Right: Column with title, subtitle, and progress bar
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = QuranifyColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Text(
                text = subtitle,
                color = subtitleColor,
                fontSize = 12.sp,
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Mini progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(QuranifyColors.Surface, RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(progressColor, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}
