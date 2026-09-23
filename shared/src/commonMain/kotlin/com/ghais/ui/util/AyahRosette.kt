package com.ghais.ui.util

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.ui.theme.GhaisNoir

/**
 * Enclosed ayah rosette: digits drawn INSIDE the ۝ ornament.
 *
 * Problem it fixes: appending " ۝N" as plain text renders as two detached
 * shapes (empty bubble + trailing digits) because the QCF_Hafs font has no
 * contextual shaping that pulls digits inside U+06DD.
 *
 * Two integration paths (pick per call site):
 *
 * 1. Single-Text contexts (inline flow):
 *      Text(
 *          text = ayahWithRosette(verse, ayahNo),
 *          inlineContent = ayahRosetteContent(),
 *          fontFamily = GhaisTypography.quranFont, ...
 *      )
 *    The digits travel as the placeholder's alternate text, so the shared
 *    [ayahRosetteContent] map (keyed by [AYAH_ROSETTE_ID]) can draw the
 *    per-instance number with zero extra params.
 *
 * 2. Row/FlowRow contexts (robust, preferred for new code):
 *      Row { Text(verse, ...); AyahEndMark(number = ayahNo) }
 *    Verse Text maxLines/wrapping must then account for the trailing mark.
 *
 * 3. Annotated-string-only contexts (mushaf keeps strings):
 *    [ayahSignString] — plain-text fallback, same detached rendering as
 *    today, kept so string pipelines keep compiling.
 */
const val AYAH_ROSETTE_ID = "ayah_rosette"

/**
 * Appends " " + an [AYAH_ROSETTE_ID] inline-content placeholder for [number].
 *
 * No digit glyphs are appended as visible text — the digits are carried as
 * the placeholder's alternate text ([toArabicDigits]) and drawn inside the
 * ornament by [ayahRosetteContent]. Alternate text also doubles as the
 * accessibility label.
 */
fun ayahWithRosette(text: String, number: Int): AnnotatedString =
    buildAnnotatedString {
        append(text)
        append(" ")
        appendInlineContent(AYAH_ROSETTE_ID, toArabicDigits(number))
    }

/**
 * Shared inline-content map for [ayahWithRosette] output.
 *
 * The per-instance number arrives via the placeholder's alternate text
 * (see [ayahWithRosette]), which this draws centered inside the ornament.
 * A legacy "[rosette]" alternate text draws the ring with no digits.
 */
@Composable
fun ayahRosetteContent(
    ornamentSize: TextUnit = 22.sp,
    digitSize: TextUnit = 10.sp,
): Map<String, InlineTextContent> {
    val tint = GhaisNoir.TextPrimary
    return mapOf(
        AYAH_ROSETTE_ID to InlineTextContent(
            placeholder = Placeholder(
                width = ornamentSize,
                height = ornamentSize,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
            )
        ) { alternateText ->
            RosetteBox(
                digits = if (alternateText == "[rosette]") "" else alternateText,
                ornamentSize = ornamentSize,
                digitSize = digitSize,
                tint = tint,
            )
        }
    )
}

/**
 * Standalone end-of-ayah mark for Row/FlowRow placement after a verse Text.
 *
 * Fixed-size [Box] ([ornamentSize] square) drawing the ring with Canvas
 * [drawCircle] (stroke 1.5.dp, [tint]) with the Arabic-Indic digits centered
 * inside via [Alignment.Center].
 */
@Composable
fun AyahEndMark(
    number: Int,
    ornamentSize: TextUnit = 24.sp,
    digitSize: TextUnit = 11.sp,
    tint: Color = GhaisNoir.TextPrimary,
) {
    RosetteBox(
        digits = toArabicDigits(number),
        ornamentSize = ornamentSize,
        digitSize = digitSize,
        tint = tint,
    )
}

/**
 * Plain-string fallback for annotated-string-only contexts (mushaf).
 * Same detached rendering as the legacy call sites; prefer [AyahEndMark] or
 * [ayahWithRosette] + [ayahRosetteContent] for the true enclosed rosette.
 */
fun ayahSignString(number: Int) = " ۝${toArabicDigits(number)}"

@Composable
private fun RosetteBox(
    digits: String,
    ornamentSize: TextUnit,
    digitSize: TextUnit,
    tint: Color,
) {
    val boxSize = with(LocalDensity.current) { ornamentSize.toDp() }
    val strokeWidth = 1.5.dp
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(boxSize),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            drawCircle(
                color = tint,
                radius = (size.minDimension - strokePx) / 2f,
                style = Stroke(width = strokePx),
            )
        }
        if (digits.isNotEmpty()) {
            Text(
                text = digits,
                fontSize = digitSize,
                fontWeight = FontWeight.Normal,
                color = tint,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}
