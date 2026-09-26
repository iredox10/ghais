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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.repository.QuranDataRepository
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.util.bidiIsolate

/**
 * Noir surah picker sheet for the schedule editor ("From surah" / "To surah").
 *
 * Searchable modal replacing +/- steppers: engraved inset search matches id
 * digits, [nameEn][com.ghais.domain.model.Surah.nameEn] and
 * [nameAr][com.ghais.domain.model.Surah.nameAr]; rows use the NoirListRow
 * language (soft card fill + ghost border + top-only specular) with an
 * engraved number well and a chrome check when selected. Tap selects and
 * dismisses.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahPickerSheet(
    visible: Boolean,
    title: String,
    selectedId: Int,
    onSelect: (id: Int) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val surahs = remember { QuranDataRepository.getSurahs() }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(visible) {
        if (visible) query = ""
    }

    val trimmed = query.trim()
    val lowered = trimmed.lowercase()
    val filtered = if (trimmed.isEmpty()) {
        surahs
    } else {
        surahs.filter { surah ->
            surah.id.toString().contains(trimmed) ||
                surah.nameEn.lowercase().contains(lowered) ||
                surah.nameAr.contains(trimmed)
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
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Sheet chrome: IconWell header + ghost close well.
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
                        icon = Icons.AutoMirrored.Filled.List,
                        size = 40.dp,
                        iconSize = 20.dp,
                        contentDescription = null
                    )
                    Column {
                        Text(
                            text = title,
                            color = GhaisNoir.TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${filtered.size} of ${surahs.size} surahs",
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
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Close",
                        tint = GhaisNoir.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Engraved inset search: matches id digits, nameEn, nameAr.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(GhaisShapes.row)
                    .background(GhaisNoir.insetFill())
                    .border(1.dp, GhaisNoir.InsetBorder, GhaisShapes.row)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
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
                    cursorBrush = SolidColor(GhaisNoir.TextPrimary),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                text = "Search number or name…",
                                color = GhaisNoir.TextDisabled,
                                fontSize = 15.sp,
                                maxLines = 1
                            )
                        }
                        inner()
                    },
                    modifier = Modifier.weight(1f)
                )
                if (query.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .noirClickable { query = "" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = GhaisNoir.TextTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (filtered.isEmpty()) {
                // Empty state ghost well.
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
                            icon = Icons.Filled.SearchOff,
                            size = 56.dp,
                            iconSize = 28.dp,
                            contentDescription = "No results",
                            tint = GhaisNoir.TextTertiary
                        )
                        Text(
                            text = "No surah found",
                            color = GhaisNoir.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Try a number or a different name",
                            color = GhaisNoir.TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filtered,
                        key = { it.id }
                    ) { surah ->
                        val isSelected = surah.id == selectedId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(GhaisShapes.row)
                                .background(
                                    if (isSelected) GhaisNoir.cardFillActive()
                                    else GhaisNoir.cardFillSoft(),
                                    GhaisShapes.row
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) GhaisNoir.SpecularTop else GhaisNoir.BorderCard,
                                    GhaisShapes.row
                                )
                                .topSpecular(inset = 22.dp)
                                .noirClickable {
                                    onSelect(surah.id)
                                    onDismiss()
                                }
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Engraved number well.
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(GhaisNoir.wellFill())
                                    .border(
                                        1.dp,
                                        if (isSelected) GhaisNoir.SpecularTop else GhaisNoir.BorderCard,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${surah.id}",
                                    color = if (isSelected) GhaisNoir.TextPrimary else GhaisNoir.TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = surah.nameEn,
                                    color = GhaisNoir.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${bidiIsolate(surah.nameAr)} • ${surah.ayahsCount} ayahs",
                                    color = GhaisNoir.TextSecondary,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            // Chrome check when selected.
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(GhaisNoir.chromeFill())
                                        .border(1.dp, GhaisNoir.SpecularTop, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = GhaisNoir.OnChrome,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
