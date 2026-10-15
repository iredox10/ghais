package com.ghais.ui.screens.reciters.personal

/**
 * Personalized section ordering for the Reciters tab.
 *
 * Stable section ids for the Reciters tab. Each entry is the identity of one
 * vertical block; [orderSections] decides the display order from lightweight
 * engagement signals. The enum entries themselves are the stable ids — use
 * them (or derived strings) as `LazyColumn` item keys.
 *
 * Editorial top is FIXED (Spotify Home rule: one hero, then ghosts):
 * SEARCH header → DECK (Today's voices hero) → VOICE_OF_DAY (Yesterday's
 * most heard) → CHARTS → personalized rails → NATIONS last. Refinement
 * filters live on the search screen, not in this scroll.
 *
 * Mapping to [RecitersScreen](com.ghais.ui.screens.reciters.RecitersScreen)
 * (`LazyColumn`, top to bottom):
 *
 * | SectionId    | Widget in RecitersScreen                               |
 * |--------------|--------------------------------------------------------|
 * | SEARCH       | Engraved search field (header item)                    |
 * | DECK         | `DeckEntryCard` (gated on `HookFlags.deckEnabled`)     |
 * | VOICE_OF_DAY | `YesterdayMostHeardSection` (+ voice fallback)         |
 * | CHARTS       | `TopQarisRail`                                         |
 * | YOUR_VOICES  | Followed-voices shelf                                  |
 * | FOR_YOU      | Taste/history shelves                                  |
 * | RITUAL       | `WeeklyRitualRow` (own section, was nested in FOR_YOU) |
 * | NATIONS      | `NoirNationReel` per nation                            |
 *
 * The editorial header (`NoirRecitersHeader`) is intentionally NOT a section:
 * it stays pinned above [SEARCH] regardless of ordering.
 */
enum class SectionId {
    SEARCH,
    DECK,
    VOICE_OF_DAY,
    CHARTS,
    YOUR_VOICES,
    FOR_YOU,
    RITUAL,
    NATIONS,
}

/**
 * Fixed editorial order for the Reciters tab. No repository, no I/O, no
 * clock, no randomness — safe to call from any thread inside `remember` and
 * trivially unit-testable.
 *
 * Rules:
 * 1. [SectionId.SEARCH] is always first; [SectionId.NATIONS] is always last.
 * 2. [SectionId.DECK], [SectionId.VOICE_OF_DAY], [SectionId.CHARTS] follow
 *    in that fixed order (the hero block).
 * 3. [SectionId.YOUR_VOICES] appears when the user follows at least one
 *    reciter ([hasFollows]).
 * 4. [SectionId.FOR_YOU] appears when there is personalization confidence:
 *    playback history ([hasHistory]) or a completed taste quiz ([hasTaste]).
 * 5. [SectionId.RITUAL] always appears (the row self-hides when its picks
 *    are empty); it stands alone instead of nesting inside FOR_YOU so the
 *    weekly ritual survives low-confidence weeks.
 * 6. Cold start (no follows, no history, no taste) is exactly
 *    `SEARCH, DECK, VOICE_OF_DAY, CHARTS, RITUAL, NATIONS`.
 *
 * Every section appears at most once; the result always starts with SEARCH
 * and ends with NATIONS.
 */
fun orderSections(
    hasFollows: Boolean,
    hasHistory: Boolean,
    hasTaste: Boolean,
): List<SectionId> {
    val middle = mutableListOf(
        SectionId.DECK,
        SectionId.VOICE_OF_DAY,
        SectionId.CHARTS,
    )
    if (hasFollows) middle += SectionId.YOUR_VOICES
    if (hasHistory || hasTaste) middle += SectionId.FOR_YOU
    middle += SectionId.RITUAL
    return listOf(SectionId.SEARCH) + middle + listOf(SectionId.NATIONS)
}
