package com.ghais.ui.screens.reciters.deck

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.ui.components.noir.NoirHeroCard
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisTypography

/**
 * Entry card for the "Today's voices" swipe deck (Agent A4 integration).
 * Sits between search and the nation reels on the Reciters tab.
 */
@Composable
fun DeckEntryCard(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    NoirHeroCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onOpen
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = GhaisNoir.TextPrimary
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Today's voices",
                    style = GhaisTypography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    ),
                    color = GhaisNoir.TextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Swipe 10 new voices in a minute",
                    style = GhaisTypography.bodyMedium,
                    color = GhaisNoir.TextSecondary
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = GhaisNoir.TextSecondary
            )
        }
    }
}
