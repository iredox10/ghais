package com.ghais.ui.screens.search

import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.seed.QuranData
import com.ghais.domain.model.Reciter
import com.ghais.domain.model.Surah

data class AyahReference(val surahId: Int, val ayahNo: Int)

data class SearchResults(
    val query: String,
    val surahs: List<Surah> = emptyList(),
    val reciters: List<Reciter> = emptyList(),
    val ayahReference: AyahReference? = null
)

object SearchEngine {
    private val famousAyahAliases = mapOf(
        "ayat al-kursi" to AyahReference(2, 255),
        "ayat al kursi" to AyahReference(2, 255),
        "ayatal kursi" to AyahReference(2, 255),
        "amanar rasul" to AyahReference(2, 285),
        "amanar-rasul" to AyahReference(2, 285),
    )

    private val referenceRegex = Regex("""^(\d{1,3})\s*[:\-]\s*(\d{1,3})$""")

    fun search(query: String, reciters: List<Reciter> = QuranDataRepository.getBrowseReciters()): SearchResults {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return SearchResults(query)
        
        val lowerQuery = trimmed.lowercase()
        
        val ayahRef = parseAyahReference(lowerQuery)
        
        val surahs = QuranData.SURAHS.filter {
            it.nameEn.lowercase().contains(lowerQuery) ||
            it.nameAr.contains(trimmed) ||
            it.transliteration.lowercase().contains(lowerQuery) ||
            it.meaning.lowercase().contains(lowerQuery) ||
            it.id.toString() == trimmed
        }
        
        val matchedReciters = reciters.filter {
            it.nameEn.lowercase().contains(lowerQuery) ||
            it.nameAr.contains(trimmed) ||
            it.slug.lowercase().contains(lowerQuery)
        }
        
        return SearchResults(
            query = query,
            surahs = surahs,
            reciters = matchedReciters,
            ayahReference = ayahRef
        )
    }
    
    private fun parseAyahReference(query: String): AyahReference? {
        val match = referenceRegex.find(query)
        if (match != null) {
            val sId = match.groupValues[1].toIntOrNull()
            val aId = match.groupValues[2].toIntOrNull()
            if (sId != null && aId != null && sId in 1..114) {
                val surah = QuranData.SURAHS.find { it.id == sId }
                if (surah != null && aId in 1..surah.ayahsCount) {
                    return AyahReference(sId, aId)
                }
            }
        }
        for ((alias, ref) in famousAyahAliases) {
            if (query.contains(alias)) {
                return ref
            }
        }
        return famousAyahAliases[query]
    }
}
