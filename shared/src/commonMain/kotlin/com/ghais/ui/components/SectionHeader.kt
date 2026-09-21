package com.ghais.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ghais.ui.theme.GhaisColors
import com.ghais.ui.theme.GhaisSpacing
import com.ghais.ui.theme.GhaisTypography

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    onSeeAll: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = GhaisSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = GhaisTypography.titleLarge,
            color = GhaisColors.Primary
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        if (onSeeAll != null) {
            Text(
                text = "See All",
                style = GhaisTypography.labelLarge,
                color = GhaisColors.Secondary,
                modifier = Modifier
                    .clickable(onClick = onSeeAll)
                    .padding(GhaisSpacing.xs)
            )
        }
    }
}
