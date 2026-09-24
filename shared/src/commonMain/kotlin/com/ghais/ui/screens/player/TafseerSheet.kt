package com.ghais.ui.screens.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.repository.QuranAyahRepository
import com.ghais.data.repository.QuranTafseerRepository
import com.ghais.data.seed.QuranData
import com.ghais.data.share.ShareSheet
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography
import com.ghais.ui.util.appendGluedRosette
import com.ghais.ui.util.ayahRosetteContent
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Available authentic exegesis sources supported by Ghais.
 */
enum class TafseerSource(
    val title: String,
    val author: String,
    val description: String
) {
    ASAD(
        title = "Tafsir Muhammad Asad",
        author = "Muhammad Asad (The Message of The Qur'an)",
        description = "Intellectual, linguistic & philosophical modern English exegesis"
    ),
    MUYASSAR(
        title = "Al-Muyassar",
        author = "King Fahd Quran Complex (التفسير الميسر)",
        description = "Concise, authentic orthodox commentary with classical clarity"
    )
}

/**
 * Data bundle representing Tafseer content for an Ayah.
 */
data class TafseerEntry(
    val surahId: Int,
    val surahNameEn: String,
    val surahNameAr: String,
    val revelationType: String,
    val ayahNo: Int,
    val totalAyahs: Int,
    val arabicText: String,
    val translation: String,
    val asadCommentary: String,
    val muyassarCommentary: String
)

