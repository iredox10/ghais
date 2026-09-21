package com.ghais.ui.screens.profile.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PillCardTop = Color.White.copy(alpha = 0.08f)
private val PillCardBottom = Color.White.copy(alpha = 0.03f)
private val PillBorder = Color.White.copy(alpha = 0.10f)
private val PillAvatarFill = Color.White.copy(alpha = 0.08f)
private val PillTitle = Color.White
private val PillMuted = Color.White.copy(alpha = 0.55f)
private val PillChevron = Color.White.copy(alpha = 0.60f)

@Composable
fun ProfileIdentityPill(
    displayName: String,
    email: String?,
    isGuest: Boolean,
    onClick: () -> Unit
) {
    val pillShape = RoundedCornerShape(28.dp)
    val subtitle = email?.takeIf { it.isNotBlank() } ?: "Guest mode"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clip(pillShape)
            .background(Brush.verticalGradient(listOf(PillCardTop, PillCardBottom)))
            .border(width = 1.dp, color = PillBorder, shape = pillShape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (displayName.isBlank()) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PillAvatarFill),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = PillTitle,
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PillAvatarFill),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName.first().uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PillTitle
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName.ifBlank { "Guest" },
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = PillTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = PillMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = PillChevron,
            modifier = Modifier.size(20.dp)
        )
    }
}
