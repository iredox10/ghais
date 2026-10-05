package com.ghais.data.repository

import com.ghais.data.seed.QuranData
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.callContext
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class QuranAyahRepositoryTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // ------------------------------------------------------------------
    // Singleton normalisation
    //
    // QuranAyahRepository is an `object`: the in-memory cache, the remembered
    // surah list and the injected HTTP client all outlive a single test, so
    // the edition one test leaves behind would decide whether the next one
    // sees bundled seeds, fetched text or placeholders. Normalise through the
    // same API the app calls on a translation switch.
    // ------------------------------------------------------------------

    /**
     * Restores the repository to the state a first-launch user of the default
     * edition would have: default edition selected, bundled `en.sahih` seeds in
     * the cache, default-edition persisted text re-read — and a client that
     * cannot answer, so nothing a previous test kicked off can refill the cache
     * from the network while this test runs.
     */
    @BeforeTest
    fun setUpQuranAyahRepository() {
        QuranAyahRepository.configureHttpClient(deadClient)
        TranslationPrefs.setEdition(TranslationPrefs.DEFAULT_EDITION)
        QuranAyahRepository.onTranslationChanged()
        awaitBackgroundFetches()
    }

    /**
     * Leaves the singleton pristine for whatever runs next: default edition,
     * freshly re-seeded cache and, last of all, no injected client. The client
     * is only released once the refetches this reset launched have settled —
     * otherwise a straggler would resolve its URL against the real default
     * client and hit api.alquran.cloud from a unit test.
     */
    @AfterTest
    fun tearDownQuranAyahRepository() {
        TranslationPrefs.setEdition(TranslationPrefs.DEFAULT_EDITION)
        QuranAyahRepository.configureHttpClient(deadClient)
        QuranAyahRepository.onTranslationChanged()
        awaitBackgroundFetches()
        QuranAyahRepository.configureHttpClient(null)
    }

    @Test
    fun testAyahVerseDataClassAndDefaults() {
        val verse = AyahVerse(
            surahId = 1,
            ayahNo = 1,
            textUthmani = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
            translation = "In the name of Allah, the Entirely Merciful, the Especially Merciful."
        )

        assertEquals(1, verse.surahId)
        assertEquals(1, verse.ayahNo)
        assertEquals("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", verse.textUthmani)
        assertEquals("In the name of Allah, the Entirely Merciful, the Especially Merciful.", verse.translation)
        assertEquals("", verse.transliteration, "Transliteration should default to empty string")
        assertEquals("", AyahVerse(2, 2, "text").translation, "Translation should default to empty string")
    }

    @Test
    fun testAyahVerseSerialization() {
        val verse = AyahVerse(
            surahId = 112,
            ayahNo = 1,
            textUthmani = "قُلْ هُوَ ٱللَّهُ أَحَدٌ",
            translation = "Say, \"He is Allah, [who is] One,\"",
            transliteration = "Qul Huwallāhu Aḥad"
        )

        val encoded = json.encodeToString(verse)
        val decoded = json.decodeFromString<AyahVerse>(encoded)

        assertEquals(verse, decoded)
    }

    @Test
    fun testBundledSeedsPresentWithCorrectVerseOneTexts() {
        // Surah 1 verse 1 IS the basmalah; full 7-ayah seed is bundled.
        val fatihah1 = QuranAyahRepository.getAyahImmediate(1, 1)
        assertEquals(1, fatihah1.surahId)
        assertEquals(1, fatihah1.ayahNo)
        assertEquals("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", fatihah1.textUthmani)
        val fatihah7 = QuranAyahRepository.getAyahImmediate(1, 7)
        assertEquals(7, fatihah7.ayahNo)
        assertTrue(fatihah7.textUthmani.isNotBlank())
        assertFalse(fatihah7.textUthmani.contains("آية رقم"))

        // Surah 112 (4 ayahs bundled).
        val ikhlas1 = QuranAyahRepository.getAyahImmediate(112, 1)
        assertEquals("قُلْ هُوَ ٱللَّهُ أَحَدٌ", ikhlas1.textUthmani)
        val ikhlas4 = QuranAyahRepository.getAyahImmediate(112, 4)
        assertEquals(4, ikhlas4.ayahNo)
        assertFalse(ikhlas4.textUthmani.contains("آية رقم"))

        // Surah 113 (5 ayahs bundled).
        val falaq1 = QuranAyahRepository.getAyahImmediate(113, 1)
        assertEquals("قُلْ أَعُوذُ بِرَبِّ ٱلْفَلَقِ", falaq1.textUthmani)
        val falaq5 = QuranAyahRepository.getAyahImmediate(113, 5)
        assertEquals(5, falaq5.ayahNo)
        assertFalse(falaq5.textUthmani.contains("آية رقم"))

        // Surah 114 (6 ayahs bundled).
        val nas1 = QuranAyahRepository.getAyahImmediate(114, 1)
        assertEquals("قُلْ أَعُوذُ بِرَبِّ ٱلنَّاسِ", nas1.textUthmani)
        val nas6 = QuranAyahRepository.getAyahImmediate(114, 6)
        assertEquals(6, nas6.ayahNo)
        assertFalse(nas6.textUthmani.contains("آية رقم"))

        // Surah 108 (3 ayahs bundled).
        val kawthar1 = QuranAyahRepository.getAyahImmediate(108, 1)
        assertEquals("إِنَّآ أَعْطَيْنَٰكَ ٱلْكَوْثَرَ", kawthar1.textUthmani)
        val kawthar3 = QuranAyahRepository.getAyahImmediate(108, 3)
        assertEquals(3, kawthar3.ayahNo)
        assertFalse(kawthar3.textUthmani.contains("آية رقم"))

        // Surah 18 is a partial seed; verse 1 has NO basmalah prefix.
        val kahf1 = QuranAyahRepository.getAyahImmediate(18, 1)
        assertEquals(18, kahf1.surahId)
        assertEquals(1, kahf1.ayahNo)
        assertTrue(kahf1.textUthmani.startsWith("ٱلْحَمْدُ لِلَّهِ"))
        assertFalse(kahf1.textUthmani.contains("بِسْمِ"))
        // Seeded through ayah 5; ayah 6+ falls back to a placeholder.
        val kahf5 = QuranAyahRepository.getAyahImmediate(18, 5)
        assertEquals(5, kahf5.ayahNo)
        assertFalse(kahf5.textUthmani.contains("آية رقم"))
        val kahf6 = QuranAyahRepository.getAyahImmediate(18, 6)
        assertEquals(6, kahf6.ayahNo)
        assertTrue(kahf6.textUthmani.contains("آية رقم"))
    }

    @Test
    fun testGetAyahImmediatePlaceholderShape() {
        // Never null: unseeded surahs return a clean baseline placeholder.
        val baqarah2: AyahVerse = QuranAyahRepository.getAyahImmediate(2, 2)
        assertEquals(2, baqarah2.surahId)
        assertEquals(2, baqarah2.ayahNo)
        assertTrue(baqarah2.textUthmani.contains("آية رقم"))
        assertTrue(baqarah2.textUthmani.contains("2"))
    }

    @Test
    fun testCleanQuranicText() {
        // Strips U+25CC (dotted circle), U+200B (zero-width space), U+FEFF (BOM).
        assertEquals("ab", QuranAyahRepository.cleanQuranicText("a\u25CCb"))
        assertEquals("ab", QuranAyahRepository.cleanQuranicText("a\u200Bb"))
        assertEquals("ab", QuranAyahRepository.cleanQuranicText("\uFEFFab"))
        // Collapses repeated spaces and trims.
        assertEquals("a b", QuranAyahRepository.cleanQuranicText("a  b"))
        assertEquals("a b", QuranAyahRepository.cleanQuranicText("  a b  "))
        assertEquals("a b", QuranAyahRepository.cleanQuranicText("\uFEFF  a\u200B   b\u25CC  "))
        // Rejoins hamza splits (lam-sukun + hamza, hamza + alef).
        assertEquals("لْء", QuranAyahRepository.cleanQuranicText("لْ ء"))
        assertEquals("لْء", QuranAyahRepository.cleanQuranicText("لْ  ء"))
        assertEquals("ءا", QuranAyahRepository.cleanQuranicText("ء ا"))
        // Strips spaces before orphan combining marks, keeps word spaces.
        assertEquals("نَصْرًا", QuranAyahRepository.cleanQuranicText("نَصْر ًا"))
        assertEquals("a b c", QuranAyahRepository.cleanQuranicText("a b c"))
    }

    @Test
    fun testCleanQuranicTextStopsRenderingUthmaniMarksAsFilledDiscs() {
        // U+06DF / U+06E3 / U+06EB are the hamza-less + pause annotation marks.
        // QCF_Hafs draws all three as the same 1255x1255 filled disc inside a
        // dashed ring, which reads as a stray circle dropped into the verse, so
        // they are stripped. None of them carries recitation meaning.
        assertEquals("ءَامَنُوا", QuranAyahRepository.cleanQuranicText("ءَامَنُوا\u06DF"))
        assertEquals("نَصْر", QuranAyahRepository.cleanQuranicText("نَصْر\u06E3"))
        assertEquals("مَاء", QuranAyahRepository.cleanQuranicText("مَاء\u06EB"))
        // Stripping must not weld two words together, and must not eat the
        // neighbouring word space.
        assertEquals("وَ مَا", QuranAyahRepository.cleanQuranicText("وَ\u06DF مَا"))
        // U+06DE (rub el hizb) and U+06E9 (sajdah) are meaningful ornaments in
        // this font and must survive.
        assertTrue(QuranAyahRepository.cleanQuranicText("۞بِسْمِ").contains('\u06DE'))
        assertTrue(QuranAyahRepository.cleanQuranicText("عِندَ\u06E9 رَبِّهِۦ").contains('\u06E9'))
    }

    @Test
    fun testHasBasmalahHeader() {
        assertTrue(QuranAyahRepository.BASMALAH.isNotBlank())
        assertFalse(QuranAyahRepository.hasBasmalahHeader(1))
        assertFalse(QuranAyahRepository.hasBasmalahHeader(9))
        assertTrue(QuranAyahRepository.hasBasmalahHeader(2))
        assertTrue(QuranAyahRepository.hasBasmalahHeader(18))
        assertTrue(QuranAyahRepository.hasBasmalahHeader(114))
    }

    @Test
    fun testSanitizeVerseTextStripBehavior() {
        val withPrefix = "بسم الله الرحمن الرحيم الْحَمْدُ لِلَّهِ"
        val stripped = QuranAyahRepository.sanitizeVerseText(2, 1, withPrefix)
        assertTrue(stripped.contains("الْحَمْدُ"))
        assertFalse(stripped.contains("بسم"))
        // Surah 1 keeps its verse-1 text untouched.
        assertEquals(withPrefix, QuranAyahRepository.sanitizeVerseText(1, 1, withPrefix))
        // Non-first ayahs are untouched.
        assertEquals(withPrefix, QuranAyahRepository.sanitizeVerseText(2, 2, withPrefix))
    }

    @Test
    fun testCacheGenNonDecreasingAfterGetAyahsForSurah() = runBlocking {
        val before = QuranAyahRepository.cacheGen.value
        // Seeded surah: served from cache, no network needed (offline-safe).
        val verses = QuranAyahRepository.getAyahsForSurah(112)
        assertTrue(verses.isNotEmpty())
        assertTrue(QuranAyahRepository.cacheGen.value >= before)
        // Prefetch smoke test (fire-and-forget, must not throw).
        QuranAyahRepository.prefetchSurah(112)
    }

    /**
     * The Uthmani spelling of the basmalah uses alef wasla (U+0671) on every
     * elided `ال`, which is the exact text the API returns for a surah like
     * 112 whose first ayah it opens with a basmalah. Compared literally against
     * `bare` it never matches, so the prefix survived and the player recited it
     * a second time on top of the ayah audio. Regression test for that.
     */
    @Test
    fun testStripBasmalahHandlesAlefWasla() {
        val uthmani = "\u0628\u0650\u0633\u0652\u0645\u0650 \u0671\u0644\u0652\u0644\u0651\u064e\u0647\u0650 " +
            "\u0671\u0644\u0652\u0631\u0651\u064e\u062d\u0652\u0645\u064e\u0646\u0650\u0650 " +
            "\u0671\u0644\u0652\u0631\u0651\u064e\u062d\u0650\u064a\u0652\u0645\u0650 \u0642\u064f\u0644\u0652"
        val stripped = QuranAyahRepository.stripBasmalahPrefix(112, 1, uthmani)
        assertFalse(stripped.contains("\u0628\u0650\u0633\u0652\u0645\u0650"), "basmalah survived: $stripped")
        assertTrue(stripped.trimStart().startsWith("\u0642\u064f\u0644\u0652"), "verse text lost: $stripped")
    }

    /** Wasla normalisation must not turn a non-basmalah opening into a match. */
    @Test
    fun testStripBasmalahLeavesUnrelatedOpeningsAlone() {
        val alifLamp = "\u0627\u0644\u0652\u0645\u0652"
        assertEquals(alifLamp, QuranAyahRepository.stripBasmalahPrefix(2, 1, alifLamp))
    }

    // ------------------------------------------------------------------
    // Edition awareness
    // ------------------------------------------------------------------

    /**
     * The request URL must be built from the edition selected **at call time**.
     * Nothing between the two switches below invalidates a cache or notifies
     * the repository — there is deliberately no shadow copy of the edition in
     * [QuranAyahRepository] — so only a live read of
     * [TranslationPrefs.selectedEdition] inside the fetch can put two different
     * editions on the wire in the same test.
     */
    @Test
    fun testFetchUrlUsesEditionSelectedAtCallTime() = runBlocking {
        val engine = FakeAlQuranEngine(failing = false)
        QuranAyahRepository.configureHttpClient(HttpClient(engine))

        TranslationPrefs.setEdition("fr.hamidullah")
        val french = QuranAyahRepository.getAyahsForSurah(110)

        TranslationPrefs.setEdition(TranslationPrefs.DEFAULT_EDITION)
        val english = QuranAyahRepository.getAyahsForSurah(109)

        val frenchUrls = engine.urls.filter { "/surah/110/" in it }
        assertTrue(frenchUrls.isNotEmpty(), "no request went out for surah 110: ${engine.urls}")
        assertTrue(
            frenchUrls.all { "editions/quran-uthmani,fr.hamidullah" in it },
            "surah 110 was requested with an edition other than the selected one: $frenchUrls"
        )
        val englishUrls = engine.urls.filter { "/surah/109/" in it }
        assertTrue(englishUrls.isNotEmpty(), "no request went out for surah 109: ${engine.urls}")
        assertTrue(
            englishUrls.all { "editions/quran-uthmani,en.sahih" in it },
            "surah 109 was requested with an edition other than the selected one: $englishUrls"
        )

        assertEquals(3, french.size, "surah 110 has 3 ayahs")
        assertEquals("[fr.hamidullah] translation 1", french[0].translation)
        assertEquals(6, english.size, "surah 109 has 6 ayahs")
        assertEquals("[en.sahih] translation 1", english[0].translation)
    }

    /**
     * The bundled seed text is `en.sahih` and may only surface while that
     * edition is active: a reader who switched to French must get
     * placeholders (and then a download), never someone else's translation.
     * Switching back to the default edition has to bring the offline text
     * with it, which is what keeps a first-launch user readable offline.
     */
    @Test
    fun testBundledSeedsAreSkippedOutsideTheDefaultEdition() = runBlocking {
        // A client that cannot answer: any text observed below therefore
        // either came from the bundle or from generated placeholders.
        QuranAyahRepository.configureHttpClient(deadClient)
        TranslationPrefs.setEdition("fr.hamidullah")
        QuranAyahRepository.onTranslationChanged()

        val placeholder = QuranAyahRepository.getAyahImmediate(112, 1)
        assertTrue(placeholder.isPlaceholder, "bundled verse leaked into another edition: $placeholder")

        val offlineFrench = QuranAyahRepository.getAyahsForSurah(112)
        assertEquals(4, offlineFrench.size, "surah 112 has 4 ayahs")
        assertTrue(
            offlineFrench.all { it.isPlaceholder },
            "en.sahih text was served for fr.hamidullah: $offlineFrench"
        )

        TranslationPrefs.setEdition(TranslationPrefs.DEFAULT_EDITION)
        QuranAyahRepository.onTranslationChanged()

        val seeded = QuranAyahRepository.getAyahImmediate(112, 1)
        assertFalse(seeded.isPlaceholder, "bundled seed did not come back for the default edition")
        // Compare against the sanitizer's own output instead of a hand-typed
        // literal: shadda and fatha can be typed in either order and still
        // render identically, so a byte-exact expectation here would only test
        // the test author, never the seed pipeline.
        assertEquals(
            QuranAyahRepository.sanitizeVerseText(112, 1, "قُلْ هُوَ ٱللَّهُ أَحَدٌ"),
            seeded.textUthmani,
        )
    }

    /**
     * One translation's download must never overwrite another's on disk.
     * Surah 113 is fetched and persisted for `fr.hamidullah`, then fetched for
     * `id.indonesian`, and finally `fr.hamidullah` is re-selected with a client
     * that cannot answer — so the text in that last step can only have been
     * read back out of the French blob written in the first, proving the
     * Indonesian download did not clobber it.
     */
    @Test
    fun testPersistedTextIsIsolatedPerEdition() = runBlocking {
        val engine = FakeAlQuranEngine(failing = false)
        QuranAyahRepository.configureHttpClient(HttpClient(engine))

        TranslationPrefs.setEdition("fr.hamidullah")
        QuranAyahRepository.onTranslationChanged()
        val french = QuranAyahRepository.getAyahsForSurah(113)
        assertEquals(5, french.size, "surah 113 has 5 ayahs")
        assertEquals("[fr.hamidullah] translation 1", french[0].translation)
        QuranAyahRepository.prefetchSurah(113)
        awaitBackgroundFetches()

        TranslationPrefs.setEdition("id.indonesian")
        QuranAyahRepository.onTranslationChanged()
        awaitBackgroundFetches()
        val indonesian = QuranAyahRepository.getAyahsForSurah(113)
        assertEquals("[id.indonesian] translation 1", indonesian[0].translation)

        // From here nothing can be fetched, so a complete verse can only be a
        // disk restore.
        QuranAyahRepository.configureHttpClient(deadClient)
        TranslationPrefs.setEdition("fr.hamidullah")
        QuranAyahRepository.onTranslationChanged()
        awaitBackgroundFetches()

        val restored = QuranAyahRepository.getAyahImmediate(113, 1)
        assertFalse(restored.isPlaceholder, "fr.hamidullah text was not restored from disk: $restored")
        assertEquals("[fr.hamidullah] translation 1", restored.translation)
    }

    /**
     * Screens collect [QuranAyahRepository.cacheGen] to know when to re-resolve
     * the ayah they are showing. A translation switch that failed to bump it
     * would leave the previous edition's text on screen until something else
     * happened to invalidate the view.
     */
    @Test
    fun testOnTranslationChangedBumpsCacheGen() {
        val before = QuranAyahRepository.cacheGen.value
        QuranAyahRepository.onTranslationChanged()
        assertTrue(
            QuranAyahRepository.cacheGen.value > before,
            "cacheGen did not advance: $before -> ${QuranAyahRepository.cacheGen.value}"
        )
    }
}

