package com.ghais.ui.screens.reciters.personal

/**
 * AGENT P4 — personalized section ordering for the Reciters tab.
 *
 * Stable section ids for the Reciters tab. Each entry is the identity of one
 * vertical block; [orderSections] decides the display order from lightweight
 * engagement signals. The enum entries themselves are the stable ids — use
 * them (or derived strings) as `LazyColumn` item keys when the screen adopts
 * ordered rendering.
 *
 * Mapping to today's [RecitersScreen](com.ghais.ui.screens.reciters.RecitersScreen)
 * layout (`LazyColumn`, top to bottom):
 *
 * | SectionId    | Today in RecitersScreen                                  |
 * |--------------|----------------------------------------------------------|
 * | SEARCH       | Engraved search field (inside the header item, unkeyed)  |
 * | FILTERS      | `FilterChipsRow` (taste-axis filters, A2 — unkeyed item) |
 * | VOICE_OF_DAY | `VoiceOfDayCard`, key `"noir_voice_of_day"`              |
 * | CHARTS       | `TopQarisRail`, key `"noir_top_qaris_rail"`              |
 * | DECK         | `DeckEntryCard` (gated on `HookFlags.deckEnabled`)       |
 * | NATIONS      | `NoirNationReel` per nation, key `"noir_reel_$nation"`   |
 * | YOUR_VOICES  | NEW — followed-voices rail (no widget yet)               |
 * | FOR_YOU      | NEW — taste/history picks rail (no widget yet)           |
 *
 * The editorial header (`NoirRecitersHeader`) is intentionally NOT a section:
 * it stays pinned above [SEARCH] regardless of ordering.
 */
enum class SectionId {
    SEARCH,
    FOR_YOU,
    YOUR_VOICES,
    DECK,
    VOICE_OF_DAY,
    CHARTS,
    FILTERS,
    NATIONS,
}

/**
 * Pure personalized ordering for the Reciters tab. No repository, no I/O, no
 * clock, no randomness — safe to call from any thread inside `remember` and
 * trivially unit-testable.
 *
 * Rules (in priority order):
 * 1. [SectionId.SEARCH] is always first; [SectionId.NATIONS] is always last.
 *    Both are structural guarantees, not signal-dependent.
 * 2. [SectionId.YOUR_VOICES] comes first after search when the user follows
 *    at least one reciter ([hasFollows]).
 * 3. [SectionId.FOR_YOU] follows the voices block (or search when there are
 *    no follows) when there is any personalization confidence: playback
 *    history ([hasHistory]) or a completed taste quiz ([hasTaste]).
 * 4. [SectionId.DECK] is promoted above the editorial block while it is still
 *    todo (`!deckDoneToday`) and demoted below it once done — resting just
 *    above [SectionId.FILTERS] so the filters stay adjacent to the
 *    [SectionId.NATIONS] reels they control.
 * 5. Fresh [SectionId.VOICE_OF_DAY] ([isNewDay]) outranks [SectionId.CHARTS];
 *    a stale voice sinks below the charts.
 * 6. Cold start (no follows, no history, no taste, fresh day, deck todo) is
 *    exactly `SEARCH, DECK, VOICE_OF_DAY, CHARTS, FILTERS, NATIONS` — the
 *    personalized rails ([SectionId.YOUR_VOICES], [SectionId.FOR_YOU]) are
 *    omitted until there is signal to fill them, rather than rendered empty.
 *
 * Every section appears at most once; the result always starts with SEARCH
 * and ends with NATIONS.
 */
fun orderSections(
    hasFollows: Boolean,
    hasHistory: Boolean,
    hasTaste: Boolean,
    isNewDay: Boolean,
    deckDoneToday: Boolean,
): List<SectionId> {
    val middle = mutableListOf<SectionId>()

    // Personalized block: followed voices first, taste/history picks second.
    if (hasFollows) middle += SectionId.YOUR_VOICES
    if (hasHistory || hasTaste) middle += SectionId.FOR_YOU

    // Deck todo: promote above the editorial block.
    if (!deckDoneToday) middle += SectionId.DECK

    // Editorial block: a fresh voice-of-day outranks charts; a stale one
    // sinks below them.
    if (isNewDay) middle += SectionId.VOICE_OF_DAY
    middle += SectionId.CHARTS
    if (!isNewDay) middle += SectionId.VOICE_OF_DAY

    // Deck done: rest below the editorial block, above the filter tail.
    if (deckDoneToday) middle += SectionId.DECK

    // Filters anchor the tail so they stay adjacent to the nation reels.
    middle += SectionId.FILTERS

    return listOf(SectionId.SEARCH) + middle + listOf(SectionId.NATIONS)
}
