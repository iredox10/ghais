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
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.components.noir.NoirSegmentedProgress
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

// Phase 3 — Storage card re-skinned: NoirCard + chrome progress + ghost pill.

@Composable
fun StorageHealthCard(usedLabel: String, summary: String, progress: Float, onClear: () -> Unit) {
    NoirCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconWell(icon = Icons.Filled.DownloadDone, size = 40.dp, iconSize = 20.dp)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = "Offline downloads",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GhaisNoir.TextPrimary
                    )
                    Text(
                        text = summary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = GhaisNoir.TextSecondary
                    )
                }
                Text(
                    text = usedLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = GhaisNoir.TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            NoirSegmentedProgress(progress = progress)

            Spacer(modifier = Modifier.height(12.dp))

            // Ghost pill (monochrome: muted white, no red).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GhaisNoir.Fill2, GhaisShapes.pill)
                    .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
                    .noirClickable(onClick = onClear)
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Clear cache",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GhaisNoir.TextSecondary
                )
            }
        }
    }
}
