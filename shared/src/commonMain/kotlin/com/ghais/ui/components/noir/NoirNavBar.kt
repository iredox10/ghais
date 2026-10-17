package com.ghais.ui.components.noir

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.ui.theme.GhaisNoir

/**
 * Standard navigation bar (iOS-stock pattern, Noir dressed): optional
 * leading slot, centered 17sp Bold title, trailing action slot. Empty
 * sides collapse to balance spacers so the title stays truly centered.
 *
 * Tabs pass no [onBack] (a leading spacer balances instead); pushed
 * screens pass [onBack] for the back well. Extra leading content (e.g.
 * an avatar) goes in [navigation]; trailing wells go in [actions].
 *
 * Two title layouts, selected by [titleAlignLeft]:
 *
 * - FALSE (default, unchanged): `[leading slot] [title centered in a
 *   weight(1f) box] [trailing slot]`, with 40dp balance spacers where a
 *   slot is empty so the title stays truly centered.
 *
 * - TRUE (reference header layout, mirrors the reciter-profile
 *   collapsing bar's title): `[leading slot] → 12dp gap → title pinned
 *   to the leading edge (17sp Bold, TextPrimary, maxLines 1, ellipsis,
 *   weight(1f)) → trailing wells right-aligned. No trailing balance
 *   spacer is emitted, so a long title may use the full remaining width
 *   before ellipsising. With no [onBack]/[navigation] the title starts
 *   after the standard 40dp leading spacer, so the row never has an
 *   unbalanced flush edge.
 *
 * [titleAlignLeft] was added last in the signature (after [actions])
 * so existing call sites — positional or named — compile and render
 * byte-for-byte identically when they don't opt in.
 */
@Composable
fun NoirNavBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backDescription: String = "Back",
    backIcon: ImageVector? = null,
    navigation: (@Composable RowScope.() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    titleAlignLeft: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            navigation != null -> navigation()
            onBack != null && backIcon != null -> NoirNavButton(
                icon = backIcon,
                contentDescription = backDescription,
                onClick = onBack
            )
            else -> Spacer(Modifier.width(40.dp))
        }
        if (titleAlignLeft) {
            // Reference layout: the title hangs directly off the leading
            // well, so a filled leading slot needs a fixed 12dp gap (the
            // empty-slot case already ends in the standard 40dp spacer).
            // fontSize/weight/color intentionally duplicate the centered
            // branch below — they must match it and the reciter-profile
            // collapsing bar exactly.
            if (navigation != null || (onBack != null && backIcon != null)) {
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text = title,
                color = GhaisNoir.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        } else {
            Text(
                text = title,
                color = GhaisNoir.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        if (actions != null) {
            actions()
        } else if (!titleAlignLeft) {
            // Only the centered layout needs a trailing balance spacer;
            // a left-aligned title in a weight(1f) box must be free to
            // reach the trailing edge before ellipsising.
            Spacer(Modifier.width(40.dp))
        }
    }
}

/** 40dp circular nav well: back, search, share, avatar-adjacent actions. */
@Composable
fun NoirNavButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(GhaisNoir.wellFill(), CircleShape)
            .border(1.dp, GhaisNoir.BorderCard, CircleShape)
            .noirClickable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = GhaisNoir.TextPrimary,
            modifier = Modifier.size(19.dp)
        )
    }
}