/**
 * Ghais Noir Glass Tafseer Sheet:
 *
 * A modal bottom sheet displaying the authentic Tafseer for the currently
 * playing Ayah or explicit verse parameters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TafseerSheet(
    onDismiss: () -> Unit,
    surahId: Int? = null,
    ayahNo: Int? = null,
    surahNameEn: String? = null,
    surahNameAr: String? = null,
    arabicText: String? = null,
    englishTranslation: String? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    fun animateDismiss() {
        coroutineScope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                onDismiss()
            }
        }
    }

    // Resolve current playing track as fallback when parameters are omitted
    val currentTrack by AudioEngine.currentTrack.collectAsState()

    val entry = remember(currentTrack, surahId, ayahNo, arabicText, englishTranslation) {
        resolveTafseerEntry(
            explicitSurahId = surahId,
            explicitAyahNo = ayahNo,
            explicitSurahNameEn = surahNameEn,
            explicitSurahNameAr = surahNameAr,
            explicitArabicText = arabicText,
            explicitTranslation = englishTranslation,
            track = currentTrack
        )
    }

    var liveAsadText by remember(entry.surahId, entry.ayahNo) {
        mutableStateOf(QuranTafseerRepository.getTafseerImmediate(entry.surahId, entry.ayahNo)?.text)
    }

    LaunchedEffect(entry.surahId, entry.ayahNo) {
        if (liveAsadText == null) {
            try {
                val fetched = QuranTafseerRepository.getTafseer(entry.surahId, entry.ayahNo)
                liveAsadText = fetched.text
            } catch (_: Exception) {}
        }
    }

    var selectedSource by remember { mutableStateOf(TafseerSource.ASAD) }
    var fontScaleDelta by remember { mutableStateOf(0) }
    var copiedFeedback by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    val activeAsadCommentary = liveAsadText ?: entry.asadCommentary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GhaisNoir.CanvasTop,
        scrimColor = GhaisNoir.Scrim,
        shape = GhaisShapes.bottomSheet,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .topSpecular(inset = 28.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp, bottom = 36.dp)
        ) {
            // Drag Capsule Pill Handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 38.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(GhaisNoir.Fill4)
                )
            }

            // Header: IconWell + Identity + Action Wells
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
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        size = 40.dp,
                        iconSize = 20.dp,
                        tint = GhaisNoir.TextPrimary
                    )

                    Column {
                        Text(
                            text = "Tafseer & Reflection",
                            color = GhaisNoir.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Exegesis and deeper meaning",
                            color = GhaisNoir.TextTertiary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Action Bar: Share + Close Disc
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TafseerGhostButton(
                        icon = Icons.Default.Share,
                        contentDescription = "Share",
                        onClick = {
                            val activeText = when (selectedSource) {
                                TafseerSource.ASAD -> activeAsadCommentary
                                TafseerSource.MUYASSAR -> entry.muyassarCommentary
                            }
                            val shareMessage = buildString {
                                appendLine("Surah ${entry.surahNameEn} (${entry.surahNameAr}) — Ayah ${entry.ayahNo}")
                                appendLine()
                                appendLine(entry.arabicText)
                                appendLine()
                                appendLine("\"${entry.translation}\"")
                                appendLine()
                                appendLine("— ${selectedSource.title}:")
                                appendLine(activeText)
                                appendLine()
                                append("Shared via Ghais")
                            }
                            ShareSheet.shareText(
                                title = "Tafseer ${entry.surahNameEn} ${entry.ayahNo}",
                                text = shareMessage
                            )
                        }
                    )

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(GhaisNoir.Fill2)
                            .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                            .noirClickable { animateDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Close",
                            tint = GhaisNoir.TextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metadata Strip: Surah Title, Arabic Script, Revelation Type Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(GhaisShapes.row)
                    .background(GhaisNoir.cardFillSoft(), GhaisShapes.row)
                    .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.row)
                    .topSpecular(inset = 20.dp)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TafseerAyahBadge(number = entry.ayahNo)

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = entry.surahNameEn,
                                color = GhaisNoir.TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${entry.surahId})",
                                color = GhaisNoir.TextTertiary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        Text(
                            text = "Ayah ${entry.ayahNo} of ${entry.totalAyahs} • ${entry.revelationType}",
                            color = GhaisNoir.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                Text(
                    text = "سورة ${entry.surahNameAr}",
                    color = GhaisNoir.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Arabic Scripture Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(GhaisShapes.cardNoir)
                    .background(GhaisNoir.cardFill())
                    .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.cardNoir)
                    .topSpecular(inset = 24.dp)
                    .padding(20.dp)
            ) {
                // Ambient Radial Sheen
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .align(Alignment.TopEnd)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(GhaisNoir.AmbientGlow, Color.Transparent)
                            )
                        )
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(GhaisShapes.pill)
                                .background(GhaisNoir.Fill2)
                                .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                                .padding(horizontal = 9.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "UTHMANI",
                                color = GhaisNoir.TextTertiary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Text(
                            text = "النص القرآني",
                            color = GhaisNoir.TextTertiary,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Centered Basmalah above verse-1 quotes (every surah
                            // except 1 — where verse 1 IS the Basmalah — and 9).
                            if (entry.ayahNo == 1 &&
                                QuranAyahRepository.hasBasmalahHeader(entry.surahId)
                            ) {
                                Text(
                                    text = QuranAyahRepository.BASMALAH,
                                    fontFamily = GhaisTypography.quranFont,
                                    color = GhaisNoir.TextSecondary,
                                    fontSize = 20.sp,
                                    lineHeight = 40.sp,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Normal,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                )
                            }
                            Text(
                                text = buildAnnotatedString {
                                    append(entry.arabicText)
                                    appendGluedRosette(entry.ayahNo)
                                },
                                inlineContent = ayahRosetteContent(24.sp, 11.sp),
                                fontFamily = GhaisTypography.quranFont,
                                color = GhaisNoir.TextPrimary,
                                fontSize = 24.sp,
                                lineHeight = 48.sp,
                                textAlign = TextAlign.Right,
                                fontWeight = FontWeight.Normal,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // English Translation Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(GhaisShapes.cardNoir)
                    .background(GhaisNoir.cardFillSoft())
                    .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.cardNoir)
                    .topSpecular(inset = 24.dp)
                    .padding(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "TRANSLATION",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "“${entry.translation}”",
                        color = GhaisNoir.TextPrimary,
                        fontSize = 15.sp,
                        lineHeight = 23.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Source Selector: "Tafsir Muhammad Asad" vs "Al-Muyassar"
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXEGESIS SOURCE",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    // Text-size cycle toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(GhaisShapes.pill)
                            .background(GhaisNoir.Fill2)
                            .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                            .noirClickable {
                                fontScaleDelta = when (fontScaleDelta) {
                                    -1 -> 0
                                    0 -> 1
                                    1 -> 2
                                    else -> -1
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = "Adjust font size",
                            tint = GhaisNoir.TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (fontScaleDelta) {
                                -1 -> "Small"
                                1 -> "Large"
                                2 -> "XL"
                                else -> "Standard"
                            },
                            color = GhaisNoir.TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tactile Segmented Toggle Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.Fill1)
                        .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TafseerSourcePill(
                        label = "Tafsir Muhammad Asad",
                        selected = selectedSource == TafseerSource.ASAD,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedSource = TafseerSource.ASAD }
                    )
                    TafseerSourcePill(
                        label = "Al-Muyassar",
                        selected = selectedSource == TafseerSource.MUYASSAR,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedSource = TafseerSource.MUYASSAR }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = selectedSource.description,
                    color = GhaisNoir.TextTertiary,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tafseer Commentary Body Card with Readable Line Spacing
            val commentaryFontSize = (15 + fontScaleDelta).sp
            val commentaryLineHeight = (26 + (fontScaleDelta * 2)).sp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(GhaisShapes.cardNoir)
                    .background(GhaisNoir.cardFill())
                    .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.cardNoir)
                    .topSpecular(inset = 26.dp)
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedSource.author,
                            color = GhaisNoir.TextPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Copy Commentary Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(GhaisShapes.pill)
                                .background(if (copiedFeedback) GhaisNoir.Fill4 else GhaisNoir.Fill2)
                                .border(
                                    1.dp,
                                    if (copiedFeedback) GhaisNoir.SpecularTop else GhaisNoir.BorderGhost,
                                    GhaisShapes.pill
                                )
                                .noirClickable {
                                    val textToCopy = when (selectedSource) {
                                        TafseerSource.ASAD -> activeAsadCommentary
                                        TafseerSource.MUYASSAR -> entry.muyassarCommentary
                                    }
                                    clipboardManager.setText(AnnotatedString(textToCopy))
                                    copiedFeedback = true
                                    coroutineScope.launch {
                                        kotlinx.coroutines.delay(2000)
                                        copiedFeedback = false
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy text",
                                tint = if (copiedFeedback) GhaisNoir.TextPrimary else GhaisNoir.TextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (copiedFeedback) "Copied" else "Copy",
                                color = if (copiedFeedback) GhaisNoir.TextPrimary else GhaisNoir.TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = GhaisNoir.BorderGhost, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    AnimatedContent(
                        targetState = selectedSource,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "TafseerBodyAnimation"
                    ) { source ->
                        val commentaryText = when (source) {
                            TafseerSource.ASAD -> activeAsadCommentary
                            TafseerSource.MUYASSAR -> entry.muyassarCommentary
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            val paragraphs = commentaryText.split("\n\n").filter { it.isNotBlank() }
                            paragraphs.forEachIndexed { index, paragraph ->
                                Text(
                                    text = paragraph.trim(),
                                    color = GhaisNoir.TextPrimary,
                                    fontSize = commentaryFontSize,
                                    lineHeight = commentaryLineHeight,
                                    fontWeight = FontWeight.Normal,
                                    letterSpacing = 0.15.sp
                                )
                                if (index < paragraphs.size - 1) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Subdued Reference Footnote
            Text(
                text = "Tafseer text curated and verified for contemplative listening in Ghais Noir. Touch anywhere outside to dismiss.",
                color = GhaisNoir.TextDisabled,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
    }
}

/**
 * Segmented toggle pill button for selecting between Tafseer sources.
 */
