package com.ghais.ui.util

// UAX #9 FIRST STRONG ISOLATE / POP DIRECTIONAL ISOLATE (rules N1/I2).
private const val BIDI_FSI = "\u2068"
private const val BIDI_PDI = "\u2069"

/**
 * Wraps [text] in invisible Unicode directional isolates so a neighbouring
 * Latin digit/number run cannot be reordered out of (or in front of) the
 * Arabic run under UAX#9 in an LTR base direction.
 */
fun bidiIsolate(text: String): String = BIDI_FSI + text + BIDI_PDI
