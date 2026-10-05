package com.ghais.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.repository.TranslationCatalog
import com.ghais.data.repository.TranslationCatalogItem
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography
import com.ghais.ui.util.bidiIsolate

/**
 * Noir translation picker sheet for the hifz player.
 *
 * Sheet mechanism copied verbatim from [QueueSheet] / [SurahPickerSheet]:
 * `ModalBottomSheet` (CanvasTop container + Scrim, `dragHandle = null`),
 * IconWell header + ghost close disc, engraved inset search field, and the
 * NoirListRow recipe for rows (soft card fill + ghost border + top-only
 * specular; the selected row reads through fill elevation, never hue).
 *
 * The catalog is grouped by country via [TranslationCatalog.groupedByCountry]
 * (browsing) or [TranslationCatalog.search] re-grouped (query non-blank), so
 * headers and rows scroll together in one lazy list. Tapping a row only calls
 * [onSelect] — dismissal is the caller's decision.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationPickerSheet(
    visible: Boolean,
    selectedEditionId: String,
    onSelect: (TranslationCatalogItem) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val totalTranslations = remember { TranslationCatalog.all().size }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(visible) {
        if (visible) query = ""
    }

    // Filtered + grouped map: blank query is plain browse, otherwise search
    // results regrouped with the same sort as groupedByCountry().
    val grouped: Map<String, List<TranslationCatalogItem>> = remember(query) {
        if (query.isBlank()) {
            TranslationCatalog.groupedByCountry()
        } else {
            TranslationCatalog.search(query)
                .groupBy { it.countryName }
                .mapValues { (_, entries) ->
                    entries.sortedWith(compareBy({ it.languageName }, { it.englishName }))
                }
                .toSortedMap()
        }
    }
    val groups = grouped.entries.toList()
    val visibleCount = grouped.values.sumOf { it.size }

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
                .imePadding()
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
                        icon = Icons.Default.Translate,
                        size = 40.dp,
                        iconSize = 20.dp,
                        contentDescription = null
                    )
                    Column {
                        Text(
                            text = "Translation",
                            color = GhaisNoir.TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (query.isBlank()) "$totalTranslations translations"
                            else "$visibleCount of $totalTranslations translations",
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

            // Engraved inset search: placeholder + clear disc only when typed.
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
                    cursorBrush = SolidColor(GhaisNoir.TextPrimary),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                text = "Search translations…",
                                color = GhaisNoir.TextDisabled,
                                fontSize = 15.sp,
                                maxLines = 1
                            )
                        }
                        inner()
                    },
                    modifier = Modifier.weight(1f)
                )
                if (query.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
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

            if (visibleCount == 0) {
                // Empty state — matches the QueueSheet "no match" ghost well.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (query.isBlank()) "No translations available"
                        else "No translations match \"${query.trim()}\"",
                        color = GhaisNoir.TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // One lazy list carries country headers AND rows so the whole
                // sheet scrolls together.
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groups.forEachIndexed { groupIndex, (countryName, entries) ->
                        item(
                            key = "country_$countryName",
                            contentType = "countryHeader"
                        ) {
                            CountrySectionHeader(
                                flag = entries.firstOrNull()?.countryFlag.orEmpty(),
                                country = countryName,
                                isFirst = groupIndex == 0
                            )
                        }
                        items(
                            items = entries,
                            key = { "edition_${it.id}" },
                            contentType = { "edition" }
                        ) { edition ->
                            TranslationRow(
                                edition = edition,
                                isSelected = edition.id == selectedEditionId,
                                onClick = { onSelect(edition) }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Country section header: `🇹🇷 Turkey` — subtle label treatment
 * (GhaisNoir.TextSecondary, ~12.5sp small-caps tracking) with extra top
 * padding on the very first group.
 */
@Composable
private fun CountrySectionHeader(
    flag: String,
    country: String,
    isFirst: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (isFirst) 12.dp else 8.dp, start = 4.dp, end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (flag.isNotBlank()) {
            Text(text = flag, fontSize = 13.sp)
        }
        Text(
            text = country,
            style = GhaisTypography.labelMedium,
            color = GhaisNoir.TextSecondary,
            fontSize = 12.5.sp,
            letterSpacing = 0.6.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Translation row in the NoirListRow language: soft fill (+ active wash when
 * selected), ghost border (specular-bright when selected), top specular.
 * Trailing chrome check disc only for the selected edition.
 */
@Composable
private fun TranslationRow(
    edition: TranslationCatalogItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
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
            .noirClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = edition.englishName.trim().ifBlank { edition.id },
                color = GhaisNoir.TextPrimary,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = editionSecondaryLine(edition),
                color = GhaisNoir.TextSecondary,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isSelected) {
            Spacer(modifier = Modifier.width(10.dp))
            SelectedCheckDisc()
        }
    }
}

/** `Native name • Language` — native dropped when it just repeats englishName. */
private fun editionSecondaryLine(edition: TranslationCatalogItem): String {
    val native = edition.nativeName.trim()
    val english = edition.englishName.trim()
    return if (native.isNotEmpty() && !native.equals(english, ignoreCase = true)) {
        val head = if (edition.rtl) bidiIsolate(native) else native
        "$head • ${edition.languageName}"
    } else {
        edition.languageName
    }
}

/** App-standard selection check: chrome disc + near-black glyph (see SleepTimerSheet). */
@Composable
private fun SelectedCheckDisc(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(GhaisNoir.chromeFill())
            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Selected",
            tint = GhaisNoir.OnChrome,
            modifier = Modifier.size(15.dp)
        )
    }
}
