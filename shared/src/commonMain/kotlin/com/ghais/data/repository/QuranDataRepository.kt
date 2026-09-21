package com.ghais.data.repository

import com.ghais.data.seed.QuranData
import com.ghais.domain.model.Reciter
import com.ghais.domain.model.Surah

/**
 * Repository providing access to Quranic data such as reciters and surahs.
 */
object QuranDataRepository {

    /**
     * Look up a reciter by slug with smart normalization and fallback to Mishary Rashid Alafasy.
     */
    fun getReciterBySlug(slug: String?): Reciter {
        if (slug.isNullOrBlank()) {
            return getFallbackReciter()
        }
        val cleanSlug = slug.trim().lowercase()
        return QuranData.RECITERS.find { reciter ->
            val rSlug = reciter.slug.lowercase()
            rSlug == cleanSlug ||
            (cleanSlug == "mishary" && rSlug == "alafasy") ||
            (cleanSlug == "al-sudais" && rSlug == "sudais") ||
            (cleanSlug == "al-muaiqly" && rSlug == "muaiqly") ||
            (cleanSlug == "al-dossari" && rSlug == "dossari") ||
            (cleanSlug == "abdul-basit" && rSlug.startsWith("abdulbaset")) ||
            (cleanSlug == "abdulbasit" && rSlug.startsWith("abdulbaset")) ||
            (cleanSlug == "shuraim" && rSlug == "shuraym") ||
            (cleanSlug == "islam-sobhi" && rSlug.contains("islam")) ||
            rSlug.replace("_", "-") == cleanSlug ||
            rSlug.replace("-", "_") == cleanSlug ||
            reciter.nameEn.lowercase().contains(cleanSlug)
        } ?: getFallbackReciter()
    }

    /**
     * Fallback reciter: Mishary Rashid Alafasy.
     */
    fun getFallbackReciter(): Reciter {
        return QuranData.RECITERS.firstOrNull { it.slug == "alafasy" }
            ?: QuranData.RECITERS.first()
    }

    /**
     * Returns all reciters.
     */
    fun getReciters(): List<Reciter> = QuranData.RECITERS

    /**
     * Returns all surahs.
     */
    fun getSurahs(): List<Surah> = QuranData.SURAHS

    /**
     * Returns a surah by its 1-indexed ID.
     */
    fun getSurahById(id: Int): Surah? = QuranData.SURAHS.find { it.id == id }

    /**
     * Returns the list of surahs recorded and available for this reciter.
     */
    fun getSurahsForReciter(reciter: Reciter): List<Surah> {
        val availableIds = reciter.getAvailableSurahIds()
        return QuranData.SURAHS.filter { availableIds.contains(it.id) }
    }
}
