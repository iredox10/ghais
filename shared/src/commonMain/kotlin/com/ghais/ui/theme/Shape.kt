package com.ghais.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object GhaisShapes {
    val small = RoundedCornerShape(8.dp)
    val medium = RoundedCornerShape(12.dp)
    val large = RoundedCornerShape(16.dp)
    val extraLarge = RoundedCornerShape(24.dp)
    
    val card = RoundedCornerShape(16.dp)
    val button = RoundedCornerShape(24.dp)
    val bottomSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val playerBar = RoundedCornerShape(20.dp)
    val navDock = RoundedCornerShape(28.dp)

    val materialShapes = Shapes(
        small = small,
        medium = medium,
        large = large,
        extraLarge = extraLarge
    )
}
