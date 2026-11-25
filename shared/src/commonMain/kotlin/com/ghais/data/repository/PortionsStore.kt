package com.ghais.data.repository

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class QuranPortion(
    val id: String,
    val name: String,
    val surahIds: List<Int>,
    val reciterSlug: String? = null,
    val isCustom: Boolean = false,
) {
    val subtitle: String
        get() = when {
            surahIds.isEmpty() -> ""
            surahIds.size == 1 -> {
                val id = surahIds.first()
                val name = QuranDataRepository.getSurahById(id)?.nameEn
                if (name != null) "Surah $id • $name" else "Surah $id"
            }
            surahIds == (surahIds.first()..surahIds.last()).toList() ->
                "Surahs ${surahIds.first()}–${surahIds.last()} (${surahIds.size})"
            else -> "${surahIds.size} surahs"
        }
}

val POPULAR_PORTIONS: List<QuranPortion> = listOf(
    QuranPortion(
        id = "popular-juz-amma",
        name = "Juz Amma",
        surahIds = (78..114).toList(),
    ),
    QuranPortion(
        id = "popular-juz-tabarak",
        name = "Juz Tabarak",
        surahIds = (67..77).toList(),
    ),
    QuranPortion(
        id = "popular-al-kahf",
        name = "Al-Kahf (Friday)",
        surahIds = listOf(18),
    ),
    QuranPortion(
        id = "popular-night-sunnah",
        name = "As-Sajdah & Al-Mulk",
        surahIds = listOf(32, 67),
    ),
    QuranPortion(
        id = "popular-yasin-rahman",
        name = "Yasin & Ar-Rahman",
        surahIds = listOf(36, 55),
    ),
    QuranPortion(
        id = "popular-al-baqarah",
        name = "Al-Baqarah",
        surahIds = listOf(2),
    ),
)

object PortionsStore {
    private const val KEY_CUSTOM_PORTIONS = "ghais_custom_portions"
    private val settings: Settings by lazy { Settings() }
    private val json = Json { ignoreUnknownKeys = true }

    private val _customPortions = MutableStateFlow(load())
    val customPortions: StateFlow<List<QuranPortion>> = _customPortions.asStateFlow()

    private fun load(): List<QuranPortion> = try {
        val raw = settings.getStringOrNull(KEY_CUSTOM_PORTIONS) ?: return emptyList()
        json.decodeFromString<List<QuranPortion>>(raw).map { it.copy(isCustom = true) }
    } catch (_: Exception) {
        emptyList()
    }

    private fun persist(list: List<QuranPortion>) {
        try {
            settings.putString(KEY_CUSTOM_PORTIONS, json.encodeToString(list))
        } catch (_: Exception) {
        }
    }

    fun addRange(name: String, fromSurah: Int, toSurah: Int, reciterSlug: String? = null) {
        val start = fromSurah.coerceIn(1, 114)
        val end = toSurah.coerceIn(start, 114)
        val cleanName = name.trim().ifBlank {
            if (start == end) "Surah $start" else "Surahs $start–$end"
        }
        val portion = QuranPortion(
            id = "custom-${kotlin.time.Clock.System.now().toEpochMilliseconds()}",
            name = cleanName,
            surahIds = (start..end).toList(),
            reciterSlug = reciterSlug?.takeIf { it.isNotBlank() },
            isCustom = true,
        )
        val updated = listOf(portion) + _customPortions.value
        _customPortions.value = updated
        persist(updated)
    }

    fun remove(id: String) {
        val updated = _customPortions.value.filterNot { it.id == id }
        if (updated.size != _customPortions.value.size) {
            _customPortions.value = updated
            persist(updated)
        }
    }
}
