package com.ghais.ui.util

/**
 * Converts western integer digits to classic Eastern Arabic numerals
 * (١, ٢, ٣...) for ayah rosettes, counters and labels.
 */
fun toArabicDigits(number: Int): String {
    val digits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    return number.toString().map { digits[it - '0'] }.joinToString("")
}
