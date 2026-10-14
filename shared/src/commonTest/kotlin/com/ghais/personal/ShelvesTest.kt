package com.ghais.personal

import com.ghais.domain.model.Reciter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Guards AGENT P3's personalized shelves.
 *
 * [buildShelves] is pure (map in, shelves out), so these tests pin the
 * contract the RecitersScreen rails rely on: shelf presence per input shape,
 * per-shelf caps, determinism (including day rotation), no duplicates across
 * shelves, and graceful empty/unknown-input behavior.
 */
class ShelvesTest {

    private fun qari(
        slug: String,
        nameEn: String = slug,
        country: String = "",
        riwayah: String = "Hafs",
        style: String = "murattal",
        teacher: Boolean = false,
        surahs: String = "",
    ): Reciter = Reciter(
        slug = slug,
        nameEn = nameEn,
        nameAr = nameEn,
        riwayah = riwayah,
        style = style,
        country = country,
        isTeacher = teacher,
        availableSurahList = surahs,
    )

    private fun build(
        reciters: List<Reciter>,
        affinities: Map<String, Float> = emptyMap(),
        followed: Set<String> = emptySet(),
        history: Set<String> = emptySet(),
        epochDay: Long = 0L,
    ): Map<String, Shelf> =
        buildShelves(reciters, affinities, followed, history, epochDay)
            .associateBy { it.id }

    // --- 1. Your voices: presence + most-affine-first ---

    @Test
    fun testYourVoicesMostAffineFirst() {
        val reciters = listOf(
            qari("low-qari", "Low Qari"),
            qari("top-qari", "Top Qari"),
            qari("mid-qari", "Mid Qari"),
            qari("unplayed", "Unplayed Qari"),
        )
        val byId = build(
            reciters = reciters,
            affinities = mapOf("low-qari" to 0.2f, "top-qari" to 0.9f, "mid-qari" to 0.5f),
            followed = setOf("low-qari", "top-qari", "mid-qari"),
        )
        val shelf = byId["your-voices"]
        assertTrue(shelf != null, "followed voices must produce a shelf")
        assertEquals("Your voices", shelf.title)
        assertTrue(shelf.subtitle.isNotBlank(), "shelf must carry a why-line")
        assertEquals(listOf("top-qari", "mid-qari", "low-qari"), shelf.slugs)
    }

    // --- 2. Because you follow X: first name, related, exclusions ---

    @Test
    fun testBecauseYouFollowUsesFirstNameAndExcludesSeed() {
        val seed = qari("seed-voice", "Ahmed Fathi", country = "Egypt")
        val reciters = listOf(
            seed,
            qari("egypt-a", "Farid Adel", country = "Egypt"),
            qari("egypt-b", "Gaber Hosni", country = "Egypt"),
            qari("france-a", "Pierre Dubois", country = "France"),
            qari("played-x", "Played Voice", country = "Egypt"),
        )
        val byId = build(
            reciters = reciters,
            affinities = mapOf("seed-voice" to 0.95f),
            followed = setOf("seed-voice"),
            history = setOf("played-x"),
        )
        val shelf = byId["because-follow"]
        assertTrue(shelf != null, "a followed voice with unplayed catalog must produce a because-shelf")
        assertEquals("Because you follow Ahmed", shelf.title)
        assertTrue(shelf.subtitle.contains("Ahmed Fathi"), "why-line must name the seed voice")
        assertTrue("egypt-a" in shelf.slugs && "egypt-b" in shelf.slugs, "same-country voices rank first")
        assertEquals("egypt-a", shelf.slugs.first(), "Farid Adel sorts before Gaber Hosni by name tie-break")
        assertFalse("seed-voice" in shelf.slugs, "seed must never suggest itself")
        assertFalse("played-x" in shelf.slugs, "played voices must be excluded")
    }

    // --- 3. For you, daily: top-affine unplayed, played excluded ---

    @Test
    fun testForYouDailyTopAffineUnplayedOnly() {
        val reciters = listOf(
            qari("played-high", "Played High"),
            qari("voice-a", "Voice A"),
            qari("voice-b", "Voice B"),
        )
        val byId = build(
            reciters = reciters,
            affinities = mapOf("played-high" to 0.99f, "voice-a" to 0.9f, "voice-b" to 0.5f),
            history = setOf("played-high"),
            epochDay = 0L,
        )
        // No followed voices: only the daily shelf can appear.
        assertEquals(setOf("for-you-daily"), byId.keys)
        assertEquals(listOf("voice-a", "voice-b"), byId["for-you-daily"]!!.slugs)
    }

