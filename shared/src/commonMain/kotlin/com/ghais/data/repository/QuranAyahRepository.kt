package com.ghais.data.repository

import com.ghais.data.seed.QuranData
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int

@Serializable
data class AyahVerse(
    val surahId: Int,
    val ayahNo: Int,
    val textUthmani: String,
    val translation: String = "",
    val transliteration: String = "",
    val isPlaceholder: Boolean = false
)

/**
 * Unified repository providing authentic Arabic Uthmani text and translations
 * for all 114 Surahs, edition-aware: the translation edition is owned by
 * [TranslationPrefs] and read from it at call time, never mirrored here.
 * Supports:
 * 1. Bundled offline seed data for popular and essential Surahs — `en.sahih`
 *    text only, so it applies solely while [TranslationPrefs.DEFAULT_EDITION]
 *    is the active edition.
 * 2. On-demand online fetching from AlQuran Cloud API with local in-memory
 *    caching, requesting `quran-uthmani,<active edition>`.
 * 3. Edition-scoped disk persistence (`ayah_text_v3_<edition>_<surah>` plus a
 *    per-edition index) so switching translations never clobbers another
 *    edition's downloaded text; pre-v3 blobs remain readable for the default
 *    edition to avoid refetching for existing users.
 * 4. Graceful fallback for offline playback.
 *
 * Call [onTranslationChanged] after [TranslationPrefs.setEdition] so caches
 * and in-flight fetches follow the switch.
 */
object QuranAyahRepository {

    const val BASMALAH = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"

    fun hasBasmalahHeader(surahId: Int): Boolean = surahId != 1 && surahId != 9

    fun cleanQuranicText(raw: String): String {
        var s = raw.replace("\u25CC", "").replace("\u200B", "").replace("\uFEFF", "")
        // Uthmani hamza-less / pause annotation marks. They carry no recitation
        // meaning, and `QCF_Hafs` draws all three as the SAME large filled disc
        // ringed by a dashed circle (glyph `uni06DF`/`uni06E3`/`uni06EB`, each
        // 1255x1255 units — 3.5x the size of the sibling "small" marks). Inline
        // in a verse they read as a stray circle dropped into the text, so drop
        // them. U+06DE (rub el hizb) and U+06E9 (sajdah) are meaningful and
        // render as proper ornaments, so those are deliberately kept.
        s = s.replace("\u06DF", "").replace("\u06E3", "").replace("\u06EB", "")
        s = s.replace("لْ  ء", "لْء").replace("لْ ء", "لْء")
        s = s.replace("ء  ا", "ءا").replace("ء ا", "ءا")
        s = s.replace(Regex(" +(?=[\u064B-\u065F\u0670\u06D6-\u06ED])"), "")
        s = s.replace(Regex(" {2,}"), " ")
        return s.trim()
    }

    private fun isTashkeel(c: Char): Boolean {
        val v = c.code
        return (v in 0x064B..0x065F) || v == 0x0670 || (v in 0x06D6..0x06ED)
    }

    /**
     * Alef wasla (U+0671) and plain alef are the same letter for a prefix test.
     *
     * The Uthmani edition spells the basmalah `بِسْمِ ٱللَّهِ …` — wasla alef on
     * the elided `ال` of every word after `بسم` — while the haystack keeps its
     * own wasla alef. Comparing the two literally can therefore never match, and
     * a surah whose first ayah the edition opens with a basmalah (103, 112)
     * would keep it and recite it a second time on top of the ayah audio. The
     * mapping is 1:1, so the caller's index arithmetic is unaffected.
     */
    private fun normalizeAlef(c: Char): Char =
        if (c.code == 0x0671) 'ا' else c

    fun stripBasmalahPrefix(surahId: Int, ayahNo: Int, text: String): String {
        if (surahId == 1 || surahId == 9 || ayahNo != 1) return text
        val stripped = StringBuilder()
        val origIndices = mutableListOf<Int>()
        for (i in text.indices) {
            val c = text[i]
            if (isTashkeel(c)) continue
            stripped.append(normalizeAlef(c))
            origIndices.add(i)
        }
        val bare = "بسم الله الرحمن الرحيم"
        val s = stripped.toString()
        for (lead in 0..2) {
            if (s.length >= lead + bare.length && s.startsWith(bare, lead)) {
                val lastStrippedIdx = lead + bare.length - 1
                if (lastStrippedIdx >= origIndices.size) continue
                val cutOriginal = origIndices[lastStrippedIdx] + 1
                if (cutOriginal >= text.length) return text
                val rest = text.substring(cutOriginal).trimStart()
                if (rest.isEmpty()) return text
                // The cut lands right after the basmalah's last letter, so the
                // vowel that followed it in the joined string is now leading and
                // orphaned — a mark floating at the start of the verse. Drop any
                // leading tashkeel (and the space before it) before returning.
                return rest.trimStart { it.isWhitespace() || isTashkeel(it) }
            }
        }
        return text
    }

