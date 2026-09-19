package com.quranify.ui.screens.reciters

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PureBlack = Color(0xFF000000)
private val DarkCard = Color(0xFF1C1C1E)
private val MutedGrey = Color(0xFF9A9AA0)
private val LinkBlue = Color(0xFF4C8DFF)

private val HeroGradientTop = Color(0xFF2E7CF6).copy(alpha = 0.5f)
private val HeroGradientBottom = Color(0xFF0A1F44)

@Composable
fun RecitersHero(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    countText: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Hero block: full-width 250dp, rounded 32dp, horizontal 16dp margins
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(250.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(HeroGradientTop, HeroGradientBottom)
                    )
                )
        ) {
            // Layered radial glows (blue + violet, alpha 0.35)
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .offset(x = (-70).dp, y = (-70).dp)
                    .align(Alignment.TopStart)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                LinkBlue.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .offset(x = 70.dp, y = (-50).dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF8B5CF6).copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Giant translucent Mosque icon, centered
            Icon(
                imageVector = Icons.Filled.Mosque,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.12f),
                modifier = Modifier
                    .size(160.dp)
                    .align(Alignment.Center)
            )

            // Bottom-up BLACK gradient scrim for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                PureBlack.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // Overlaid bottom-start content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Text(
                        text = "Reciters",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = countText,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Search pill below hero
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.07f))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MutedGrey,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 15.sp
                ),
                decorationBox = { inner ->
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search reciters...",
                            color = MutedGrey,
                            fontSize = 15.sp
                        )
                    }
                    inner()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
