package com.ghais.personal

import com.ghais.domain.model.Reciter
import com.ghais.ui.screens.reciters.taste.TasteProfile
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * AGENT P10 — guards the [ForYou] facade contract: empty-catalog short
 * circuits, failure fallbacks that return empty instead of throwing, and
 * [ForYou.topPicks] limit capping/ordering.
 *
 * Isolation mirrors `SignalsTest`: `SignalStore` is an `object` singleton
 * whose default `Settings()` backend throws on the unit-test classpath, so
 * persistence is a silent no-op and these tests assert in-memory transitions
 * only. Every test binds a provably fresh random owner and clears both the
 * signal log and the [ForYou] affinity cache; the [TasteStore] profile is
 * snapshotted, cleared, and restored so [ForYou.isPersonalized] reads a
 * known taste state without leaking into other suites.
 */
class ForYouTest {

    private fun newOwnerId(tag: String): String = "$tag-${Random.nextLong().toString(16)}"

    private fun reciter(slug: String): Reciter =
        Reciter(slug = slug, nameEn = slug, nameAr = "اختبار")

    private var tasteBefore: TasteProfile = TasteProfile()

    @BeforeTest
    fun setUp() {
        SignalStore.settingsOverride = null
        SignalStore.setOwner(newOwnerId("foryou-owner"))
        SignalStore.clear()
        ForYou.invalidateCache()
        tasteBefore = try {
            com.ghais.ui.screens.reciters.taste.TasteStore.taste.value
        } catch (_: Exception) {
            TasteProfile()
        }
        try {
            com.ghais.ui.screens.reciters.taste.TasteStore.clear()
        } catch (_: Exception) {
        }
    }

    @AfterTest
    fun tearDown() {
        SignalStore.settingsOverride = null
        SignalStore.clear()
        SignalStore.setOwner("local")
        ForYou.invalidateCache()
        try {
            com.ghais.ui.screens.reciters.taste.TasteStore.save(tasteBefore)
        } catch (_: Exception) {
        }
    }

    // --- Empty catalog ---

    @Test
    fun testEmptyCatalogAffinitiesIsEmpty() {
        assertEquals(emptyMap(), ForYou.affinities(emptyList()))
    }

    @Test
    fun testEmptyCatalogTopPicksIsEmpty() {
        assertEquals(emptyList(), ForYou.topPicks(emptyList(), 5))
    }

    // --- Failure fallbacks (empty, never throw) ---

    @Test
    fun testBlankSlugCatalogYieldsEmptyWithoutThrowing() {
        val catalog = listOf(reciter(""), reciter("   "))
        assertEquals(emptyMap(), ForYou.affinities(catalog))
        assertEquals(emptyList(), ForYou.topPicks(catalog, 3))
    }

    @Test
    fun testTopPicksNonPositiveLimitIsEmptyWithoutThrowing() {
        val catalog = listOf(reciter("alafasy"), reciter("husary"))
        assertEquals(emptyList(), ForYou.topPicks(catalog, 0))
        assertEquals(emptyList(), ForYou.topPicks(catalog, -2))
    }

    @Test
    fun testTopPicksHugeLimitIsCappedWithoutThrowing() {
        val catalog = listOf(reciter("alafasy"), reciter("husary"))
        SignalStore.recordPlayCompleted("alafasy")
        val picks = ForYou.topPicks(catalog, Int.MAX_VALUE)
        assertEquals(2, picks.size, "oversized limit must cap at catalog size, got $picks")
    }

    // --- Limit cap + ordering ---

    @Test
    fun testTopPicksLimitCapsAtRequestedCount() {
        val catalog = listOf(reciter("aaa"), reciter("bbb"), reciter("ccc"))
        SignalStore.recordPlayCompleted("aaa")
        SignalStore.recordPlayStarted("bbb")
        SignalStore.recordPreview("ccc")
        val picks = ForYou.topPicks(catalog, 2)
        assertEquals(2, picks.size)
    }

    @Test
    fun testTopPicksRanksHigherAffinityFirst() {
        // Same-day signals, so time decay is equal: PLAY_COMPLETED (3f)
        // outranks PLAY_STARTED (1f) regardless of input order.
        val catalog = listOf(reciter("bbb"), reciter("aaa"))
        SignalStore.recordPlayStarted("bbb")
        SignalStore.recordPlayCompleted("aaa")
        val picks = ForYou.topPicks(catalog, 2)
        assertEquals(listOf("aaa", "bbb"), picks.map { it.slug })
    }

    @Test
    fun testAffinitiesOnlyContainsCatalogSlugs() {
        SignalStore.recordPlayCompleted("in-catalog")
        SignalStore.recordPlayCompleted("elsewhere")
        val affinities = ForYou.affinities(listOf(reciter("in-catalog")))
        assertTrue("in-catalog" in affinities, "expected scored slug, got $affinities")
        assertTrue("elsewhere" !in affinities, "non-catalog slug must be filtered, got $affinities")
    }

    // --- Personalized gate ---

    @Test
    fun testColdStartIsNotPersonalized() {
        assertFalse(ForYou.isPersonalized(), "fresh owner, no signals, no quiz -> cold")
    }

    @Test
    fun testMoreThanThreeSignalsIsPersonalized() {
        SignalStore.recordPlayStarted("a")
        SignalStore.recordPlayStarted("b")
        SignalStore.recordPreview("c")
        assertFalse(ForYou.isPersonalized(), "exactly 3 signals must stay cold")
        SignalStore.recordSearchOpen("d")
        assertTrue(ForYou.isPersonalized(), "4 signals must count as personalized")
    }

    // --- Record fan-out (SignalStore + never throws) ---

    @Test
    fun testRecordPreviewAppendsPreviewSignal() {
        ForYou.recordPreview("alafasy")
        assertEquals(
            listOf(SignalKind.PREVIEW),
            SignalStore.signals.value.map { it.kind },
        )
    }

    @Test
    fun testRecordPlayRoutesOnCompletedFlag() {
        ForYou.recordPlay("a", completed = false)
        ForYou.recordPlay("b", completed = true)
        assertEquals(
            listOf(SignalKind.PLAY_STARTED, SignalKind.PLAY_COMPLETED),
            SignalStore.signals.value.map { it.kind },
        )
    }

    @Test
    fun testRecordSearchOpenAppendsSearchSignal() {
        ForYou.recordSearchOpen("husary")
        assertEquals(
            listOf(SignalKind.SEARCH_OPEN),
            SignalStore.signals.value.map { it.kind },
        )
    }

    @Test
    fun testRecordBlankSlugNeverThrowsAndStoresNothing() {
        ForYou.recordPlay("", completed = true)
        ForYou.recordPreview("   ")
        ForYou.recordSearchOpen("")
        assertTrue(SignalStore.signals.value.isEmpty(), "blank slugs must be dropped")
    }

    // --- Flags ---

    @Test
    fun testPersonalFlagsDefaultOnAndReset() {
        PersonalFlags.personalShelves = false
        PersonalFlags.tasteV2 = false
        PersonalFlags.weeklyRitual = false
        PersonalFlags.coldImport = false
        PersonalFlags.reset()
        assertTrue(PersonalFlags.personalShelves)
        assertTrue(PersonalFlags.tasteV2)
        assertTrue(PersonalFlags.weeklyRitual)
        assertTrue(PersonalFlags.coldImport)
    }
}
