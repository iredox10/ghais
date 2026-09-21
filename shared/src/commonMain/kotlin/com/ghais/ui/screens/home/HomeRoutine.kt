package com.ghais.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Work
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
import com.ghais.domain.model.RoutineModeType

private val RoutineCardRadius = 20.dp
private val RoutineCardHeight = 200.dp
private val RoutineArtSize = 88.dp

private val StudyGradientStart = Color(0xFF0E7A5F)
private val StudyGradientEnd = Color(0xFF06231C)
private val WorkGradientStart = Color(0xFF2E7CF6)
private val WorkGradientEnd = Color(0xFF0A1F44)
private val SleepGradientStart = Color(0xFF3B2E7C)
private val SleepGradientEnd = Color(0xFF0B0B18)

private data class RoutineCardSpec(
    val mode: RoutineModeType,
    val label: String,
    val subtitle: String,
    val icon: ImageVector,
    val gradientStart: Color,
    val gradientEnd: Color,
)

private val routineCards = listOf(
    RoutineCardSpec(
        mode = RoutineModeType.STUDY,
        label = "Study",
        subtitle = "Focus",
        icon = Icons.Filled.MenuBook,
        gradientStart = StudyGradientStart,
        gradientEnd = StudyGradientEnd,
    ),
    RoutineCardSpec(
        mode = RoutineModeType.WORK,
        label = "Work",
        subtitle = "Deep work",
        icon = Icons.Filled.Work,
        gradientStart = WorkGradientStart,
        gradientEnd = WorkGradientEnd,
    ),
    RoutineCardSpec(
        mode = RoutineModeType.SLEEP,
        label = "Sleep",
        subtitle = "Rest",
        icon = Icons.Filled.Bedtime,
        gradientStart = SleepGradientStart,
        gradientEnd = SleepGradientEnd,
    ),
)

@Composable
fun HomeRoutineRow(onMode: (RoutineModeType) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        routineCards.forEach { card ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(RoutineCardHeight)
                    .clip(RoundedCornerShape(RoutineCardRadius))
                    .background(
                        Brush.verticalGradient(
                            listOf(card.gradientStart, card.gradientEnd)
                        )
                    )
                    .clickable { onMode(card.mode) }
                    .padding(14.dp)
            ) {
                Icon(
                    imageVector = card.icon,
                    contentDescription = card.label,
                    tint = Color.White,
                    modifier = Modifier
                        .size(RoutineArtSize)
                        .align(Alignment.Center)
                )
                Column(
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(
                        text = card.label,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = card.subtitle,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}
