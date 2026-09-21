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
import com.ghais.ui.theme.QuranifyColors
import com.ghais.ui.theme.QuranifySpacing
import com.ghais.ui.theme.QuranifyTypography

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    onSeeAll: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = QuranifySpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = QuranifyTypography.titleLarge,
            color = QuranifyColors.Primary
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        if (onSeeAll != null) {
            Text(
                text = "See All",
                style = QuranifyTypography.labelLarge,
                color = QuranifyColors.Secondary,
                modifier = Modifier
                    .clickable(onClick = onSeeAll)
                    .padding(QuranifySpacing.xs)
            )
        }
    }
}
