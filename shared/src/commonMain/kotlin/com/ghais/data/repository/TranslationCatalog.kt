package com.ghais.data.repository

/**
 * One entry in the bundled translation-edition catalog shown by the
 * hifz player's translation picker.
 *
 * [id] is the alquran.cloud edition identifier (e.g. `en.sahih`) — the same
 * identifier the network layer appends to `/v1/surah/{id}/editions/…`.
 * The country fields exist because the API only ships a language code;
 * we group and label entries by the language's primary country so users
 * can browse translations the way they think about them ("Turkish",
 * "Urdu — Pakistan"), not by ISO code.
 */
data class TranslationCatalogItem(
    /** alquran.cloud edition identifier, e.g. `en.sahih`. */
    val id: String,
    /** ISO 639-1 language code, e.g. `en`. */
    val languageCode: String,
    /** English display name of the language, e.g. `English`. */
    val languageName: String,
    /** Language endonym as spoken by its readers, e.g. `العربية`. */
    val nativeLanguageName: String,
    /** Primary country the language is grouped under, e.g. `Pakistan`. */
    val countryName: String,
    /** Emoji flag matching [countryName], e.g. `🇵🇰`. */
    val countryFlag: String,
    /** Translation title/author in English (from the API). */
    val englishName: String,
    /** Translation title/author in its own script (from the API `name`). */
    val nativeName: String,
    /** True when the edition's text reads right-to-left. */
    val rtl: Boolean = false,
)

/**
 * Static, offline catalog of every text translation the app can fetch.
 *
 * Source: `https://api.alquran.cloud/v1/edition/type/translation`
 * (raw response cached at build time — see `SEED`). Audio "translation"
 * editions (`format == "audio"`) are excluded; this catalog only describes
 * text. Everything here is in-memory: no network is needed to browse or
 * search the picker, only to *load* a selected edition's ayah text.
 */
object TranslationCatalog {

    /** Every known text translation. Stable order (API order). */
    fun all(): List<TranslationCatalogItem> = SEED

    /** Lookup by edition identifier; null when unknown. */
    fun byId(id: String): TranslationCatalogItem? = SEED.firstOrNull { it.id == id }

