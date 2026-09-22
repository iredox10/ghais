package com.ghais.ui.screens.explore.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghais.data.seed.GhaisAssets
import org.jetbrains.compose.resources.painterResource
import ghais.shared.generated.resources.Res
import ghais.shared.generated.resources.ghais_mark
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography

/**
 * Noir Glass Explore header — strict monochrome.
 *
 * - Zero hue: all legacy emerald accents replaced with the
 *   [GhaisNoir] text ladder / chrome.
 * - Editorial type: tracked-out tertiary eyebrow + display-editorial headline
 *   (matches Home "Return to / Your Quran" and Profile "Your / Space").
 * - Specular hairlines: ghost card border + bright top-only specular on the
 *   logo glass; clay wells for actions.
 * - Chrome pills: selected filter is a white-chrome pill with near-black
 *   label; unselected are ghost pills.
 */
@Composable
fun ExploreTopBar(
    onSearchClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // App branding & Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // App mark in mini glass card — gradient fill + ghost border + specular
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(GhaisShapes.medium)
                    .background(GhaisNoir.cardFill())
                    .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.medium)
                    .topSpecular(inset = 10.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.ghais_mark),
                    contentDescription = "Ghais App Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "ASSALAMU ALAIKUM",
                    color = GhaisNoir.TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "Explore",
                    style = GhaisTypography.displayEditorialBold.copy(
                        fontSize = 28.sp,
                        lineHeight = 34.sp
                    ),
                    maxLines = 1
                )
            }
        }

        // Action icons with 48dp minimum touch target areas
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Search button with full 48dp touch target
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(GhaisShapes.well)
                    .noirClickable(onClick = onSearchClick),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(GhaisNoir.wellFill(), GhaisShapes.well)
                        .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.well),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = GhaisNoir.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Profile avatar with full 48dp touch target
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(GhaisShapes.well)
                    .noirClickable(onClick = onProfileClick),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = GhaisAssets.ProfileAvatarUrl,
                    contentDescription = "Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(GhaisShapes.well)
                        .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.well)
                        .background(GhaisNoir.Fill2)
                )
            }
        }
    }
}

@Composable
fun ExploreSearchBar(
    query: String = "",
    onQueryChange: (String) -> Unit = {},
    onFilterClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Engraved inset pill (RecitersHero recipe). Focus read is monochrome:
    // specular rim when active, ghost inset rim at rest — never hue.
    val rim = if (query.isNotEmpty()) GhaisNoir.SpecularTop else GhaisNoir.InsetBorder

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .heightIn(min = 48.dp)
            .clip(GhaisShapes.pill)
            .background(GhaisNoir.insetFill())
            .border(width = 1.dp, color = rim, shape = GhaisShapes.pill)
            .padding(start = 14.dp, end = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = if (query.isNotEmpty()) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Interactive Text Input Field
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "Surah name, verse keywords, reciter...",
                        fontSize = 15.sp,
                        color = GhaisNoir.TextDisabled,
                        fontWeight = FontWeight.Normal
                    )
                }

                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = GhaisNoir.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(GhaisNoir.TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Clear button if query has text with 48dp touch target
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(GhaisShapes.well)
                        .noirClickable(onClick = { onQueryChange("") }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = GhaisNoir.TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Filter button with 48dp touch target
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(GhaisShapes.well)
                    .noirClickable(onClick = onFilterClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Filter",
                    tint = GhaisNoir.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ExploreFilterPills(
    selectedPill: String = "All",
    onSelectPill: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pills = listOf("All", "Surahs (114)", "Juz (30)", "Reciters", "Topics", "Duas & Ruqyah")

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(pills) { pill ->
            val isSelected = pill == selectedPill
            // Wrapper ensuring full 48dp touch target
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(GhaisShapes.pill)
                    .noirClickable(onClick = { onSelectPill(pill) }),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    // Chrome pill — white gradient, near-black label, inner highlight
                    Box(
                        modifier = Modifier
                            .heightIn(min = 34.dp)
                            .clip(GhaisShapes.pill)
                            .background(GhaisNoir.chromeFill())
                            .border(1.dp, Color.White.copy(alpha = 0.35f), GhaisShapes.pill)
                            .drawBehind {
                                drawLine(
                                    GhaisNoir.ChromeInnerHighlight,
                                    Offset(size.width * 0.18f, 1.5f),
                                    Offset(size.width * 0.82f, 1.5f),
                                    strokeWidth = 1.5f
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pill,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GhaisNoir.OnChrome
                        )
                    }
                } else {
                    // Ghost pill — white wash fill + hairline, secondary label
                    Box(
                        modifier = Modifier
                            .heightIn(min = 34.dp)
                            .clip(GhaisShapes.pill)
                            .background(GhaisNoir.Fill2)
                            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pill,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = GhaisNoir.TextSecondary
                        )
                    }
                }
            }
        }
    }
}
