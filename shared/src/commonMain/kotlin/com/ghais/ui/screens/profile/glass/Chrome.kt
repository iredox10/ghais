package com.ghais.ui.screens.profile.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BoxScope.AuroraBackdrop() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-160).dp)
                .size(420.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF4EDEA3).copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )
        )
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val gridColor = Color.White.copy(alpha = 0.03f)
            val vStep = maxWidth / 6
            for (i in 0 until 5) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .offset(x = vStep * (i + 1))
                        .background(gridColor)
                )
            }
            val hStep = maxHeight / 9
            for (j in 0 until 8) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .offset(y = hStep * (j + 1))
                        .background(gridColor)
                )
            }
        }
    }
}

@Composable
fun ChromeButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pill = RoundedCornerShape(50.dp)
    val dark = Color(0xFF0B0C0E)
    Row(
        modifier = modifier
            .clip(pill)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF5F5F7),
                        Color(0xFFC7C7CC)
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = dark,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            color = dark,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