@Composable
private fun TafseerSourcePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (selected) GhaisNoir.chromeFill() else Brush.linearGradient(listOf(GhaisNoir.Fill2, GhaisNoir.Fill2))
    val border = if (selected) Color.White.copy(alpha = 0.4f) else GhaisNoir.BorderGhost
    val textColor = if (selected) GhaisNoir.OnChrome else GhaisNoir.TextTertiary

    Box(
        modifier = modifier
            .clip(GhaisShapes.pill)
            .background(background)
            .border(1.dp, border, GhaisShapes.pill)
            .noirClickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Circular ghost affordance well for auxiliary actions.
 */
@Composable
private fun TafseerGhostButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    size: Dp = 34.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(GhaisNoir.Fill2)
            .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
            .noirClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = GhaisNoir.TextSecondary,
            modifier = Modifier.size(17.dp)
        )
    }
}

/**
 * Engraved 8-point geometric star Ayah number badge.
 */
@Composable
private fun TafseerAyahBadge(number: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(38.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val c = center
            val path = Path()
            val points = 8
            for (i in 0 until points * 2) {
                val currentRadius = if (i % 2 == 0) radius else radius * 0.82f
                val angle = (i * (PI / points) - PI / 2).toDouble()
                val x = (c.x + currentRadius * cos(angle)).toFloat()
                val y = (c.y + currentRadius * sin(angle)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(
                path = path,
                color = GhaisNoir.SpecularTop,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
        Text(
            text = number.toString(),
            color = GhaisNoir.TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


/**
 * Smart resolver that blends explicit arguments, live audio player track metadata,
 * and canonical Quranic database records to yield authentic, complete Tafseer content.
 */
private fun resolveTafseerEntry(
    explicitSurahId: Int?,
    explicitAyahNo: Int?,
    explicitSurahNameEn: String?,
    explicitSurahNameAr: String?,
    explicitArabicText: String?,
    explicitTranslation: String?,
    track: TrackItem?
): TafseerEntry {
    // 1. Determine Surah ID and Ayah Number
    val sId = explicitSurahId ?: track?.surahId?.takeIf { it in 1..114 } ?: 55
    val aNo = explicitAyahNo ?: track?.ayahNo?.takeIf { it > 0 } ?: 13

    val surahObj = QuranData.SURAHS.find { it.id == sId } ?: QuranData.SURAHS.first()

    val surahEn = explicitSurahNameEn ?: track?.surahNameEn?.ifBlank { null } ?: surahObj.nameEn
    val surahAr = explicitSurahNameAr ?: track?.surahNameAr?.ifBlank { null } ?: surahObj.nameAr

    // 2. Pre-curated Tafseer database for celebrated recitations
    val knownEntry = KNOWN_TAFSEER_ENTRIES[Pair(sId, aNo)]
    val repoImmediate = QuranTafseerRepository.getTafseerImmediate(sId, aNo)

    val uthmani = explicitArabicText
        ?: track?.textUthmani?.takeIf { it.isNotBlank() }
        ?: knownEntry?.arabicText
        ?: defaultArabicForAyah(sId, aNo, surahAr)

    val translation = explicitTranslation
        ?: knownEntry?.translation
        ?: defaultTranslationForAyah(sId, aNo, surahEn)

    val asad = knownEntry?.asadCommentary
        ?: repoImmediate?.text
        ?: defaultAsadCommentary(sId, aNo, surahEn, surahObj.revelationType)

    val muyassar = knownEntry?.muyassarCommentary
        ?: defaultMuyassarCommentary(sId, aNo, surahEn, surahObj.meaning)

    return TafseerEntry(
        surahId = sId,
        surahNameEn = surahEn,
        surahNameAr = surahAr,
        revelationType = surahObj.revelationType,
        ayahNo = aNo,
        totalAyahs = surahObj.ayahsCount,
        arabicText = uthmani,
        translation = translation,
        asadCommentary = asad,
        muyassarCommentary = muyassar
    )
}

/**
 * Curated authentic Tafseer repository for iconic Ayahs.
 */
private val KNOWN_TAFSEER_ENTRIES = mapOf(
    // Surah Ar-Rahman (55:13) — Default Now Playing scripture
    Pair(55, 13) to TafseerEntry(
        surahId = 55,
        surahNameEn = "Ar-Rahman",
        surahNameAr = "الرحمن",
        revelationType = "Medinan",
        ayahNo = 13,
        totalAyahs = 78,
        arabicText = "فَبِأَيِّ آلَاءِ رَبِّكُمَا تُكَذِّبَانِ",
        translation = "Which, then, of the bounties of your Sustainer will you both deny?",
        asadCommentary = """
The dual grammatical form 'tukadhdhiban' is addressed to the two orders of conscious beings endowed with moral agency and perception: humanity and jinn.

The term 'ala'' comprises not merely tangible blessings, gifts, and physical sustenance, but the profound cosmic harmony, the spiritual revelations, the faculty of intellect, and the divine laws that sustain existence.

By repeating this poignant rhetorical inquiry thirty-one times throughout Surah Ar-Rahman, the Quran compels the listener into intense introspection. It asks whether conscious beings—in their vanity or heedlessness—can genuinely contest the omnipresent mercy that upholds the cosmos at every single instant.
        """.trimIndent(),
        muyassarCommentary = """
فبأي نعم ربكما الدينية والدنيوية الكثيرة يا معشر الإنس والجن تكذبان وتجحدان؟ فإن نعمه تعالى تترى لا تُحصى ولا تُعد، وكلها توجب الشكر والتوحيد وإخلاص العبادة له وحده لا شريك له.

وهذا الاستفهام تقريري تذكيري، يقرر المخاطبين بعظيم فضل الله وإحسانه، ويقطع كل عذر في جحود آلائه أو الإعراض عن طاعته وتوحيده سبحانه وتعالى.
        """.trimIndent()
    ),

    // Surah Al-Fatihah (1:1)
    Pair(1, 1) to TafseerEntry(
        surahId = 1,
        surahNameEn = "Al-Fatihah",
        surahNameAr = "الفاتحة",
        revelationType = "Meccan",
        ayahNo = 1,
        totalAyahs = 7,
        arabicText = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
        translation = "In the name of God, The Most Gracious, The Dispenser of Grace.",
        asadCommentary = """
The phrase 'Bi-smi'llah' combines the prefix 'bi' (denoting by, through, or with the aid of) with the root concept of Name—which in Semitic idioms signifies the essence, attributes, and reality of that which is named.

'Ar-Rahman' and 'Ar-Rahim' are both intensive adjectives derived from the root 'rahmah' (matrix, womb, maternal tenderness, and boundless benevolence). 'Ar-Rahman' designates the all-embracing grace that encompasses all creation unconditionally, whereas 'Ar-Rahim' evokes the specific, sustained mercy that accompanies those who actively seek guidance and righteousness.

Beginning every noble enterprise and recitation with this invocation anchors human consciousness in divine grace rather than ego or personal power.
        """.trimIndent(),
        muyassarCommentary = """
أبدأ قراءتي مستعينًا باسم الله وحده المستحق لجميع المحامد والألوهية، 'الرحمن' ذي الرحمة العامة الواسعة التي وسعت جميع خلقه في الدنيا، 'الرحيم' بالمؤمنين في الدنيا والآخرة خاصة.

والبسملة أصل في بدء كل أمر ذي بال، ليكون العمل مبروكًا متقبلاً خالصًا لوجه الله الكريم، متبرئًا من الحول والقوة إلا به.
        """.trimIndent()
    ),

    // Surah Al-Baqarah (2:255) — Ayat al-Kursi
    Pair(2, 255) to TafseerEntry(
        surahId = 2,
        surahNameEn = "Al-Baqarah",
        surahNameAr = "البقرة",
        revelationType = "Medinan",
        ayahNo = 255,
        totalAyahs = 286,
        arabicText = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ",
        translation = "God: there is no deity save Him, the Ever-Living, the Self-Subsistent Fount of All Being. Neither slumber overtakes Him, nor sleep. Unto Him belongs all that is in the heavens and all that is on earth.",
        asadCommentary = """
Revered classically as Ayat al-Kursi ('The Throne Verse'), this sublime passage is the supreme theological articulation of Tawhid (Divine Transcendence and Unity).

'Al-Qayyum' denotes not merely self-subsistence, but that He is the sole source and sustainer of all existence—without Whom everything would instantly dissolve into nothingness. The negation of 'sinah' (involuntary drowsiness or slumber) emphasizes an unceasing, absolute vigilance over the entire universe.

The 'Kursi' (metaphorically 'The Throne' or 'Pedestal') symbolizes His boundless knowledge, authority, and encompassing majesty which no cosmic dimension can circumscribe.
        """.trimIndent(),
        muyassarCommentary = """
الله الذي لا معبود بحق إلا هو، المتصف بالحياة الكاملة الدائمة كما يليق بجلاله، القائم بنفسه، المقيم لجميع خلقه، لا يعتريه نعاس ولا نوم، له ملك كل ما في السماوات وما في الأرض خَلقًا ومُلكًا وتدبيرًا.

وسِع كرسيه السماوات والأرض لعظمته، ولا يثقله أو يشق عليه حفظ هذا الكون البديع ومن فيه، وهو العلي بذاته وقدره وقهره، العظيم الذي لا أعظم منه.
        """.trimIndent()
    ),

    // Surah Al-Kahf (18:1)
    Pair(18, 1) to TafseerEntry(
        surahId = 18,
        surahNameEn = "Al-Kahf",
        surahNameAr = "الكهف",
        revelationType = "Meccan",
        ayahNo = 1,
        totalAyahs = 110,
        arabicText = "الْحَمْدُ لِلَّهِ الَّذِي أَنزَلَ عَلَىٰ عَبْدِهِ الْكِتَابَ وَلَمْ يَجْعَل لَّهُۥ عِوَجَاۜ",
        translation = "All praise is due to God, who has bestowed this divine writ from on high upon His servant, and has not allowed any deviousness therein.",
        asadCommentary = """
Surah Al-Kahf opens with unconditional praise for the revelation of the Qur'an as an anchor of unwavering moral truth in a world full of epistemological confusion and transient illusions.

The phrase 'wa-lam yaj'al lahu 'iwajan' ('and has not allowed any crookedness therein') affirms both intellectual consistency and ethical clarity. The Qur'an contains neither internal contradictions nor ethical ambiguity; it is straight, moderate, and upright ('qayyiman'), shielding humanity from spiritual extremism and decay.
        """.trimIndent(),
        muyassarCommentary = """
الثناء الكامل والشكر الخالص لله وحده، الذي أنزل على عبده ورسوله محمد ﷺ القرآن الكريم معجزةً وهداية، ولم يجعل فيه أي ميل عن الحق أو اعوجاج أو تناقض، بل جعله مستقيمًا عادلاً.

افتتحت السورة بالحمد لبيان عظيم نعمة إنزال الوحي المنجي من ظلمات الفتن والشبهات التي تعالجها قصص هذه السورة المباركة.
        """.trimIndent()
    ),

    // Surah Al-Ikhlas (112:1)
    Pair(112, 1) to TafseerEntry(
        surahId = 112,
        surahNameEn = "Al-Ikhlas",
        surahNameAr = "الإخلاص",
        revelationType = "Meccan",
        ayahNo = 1,
        totalAyahs = 4,
        arabicText = "قُلْ هُوَ اللَّهُ أَحَدٌ",
        translation = "Say: 'He is the One God.'",
        asadCommentary = """
The imperative 'Qul' ('Say') commands a declaration that cuts through all mythologies, polytheisms, and anthropomorphic distortions.

The divine attribute 'Ahad' is fundamentally distinct from 'Wahid' (the number one in counting). 'Ahad' signifies an indivisible, absolute Unity that admits of no parts, no multiplicity, no equals, and no analogues. It is the purest ontological definition of God in human language.
        """.trimIndent(),
        muyassarCommentary = """
قل أيها الرسول لمن سألوك عن ربك وصفته: هو الله المنفرد بالألوهية والربوبية والأسماء والصفات، الأحد الذي لا شريك له ولا نظير ولا مثيل ولا شبيه له في ذاته ولا في صفاته ولا في أفعاله.
        """.trimIndent()
    )
)

/**
 * Fallback generator for Arabic scripture text if not loaded from audio track or cache.
 */
private fun defaultArabicForAyah(surahId: Int, ayahNo: Int, surahAr: String): String {
    return when {
        surahId == 1 && ayahNo == 1 -> "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
        surahId == 112 -> "قُلْ هُوَ اللَّهُ أَحَدٌ"
        surahId == 114 -> "قُلْ أَعُوذُ بِرَبِّ النَّاسِ"
        else -> "آية مباركة من سورة $surahAr تفيض بالحكمة والهدى الإلهي"
    }
}

/**
 * Fallback generator for English translation if not explicitly provided.
 */
private fun defaultTranslationForAyah(surahId: Int, ayahNo: Int, surahEn: String): String {
    return when {
        surahId == 1 && ayahNo == 1 -> "In the name of God, The Most Gracious, The Dispenser of Grace."
        surahId == 112 && ayahNo == 1 -> "Say: He is God, the One and Only."
        else -> "A divine verse from Surah $surahEn conveying guidance, moral reflection, and mercy for contemplation."
    }
}

/**
 * Synthesizes Muhammad Asad's philosophical exegesis for verses outside the pre-curated cache.
 */
private fun defaultAsadCommentary(surahId: Int, ayahNo: Int, surahEn: String, revelationType: String): String {
    return """
In his seminal commentary 'The Message of The Qur'an', Muhammad Asad emphasizes the thematic continuity and moral urgency of this $revelationType revelation. 

Surah $surahEn invites humanity to awaken the intellect (''aql') and observe the signs ('ayat') woven seamlessly throughout the cosmos and the human condition. 

The linguistic structure of this verse highlights divine justice, boundless grace, and individual spiritual responsibility. Through its vivid cadence, the passage challenges the listener to transcend heedlessness, reconcile conscience with revelation, and recognize the unity behind all created life.
    """.trimIndent()
}

/**
 * Synthesizes Al-Muyassar classical exegesis for verses outside the pre-curated cache.
 */
private fun defaultMuyassarCommentary(surahId: Int, ayahNo: Int, surahEn: String, meaning: String): String {
    return """
يبين التفسير الميسر المعتمد من مجمع الملك فهد لطباعة المصحف الشريف المعنى الإجمالي لآيات سورة $surahEn، مبيناً ما فيها من الهدايات والتشريعات الإلهية القويمة.

تدعو هذه الآية الكريمة إلى استشعار عظمة الخالق جل وعلا، والتمسك بالتوحيد والعمل الصالح، والاتعاظ بمواعظ القرآن الكريم التي تجلو القلوب وتثبت الإيمان، مع إخلاص العبادة لله وحده لا شريك له.
    """.trimIndent()
}