    fun sanitizeVerseText(surahId: Int, ayahNo: Int, raw: String): String {
        val cleaned = cleanQuranicText(raw)
        val stripped = stripBasmalahPrefix(surahId, ayahNo, cleaned)
        return cleanQuranicText(stripped)
    }

    // ------------------------------------------------------------------
    // Bundled Seeds for Instant Offline Access
    // en.sahih text: only ever served while that is the active edition
    // (see seedBundledVerses / resolveAyahs) — a French or Indonesian
    // reader must never be shown these translations.
    // ------------------------------------------------------------------
    private val BUNDLED_SEEDS: Map<Int, List<AyahVerse>> = mapOf(
        // Surah 1: Al-Fatihah
        1 to listOf(
            AyahVerse(1, 1, "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", "In the name of Allah, the Entirely Merciful, the Especially Merciful.", "Bismillāhir-Raḥmānir-Raḥīm"),
            AyahVerse(1, 2, "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ", "[All] praise is [due] to Allah, Lord of the worlds -", "Al-ḥamdu lillāhi Rabbil-ʻālamīn"),
            AyahVerse(1, 3, "ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", "The Entirely Merciful, the Especially Merciful,", "Ar-Raḥmānir-Raḥīm"),
            AyahVerse(1, 4, "مَٰلِكِ يَوْمِ ٱلدِّينِ", "Sovereign of the Day of Recompense.", "Māliki Yawmid-Dīn"),
            AyahVerse(1, 5, "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", "It is You we worship and You we ask for help.", "Iyyāka naʻbudu wa-iyyāka nastaʻīn"),
            AyahVerse(1, 6, "ٱهْدِنَا ٱلصِّرَٰطَ ٱلْمُسْتَقِيمَ", "Guide us to the straight path -", "Ihdinaṣ-ṣirāṭal-mustaqīm"),
            AyahVerse(1, 7, "صِرَٰطَ ٱلَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ ٱلْمَغْضُوبِ عَلَيْهِمْ وَلَا ٱلضَّآلِّينَ", "The path of those upon whom You have bestowed favor, not of those who have evoked [Your] anger or of those who are astray.", "Ṣirāṭalladhīna anʻamta ʻalayhim ghayril-maghḍūbi ʻalayhim walāḍ-ḍāllīn")
        ),
        // Surah 112: Al-Ikhlas
        112 to listOf(
            AyahVerse(112, 1, "قُلْ هُوَ ٱللَّهُ أَحَدٌ", "Say, \"He is Allah, [who is] One,", "Qul Huwallāhu Aḥad"),
            AyahVerse(112, 2, "ٱللَّهُ ٱلصَّمَدُ", "Allah, the Eternal Refuge.", "Allāhuṣ-Ṣamad"),
            AyahVerse(112, 3, "لَمْ يَلِدْ وَلَمْ يُولَدْ", "He neither begets nor is born,", "Lam yalid walam yūlad"),
            AyahVerse(112, 4, "وَلَمْ يَكُن لَّهُۥ كُفُوًا أَحَدٌۢ", "Nor is there to Him any equivalent.\"", "Walam yakul-lahū kufuwan aḥad")
        ),
        // Surah 113: Al-Falaq
        113 to listOf(
            AyahVerse(113, 1, "قُلْ أَعُوذُ بِرَبِّ ٱلْفَلَقِ", "Say, \"I seek refuge in the Lord of daybreak", "Qul aʻūdhu bi-Rabbil-falaq"),
            AyahVerse(113, 2, "مِن شَرِّ مَا خَلَقَ", "From the evil of that which He created", "Min sharri mā khalaq"),
            AyahVerse(113, 3, "وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ", "And from the evil of darkness when it settles", "Wa-min sharri ghāsiqin idhā waqab"),
            AyahVerse(113, 4, "وَمِن شَرِّ ٱلنَّفَّٰثَٰتِ فِى ٱلْعُقَدِ", "And from the evil of the blowers in knots", "Wa-min sharrin-naffāthāti fīl-ʻuqad"),
            AyahVerse(113, 5, "وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ", "And from the evil of an envier when he envies.\"", "Wa-min sharri ḥāsidin idhā ḥasad")
        ),
        // Surah 114: An-Nas
        114 to listOf(
            AyahVerse(114, 1, "قُلْ أَعُوذُ بِرَبِّ ٱلنَّاسِ", "Say, \"I seek refuge in the Lord of mankind,", "Qul aʻūdhu bi-Rabbin-nās"),
            AyahVerse(114, 2, "مَلِكِ ٱلنَّاسِ", "The Sovereign of mankind,", "Malikin-nās"),
            AyahVerse(114, 3, "إِلَٰهِ ٱلنَّاسِ", "The God of mankind,", "Ilāhin-nās"),
            AyahVerse(114, 4, "مِن شَرِّ ٱلْوَسْوَاسِ ٱلْخَنَّاسِ", "From the evil of the retreating whisperer -", "Min sharril-waswāsil-khannās"),
            AyahVerse(114, 5, "ٱلَّذِى يُوَسْوِسُ فِى صُدُورِ ٱلنَّاسِ", "Who whispers into the breasts of mankind -", "Alladhī yuwaswisu fī ṣudūrin-nās"),
            AyahVerse(114, 6, "مِنَ ٱلْجِنَّةِ وَٱلنَّاسِ", "From among the jinn and mankind.\"", "Minal-jinnati wan-nās")
        ),
        // Surah 108: Al-Kawthar
        108 to listOf(
            AyahVerse(108, 1, "إِنَّآ أَعْطَيْنَٰكَ ٱلْكَوْثَرَ", "Indeed, We have granted you, [O Muhammad], al-Kawthar.", "Innā aʻṭaynākal-Kawthar"),
            AyahVerse(108, 2, "فَصَلِّ لِرَبِّكَ وَٱنْحَرْ", "So pray to your Lord and sacrifice [to Him alone].", "Fa-ṣalli li-Rabbika wan-ḥar"),
            AyahVerse(108, 3, "إِنَّ شَانِئَكَ هُوَ ٱلْأَبْتَرُ", "Indeed, your enemy is the one cut off.", "Inna shāni-aka huwal-abtar")
        ),
        // Surah 18: Al-Kahf (first 5 ayahs)
        18 to listOf(
            AyahVerse(18, 1, "ٱلْحَمْدُ لِلَّهِ ٱلَّذِىٓ أَنزَلَ عَلَىٰ عَبْدِهِ ٱلْكِتَٰبَ وَلَمْ يَجْعَل لَّهُۥ عِوَجَاۜ", "[All] praise is [due] to Allah, who has sent down upon His Servant the Book and has not made therein any deviance.", "Al-ḥamdu lillāhilladhī anzala ʻalā ʻabdihil-Kitāba walam yajʻal lahū ʻiwajā"),
            AyahVerse(18, 2, "قَيِّمًا لِّيُنذِرَ بَأْسًا شَدِيدًا مِّن لَّدُنْهُ وَيُبَشِّرَ ٱلْمُؤْمِنِينَ ٱلَّذِينَ يَعْمَلُونَ ٱلصَّٰلِحَٰتِ أَنَّ لَهُمْ أَجْرًا حَسَنًا", "[He has made it] straight, to warn of severe punishment from Him and to give good tidings to the believers who do righteous deeds that they will have a good reward", "Qayyiman liyundhira ba'san shadīdan mil-ladunhu wayubashshiral-mu'minīnalladhīna yaʻmalūnaṣ-ṣāliḥāti anna lahum ajran ḥasanā"),
            AyahVerse(18, 3, "مَّٰكِثِينَ فِيهِ أَبَدًا", "In which they will remain forever", "Mākithīna fīhi abadā"),
            AyahVerse(18, 4, "وَيُنذِرَ ٱلَّذِينَ قَالُوا۟ ٱتَّخَذَ ٱللَّهُ وَلَدًا", "And to warn those who say, \"Allah has taken a son.\"", "Wayundhiralladhīna qāluttakhadhallāhu waladā"),
            AyahVerse(18, 5, "مَّا لَهُم بِهِۦ مِنْ عِلْمٍ وَلَا لِءَابَآئِهِمْ ۚ كَبُرَتْ كَلِمَةً تَخْرُجُ مِنْ أَفْوَٰهِهِمْ ۚ إِن يَقُولُونَ إِلَّا كَذِبًا", "They have no knowledge of it, nor had their fathers. Grave is the word that comes out of their mouths; they speak not except a lie.", "Mā lahum bihī min ʻilmin walā li-ābā-ihim kaburat kalimatan takhruju min afwāhihim iy-yaqūlūna illā kadhibā")
        )
    )

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mutex = Mutex()
    private val cache = mutableMapOf<Int, List<AyahVerse>>()

