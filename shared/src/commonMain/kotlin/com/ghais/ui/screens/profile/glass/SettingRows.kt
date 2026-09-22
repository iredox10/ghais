package com.ghais.ui.screens.profile.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirSwitch
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

// Phase 3 — legacy row names re-skinned to Noir (signatures kept).

@Composable
fun ProtonRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    countBadge: Int = 0,
    destructive: Boolean = false,
    onClick: () -> Unit = {}
) {
    // Monochrome danger: muted 38% white (no red in Noir).
    val titleColor = if (destructive) GhaisNoir.TextTertiary else GhaisNoir.TextPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noirClickable(onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            IconWell(icon = icon, size = 40.dp, iconSize = 20.dp, tint = titleColor)
            if (countBadge > 0) {
                NoirBadgeDot(countBadge, modifier = Modifier.align(Alignment.TopEnd))
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = titleColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = GhaisNoir.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        NoirChevronRing()
    }
}

@Composable
fun ProtonSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noirClickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconWell(icon = icon, size = 40.dp, iconSize = 20.dp)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = GhaisNoir.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = GhaisNoir.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        NoirSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun ProtonValueRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconWell(icon = icon, size = 40.dp, iconSize = 20.dp)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = GhaisNoir.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = GhaisNoir.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (value.isNotEmpty()) {
            Spacer(modifier = Modifier.width(12.dp))
            NoirValueChip(value)
        }
    }
}

/** Chrome badge dot (shared by rows). */
@Composable
fun NoirBadgeDot(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .offset(x = 4.dp, y = (-4).dp)
            .size(18.dp)
            .background(GhaisNoir.chromeFill(), CircleShape)
            .border(1.dp, GhaisNoir.NoirBlack, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = GhaisNoir.OnChrome,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/** Circular ghost chevron affordance. */
@Composable
fun NoirChevronRing(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(28.dp)
            .border(1.dp, GhaisNoir.BorderGhost, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = GhaisNoir.TextTertiary,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** Ghost value chip (version strings, "Verified"). */
@Composable
fun NoirValueChip(value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(GhaisNoir.Fill2, GhaisShapes.pill)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            color = GhaisNoir.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
