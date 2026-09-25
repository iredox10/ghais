package com.ghais.ui.screens.routines

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ghais.data.repository.CustomRoutinesStore
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.rememberMergedReciters
import com.ghais.data.repository.RoutineItem
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.components.noir.NoirListRow
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSwitch
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

class RoutineEditorScreen(val routineId: String? = null) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val existing = remember(routineId) {
            routineId?.let { CustomRoutinesStore.get(it) }
        }

        var title by remember { mutableStateOf(existing?.title ?: "") }
        var description by remember { mutableStateOf(existing?.description ?: "") }
        var isPublic by remember { mutableStateOf(existing?.isPublic ?: false) }
        var items by remember { mutableStateOf(existing?.items ?: emptyList()) }

        val reciters = rememberMergedReciters()
        val surahs = remember { QuranDataRepository.getSurahs() }

        var selectedReciterSlug by remember {
            mutableStateOf(items.lastOrNull()?.reciterSlug ?: reciters.firstOrNull()?.slug.orEmpty())
        }
        var selectedSurahId by remember {
            mutableStateOf(items.lastOrNull()?.surahId ?: 1)
        }
        var reciterExpanded by remember { mutableStateOf(false) }
        var surahExpanded by remember { mutableStateOf(false) }

        val selectedReciterName = remember(selectedReciterSlug) {
            reciters.find { it.slug == selectedReciterSlug }?.nameEn
                ?: reciters.firstOrNull()?.nameEn.orEmpty()
        }
        val selectedSurahLabel = remember(selectedSurahId) {
            surahs.find { it.id == selectedSurahId }?.let { "${it.id}. ${it.nameEn}" }
                ?: "Surah $selectedSurahId"
        }
        val canSave = title.isNotBlank()

        NoirScreenRoot {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header: ghost back + title.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(GhaisNoir.Fill2)
                            .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                            .noirClickable { navigator.pop() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GhaisNoir.TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (routineId != null) "Edit routine" else "New routine",
                        color = GhaisNoir.TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                }

                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 6.dp,
                        bottom = 112.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    item {
                        GlassFieldLabel("Title")
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("e.g. Morning adhkar recitations", color = GhaisNoir.TextTertiary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = GhaisShapes.row,
                            colors = glassFieldColors()
                        )
                    }

                    item {
                        GlassFieldLabel("Description")
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text("What is this routine for?", color = GhaisNoir.TextTertiary) },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                            shape = GhaisShapes.row,
                            colors = glassFieldColors()
                        )
                    }

                    item {
                        GlassFieldLabel("Reciter")
                        ExposedDropdownMenuBox(
                            expanded = reciterExpanded,
                            onExpandedChange = { reciterExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedReciterName,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = reciterExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                shape = GhaisShapes.row,
                                colors = glassFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = reciterExpanded,
                                onDismissRequest = { reciterExpanded = false }
                            ) {
                                reciters.forEach { reciter ->
                                    DropdownMenuItem(
                                        text = { Text(reciter.nameEn, color = GhaisNoir.TextPrimary) },
                                        onClick = {
                                            selectedReciterSlug = reciter.slug
                                            reciterExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        GlassFieldLabel("Surah")
                        ExposedDropdownMenuBox(
                            expanded = surahExpanded,
                            onExpandedChange = { surahExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedSurahLabel,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = surahExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                shape = GhaisShapes.row,
                                colors = glassFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = surahExpanded,
                                onDismissRequest = { surahExpanded = false }
                            ) {
                                surahs.forEach { surah ->
                                    DropdownMenuItem(
                                        text = { Text("${surah.id}. ${surah.nameEn}", color = GhaisNoir.TextPrimary) },
                                        onClick = {
                                            selectedSurahId = surah.id
                                            surahExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        val candidate = RoutineItem(
                            reciterSlug = selectedReciterSlug,
                            surahId = selectedSurahId
                        )
                        val isDuplicate = items.any {
                            it.reciterSlug == candidate.reciterSlug && it.surahId == candidate.surahId
                        }
                        if (isDuplicate) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(GhaisShapes.pill)
                                    .background(GhaisNoir.Fill2)
                                    .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                                    .padding(vertical = 15.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Already added",
                                    color = GhaisNoir.TextTertiary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            ChromePillButton(
                                text = "Add surah",
                                onClick = { items = items + candidate },
                                leadingIcon = Icons.Filled.Add,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Surahs (${items.size})",
                                color = GhaisNoir.TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (items.isNotEmpty()) {
                                TextButton(onClick = { items = emptyList() }) {
                                    Text(
                                        text = "Clear all",
                                        color = GhaisNoir.TextSecondary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    itemsIndexed(items, key = { index, item -> "${item.reciterSlug}:${item.surahId}#$index" }) { _, item ->
                        val surah = surahs.find { it.id == item.surahId }
                        val reciterName = reciters.find { it.slug == item.reciterSlug }?.nameEn
                            ?: QuranDataRepository.getReciterBySlug(item.reciterSlug).nameEn
                        NoirListRow(
                            title = surah?.let { "${it.id}. ${it.nameEn}" }
                                ?: "Surah ${item.surahId}",
                            subtitle = reciterName,
                            icon = Icons.Filled.QueueMusic,
                            chevron = false,
                            trailing = {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(GhaisNoir.Fill1)
                                        .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                                        .noirClickable {
                                            items = items.filterNot {
                                                it.reciterSlug == item.reciterSlug && it.surahId == item.surahId
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Remove",
                                        tint = GhaisNoir.TextTertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )
                    }

                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(GhaisShapes.row)
                                .background(GhaisNoir.cardFillSoft())
                                .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.row)
                                .topSpecular(inset = 22.dp)
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Share publicly",
                                    color = GhaisNoir.TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Let others discover this routine",
                                    color = GhaisNoir.TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                            NoirSwitch(
                                checked = isPublic,
                                onCheckedChange = { isPublic = it }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        ChromePillButton(
                            text = "Save routine",
                            onClick = {
                                val trimmedTitle = title.trim()
                                val trimmedDescription = description.trim()
                                if (existing != null) {
                                    CustomRoutinesStore.update(
                                        existing.copy(
                                            title = trimmedTitle,
                                            description = trimmedDescription,
                                            items = items,
                                            isPublic = isPublic
                                        )
                                    )
                                } else {
                                    CustomRoutinesStore.create(
                                        title = trimmedTitle,
                                        description = trimmedDescription,
                                        items = items,
                                        isPublic = isPublic
                                    )
                                }
                                navigator.pop()
                            },
                            enabled = canSave,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (!canSave) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Give your routine a title to save it.",
                                color = GhaisNoir.TextTertiary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassFieldLabel(text: String) {
    Text(
        text = text,
        color = GhaisNoir.TextPrimary,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

/**
 * Engraved noir field chrome: white cursor, specular focus rim, alpha-white wash.
 */
@Composable
private fun glassFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = GhaisNoir.TextPrimary,
    unfocusedTextColor = GhaisNoir.TextPrimary,
    cursorColor = Color.White,
    focusedBorderColor = GhaisNoir.SpecularTop,
    unfocusedBorderColor = GhaisNoir.BorderCard,
    focusedContainerColor = GhaisNoir.Fill2,
    unfocusedContainerColor = GhaisNoir.Fill1,
    focusedTrailingIconColor = GhaisNoir.TextSecondary,
    unfocusedTrailingIconColor = GhaisNoir.TextTertiary,
    focusedPlaceholderColor = GhaisNoir.TextTertiary,
    unfocusedPlaceholderColor = GhaisNoir.TextTertiary
)
