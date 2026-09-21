package com.ghais.data.share

/**
 * iOS stub: no `UIActivityViewController` binding yet, so shares are dropped
 * and [isAvailable] reports false (callers fall back to clipboard copy).
 */
actual object ShareSheet {

    actual fun shareText(title: String, text: String) {
        // No-op until a UIActivityViewController binding lands.
    }

    actual fun isAvailable(): Boolean = false
}
