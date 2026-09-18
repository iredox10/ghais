package com.quranify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.quranify.ui.theme.QuranifyColors
import com.quranify.ui.theme.QuranifyShapes
import com.quranify.ui.theme.QuranifySpacing

@Composable
fun QuranifyCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = QuranifyColors.Card,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    var cardModifier = modifier
        .clip(QuranifyShapes.card)
        .background(backgroundColor)
        
    if (onClick != null) {
        cardModifier = cardModifier.clickable(onClick = onClick)
    }
    
    Box(
        modifier = cardModifier.padding(QuranifySpacing.lg),
        content = content
    )
}
