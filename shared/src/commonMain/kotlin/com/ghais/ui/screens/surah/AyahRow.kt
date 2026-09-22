package com.ghais.ui.screens.surah

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.domain.model.Ayah
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Strict Noir Glass ayah row — the [com.ghais.ui.components.noir.NoirListRow] recipe
 * adapted for scripture: soft glass fill (active = stronger wash) + 1px card
 * border + top-only specular, number in an engraved star well, action
 * affordances as chrome (live) / ghost wells. Zero hue.
 *
 * Arabic stays at 100% white in all states — never dimmed.
 * Signature + playback/favorite/share callbacks are unchanged.
 */
@Composable
fun AyahRow(
    ayah: Ayah,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onShareClick: () -> Unit,
    /** True when this ayah is the current track (highlight stays even while paused). Defaults to [isPlaying] for backward compat. */
    isActive: Boolean = isPlaying
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
    ) {
        val fill = if (isActive) GhaisNoir.cardFillActive() else GhaisNoir.cardFillSoft()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(GhaisShapes.row)
                .background(fill, GhaisShapes.row)
                .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.row)
                .topSpecular(inset = 22.dp)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Engraved ayah-number well (monochrome star, number at 100%).
            AyahBadge(number = ayah.ayahNo, active = isActive)

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = ayah.textUthmani,
                    color = GhaisNoir.TextPrimary,
                    fontSize = 22.sp,
                    lineHeight = 40.sp,
                    textAlign = TextAlign.Right,
                    fontWeight = FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ghost affordances — state reads through fill + glyph opacity.
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GhostWell(
                            icon = Icons.Filled.Share,
                            contentDescription = "Share Ayah",
                            onClick = onShareClick
                        )
                        GhostWell(
                            icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite Ayah",
                            tint = if (isFavorite) GhaisNoir.TextPrimary else GhaisNoir.TextSecondary,
                            onClick = onFavoriteClick
                        )
                    }

                    // Play disc: chromium while live, clay well otherwise.
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(GhaisShapes.well)
                            .background(
                                if (isActive) GhaisNoir.chromeFill() else GhaisNoir.wellFill(),
                                GhaisShapes.well
                            )
                            .border(
                                1.dp,
                                if (isActive) Color.White.copy(alpha = 0.4f) else GhaisNoir.BorderCard,
                                GhaisShapes.well
                            )
                            .noirClickable(onClick = onPlayClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause Ayah" else "Play Ayah",
                            tint = if (isActive) GhaisNoir.OnChrome else GhaisNoir.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Ghost circular affordance — Fill2 wash + hairline rim, monochrome glyph. */
@Composable
private fun GhostWell(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = GhaisNoir.TextSecondary
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(GhaisShapes.well)
            .background(GhaisNoir.Fill2, GhaisShapes.well)
            .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.well)
            .noirClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * Engraved ayah-number well: 8-point star outline in the monochrome ramp,
 * number at 100% white in all states.
 */
@Composable
fun AyahBadge(number: Int, active: Boolean = false) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(40.dp)
    ) {
        val strokeColor = if (active) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val c = center
            val path = Path()

            val points = 8
            for (i in 0 until points * 2) {
                val currentRadius = if (i % 2 == 0) radius else radius * 0.8f
                val angle = (i * (PI / points) - PI / 2).toDouble()

                val x = (c.x + currentRadius * cos(angle)).toFloat()
                val y = (c.y + currentRadius * sin(angle)).toFloat()

                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()

            drawPath(
                path = path,
                color = strokeColor,
                style = Stroke(width = 2.dp.toPx())
            )
        }
        Text(
            text = number.toString(),
            color = GhaisNoir.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
