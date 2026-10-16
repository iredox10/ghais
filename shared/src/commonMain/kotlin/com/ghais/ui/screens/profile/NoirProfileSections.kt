package com.ghais.ui.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.ui.components.noir.NoirNavBar
import com.ghais.ui.screens.profile.glass.GlassDivider
import com.ghais.ui.theme.GhaisNoir

/**
 * Phase 3 — Noir Profile sections: editorial header + shared row chrome.
 */

/** Standard nav bar + greeting (reference headline). */
@Composable
fun NoirProfileHeader(
    userName: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        NoirNavBar(title = "Profile")
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (userName.isBlank()) "Assalamu alaikum" else "Assalamu alaikum, $userName",
            color = GhaisNoir.TextSecondary,
            fontSize = 13.sp
        )
    }
}

/** Thin ghost divider for stacked rows inside a card. */
@Composable
fun NoirRowDivider() {
    GlassDivider()
}
