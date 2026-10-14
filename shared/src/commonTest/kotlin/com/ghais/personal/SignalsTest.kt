package com.ghais.personal

import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the implicit-feedback foundation: [SignalKind] weights,
 * [scoreSignals] decay math, and the [SignalStore] append-only log
 * (ordering, normalization, 500-entry cap/prune, owner isolation).
 *
 * `SignalStore` is an `object` singleton whose default `Settings()` backend
 * throws on the unit-test classpath (same limitation as `FollowStoreTest`),
 * so with no [SignalStore.settingsOverride] persistence is a silent no-op
 * and these tests assert in-memory `StateFlow` transitions only. Every test
 * binds a provably fresh random owner so "empty" holds with or without a
 * backend. Persistence roundtrips through the [SignalStore.settingsOverride]
 * seam are deliberately NOT asserted here (no in-memory `Settings`
 * implementation in `commonTest` dependencies).
 */
class SignalsTest {

    private fun newOwnerId(tag: String): String = "$tag-${Random.nextLong().toString(16)}"

    @BeforeTest
    fun setUp() {
        SignalStore.settingsOverride = null
        SignalStore.setOwner(newOwnerId("signals-owner"))
        SignalStore.clear()
    }

    @AfterTest
    fun tearDown() {
        SignalStore.settingsOverride = null
        SignalStore.clear()
        SignalStore.setOwner("local")
    }

    private fun assertClose(expected: Float, actual: Float, tolerance: Float = 0.001f) {
        assertTrue(
            abs(expected - actual) <= tolerance,
            "expected=$expected actual=$actual tolerance=$tolerance",
        )
    }

    // --- SignalKind weights ---

    @Test
    fun testKindWeightsMatchSpec() {
        assertEquals(1f, SignalKind.PLAY_STARTED.weight)
        assertEquals(3f, SignalKind.PLAY_COMPLETED.weight)
        assertEquals(1f, SignalKind.PREVIEW.weight)
        assertEquals(4f, SignalKind.FOLLOW.weight)
        assertEquals(2f, SignalKind.SEARCH_OPEN.weight)
        assertEquals(2f, SignalKind.DOWNLOAD.weight)
        assertEquals(-2f, SignalKind.SKIP_QUICK.weight)
        assertEquals(-4f, SignalKind.UNFOLLOW.weight)
    }

    @Test
    fun testFreshSignalScoresItsKindWeight() {
        val today = 2_000L
        for (kind in SignalKind.entries) {
            val scores = scoreSignals(listOf(ReciterSignal("qari", kind, today)), today)
            assertClose(kind.weight, scores.getValue("qari"))
        }
    }

    @Test
    fun testReciterSignalDefaultsWeightToKindWeight() {
        val signal = ReciterSignal("alafasy", SignalKind.FOLLOW, epochDay = 10L)
        assertEquals(4f, signal.weight)
    }

    // --- Decay math ---

    @Test
    fun testEmptyScoresEmpty() {
        assertTrue(scoreSignals(emptyList(), todayDay = 100L).isEmpty())
    }

    @Test
    fun testSignalHalvesAfterOneHalfLife() {
        val today = 1_000L
        val signals = listOf(ReciterSignal("alafasy", SignalKind.FOLLOW, today - 30L))
        assertClose(2f, scoreSignals(signals, today).getValue("alafasy"))
    }

    @Test
    fun testSignalQuartersAfterTwoHalfLives() {
        val today = 1_000L
        val signals = listOf(ReciterSignal("alafasy", SignalKind.FOLLOW, today - 60L))
        assertClose(1f, scoreSignals(signals, today).getValue("alafasy"))
    }