    /**
     * Arabic-only snapshot of the verses a translation switch retired.
     *
     * Deliberately NOT part of [cache]: `resolveAyahs` treats a complete cache
     * entry as "already have this surah" and skips the network, so parking
     * translation-less verses there would silently disable the refetch and the
     * reader would stare at a permanently blank translation. This map is read
     * by [getAyahImmediate] alone — the display path — so the verse keeps
     * rendering while the new edition is still on its way, and disappears the
     * moment real data for that surah lands (see the removal in
     * `resolveAyahs`). Arabic is edition-independent, so nothing here can ever
     * show the wrong language.
     */
    private val rememberedArabic = mutableMapOf<Int, List<AyahVerse>>()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var customHttpClient: HttpClient? = null

    private val defaultHttpClient: HttpClient? by lazy {
        try {
            HttpClient()
        } catch (_: Exception) {
            null
        }
    }

    private val httpClient: HttpClient?
        get() = customHttpClient ?: defaultHttpClient

    /**
     * Optional hook to inject a configured [HttpClient] (e.g. custom timeout,
     * OkHttp engine, or a test mock); pass null to fall back to the lazily
     * built default client. Mirrors [QuranTafseerRepository.configureHttpClient].
     */
    fun configureHttpClient(client: HttpClient?) {
        customHttpClient = client
    }

