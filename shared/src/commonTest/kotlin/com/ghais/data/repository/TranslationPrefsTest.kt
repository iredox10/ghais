package com.ghais.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

/**
 * Guards the translation-edition preference — the single piece of state every
 * other part of the multi-language feature reads at call time.
 *
 * [TranslationPrefs] is a process-wide `object` and the whole test module
 * shares one instance, so every test here states the edition it needs
 * explicitly and restores [TranslationPrefs.DEFAULT_EDITION] when it finishes.
 * Nothing in this file asserts a "pristine initial value": whichever test class
 * ran before this one may have left another edition selected, and such a test
 * would fail purely on ordering rather than on behaviour.
 *
 * Scope note — persistence is deliberately **not** asserted.
 * [TranslationPrefs] builds its store through the no-arg multiplatform
 * `Settings()` factory, which needs a platform `Context` (an androidx.startup
 * initializer on Android, NSUserDefaults on iOS) and therefore throws on the
 * JVM unit-test classpath; [TranslationPrefs] swallows that, so the write is a
 * no-op here and nothing can be read back out (same limitation, and the same
 * reasoning, as `OnboardingStoreTest`). These tests pin the in-memory
 * StateFlow contract only: a real change reaches collectors, a repeat write is
 * a no-op, and a blank edition is dropped.
 */
class TranslationPrefsTest {

    /** An edition from the bundled catalog, distinct from the default. */
    private val turkish = "tr.yazir"

    /** A second non-default edition, for round trips. */
    private val indonesian = "id.indonesian"

    // --- 1. The default edition ---

    /**
     * `en.sahih` is pinned twice over: as the literal default a first-launch
     * user gets, and as a real catalog entry. The second half is the
     * load-bearing one — [QuranAyahRepository] ships bundled offline text for
     * the default edition and for no other, so a default that named something
     * outside the catalog would silently turn offline first launch into
     * placeholders.
     */
    @Test
    fun testDefaultEditionIsTheBundledEnglishSahih() {
        assertEquals("en.sahih", TranslationPrefs.DEFAULT_EDITION)
        assertTrue(
            TranslationCatalog.byId(TranslationPrefs.DEFAULT_EDITION) != null,
            "DEFAULT_EDITION is not a bundled catalog edition, so no seed text would apply to it",
        )
        assertEquals(
            "English",
            TranslationCatalog.byId(TranslationPrefs.DEFAULT_EDITION)?.languageName,
        )
        TranslationPrefs.setEdition(TranslationPrefs.DEFAULT_EDITION)
    }

    // --- 2. Blank input ---

    /**
     * A blank edition would go straight onto the wire as
     * `…/editions/quran-uthmani,` and would also be stored as the user's
     * choice, so both `""` and a whitespace-only id must be dropped. The
     * starting edition is set to a real one first, so "unchanged" cannot pass
     * vacuously against the default.
     */
    @Test
    fun testBlankEditionIsIgnored() {
        TranslationPrefs.setEdition(turkish)
        val before = TranslationPrefs.selectedEdition.value

        TranslationPrefs.setEdition("")
        assertEquals(before, TranslationPrefs.selectedEdition.value, "empty edition was accepted")
        assertEquals(turkish, TranslationPrefs.selectedEdition.value)

        TranslationPrefs.setEdition("   ")
        assertEquals(before, TranslationPrefs.selectedEdition.value, "blank edition was accepted")
        assertEquals(turkish, TranslationPrefs.selectedEdition.value)

        TranslationPrefs.setEdition(TranslationPrefs.DEFAULT_EDITION)
        assertEquals(TranslationPrefs.DEFAULT_EDITION, TranslationPrefs.selectedEdition.value)
    }

    // --- 3. Repeat writes ---

    /**
     * Writing the id that is already selected changes nothing, and writing a
     * different one does. Together these are the whole guard in `setEdition`:
     * a picker that re-selects the current translation must not churn the flow,
     * while a real choice must land.
     */
    @Test
    fun testSettingTheSameEditionTwiceIsANoOp() {
        TranslationPrefs.setEdition(turkish)
        assertEquals(turkish, TranslationPrefs.selectedEdition.value)

        TranslationPrefs.setEdition(turkish)
        assertEquals(turkish, TranslationPrefs.selectedEdition.value)

        TranslationPrefs.setEdition(indonesian)
        assertEquals(indonesian, TranslationPrefs.selectedEdition.value)

        TranslationPrefs.setEdition(TranslationPrefs.DEFAULT_EDITION)
        assertEquals(TranslationPrefs.DEFAULT_EDITION, TranslationPrefs.selectedEdition.value)
    }

