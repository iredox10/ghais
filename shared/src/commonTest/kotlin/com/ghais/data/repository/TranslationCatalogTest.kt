package com.ghais.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the offline translation catalog the hifz picker is built on.
 *
 * The seed data behind [TranslationCatalog] is generated from a cached
 * `edition/type/translation` alquran.cloud response, so these tests are the
 * tripwire that (re)generation kept the contract the UI relies on:
 *
 * 1. Completeness: exactly the 117 text editions — the six audio
 *    "translation" editions and the `quran-buck` transliteration never leak
 *    in, and `en.sahih` (the default English pick) is always bundled.
 * 2. Search: case-insensitive across id/title/language/country, an empty
 *    query returns everything, "urdu" maps exactly onto the `ur.*` editions,
 *    and "pakistan" spans multiple languages (Urdu + Sindhi) grouped under
 *    Pakistan.
 * 3. Grouping: country groups partition the whole catalog (no row lost or
 *    duplicated), keys come out alphabetically, no group is empty.
 * 4. Renderability: every row carries non-blank label/flag metadata so a
 *    picker row can never render a blank string.
 *
 * Everything here is pure in-memory data: no network, no platform state.
 */
class TranslationCatalogTest {

    /**
     * Exact size of the seed: 124 API editions minus the six audio
     * editions and `quran-buck`. Pinned so a regeneration that silently
     * drops or duplicates rows fails loudly.
     */
    private val expectedCount = 117

    /** Languages represented by those 117 editions (47 codes incl. `bs`/`ba`). */
    private val expectedLanguageCount = 47

    /** Countries the rows group under once metadata is applied. */
    private val expectedCountryCount = 40

    /** Editions that must never appear: audio rows + a transliteration. */
    private val excludedIds = listOf(
        "ar.abdulbasitmurattal",
        "ar.minshawi",
        "ar.minshawimujawwad",
        "ar.minshawi-2",
        "fa.hedayatfarfooladvand",
        "tr.vakfi-audio",
        "quran-buck",
    )

    // --- 1. Completeness ---

    @Test
    fun testCatalogContainsExactlyTheBundledTextEditions() {
        val all = TranslationCatalog.all()
        assertEquals(expectedCount, all.size)
        assertEquals(expectedCount, all.map { it.id }.distinct().size)
    }

    @Test
    fun testAudioAndTransliterationEditionsAreExcluded() {
        val ids = TranslationCatalog.all().map { it.id }.toSet()
        excludedIds.forEach { id ->
            assertTrue(id !in ids, "$id must not be bundled in the catalog")
        }
    }

    @Test
    fun testEnglishSahihEditionIsAlwaysPresent() {
        assertEquals("English", TranslationCatalog.byId("en.sahih")?.languageName)
        assertEquals("United States", TranslationCatalog.byId("en.sahih")?.countryName)
    }

    @Test
    fun testUnknownEditionIdReturnsNull() {
        assertTrue(TranslationCatalog.byId("xx.doesnotexist") == null)
    }

    // --- 2. Search ---

    @Test
    fun testSearchByAuthorFindsDiyanet() {
        val results = TranslationCatalog.search("diyanet")
        assertTrue(results.any { it.id == "tr.diyanet" })
    }

    @Test
    fun testSearchByLanguageNameReturnsEveryUrduEdition() {
        val expected = TranslationCatalog.all().filter { it.id.startsWith("ur.") }
        val actual = TranslationCatalog.search("urdu")
        assertEquals(8, expected.size)
        assertEquals(expected, actual)
    }

    @Test
    fun testSearchByCountrySpansMultipleLanguagesOfThatCountry() {
        val results = TranslationCatalog.search("pakistan")
        assertEquals(9, results.size)
        // Urdu (8) + Sindhi (1): several languages, one grouping country.
        assertEquals(setOf("ur", "sd"), results.map { it.languageCode }.toSet())
        assertTrue(results.all { it.countryName == "Pakistan" })
    }

    @Test
    fun testSearchIsCaseInsensitive() {
        assertTrue(TranslationCatalog.search("urdu").isNotEmpty())
        assertEquals(TranslationCatalog.search("urdu"), TranslationCatalog.search("URDU"))
        assertEquals(TranslationCatalog.search("diyanet"), TranslationCatalog.search("Diyanet"))
        assertEquals(
            TranslationCatalog.search("pakistan"),
            TranslationCatalog.search("PAKISTAN"),
        )
    }

    @Test
    fun testEmptyQueryReturnsTheFullCatalog() {
        assertEquals(TranslationCatalog.all(), TranslationCatalog.search(""))
        assertEquals(TranslationCatalog.all(), TranslationCatalog.search("   "))
    }

    // --- 3. Grouping ---

    @Test
    fun testCountryGroupsPartitionTheWholeCatalog() {
        val grouped = TranslationCatalog.groupedByCountry()
        val all = TranslationCatalog.all()
        assertEquals(all.size, grouped.values.sumOf { it.size })
        assertEquals(
            all.map { it.id }.toSet(),
            grouped.values.flatten().map { it.id }.toSet(),
        )
        assertEquals(expectedCountryCount, grouped.size)
        assertTrue(grouped.values.all { it.isNotEmpty() })
        grouped.forEach { (country, items) ->
            assertTrue(items.all { it.countryName == country }, "group $country misfiled")
        }
    }

    @Test
    fun testCountryKeysComeOutAlphabetically() {
        val keys = TranslationCatalog.groupedByCountry().keys.toList()
        assertEquals(keys.sorted(), keys)
    }

    // --- 4. Renderable metadata ---

    @Test
    fun testEveryItemCarriesNonBlankDisplayMetadata() {
        TranslationCatalog.all().forEach { item ->
            assertTrue(item.id.isNotBlank(), "${item.id}: blank id")
            assertTrue(item.languageName.isNotBlank(), "${item.id}: blank languageName")
            assertTrue(
                item.nativeLanguageName.isNotBlank(),
                "${item.id}: blank nativeLanguageName",
            )
            assertTrue(item.countryName.isNotBlank(), "${item.id}: blank countryName")
            assertTrue(item.countryFlag.isNotBlank(), "${item.id}: blank countryFlag")
        }
    }

    @Test
    fun testEveryLanguageHasCuratedMetadata() {
        val languages = TranslationCatalog.all().map { it.languageCode }.distinct()
        assertEquals(expectedLanguageCount, languages.size)
    }
}
