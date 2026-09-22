package com.ghais.ui.screens.profile.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * Phase 3 — Noir identity pill: avatar clay-well + name/email + Edit chrome chip.
 * Tap anywhere to edit; the chip is the affordance (reference "Enable" pill).
 */
@Composable
fun ProfileIdentityPill(
    displayName: String,
    email: String?,
    isGuest: Boolean,
    onClick: () -> Unit
) {
    val subtitle = email?.takeIf { it.isNotBlank() } ?: "Guest mode"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GhaisNoir.cardFill(), GhaisShapes.row)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.row)
            .topSpecular(inset = 22.dp)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (displayName.isBlank()) {
            IconWell(icon = Icons.Default.Person, size = 52.dp, iconSize = 26.dp)
        } else {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(GhaisNoir.wellFill(), GhaisShapes.well)
                    .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.well),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName.first().uppercase(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = GhaisNoir.TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName.ifBlank { "Guest" },
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = GhaisNoir.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = GhaisNoir.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Chrome "Edit" affordance chip (reference Enable pill, compact).
        Box(
            modifier = Modifier
                .background(GhaisNoir.chromeFill(), GhaisShapes.pill)
                .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
                .noirClickable(onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Edit",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = GhaisNoir.OnChrome
            )
        }
    }
}

