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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisTypography

/**
 * Enclosed end-of-ayah mark: the ayah number drawn INSIDE a ring.
 *
 * TRAP — do not "simplify" this back to appending the number as plain text in
 * the Quran font. `QCF_Hafs` has an `rlig` ligature that turns any run of
 * U+0660..U+0669 into its own ayah ornament, and that ornament is only
 * correct for runs of TWO or THREE digits. A single digit resolves to the
 * `_770`.._778` glyphs, which are broken in this font: they draw the ring but
 * collapse the numeral to an unreadable sliver, so every ayah 1-9 renders as
 * a bare empty circle. Verified against the shipped `QCF_Hafs.ttf`:
 * `hb-shape` of U+0661 yields `_770`, and that glyph renders as a ring with
 * only a thin mark inside, while U+0661 U+0662 yields `a001_a002` and draws
 * both digits legibly. Padding to `U+0660 U+0661` does not help, because
 * `a000` is not a valid first glyph in that ligature lookup. Drawing the ring
 * ourselves is the only way to make 1-286 consistent.
 *
 * The digits therefore use [GhaisTypography.rosetteDigitFont] rather than
 * [GhaisTypography.quranFont] — see that property for why.
 *
 * Usage — inside a `Text` whose style is [GhaisTypography.quranScript], pass
 * the content map from [ayahMarkContent]:
 * ```
 * Text(
 *     text = buildAnnotatedString {
 *         append(ayah.textUthmani)
 *         appendAyahMark(ayah.ayahNumber)
 *     },
 *     style = GhaisTypography.quranScript,
 *     inlineContent = ayahMarkContent(),
 * )
 * ```
 */
const val AYAH_ROSETTE_ID = "ayahRosette"

/**
 * Appends a non-breaking space, the rosette placeholder for [number], and a
 * trailing space.
 *
 * The NBSP glues the mark to the preceding word so continuous mushaf flow can
 * never strand it alone on the next line; the trailing plain space gives the
 * next ayah a break opportunity so a verse can start a fresh line. The
 * alternate text carries the number, so it also doubles as the accessibility
 * label.
 */
fun AnnotatedString.Builder.appendAyahMark(number: Int) {
    append("\u00A0")
    appendInlineContent(AYAH_ROSETTE_ID, toArabicDigits(number))
    append(" ")
}

/**
 * Shared `inlineContent` map for [appendAyahMark] output.
 *
 * Size both [ringSize] and [digitSize] to the surrounding verse: they are
 * absolute `TextUnit`s reserved by the placeholder, not multiples of the
 * `Text`'s own `fontSize`.
 */
@Composable
fun ayahMarkContent(
    ringSize: TextUnit = 19.sp,
    digitSize: TextUnit = 9.sp,
    tint: Color = GhaisNoir.TextPrimary,
): Map<String, InlineTextContent> = mapOf(
    AYAH_ROSETTE_ID to InlineTextContent(
        placeholder = Placeholder(
            width = ringSize,
            height = ringSize,
            placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
        )
    ) { alternateText ->
        AyahRosette(
            digits = alternateText,
            ringSize = ringSize,
            digitSize = digitSize,
            tint = tint,
        )
    }
)

/**
 * Standalone end-of-ayah mark for `Row`/`FlowRow` placement after a verse
 * `Text`, for surfaces that are not a single flowing paragraph.
 */
@Composable
fun AyahEndMark(
    number: Int,
    ringSize: TextUnit = 22.sp,
    digitSize: TextUnit = 10.sp,
    tint: Color = GhaisNoir.TextPrimary,
) {
    AyahRosette(
        digits = toArabicDigits(number),
        ringSize = ringSize,
        digitSize = digitSize,
        tint = tint,
    )
}

@Composable
private fun AyahRosette(
    digits: String,
    ringSize: TextUnit,
    digitSize: TextUnit,
    tint: Color,
) {
    val boxSize = with(LocalDensity.current) { ringSize.toDp() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(boxSize),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 1.1.dp.toPx()
            val outer = (size.minDimension - strokePx) / 2f
            drawCircle(color = tint, radius = outer, style = Stroke(width = strokePx))
            drawCircle(
                color = tint,
                radius = outer * 0.78f,
                style = Stroke(width = strokePx * 0.7f),
            )
        }
        if (digits.isNotEmpty()) {
            Text(
                text = digits,
                fontFamily = GhaisTypography.rosetteDigitFont,
                fontSize = digitSize,
                fontWeight = FontWeight.Normal,
                color = tint,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}
