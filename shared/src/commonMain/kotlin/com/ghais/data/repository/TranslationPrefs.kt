package com.ghais.data.repository

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persisted choice of translation edition (alquran.cloud identifier) for
 * the hifz player and any other surface that renders ayah translations.
 *
 * Default [DEFAULT_EDITION] matches the only edition bundled in
 * [QuranAyahRepository.BUNDLED_SEEDS], so first-launch users keep the
 * exact translation they have today. Switching persists immediately and
 * flips [selectedEdition]; the repository invalidates its caches in
 * response (see `QuranAyahRepository.onTranslationChanged`).
 */
object TranslationPrefs {

    const val DEFAULT_EDITION = "en.sahih"

    private const val KEY_EDITION = "hifz_translation_edition"

    private val settings: Settings by lazy { Settings() }

    private val _selectedEdition = MutableStateFlow(load())
    val selectedEdition: StateFlow<String> = _selectedEdition.asStateFlow()

    fun setEdition(editionId: String) {
        // Trim before validating: a stray space from hand-edited storage or a
        // deep link would otherwise be persisted and then go onto the wire
        // verbatim, producing a 404 on
        // `…/editions/quran-uthmani,%20en.sahih%20`. Ids are catalog slugs
        // (`en.sahih`, `tr.yazir`), so surrounding whitespace is never
        // meaningful and is dropped here rather than at every read site.
        val id = editionId.trim()
        if (id.isBlank() || id == _selectedEdition.value) return
        _selectedEdition.value = id
        try {
            settings.putString(KEY_EDITION, id)
        } catch (_: Exception) {
        }
    }

    private fun load(): String = try {
        settings.getString(KEY_EDITION, DEFAULT_EDITION).ifBlank { DEFAULT_EDITION }
    } catch (_: Exception) {
        DEFAULT_EDITION
    }
}
