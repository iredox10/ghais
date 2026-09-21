package com.ghais.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.ghais.ui.theme.GhaisColors
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisSpacing

@Composable
fun GhaisCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = GhaisColors.Card,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    var cardModifier = modifier
        .clip(GhaisShapes.card)
        .background(backgroundColor)
        
    if (onClick != null) {
        cardModifier = cardModifier.clickable(onClick = onClick)
    }
    
    Box(
        modifier = cardModifier.padding(GhaisSpacing.lg),
        content = content
    )
}
