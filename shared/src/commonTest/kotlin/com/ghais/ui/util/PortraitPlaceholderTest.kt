package com.ghais.ui.util

import com.ghais.ui.screens.reciters.monogramForReciter
import com.ghais.ui.screens.reciters.photoForSlug
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertEquals

class PortraitPlaceholderTest {
    @Test fun generatedDefaultArtworkUsesInitialsInstead() {
        assertNull(realImageUrlOrNull("https://lh3.googleusercontent.com/aida-public/default-generated-image"))
        assertEquals("M", monogramForReciter("Mahmoud Khalil Al-Husary"))
        assertNull(photoForSlug("husary"))
    }
}
