package com.ghais.data.repository

import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Guards the schedule mutators' pure contract: normalization (hour/minute
 * clamps, surah-range swap), time ordering, id-keyed replace/remove/enable,
 * the merging cloud restore, and id generation.
 *
 * `SchedulesStore` is an `object` singleton whose `Settings` is a private
 * lazy no-arg factory with no injection hook (`SchedulesStore.kt:79`), which
 * throws on the unit-test classpath, so persistence is a silent no-op here
 * (same limitation as `OnboardingStoreTest`). These tests assert only
 * in-memory list transitions on provably fresh random owners; persistence
 * roundtrip is deliberately NOT asserted.
 *
 * No engine is needed: every `ScheduleEngine.refresh` call in the store is
 * already wrapped in try/catch (`SchedulesStore.kt:103-106`, `:278-287`), so
 * the pure mutator contract is observable without alarms.
 */
class SchedulesStoreTest {

    private fun newOwnerId(tag: String): String = "$tag-${Random.nextLong().toString(16)}"

    private fun schedule(
        id: String,
        hour: Int = 8,
        minute: Int = 0,
        fromSurah: Int = 1,
        toSurah: Int = 5,
        enabled: Boolean = true,
    ) = RecitationSchedule(
        id = id,
        hour = hour,
        minute = minute,
        reciterSlug = "alafasy",
        fromSurah = fromSurah,
        toSurah = toSurah,
        enabled = enabled,
    )

    @BeforeTest
    fun setUp() {
        SchedulesStore.setOwner(newOwnerId("schedules-owner"))
    }

    @AfterTest
    fun tearDown() {
        for (s in SchedulesStore.schedules.value.toList()) {
            SchedulesStore.remove(s.id)
        }
        SchedulesStore.setOwner("local")
    }

    // --- Normalization ---

    @Test
    fun testAddSwapsAnInvertedSurahRange() {
        val stored = SchedulesStore.add(schedule("swap", fromSurah = 10, toSurah = 2))

        assertEquals(2, stored.fromSurah)
        assertEquals(10, stored.toSurah)
        assertEquals(2, SchedulesStore.get("swap")?.fromSurah)
        assertEquals(10, SchedulesStore.get("swap")?.toSurah)
    }

    @Test
    fun testAddClampsHourMinuteAndSurahBounds() {
        val stored = SchedulesStore.add(
            schedule("clamp", hour = 99, minute = -5, fromSurah = 0, toSurah = 200)
        )

        assertEquals(23, stored.hour)
        assertEquals(0, stored.minute)
        assertEquals(1, stored.fromSurah)
        assertEquals(114, stored.toSurah)
    }

    @Test
    fun testAddKeepsTheListSortedByTime() {
        SchedulesStore.add(schedule("late", hour = 17, minute = 30))
        SchedulesStore.add(schedule("early", hour = 8, minute = 5))
        SchedulesStore.add(schedule("mid", hour = 12, minute = 0))

        assertEquals(
            listOf("early", "mid", "late"),
            SchedulesStore.schedules.value.map { it.id },
        )
    }

    @Test
    fun testAddWithAnExistingIdReplacesInPlace() {
        SchedulesStore.add(schedule("same", hour = 8))
        SchedulesStore.add(schedule("same", hour = 20))

        assertEquals(1, SchedulesStore.schedules.value.size)
        assertEquals(20, SchedulesStore.get("same")?.hour)
    }

    @Test
    fun testUpdateNormalizesAndReplaces() {
        SchedulesStore.add(schedule("u", hour = 8, fromSurah = 1, toSurah = 3))
        SchedulesStore.update(schedule("u", hour = 9, fromSurah = 7, toSurah = 4))

        val stored = SchedulesStore.get("u")
        assertNotNull(stored)
        assertEquals(9, stored.hour)
        assertEquals(4, stored.fromSurah)
        assertEquals(7, stored.toSurah)
        assertEquals(1, SchedulesStore.schedules.value.size)
    }

    @Test
    fun testUpdateWithAnUnknownIdAddsTheSchedule() {
        // `update` is an upsert: it filters by id and appends, so an unknown
        // id lands as a new row rather than a no-op.
        SchedulesStore.update(schedule("ghost", hour = 6))

        assertEquals(6, SchedulesStore.get("ghost")?.hour)
    }

