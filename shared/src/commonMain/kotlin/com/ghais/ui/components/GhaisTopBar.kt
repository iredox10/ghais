package com.ghais.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ghais.ui.theme.GhaisColors
import com.ghais.ui.theme.GhaisSpacing
import com.ghais.ui.theme.GhaisTypography

@Composable
fun GhaisTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    backgroundColor: Color = Color.Transparent
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(
                horizontal = GhaisSpacing.md,
                vertical = GhaisSpacing.sm
            )
    ) {
        if (onBackClick != null) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = GhaisColors.TextPrimary
                )
            }
        }
        
        Text(
            text = title,
            style = GhaisTypography.titleMedium,
            color = GhaisColors.TextPrimary,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
