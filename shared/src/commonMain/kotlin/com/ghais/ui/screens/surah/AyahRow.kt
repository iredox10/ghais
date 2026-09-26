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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.repository.MasteryStatus
import com.ghais.domain.model.Ayah
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography
import androidx.compose.ui.text.buildAnnotatedString
import com.ghais.ui.util.appendGluedRosette
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
 *
 * No per-row Basmalah here: the single header lives in SurahDetailScreen
 * (this row's only caller), so verse 1 never double-renders it.
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
    isActive: Boolean = isPlaying,
    masteryStatus: MasteryStatus? = null,
    onMasteryClick: (() -> Unit)? = null
) {
    // Explicit RTL for the whole row: the verse text, the weight(1f) column's
    // alignment and the SpaceBetween action row below it all resolve their
    // direction from here, so this composable is correct on its own rather
    // than by inheritance from the caller's layout direction.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
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
                // Engraved ayah-number well + Hifz mastery status ring
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AyahBadge(number = ayah.ayahNo, active = isActive)
                    if (masteryStatus != null && onMasteryClick != null) {
                        MasteryStatusRing(
                            status = masteryStatus,
                            onClick = onMasteryClick
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                append(ayah.textUthmani)
                                appendGluedRosette(ayah.ayahNo)
                            },
                            color = GhaisNoir.TextPrimary,
                            fontSize = 22.sp,
                            lineHeight = 44.sp,
                            textAlign = TextAlign.Right,
                            fontWeight = FontWeight.Normal,
                            style = GhaisTypography.quranScript
                        )
                    }

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

/**
 * Three-tier mastery status ring:
 * - NEW: subtle hairline outline circle
 * - REVIEW_NEEDED: dashed outline circle with soft core
 * - MASTERED: solid white disc with razor-thin ink-black checkmark
 *
 * The ring is a click target, so it carries its own "Hifz status: …"
 * contentDescription and clears the Canvas semantics — TalkBack otherwise
 * announces a bare "button" mid-verse.
 */
@Composable
fun MasteryStatusRing(
    status: MasteryStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .semantics {
                contentDescription = when (status) {
                    MasteryStatus.NEW -> "Hifz status: new"
                    MasteryStatus.REVIEW_NEEDED -> "Hifz status: needs review"
                    MasteryStatus.MASTERED -> "Hifz status: memorized"
                }
            }
            .noirClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(16.dp)
                .clearAndSetSemantics { }
        ) {
            val strokeWidth = 1.6.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2f
            when (status) {
                MasteryStatus.NEW -> {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.35f),
                        radius = radius,
                        style = Stroke(width = strokeWidth)
                    )
                }
                MasteryStatus.REVIEW_NEEDED -> {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.25f),
                        radius = radius * 0.55f
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = radius,
                        style = Stroke(
                            width = strokeWidth,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)
                        )
                    )
                }
                MasteryStatus.MASTERED -> {
                    drawCircle(
                        color = Color.White,
                        radius = radius
                    )
                    val checkPath = Path().apply {
                        moveTo(size.width * 0.28f, size.height * 0.50f)
                        lineTo(size.width * 0.44f, size.height * 0.68f)
                        lineTo(size.width * 0.72f, size.height * 0.34f)
                    }
                    drawPath(
                        path = checkPath,
                        color = Color.Black,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}

