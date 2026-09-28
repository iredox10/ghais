package com.ghais.domain.model

import kotlinx.serialization.Serializable

/**
 * The two devotional collections the app ships alongside the Quran.
 *
 * Both are read-and-listen items: a piece of Arabic, a translation, and a
 * citation. They deliberately share one type because the reading and playing
 * surfaces are identical — only the catalogue, the citation shape and the audio
 * source differ.
 */
enum class DevotionKind(val wire: String, val label: String) {
    /** Supplications (adhkar), each with its own recited-audio clip. */
    DUA("dua", "Duas"),

    /** Quranic verses for reflection. Audio reuses the app's everyayah source. */
    RUQIYAH("ruqiyah", "Ruqiyah");

    companion object {
        fun fromWire(wire: String): DevotionKind =
            entries.firstOrNull { it.wire.equals(wire, ignoreCase = true) } ?: DUA
    }
}

/**
 * One devotional entry.
 *
 * [audioUrl] is nullable on purpose. It holds one clip, or several clips
 * joined with `"|"` for a multi-part entry (the after-wudu dhikr, a Quranic
 * passage) — [DevotionalRepository.playableTracks] splits those into queue
 * rows. A play control that cannot play is worse than no play control, so the
 * UI reads the RESOLVED audio and degrades to read-only when there is none.
 *
 * [surahId]/[startAyah]/[endAyah] are set for ruqiyah only, and drive the
 * everyayah audio URL. Duas leave them at 0, which is what keeps them out of
 * ayah mode and out of every Quran-keyed index in the app — including Quranic
 * duas, whose recitation reuses everyayah through an explicit [audioUrl].
 */
@Serializable
data class DevotionalItem(
    val id: String,
    val kind: DevotionKind,
    val title: String,
    val category: String = "",
    val arabic: String = "",
    val translation: String = "",
    val reference: String = "",
    val reciterName: String = "",
    val audioUrl: String? = null,
    val surahId: Int = 0,
    val startAyah: Int = 0,
    val endAyah: Int = 0,
    val durationMs: Long = 0L
) {
    val hasAudio: Boolean get() = !audioUrl.isNullOrBlank()

    /** True when this item maps onto Quran coordinates, i.e. it is ruqiyah. */
    val isQuranic: Boolean get() = surahId > 0 && startAyah > 0

    /**
     * Adapts into the engine's queue type.
     *
     * `ayahNo = 0` is load-bearing, not a placeholder: `AudioEngine` derives
     * ayah mode from `ayahNo > 0`, so a nonzero value would drag a devotional
     * into the Quran verse pipeline — a catalog lookup, a basmalah header and an
     * ayah rosette. `surahId = 0` keeps it out of the surah-keyed history,
     * stats and favourite indexes for the same reason.
     *
     * The audio URL passes through verbatim, which is what lets a
     * non-everyayah host work: the player bridge parses whatever URL it is
     * given and has no host allowlist.
     */
    fun asTrackItem(): TrackItem = TrackItem(
        reciterSlug = DEVOTIONAL_TRACK_SLUG,
        reciterName = reciterName.ifBlank { kind.label },
        surahId = 0,
        surahNameEn = title.ifBlank { kind.label },
        surahNameAr = "",
        ayahNo = 0,
        audioUrl = audioUrl.orEmpty(),
        textUthmani = arabic,
        durationMs = durationMs
    )
}

/**
 * Sentinel `reciterSlug` for every devotional track.
 *
 * Observers that assume a real reciter (history, stats, the sync layer) are
 * expected to branch on this rather than on `surahId == 0`, because a genuine
 * track can also arrive with an unresolvable slug.
 */
const val DEVOTIONAL_TRACK_SLUG: String = "__devotional"

/** Every ayah in this range, used to build a multi-ayah everyayah URL. */
val DevotionalItem.ayahSpan: Int
    get() = if (isQuranic) (endAyah - startAyah + 1).coerceAtLeast(1) else 0
