package com.ghais.ui.screens.profile.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.repository.QuranDataRepository
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirInsetField
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.screens.reciters.NoirReciterAvatar
import com.ghais.ui.screens.reciters.photoForSlug
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * Schedule-editor reciter picker: searchable Noir modal bottom sheet.
 *
 * Replaces the chip LazyRow with a [ModalBottomSheet] in Noir chrome
 * (CanvasTop + scrim, no drag handle — SleepTimerSheet/QueueSheet recipe):
 * IconWell header + ghost close disc, engraved inset search filtering
 * nameEn/nameAr (case-insensitive), LazyColumn rows with a 48dp
 * [NoirReciterAvatar], nameEn primary + nameAr/riwayah secondary text, and a
 * chrome check disc when selected. Tap selects and dismisses; an empty-result
 * ghost well covers the no-match state. Zero hue, Noir tokens only.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReciterPickerSheet(
    visible: Boolean,
    selectedSlug: String,
    onSelect: (slug: String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val reciters = remember { QuranDataRepository.getReciters() }
    var query by remember { mutableStateOf("") }

    val filtered = remember(query, reciters) {
        val q = query.trim()
        if (q.isBlank()) reciters
        else reciters.filter {
            it.nameEn.contains(q, ignoreCase = true) ||
                it.nameAr.contains(q, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GhaisNoir.CanvasTop,
        scrimColor = GhaisNoir.Scrim,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Sheet chrome: IconWell header + ghost close disc.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconWell(
                        icon = Icons.Default.GraphicEq,
                        size = 40.dp,
                        iconSize = 20.dp,
                        contentDescription = null
                    )
                    Column {
                        Text(
                            text = "Choose reciter",
                            color = GhaisNoir.TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${filtered.size} of ${reciters.size} reciters",
                            color = GhaisNoir.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GhaisNoir.Fill2)
                        .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                        .noirClickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = GhaisNoir.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Search — engraved inset field, monochrome.
            NoirInsetField {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = GhaisNoir.TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = GhaisNoir.TextPrimary,
                            fontSize = 15.sp
                        ),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search reciters…",
                                    color = GhaisNoir.TextDisabled,
                                    fontSize = 15.sp
                                )
                            }
                            inner()
                        }
                    )
                    if (query.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(GhaisNoir.Fill4)
                                .noirClickable { query = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = GhaisNoir.TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                // Empty state — ghost well, monochrome.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconWell(
                            icon = Icons.Default.Search,
                            size = 56.dp,
                            iconSize = 28.dp,
                            contentDescription = "No results",
                            tint = GhaisNoir.TextTertiary
                        )
                        Text(
                            text = "No reciters found",
                            color = GhaisNoir.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Try a different name",
                            color = GhaisNoir.TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filtered,
                        key = { it.slug }
                    ) { reciter ->
                        val isSelected = reciter.slug.equals(selectedSlug, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(GhaisShapes.row)
                                .background(
                                    if (isSelected) GhaisNoir.cardFillActive()
                                    else GhaisNoir.cardFillSoft()
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) GhaisNoir.SpecularTop else GhaisNoir.BorderCard,
                                    GhaisShapes.row
                                )
                                .topSpecular(inset = 22.dp)
                                .noirClickable {
                                    onSelect(reciter.slug)
                                    onDismiss()
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NoirReciterAvatar(
                                photoUrl = photoForSlug(reciter.slug),
                                nameEn = reciter.nameEn,
                                size = 48.dp,
                                shape = RoundedCornerShape(14.dp),
                                monogramSize = 18.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = reciter.nameEn,
                                    color = GhaisNoir.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = buildSecondaryLabel(reciter.nameAr, reciter.riwayah),
                                    color = GhaisNoir.TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                ReciterPickerCheckDisc()
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun buildSecondaryLabel(nameAr: String, riwayah: String): String {
    val ar = nameAr.trim()
    val riw = riwayah.trim()
    return when {
        ar.isNotEmpty() && riw.isNotEmpty() -> "$ar • $riw"
        ar.isNotEmpty() -> ar
        riw.isNotEmpty() -> riw
        else -> ""
    }
}

@Composable
private fun ReciterPickerCheckDisc(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(24.dp)
            .background(GhaisNoir.chromeFill(), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Selected",
            tint = GhaisNoir.OnChrome,
            modifier = Modifier.size(14.dp)
        )
    }
}
