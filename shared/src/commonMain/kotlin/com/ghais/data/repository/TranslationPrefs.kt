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
        if (editionId.isBlank() || editionId == _selectedEdition.value) return
        _selectedEdition.value = editionId
        try {
            settings.putString(KEY_EDITION, editionId)
        } catch (_: Exception) {
        }
    }

    private fun load(): String = try {
        settings.getString(KEY_EDITION, DEFAULT_EDITION).ifBlank { DEFAULT_EDITION }
    } catch (_: Exception) {
        DEFAULT_EDITION
    }
}
