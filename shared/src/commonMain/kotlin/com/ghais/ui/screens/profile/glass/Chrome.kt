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
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.theme.GhaisNoir

@Composable
fun BoxScope.AuroraBackdrop() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GhaisNoir.NoirBlack)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-160).dp)
                .size(420.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.07f),
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

/** Legacy name kept — now a thin wrapper over the Noir chrome pill. */
@Composable
fun ChromeButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ChromePillButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        leadingIcon = icon
    )
}
