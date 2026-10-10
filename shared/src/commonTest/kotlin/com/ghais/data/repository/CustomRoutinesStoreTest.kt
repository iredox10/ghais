package com.ghais.data.repository

import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Guards the custom-routines list: create/update/delete/get, the public flag,
 * and the two bulk JSON entry points (empty-gated restore vs merging sync).
 *
 * `CustomRoutinesStore` is an `object` singleton whose `Settings` is a
 * private lazy no-arg factory with no injection hook
 * (`CustomRoutinesStore.kt:55`), which throws on the unit-test classpath, so
 * persistence is a silent no-op here (same limitation as
 * `OnboardingStoreTest`). These tests assert only in-memory list transitions
 * on provably fresh random owners; persistence roundtrip is deliberately NOT
 * asserted.
 *
 * `create`/`update`/`setPublic` stamp the real clock
 * (`CustomRoutinesStore.kt:77,93,115`), so timestamps are asserted only as
 * "stamped" (> 0), never as exact values; ordering tests use
 * caller-supplied `updatedAtMs` through the JSON paths instead.
 */
class CustomRoutinesStoreTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun newOwnerId(tag: String): String = "$tag-${Random.nextLong().toString(16)}"

    private fun routineJson(id: String, updatedAtMs: Long, title: String = "Routine $id") =
        json.encodeToString(
            CustomRoutine(
                id = id,
                title = title,
                description = "desc",
                items = listOf(RoutineItem(reciterSlug = "alafasy", surahId = 1)),
                isPublic = false,
                createdAtMs = updatedAtMs,
                updatedAtMs = updatedAtMs,
            )
        )

    @BeforeTest
    fun setUp() {
        CustomRoutinesStore.setOwner(newOwnerId("routines-owner"))
        for (r in CustomRoutinesStore.routines.value.toList()) {
            CustomRoutinesStore.delete(r.id)
        }
    }

    @AfterTest
    fun tearDown() {
        for (r in CustomRoutinesStore.routines.value.toList()) {
            CustomRoutinesStore.delete(r.id)
        }
        CustomRoutinesStore.setOwner("local")
    }

    // --- Single-item mutators ---

    @Test
    fun testCreatePrependsWithAGeneratedIdAndStamps() {
        val first = CustomRoutinesStore.create(
            title = "Morning",
            description = "",
            items = emptyList(),
            isPublic = false,
        )
        val second = CustomRoutinesStore.create(
            title = "Evening",
            description = "",
            items = emptyList(),
            isPublic = false,
        )

        assertTrue(first.id.startsWith("rtn-"), "Unexpected id shape: ${first.id}")
        assertTrue(first.id != second.id, "Two routines minted together collided")
        assertTrue(first.createdAtMs > 0L && first.updatedAtMs > 0L)
        assertEquals(listOf(second.id, first.id), CustomRoutinesStore.routines.value.map { it.id })
        assertEquals(first, CustomRoutinesStore.get(first.id))
    }

    @Test
    fun testUpdateReplacesContentKeepingTheId() {
        val created = CustomRoutinesStore.create(
            title = "Old",
            description = "",
            items = emptyList(),
            isPublic = false,
        )

        CustomRoutinesStore.update(created.copy(title = "New"))

        val stored = CustomRoutinesStore.get(created.id)
        assertNotNull(stored)
        assertEquals("New", stored.title)
        assertEquals(1, CustomRoutinesStore.routines.value.size)
    }

    @Test
    fun testUpdateUnknownIdLeavesTheListUnchanged() {
        val created = CustomRoutinesStore.create(
            title = "Kept",
            description = "",
            items = emptyList(),
            isPublic = false,
        )

        CustomRoutinesStore.update(
            CustomRoutine(id = "ghost", title = "Ghost", updatedAtMs = 1L)
        )

        assertEquals(listOf(created.id), CustomRoutinesStore.routines.value.map { it.id })
    }

    @Test
    fun testDeleteRemovesOnlyTheMatchingRoutine() {
        val a = CustomRoutinesStore.create("A", "", emptyList(), false)
        val b = CustomRoutinesStore.create("B", "", emptyList(), false)

        CustomRoutinesStore.delete(a.id)

        assertNull(CustomRoutinesStore.get(a.id))
        assertEquals(listOf(b.id), CustomRoutinesStore.routines.value.map { it.id })
    }

    @Test
    fun testDeleteUnknownIdIsANoOp() {
        val created = CustomRoutinesStore.create("A", "", emptyList(), false)
        CustomRoutinesStore.delete("missing")

        assertEquals(listOf(created.id), CustomRoutinesStore.routines.value.map { it.id })
    }

    @Test
    fun testSetPublicFlipsTheFlag() {
        val created = CustomRoutinesStore.create("A", "", emptyList(), false)

        CustomRoutinesStore.setPublic(created.id, true)
        assertEquals(true, CustomRoutinesStore.get(created.id)?.isPublic)

        CustomRoutinesStore.setPublic(created.id, false)
        assertEquals(false, CustomRoutinesStore.get(created.id)?.isPublic)
    }

    @Test
    fun testSetPublicUnknownIdIsANoOp() {
        val created = CustomRoutinesStore.create("A", "", emptyList(), false)
        CustomRoutinesStore.setPublic("missing", true)

        assertEquals(listOf(created.id), CustomRoutinesStore.routines.value.map { it.id })
        assertEquals(false, CustomRoutinesStore.get(created.id)?.isPublic)
    }

    @Test
    fun testGetUnknownIdReturnsNull() {
        CustomRoutinesStore.create("A", "", emptyList(), false)
        assertNull(CustomRoutinesStore.get("missing"))
    }

    // --- Bulk restore (empty-gated) ---

    @Test
    fun testRestoreAllJsonAdoptsDecodableRowsNewestFirst() {
        val adopted = CustomRoutinesStore.restoreAllJson(
            listOf(
                routineJson("r-old", updatedAtMs = 1000L),
                routineJson("r-new", updatedAtMs = 2000L),
                "", // blank payload skipped
                "{not json", // malformed payload skipped
                routineJson("", updatedAtMs = 3000L), // blank id skipped
                routineJson("r-old", updatedAtMs = 1000L), // duplicate id collapses
            )
        )

        assertEquals(2, adopted)
        // Ids are preserved verbatim (the push prunes against them) and the
        // exposed order is newest-first by `updatedAtMs`.
        assertEquals(
            listOf("r-new", "r-old"),
            CustomRoutinesStore.routines.value.map { it.id },
        )
        assertEquals("Routine r-new", CustomRoutinesStore.get("r-new")?.title)
    }

    @Test
    fun testRestoreAllJsonRefusesWhenTheStoreIsNonEmpty() {
        val created = CustomRoutinesStore.create("Local", "", emptyList(), false)

        val adopted = CustomRoutinesStore.restoreAllJson(listOf(routineJson("r-cloud", 5000L)))

        assertEquals(0, adopted)
        assertEquals(listOf(created.id), CustomRoutinesStore.routines.value.map { it.id })
    }

    @Test
    fun testRestoreAllJsonAllSkippedReturnsZero() {
        assertEquals(0, CustomRoutinesStore.restoreAllJson(listOf("", "{bad", routineJson("", 1L))))
        assertTrue(CustomRoutinesStore.routines.value.isEmpty())
    }

    // --- Bulk merge (never gated, never replace) ---

    @Test
    fun testMergeJsonInsertsMissingAndLetsLocalWin() {
        val local = CustomRoutinesStore.create("Local title", "", emptyList(), false)

        val inserted = CustomRoutinesStore.mergeJson(
            listOf(
                // Same id, edited upstream: local is the live truth and wins.
                routineJson(local.id, updatedAtMs = 9_999L, title = "Upstream title"),
                routineJson("r-new", updatedAtMs = 1000L),
                "", // blank skipped
                routineJson("r-new", updatedAtMs = 1000L), // duplicate collapses
            )
        )

        assertEquals(1, inserted)
        assertEquals("Local title", CustomRoutinesStore.get(local.id)?.title)
        assertNotNull(CustomRoutinesStore.get("r-new"))
        assertEquals(
            setOf(local.id, "r-new"),
            CustomRoutinesStore.routines.value.map { it.id }.toSet(),
        )
    }

    @Test
    fun testMergeJsonEmptyPayloadIsANoOp() {
        CustomRoutinesStore.create("Local", "", emptyList(), false)

        assertEquals(0, CustomRoutinesStore.mergeJson(emptyList()))
        assertEquals(1, CustomRoutinesStore.routines.value.size)
    }

    @Test
    fun testMergeJsonSecondPassWithTheSamePayloadInsertsNothing() {
        val first = CustomRoutinesStore.mergeJson(listOf(routineJson("r-1", 1000L)))
        assertEquals(1, first)

        assertEquals(0, CustomRoutinesStore.mergeJson(listOf(routineJson("r-1", 1000L))))
        assertEquals(1, CustomRoutinesStore.routines.value.size)
    }
}