    /**
     * Case-insensitive substring search across id, author/title (both
     * scripts), language (both names) and country. Empty query returns
     * the full list so a picker can show everything before typing.
     */
    fun search(query: String): List<TranslationCatalogItem> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return SEED
        return SEED.filter { item ->
            item.id.lowercase().contains(q) ||
                item.englishName.lowercase().contains(q) ||
                item.nativeName.lowercase().contains(q) ||
                item.languageName.lowercase().contains(q) ||
                item.nativeLanguageName.lowercase().contains(q) ||
                item.countryName.lowercase().contains(q)
        }
    }

    /**
     * Picker grouping: country name → entries sorted by language, then
     * author. Countries come out alphabetically.
     */
    fun groupedByCountry(): Map<String, List<TranslationCatalogItem>> =
        SEED.groupBy { it.countryName }
            .mapValues { (_, items) ->
                items.sortedWith(compareBy({ it.languageName }, { it.englishName }))
            }
            .toSortedMap()

    // ---------------------------------------------------------------------

    /**
     * Curated display metadata for every language code used in [SEED],
     * keyed by ISO 639-1 code.
     *
     * WHY this exists: alquran.cloud only ships a bare language code, but
     * the picker needs an English language name, an endonym for the row
     * label, and a primary country + flag to group entries under — the API
     * carries none of that. Rules used when curating:
     *
     * - `countryName` is always the *English* country name so grouping and
     *   alphabetical sorting are stable regardless of script.
     * - `nativeLanguageName` is the language's own endonym (e.g. `Türkçe`),
     *   not the country's name in that language (e.g. `Türkiye`).
     * - Multi-country languages are pinned to one primary country
     *   (`ur`/`sd` → Pakistan, `ru`/`tt`/`ce` → Russia, `zh`/`ug` → China,
     *   `hi`/`ta`/`ml` → India) so no picker row can appear twice.
     * - `bs` and `ba` are both emitted by the API for Bosnian editions, so
     *   both map to the same Bosnian metadata.
     */
    private val LANGUAGE_META: Map<String, LanguageMeta> = mapOf(
        "am" to LanguageMeta("Amharic", "አማርኛ", "Ethiopia", "🇪🇹"),
        "az" to LanguageMeta("Azerbaijani", "Azərbaycan dili", "Azerbaijan", "🇦🇿"),
        "ba" to LanguageMeta("Bosnian", "Bosanski", "Bosnia and Herzegovina", "🇧🇦"),
        "ber" to LanguageMeta("Amazigh", "ⵜⴰⵎⴰⵣⵉⵖⵜ", "Algeria", "🇩🇿"),
        "bg" to LanguageMeta("Bulgarian", "Български", "Bulgaria", "🇧🇬"),
        "bn" to LanguageMeta("Bengali", "বাংলা", "Bangladesh", "🇧🇩"),
        "bs" to LanguageMeta("Bosnian", "Bosanski", "Bosnia and Herzegovina", "🇧🇦"),
        "ce" to LanguageMeta("Chechen", "Нохчийн мотт", "Russia", "🇷🇺"),
        "cs" to LanguageMeta("Czech", "Čeština", "Czech Republic", "🇨🇿"),
        "de" to LanguageMeta("German", "Deutsch", "Germany", "🇩🇪"),
        "dv" to LanguageMeta("Divehi", "ދިވެހި", "Maldives", "🇲🇻"),
        "en" to LanguageMeta("English", "English", "United States", "🇺🇸"),
        "es" to LanguageMeta("Spanish", "Español", "Spain", "🇪🇸"),
        "fa" to LanguageMeta("Persian", "فارسی", "Iran", "🇮🇷"),
        "fr" to LanguageMeta("French", "Français", "France", "🇫🇷"),
        "ha" to LanguageMeta("Hausa", "Hausa", "Nigeria", "🇳🇬"),
        "hi" to LanguageMeta("Hindi", "हिन्दी", "India", "🇮🇳"),
        "id" to LanguageMeta("Indonesian", "Bahasa Indonesia", "Indonesia", "🇮🇩"),
        "it" to LanguageMeta("Italian", "Italiano", "Italy", "🇮🇹"),
        "ja" to LanguageMeta("Japanese", "日本語", "Japan", "🇯🇵"),
        "ko" to LanguageMeta("Korean", "한국어", "South Korea", "🇰🇷"),
        "ku" to LanguageMeta("Kurdish", "Kurdî", "Iraq", "🇮🇶"),
        "ml" to LanguageMeta("Malayalam", "മലയാളം", "India", "🇮🇳"),
        "ms" to LanguageMeta("Malay", "Bahasa Melayu", "Malaysia", "🇲🇾"),
        "my" to LanguageMeta("Burmese", "မြန်မာဘာသာ", "Myanmar", "🇲🇲"),
        "nl" to LanguageMeta("Dutch", "Nederlands", "Netherlands", "🇳🇱"),
        "no" to LanguageMeta("Norwegian", "Norsk", "Norway", "🇳🇴"),
        "pl" to LanguageMeta("Polish", "Polski", "Poland", "🇵🇱"),
        "ps" to LanguageMeta("Pashto", "پښتو", "Afghanistan", "🇦🇫"),
        "pt" to LanguageMeta("Portuguese", "Português", "Brazil", "🇧🇷"),
        "ro" to LanguageMeta("Romanian", "Română", "Romania", "🇷🇴"),
        "ru" to LanguageMeta("Russian", "Русский", "Russia", "🇷🇺"),
        "sd" to LanguageMeta("Sindhi", "سنڌي", "Pakistan", "🇵🇰"),
        "si" to LanguageMeta("Sinhala", "සිංහල", "Sri Lanka", "🇱🇰"),
        "so" to LanguageMeta("Somali", "Soomaali", "Somalia", "🇸🇴"),
        "sq" to LanguageMeta("Albanian", "Shqip", "Albania", "🇦🇱"),
        "sv" to LanguageMeta("Swedish", "Svenska", "Sweden", "🇸🇪"),
        "sw" to LanguageMeta("Swahili", "Kiswahili", "Tanzania", "🇹🇿"),
        "ta" to LanguageMeta("Tamil", "தமிழ்", "India", "🇮🇳"),
        "tg" to LanguageMeta("Tajik", "Тоҷикӣ", "Tajikistan", "🇹🇯"),
        "th" to LanguageMeta("Thai", "ไทย", "Thailand", "🇹🇭"),
        "tr" to LanguageMeta("Turkish", "Türkçe", "Turkey", "🇹🇷"),
        "tt" to LanguageMeta("Tatar", "Татарча", "Russia", "🇷🇺"),
        "ug" to LanguageMeta("Uyghur", "ئۇيغۇرچە", "China", "🇨🇳"),
        "ur" to LanguageMeta("Urdu", "اردو", "Pakistan", "🇵🇰"),
        "uz" to LanguageMeta("Uzbek", "Oʻzbekcha", "Uzbekistan", "🇺🇿"),
        "zh" to LanguageMeta("Chinese", "中文", "China", "🇨🇳"),
    )

    /** Display metadata attached to each [TranslationCatalogItem]. */
    private data class LanguageMeta(
        /** English display name of the language, e.g. `English`. */
        val languageName: String,
        /** Language endonym as spoken by its readers, e.g. `English`. */
        val nativeLanguageName: String,
        /** Primary country (English name) the language groups under. */
        val countryName: String,
        /** Emoji flag matching [countryName]. */
        val countryFlag: String,
    )

    /**
     * Builds one [SEED] row by merging an API edition with its curated
     * [LANGUAGE_META].
     *
     * WHY a helper: all rows of a language share the same four metadata
     * fields — centralizing the merge keeps the seed rows to a single line
     * and means a country/endonym fix is one map entry, never 17 edits.
     */
    private fun seed(
        id: String,
        languageCode: String,
        englishName: String,
        nativeName: String,
        rtl: Boolean = false,
    ): TranslationCatalogItem {
        val meta = LANGUAGE_META.getValue(languageCode)
        return TranslationCatalogItem(
            id = id,
            languageCode = languageCode,
            languageName = meta.languageName,
            nativeLanguageName = meta.nativeLanguageName,
            countryName = meta.countryName,
            countryFlag = meta.countryFlag,
            englishName = englishName,
            nativeName = nativeName,
            rtl = rtl,
        )
    }

    /**
     * Generated from the alquran.cloud `edition/type/translation` response
     * (raw JSON cached at build time), in API order.
     *
     * Kept: `format == "text"` only — the six audio "translation" editions
     * and `quran-buck` (a Buck-script transliteration of the Arabic, not a
     * translation) are excluded, leaving 117 rows across 47 languages. The
     * three editions the API names `Unknown` are labelled with their
     * language's English name instead (`id.indonesian` → "Indonesian",
     * `ja.japanese` → "Japanese", `ko.korean` → "Korean").
     */
    private val SEED: List<TranslationCatalogItem> = listOf(
        seed(
            "az.mammadaliyev",
            "az",
            "Vasim Mammadaliyev and Ziya Bunyadov",
            "Məmmədəliyev & Bünyadov",
        ),
        seed("az.musayev", "az", "Alikhan Musayev", "Musayev"),
        seed("bn.bengali", "bn", "Muhiuddin Khan", "মুহিউদ্দীন খান"),
        seed("cs.hrbek", "cs", "Preklad I. Hrbek", "Hrbek"),
        seed("cs.nykl", "cs", "A. R. Nykl", "Nykl"),
        seed("de.aburida", "de", "Abu Rida Muhammad ibn Ahmad ibn Rassoul", "Abu Rida"),
        seed("de.bubenheim", "de", "A. S. F. Bubenheim and N. Elyas", "Bubenheim & Elyas"),
        seed("de.khoury", "de", "Adel Theodor Khoury", "Khoury"),
        seed("de.zaidan", "de", "Amir Zaidan", "Zaidan"),
        seed("dv.divehi", "dv", "Office of the President of Maldives", "ދިވެހި", rtl = true),
        seed("en.ahmedali", "en", "Ahmed Ali", "Ahmed Ali"),
        seed("en.ahmedraza", "en", "Ahmed Raza Khan", "Ahmed Raza Khan"),
        seed("en.arberry", "en", "A. J. Arberry", "Arberry"),
        seed("en.asad", "en", "Muhammad Asad", "Asad"),
        seed("en.daryabadi", "en", "Abdul Majid Daryabadi", "Daryabadi"),
        seed(
            "en.hilali",
            "en",
            "Muhammad Taqi-ud-Din al-Hilali and Muhammad Muhsin Khan",
            "Hilali & Khan",
        ),
        seed("en.pickthall", "en", "Mohammed Marmaduke William Pickthall", "Pickthall"),
        seed(
            "en.qaribullah",
            "en",
            "Hasan al-Fatih Qaribullah and Ahmad Darwish",
            "Qaribullah & Darwish",
        ),
        seed("en.sahih", "en", "Saheeh International", "Saheeh International"),
        seed("en.sarwar", "en", "Muhammad Sarwar", "Sarwar"),
        seed("en.yusufali", "en", "Abdullah Yusuf Ali", "Yusuf Ali"),
        seed("fa.ayati", "fa", "AbdolMohammad Ayati", "آیتی", rtl = true),
        seed("fa.fooladvand", "fa", "Mohammad Mahdi Fooladvand", "فولادوند", rtl = true),
        seed("fa.ghomshei", "fa", "Mahdi Elahi Ghomshei", "الهی قمشه‌ای", rtl = true),
        seed("fa.makarem", "fa", "Naser Makarem Shirazi", "مکارم شیرازی", rtl = true),
        seed("fr.hamidullah", "fr", "Muhammad Hamidullah", "Hamidullah"),
        seed("ha.gumi", "ha", "Abubakar Mahmoud Gumi", "Gumi"),
        seed("hi.hindi", "hi", "Suhel Farooq Khan and Saifur Rahman Nadwi", "फ़ारूक़ ख़ान & नदवी"),
        seed("id.indonesian", "id", "Indonesian", "Bahasa Indonesia"),
        seed("it.piccardo", "it", "Hamza Roberto Piccardo", "Piccardo"),
        seed("ja.japanese", "ja", "Japanese", "Japanese"),
        seed("ko.korean", "ko", "Korean", "Korean"),
        seed("ku.asan", "ku", "Burhan Muhammad-Amin", "ته‌فسیری ئاسان"),
        seed(
            "ml.abdulhameed",
            "ml",
            "Cheriyamundam Abdul Hameed and Kunhi Mohammed Parappoor",
            "അബ്ദുല്‍ ഹമീദ് & പറപ്പൂര്‍",
        ),
        seed("nl.keyzer", "nl", "Salomo Keyzer", "Keyzer"),
        seed("no.berg", "no", "Einar Berg", "Einar Berg"),
        seed("pl.bielawskiego", "pl", "Józefa Bielawskiego", "Bielawskiego"),
        seed("pt.elhayek", "pt", "Samir El-Hayek", "El-Hayek"),
        seed("ro.grigore", "ro", "George Grigore", "Grigore"),
        seed("ru.kuliev", "ru", "Elmir Kuliev", "Кулиев"),
        seed("ru.osmanov", "ru", "Magomed-Nuri Osmanovich Osmanov", "Османов"),
        seed("ru.porokhova", "ru", "V. Porokhova", "Порохова"),
        seed("sd.amroti", "sd", "Taj Mehmood Amroti", "امروٽي", rtl = true),
        seed("so.abduh", "so", "Mahmud Muhammad Abduh", "Abduh"),
        seed("sq.ahmeti", "sq", "Sherif Ahmeti", "Sherif Ahmeti"),
        seed("sq.mehdiu", "sq", "Feti Mehdiu", "Feti Mehdiu"),
        seed("sq.nahi", "sq", "Hasan Efendi Nahi", "Efendi Nahi"),
        seed("sv.bernstrom", "sv", "Knut Bernström", "Bernström"),
        seed("sw.barwani", "sw", "Ali Muhsin Al-Barwani", "Al-Barwani"),
        seed("ta.tamil", "ta", "Jan Turst Foundation", "ஜான் டிரஸ்ட்"),
        seed("tg.ayati", "tg", "AbdolMohammad Ayati", "Оятӣ"),
        seed("th.thai", "th", "King Fahad Quran Complex", "ภาษาไทย"),
        seed("tr.ates", "tr", "Suleyman Ates", "Süleyman Ateş"),
        seed("tr.bulac", "tr", "Alİ Bulaç", "Alİ Bulaç"),
        seed("tr.diyanet", "tr", "Diyanet Isleri", "Diyanet İşleri"),
        seed("tr.golpinarli", "tr", "Abdulbaki Golpinarli", "Abdulbakî Gölpınarlı"),
        seed("tr.ozturk", "tr", "Yasar Nuri Ozturk", "Öztürk"),
        seed("tr.vakfi", "tr", "Diyanet Vakfi", "Diyanet Vakfı"),
        seed("tr.yazir", "tr", "Elmalili Hamdi Yazir", "Elmalılı Hamdi Yazır"),
        seed("tr.yildirim", "tr", "Suat Yildirim", "Suat Yıldırım"),
        seed("tr.yuksel", "tr", "Edip Yüksel", "Edip Yüksel"),
        seed("tt.nugman", "tt", "Yakub Ibn Nugman", "Якуб ибн Нугман"),
        seed("ug.saleh", "ug", "Muhammad Saleh", "محمد صالح", rtl = true),
        seed("ur.ahmedali", "ur", "Ahmed Ali", "احمد علی", rtl = true),
        seed("ur.jalandhry", "ur", "Fateh Muhammad Jalandhry", "جالندہری", rtl = true),
        seed("ur.jawadi", "ur", "Syed Zeeshan Haider Jawadi", "علامہ جوادی", rtl = true),
        seed("ur.kanzuliman", "ur", "Ahmed Raza Khan", "احمد رضا خان", rtl = true),
        seed("ur.qadri", "ur", "Tahir ul Qadri", "طاہر القادری", rtl = true),
        seed("uz.sodik", "uz", "Muhammad Sodik Muhammad Yusuf", "Мухаммад Содик"),
        seed("en.maududi", "en", "Abul Ala Maududi", "Maududi"),
        seed("en.shakir", "en", "Mohammad Habib Shakir", "Shakir"),
        seed("es.cortes", "es", "Julio Cortes", "Cortes"),
        seed("fa.ansarian", "fa", "Hussain Ansarian", "انصاریان", rtl = true),
        seed("bg.theophanov", "bg", "Tzvetan Theophanov", "Теофанов"),
        seed("bs.mlivo", "bs", "Mustafa Mlivo", "Mlivo"),
        seed("fa.bahrampour", "fa", "Abolfazl Bahrampour", "بهرام پور", rtl = true),
        seed("es.asad", "es", "Muhammad Asad - Abdurrasak Pérez", "Asad"),
        seed("fa.khorramshahi", "fa", "Baha'oddin Khorramshahi", "خرمشاهی", rtl = true),
        seed("fa.mojtabavi", "fa", "Sayyed Jalaloddin Mojtabavi", "مجتبوی", rtl = true),
        seed("hi.farooq", "hi", "Muhammad Farooq Khan and Muhammad Ahmed", "फ़ारूक़ ख़ान & अहमद"),
        seed("id.muntakhab", "id", "Muhammad Quraish Shihab et al.", "Quraish Shihab"),
        seed("ms.basmeih", "ms", "Abdullah Muhammad Basmeih", "Basmeih"),
        seed("ru.abuadel", "ru", "Abu Adel", "Абу Адель"),
        seed("ru.krachkovsky", "ru", "Ignaty Yulianovich Krachkovsky", "Крачковский"),
        seed("ru.muntahab", "ru", "Ministry of Awqaf, Egypt", "Аль-Мунтахаб"),
        seed("ru.sablukov", "ru", "Gordy Semyonovich Sablukov", "Саблуков"),
        seed("ur.junagarhi", "ur", "Muhammad Junagarhi", "محمد جوناگڑھی", rtl = true),
        seed("ur.maududi", "ur", "Abul A'ala Maududi", "ابوالاعلی مودودی", rtl = true),
        seed("zh.jian", "zh", "Ma Jian", "马坚"),
        seed("zh.majian", "zh", "Ma Jian", "Ma Jian (Traditional)"),
        seed("fa.khorramdel", "fa", "Mostafa Khorramdel", "خرمدل", rtl = true),
        seed("fa.moezzi", "fa", "Mohammad Kazem Moezzi", "معزی", rtl = true),
        seed("bs.korkut", "bs", "Besim Korkut", "Korkut"),
        seed("si.naseemismail", "si", "Naseem Isamil and Masoor Maulana, Kaleel", "Naseem Ismail"),
        seed("zh.mazhonggang", "zh", "马仲刚", "Ma Zhong Gang"),
        seed(
            "ba.mehanovic",
            "ba",
            "Quran translation by Muhamed Mehanovic",
            "Kur'an - sa prevodom (značenja) na bosanski jezik, utemeljen na Ibn Kesirovom tumačenju, i kratki komentar",
        ),
        seed("en.itani", "en", "Clear Qur'an by Talal Itani", "Clear Qur'an - Talal Itani"),
        seed("my.ghazi", "my", "Translation by Ghazi Muhammed Hashim", "Ghazi Muhammed Hashim"),
        seed("en.mubarakpuri", "en", "Mubarakpuri", "Mubarakpuri"),
        seed("am.sadiq", "am", "Sadiq & Sani Habib", "ሳዲቅ & ሳኒ ሐቢብ"),
        seed("ber.mensur", "ber", "At Mensur", "At Mensur"),
        seed("bn.hoque", "bn", "Zohurul Hoque", "জহুরুল হক"),
        seed("en.qarai", "en", "Qarai", "Qarai"),
        seed("en.wahiduddin", "en", "Wahiduddin Khan", "Wahiduddin Khan"),
        seed("es.bornez", "es", "Bornez", "Bornez"),
        seed("es.garcia", "es", "Garcia", "Garcia"),
        seed("ur.najafi", "ur", "Muhammad Hussain Najafi", "محمد حسین نجفی", rtl = true),
        seed("fa.gharaati", "fa", "Mohsen Gharaati", "قرائتی", rtl = true),
        seed("fa.sadeqi", "fa", "Mohammad Sadeqi Tehrani", "صادقی تهرانی", rtl = true),
        seed("fa.safavi", "fa", "Mohammad Reza Safavi", "صفوی", rtl = true),
        seed("id.jalalayn", "id", "Tafsir Jalalayn", "Tafsir Jalalayn"),
        seed("ml.karakunnu", "ml", "Karakunnu & Elayavoor", "കാരകുന്ന് & എളയാവൂര്"),
        seed("nl.leemhuis", "nl", "Leemhuis", "Leemhuis"),
        seed("nl.siregar", "nl", "Siregar", "Siregar"),
        seed("ps.abdulwali", "ps", "Abdul Wali", "عبدالولي", rtl = true),
        seed("ru.kuliev-alsaadi", "ru", "Kuliev & as-Saadi", "Кулиев + ас-Саади"),
        seed("ce.magomedov", "ce", "Chechen by Magomedov", "Магомедов"),
    )
}