    // --- 4. Day rotation: same set, fresh order, negative-safe ---

    @Test
    fun testForYouDailyRotatesAcrossDays() {
        val reciters = listOf(
            qari("voice-a", "Voice A"),
            qari("voice-b", "Voice B"),
            qari("voice-c", "Voice C"),
        )
        val affinities = mapOf("voice-a" to 0.9f, "voice-b" to 0.8f, "voice-c" to 0.7f)
        val day0 = build(reciters, affinities, epochDay = 0L)["for-you-daily"]!!.slugs
        val day1 = build(reciters, affinities, epochDay = 1L)["for-you-daily"]!!.slugs
        val dayNeg = build(reciters, affinities, epochDay = -1L)["for-you-daily"]!!.slugs
        assertEquals(listOf("voice-a", "voice-b", "voice-c"), day0)
        assertEquals(listOf("voice-b", "voice-c", "voice-a"), day1)
        assertEquals(day0.toSet(), day1.toSet(), "rotation must reorder, never add/drop")
        assertEquals(day0.toSet(), dayNeg.toSet(), "negative days must stay safe")
        assertEquals(listOf("voice-c", "voice-a", "voice-b"), dayNeg)
    }

    // --- 5. Determinism: repeat + reordered input ---

    @Test
    fun testBuildIsDeterministic() {
        val reciters = richCatalog()
        val affinities = richAffinities()
        val followed = setOf("f-ahmed", "f-bilal", "f-cem")
        val history = setOf("h-daoud", "h-emad")
        val first = buildShelves(reciters, affinities, followed, history, 7L)
        val second = buildShelves(reciters, affinities, followed, history, 7L)
        assertEquals(first, second, "repeat calls must be identical")
        val reversed = buildShelves(reciters.reversed(), affinities, followed, history, 7L)
        assertEquals(
            first.map { it.slugs },
            reversed.map { it.slugs },
            "input order must not affect rankings",
        )
    }

    // --- 6. Caps: 8 per shelf ---

    @Test
    fun testShelvesAreCappedAtEight() {
        val reciters = (1..20).map { qari("v-$it", "Voice $it") }
        val affinities = (1..20).associate { "v-$it" to (it * 0.04f) }
        val byId = build(
            reciters = reciters,
            affinities = affinities,
            followed = (1..20).map { "v-$it" }.toSet(),
        )
        val shelf = byId["your-voices"]!!
        assertEquals(8, shelf.slugs.size)
        assertEquals("v-20", shelf.slugs.first(), "cap must keep the most-affine voices")
        // Rich catalog: every emitted shelf respects the cap.
        buildShelves(richCatalog(), richAffinities(), setOf("f-ahmed", "f-bilal", "f-cem"), setOf("h-daoud", "h-emad"), 3L)
            .forEach { assertTrue(it.slugs.size <= 8, "shelf ${it.id} exceeds cap") }
    }

    // --- 7. No duplicates across shelves; played/followed placement ---

    @Test
    fun testNoDuplicatesAcrossShelves() {
        val followed = setOf("f-ahmed", "f-bilal", "f-cem")
        val history = setOf("h-daoud", "h-emad")
        val shelves = buildShelves(richCatalog(), richAffinities(), followed, history, 3L)
        assertTrue(shelves.size >= 3, "rich input should yield several shelves, got ${shelves.map { it.id }}")
        val all = shelves.flatMap { it.slugs.map { slug -> slug.trim().lowercase() } }
        assertEquals(all.size, all.toSet().size, "a voice must appear on at most one shelf")
        val yourVoices = shelves.first { it.id == "your-voices" }.slugs.toSet()
        assertEquals(followed, yourVoices, "followed voices belong only on Your voices")
        shelves.filter { it.id != "your-voices" }.flatMap { it.slugs }.forEach { slug ->
            assertFalse(slug in followed, "$slug leaked outside Your voices")
            assertFalse(slug in history, "played $slug must never be shelved as unplayed")
        }
    }