/**
 * Offline client used by the setup/teardown reset and by the test that must
 * prove a verse came from disk. It replies with a body that carries no `data`,
 * which is exactly how an unreachable api.alquran.cloud presents itself to
 * `fetchSurahFromApi`, and it never opens a socket.
 */
private val deadClient: HttpClient by lazy { HttpClient(FakeAlQuranEngine(failing = true)) }

/**
 * Lets the repository's fire-and-forget work settle. Both
 * [QuranAyahRepository.onTranslationChanged] and [QuranAyahRepository.prefetchSurah]
 * launch refetches/persists on the repository's own scope, outside any handle
 * a test can join, and a straggler landing after an assertion would make the
 * result depend on scheduling. Those launches run against a zero-latency fake
 * engine, so polling [QuranAyahRepository.loadingSurahs] until it has stayed
 * empty for a handful of samples is a bounded wait rather than a blind sleep —
 * and it keeps a leftover fetch from resolving its URL against the real client.
 */
private fun awaitBackgroundFetches() {
    runBlocking {
        var idleSamples = 0
        var waitedSamples = 0
        while (idleSamples < 4 && waitedSamples < 40) {
            delay(25)
            waitedSamples++
            idleSamples = if (QuranAyahRepository.loadingSurahs.value.isEmpty()) {
                idleSamples + 1
            } else {
                0
            }
        }
    }
}

