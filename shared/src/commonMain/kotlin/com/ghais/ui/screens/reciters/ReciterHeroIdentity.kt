package com.ghais.ui.screens.reciters

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.domain.model.Reciter
import com.ghais.ui.theme.GhaisNoir

/**
 * Minimal hero identity — portrait + names only.
 *
 * No eyebrow, no chips, no captions. Tonal and calm.
 */
@Composable
fun ReciterHeroIdentity(
    reciter: Reciter,
    meta: ReciterDisplayMeta,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NoirReciterAvatar(
            photoUrl = meta.photoUrl,
            nameEn = reciter.nameEn,
            size = 96.dp,
            shape = CircleShape,
            monogramSize = 32.sp,
            ring = true,
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = reciter.nameEn,
                color = GhaisNoir.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = reciter.nameAr,
                color = GhaisNoir.TextSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
