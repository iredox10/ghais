package com.ghais.hifz

/**
 * A verse in another Surah (or later in the same Surah) that shares wording
 * or structure (Mutashabihat) with the queried ayah.
 */
data class MutashabihatMatch(
    val otherSurahId: Int,
    val otherAyahNo: Int,
    val note: String
)

private val WAQF_MARKS: Set<Char> = setOf(
    '\u06D6', // ۖ  Small High Ligature Sad with Lam with Alef Maksura
    '\u06D7', // ۗ  Small High Ligature Qaf with Lam with Alef Maksura
    '\u06D8', // ۘ  Small High Meem Initial Form
    '\u06D9', // ۙ  Small High Lam Alef
    '\u06DA', // ۚ  Small High Jeem
    '\u06DB', // ۛ  Small High Three Dots
    '\u06DC'  // ۜ  Small High Seen
)

private const val MIN_SPLIT_AYAH_LENGTH = 20

/**
 * Splits a long Arabic ayah at natural Quranic waqf marks (`ۖ`, `ۗ`, `ۘ`, `ۙ`,
 * `ۚ`, `ۛ`, `ۜ`), keeping each waqf mark attached to its preceding phrase.
 *
 * Returns a single-element list when the ayah is short or contains no waqf marks,
 * and an empty list when [arabicText] is blank.
 */
fun splitAyahByWaqf(arabicText: String): List<String> {
    val trimmed = arabicText.trim()
    if (trimmed.isEmpty()) return emptyList()
    if (trimmed.length < MIN_SPLIT_AYAH_LENGTH) return listOf(trimmed)
    if (trimmed.none { it in WAQF_MARKS }) return listOf(trimmed)

    val phrases = mutableListOf<String>()
    val current = StringBuilder()

    for (ch in trimmed) {
        current.append(ch)
        if (ch in WAQF_MARKS) {
            val phrase = current.toString().trim()
            if (phrase.isNotEmpty()) {
                phrases.add(phrase)
            }
            current.clear()
        }
    }

    val tail = current.toString().trim()
    if (tail.isNotEmpty()) {
        phrases.add(tail)
    }

    return if (phrases.size <= 1) listOf(trimmed) else phrases
}

private typealias AyahRef = Pair<Int, Int>

private val MUTASHABIHAT_INDEX: Map<AyahRef, List<MutashabihatMatch>> by lazy {
    val map = LinkedHashMap<AyahRef, MutableList<MutashabihatMatch>>()

    fun addSymmetricPair(
        surahA: Int,
        ayahA: Int,
        surahB: Int,
        ayahB: Int,
        note: String
    ) {
        map.getOrPut(surahA to ayahA) { mutableListOf() }
            .add(MutashabihatMatch(surahB, ayahB, note))
        map.getOrPut(surahB to ayahB) { mutableListOf() }
            .add(MutashabihatMatch(surahA, ayahA, note))
    }

    fun addGroup(refs: List<AyahRef>, note: String) {
        for (source in refs) {
            val bucket = map.getOrPut(source) { mutableListOf() }
            for (target in refs) {
                if (target != source) {
                    bucket.add(MutashabihatMatch(target.first, target.second, note))
                }
            }
        }
    }

    // Classic cross-surah and intra-surah Mutashabihat pairs
    addSymmetricPair(2, 35, 7, 19, "Adam & spouse in Paradise: \"faminha\" vs \"minha\"")
    addSymmetricPair(2, 58, 7, 161, "Entering the town: \"wa qulu hittah\" vs \"qulu hittah\"")
    addSymmetricPair(2, 60, 7, 160, "Twelve springs gushing forth for the tribes")
    addSymmetricPair(2, 134, 2, 141, "\"Tilka ummatun qad khalat\" refrain")
    addSymmetricPair(2, 47, 2, 122, "\"Ya Bani Isra'ila-dhkuru ni'mati\" opening")
    addSymmetricPair(2, 23, 10, 38, "Challenge to produce a Surah like it")
    addSymmetricPair(2, 163, 2, 255, "\"Wa ilahukum ilahun wahid / La ilaha illa Huwa\"")
    addSymmetricPair(6, 151, 17, 31, "Prohibition of killing children out of poverty")
    addSymmetricPair(6, 152, 17, 34, "Guarding the orphan's property until maturity")
    addSymmetricPair(4, 1, 7, 189, "Creation from a single soul (nafsin wahidah)")
    addSymmetricPair(109, 3, 109, 5, "\"Wa la antum 'abiduna ma a'bud\" refrain")
    addSymmetricPair(56, 74, 69, 52, "\"Fasabbih bismi Rabbikal-'Azim\"")
    addSymmetricPair(68, 52, 81, 27, "\"In huwa illa dhikrun lil-'alamin\"")

    // Ash-Shu'ara (26) prophetic story refrains
    addGroup(
        listOf(26 to 8, 26 to 67, 26 to 103, 26 to 121, 26 to 139, 26 to 158, 26 to 174, 26 to 190),
        "Ash-Shu'ara refrain: \"Inna fi dhalika la-ayah\""
    )
    addGroup(
        listOf(26 to 9, 26 to 68, 26 to 104, 26 to 122, 26 to 140, 26 to 159, 26 to 175, 26 to 191),
        "Ash-Shu'ara refrain: \"Wa inna Rabbaka la-Huwal-'Azizur-Rahim\""
    )
    addGroup(
        listOf(26 to 108, 26 to 110, 26 to 126, 26 to 131, 26 to 144, 26 to 150, 26 to 163, 26 to 179),
        "Ash-Shu'ara refrain: \"Fattaqullaha wa ati'un\""
    )

    // Al-Mursalat (77) refrain
    addGroup(
        listOf(77 to 15, 77 to 19, 77 to 24, 77 to 28, 77 to 34, 77 to 37, 77 to 40, 77 to 45, 77 to 47, 77 to 49),
        "Al-Mursalat refrain: \"Waylun yawma'idhin lil-mukadhdhibin\""
    )

    map.mapValues { (_, list) -> list.distinct() }
}

/**
 * Returns curated Mutashabihat (similar verse) matches for ([surahId], [ayahNo]),
 * or an empty list if none are indexed.
 */
fun mutashabihatFor(surahId: Int, ayahNo: Int): List<MutashabihatMatch> =
    MUTASHABIHAT_INDEX[surahId to ayahNo].orEmpty()