    private val _loadingSurahs = MutableStateFlow<Set<Int>>(emptySet())
    val loadingSurahs: StateFlow<Set<Int>> = _loadingSurahs.asStateFlow()

    /**
     * Bumped on every cache write (network fetch, bundled seed adoption,
     * disk restore). UI collects this and re-reads
     * [getAyahImmediate] so a placeholder swaps to real text the moment the
     * fetch lands instead of sticking until the track changes.
     */
    private val _cacheGen = MutableStateFlow(0)
    val cacheGen: StateFlow<Int> = _cacheGen.asStateFlow()

    private fun bumpCacheGen() {
        _cacheGen.value = _cacheGen.value + 1
    }

    /**
     * Surahs most recently resolved through [getAyahImmediate] /
     * [getAyahsForSurah], most recent last. [onTranslationChanged] re-fetches
     * exactly these when the translation edition changes: a cacheGen bump alone
     * only helps screens that are already composing, and even they skip
     * launching while the previous edition's request sits in `_loadingSurahs`.
     * Capped so a user who has downloaded many surahs cannot trigger a refetch
     * storm — the surah on screen is always the most recent requester because
     * screens re-enter through getAyahImmediate on every recomposition.
     * Held in a StateFlow because it is written from arbitrary dispatchers.
     */
    private val requestedSurahs = MutableStateFlow<Set<Int>>(emptySet())

    private const val MAX_REMEMBERED_SURAHS = 8

    private fun noteRequestedSurah(surahId: Int) {
        requestedSurahs.update { current ->
            val reordered = (current - surahId) + surahId
            if (reordered.size <= MAX_REMEMBERED_SURAHS) {
                reordered
            } else {
                reordered.toList().takeLast(MAX_REMEMBERED_SURAHS).toSet()
            }
        }
    }

    // Disk-backed text cache so a surah downloaded for hifz stays readable
    // offline across restarts. multiplatform-settings holds one JSON blob per
    // edition+surah plus a per-edition index of which surahs were persisted;
    // the in-memory cache above stays the hot path. Generated placeholders are
    // never persisted.
    //
    // The no-arg `Settings()` factory needs a platform context (an
    // androidx.startup initializer on Android, NSUserDefaults on iOS). When it
    // throws — which is exactly what happens on the JVM unit-test classpath,
    // where no initializer runs — degrade to a process-local map instead of
    // disabling persistence outright: same effective durability as the old
    // `null` store (nothing survives the process), but every edition-scoped
    // read/write path keeps executing and stays testable on every platform.
    private val textSettings: Settings by lazy {
        try {
            Settings()
        } catch (_: Exception) {
            InMemorySettings()
        }
    }

    /** v3 keys are scoped by edition so translations cannot overwrite each other. */
    private fun textKey(edition: String, surahId: Int): String = "ayah_text_v3_${edition}_$surahId"
    private fun textIndexKey(edition: String): String = "ayah_text_v3_index_${edition}"

    /**
     * v2 keys: written by earlier releases for the default edition only, so
     * the legacy fallback below is safe to apply only for that edition.
     */
    private fun legacyTextKey(surahId: Int): String = "ayah_text_v2_$surahId"
    private fun legacyTextIndexKey(): String = "ayah_text_v2_index"

