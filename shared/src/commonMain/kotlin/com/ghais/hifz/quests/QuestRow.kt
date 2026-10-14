package com.ghais.hifz.quests

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.ui.components.noir.NoirSegmentedProgress
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * AGENT A9 — Noir quest row: chrome checkbox state, engraved progress bar,
 * reward label (`+20 XP`) and a `progress/target` counter.
 *
 * Stateless: [quest] is a [Quest] snapshot, [onClick] (when given) is the
 * caller's hook to record progress / navigate. Done rows render the active
 * wash with a specular rim; open rows stay on the soft resting fill.
 */
@Composable
fun QuestRow(
    quest: Quest,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    var rowModifier = modifier
        .background(
            if (quest.done) GhaisNoir.cardFillActive() else GhaisNoir.cardFillSoft(),
            GhaisShapes.row,
        )
        .border(
            1.dp,
            if (quest.done) GhaisNoir.SpecularTop else GhaisNoir.BorderCard,
            GhaisShapes.row,
        )
    if (onClick != null) rowModifier = rowModifier.noirClickable(onClick)

    Row(
        modifier = rowModifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .then(
                    if (quest.done) {
                        Modifier.background(GhaisNoir.chromeFill())
                    } else {
                        Modifier.background(GhaisNoir.Fill2)
                    },
                )
                .border(
                    1.dp,
                    if (quest.done) {
                        Color.White.copy(alpha = 0.4f)
                    } else {
                        GhaisNoir.BorderCard
                    },
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (quest.done) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Quest done",
                    tint = GhaisNoir.OnChrome,
                    modifier = Modifier.size(15.dp),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = quest.title,
                color = GhaisNoir.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (quest.description.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = quest.description,
                    color = GhaisNoir.TextTertiary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(8.dp))
            NoirSegmentedProgress(
                progress = if (quest.target > 0) {
                    quest.progress.toFloat() / quest.target.toFloat()
                } else {
                    0f
                },
                trackHeight = 4.dp,
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "+${quest.rewardXp} XP",
                color = GhaisNoir.TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${quest.progress}/${quest.target}",
                color = GhaisNoir.TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
