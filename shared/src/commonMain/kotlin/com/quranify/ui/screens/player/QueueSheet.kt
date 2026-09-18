package com.quranify.ui.screens.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val surfaceColor = Color(0xFF141419)
    val cardColor = Color(0xFF1C1C24)
    val primaryColor = Color(0xFFD4A853)
    val textPrimaryColor = Color(0xFFF0EDE6)
    val textSecondaryColor = Color(0xFF8A8A96)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = surfaceColor,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Up Next",
                    color = textPrimaryColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { /* Clear Queue */ }) {
                    Text("Clear Queue", color = primaryColor)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val queueItems = listOf(
                "Ayah 255 (Al-Baqarah)",
                "Ayah 256 (Al-Baqarah)",
                "Ayah 257 (Al-Baqarah)",
                "Ayah 258 (Al-Baqarah)"
            )
            
            LazyColumn {
                itemsIndexed(queueItems) { index, item ->
                    val isActive = index == 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Drag Handle",
                                tint = textSecondaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = item,
                                    color = if (isActive) primaryColor else textPrimaryColor,
                                    fontSize = 16.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = "Mishary Rashid Alafasy",
                                    color = textSecondaryColor,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        IconButton(onClick = { /* Remove */ }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Remove",
                                tint = textSecondaryColor
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
