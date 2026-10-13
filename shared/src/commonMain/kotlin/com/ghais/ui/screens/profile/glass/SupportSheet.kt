package com.ghais.ui.screens.profile.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.share.SUPPORT_EMAIL
import com.ghais.data.share.SupportKind
import com.ghais.data.share.SupportMail
import com.ghais.data.share.buildSupportBody
import com.ghais.data.share.supportSubject
import com.ghais.ui.components.noir.NoirInsetField
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.theme.GhaisNoir

/**
 * Report-a-bug / suggest-a-feature / contact-us sheet.
 *
 * The user writes free text; Send opens the mail client with a pre-filled
 * message to [SUPPORT_EMAIL] whose body carries the app version, device
 * model and OS version plus the signed-in account ([buildSupportBody]).
 * Null [kind] hides the sheet. When no mail client is available the send
 * button explains instead of failing silently. Never throws.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportSheet(
    kind: SupportKind?,
    accountEmail: String?,
    onDismiss: () -> Unit
) {
    if (kind == null) return
    var message by remember(kind) { mutableStateOf("") }
    var noMailClient by remember(kind) { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val title = when (kind) {
        SupportKind.BUG -> "Report a bug"
        SupportKind.FEATURE -> "Suggest a feature"
        SupportKind.CONTACT -> "Contact us"
    }
    val hint = when (kind) {
        SupportKind.BUG -> "What went wrong? Tell us what happened…"
        SupportKind.FEATURE -> "What should Ghaith do? Describe your idea…"
        SupportKind.CONTACT -> "How can we help? Write your message…"
    }
    val available = SupportMail.isAvailable()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GhaisNoir.NoirBlack,
        contentColor = GhaisNoir.TextPrimary,
        shape = com.ghais.ui.theme.GhaisShapes.cardNoir
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                color = GhaisNoir.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Sends an email to $SUPPORT_EMAIL with your app details attached.",
                color = GhaisNoir.TextTertiary,
                fontSize = 12.sp
            )
            NoirInsetField {
                BasicTextField(
                    value = message,
                    onValueChange = { message = it },
                    singleLine = false,
                    minLines = 4,
                    textStyle = TextStyle(
                        color = GhaisNoir.TextPrimary,
                        fontSize = 15.sp
                    ),
                    cursorBrush = SolidColor(GhaisNoir.TextPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 110.dp),
                    decorationBox = { inner ->
                        if (message.isEmpty()) {
                            Text(
                                text = hint,
                                color = GhaisNoir.TextDisabled,
                                fontSize = 15.sp
                            )
                        }
                        inner()
                    }
                )
            }
            if (noMailClient || !available) {
                Text(
                    text = "No email app found on this device — write to us at $SUPPORT_EMAIL.",
                    color = GhaisNoir.TextSecondary,
                    fontSize = 12.sp
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (message.isNotBlank() && available) GhaisNoir.Fill4 else GhaisNoir.Fill2,
                        CircleShape
                    )
                    .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                    .then(
                        if (message.isNotBlank() && available) {
                            Modifier.noirClickable {
                                val sent = runCatching {
                                    SupportMail.composeEmail(
                                        SUPPORT_EMAIL,
                                        supportSubject(kind),
                                        buildSupportBody(kind, message, accountEmail)
                                    )
                                }.getOrDefault(false)
                                if (sent) {
                                    onDismiss()
                                } else {
                                    noMailClient = true
                                }
                            }
                        } else Modifier
                    )
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Send",
                    color = if (message.isNotBlank() && available) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
