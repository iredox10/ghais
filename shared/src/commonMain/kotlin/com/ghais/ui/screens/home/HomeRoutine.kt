package com.ghais.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.domain.model.RoutineModeType
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

private val RoutineTileHeight = 156.dp

/**
 * Phase 4 — "Listen by routine".
 *
 * The emerald / royal-blue / violet gradient tiles are gone. Study, Work and
 * Sleep are now three identical glass plates whose identity comes from a
 * claymorphic icon well, so the row reads as one monochrome system.
 */
private data class RoutineCardSpec(
    val mode: RoutineModeType,
    val label: String,
    val subtitle: String,
    val icon: ImageVector
)

private val routineCards = listOf(
    RoutineCardSpec(
        mode = RoutineModeType.STUDY,
        label = "Study",
        subtitle = "Focus",
        icon = Icons.AutoMirrored.Filled.MenuBook
    ),
    RoutineCardSpec(
        mode = RoutineModeType.WORK,
        label = "Work",
        subtitle = "Deep work",
        icon = Icons.Filled.Work
    ),
    RoutineCardSpec(
        mode = RoutineModeType.SLEEP,
        label = "Sleep",
        subtitle = "Rest",
        icon = Icons.Filled.Bedtime
    )
)

@Composable
fun HomeRoutineRow(onMode: (RoutineModeType) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        routineCards.forEach { card ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(RoutineTileHeight)
                    .background(GhaisNoir.cardFill(), GhaisShapes.cardNoir)
                    .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.cardNoir)
                    .topSpecular(inset = 24.dp)
                    .noirClickable { onMode(card.mode) }
                    .padding(14.dp)
            ) {
                IconWell(
                    icon = card.icon,
                    size = 48.dp,
                    iconSize = 22.dp,
                    contentDescription = card.label,
                    modifier = Modifier.align(Alignment.TopStart)
                )
                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                    Text(
                        text = card.label,
                        color = GhaisNoir.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = card.subtitle,
                        color = GhaisNoir.TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
