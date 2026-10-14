package com.ghais.ui.screens.reciters.taste

import com.ghais.domain.model.Reciter
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Unit tests for the pure [scoreReciter] heuristic (no engine, no Settings).
 */
class TasteScoringTest {

    private fun reciter(
        slug: String,
        tempo: String = "Medium",
        style: String = "Murattal",
        riwayah: String = "Hafs",
        country: String = "Kuwait"
    ): Reciter = Reciter(
        slug = slug,
        nameEn = slug,
        nameAr = "اختبار",
        riwayah = riwayah,
        style = style,
        tempo = tempo,
        country = country
    )

    @Test
    fun testEmptyTasteScoresNeutralBaseline() {
        val r = reciter("mishary")
        val score = scoreReciter(r, TasteProfile())
        // 0.5 baseline + Hafs familiarity whisper (≈0.04); tolerance avoids
        // 1-ulp brittleness between `0.5f + 0.04f` and the `0.54f` literal.
        assertTrue(score in 0.53f..0.55f, "expected neutral baseline, got $score")
    }

    @Test
    fun testSlowPacePrefersSlowTempoOverFast() {
        val taste = TasteProfile(pace = Pace.SLOW)
        val slow = reciter("abdul-basit-mujawwad", tempo = "Slow & Emotive", style = "Mujawwad", country = "Egypt")
        val fast = reciter("brisk-voice", tempo = "Fast & Expressive", style = "Modern Murattal", country = "Kuwait")
        assertTrue(
            scoreReciter(slow, taste) > scoreReciter(fast, taste),
            "SLOW pace must rank a slow mujawwad voice above a fast modern one"
        )
    }

    @Test
    fun testFlowingPacePrefersFlowingTempo() {
        val taste = TasteProfile(pace = Pace.FLOWING)
        val flowing = reciter("flowing-voice", tempo = "Flowing", style = "Modern Murattal")
        val slow = reciter("slow-voice", tempo = "Deliberate & Slow", style = "Mujawwad")
        assertTrue(scoreReciter(flowing, taste) > scoreReciter(slow, taste))
    }

    @Test
    fun testCalmMoodPrefersMujawwadOverTeacher() {
        val taste = TasteProfile(mood = Mood.CALM)
        val mujawwad = reciter("abdul-basit-mujawwad", style = "Mujawwad", country = "Egypt")
        val teacher = reciter("husary-muallim", style = "Teacher (Mu'allim)", country = "Egypt")
        assertTrue(
            scoreReciter(mujawwad, taste) > scoreReciter(teacher, taste),
            "CALM must prefer the mujawwad school over the teacher school"
        )
    }

    @Test
    fun testFocusMoodPrefersTeacherSchool() {
        val taste = TasteProfile(mood = Mood.FOCUS)
        val teacher = reciter("husary-muallim", style = "Teacher (Mu'allim)", tempo = "Medium")
        val taraweeh = reciter("taraweeh-voice", style = "Taraweeh", tempo = "Fast & Expressive", country = "Saudi Arabia")
        assertTrue(scoreReciter(teacher, taste) > scoreReciter(taraweeh, taste))
    }

    @Test
    fun testLikedSlugBeatsSkippedSlugForIdenticalVoices() {
        val base = reciter("mishary")
        val likedTaste = TasteProfile(likedSlugs = setOf("mishary"))
        val skippedTaste = TasteProfile(skippedSlugs = setOf("mishary"))
        assertTrue(scoreReciter(base, likedTaste) > scoreReciter(base, TasteProfile()))
        assertTrue(scoreReciter(base, skippedTaste) < scoreReciter(base, TasteProfile()))
        assertTrue(scoreReciter(base, likedTaste) > scoreReciter(base, skippedTaste))
    }

    @Test
    fun testVoiceVerdictOutranksPaceHeuristic() {
        // A skipped slug with a perfectly matching pace must still lose to a
        // liked slug with a mismatched pace: explicit picks always win.
        val slowSkipped = reciter("slow-voice", tempo = "Deliberate & Slow", style = "Mujawwad")
        val fastLiked = reciter("fast-voice", tempo = "Fast & Powerful", style = "Modern Murattal")
        val taste = TasteProfile(
            pace = Pace.SLOW,
            skippedSlugs = setOf("slow-voice"),
            likedSlugs = setOf("fast-voice")
        )
        assertTrue(scoreReciter(fastLiked, taste) > scoreReciter(slowSkipped, taste))
    }

    @Test
    fun testScoresAlwaysClampedToUnitRange() {
        val maximal = TasteProfile(
            pace = Pace.SLOW,
            mood = Mood.CALM,
            likedSlugs = setOf("perfect"),
            completed = true
        )
        val perfect = reciter(
            "perfect",
            tempo = "Slow & Tearful",
            style = "Mujawwad",
            country = "Egypt"
        )
        val minimal = TasteProfile(
            pace = Pace.FLOWING,
            mood = Mood.UPLIFTING,
            skippedSlugs = setOf("worst"),
            completed = true
        )
        val worst = reciter("worst", tempo = "Deliberate & Slow", style = "Mujawwad", country = "Egypt")
        val hi = scoreReciter(perfect, maximal)
        val lo = scoreReciter(worst, minimal)
        assertTrue(hi in 0f..1f, "maximal score out of range: $hi")
        assertTrue(lo in 0f..1f, "minimal score out of range: $lo")
        assertTrue(hi > lo)
    }
}
