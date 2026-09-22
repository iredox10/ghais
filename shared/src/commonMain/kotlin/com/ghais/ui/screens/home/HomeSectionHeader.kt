package com.ghais.ui.screens.home

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ghais.ui.components.noir.NoirSectionHeader

/**
 * Phase 4 — Home section label.
 *
 * Delegates to the shared NoirSectionHeader (bright 15sp label + ghost action)
 * so Home, Profile and the upcoming screens share one label rhythm. The old
 * 26sp ExtraBold title + blue "See all" link (#4C8DFF) are gone.
 */
@Composable
fun HomeSectionHeader(
    title: String,
    onSeeAll: (() -> Unit)?
) {
    NoirSectionHeader(
        label = title,
        modifier = Modifier.padding(top = 8.dp),
        actionLabel = if (onSeeAll != null) "See all" else null,
        onAction = onSeeAll
    )
}
