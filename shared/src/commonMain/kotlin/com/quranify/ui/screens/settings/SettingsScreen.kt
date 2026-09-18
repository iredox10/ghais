package com.quranify.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.quranify.ui.theme.QuranifyColors

object SettingsScreen : Tab {
    override val options: TabOptions
        @Composable
        get() = remember {
            TabOptions(
                index = 3u,
                title = "Settings",
                icon = null
            )
        }

    @Composable
    override fun Content() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(QuranifyColors.Background)
        ) {
            Text(
                text = "Settings",
                color = QuranifyColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(24.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
            ) {
                item {
                    SettingsGroup("Account") {
                        ProfileRow()
                    }
                }
                item {
                    SettingsGroup("Audio") {
                        SettingsRow(title = "Default quality", value = "High (320kbps)")
                        SettingsRow(title = "Default reciter", value = "Mishary Alafasy")
                    }
                }
                item {
                    SettingsGroup("Downloads") {
                        SettingsRow(title = "Storage used", value = "1.2 GB")
                        SettingsToggleRow(title = "WiFi only", checked = true)
                    }
                }
                item {
                    SettingsGroup("Notifications") {
                        SettingsToggleRow(title = "Daily reminder", checked = true)
                        SettingsToggleRow(title = "Friday Kahf", checked = false)
                    }
                }
                item {
                    SettingsGroup("Appearance") {
                        SettingsToggleRow(title = "Dark mode", checked = true)
                    }
                }
                item {
                    SettingsGroup("About") {
                        SettingsRow(title = "Version", value = "1.0.0")
                        SettingsRow(title = "Attributions", value = "")
                    }
                }
                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Text(
            text = title,
            color = QuranifyColors.Primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(QuranifyColors.Card)
        ) {
            content()
        }
    }
}

@Composable
private fun ProfileRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(QuranifyColors.Surface)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = "Guest User", color = QuranifyColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(text = "Sign in to sync your library", color = QuranifyColors.TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SettingsRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = QuranifyColors.TextPrimary, fontSize = 16.sp)
        if (value.isNotEmpty()) {
            Text(text = value, color = QuranifyColors.TextSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun SettingsToggleRow(title: String, checked: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = QuranifyColors.TextPrimary, fontSize = 16.sp)
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = QuranifyColors.Background,
                checkedTrackColor = QuranifyColors.Primary,
                uncheckedThumbColor = QuranifyColors.TextSecondary,
                uncheckedTrackColor = QuranifyColors.Surface
            )
        )
    }
}
