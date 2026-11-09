package com.ghais.ui.screens.mood

import kotlin.test.Test
import kotlin.test.assertEquals

class MoodCatalogTest {
    @Test
    fun editorialCopyPreservesBundledPlaybackAndIgnoresUnknownMoods() {
        val base = AVAILABLE_MOODS.first()
        val result = MoodCatalog.overlay("""{"moods":[{"slug":"${base.id}","title":"Updated title","subtitle":"Updated copy","category":"Comfort"},{"slug":"unknown","title":"Unknown"}]}""")
        assertEquals(AVAILABLE_MOODS.size, result.size)
        assertEquals("Updated title", result.first().title)
        assertEquals("Updated copy", result.first().subtitle)
        assertEquals(base.audioUrl, result.first().audioUrl)
        assertEquals(base.surahId, result.first().surahId)
    }

    @Test
    fun invalidEditorialFieldsFallBackToBundledValues() {
        val base = AVAILABLE_MOODS.first()
        val result = MoodCatalog.overlay("""{"moods":[{"slug":"${base.id}","title":" ","category":"Unsupported"}]}""")
        assertEquals(base, result.first())
    }
}