    /**
     * The "already selected" guard compares against the *current* edition, not
     * against a set of ids seen before: an id the user picked, switched away
     * from and comes back to must still be applied. A user flipping between two
     * translations is the ordinary case, so a guard implemented as "never
     * re-apply" would break the feature on the second tap.
     */
    @Test
    fun testEditionCanBeSwitchedBackAndForth() {
        TranslationPrefs.setEdition(turkish)
        assertEquals(turkish, TranslationPrefs.selectedEdition.value)

        TranslationPrefs.setEdition(indonesian)
        assertEquals(indonesian, TranslationPrefs.selectedEdition.value)

        TranslationPrefs.setEdition(turkish)
        assertEquals(turkish, TranslationPrefs.selectedEdition.value, "returning to a previously used edition was ignored")

        TranslationPrefs.setEdition(indonesian)
        assertEquals(indonesian, TranslationPrefs.selectedEdition.value)

        TranslationPrefs.setEdition(TranslationPrefs.DEFAULT_EDITION)
        assertEquals(TranslationPrefs.DEFAULT_EDITION, TranslationPrefs.selectedEdition.value)
    }

    // --- 4. Whitespace padding ---

    /**
     * Surrounding whitespace is trimmed rather than rejected: a padded id
     * coming from hand-edited storage or a deep link is a real edition, and
     * persisting it verbatim would put `%20` on the wire
     * (`…/editions/quran-uthmani,%20en.sahih%20`) and silently 404 the
     * translation for every ayah. The starting edition is set to a real one
     * first so an unchanged flow could not pass vacuously.
     */
    @Test
    fun testSurroundingWhitespaceIsTrimmedFromEdition() {
        TranslationPrefs.setEdition(turkish)
        assertEquals(turkish, TranslationPrefs.selectedEdition.value)

        TranslationPrefs.setEdition(" $turkish ")
        assertEquals(turkish, TranslationPrefs.selectedEdition.value, "padded edition was not trimmed")

        TranslationPrefs.setEdition(" $indonesian ")
        assertEquals(indonesian, TranslationPrefs.selectedEdition.value, "padded edition switch was not trimmed")

        TranslationPrefs.setEdition(TranslationPrefs.DEFAULT_EDITION)
        assertEquals(TranslationPrefs.DEFAULT_EDITION, TranslationPrefs.selectedEdition.value)
    }

    // --- 5. The flow itself ---

    /**
     * [TranslationPrefs.selectedEdition] is what the UI collects, so a change
     * has to reach an existing collector rather than only the next reader.
     * The collector is started `UNDISPATCHED` so it holds the current value
     * before the write, and the drain is bounded so a flow that stopped
     * propagating fails the assertion instead of hanging the test run.
     */
    @Test
    fun testEditionChangeIsPublishedToExistingCollectors() = runBlocking {
        TranslationPrefs.setEdition(turkish)
        val seen = mutableListOf<String>()
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            TranslationPrefs.selectedEdition.collect { seen += it }
        }
        try {
            assertEquals(listOf(turkish), seen, "collector did not start at the selected edition")

            TranslationPrefs.setEdition(indonesian)
            drain()
            assertEquals(listOf(turkish, indonesian), seen, "edition change was not published")

            // A repeat write must not wake the collector again: a StateFlow
            // conflates an equal value, and a second emission here would make
            // every re-render triggered by a re-selection look like a switch.
            TranslationPrefs.setEdition(indonesian)
            drain()
            assertEquals(listOf(turkish, indonesian), seen, "a no-op write emitted a new value")

            // Blank input must not reach collectors either.
            TranslationPrefs.setEdition("   ")
            drain()
            assertEquals(listOf(turkish, indonesian), seen, "a blank edition was published")
        } finally {
            collector.cancel()
            TranslationPrefs.setEdition(TranslationPrefs.DEFAULT_EDITION)
        }
    }

    /**
     * Gives the event loop a chance to run the emissions a just-made write
     * dispatched to this flow's collector. Bounded, and bounded generously:
     * a slow drain could only ever hide a regression here, never invent one.
     */
    private suspend fun drain() {
        repeat(200) { yield() }
    }
}