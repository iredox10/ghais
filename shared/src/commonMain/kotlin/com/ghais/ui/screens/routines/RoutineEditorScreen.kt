package com.ghais.ui.screens.routines

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ghais.data.repository.CustomRoutinesStore
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.RoutineItem

private val PureBlack = Color(0xFF000000)
private val MutedGrey = Color(0xFF9A9AA0)
private val LinkBlue = Color(0xFF4C8DFF)

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

        val reciters = remember { QuranDataRepository.getReciters() }
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack)
        ) {
            // Header: back + title
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
                        .background(Color.Black.copy(alpha = 0.35f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                        .clickable { navigator.pop() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (routineId != null) "Edit routine" else "New routine",
                    color = Color.White,
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
                        placeholder = { Text("e.g. Morning adhkar recitations", color = MutedGrey) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = glassFieldColors()
                    )
                }

                item {
                    GlassFieldLabel("Description")
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("What is this routine for?", color = MutedGrey) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
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
                            shape = RoundedCornerShape(20.dp),
                            colors = glassFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = reciterExpanded,
                            onDismissRequest = { reciterExpanded = false }
                        ) {
                            reciters.forEach { reciter ->
                                DropdownMenuItem(
                                    text = { Text(reciter.nameEn) },
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
                            shape = RoundedCornerShape(20.dp),
                            colors = glassFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = surahExpanded,
                            onDismissRequest = { surahExpanded = false }
                        ) {
                            surahs.forEach { surah ->
                                DropdownMenuItem(
                                    text = { Text("${surah.id}. ${surah.nameEn}") },
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .background(if (isDuplicate) MutedGrey.copy(alpha = 0.4f) else LinkBlue)
                            .clickable(enabled = !isDuplicate) {
                                items = items + candidate
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add surah",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isDuplicate) "Already added" else "Add surah",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Surahs (${items.size})",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (items.isNotEmpty()) {
                            TextButton(onClick = { items = emptyList() }) {
                                Text(
                                    text = "Clear all",
                                    color = LinkBlue,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                items(items, key = { "${it.reciterSlug}:${it.surahId}" }) { item ->
                    val surah = surahs.find { it.id == item.surahId }
                    val reciterName = reciters.find { it.slug == item.reciterSlug }?.nameEn
                        ?: QuranDataRepository.getReciterBySlug(item.reciterSlug).nameEn
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = surah?.let { "${it.id}. ${it.nameEn}" }
                                    ?: "Surah ${item.surahId}",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = reciterName,
                                color = MutedGrey,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = {
                                items = items.filterNot {
                                    it.reciterSlug == item.reciterSlug && it.surahId == item.surahId
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove",
                                tint = MutedGrey,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Share publicly",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Let others discover this routine",
                                color = MutedGrey,
                                fontSize = 13.sp
                            )
                        }
                        Switch(
                            checked = isPublic,
                            onCheckedChange = { isPublic = it },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = LinkBlue,
                                checkedThumbColor = Color.White
                            )
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .background(if (canSave) LinkBlue else MutedGrey.copy(alpha = 0.4f))
                            .clickable(enabled = canSave) {
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
                            }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Save routine",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (!canSave) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Give your routine a title to save it.",
                            color = MutedGrey,
                            fontSize = 13.sp
                        )
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
        color = Color.White,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun glassFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = LinkBlue,
    focusedBorderColor = LinkBlue,
    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
    focusedContainerColor = Color.White.copy(alpha = 0.05f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
)
