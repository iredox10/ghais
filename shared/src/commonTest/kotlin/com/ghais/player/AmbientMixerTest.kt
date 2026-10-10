package com.ghais.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * AmbientMixer pure-helper tests with zero engine.
 *
 * Covered: the engine-free pure parts ([AmbientType.assetKey],
 * [AmbientType.displayName], [AmbientType.videoKeys], [AmbientChannel]
 * defaults, and the `List<AmbientChannel>.selectedAmbient*` extensions).
 *
 * Deliberately NOT covered (blocked — see report): the volume scaling / ducking
 * mix math (`(master * selected.volume * 0.6f).coerceIn(0f, 1f)` plus the
 * muted/no-selection → 0f rule) is inlined in the `AmbientMixer.init`
 * collector. Referencing the `AmbientMixer` object boots its `init`, which
 * collects `AudioEngine.isPlaying` and therefore needs a platform player, so
 * the formula is untestable without an extraction seam. These tests assert the
 * READ mapping behavior only and never touch the singleton.
 */
class AmbientMixerTest {

    private fun channels(vararg enabled: AmbientType): List<AmbientChannel> =
        AmbientType.values().map { AmbientChannel(it, isEnabled = it in enabled) }

    // --- assetKey mappings ---

    @Test
    fun assetKeyMappingMatchesBundledLoops() {
        assertEquals("rain", AmbientType.Rain.assetKey())
        assertEquals("waves", AmbientType.OceanWaves.assetKey())
        assertEquals("birds", AmbientType.Birdsong.assetKey())
        assertEquals("wind", AmbientType.GentleWind.assetKey())
        assertEquals("river", AmbientType.River.assetKey())
        assertEquals("crickets", AmbientType.NightCrickets.assetKey())
        assertEquals("fire", AmbientType.Fire.assetKey())
        assertEquals("cat", AmbientType.Cat.assetKey())
        assertEquals("owl", AmbientType.Owl.assetKey())
        assertEquals("whale", AmbientType.Whale.assetKey())
        assertEquals("thunder", AmbientType.Thunder.assetKey())
        assertEquals("storm", AmbientType.Storm.assetKey())
        assertEquals("train", AmbientType.Train.assetKey())
    }

    @Test
    fun forestStreamAliasesRiverAssetKey_ReadBehavior() {
        // READ: ForestStream and River share the "river" loop file.
        assertEquals(AmbientType.River.assetKey(), AmbientType.ForestStream.assetKey())
        assertEquals("river", AmbientType.ForestStream.assetKey())
    }

    // --- displayName ---

    @Test
    fun displayNameDistinguishesStreamFromRiver() {
        assertEquals("Stream", AmbientType.ForestStream.displayName())
        assertEquals("River", AmbientType.River.displayName())
        assertEquals("Rain", AmbientType.Rain.displayName())
        assertEquals("Waves", AmbientType.OceanWaves.displayName())
    }

    // --- videoKeys ---

    @Test
    fun audioOnlyTypesHaveNoVideoKeys() {
        // READ: empty list = audio-only glow fallback (Owl, Whale, Storm, Train).
        assertTrue(AmbientType.Owl.videoKeys().isEmpty())
        assertTrue(AmbientType.Whale.videoKeys().isEmpty())
        assertTrue(AmbientType.Storm.videoKeys().isEmpty())
        assertTrue(AmbientType.Train.videoKeys().isEmpty())
    }

    @Test
    fun videoTypesExposeTheirClips() {
        assertEquals(listOf("Rain1", "Rain2"), AmbientType.Rain.videoKeys())
        assertEquals(listOf("fire1", "fire2"), AmbientType.Fire.videoKeys())
        assertEquals(listOf("Night_Ambient"), AmbientType.NightCrickets.videoKeys())
    }

    // --- AmbientChannel defaults ---

    @Test
    fun channelDefaultsToDisabledAtHalfVolume() {
        val channel = AmbientChannel(AmbientType.Rain)
        assertFalse(channel.isEnabled)
        assertEquals(0.5f, channel.volume)
    }

    // --- selectedAmbient* extensions (pure) ---

    @Test
    fun selectedChannelIsNullWhenNoneEnabled() {
        val list = channels()
        assertNull(list.selectedAmbientChannel)
        assertNull(list.selectedAmbientType)
    }

    @Test
    fun selectedChannelReturnsFirstEnabled() {
        val list = channels(AmbientType.Fire, AmbientType.Rain)
        // First in AmbientType.values() order among the enabled ones.
        val expected = list.first { it.type == AmbientType.Rain }
        assertEquals(expected, list.selectedAmbientChannel)
        assertEquals(AmbientType.Rain, list.selectedAmbientType)
    }
}