    init {
        seedBundledVerses()
        loadPersistedText()
        clearLegacyV1Keys()
    }

    /**
     * Pre-populates the cache with bundled seeds (sanitized copies;
     * BUNDLED_SEEDS stays raw). The bundle is en.sahih text, so it must never
     * appear while another edition is selected.
     */
    private fun seedBundledVerses() {
        if (TranslationPrefs.selectedEdition.value != TranslationPrefs.DEFAULT_EDITION) return
        cache.putAll(
            BUNDLED_SEEDS.mapValues { (_, verses) ->
                verses.map { v -> v.copy(textUthmani = sanitizeVerseText(v.surahId, v.ayahNo, v.textUthmani)) }
            }
        )
    }

    private fun clearLegacyV1Keys() {
        val settings = textSettings
        try {
            val oldIndex = settings.getStringOrNull("ayah_text_v1_index")
            oldIndex?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.forEach { id ->
                try { settings.remove("ayah_text_v1_$id") } catch (_: Exception) { }
            }
            settings.remove("ayah_text_v1_index")
        } catch (_: Exception) {
        }
    }

    private fun isStoredPlaceholder(v: AyahVerse): Boolean =
        v.isPlaceholder || v.textUthmani.startsWith("آية رقم")

    private fun isSurahListComplete(surahId: Int, verses: List<AyahVerse>): Boolean {
        if (verses.isEmpty()) return false
        if (verses.any { it.isPlaceholder }) return false
        val expected = QuranData.SURAHS.firstOrNull { it.id == surahId }?.ayahsCount ?: return true
        val present = verses.mapTo(mutableSetOf()) { it.ayahNo }
        if (present.size != verses.size) return false
        return (1..expected).all { it in present }
    }

    /**
     * Restores the ACTIVE edition's persisted blobs into the cache: v3 keys
     * first, and — only for [TranslationPrefs.DEFAULT_EDITION] — a fallback to
     * the legacy v2 keys where v3 misses, so existing users keep their
     * downloaded default-edition text instead of refetching it. Every new
     * write goes to v3; other editions' blobs are never read or removed here.
     */
    private fun loadPersistedText() {
        val settings = textSettings
        val edition = TranslationPrefs.selectedEdition.value
        try {
            var restored = false
            val restoredFromV3 = mutableSetOf<Int>()

            val v3IndexKey = textIndexKey(edition)
            val v3Ids = readIndex(settings, v3IndexKey)
            val brokenV3 = mutableListOf<Int>()
            for (surahId in v3Ids) {
                val raw = settings.getStringOrNull(textKey(edition, surahId))
                if (raw != null && adoptStoredVerses(surahId, raw)) {
                    restored = true
                    restoredFromV3 += surahId
                } else {
                    // Missing or corrupt blob: forget it from THIS edition's
                    // index only — other editions' data is untouched.
                    brokenV3 += surahId
                }
            }
            if (brokenV3.isNotEmpty()) persistIndex(settings, v3IndexKey, v3Ids - brokenV3.toSet())

            if (edition == TranslationPrefs.DEFAULT_EDITION) {
                val legacyIndexKey = legacyTextIndexKey()
                val legacyIds = readIndex(settings, legacyIndexKey)
                val brokenLegacy = mutableListOf<Int>()
                for (surahId in legacyIds) {
                    if (surahId in restoredFromV3) continue
                    val raw = settings.getStringOrNull(legacyTextKey(surahId))
                    if (raw != null && adoptStoredVerses(surahId, raw)) {
                        restored = true
                    } else {
                        brokenLegacy += surahId
                    }
                }
                if (brokenLegacy.isNotEmpty()) persistIndex(settings, legacyIndexKey, legacyIds - brokenLegacy.toSet())
            }

            if (restored) bumpCacheGen()
        } catch (_: Exception) {
            // Text cache is best-effort; network fetch remains the fallback.
        }
    }

    private fun readIndex(settings: Settings, key: String): List<Int> =
        settings.getStringOrNull(key)?.split(",")?.mapNotNull { it.trim().toIntOrNull() }.orEmpty()

    private fun persistIndex(settings: Settings, key: String, ids: List<Int>) {
        try {
            settings.putString(key, ids.distinct().joinToString(","))
        } catch (_: Exception) {
        }
    }