    // --- 8. Empty input ---

    @Test
    fun testEmptyInputYieldsNoShelves() {
        assertEquals(
            emptyList(),
            buildShelves(emptyList(), emptyMap(), emptySet(), emptySet(), 0L),
        )
        // Followed/history slugs absent from the catalog are ignored, not fatal.
        val reciters = listOf(qari("lone", "Lone Voice"))
        val byId = build(reciters, followed = setOf("ghost"), history = setOf("phantom"))
        assertFalse("your-voices" in byId, "unknown followed slugs must not fabricate a shelf")
        assertFalse("because-follow" in byId, "no resolvable followed voice means no because-shelf")
    }

    // --- 9. Unknown followed slugs never crash nor leak ---

    @Test
    fun testUnknownFollowedSlugsAreIgnored() {
        val reciters = listOf(
            qari("real-followed", "Real Followed"),
            qari("other", "Other Voice"),
        )
        val byId = build(
            reciters = reciters,
            affinities = mapOf("real-followed" to 0.8f),
            followed = setOf("real-followed", "ghost-voice"),
        )
        assertEquals(listOf("real-followed"), byId["your-voices"]!!.slugs)
        byId.values.flatMap { it.slugs }.forEach { slug ->
            assertFalse(slug == "ghost-voice", "unknown slug must never appear on a shelf")
        }
    }

    // --- 10. Outside your usual + New to explore ---

    @Test
    fun testOutsideUsualAndNewToExplore() {
        val reciters = buildList {
            // Nine high-affinity fillers saturate "For you, daily" (cap 8)…
            (1..9).forEach { add(qari("u-fill-$it", "Filler $it")) }
            // …leaving the low-affinity teacher for exploration…
            add(qari("u-teacher", "Teacher Tariq", country = "Spain", teacher = true))
            // …while the partial-coverage voice is not "well-known".
            add(qari("u-obscure", "Obscure Omar", country = "Spain", surahs = "1,2"))
        }
        val affinities = buildMap {
            (1..9).forEach { put("u-fill-$it", 0.99f - it * 0.01f) }
            put("u-teacher", 0.1f)
            put("u-obscure", 0.1f)
        }
        val byId = build(reciters, affinities, epochDay = 0L)
        assertEquals(8, byId["for-you-daily"]!!.slugs.size)
        assertEquals(
            listOf("u-teacher"),
            byId["outside-usual"]!!.slugs,
            "only the low-affinity well-known voice belongs outside",
        )
        assertEquals(
            listOf("u-fill-9"),
            byId["new-to-explore"]!!.slugs,
            "the leftover well-known voice is new to explore",
        )
        assertFalse("u-obscure" in byId.values.flatMap { it.slugs }, "partial-coverage voice is not well-known")
    }

    // --- Fixtures ---

    private fun richCatalog(): List<Reciter> = buildList {
        add(qari("f-ahmed", "Ahmed Fathi", country = "Egypt"))
        add(qari("f-bilal", "Bilal Karim", country = "Morocco"))
        add(qari("f-cem", "Cem Yilmaz", country = "Turkey"))
        add(qari("h-daoud", "Daoud Nasser", country = "Jordan"))
        add(qari("h-emad", "Emad Saleh", country = "Egypt"))
        add(qari("u-farid", "Farid Adel", country = "Egypt"))
        add(qari("u-gaber", "Gaber Hosni", country = "Egypt"))
        (1..9).forEach { add(qari("u-fill-$it", "Filler $it", country = "France")) }
        add(qari("u-teacher", "Teacher Tariq", country = "Spain", teacher = true))
        add(qari("u-obscure", "Obscure Omar", country = "Spain", surahs = "1,2"))
    }

    private fun richAffinities(): Map<String, Float> = buildMap {
        put("f-ahmed", 0.95f)
        put("f-bilal", 0.8f)
        put("f-cem", 0.7f)
        put("h-daoud", 0.9f)
        put("h-emad", 0.85f)
        put("u-farid", 0.6f)
        put("u-gaber", 0.55f)
        (1..9).forEach { put("u-fill-$it", 0.99f - it * 0.01f) }
        put("u-teacher", 0.1f)
        put("u-obscure", 0.1f)
    }
}
