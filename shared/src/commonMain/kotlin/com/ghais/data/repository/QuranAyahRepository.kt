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
 * Unified repository providing authentic Arabic Uthmani text and English translations
 * for all 114 Surahs. Supports:
 * 1. Bundled offline seed data for popular and essential Surahs.
 * 2. On-demand online fetching from AlQuran Cloud API with local in-memory caching.
 * 3. Graceful fallback for offline playback.
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

    fun stripBasmalahPrefix(surahId: Int, ayahNo: Int, text: String): String {
        if (surahId == 1 || surahId == 9 || ayahNo != 1) return text
        val stripped = StringBuilder()
        val origIndices = mutableListOf<Int>()
        for (i in text.indices) {
            val c = text[i]
            if (isTashkeel(c)) continue
            stripped.append(c)
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
                return rest
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

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient by lazy {
        try {
            HttpClient()
        } catch (_: Exception) {
            null
        }
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

    // Disk-backed text cache so a surah downloaded for hifz stays readable
    // offline across restarts. multiplatform-settings holds one JSON blob per
    // surah plus an index of which surahs were persisted; the in-memory cache
    // above stays the hot path. Generated placeholders are never persisted.
    private val textSettings: Settings? by lazy {
        try {
            Settings()
        } catch (_: Exception) {
            null
        }
    }

    private fun textKey(surahId: Int): String = "ayah_text_v2_$surahId"
    private fun textIndexKey(): String = "ayah_text_v2_index"

    init {
        // Pre-populate cache with bundled seeds (sanitized copies; BUNDLED_SEEDS stays raw)
        cache.putAll(
            BUNDLED_SEEDS.mapValues { (_, verses) ->
                verses.map { v -> v.copy(textUthmani = sanitizeVerseText(v.surahId, v.ayahNo, v.textUthmani)) }
            }
        )
        loadPersistedText()
        clearLegacyV1Keys()
    }

    private fun clearLegacyV1Keys() {
        val settings = textSettings ?: return
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

    private fun loadPersistedText() {
        val settings = textSettings ?: return
        try {
            val rawIndex = settings.getStringOrNull(textIndexKey()) ?: return
            val ids = rawIndex.split(",").mapNotNull { it.trim().toIntOrNull() }
            var restored = false
            for (surahId in ids) {
                try {
                    val raw = settings.getStringOrNull(textKey(surahId)) ?: continue
                    val verses = json.decodeFromString<List<AyahVerse>>(raw)
                        .map { v -> v.copy(textUthmani = sanitizeVerseText(v.surahId, v.ayahNo, v.textUthmani)) }
                    if (verses.isNotEmpty() && verses.none { isStoredPlaceholder(it) }) {
                        cache[surahId] = verses
                        restored = true
                    } else {
                        persistTextIndex(ids - surahId)
                    }
                } catch (_: Exception) {
                    // Corrupt entry: drop it from the index, keep going.
                    persistTextIndex(ids - surahId)
                }
            }
            if (restored) bumpCacheGen()
        } catch (_: Exception) {
            // Text cache is best-effort; network fetch remains the fallback.
        }
    }

    private fun persistTextIndex(ids: List<Int>) {
        try {
            textSettings?.putString(textIndexKey(), ids.joinToString(","))
        } catch (_: Exception) {
        }
    }

    /**
     * Warms the verse-text cache for offline reading and persists it to disk.
     * Fire-and-forget from download actions: fetches from network when needed
     * (bundled seeds persist immediately), persists only complete, real sets.
     */
    fun prefetchSurah(surahId: Int) {
        scope.launch {
            try {
                val verses = getAyahsForSurah(surahId)
                if (verses.isEmpty()) return@launch
                if (verses.any { it.isPlaceholder }) return@launch
                if (!isSurahListComplete(surahId, verses)) return@launch
                mutex.withLock {
                    try {
                        textSettings?.putString(textKey(surahId), json.encodeToString(verses))
                        val ids = (textSettings?.getStringOrNull(textIndexKey())
                            ?.split(",")?.mapNotNull { it.trim().toIntOrNull() }
                            .orEmpty() + surahId).distinct()
                        persistTextIndex(ids)
                    } catch (_: Exception) {
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun isAyahAvailable(surahId: Int, ayahNo: Int): Boolean =
        cache[surahId]?.any { it.ayahNo == ayahNo && !it.isPlaceholder } == true

    /**
     * Synchronously returns cached or bundled Ayah verse if available,
     * otherwise triggers background fetch and returns a clean baseline placeholder.
     */
    fun getAyahImmediate(surahId: Int, ayahNo: Int): AyahVerse {
        val cached = cache[surahId]?.firstOrNull { it.ayahNo == ayahNo && !it.isPlaceholder }
        if (cached != null) return cached

        // Trigger background fetch if not already in flight
        if (surahId !in _loadingSurahs.value) {
            scope.launch {
                getAyahsForSurah(surahId)
            }
        }

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
     */
    suspend fun getAyahsForSurah(surahId: Int): List<AyahVerse> {
        mutex.withLock {
            cache[surahId]?.takeIf { isSurahListComplete(surahId, it) }?.let { return it }
        }

        _loadingSurahs.value = _loadingSurahs.value + surahId
        try {
            val fetched = fetchSurahFromApi(surahId)
            if (!fetched.isNullOrEmpty() && isSurahListComplete(surahId, fetched)) {
                mutex.withLock {
                    cache[surahId] = fetched
                }
                bumpCacheGen()
                return fetched
            }
        } catch (_: Exception) {
            // Network failure / offline
        } finally {
            _loadingSurahs.value = _loadingSurahs.value - surahId
        }

        // Check bundled seed
        BUNDLED_SEEDS[surahId]?.let {
            val cleaned = it.map { v -> v.copy(textUthmani = sanitizeVerseText(v.surahId, v.ayahNo, v.textUthmani)) }
            mutex.withLock { cache[surahId] = cleaned }
            bumpCacheGen()
            return cleaned
        }

        // Fallback generator using Surah metadata
        val surah = QuranData.SURAHS.firstOrNull { it.id == surahId }
        val count = surah?.ayahsCount ?: 7
        val generated = (1..count).map { ayahNo -> placeholderAyah(surahId, ayahNo) }
        return generated
    }

    private suspend fun fetchSurahFromApi(surahId: Int): List<AyahVerse>? {
        val client = httpClient ?: return null
        val url = "https://api.alquran.cloud/v1/surah/$surahId/editions/quran-uthmani,en.sahih"
        val responseText = client.get(url).bodyAsText()
        val root = json.parseToJsonElement(responseText).jsonObject
        val dataArray = root["data"]?.jsonArray ?: return null
        if (dataArray.size < 2) return null

        val uthmaniEdition = dataArray[0].jsonObject
        val englishEdition = dataArray[1].jsonObject

        val uthmaniAyahs = uthmaniEdition["ayahs"]?.jsonArray ?: return null
        val englishAyahs = englishEdition["ayahs"]?.jsonArray ?: return null

        val result = mutableListOf<AyahVerse>()
        for (i in 0 until uthmaniAyahs.size) {
            val uObj = uthmaniAyahs[i].jsonObject
            val eObj = englishAyahs.getOrNull(i)?.jsonObject

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
}
