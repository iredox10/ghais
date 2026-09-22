package com.ghais.ui.components.noir

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ghais.ui.theme.GhaisNoir

/**
 * Phase 3 — NoirSwitch: full-monochrome toggle.
 * ON = solid chrome track + near-black thumb (the "Enable" pill language).
 * OFF = engraved track + grey thumb.
 */
@Composable
fun NoirSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = GhaisNoir.NoirBlack,
            checkedTrackColor = Color.White,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = GhaisNoir.TextTertiary,
            uncheckedTrackColor = GhaisNoir.Fill4,
            uncheckedBorderColor = GhaisNoir.BorderCard
        )
    )
}
