package com.ghais.ui.screens.reciters

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
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * Noir Glass reciters hero — strict monochrome.
 *
 * Depth recipe: alpha-white card fill + ghost border + bright TOP-ONLY
 * specular. Atmosphere is white-on-black only (ambient glow + engraved
 * rings); zero hue. Search is an engraved inset pill.
 */
@Composable
fun RecitersHero(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    countText: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Hero block: full-width 250dp, large noir radius, horizontal 16dp margins
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(250.dp)
                .clip(GhaisShapes.cardLarge)
                .background(GhaisNoir.cardFill())
                .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.cardLarge)
                .topSpecular(inset = 30.dp)
        ) {
            // Ambient top glow — diffused white light, monochrome only
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .offset(x = (-70).dp, y = (-70).dp)
                    .align(Alignment.TopStart)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                GhaisNoir.AmbientGlow,
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
                                GhaisNoir.AmbientGlow,
                                Color.Transparent
                            )
                        )
                    )
            )

            // Giant engraved Mosque glyph, centered — dimmed white, no hue
            Icon(
                imageVector = Icons.Filled.Mosque,
                contentDescription = null,
                tint = GhaisNoir.TextDisabled,
                modifier = Modifier
                    .size(160.dp)
                    .align(Alignment.Center)
            )

            // Diagonal glass sheen swept across the hero
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(GhaisNoir.sheen())
            )

            // Bottom-up BLACK scrim for readability (monochrome)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
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
                        color = GhaisNoir.TextPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = countText,
                        color = GhaisNoir.TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Engraved search pill below hero (carved-in recessed surface)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
                .clip(GhaisShapes.pill)
                .background(GhaisNoir.insetFill())
                .border(1.dp, GhaisNoir.InsetBorder, GhaisShapes.pill)
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = GhaisNoir.TextTertiary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = GhaisNoir.TextPrimary,
                    fontSize = 15.sp
                ),
                decorationBox = { inner ->
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search reciters...",
                            color = GhaisNoir.TextDisabled,
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
