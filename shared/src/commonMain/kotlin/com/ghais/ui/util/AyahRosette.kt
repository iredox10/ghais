package com.ghais.ui.util

import androidx.compose.ui.text.AnnotatedString

/**
 * End-of-ayah rosette for the bundled `QCF_Hafs` font (family
 * `KFGQPC HAFS Uthmanic Script`, declared as
 * [com.ghais.ui.theme.GhaisTypography.quranFont]).
 *
 * The ornament is the font's own: an `rlig` ligature turns the Arabic-Indic
 * digits U+0660..U+0669 — exactly what [toArabicDigits] emits — into a single
 * composite glyph that contains the ayah-number ring with the digits already
 * hand-placed inside it. All 286 ayah numbers resolve to the correct ring this
 * way, and 286 is Al-Baqarah's last ayah, so coverage spans the whole Quran.
 * The ring is an ordinary text glyph, so it flows and wraps with the verse and
 * inherits the text colour — no `InlineTextContent`, no `Placeholder`, no
 * baseline offset and no extra line height are involved.
 *
 * TRAP — never emit U+06DD (ARABIC END OF AYAH). It is present in the font's
 * cmap as a *bare empty ring* and referenced by zero GSUB/GPOS rules, so
 * "U+06DD + digits" shapes to TWO glyphs: a correctly numbered ring followed
 * by a second, empty one. Always append the digits alone.
 *
 * Three further rules the shaping depends on:
 * - Use U+0660..U+0669 only. The extended-Arabic U+06F0..U+06F9 digits take
 *   no part in the ligature and render as loose digits with no ring.
 * - Keep the digits CONTIGUOUS. A space or ZWJ inside the number splits it
 *   into several separately numbered rings.
 * - [number] must be 1..286. The font only carries the digit-ligature for
 *   those (Al-Baqarah's last ayah is 286, so the whole Quran is covered);
 *   287+ shape to bare digits with NO ring and degrade silently.
 *
 * Usage — append inside a `buildAnnotatedString` whose `Text` is styled with
 * [com.ghais.ui.theme.GhaisTypography.quranFont] and an RTL layout direction:
 * ```
 * Text(
 *     text = buildAnnotatedString {
 *         append(ayah.textUthmani)
 *         appendGluedRosette(ayah.ayahNumber)
 *     },
 *     fontFamily = GhaisTypography.quranFont,
 *     ...
 * )
 * ```
 */
fun AnnotatedString.Builder.appendGluedRosette(number: Int) {
    // NBSP glues the rosette to the preceding word so continuous mushaf flow
    // can never strand the mark alone on the next line; the trailing plain
    // space then gives the NEXT ayah a break opportunity so a new verse can
    // start a fresh line. Both sit outside the digit run, so the ligature
    // still forms — do not "tidy" either space away.
    append("\u00A0")
    append(toArabicDigits(number))
    append(" ")
}
