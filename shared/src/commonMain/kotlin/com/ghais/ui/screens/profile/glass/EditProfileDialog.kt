package com.ghais.ui.screens.profile.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val GlassCard = Color(0xFF1C1C1E)
private val GlassMuted = Color(0xFF9A9AA0)

@Composable
fun EditProfileDialogGlass(
    initialName: String,
    initialBio: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var editingName by remember(initialName) { mutableStateOf(initialName) }
    var editingBio by remember(initialBio) { mutableStateOf(initialBio) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassCard,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Edit Profile",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Display Name",
                    color = GlassMuted,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                GlassField(
                    value = editingName,
                    onValueChange = { editingName = it },
                    singleLine = true,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Spiritual Bio / Intention",
                    color = GlassMuted,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                GlassField(
                    value = editingBio,
                    onValueChange = { editingBio = it },
                    singleLine = false,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(editingName.trim(), editingBio.trim()) }) {
                Text(
                    text = "Save",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = GlassMuted
                )
            }
        }
    )
}

@Composable
private fun GlassField(
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean,
    maxLines: Int
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        maxLines = maxLines,
        textStyle = TextStyle(
            color = Color.White,
            fontSize = 14.sp
        ),
        cursorBrush = SolidColor(Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    )
}
