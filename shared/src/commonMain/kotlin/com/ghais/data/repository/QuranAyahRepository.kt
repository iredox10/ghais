package com.ghais.data.repository

import com.ghais.data.seed.QuranData
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
    val transliteration: String = ""
)

/**
 * Unified repository providing authentic Arabic Uthmani text and English translations
 * for all 114 Surahs. Supports:
 * 1. Bundled offline seed data for popular and essential Surahs.
 * 2. On-demand online fetching from AlQuran Cloud API with local in-memory caching.
 * 3. Graceful fallback for offline playback.
 */
object QuranAyahRepository {

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

    init {
        // Pre-populate cache with bundled seeds
        cache.putAll(BUNDLED_SEEDS)
    }

    /**
     * Synchronously returns cached or bundled Ayah verse if available,
     * otherwise triggers background fetch and returns a clean baseline placeholder.
     */
    fun getAyahImmediate(surahId: Int, ayahNo: Int): AyahVerse {
        val cached = cache[surahId]?.firstOrNull { it.ayahNo == ayahNo }
        if (cached != null) return cached

        // Trigger background fetch if not already in flight
        if (surahId !in _loadingSurahs.value) {
            scope.launch {
                getAyahsForSurah(surahId)
            }
        }

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
            translation = "Ayah $ayahNo of Surah $surahNameEn"
        )
    }

    /**
     * Retrieves all Ayahs for a given Surah (1..114). Checks cache first,
     * then attempts online fetch, falling back to bundled or generated seeds.
     */
    suspend fun getAyahsForSurah(surahId: Int): List<AyahVerse> {
        mutex.withLock {
            cache[surahId]?.let { return it }
        }

        _loadingSurahs.value = _loadingSurahs.value + surahId
        try {
            val fetched = fetchSurahFromApi(surahId)
            if (!fetched.isNullOrEmpty()) {
                mutex.withLock {
                    cache[surahId] = fetched
                }
                return fetched
            }
        } catch (_: Exception) {
            // Network failure / offline
        } finally {
            _loadingSurahs.value = _loadingSurahs.value - surahId
        }

        // Check bundled seed
        BUNDLED_SEEDS[surahId]?.let {
            mutex.withLock { cache[surahId] = it }
            return it
        }

        // Fallback generator using Surah metadata
        val surah = QuranData.SURAHS.firstOrNull { it.id == surahId }
        val count = surah?.ayahsCount ?: 7
        val generated = (1..count).map { ayahNo ->
            AyahVerse(
                surahId = surahId,
                ayahNo = ayahNo,
                textUthmani = if (ayahNo == 1 && surahId == 1) {
                    "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
                } else {
                    "آية رقم $ayahNo من سورة ${surah?.nameAr ?: ""}"
                },
                translation = "Verse $ayahNo of Surah ${surah?.nameEn ?: ""}"
            )
        }
        mutex.withLock {
            cache[surahId] = generated
        }
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
            val textUthmani = uObj["text"]?.jsonPrimitive?.content ?: ""
            val translation = eObj?.get("text")?.jsonPrimitive?.content ?: ""

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
