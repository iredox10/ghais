package com.ghais.ui.screens.reciters.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.repository.reciterCountryKey
import com.ghais.domain.model.Reciter
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * AGENT A2 — Noir taste-axis chip groups for the Reciters tab.
 *
 * One labeled, horizontally scrollable chip row per axis (Riwaya / Style /
 * Pace / Mood / Nation). Single-select per group: tapping the selected chip
 * again clears that axis. A "Clear all" affordance appears only while at
 * least one axis is active. Pure presentational: state lives with the caller.
 *
 * Nation is rendered as chips (not a dropdown) to stay consistent with the
 * other four axes; each chip label is the option display spelling while
 * selection compares on [reciterCountryKey].
 */
@Composable
fun FilterChipsRow(
    state: ReciterFilterState,
    options: ReciterFilterOptions,
    onStateChange: (ReciterFilterState) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (options.riwayahs.isNotEmpty()) {
            FilterGroupRow(
                label = "Riwaya",
                options = options.riwayahs,
                isSelected = { option -> state.riwayah?.trim().equals(option.trim(), ignoreCase = true) },
                onToggle = { option ->
                    val active = state.riwayah?.trim().equals(option.trim(), ignoreCase = true)
                    onStateChange(state.copy(riwayah = if (active) null else option))
                }
            )
        }
        if (options.styles.isNotEmpty()) {
            FilterGroupRow(
                label = "Style",
                options = options.styles,
                isSelected = { option -> state.style?.trim().equals(option.trim(), ignoreCase = true) },
                onToggle = { option ->
                    val active = state.style?.trim().equals(option.trim(), ignoreCase = true)
                    onStateChange(state.copy(style = if (active) null else option))
                }
            )
        }
        if (options.tempos.isNotEmpty()) {
            FilterGroupRow(
                label = "Pace",
                options = options.tempos,
                isSelected = { option -> state.tempo?.trim().equals(option.trim(), ignoreCase = true) },
                onToggle = { option ->
                    val active = state.tempo?.trim().equals(option.trim(), ignoreCase = true)
                    onStateChange(state.copy(tempo = if (active) null else option))
                }
            )
        }
        if (options.moods.isNotEmpty()) {
            FilterGroupRow(
                label = "Mood",
                options = options.moods.map { it.label },
                isSelected = { option ->
                    state.mood?.label.equals(option, ignoreCase = true)
                },
                onToggle = { option ->
                    val picked = options.moods.firstOrNull { it.label.equals(option, ignoreCase = true) }
                    onStateChange(state.copy(mood = if (state.mood == picked) null else picked))
                }
            )
        }
        if (options.nations.isNotEmpty()) {
            FilterGroupRow(
                label = "Nation",
                options = options.nations,
                isSelected = { option -> reciterCountryKey(option) == state.nation },
                onToggle = { option ->
                    val key = reciterCountryKey(option)
                    onStateChange(state.copy(nation = if (state.nation == key) null else key))
                }
            )
        }
        if (state.hasActiveFilters()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val count = state.activeCount()
                Text(
                    text = if (count == 1) "1 filter • Clear all" else "$count filters • Clear all",
                    color = GhaisNoir.TextTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(GhaisShapes.pill)
                        .noirClickable { onStateChange(state.cleared()) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * Convenience overload: derives [ReciterFilterOptions] from the catalog via
 * remember, so the coordinator only holds [ReciterFilterState].
 */
@Composable
fun FilterChipsRow(
    reciters: List<Reciter>,
    state: ReciterFilterState,
    onStateChange: (ReciterFilterState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = remember(reciters) { deriveFilterOptions(reciters) }
    FilterChipsRow(
        state = state,
        options = options,
        onStateChange = onStateChange,
        modifier = modifier
    )
}

@Composable
private fun FilterGroupRow(
    label: String,
    options: List<String>,
    isSelected: (String) -> Boolean,
    onToggle: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            color = GhaisNoir.TextTertiary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = options, key = { it.lowercase() }) { option ->
                NoirFilterChip(
                    text = option,
                    selected = isSelected(option),
                    onToggle = { onToggle(option) }
                )
            }
        }
    }
}

/**
 * Noir pill: smoked-glass resting state (soft card wash + ghost border +
 * tertiary text), chrome selected state (bright wash + near-black label).
 * Monochrome only — selection never introduces hue.
 */
@Composable
private fun NoirFilterChip(
    text: String,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(GhaisShapes.pill)
            .background(if (selected) GhaisNoir.chromeFill() else GhaisNoir.cardFillSoft())
            .border(
                1.dp,
                if (selected) GhaisNoir.SpecularTop else GhaisNoir.BorderCard,
                GhaisShapes.pill
            )
            .noirClickable(onToggle)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) GhaisNoir.OnChrome else GhaisNoir.TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}