    @Test
    fun testCustomHalfLifeChangesDecay() {
        val today = 1_000L
        val signals = listOf(ReciterSignal("alafasy", SignalKind.FOLLOW, today - 30L))
        // Half-life 15: 30 days old = two half-lives = quarter weight.
        assertClose(1f, scoreSignals(signals, today, halfLifeDays = 15).getValue("alafasy"))
        // Half-life 60: 30 days old = half a half-life = 4 / sqrt(2).
        assertClose(2.828f, scoreSignals(signals, today, halfLifeDays = 60).getValue("alafasy"), 0.01f)
    }

    @Test
    fun testFutureSignalClampsToFullWeight() {
        val today = 1_000L
        val signals = listOf(ReciterSignal("alafasy", SignalKind.FOLLOW, today + 50L))
        assertClose(4f, scoreSignals(signals, today).getValue("alafasy"))
    }

    @Test
    fun testNonPositiveHalfLifeIsCoercedNotDividedByZero() {
        val today = 1_000L
        val signals = listOf(ReciterSignal("alafasy", SignalKind.FOLLOW, today))
        assertClose(4f, scoreSignals(signals, today, halfLifeDays = 0).getValue("alafasy"))
        assertClose(4f, scoreSignals(signals, today, halfLifeDays = -5).getValue("alafasy"))
    }

    // --- Aggregation ---

    @Test
    fun testSameSlugSumsAcrossKindsAndDays() {
        val today = 1_000L
        val signals = listOf(
            ReciterSignal("alafasy", SignalKind.FOLLOW, today), // 4 * 1.0
            ReciterSignal("alafasy", SignalKind.PLAY_COMPLETED, today - 30L), // 3 * 0.5
        )
        assertClose(5.5f, scoreSignals(signals, today).getValue("alafasy"))
    }

    @Test
    fun testNegativeSignalsSubtract() {
        val today = 1_000L
        val signals = listOf(
            ReciterSignal("alafasy", SignalKind.FOLLOW, today), // +4
            ReciterSignal("alafasy", SignalKind.UNFOLLOW, today), // -4
        )
        assertClose(0f, scoreSignals(signals, today).getValue("alafasy"))
    }

    @Test
    fun testDifferentSlugsScoreSeparately() {
        val today = 1_000L
        val signals = listOf(
            ReciterSignal("alafasy", SignalKind.FOLLOW, today),
            ReciterSignal("husary", SignalKind.PLAY_STARTED, today),
        )
        val scores = scoreSignals(signals, today)
        assertClose(4f, scores.getValue("alafasy"))
        assertClose(1f, scores.getValue("husary"))
    }

    @Test
    fun testStoredWeightWinsOverEnumWeight() {
        // Forward-compat pin: retuning the enum must not rescore old events.
        val today = 1_000L
        val signals = listOf(ReciterSignal("alafasy", SignalKind.PLAY_STARTED, today, weight = 10f))
        assertClose(10f, scoreSignals(signals, today).getValue("alafasy"))
    }

    @Test
    fun testBlankSlugsAreSkipped() {
        val today = 1_000L
        val signals = listOf(
            ReciterSignal("   ", SignalKind.FOLLOW, today),
            ReciterSignal("alafasy", SignalKind.PLAY_STARTED, today),
        )
        assertEquals(setOf("alafasy"), scoreSignals(signals, today).keys)
    }

    // --- SignalStore ---

    @Test
    fun testRecordAppendsOldestFirstTrimsAndDropsBlanks() {
        SignalStore.record(SignalKind.PLAY_STARTED, "  alafasy  ", epochDay = 100L)
        SignalStore.record(SignalKind.FOLLOW, "husary", epochDay = 101L)
        SignalStore.record(SignalKind.PLAY_STARTED, "")
        SignalStore.record(SignalKind.PLAY_STARTED, "   ")

        val log = SignalStore.signals.value
        assertEquals(2, log.size)
        assertEquals("alafasy", log[0].slug)
        assertEquals(SignalKind.PLAY_STARTED, log[0].kind)
        assertEquals(100L, log[0].epochDay)
        assertEquals("husary", log[1].slug)
    }

