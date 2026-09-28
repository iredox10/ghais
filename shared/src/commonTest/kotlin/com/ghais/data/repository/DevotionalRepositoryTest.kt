package com.ghais.data.repository

import com.ghais.data.seed.DevotionalData
import com.ghais.data.seed.EveryAyahReciters
import com.ghais.domain.model.DEVOTIONAL_TRACK_SLUG
import com.ghais.domain.model.DevotionKind
import com.ghais.domain.model.DevotionalItem
import com.ghais.domain.model.ayahSpan
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** `U+06DF`, `U+06E3`, `U+06EB` — the three marks QCF_Hafs draws as a filled disc. */
private val DISCS = listOf("\u06DF", "\u06E3", "\u06EB")

/** Tashkeel-free prefix of the basmalah, for a cheap `startsWith` check. */
private const val BASMALAH_LEAD = "بسم الله الرحمن الرحيم"

/**
 * Guards the devotional layer's four load-bearing contracts:
 *
 * 1. Seed integrity. Ids are globally unique ACROSS both catalogues, so `byId`
 *    is a real deep link. This is a regression test for a shipped bug: the old
 *    seed reused `dua-1` for five different supplications, so every deep link
 *    to those five resolved to whichever row came first and four of them were
 *    unreachable. Ids are now `dua-<slug>-<n>` / `ruq-<surah>-<start>-<end>`.
 * 2. Audio resolution is byte-identical to a Mushaf ayah row, because it goes
 *    through the one existing builder, `Reciter.getAyahAudioUrl`.
 * 3. A devotional never enters the Quran ayah pipeline: `surahId == 0` and
 *    `ayahNo == 0` on every adapted `TrackItem`.
 * 4. Nothing is silently dropped: `playableTracks` keeps every entry and
 *    expands a multi-part item (a multi-ayah ruqiyah passage, the three-clip
 *    after-wudu dua) into one playable track per part.
 *
 * Everything here is offline: the catalogue is a compile-time constant and
 * every repository function is synchronous and side-effect free.
 */
class DevotionalRepositoryTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val everyItem: List<DevotionalItem>
        get() = DevotionalRepository.all(DevotionKind.DUA) + DevotionalRepository.all(DevotionKind.RUQIYAH)

    // ---------------------------------------------------------------------
    // 1. Seed integrity
    // ---------------------------------------------------------------------

    @Test
    fun testSeedIdsAreGloballyUniqueAcrossBothCatalogues() {
        // The real bug: `dua-1` was reused by five different supplications, so
        // `byId` (first match wins) could only ever reach one of them and an id
        // was a display key rather than a deep link. Duplicates must fail here
        // with the offending ids named, not surface as a confusing deep link.
        val collisions = everyItem
            .groupBy { it.id }
            .filterValues { it.size > 1 }
            .map { (id, rows) ->
                "$id used ${rows.size}x: " + rows.joinToString(" / ") { "${it.kind.wire} ${it.title}" }
            }
            .sorted()
        assertEquals(emptyList<String>(), collisions, "Devotional ids must be globally unique")
    }

    @Test
    fun testSeedIdsAreNonBlankAndLiveInDisjointNamespaces() {
        // `byId` searches both catalogues at once, so the two prefixes must not
        // collide and no id may be blank (a blank id would trim to "" and be
        // unreachable, since `byId("")` returns null by contract).
        for (item in everyItem) {
            assertTrue(item.id.isNotBlank(), "blank id on ${item.title}")
            assertEquals(item.id, item.id.trim(), "padded id on ${item.title}")
        }
        assertTrue(
            DevotionalRepository.all(DevotionKind.DUA).all { it.id.startsWith("dua-") },
            "dua ids should carry the dua- prefix",
        )
        assertTrue(
            DevotionalRepository.all(DevotionKind.RUQIYAH).all { it.id.startsWith("ruq-") },
            "ruqiyah ids should carry the ruq- prefix",
        )
    }

    @Test
    fun testSeedCountsAndKindTagsMatchTheDocumentedCatalogue() {
        // 97 duas, 20 ruqiyah — the numbers the repository doc quotes.
        assertEquals(97, DevotionalRepository.all(DevotionKind.DUA).size)
        assertEquals(20, DevotionalRepository.all(DevotionKind.RUQIYAH).size)
        assertTrue(DevotionalRepository.all(DevotionKind.DUA).all { it.kind == DevotionKind.DUA })
        assertTrue(DevotionalRepository.all(DevotionKind.RUQIYAH).all { it.kind == DevotionKind.RUQIYAH })
    }

    @Test
    fun testEveryItemHasNonBlankTitleAndArabic() {
        // Title is the track label and the audio/reader heading; arabic is the
        // passage itself. A blank either renders an empty card.
        for (item in everyItem) {
            assertTrue(item.title.isNotBlank(), "blank title on ${item.id}")
            assertTrue(item.arabic.isNotBlank(), "blank arabic on ${item.id}")
            assertTrue(item.translation.isNotBlank(), "blank translation on ${item.id}")
        }
    }

    @Test
    fun testBlankReferenceIsToleratedEverywhere() {
        // Three seeded duas (Tasbih / Tahmid / Takbir) ship `reference = ""`.
        // A blank citation is a display choice, not a broken row: it must still
        // round-trip through byId, resolve audio, and be searchable by title.
        val blankReference = everyItem.filter { it.reference.isBlank() }
        assertEquals(3, blankReference.size, "seed has exactly three citation-free adhkar")
        for (item in blankReference) {
            assertEquals(item, DevotionalRepository.byId(item.id))
            assertNotNull(DevotionalRepository.audioUrlFor(item), "${item.id} must resolve audio")
            assertTrue(DevotionalRepository.search(item.kind, item.title).any { it.id == item.id })
        }
    }

    @Test
    fun testDevotionalItemSurvivesSerializationRoundTrip() {
        val dua = assertNotNull(DevotionalRepository.byId("dua-daily-dua-1"))
        val ruqiyah = assertNotNull(DevotionalRepository.byId("ruq-3-190-191"))
        for (item in listOf(dua, ruqiyah)) {
            assertEquals(item, json.decodeFromString<DevotionalItem>(json.encodeToString(item)))
        }
    }

    @Test
    fun testDevotionKindWireRoundTripAndLabels() {
        for (kind in DevotionKind.entries) {
            assertEquals(kind, DevotionKind.fromWire(kind.wire))
            assertEquals(kind, DevotionKind.fromWire(kind.wire.uppercase()))
            assertTrue(kind.label.isNotBlank())
        }
        // Unknown wire degrades to DUA rather than throwing.
        assertEquals(DevotionKind.DUA, DevotionKind.fromWire("not-a-kind"))
    }

    // ---------------------------------------------------------------------
    // 2. byId
    // ---------------------------------------------------------------------

    @Test
    fun testByIdRoundTripsEveryItemFromBothKinds() {
        // With unique ids (locked above) `byId` is exact for the whole
        // catalogue, not just a sample: a single duplicated id fails here.
        assertEquals(117, everyItem.size)
        for (item in everyItem) {
            assertEquals(item, DevotionalRepository.byId(item.id), "byId must round-trip ${item.id}")
        }
    }

    @Test
    fun testByIdCrossesCatalogueNamespaces() {
        // The lookup is kind-agnostic on purpose: a deep link carries only an id.
        assertEquals(DevotionKind.DUA, DevotionalRepository.byId("dua-daily-dua-1")?.kind)
        assertEquals(DevotionKind.RUQIYAH, DevotionalRepository.byId("ruq-93-3-3")?.kind)
    }

    @Test
    fun testByIdTrimsAndRejectsUnknownOrBlankIds() {
        val item = assertNotNull(DevotionalRepository.byId("dua-daily-dua-1"))
        assertEquals(item, DevotionalRepository.byId("  dua-daily-dua-1  "))
        assertNull(DevotionalRepository.byId(""))
        assertNull(DevotionalRepository.byId("   "))
        assertNull(DevotionalRepository.byId("dua-does-not-exist"))
        // The legacy colliding id must no longer resolve to a random supplication.
        assertNull(DevotionalRepository.byId("dua-1"))
    }

    // ---------------------------------------------------------------------
    // 3. categories
    // ---------------------------------------------------------------------

    @Test
    fun testCategoriesAreNonBlankDuplicateFreeAndInFirstAppearanceOrder() {
        for (kind in DevotionKind.entries) {
            val categories = DevotionalRepository.categories(kind)
            assertTrue(categories.isNotEmpty(), "$kind must expose at least one filter")
            assertEquals(categories.distinct(), categories, "$kind categories must not repeat")
            for (name in categories) {
                assertTrue(name.isNotBlank(), "$kind has a blank category")
                assertEquals(name, name.trim(), "$kind category is padded: '$name'")
            }
            // First-appearance order, so the filter row groups the way the
            // catalogue itself is grouped rather than alphabetically.
            val expected = DevotionalRepository.all(kind)
                .map { it.category.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
            assertEquals(expected, categories, "$kind category order drifted")
        }
    }

    @Test
    fun testCategoriesAreTheExpectedGroupsInTheExpectedOrder() {
        assertEquals(
            listOf("Daily Dua", "Morning", "Evening", "Selected", "After Prayer"),
            DevotionalRepository.categories(DevotionKind.DUA),
        )
        assertEquals(listOf("Reflection"), DevotionalRepository.categories(DevotionKind.RUQIYAH))
    }

    @Test
    fun testCategoriesAreStableAcrossRepeatedCalls() {
        // Backed by a lazy index; a recompute must not reshuffle the filter row.
        for (kind in DevotionKind.entries) {
            val first = DevotionalRepository.categories(kind)
            val second = DevotionalRepository.categories(kind)
            assertEquals(first, second)
            assertEquals(first, DevotionalRepository.categories(kind))
        }
    }

    // ---------------------------------------------------------------------
    // 4. byKindAndCategory
    // ---------------------------------------------------------------------

    @Test
    fun testByKindAndCategoryWithNullOrBlankCategoryReturnsEverything() {
        for (kind in DevotionKind.entries) {
            val all = DevotionalRepository.all(kind)
            assertEquals(all, DevotionalRepository.byKindAndCategory(kind, null))
            assertEquals(all, DevotionalRepository.byKindAndCategory(kind, ""))
            assertEquals(all, DevotionalRepository.byKindAndCategory(kind, "   "))
        }
    }

    @Test
    fun testByKindAndCategoryReturnsOnlyThatGroupsItems() {
        val morning = DevotionalRepository.byKindAndCategory(DevotionKind.DUA, "Morning")
        assertTrue(morning.isNotEmpty())
        assertTrue(morning.all { it.category == "Morning" })
        assertTrue(morning.all { it.kind == DevotionKind.DUA })
        // The single ruqiyah group is the whole ruqiyah catalogue.
        assertEquals(
            DevotionalRepository.all(DevotionKind.RUQIYAH),
            DevotionalRepository.byKindAndCategory(DevotionKind.RUQIYAH, "Reflection"),
        )
    }

    @Test
    fun testByKindAndCategoryIsCaseInsensitiveAndTrimmed() {
        val expected = DevotionalRepository.byKindAndCategory(DevotionKind.DUA, "Daily Dua")
        assertEquals(expected, DevotionalRepository.byKindAndCategory(DevotionKind.DUA, "daily dua"))
        assertEquals(expected, DevotionalRepository.byKindAndCategory(DevotionKind.DUA, "DAILY DUA"))
        assertEquals(expected, DevotionalRepository.byKindAndCategory(DevotionKind.DUA, "  Daily Dua  "))
        assertEquals(emptyList(), DevotionalRepository.byKindAndCategory(DevotionKind.DUA, "Nonexistent"))
    }

    @Test
    fun testByKindAndCategoryDoesNotLeakAcrossKinds() {
        // "Reflection" is a dua category name nowhere; asking duas for it must
        // not fall through to the ruqiyah rows.
        assertEquals(emptyList(), DevotionalRepository.byKindAndCategory(DevotionKind.DUA, "Reflection"))
        assertEquals(emptyList(), DevotionalRepository.byKindAndCategory(DevotionKind.RUQIYAH, "Morning"))
    }

    @Test
    fun testUnionOfEveryCategoryEqualsTheWholeCatalogue() {
        for (kind in DevotionKind.entries) {
            val union = DevotionalRepository.categories(kind)
                .flatMap { DevotionalRepository.byKindAndCategory(kind, it) }
            assertEquals(DevotionalRepository.all(kind), union, "$kind lost rows through the category filter")
        }
    }

    // ---------------------------------------------------------------------
    // 5. search
    // ---------------------------------------------------------------------

    @Test
    fun testSearchMatchesTitleCaseInsensitively() {
        val hits = DevotionalRepository.search(DevotionKind.DUA, "Supplication Before Sleeping")
        assertEquals(listOf("dua-daily-dua-1"), hits.map { it.id })
        // Latin fields are lowercased on both sides.
        assertEquals(hits, DevotionalRepository.search(DevotionKind.DUA, "supplication before sleeping"))
        assertEquals(hits, DevotionalRepository.search(DevotionKind.DUA, "SUPPLICATION BEFORE SLEEPING"))
        // Surrounding whitespace is trimmed before matching.
        assertEquals(hits, DevotionalRepository.search(DevotionKind.DUA, "  Supplication Before Sleeping  "))
    }

    @Test
    fun testSearchMatchesTranslationAndReference() {
        assertEquals(
            listOf("dua-daily-dua-1"),
            DevotionalRepository.search(DevotionKind.DUA, "i die and i live").map { it.id },
            "translation match",
        )
        assertEquals(
            listOf("dua-daily-dua-2"),
            DevotionalRepository.search(DevotionKind.DUA, "6327").map { it.id },
            "reference match",
        )
        // Ruqiyah is searchable the same way (title + citation).
        assertEquals(
            listOf("ruq-20-14-14"),
            DevotionalRepository.search(DevotionKind.RUQIYAH, "Taha").map { it.id },
        )
        assertEquals(
            listOf("ruq-103-1-3"),
            DevotionalRepository.search(DevotionKind.RUQIYAH, "Al-Asr 103:1-3").map { it.id },
        )
    }

    @Test
    fun testSearchMatchesVocalizedArabicSubstrings() {
        // The seed is fully vocalized, so the Arabic arm of the predicate is a
        // plain `contains` on the pasted characters and must hit. The query is
        // copied verbatim from the seed, diacritics AND combining-mark order
        // included — the seed writes shadda before fatha in "وَدَّعَكَ", which is
        // NOT the order an editor or a normaliser produces, so retyping this
        // literal by hand silently breaks the match.
        assertEquals(
            listOf("dua-daily-dua-1"),
            DevotionalRepository.search(DevotionKind.DUA, "أَمُوْتُ وَأَحْيَا").map { it.id },
            "vocalized arabic dua",
        )
        assertEquals(
            listOf("ruq-93-3-3"),
            DevotionalRepository.search(DevotionKind.RUQIYAH, "وَدَّعَكَ رَبُّكَ").map { it.id },
            "vocalized arabic ruqiyah",
        )
    }

    @Test
    fun testSearchDoesNotMatchUnvocalizedArabicKnownLimitation() {
        // KNOWN LIMITATION, not a bug in the test: the seed ships fully
        // vocalized Arabic and the Arabic arm of the search predicate is a
        // case-sensitive `contains` (mirroring SearchEngine). An unvocalized
        // query therefore cannot match a vocalized seed entry, so a real user
        // typing "بسم الله" on a Latin keyboard finds nothing. This test
        // documents the CURRENT behaviour so a future normalisation/harakat
        // stripping change has to be a deliberate, visible edit here.
        val unvocalized = DevotionalRepository.search(DevotionKind.DUA, "بسم الله")
        assertTrue(
            unvocalized.none { it.id == "dua-daily-dua-5" },
            "unvocalized 'بسم الله' does not reach the vocalized 'بِسْمِ اللَّهِ' entry",
        )
        // The vocalized form of the same phrase does hit it.
        assertTrue(
            DevotionalRepository.search(DevotionKind.DUA, "بِسْمِ اللَّهِ").any { it.id == "dua-daily-dua-5" },
        )
    }

    @Test
    fun testSearchReturnsEmptyForNonMatchingQuery() {
        assertEquals(emptyList(), DevotionalRepository.search(DevotionKind.DUA, "zzz-no-such-devotional-zzz"))
        assertEquals(emptyList(), DevotionalRepository.search(DevotionKind.RUQIYAH, "zzz-no-such-devotional-zzz"))
        // A dua query never reaches ruqiyah rows, and vice versa.
        val duaHits = DevotionalRepository.search(DevotionKind.DUA, "Al-Baqarah")
        assertTrue(duaHits.isNotEmpty(), "a dua cites Al-Baqarah")
        assertTrue(duaHits.none { it.id.startsWith("ruq-") }, "kind scope leaked")
        assertTrue(duaHits.none { it.id == "ruq-2-255-255" })
        assertEquals(emptyList(), DevotionalRepository.search(DevotionKind.RUQIYAH, "Supplication"))
    }

    @Test
    fun testSearchBlankQueryReturnsNothingRatherThanEverything() {
        // Documented behaviour: an empty search box reads as "no results", not
        // as "no filter", so a freshly opened box does not dump all 97 rows.
        for (kind in DevotionKind.entries) {
            assertEquals(emptyList(), DevotionalRepository.search(kind, ""))
            assertEquals(emptyList(), DevotionalRepository.search(kind, "   "))
        }
    }

    @Test
    fun testSearchRespectsLimitAndTruncatesFromTheFront() {
        val full = DevotionalRepository.search(DevotionKind.DUA, "Supplication", limit = 1000)
        assertTrue(full.size > 3, "query should be broad enough to test the cap")
        assertEquals(full.take(3), DevotionalRepository.search(DevotionKind.DUA, "Supplication", limit = 3))
        assertEquals(emptyList(), DevotionalRepository.search(DevotionKind.DUA, "Supplication", limit = 0))
        assertEquals(emptyList(), DevotionalRepository.search(DevotionKind.DUA, "Supplication", limit = -1))
    }

    // ---------------------------------------------------------------------
    // 6. audioUrlFor / ayahUrlsFor
    // ---------------------------------------------------------------------

    @Test
    fun testRuqiyahAudioFolderMatchesTheEveryAyahSeedRow() {
        // The repository resolves through the `mishary` EveryAyah row, so the
        // folder must be the one that row declares — not a private constant
        // that could drift from the Quran player's voice.
        val reciter = assertNotNull(EveryAyahReciters.findBySlug("mishary"))
        assertEquals(DevotionalData.DEFAULT_EVERAYAH_FOLDER, reciter.audioFolder)
    }

    @Test
    fun testSingleAyahRuqiyahResolvesToExactlyOneEveryAyahUrl() {
        val item = assertNotNull(DevotionalRepository.byId("ruq-2-255-255"))
        val urls = DevotionalRepository.ayahUrlsFor(item)
        assertEquals(listOf("https://everyayah.com/data/Alafasy_128kbps/002255.mp3"), urls)
        assertEquals(urls.single(), DevotionalRepository.audioUrlFor(item))
        assertTrue(urls.single().startsWith("https://everyayah.com/data/${DevotionalData.DEFAULT_EVERAYAH_FOLDER}/"))
    }

    @Test
    fun testResolvedUrlIsIdenticalToTheAppsOwnAyahBuilder() {
        // The important invariant: devotional audio is byte-identical to what
        // the Quran player builds for the same ayah, so a reciter swap or a
        // padding change in `Reciter.getAyahAudioUrl` cannot desync the two.
        val reciter = assertNotNull(EveryAyahReciters.findBySlug("mishary")).toReciter()
        for (item in DevotionalRepository.all(DevotionKind.RUQIYAH)) {
            val expected = (item.startAyah..item.endAyah).map { reciter.getAyahAudioUrl(item.surahId, it) }
            assertEquals(expected, DevotionalRepository.ayahUrlsFor(item), "url mismatch for ${item.id}")
        }
    }

    @Test
    fun testMultiAyahItemResolvesToOneUrlPerAyahInPassageOrder() {
        val item = assertNotNull(DevotionalRepository.byId("ruq-3-190-191"))
        assertEquals(2, item.ayahSpan)
        assertEquals(
            listOf(
                "https://everyayah.com/data/Alafasy_128kbps/003190.mp3",
                "https://everyayah.com/data/Alafasy_128kbps/003191.mp3",
            ),
            DevotionalRepository.ayahUrlsFor(item),
        )
        // 89:27-30 — a four-ayah passage.
        assertEquals(
            listOf("089027", "089028", "089029", "089030").map {
                "https://everyayah.com/data/Alafasy_128kbps/$it.mp3"
            },
            DevotionalRepository.ayahUrlsFor(assertNotNull(DevotionalRepository.byId("ruq-89-27-30"))),
        )
    }

    @Test
    fun testSurahAndAyahAreZeroPaddedToThreeDigits() {
        // 93:3 -> 093003 (both components < 100), 103:1 -> 103001 (surah > 99),
        // 112:1-4 -> 112001..112004. A padStart(2) regression would produce
        // 93003 and everyayah would 404.
        assertEquals(
            "https://everyayah.com/data/Alafasy_128kbps/093003.mp3",
            DevotionalRepository.audioUrlFor(assertNotNull(DevotionalRepository.byId("ruq-93-3-3"))),
        )
        assertEquals(
            listOf("112001", "112002", "112003", "112004").map {
                "https://everyayah.com/data/Alafasy_128kbps/$it.mp3"
            },
            DevotionalRepository.ayahUrlsFor(assertNotNull(DevotionalRepository.byId("ruq-112-1-4"))),
        )
        assertEquals(
            listOf("103001", "103002", "103003").map {
                "https://everyayah.com/data/Alafasy_128kbps/$it.mp3"
            },
            DevotionalRepository.ayahUrlsFor(assertNotNull(DevotionalRepository.byId("ruq-103-1-3"))),
        )
    }

    @Test
    fun testMultiAyahAudioUrlIsTheAyahUrlsJoinedByTheSeparator() {
        // Documented as a queue description, not a fetchable URL: the joined
        // form exists so passage order survives; the player gets real URLs
        // from `playableTracks`.
        val item = assertNotNull(DevotionalRepository.byId("ruq-3-190-191"))
        val joined = assertNotNull(DevotionalRepository.audioUrlFor(item))
        assertEquals(DevotionalRepository.ayahUrlsFor(item), joined.split(DevotionalRepository.MULTI_AUDIO_SEPARATOR))
        assertEquals("|", DevotionalRepository.MULTI_AUDIO_SEPARATOR)
    }

    @Test
    fun testEveryDuaResolvesToPlayableAudio() {
        // All 97 duas ship a verified clip (open IslamicAPI recordings,
        // HisnMuslim per-dua clips, everyayah for Quranic duas). A dua with no
        // resolvable audio would render a read-only row the catalogue no
        // longer intends.
        val duas = DevotionalRepository.all(DevotionKind.DUA)
        assertEquals(97, duas.size)
        for (item in duas) {
            val url = DevotionalRepository.audioUrlFor(item)
            assertNotNull(url, "dua ${item.id} must resolve audio")
            assertTrue(url.isNotBlank(), "dua ${item.id} resolved a blank URL")
            assertTrue(
                url.split(DevotionalRepository.MULTI_AUDIO_SEPARATOR).all {
                    it.startsWith("https://")
                },
                "dua ${item.id} resolved a non-https URL: $url"
            )
            assertEquals(emptyList(), DevotionalRepository.ayahUrlsFor(item))
        }
    }

    @Test
    fun testNonQuranicAndMalformedRangesResolveNothing() {
        // `isQuranic` needs BOTH surahId and startAyah; a half-filled row is
        // not silently half-playable.
        val surahOnly = DevotionalItem(
            id = "x", kind = DevotionKind.RUQIYAH, title = "x", arabic = "x",
            surahId = 2, startAyah = 0, endAyah = 0,
        )
        assertFalse(surahOnly.isQuranic)
        assertEquals(0, surahOnly.ayahSpan)
        assertNull(DevotionalRepository.audioUrlFor(surahOnly))
        assertEquals(emptyList(), DevotionalRepository.ayahUrlsFor(surahOnly))

        // A reversed range is coerced to a single ayah (startAyah) rather than
        // producing a negative-length list or a crash.
        val reversed = surahOnly.copy(startAyah = 5, endAyah = 2)
        assertTrue(reversed.isQuranic)
        assertEquals(1, reversed.ayahSpan)
        assertEquals(
            listOf("https://everyayah.com/data/Alafasy_128kbps/002005.mp3"),
            DevotionalRepository.ayahUrlsFor(reversed),
        )
    }

    @Test
    fun testExplicitAudioUrlWinsOverEveryAyahResolution() {
        // A set field takes priority over ayah resolution without a code
        // change — this is how every dua's clip is wired.
        val hosted = DevotionalItem(
            id = "dua-hosted", kind = DevotionKind.DUA, title = "Hosted", arabic = "x",
            audioUrl = "https://example.org/dua/hosted.mp3",
        )
        assertEquals("https://example.org/dua/hosted.mp3", DevotionalRepository.audioUrlFor(hosted))
        assertTrue(hosted.hasAudio)
        // A blank-but-present url is treated as absent, not as a real one.
        assertNull(DevotionalRepository.audioUrlFor(hosted.copy(audioUrl = "   ")))
        assertFalse(hosted.copy(audioUrl = null).hasAudio)
    }

    @Test
    fun testExplicitMultiPartUrlSplitsIntoPlayableParts() {
        // The after-wudu dhikr ships three clips joined with the separator;
        // `explicitUrls` must hand back three real URLs and never an empty one,
        // and a stray separator must not smuggle a blank URL into the queue.
        val item = assertNotNull(DevotionalRepository.byId("dua-daily-dua-11"))
        val parts = DevotionalRepository.explicitUrls(item)
        assertEquals(3, parts.size)
        assertTrue(parts.all { it.startsWith("https://") })
        val messy = item.copy(audioUrl = "https://a.example/1.mp3||  |https://a.example/2.mp3")
        assertEquals(
            listOf("https://a.example/1.mp3", "https://a.example/2.mp3"),
            DevotionalRepository.explicitUrls(messy)
        )
        assertEquals(emptyList(), DevotionalRepository.explicitUrls(item.copy(audioUrl = null)))
        assertEquals(emptyList(), DevotionalRepository.explicitUrls(item.copy(audioUrl = "   ")))
    }

    @Test
    fun testHasAudioReadsTheRawFieldSoRuqiyahIsFalseDespitePlayingAudio() {
        // `hasAudio` is the *raw field* flag: the ruqiyah seed ships
        // `audioUrl = null` and gets an everyayah URL built on read, so
        // `hasAudio` is false for all 20 while `audioUrlFor` is non-null.
        // Every call site in the UI keys off `audioUrlFor` for exactly this
        // reason; keying off `hasAudio` would mute all 20 ruqiyah rows.
        // Duas are the opposite: each ships its clip in the field itself.
        val ruqiyah = DevotionalRepository.all(DevotionKind.RUQIYAH)
        assertTrue(ruqiyah.none { it.hasAudio })
        assertTrue(ruqiyah.all { DevotionalRepository.audioUrlFor(it) != null })
        assertTrue(DevotionalRepository.all(DevotionKind.DUA).all { it.hasAudio })
    }

    @Test
    fun testEveryRuqiyahItemIsQuranicWithAConsistentSpan() {
        val items = DevotionalRepository.all(DevotionKind.RUQIYAH)
        // 16 single-ayah + spans 2, 4, 3, 4 = 29 queue rows.
        assertEquals(16, items.count { it.ayahSpan == 1 })
        for (item in items) {
            assertTrue(item.isQuranic, "${item.id} must carry Quran coordinates")
            assertTrue(item.surahId in 1..114, "${item.id} surahId out of range")
            assertTrue(item.startAyah in 1..286, "${item.id} startAyah out of range")
            assertTrue(item.endAyah >= item.startAyah, "${item.id} has a reversed range")
            assertEquals(item.endAyah - item.startAyah + 1, item.ayahSpan)
            assertEquals(DevotionalRepository.ayahUrlsFor(item).size, item.ayahSpan)
        }
    }

    @Test
    fun testDuasCarryNoQuranCoordinates() {
        // surahId/startAyah stay at 0 for every dua, which is what keeps them
        // out of ayah mode and out of every Quran-keyed index in the app.
        for (item in DevotionalRepository.all(DevotionKind.DUA)) {
            assertFalse(item.isQuranic, "${item.id} must not be Quranic")
            assertEquals(0, item.ayahSpan)
            assertEquals(0, item.surahId)
            assertEquals(0, item.startAyah)
            assertEquals(0, item.endAyah)
        }
    }

    // ---------------------------------------------------------------------
    // 7. asTrackItem
    // ---------------------------------------------------------------------

    @Test
    fun testAsTrackItemUsesTheDevotionalSentinelAndZeroesQuranCoordinates() {
        // Both zeroes are load-bearing: `AudioEngine` derives ayah mode from
        // `ayahNo > 0`, and `surahId == 0` keeps the row out of the
        // surah-keyed history / stats / favourite indexes.
        for (item in everyItem) {
            val track = item.asTrackItem()
            assertEquals(DEVOTIONAL_TRACK_SLUG, track.reciterSlug, "${item.id} escaped the devotional slug")
            assertEquals("__devotional", track.reciterSlug)
            assertEquals(0, track.surahId, "${item.id} must not claim a surah")
            assertEquals(0, track.ayahNo, "${item.id} must not claim an ayah")
            assertEquals(item.title, track.surahNameEn)
            assertEquals(item.arabic, track.textUthmani)
            assertEquals("", track.surahNameAr)
            assertEquals(item.durationMs, track.durationMs)
        }
    }

    @Test
    fun testAsTrackItemCarriesTheItemsOwnAudioUrlFieldNotTheResolvedOne() {
        // CURRENT BEHAVIOUR, worth naming because the doc comment reads
        // "audio URL passes through verbatim": `asTrackItem` copies the RAW
        // `audioUrl` field, it does not call the repository. A seeded ruqiyah
        // item has `audioUrl = null`, so its adapted row has a BLANK url; the
        // resolved everyayah url only reaches the queue through
        // `playableTracks`. Asserting the resolved value here would fail.
        for (item in everyItem) {
            assertEquals(item.audioUrl.orEmpty(), item.asTrackItem().audioUrl)
        }
        assertEquals("", DevotionalRepository.byId("ruq-93-3-3")!!.asTrackItem().audioUrl)

        val hosted = DevotionalItem(
            id = "dua-hosted", kind = DevotionKind.DUA, title = "Hosted", arabic = "x",
            reciterName = "Some Reciter", audioUrl = "https://example.org/dua/hosted.mp3", durationMs = 4200L,
        )
        val track = hosted.asTrackItem()
        assertEquals("https://example.org/dua/hosted.mp3", track.audioUrl)
        assertEquals("Some Reciter", track.reciterName)
        assertEquals(4200L, track.durationMs)
    }

    @Test
    fun testAsTrackItemFallsBackToTheKindLabelForBlankNames() {
        val blank = DevotionalItem(id = "x", kind = DevotionKind.RUQIYAH, title = "  ", arabic = "  ")
        val track = blank.asTrackItem()
        assertEquals(DevotionKind.RUQIYAH.label, track.surahNameEn)
        assertEquals(DevotionKind.RUQIYAH.label, track.reciterName)
        // Ruqiyah rows carry the seed's reciter name; duas ship no reciter name
        // and fall back to their collection label so a queue row is never
        // nameless.
        assertEquals(
            DevotionalData.RUQIYAH_RECITER,
            DevotionalRepository.byId("ruq-93-3-3")!!.asTrackItem().reciterName,
        )
        assertEquals(
            DevotionKind.DUA.label,
            DevotionalRepository.byId("dua-daily-dua-1")!!.asTrackItem().reciterName,
        )
    }

    // ---------------------------------------------------------------------
    // 8. playableTracks
    // ---------------------------------------------------------------------

    @Test
    fun testPlayableTracksDropsNothingAndCountsMatchTheCatalogue() {
        // Every entry keeps its rows: 86 single-clip duas, the after-wudu dua
        // as its 3 clips, the 11 Quranic duas as one row per ayah (2 + 4 + 5 +
        // 6 ayahs, three collections over), and the 20 ruqiyah entries as 29
        // ayah rows. A queue that silently dropped rows would be inexplicable.
        val duas = DevotionalRepository.all(DevotionKind.DUA)
        val duaTracks = DevotionalRepository.playableTracks(DevotionKind.DUA)
        assertEquals(136, duaTracks.size, "86 singles + 3 wudu clips + 47 Quranic ayah rows")
        assertTrue(duaTracks.all { it.audioUrl.isNotBlank() }, "every dua row is playable")
        assertTrue(duaTracks.all { it.reciterSlug == DEVOTIONAL_TRACK_SLUG })
        val wuduTracks = duaTracks.filter { it.surahNameEn == "Supplication After Performing Ablution" }
        assertEquals(3, wuduTracks.size)
        val ikhlasTracks = duaTracks.filter { it.surahNameEn == "Al-Ikhlas" }
        assertEquals(12, ikhlasTracks.size, "Al-Ikhlas x3 duas x4 ayahs")

        val ruqiyah = DevotionalRepository.all(DevotionKind.RUQIYAH)
        val ruqiyahTracks = DevotionalRepository.playableTracks(DevotionKind.RUQIYAH)
        assertEquals(29, ruqiyahTracks.size, "16 single-ayah + spans 2, 4, 3 and 4")
        assertTrue(ruqiyahTracks.size > ruqiyah.size, "multi-ayah items must expand")
        assertEquals(ruqiyah.sumOf { it.ayahSpan }, ruqiyahTracks.size)
        assertTrue(ruqiyahTracks.all { it.audioUrl.isNotBlank() }, "every ruqiyah track is playable")
        assertTrue(ruqiyahTracks.all { it.audioUrl.contains("everyayah.com") })
    }

    @Test
    fun testMultiAyahItemExpandsToOnePlayableTrackPerAyah() {
        val item = assertNotNull(DevotionalRepository.byId("ruq-3-190-191"))
        val tracks = DevotionalRepository.playableTracks(DevotionKind.RUQIYAH)
            .filter { it.surahNameEn == item.title && it.textUthmani == item.arabic }
        assertEquals(2, tracks.size)
        assertEquals(DevotionalRepository.ayahUrlsFor(item), tracks.map { it.audioUrl })
        // Every expanded track keeps the entry's identity and stays out of the
        // Quran ayah pipeline, so the player bridge can take a single URL each.
        for (track in tracks) {
            assertEquals(DEVOTIONAL_TRACK_SLUG, track.reciterSlug)
            assertEquals(0, track.surahId)
            assertEquals(0, track.ayahNo)
            assertEquals(item.arabic, track.textUthmani)
        }
    }

    @Test
    fun testPlayableTracksPreserveSeedOrderAndGroupEachItemContiguously() {
        // Queue slices are located arithmetically by id, so each item's tracks
        // must be one contiguous run in catalogue order.
        for (kind in DevotionKind.entries) {
            val expectedWidths = DevotionalRepository.all(kind).map { item ->
                maxOf(1, item.ayahSpan, DevotionalRepository.explicitUrls(item).size)
            }
            val tracks = DevotionalRepository.playableTracks(kind)
            var cursor = 0
            for ((index, width) in expectedWidths.withIndex()) {
                val run = tracks.drop(cursor).take(width)
                val item = DevotionalRepository.all(kind)[index]
                assertEquals(width, run.size, "${kind.wire} #$index (${item.id}) queue slice is short")
                assertTrue(
                    run.all {
                        it.reciterSlug == DEVOTIONAL_TRACK_SLUG &&
                            it.surahNameEn == item.title.ifBlank { item.kind.label } &&
                            it.textUthmani == item.arabic
                    },
                    "${kind.wire} #$index (${item.id}) lost its queue slice at $cursor",
                )
                cursor += width
            }
            assertEquals(tracks.size, cursor, "$kind queue has trailing rows")
        }
    }

    @Test
    fun testPlayableTracksAreDistinctPlayableUrlsInAyahOrder() {
        val tracks = DevotionalRepository.playableTracks(DevotionKind.RUQIYAH)
        val seen = mutableSetOf<String>()
        for (track in tracks) {
            // Two ruqiyah entries never share a passage, so a repeated URL
            // would mean the expansion dropped or duplicated an ayah.
            assertTrue(seen.add(track.audioUrl), "duplicate queue url ${track.audioUrl}")
        }
        assertEquals(29, seen.size)
    }

    /**
     * A Quranic passage must render through the same rule set as a Mushaf ayah.
     *
     * The ruqiyah seed is raw Uthmani from the same edition the Quran reader
     * fetches, so it carries `U+06DF` — the annotation mark `QCF_Hafs` draws as
     * a large filled disc dropped into the middle of a verse. Ten of the twenty
     * entries did, which is why `all(RUQIYAH)` now runs the text back through
     * `QuranAyahRepository.cleanQuranicText`.
     */
    @Test
    fun testRuqiyahArabicIsFreeOfFilledDiscAnnotationMarks() {
        val offenders = DevotionalRepository.all(DevotionKind.RUQIYAH)
            .filter { item ->
                DISCS.any { it in item.arabic }
            }
            .map { it.id }
        assertTrue(
            offenders.isEmpty(),
            "ruqiyah passages still carry a filled-disc mark: $offenders"
        )
    }

    /**
     * No passage may open with a basmalah the player is about to recite twice.
     *
     * `ruq-103-1-3` and `ruq-112-1-4` both span ayah 1, so the seed's leading
     * basmalah is dropped for every surah except 1 and 9.
     */
    @Test
    fun testRuqiyahPassagesDoNotRepeatTheBasmalah() {
        for (item in DevotionalRepository.all(DevotionKind.RUQIYAH)) {
            if (item.startAyah != 1) continue
            if (!QuranAyahRepository.hasBasmalahHeader(item.surahId)) continue
            assertFalse(
                item.arabic.trimStart().startsWith(BASMALAH_LEAD),
                "${item.id} opens with a basmalah it will recite twice"
            )
        }
    }

    /**
     * Every translation must be a whole thought, not a severed clause.
     *
     * The seed shipped fourteen of twenty ruqiyah translations cut mid-sentence
     * ("...the Sustainer of"), which is visible in the reader because the
     * translation is rendered in full under the Arabic. A translation ending on
     * one of these words was truncated at a token boundary.
     */
    @Test
    fun testNoTranslationEndsMidClause() {
        val dangling = listOf("of", "except", "or", "while", "your", "whose", "yours", "saying")
        val offenders = DevotionalRepository.all(DevotionKind.RUQIYAH)
            .filter { item ->
                val last = item.translation.trim().trimEnd('"', '.', '”')
                    .substringAfterLast(' ')
                last.lowercase() in dangling
            }
            .map { it.id to it.translation.takeLast(40) }
        assertTrue(offenders.isEmpty(), "truncated translations: $offenders")
    }

    @Test
    fun testEveryRuqiyahTranslationIsSubstantial() {
        for (item in DevotionalRepository.all(DevotionKind.RUQIYAH)) {
            assertTrue(
                item.translation.length > 40,
                "${item.id} translation is too short to be a whole passage: '${item.translation}'"
            )
        }
    }

    // ---------------------------------------------------------------------
    // 9. queue arithmetic (twin-safe index mapping)
    // ---------------------------------------------------------------------

    @Test
    fun testQueueArithmeticCoversEveryRowExactlyOnce() {
        // Start indexes are strictly increasing by stride, ranges tile the
        // queue with no gaps and no overlaps, and the inverse maps every row
        // back to its catalogue index.
        for (kind in DevotionKind.entries) {
            val items = DevotionalRepository.all(kind)
            val tracks = DevotionalRepository.playableTracks(kind)
            var cursor = 0
            for ((index, item) in items.withIndex()) {
                val width = DevotionalRepository.queueWidth(item)
                assertTrue(width >= 1, "${item.id} has a non-positive queue width")
                assertEquals(cursor, DevotionalRepository.queueStartIndex(kind, item))
                assertEquals(
                    cursor..(cursor + width - 1),
                    DevotionalRepository.queueRange(kind, item)
                )
                for (offset in 0 until width) {
                    assertEquals(
                        index,
                        DevotionalRepository.collectionIndexForQueueIndex(kind, cursor + offset),
                        "${item.id} row $offset maps back to #$index"
                    )
                }
                cursor += width
            }
            assertEquals(tracks.size, cursor, "$kind queue has trailing rows")
            assertEquals(-1, DevotionalRepository.collectionIndexForQueueIndex(kind, cursor))
            assertEquals(-1, DevotionalRepository.collectionIndexForQueueIndex(kind, -1))
        }
    }

    @Test
    fun testQueueArithmeticDistinguishesSameTitledTwins() {
        // Three "Al-Ikhlas" duas share title AND text, so content matching can
        // only ever find the first twin. Arithmetic must land each twin on its
        // own rows.
        val twins = DevotionalRepository.all(DevotionKind.DUA)
            .filter { it.title == "Al-Ikhlas" }
        assertEquals(3, twins.size)
        val starts = twins.map { DevotionalRepository.queueStartIndex(DevotionKind.DUA, it) }
        assertEquals(3, starts.toSet().size, "twins collapsed onto one slice: $starts")
        val tracks = DevotionalRepository.playableTracks(DevotionKind.DUA)
        for ((twin, first) in twins.zip(starts)) {
            val range = DevotionalRepository.queueRange(DevotionKind.DUA, twin)
            assertEquals(4, range.count(), "${twin.id} should own 4 ayah rows")
            assertTrue(
                tracks.subList(range.first, range.last + 1).all {
                    it.audioUrl.contains("11200")
                },
                "${twin.id} rows do not play Al-Ikhlas"
            )
        }
    }

    @Test
    fun testQueueArithmeticMissesUnknownItems() {
        val ghost = DevotionalItem(id = "ghost", kind = DevotionKind.DUA, title = "Ghost", arabic = "x")
        assertEquals(-1, DevotionalRepository.queueStartIndex(DevotionKind.DUA, ghost))
        assertTrue(DevotionalRepository.queueRange(DevotionKind.DUA, ghost).isEmpty())
    }
}