    // --- Remove / enable / get ---

    @Test
    fun testRemoveDeletesTheMatchingSchedule() {
        SchedulesStore.add(schedule("a", hour = 8))
        SchedulesStore.add(schedule("b", hour = 9))
        SchedulesStore.remove("a")

        assertNull(SchedulesStore.get("a"))
        assertEquals(listOf("b"), SchedulesStore.schedules.value.map { it.id })
    }

    @Test
    fun testRemoveUnknownIdIsANoOp() {
        SchedulesStore.add(schedule("a", hour = 8))
        SchedulesStore.remove("missing")

        assertEquals(listOf("a"), SchedulesStore.schedules.value.map { it.id })
    }

    @Test
    fun testSetEnabledFlipsTheFlag() {
        SchedulesStore.add(schedule("e", enabled = true))
        SchedulesStore.setEnabled("e", false)
        assertFalse(SchedulesStore.get("e")?.enabled ?: true)

        SchedulesStore.setEnabled("e", true)
        assertTrue(SchedulesStore.get("e")?.enabled ?: false)
    }

    @Test
    fun testSetEnabledUnknownIdIsANoOp() {
        SchedulesStore.add(schedule("e", enabled = true))
        SchedulesStore.setEnabled("missing", false)

        assertEquals(listOf("e"), SchedulesStore.schedules.value.map { it.id })
        assertTrue(SchedulesStore.get("e")?.enabled ?: false)
    }

    @Test
    fun testGetUnknownIdReturnsNull() {
        SchedulesStore.add(schedule("e"))
        assertNull(SchedulesStore.get("missing"))
    }

    // --- Cloud restore ---

    @Test
    fun testRestoreAllFromCloudMergesWithoutClobberingLocal() {
        SchedulesStore.add(schedule("mine", hour = 8, fromSurah = 1, toSurah = 3))

        val added = SchedulesStore.restoreAllFromCloud(
            listOf(
                // Local wins on id match: the cloud's stale 6am must not move it.
                schedule("mine", hour = 6, fromSurah = 1, toSurah = 3),
                // New remote row is adopted with its id preserved verbatim
                // (the push prunes against this id) and normalized.
                schedule("cloud", hour = 9, fromSurah = 12, toSurah = 10),
                // Blank ids are skipped per-item...
                schedule("", hour = 10),
                // ...and duplicate ids inside one snapshot collapse to the first.
                schedule("cloud", hour = 11),
            )
        )

        assertEquals(1, added)
        assertEquals(8, SchedulesStore.get("mine")?.hour)
        val cloud = SchedulesStore.get("cloud")
        assertNotNull(cloud)
        assertEquals(9, cloud.hour)
        assertEquals(10, cloud.fromSurah)
        assertEquals(12, cloud.toSurah)
        assertEquals(
            listOf("mine", "cloud"),
            SchedulesStore.schedules.value.map { it.id },
        )
    }

    @Test
    fun testRestoreAllFromCloudEmptySnapshotIsANoOp() {
        SchedulesStore.add(schedule("mine", hour = 8))

        assertEquals(0, SchedulesStore.restoreAllFromCloud(emptyList()))
        assertEquals(listOf("mine"), SchedulesStore.schedules.value.map { it.id })
    }

    @Test
    fun testRestoreAllFromCloudAllKnownReturnsZero() {
        SchedulesStore.add(schedule("mine", hour = 8))
        val first = SchedulesStore.restoreAllFromCloud(listOf(schedule("cloud", hour = 9)))
        assertEquals(1, first)

        // Second pass with the same snapshot is idempotent.
        assertEquals(0, SchedulesStore.restoreAllFromCloud(listOf(schedule("cloud", hour = 9))))
        assertEquals(2, SchedulesStore.schedules.value.size)
    }

    // --- Ids ---

    @Test
    fun testNewIdHasTheSchedulePrefixAndIsUnique() {
        val first = SchedulesStore.newId()
        val second = SchedulesStore.newId()

        assertTrue(first.startsWith("sch-"), "Unexpected id shape: $first")
        assertTrue(second.startsWith("sch-"), "Unexpected id shape: $second")
        assertFalse(first == second, "Two ids minted in the same millisecond collided")
    }
}