    @Test
    fun testPerKindHelpersRecordTheRightKind() {
        SignalStore.recordPlayStarted("a", epochDay = 1L)
        SignalStore.recordPlayCompleted("b", epochDay = 1L)
        SignalStore.recordPreview("c", epochDay = 1L)
        SignalStore.recordFollow("d", epochDay = 1L)
        SignalStore.recordSearchOpen("e", epochDay = 1L)
        SignalStore.recordDownload("f", epochDay = 1L)
        SignalStore.recordSkipQuick("g", epochDay = 1L)
        SignalStore.recordUnfollow("h", epochDay = 1L)

        assertEquals(
            listOf(
                SignalKind.PLAY_STARTED,
                SignalKind.PLAY_COMPLETED,
                SignalKind.PREVIEW,
                SignalKind.FOLLOW,
                SignalKind.SEARCH_OPEN,
                SignalKind.DOWNLOAD,
                SignalKind.SKIP_QUICK,
                SignalKind.UNFOLLOW,
            ),
            SignalStore.signals.value.map { it.kind },
        )
    }

    @Test
    fun testLogCapsAtFiveHundredPruningOldest() {
        for (i in 0 until 520) {
            SignalStore.record(SignalKind.PLAY_STARTED, "slug-$i", epochDay = 1L)
        }

        val log = SignalStore.signals.value
        assertEquals(SignalStore.MAX_SIGNALS, log.size)
        assertEquals("slug-20", log.first().slug)
        assertEquals("slug-519", log.last().slug)
        assertTrue(log.none { it.slug == "slug-0" })
    }

    @Test
    fun testClearEmptiesTheLog() {
        SignalStore.recordPlayStarted("alafasy", epochDay = 1L)
        SignalStore.clear()
        assertTrue(SignalStore.signals.value.isEmpty())
    }

    @Test
    fun testFreshOwnerStartsWithAnEmptyLog() {
        SignalStore.recordPlayStarted("alafasy", epochDay = 1L)
        SignalStore.setOwner(newOwnerId("other-owner"))
        assertTrue(SignalStore.signals.value.isEmpty())
    }

    @Test
    fun testScoresDelegatesToScoreSignals() {
        SignalStore.record(SignalKind.FOLLOW, "alafasy", epochDay = 1_000L)
        SignalStore.record(SignalKind.SKIP_QUICK, "husary", epochDay = 1_000L)
        val scores = SignalStore.scores(todayDay = 1_000L)
        assertClose(4f, scores.getValue("alafasy"))
        assertClose(-2f, scores.getValue("husary"))
    }

    @Test
    fun testExportImportRoundtripNormalizes() {
        SignalStore.recordPlayStarted("alafasy", epochDay = 5L)
        val snapshot = SignalStore.exportSnapshot().toMutableList()
        snapshot.add(ReciterSignal("injected", SignalKind.FOLLOW, 5L))
        // Export is a copy: mutating it changes nothing live.
        assertEquals(1, SignalStore.signals.value.size)

        SignalStore.importSnapshot(
            listOf(
                ReciterSignal("  husary  ", SignalKind.FOLLOW, 6L),
                ReciterSignal("", SignalKind.FOLLOW, 6L),
                ReciterSignal("   ", SignalKind.PLAY_STARTED, 6L),
            ),
        )
        val log = SignalStore.signals.value
        assertEquals(1, log.size)
        assertEquals("husary", log.single().slug)
    }

    @Test
    fun testImportSnapshotPrunesToCapKeepingNewest() {
        val oversized = (0 until SignalStore.MAX_SIGNALS + 50).map { i ->
            ReciterSignal("slug-$i", SignalKind.PLAY_STARTED, 1L)
        }
        SignalStore.importSnapshot(oversized)
        val log = SignalStore.signals.value
        assertEquals(SignalStore.MAX_SIGNALS, log.size)
        assertEquals("slug-50", log.first().slug)
    }
}
