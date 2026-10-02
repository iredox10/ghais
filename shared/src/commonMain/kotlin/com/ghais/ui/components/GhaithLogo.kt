package com.ghais.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import ghais.shared.generated.resources.Res
import ghais.shared.generated.resources.ghaith_mark
import org.jetbrains.compose.resources.painterResource

/**
 * Ghaith calligraphy mark (white-on-transparent PNG).
 *
 * Calm Noir aesthetic: when [animated] the mark gently "breathes" (scale +
 * alpha). Deliberately no rotation — a spinning calligraphy logo reads as a
 * loader.
 *
 * The 96.dp default size comes first in the modifier chain so a caller may
 * override it with its own `.size(...)`.
 */
@Composable
fun GhaithLogo(
    modifier: Modifier = Modifier,
    animated: Boolean = true,
) {
    if (!animated) {
        Image(
            painter = painterResource(Res.drawable.ghaith_mark),
            contentDescription = "Ghaith",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(96.dp).then(modifier),
        )
        return
    }

    val breathe = rememberInfiniteTransition(label = "GhaithBreathe")
    val breatheSpec = infiniteRepeatable<Float>(
        animation = tween(durationMillis = 2600),
        repeatMode = RepeatMode.Reverse,
    )
    val scale by breathe.animateFloat(
        initialValue = 1f,
        targetValue = 1.045f,
        animationSpec = breatheSpec,
        label = "breatheScale",
    )
    val alpha by breathe.animateFloat(
        initialValue = 0.92f,
        targetValue = 1f,
        animationSpec = breatheSpec,
        label = "breatheAlpha",
    )

    Image(
        painter = painterResource(Res.drawable.ghaith_mark),
        contentDescription = "Ghaith",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .size(96.dp)
            .then(modifier)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
    )
}
