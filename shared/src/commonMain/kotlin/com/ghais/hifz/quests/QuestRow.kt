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
 * Minimal quest row: checkbox state, title, slim progress bar, and a single
 * reward line. Descriptions are gone — the titles ("Review 5 due ayahs")
 * already say everything, and the extra lines were colliding with the
 * counters on narrow screens.
 *
 * Stateless: [quest] is a [Quest] snapshot, [onClick] (when given) is the
 * caller's hook to record progress / navigate. Done rows read lavender.
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
            if (quest.done) GhaisNoir.Lavender.copy(alpha = 0.4f) else GhaisNoir.BorderCard,
            GhaisShapes.row,
        )
    if (onClick != null) rowModifier = rowModifier.noirClickable(onClick)

    Row(
        modifier = rowModifier.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .then(
                    if (quest.done) {
                        Modifier.background(GhaisNoir.lavenderFill())
                    } else {
                        Modifier.background(GhaisNoir.Fill2)
                    },
                )
                .border(
                    1.dp,
                    if (quest.done) {
                        Color.Transparent
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
                    tint = GhaisNoir.LavenderInk,
                    modifier = Modifier.size(14.dp),
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
            Spacer(Modifier.height(6.dp))
            NoirSegmentedProgress(
                progress = if (quest.target > 0) {
                    quest.progress.toFloat() / quest.target.toFloat()
                } else {
                    0f
                },
                trackHeight = 4.dp,
                lavender = quest.done,
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "+${quest.rewardXp}",
                color = if (quest.done) GhaisNoir.Lavender else GhaisNoir.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${quest.progress}/${quest.target}",
                color = GhaisNoir.TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