/**
 * In-process stand-in for `https://api.alquran.cloud`.
 *
 * With [failing] it answers with a body that has no `data`, so the repository
 * falls through to its offline path. Otherwise it returns a complete
 * `quran-uthmani` + translation pair whose translation text embeds the edition
 * taken from the request URL, letting a test tell which edition a response
 * really belonged to. Every URL is recorded because the request line is the
 * only observable proof that the edition was read at call time instead of
 * being remembered from an earlier request.
 */
@OptIn(InternalAPI::class)
private class FakeAlQuranEngine(
    private val failing: Boolean
) : HttpClientEngineBase("fake-alquran") {

    override val config: HttpClientEngineConfig = HttpClientEngineConfig()

    private val recordedUrls = MutableStateFlow<List<String>>(emptyList())

    /** Requested URLs, in order, exactly as the repository spelled them. */
    val urls: List<String> get() = recordedUrls.value

    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val url = data.url.toString()
        recordedUrls.update { it + url }
        return HttpResponseData(
            HttpStatusCode.OK,
            GMTDate(),
            headersOf(HttpHeaders.ContentType, listOf("application/json; charset=utf-8")),
            HttpProtocolVersion.HTTP_1_1,
            ByteReadChannel(if (failing) "{}" else successBody(url)),
            callContext()
        )
    }

    private fun successBody(url: String): String {
        val surahId = url.substringAfter("/surah/").substringBefore("/").toIntOrNull() ?: return "{}"
        val edition = url.substringAfter("editions/quran-uthmani,", "")
        val ayahCount = QuranData.SURAHS.firstOrNull { it.id == surahId }?.ayahsCount ?: return "{}"
        val uthmaniAyahs = (1..ayahCount).joinToString(",") { n ->
            """{"numberInSurah":$n,"text":"نص $n"}"""
        }
        val translationAyahs = (1..ayahCount).joinToString(",") { n ->
            """{"numberInSurah":$n,"text":"[$edition] translation $n"}"""
        }
        return """{"code":200,"status":"OK","data":[""" +
            """{"number":$surahId,"ayahs":[$uthmaniAyahs]},""" +
            """{"number":$surahId,"ayahs":[$translationAyahs]}]}"""
    }
}
