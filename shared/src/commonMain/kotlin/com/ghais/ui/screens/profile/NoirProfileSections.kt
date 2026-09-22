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
import com.ghais.ui.components.noir.GhostPillButton
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.screens.profile.glass.GlassDivider
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisTypography

/**
 * Phase 3 — Noir Profile sections: editorial header + shared row chrome.
 */

/** Editorial header: "Your • Space" + sync ghost pill (reference headline). */
@Composable
fun NoirProfileHeader(
    userName: String,
    syncText: String,
    onSync: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Your",
            style = GhaisTypography.displayEditorial,
            maxLines = 1
        )
        Text(
            text = "Space",
            style = GhaisTypography.displayEditorialBold,
            maxLines = 1
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (userName.isBlank()) "Assalamu alaikum" else "Assalamu alaikum, $userName",
            color = GhaisNoir.TextSecondary,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(12.dp))
        GhostPillButton(text = syncText, onClick = onSync)
    }
}

/** Thin ghost divider for stacked rows inside a card. */
@Composable
fun NoirRowDivider() {
    GlassDivider()
}