    /**
     * Decodes + sanitizes one persisted blob and caches it when it holds a
     * real (non-placeholder) set. Returns false for missing/corrupt entries so
     * the caller can drop them from their own index.
     */
    private fun adoptStoredVerses(surahId: Int, raw: String): Boolean = try {
        val verses = json.decodeFromString<List<AyahVerse>>(raw)
            .map { v -> v.copy(textUthmani = sanitizeVerseText(v.surahId, v.ayahNo, v.textUthmani)) }
        if (verses.isNotEmpty() && verses.none { isStoredPlaceholder(it) }) {
            cache[surahId] = verses
            true
        } else {
            false
        }
    } catch (_: Exception) {
        false
    }

    /**
     * Warms the verse-text cache for offline reading and persists it to disk.
     * Fire-and-forget from download actions: fetches from network when needed
     * (bundled seeds persist immediately), persists only complete, real sets.
     *
     * The edition is snapshotted BEFORE the fetch, the blob is written under
     * that edition's key, and the snapshot is re-checked under the lock before
     * the write: [resolveAyahs] answers from the shared in-memory cache on a
     * hit, so a translation switched while this download was in flight must
     * never have the new edition's text filed under the old edition's key.
     */
    fun prefetchSurah(surahId: Int) {
        val edition = TranslationPrefs.selectedEdition.value
        scope.launch {
            try {
                val verses = resolveAyahs(surahId, edition)
                if (verses.isEmpty()) return@launch
                if (verses.any { it.isPlaceholder }) return@launch
                if (!isSurahListComplete(surahId, verses)) return@launch
                mutex.withLock {
                    // Re-check the edition under the lock, not just the
                    // snapshot taken at call time: resolveAyahs answers from
                    // the cache on a hit, and after a mid-download switch that
                    // cache slot already belongs to the NEW edition. Writing
                    // those verses under [edition]'s key would store one
                    // translation's text behind another's. Dropping the write
                    // loses nothing: the active edition keeps its own copy in
                    // memory, and its own download is what re-persists it.
                    if (TranslationPrefs.selectedEdition.value != edition) return@withLock
                    try {
                        val settings = textSettings
                        settings.putString(textKey(edition, surahId), json.encodeToString(verses))
                        val ids = readIndex(settings, textIndexKey(edition)) + surahId
                        persistIndex(settings, textIndexKey(edition), ids)
                    } catch (_: Exception) {
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Call after [TranslationPrefs.setEdition]: retires the previous edition's
     * translation out of the in-memory verses and bumps [cacheGen] so
     * collecting screens re-resolve the current ayah and the newly selected
     * edition gets fetched. The Arabic text survives the switch — only the
     * translation half is edition-specific.
     *
     * A cacheGen bump alone is not enough to guarantee that refetch: screens
     * re-enter through [getAyahImmediate], which skips launching a fetch while
     * the previous edition's request is still marked in-flight in
     * `_loadingSurahs` — a switch racing an in-flight download would otherwise
     * leave the player on a placeholder forever. The surahs most recently
     * requested via [getAyahImmediate] / [getAyahsForSurah] are therefore
     * remembered (see `requestedSurahs`) and re-triggered here, fire-and-forget
     * on the repository's own scope — the same mechanism [prefetchSurah] uses.
     * Results fetched under the previous edition are discarded by the edition
     * guard in `resolveAyahs`, so racing requests cannot clobber each other.
     * The new edition's persisted text is re-read so switching back to an
     * already-downloaded translation is instant, even offline.
     */
    fun onTranslationChanged() {
        // Park the Arabic before wiping the cache. Wiping used to blank the
        // verse to "Loading ayah…" for as long as the newly selected edition
        // took to arrive — the most visible way to make a language switch feel
        // like a glitch, and it threw away the very text the reader is in the
        // middle of memorizing. Only the translation half is edition-specific,
        // so the Arabic goes to `rememberedArabic` (display-only, never a fetch
        // short-circuit) and the refetch below refills the translation. If that
        // fetch never arrives — offline, or an edition the API cannot serve —
        // the reader keeps the Arabic, and the translation chip is right there
        // to switch back.
        cache.forEach { (surahId, verses) ->
            val arabic = verses
                .filterNot { it.isPlaceholder }
                .map { it.copy(translation = "", transliteration = "") }
            if (arabic.isNotEmpty()) rememberedArabic[surahId] = arabic
        }
        // Only a surah the player has actually asked for can still be on
        // screen, and that set is already capped — so bound the parking lot to
        // it rather than letting it grow with every switch.
        rememberedArabic.keys.retainAll(requestedSurahs.value.toSet())
        cache.clear()
        seedBundledVerses()
        loadPersistedText()
        bumpCacheGen()
        requestedSurahs.value.forEach { surahId ->
            scope.launch { getAyahsForSurah(surahId) }
        }
    }

    fun isAyahAvailable(surahId: Int, ayahNo: Int): Boolean =
        cache[surahId]?.any { it.ayahNo == ayahNo && !it.isPlaceholder } == true

    /**
     * Synchronous accessor for the verse on screen. Order of preference:
     * real cached text, then the Arabic parked by a translation switch (so the
     * ayah never blanks out from under the reader), and only then a clean
     * baseline placeholder. A fetch is kicked off whenever the cache misses,
     * so both fallback paths are temporary by construction.
     */
    fun getAyahImmediate(surahId: Int, ayahNo: Int): AyahVerse {
        // Remember even on a cache hit: this is the surah the player is
        // showing, so it must be re-fetched if the edition changes later.
        noteRequestedSurah(surahId)
        val cached = cache[surahId]?.firstOrNull { it.ayahNo == ayahNo && !it.isPlaceholder }
        if (cached != null) return cached

        // Trigger background fetch if not already in flight
        if (surahId !in _loadingSurahs.value) {
            scope.launch {
                getAyahsForSurah(surahId)
            }
        }

        // Switch in flight: show the Arabic we already know instead of a
        // placeholder. Translation stays empty on purpose — half the old
        // edition's text on screen would be worse than none.
        rememberedArabic[surahId]?.firstOrNull { it.ayahNo == ayahNo }?.let { return it }

        return placeholderAyah(surahId, ayahNo)
    }

    private fun placeholderAyah(surahId: Int, ayahNo: Int): AyahVerse {
        val surah = QuranData.SURAHS.firstOrNull { it.id == surahId }
        val surahNameAr = surah?.nameAr ?: ""
        val surahNameEn = surah?.nameEn ?: ""
        return AyahVerse(
            surahId = surahId,
            ayahNo = ayahNo,
            textUthmani = if (ayahNo == 1 && surahId == 1) {
                "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
            } else {
                "آية رقم $ayahNo من سورة $surahNameAr"
            },
            translation = "Ayah $ayahNo of Surah $surahNameEn",
            isPlaceholder = true
        )
    }

    /**
     * Retrieves all Ayahs for a given Surah (1..114). Checks cache first,
     * then attempts online fetch, falling back to bundled or generated seeds.
     * The active edition is snapshotted from [TranslationPrefs] at call time so
     * every path below speaks to a single translation.
     */
    suspend fun getAyahsForSurah(surahId: Int): List<AyahVerse> =
        resolveAyahs(surahId, TranslationPrefs.selectedEdition.value)

    /**
     * Resolves a surah for one specific [edition] (snapshot taken by the
     * caller — [prefetchSurah] snapshots it so its disk key and its verses can
     * never disagree). Data is only cached/returned when it belongs to that
     * edition:
     * - a successful fetch is dropped if the user switched editions while the
     *   request was in flight, so the previous translation cannot land in the
     *   new edition's cache slot;
     * - bundled seeds are en.sahih text, so they apply only when [edition] is
     *   the default AND is still the active edition;
     * - otherwise the caller gets the placeholder/offline path.
     */
    private suspend fun resolveAyahs(surahId: Int, edition: String): List<AyahVerse> {
        noteRequestedSurah(surahId)
        mutex.withLock {
            cache[surahId]?.takeIf { isSurahListComplete(surahId, it) }?.let { return it }
        }

        _loadingSurahs.value = _loadingSurahs.value + surahId
        try {
            val fetched = fetchSurahFromApi(surahId, edition)
            if (!fetched.isNullOrEmpty() && isSurahListComplete(surahId, fetched)) {
                // Edition guard: cache only for the edition this response was
                // requested under; onTranslationChanged re-fetches whatever a
                // switch made stale.
                if (TranslationPrefs.selectedEdition.value == edition) {
                    mutex.withLock {
                        cache[surahId] = fetched
                    }
                    // Real text is back: retire the parked Arabic so it can
                    // never outlive the data it was standing in for.
                    rememberedArabic.remove(surahId)
                    bumpCacheGen()
                    return fetched
                }
            }
        } catch (_: Exception) {
            // Network failure / offline
        } finally {
            _loadingSurahs.value = _loadingSurahs.value - surahId
        }

        // Bundled seeds are en.sahih text — usable only for that edition.
        if (edition == TranslationPrefs.DEFAULT_EDITION &&
            TranslationPrefs.selectedEdition.value == edition
        ) {
            BUNDLED_SEEDS[surahId]?.let {
                val cleaned = it.map { v -> v.copy(textUthmani = sanitizeVerseText(v.surahId, v.ayahNo, v.textUthmani)) }
                mutex.withLock { cache[surahId] = cleaned }
                bumpCacheGen()
                return cleaned
            }
        }

        // Fallback generator using Surah metadata
        val surah = QuranData.SURAHS.firstOrNull { it.id == surahId }
        val count = surah?.ayahsCount ?: 7
        val generated = (1..count).map { ayahNo -> placeholderAyah(surahId, ayahNo) }
        return generated
    }

    /**
     * Fetches `quran-uthmani` plus the caller's [edition] in one request.
     * [edition] comes from the caller's snapshot of
     * [TranslationPrefs.selectedEdition] — the single source of truth — never
     * from a shadow copy inside this repository.
     */
    private suspend fun fetchSurahFromApi(surahId: Int, edition: String): List<AyahVerse>? {
        val client = httpClient ?: return null
        val url = "https://api.alquran.cloud/v1/surah/$surahId/editions/quran-uthmani,$edition"
        val responseText = client.get(url).bodyAsText()
        val root = json.parseToJsonElement(responseText).jsonObject
        val dataArray = root["data"]?.jsonArray ?: return null
        if (dataArray.size < 2) return null

        val uthmaniEdition = dataArray[0].jsonObject
        val translationEdition = dataArray[1].jsonObject

        val uthmaniAyahs = uthmaniEdition["ayahs"]?.jsonArray ?: return null
        val translationAyahs = translationEdition["ayahs"]?.jsonArray ?: return null

        val result = mutableListOf<AyahVerse>()
        for (i in 0 until uthmaniAyahs.size) {
            val uObj = uthmaniAyahs[i].jsonObject
            val eObj = translationAyahs.getOrNull(i)?.jsonObject

            val ayahNo = uObj["numberInSurah"]?.jsonPrimitive?.int ?: (i + 1)
            val rawText = uObj["text"]?.jsonPrimitive?.content ?: ""
            val translation = eObj?.get("text")?.jsonPrimitive?.content ?: ""
            val textUthmani = sanitizeVerseText(surahId, ayahNo, rawText)

            result.add(
                AyahVerse(
                    surahId = surahId,
                    ayahNo = ayahNo,
                    textUthmani = textUthmani,
                    translation = translation
                )
            )
        }
        return result
    }

    /**
     * Process-local [Settings] used by [textSettings] when the platform's
     * no-arg `Settings()` factory cannot build a store (no Context on the JVM
     * unit-test classpath). Data dies with the process — the same effective
     * durability as the previous `null` settings — but the edition-scoped
     * persistence logic runs unmodified on every platform instead of silently
     * no-oping, which is what makes it testable.
     */
    private class InMemorySettings : Settings {
        private val values = mutableMapOf<String, Any?>()

        override val keys: Set<String> get() = values.keys
        override val size: Int get() = values.size

        override fun clear() {
            values.clear()
        }

        override fun remove(key: String) {
            values.remove(key)
        }

        override fun hasKey(key: String): Boolean = values.containsKey(key)

        override fun putInt(key: String, value: Int) {
            values[key] = value
        }

        override fun getInt(key: String, defaultValue: Int): Int = values[key] as? Int ?: defaultValue
        override fun getIntOrNull(key: String): Int? = values[key] as? Int

        override fun putLong(key: String, value: Long) {
            values[key] = value
        }

        override fun getLong(key: String, defaultValue: Long): Long = values[key] as? Long ?: defaultValue
        override fun getLongOrNull(key: String): Long? = values[key] as? Long

        override fun putString(key: String, value: String) {
            values[key] = value
        }

        override fun getString(key: String, defaultValue: String): String =
            values[key] as? String ?: defaultValue

        override fun getStringOrNull(key: String): String? = values[key] as? String

        override fun putFloat(key: String, value: Float) {
            values[key] = value
        }

        override fun getFloat(key: String, defaultValue: Float): Float = values[key] as? Float ?: defaultValue
        override fun getFloatOrNull(key: String): Float? = values[key] as? Float

        override fun putDouble(key: String, value: Double) {
            values[key] = value
        }

        override fun getDouble(key: String, defaultValue: Double): Double = values[key] as? Double ?: defaultValue
        override fun getDoubleOrNull(key: String): Double? = values[key] as? Double

        override fun putBoolean(key: String, value: Boolean) {
            values[key] = value
        }

        override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
            values[key] as? Boolean ?: defaultValue

        override fun getBooleanOrNull(key: String): Boolean? = values[key] as? Boolean
    }
}
