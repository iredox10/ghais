package com.ghais.ui.screens.mushaf

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.ghais.data.repository.AyahVerse
import com.ghais.data.repository.QuranAyahRepository
import com.ghais.data.seed.PageJuzIndex
import com.ghais.data.seed.QuranData
import com.ghais.data.share.ShareSheet
import com.ghais.domain.model.RepeatMode
import com.ghais.domain.model.Surah
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisTypography
import com.ghais.ui.util.appendAyahMark
import com.ghais.ui.util.ayahMarkContent
import com.ghais.ui.util.toArabicDigits
import com.russhwolf.settings.Settings
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Data representation for an Ayah entry in the Mushaf screen,
 * supporting Tajweed segments, translations, audio, and Tafsir excerpts.
 */
data class MushafAyahData(
    val ayahNumber: Int,
    val textUthmani: String,
    val tajweedPart: String? = null,
    val tajweedPrefix: Boolean = false,
    val translation: String,
    val footnote: String? = null,
    val audioUrl: String,
    val durationMs: Long,
    val tafsirSnippet: String,
    val isPlaceholder: Boolean = false
)

/** Default reciter for Mushaf playback — matches the engine default and the
 * previously hardcoded Alafasy EveryAyah audio URLs. */
private const val MUSHAF_RECITER_SLUG = "mishary"
private const val MUSHAF_AUDIO_FOLDER = "Alafasy_128kbps"

/** Per-ayah audio URL in the EveryAyah shape (SS SAAA.mp3), same as before. */
private fun mushafAyahAudioUrl(surahId: Int, ayahNo: Int): String {
    val s = surahId.toString().padStart(3, '0')
    val a = ayahNo.toString().padStart(3, '0')
    return "https://everyayah.com/data/$MUSHAF_AUDIO_FOLDER/$s$a.mp3"
}

/** Maps a repository verse to the UI model. Placeholder verses (offline /
 * not-yet-fetched) are kept honestly: their generated text is shown as-is
 * with a footnote, never blanked and never crashing. */
private fun AyahVerse.toMushafAyah(): MushafAyahData = MushafAyahData(
    ayahNumber = ayahNo,
    textUthmani = textUthmani,
    translation = translation.ifBlank { "Translation unavailable — check connection and retry." },
    footnote = if (isPlaceholder) "Full text not downloaded yet — connect and tap Retry." else null,
    audioUrl = mushafAyahAudioUrl(surahId, ayahNo),
    durationMs = 0L,
    tafsirSnippet = "",
    isPlaceholder = isPlaceholder
)

/**
 * Juz/page numbers come from [PageJuzIndex] (Quran.com v4 Madani pagination,
 * verified against AlQuran Cloud; see data/seed/PageJuzIndex.kt).
 */

/**
 * Persisted UI prefs for the Mushaf screen (HifzUiPrefs-style: multiplatform
 * [Settings] with try/catch reads/writes, file-local since HifzUiPrefs only
 * owns its loop-settings flag).
 *
 * Bookmarks are `(surahId, ayahNo)` pairs persisted as `"surahId:ayahNo"`
 * strings under [KEY_BOOKMARKS] (FollowStore-style JSON list), because the
 * track-level [com.ghais.data.repository.FavoritesStore] identity (audio URL)
 * does not fit verse bookmarks. Same key as before, so old bookmarks
 * (including `:18:`-era `"18:N"` entries) still load — the only change is
 * that saving no longer drops other surahs (the old saver rewrote the whole
 * list with a single surah's set).
 */
private object MushafUiPrefs {
    const val DEFAULT_SURAH_ID = 18

    private const val KEY_BOOKMARKS = "mushaf_bookmarks"
    private const val KEY_TAJWEED = "mushaf_tajweed"
    private const val KEY_TRANSLATION = "mushaf_show_translation"
    private const val KEY_FONT_SIZE = "mushaf_font_size"
    private const val KEY_PAGE_VIEW = "mushaf_page_view"
    private const val KEY_REPEAT_IDX = "mushaf_repeat_idx"
    private const val KEY_SURAH_ID = "mushaf_surah_id"

    private val settings: Settings by lazy { Settings() }
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun loadSurahId(): Int = try {
        settings.getInt(KEY_SURAH_ID, DEFAULT_SURAH_ID).coerceIn(1, 114)
    } catch (_: Exception) {
        DEFAULT_SURAH_ID
    }

    fun saveSurahId(surahId: Int) {
        try {
            settings.putInt(KEY_SURAH_ID, surahId.coerceIn(1, 114))
        } catch (_: Exception) {
        }
    }

    fun loadAllBookmarks(): Set<Pair<Int, Int>> = try {
        val raw = settings.getString(KEY_BOOKMARKS, "")
        if (raw.isBlank()) emptySet()
        else json.decodeFromString<List<String>>(raw)
            .mapNotNull { entry ->
                val parts = entry.split(":")
                val s = parts.getOrNull(0)?.toIntOrNull()
                val a = parts.getOrNull(1)?.toIntOrNull()
                if (s != null && a != null && s in 1..114 && a >= 1) s to a else null
            }.toSet()
    } catch (_: Exception) {
        emptySet()
    }

    fun saveAllBookmarks(all: Set<Pair<Int, Int>>) {
        try {
            val clean = all
                .filter { (s, a) -> s in 1..114 && a >= 1 }
                .sortedWith(compareBy({ it.first }, { it.second }))
            settings.putString(
                KEY_BOOKMARKS,
                json.encodeToString(clean.map { (s, a) -> "$s:$a" })
            )
        } catch (_: Exception) {
        }
    }

    fun isTajweed(): Boolean = getFlag(KEY_TAJWEED, true)
    fun setTajweed(active: Boolean) = putFlag(KEY_TAJWEED, active)

    fun showTranslation(): Boolean = getFlag(KEY_TRANSLATION, true)
    fun setShowTranslation(visible: Boolean) = putFlag(KEY_TRANSLATION, visible)

    fun isPageView(): Boolean = getFlag(KEY_PAGE_VIEW, false)
    fun setPageView(pageView: Boolean) = putFlag(KEY_PAGE_VIEW, pageView)

    fun fontSize(): Int = try {
        settings.getInt(KEY_FONT_SIZE, 28)
    } catch (_: Exception) {
        28
    }

    fun setFontSize(size: Int) {
        try {
            settings.putInt(KEY_FONT_SIZE, size)
        } catch (_: Exception) {
        }
    }

    fun repeatIdx(): Int = try {
        settings.getInt(KEY_REPEAT_IDX, 1).coerceIn(0, 3)
    } catch (_: Exception) {
        1
    }

    fun setRepeatIdx(idx: Int) {
        try {
            settings.putInt(KEY_REPEAT_IDX, idx.coerceIn(0, 3))
        } catch (_: Exception) {
        }
    }

    private fun getFlag(key: String, default: Boolean): Boolean = try {
        settings.getBoolean(key, default)
    } catch (_: Exception) {
        default
    }

    private fun putFlag(key: String, value: Boolean) {
        try {
            settings.putBoolean(key, value)
        } catch (_: Exception) {
        }
    }
}

/**
 * Repeat-capsule label position → engine wiring.
 *
 * Uses the N-times engine API (`setAyahRepeatCount` / `clearAyahRepeat`,
 * landed mid-wave in AudioEngine.kt) exactly per spec:
 *
 * - "1x Loop" → setAyahRepeatCount(1) (+ RepeatMode OFF, so playback
 *   advances normally after the one replay instead of looping forever)
 * - "3x Loop" → setAyahRepeatCount(3) (+ RepeatMode OFF, same reason)
 * - "∞ Hifz" → clearAyahRepeat() + setRepeatMode(QUEUE) (loop the queue;
 *   clearing first so a stale counter can't eat the start of the loop)
 * - "Loop Off" → clearAyahRepeat() + setRepeatMode(OFF)
 *
 * Note: tapping any verse calls playSurahInAyahMode, which clears the
 * counter engine-side — arming 1x/3x then tapping a new verse disarms it.
 */
private fun applyMushafRepeat(idx: Int) {
    when (idx) {
        0 -> {
            AudioEngine.setAyahRepeatCount(1)
            AudioEngine.setRepeatMode(RepeatMode.OFF)
        }
        1 -> {
            AudioEngine.setAyahRepeatCount(3)
            AudioEngine.setRepeatMode(RepeatMode.OFF)
        }
        2 -> {
            AudioEngine.clearAyahRepeat()
            AudioEngine.setRepeatMode(RepeatMode.QUEUE)
        }
        else -> {
            AudioEngine.clearAyahRepeat()
            AudioEngine.setRepeatMode(RepeatMode.OFF)
        }
    }
}

/** "Surah X ayah Y + translation" share text (TafseerSheet-style). */
private fun mushafShareText(ayah: MushafAyahData, surahNameEn: String = "Al-Kahf"): String =
    buildString {
        appendLine("Surah $surahNameEn — Ayah ${ayah.ayahNumber}")
        appendLine()
        appendLine(ayah.textUthmani + (ayah.tajweedPart.orEmpty()))
        appendLine()
        appendLine("\"${ayah.translation}\"")
        appendLine()
        append("Shared via Ghaith")
    }

/**
 * Strict Noir Glass Mushaf — true-black canvas (glow zone -> #050506 +
 * ambient glow + film grain), monochrome hero banner, ayah blocks in the
 * NoirListRow recipe (engraved number well, chrome/ghost affordances).
 * Monochrome chrome keeps tajweed distinction and state readable through
 * fill elevation, chromium and opacity.
 *
 * The Mushaf page is dark paper (near-black #0E0E10, NOT pure white) for
 * eye comfort. Arabic/uthmani stays at 100% white on all dark wells —
 * never dimmed below 85%. Tab signature, audio/bookmark/tafsir state and
 * navigation targets are unchanged.
 *
 * Real reader: [surahId] state (persisted, default 18) drives content fetch
 * for all 114 surahs via [QuranAyahRepository.getAyahsForSurah], the header
 * shows real juz/page + metadata, audio goes through
 * [AudioEngine.playSurahInAyahMode], and bookmarks are `surah:ayah` pairs
 * across all surahs with a jump-to-verse section.
 */
object MushafScreen : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Filled.AutoStories)
            return remember {
                TabOptions(
                    index = 3u,
                    title = "Mushaf",
                    icon = icon
                )
            }
        }

    @Composable
    override fun Content() {
        var isTajweedActive by remember { mutableStateOf(MushafUiPrefs.isTajweed()) }
        var showTranslation by remember { mutableStateOf(MushafUiPrefs.showTranslation()) }
        var arabicFontSize by remember { mutableStateOf(MushafUiPrefs.fontSize()) }
        var surahId by remember { mutableStateOf(MushafUiPrefs.loadSurahId()) }
        var playingAyah by remember { mutableStateOf(1) }
        var allBookmarks by remember { mutableStateOf(MushafUiPrefs.loadAllBookmarks()) }
        var repeatStateIdx by remember { mutableStateOf(MushafUiPrefs.repeatIdx()) } // 0: 1x, 1: 3x Loop, 2: ∞ Hifz, 3: Loop Off
        var isPageViewMode by remember { mutableStateOf(MushafUiPrefs.isPageView()) }
        var activeTafsirAyah by remember { mutableStateOf<MushafAyahData?>(null) }
        var isSurahMenuOpen by remember { mutableStateOf(false) }
        var pendingJumpAyah by remember { mutableStateOf<Int?>(null) }

        val surah: Surah = remember(surahId) {
            QuranData.SURAHS.firstOrNull { it.id == surahId }
                ?: QuranData.SURAHS.firstOrNull { it.id == MushafUiPrefs.DEFAULT_SURAH_ID }
                ?: QuranData.SURAHS.first()
        }

        val bookmarkedAyahs: Set<Int> = remember(allBookmarks, surahId) {
            allBookmarks.filter { it.first == surahId }.map { it.second }.toSet()
        }

        // ---- Verse fetch (QuranAyahRepository is the single source of truth).
        // getAyahsForSurah suspends through cache → network → bundled seeds →
        // generated placeholders, so one call yields the final list; the
        // cacheGen refresh below only swaps text when a background write
        // (e.g. translation switch) lands, without flashing the skeleton.
        var verses by remember(surahId) { mutableStateOf<List<AyahVerse>?>(null) }
        var loadFailed by remember(surahId) { mutableStateOf(false) }
        var retryToken by remember(surahId) { mutableStateOf(0) }
        val cacheGen by QuranAyahRepository.cacheGen.collectAsState()

        LaunchedEffect(surahId, retryToken) {
            verses = null
            loadFailed = false
            try {
                verses = QuranAyahRepository.getAyahsForSurah(surahId)
            } catch (_: Exception) {
                loadFailed = true
            }
        }
        LaunchedEffect(cacheGen) {
            if (verses != null && !loadFailed) {
                try {
                    verses = QuranAyahRepository.getAyahsForSurah(surahId)
                } catch (_: Exception) {
                }
            }
        }

        val isLoading = verses == null && !loadFailed
        val ayahs = remember(verses) {
            (verses ?: emptyList()).sortedBy { it.ayahNo }.map { it.toMushafAyah() }
        }
        val allPlaceholders = ayahs.isNotEmpty() && (verses?.all { it.isPlaceholder } == true)

        val headerJuz = remember(surahId, playingAyah) {
            PageJuzIndex.juzOf(surahId, playingAyah)
        }
        val headerPage = remember(surahId, playingAyah) {
            PageJuzIndex.pageOf(surahId, playingAyah)
        }

        val isAudioPlaying by AudioEngine.isPlaying.collectAsState()
        val currentTrack by AudioEngine.currentTrack.collectAsState()

        val repeatStates = listOf("1x Loop", "3x Loop", "∞ Hifz", "Loop Off")

        val listState = rememberLazyListState()
        val uiScope = rememberCoroutineScope()

        fun selectSurah(id: Int, ayah: Int = 1) {
            val target = id.coerceIn(1, 114)
            isSurahMenuOpen = false
            pendingJumpAyah = null
            if (target != surahId) {
                surahId = target
                MushafUiPrefs.saveSurahId(target)
            }
            playingAyah = ayah.coerceAtLeast(1)
            uiScope.launch {
                try {
                    listState.scrollToItem(0)
                } catch (_: Exception) {
                }
            }
        }

        fun jumpToBookmark(targetSurah: Int, targetAyah: Int) {
            isSurahMenuOpen = false
            val ayah = targetAyah.coerceAtLeast(1)
            if (targetSurah != surahId) {
                surahId = targetSurah.coerceIn(1, 114)
                MushafUiPrefs.saveSurahId(surahId)
            }
            playingAyah = ayah
            if (isPageViewMode) {
                isPageViewMode = false
                MushafUiPrefs.setPageView(false)
            }
            // List for a just-switched surah composes after fetch; retry the
            // scroll when verses arrive (see LaunchedEffect below).
            pendingJumpAyah = ayah
            uiScope.launch {
                try {
                    listState.scrollToItem((2 + ayah - 1).coerceAtLeast(0))
                } catch (_: Exception) {
                }
            }
        }

        LaunchedEffect(surahId, verses) {
            pendingJumpAyah?.let { ayah ->
                try {
                    listState.scrollToItem((2 + ayah - 1).coerceAtLeast(0))
                } catch (_: Exception) {
                } finally {
                    pendingJumpAyah = null
                }
            }
        }

        // Visible-surah playback in ayah mode (mirrors MemorizationScreen's
        // playSurahInAyahMode invocation: surahId + reciter slug + start ayah).
        val playAyahAudio: (MushafAyahData) -> Unit = { ayah ->
            playingAyah = ayah.ayahNumber
            AudioEngine.playSurahInAyahMode(
                surahId = surahId,
                reciterSlug = MUSHAF_RECITER_SLUG,
                startAyahNo = ayah.ayahNumber
            )
        }

        val toggleGlobalAudio: () -> Unit = {
            if (isAudioPlaying) {
                AudioEngine.pause()
            } else {
                val targetAyah = ayahs.find { it.ayahNumber == playingAyah } ?: ayahs.firstOrNull()
                if (targetAyah != null) playAyahAudio(targetAyah)
            }
        }

        val toggleBookmark: (Int) -> Unit = { ayahNo ->
            val key = surahId to ayahNo
            allBookmarks = if (key in allBookmarks) allBookmarks - key else allBookmarks + key
            MushafUiPrefs.saveAllBookmarks(allBookmarks)
        }

        NoirScreenRoot {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp)
            ) {
                // 1. Top floating control capsule (noir glass).
                item {
                    MushafControlCapsule(
                        selectedSurahId = surahId,
                        selectedSurahLabel = "${surah.nameEn} (${surah.id})",
                        juzNo = headerJuz,
                        pageNo = headerPage,
                        isSurahMenuOpen = isSurahMenuOpen,
                        onToggleSurahMenu = { isSurahMenuOpen = !isSurahMenuOpen },
                        onSelectSurah = { selectSurah(it) },
                        arabicFontSize = arabicFontSize,
                        onCycleFontSize = {
                            arabicFontSize = when (arabicFontSize) {
                                28 -> 32
                                32 -> 36
                                else -> 28
                            }
                            MushafUiPrefs.setFontSize(arabicFontSize)
                        },
                        showTranslation = showTranslation,
                        onToggleTranslation = {
                            showTranslation = !showTranslation
                            MushafUiPrefs.setShowTranslation(showTranslation)
                        },
                        isTajweed = isTajweedActive,
                        onToggleTajweed = {
                            isTajweedActive = !isTajweedActive
                            MushafUiPrefs.setTajweed(isTajweedActive)
                        },
                        isAudioPlaying = isAudioPlaying,
                        onToggleAudio = toggleGlobalAudio
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 2. Surah ornamentation header banner (real metadata).
                item {
                    SurahHeaderBanner(
                        surah = surah,
                        juzNo = headerJuz,
                        pageNo = headerPage
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 3. Loading / error / offline states (never blank-crash).
                if (isLoading) {
                    item {
                        MushafLoadingCard(surahNameEn = surah.nameEn)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                if (loadFailed && verses == null) {
                    item {
                        MushafErrorCard(
                            surahNameEn = surah.nameEn,
                            onRetry = { retryToken++ }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                if (!isLoading && !loadFailed && allPlaceholders) {
                    item {
                        MushafOfflineNoticeCard(
                            surahNameEn = surah.nameEn,
                            onRetry = { retryToken++ }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // 4. Ayah reading stream OR classic dark-paper page view.
                if (isPageViewMode) {
                    item {
                        MushafPageViewCard(
                            ayahs = ayahs,
                            activeAyah = playingAyah,
                            arabicFontSize = arabicFontSize,
                            isTajweed = isTajweedActive,
                            surahId = surahId,
                            surahNameAr = surah.nameAr,
                            juzNo = headerJuz,
                            pageNo = headerPage,
                            onSelectAyah = { ayah -> playAyahAudio(ayah) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                } else {
                    items(ayahs, key = { it.ayahNumber }) { ayah ->
                        val isActive = if (currentTrack?.surahId == surahId && (currentTrack?.ayahNo ?: 0) > 0) {
                            (currentTrack?.ayahNo ?: -1) == ayah.ayahNumber
                        } else {
                            playingAyah == ayah.ayahNumber
                        }
                        val isBookmarked = bookmarkedAyahs.contains(ayah.ayahNumber)

                        AyahBlock(
                            ayah = ayah,
                            isActive = isActive,
                            isBookmarked = isBookmarked,
                            isTajweed = isTajweedActive,
                            showTranslation = showTranslation,
                            arabicFontSize = arabicFontSize,
                            surahId = surahId,
                            onCardClick = { playingAyah = ayah.ayahNumber },
                            onPlayClick = {
                                if (isActive && isAudioPlaying) {
                                    AudioEngine.pause()
                                } else {
                                    playAyahAudio(ayah)
                                }
                            },
                            onBookmarkClick = { toggleBookmark(ayah.ayahNumber) },
                            onTafsirClick = { activeTafsirAyah = ayah },
                            onShareClick = {
                                if (ShareSheet.isAvailable()) {
                                    ShareSheet.shareText(
                                        title = "${surah.nameEn} ${ayah.ayahNumber}",
                                        text = mushafShareText(ayah, surah.nameEn)
                                    )
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // 5. Saved bookmarks across all surahs (tap jumps to surah/ayah).
                item {
                    MushafBookmarksSection(
                        bookmarks = allBookmarks,
                        onJump = { s, a -> jumpToBookmark(s, a) },
                        onRemove = { s, a ->
                            allBookmarks = allBookmarks - (s to a)
                            MushafUiPrefs.saveAllBookmarks(allBookmarks)
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 6. Reading-mode bottom utilities capsule.
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    MushafBottomToolsCapsule(
                        repeatLabel = repeatStates[repeatStateIdx],
                        onCycleRepeat = {
                            val nextIdx = (repeatStateIdx + 1) % repeatStates.size
                            repeatStateIdx = nextIdx
                            MushafUiPrefs.setRepeatIdx(nextIdx)
                            applyMushafRepeat(nextIdx)
                        },
                        onOpenTafsir = {
                            activeTafsirAyah = ayahs.find { it.ayahNumber == playingAyah } ?: ayahs.firstOrNull()
                        },
                        isPageView = isPageViewMode,
                        onTogglePageView = {
                            isPageViewMode = !isPageViewMode
                            MushafUiPrefs.setPageView(isPageViewMode)
                        }
                    )
                    Spacer(modifier = Modifier.height(120.dp))
                }
            }

            // Tafsir modal sheet dialog (noir).
            if (activeTafsirAyah != null) {
                TafsirDialog(
                    ayah = activeTafsirAyah!!,
                    surahNameEn = surah.nameEn,
                    onDismiss = { activeTafsirAyah = null }
                )
            }
        }
    }
}

/**
 * Top floating control capsule in noir glass: ghost surah selector, engraved
 * Juz/Page indicator, ghost preference wells, chromium audio disc.
 */
@Composable
private fun MushafControlCapsule(
    selectedSurahId: Int,
    selectedSurahLabel: String,
    juzNo: Int,
    pageNo: Int,
    isSurahMenuOpen: Boolean,
    onToggleSurahMenu: () -> Unit,
    onSelectSurah: (Int) -> Unit,
    arabicFontSize: Int,
    onCycleFontSize: () -> Unit,
    showTranslation: Boolean,
    onToggleTranslation: () -> Unit,
    isTajweed: Boolean,
    onToggleTajweed: () -> Unit,
    isAudioPlaying: Boolean,
    onToggleAudio: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(GhaisNoir.cardFillSoft())
            .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(32.dp))
            .topSpecular(inset = 30.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Surah selector — ghost pill + noir dropdown (all 114 surahs).
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(GhaisNoir.Fill2)
                        .border(1.dp, GhaisNoir.BorderGhost, RoundedCornerShape(20.dp))
                        .noirClickable(onClick = onToggleSurahMenu)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = "Surah Selector",
                        tint = GhaisNoir.TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectedSurahLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GhaisNoir.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Dropdown",
                        tint = GhaisNoir.TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = isSurahMenuOpen,
                    onDismissRequest = onToggleSurahMenu,
                    modifier = Modifier.background(Color(0xFF141416))
                ) {
                    QuranData.SURAHS.forEach { s ->
                        val selected = s.id == selectedSurahId
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = s.id.toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary
                                    )
                                    Text(
                                        text = s.nameEn,
                                        color = if (selected) GhaisNoir.TextPrimary else GhaisNoir.TextSecondary,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = s.nameAr,
                                        color = GhaisNoir.TextTertiary,
                                        fontSize = 12.sp
                                    )
                                }
                            },
                            onClick = { onSelectSurah(s.id) }
                        )
                    }
                }
            }

            // Juz / Page indicator — engraved inset (real header numbers).
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(GhaisNoir.insetFill())
                    .border(1.dp, GhaisNoir.InsetBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "JUZ",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = GhaisNoir.TextTertiary
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = juzNo.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GhaisNoir.TextPrimary
                    )
                    Text(
                        text = " • ",
                        fontSize = 10.sp,
                        color = GhaisNoir.TextTertiary
                    )
                    Text(
                        text = "P.",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = GhaisNoir.TextTertiary
                    )
                    Text(
                        text = pageNo.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GhaisNoir.TextPrimary
                    )
                }
            }

            // Quick preferences: font-size well, translation + tajweed ghosts, chrome audio disc.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 1. Font size well.
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(GhaisNoir.wellFill())
                        .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                        .noirClickable(onClick = onCycleFontSize),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = "Font size: $arabicFontSize",
                            tint = GhaisNoir.TextPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${arabicFontSize}pt",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = GhaisNoir.TextTertiary,
                            lineHeight = 9.sp
                        )
                    }
                }

                // 2. Translation ghost toggle — active = elevated fill + primary label.
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (showTranslation) GhaisNoir.Fill4 else GhaisNoir.Fill2)
                        .border(
                            1.dp,
                            if (showTranslation) GhaisNoir.SpecularTop else GhaisNoir.BorderGhost,
                            CircleShape
                        )
                        .noirClickable(onClick = onToggleTranslation),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Toggle Translation",
                        tint = if (showTranslation) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // 3. Tajweed ghost toggle.
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isTajweed) GhaisNoir.Fill4 else GhaisNoir.Fill2)
                        .border(
                            1.dp,
                            if (isTajweed) GhaisNoir.SpecularTop else GhaisNoir.BorderGhost,
                            RoundedCornerShape(18.dp)
                        )
                        .noirClickable(onClick = onToggleTajweed)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Tajweed",
                            tint = if (isTajweed) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Tajweed",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTajweed) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary
                        )
                    }
                }

                // 4. Audio disc — chromium while playing, clay well otherwise.
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isAudioPlaying) GhaisNoir.chromeFill() else GhaisNoir.wellFill(),
                            CircleShape
                        )
                        .border(
                            1.dp,
                            if (isAudioPlaying) Color.White.copy(alpha = 0.4f) else GhaisNoir.BorderCard,
                            CircleShape
                        )
                        .noirClickable(onClick = onToggleAudio),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAudioPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                        contentDescription = if (isAudioPlaying) "Pause Recitation" else "Play Recitation",
                        tint = if (isAudioPlaying) GhaisNoir.OnChrome else GhaisNoir.TextPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

/**
 * Monochrome surah banner: sacred-geometry backdrop, ghost info pills,
 * engraved rosette with uthmani at 100% white, luminous Bismillah.
 * Metadata (revelation type, verse count, juz/page) is real per surah.
 */
@Composable
private fun SurahHeaderBanner(
    surah: Surah,
    juzNo: Int,
    pageNo: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(GhaisNoir.cardFill())
            .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(22.dp))
            .topSpecular(inset = 30.dp)
            .padding(18.dp)
    ) {
        // Monochrome sacred-geometry backdrop — white line-work only.
        Canvas(modifier = Modifier.matchParentSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r1 = size.minDimension * 0.35f
            val r2 = size.minDimension * 0.46f

            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = r1,
                center = center,
                style = Stroke(width = 1.5f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = r2,
                center = center,
                style = Stroke(
                    width = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )
            )

            // 8-pointed star motif.
            val path = Path()
            val numPoints = 8
            val outerR = size.minDimension * 0.42f
            val innerR = size.minDimension * 0.22f
            for (i in 0 until numPoints * 2) {
                val angle = (i * PI / numPoints) - (PI / 2)
                val r = if (i % 2 == 0) outerR else innerR
                val x = cx + (r * cos(angle)).toFloat()
                val y = cy + (r * sin(angle)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.05f),
                style = Stroke(width = 1.5f)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Info pills — ghost washes, monochrome glyphs (real metadata).
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NoirInfoPill(icon = Icons.Default.Place, text = surah.revelationType)
                NoirInfoPill(icon = Icons.Default.FilterList, text = "${surah.ayahsCount} Verses")
                NoirInfoPill(icon = Icons.Default.Star, text = surah.transliteration, emphasized = true)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Surah rosette — engraved well, uthmani at 100% white.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    GhaisNoir.BorderCard,
                                    GhaisNoir.SpecularTop.copy(alpha = 0.5f)
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(GhaisNoir.insetFill())
                        .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(24.dp))
                        .padding(horizontal = 22.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "سُورَةُ ${surah.nameAr}",
                            fontSize = 32.sp,
                            lineHeight = 64.sp,
                            fontWeight = FontWeight.Normal,
                            style = GhaisTypography.quranScript,
                            color = GhaisNoir.TextPrimary
                        )
                        Text(
                            text = surah.nameEn.uppercase(),
                            fontSize = 10.sp,
                            letterSpacing = 2.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GhaisNoir.TextTertiary
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    GhaisNoir.SpecularTop.copy(alpha = 0.5f),
                                    GhaisNoir.BorderCard,
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bismillah — uthmani at 100% white, translation secondary.
            // Surah 9 has no Bismillah header (single source of truth:
            // QuranAyahRepository.hasBasmalahHeader); surah 1 keeps it since
            // verse 1 IS the Basmalah there.
            if (QuranAyahRepository.hasBasmalahHeader(surah.id)) {
                Text(
                    text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Normal,
                    style = GhaisTypography.quranScript,
                    color = GhaisNoir.TextPrimary,
                    lineHeight = 52.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "In the name of Allah, the Entirely Merciful, the Especially Merciful.",
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    color = GhaisNoir.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Juz / page line — real header numbers.
            Text(
                text = "Juz $juzNo • Page $pageNo",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                color = GhaisNoir.TextTertiary
            )
        }
    }
}

/** Ghost info pill — informational, zero hue. */
@Composable
private fun NoirInfoPill(icon: ImageVector, text: String, emphasized: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (emphasized) GhaisNoir.Fill4 else GhaisNoir.Fill2)
            .border(1.dp, GhaisNoir.BorderGhost, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (emphasized) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
            color = if (emphasized) GhaisNoir.TextPrimary else GhaisNoir.TextSecondary
        )
    }
}

/**
 * Ayah block in the NoirListRow recipe: soft glass fill (active = stronger
 * wash) + card border + top-only specular, chromium edge strip while live,
 * engraved number well, chrome/ghost action cluster.
 *
 * Tajweed distinction is color-only at 100% white — QCF Hafs ships a single
 * Regular file, so all uthmani spans stay Normal (no synthetic bold).
 */
@Composable
private fun AyahBlock(
    ayah: MushafAyahData,
    isActive: Boolean,
    isBookmarked: Boolean,
    isTajweed: Boolean,
    showTranslation: Boolean,
    arabicFontSize: Int,
    surahId: Int = 18,
    onCardClick: () -> Unit,
    onPlayClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onTafsirClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (isActive) GhaisNoir.cardFillActive() else GhaisNoir.cardFillSoft())
            .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(18.dp))
            .topSpecular(inset = 24.dp)
            .clickable { onCardClick() }
    ) {
        // Chromium edge strip while this ayah is live (monochrome, not emerald).
        if (isActive) {
            Box(modifier = Modifier.matchParentSize()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .width(4.dp)
                        .clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
                        .background(GhaisNoir.chromeFill())
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Header row: engraved index + live badge + ghost/chrome actions.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(GhaisNoir.wellFill())
                            .border(1.dp, GhaisNoir.BorderCard, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ayah.ayahNumber.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GhaisNoir.TextPrimary
                        )
                    }

                    if (isActive) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(GhaisNoir.Fill4)
                                .border(1.dp, GhaisNoir.BorderGhost, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Waveform",
                                tint = GhaisNoir.TextPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "RECITING NOW",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = GhaisNoir.TextPrimary,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    if (ayah.isPlaceholder) {
                        Text(
                            text = "OFFLINE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = GhaisNoir.TextTertiary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(GhaisNoir.Fill2)
                                .border(1.dp, GhaisNoir.BorderGhost, RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Play / pause disc — chromium while live, clay well otherwise.
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) GhaisNoir.chromeFill() else GhaisNoir.wellFill(),
                                CircleShape
                            )
                            .border(
                                1.dp,
                                if (isActive) Color.White.copy(alpha = 0.4f) else GhaisNoir.BorderCard,
                                CircleShape
                            )
                            .noirClickable(onClick = onPlayClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isActive) "Pause verse audio" else "Play verse audio",
                            tint = if (isActive) GhaisNoir.OnChrome else GhaisNoir.TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    GhostCircleButton(
                        icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark ayah",
                        tint = if (isBookmarked) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                        onClick = onBookmarkClick
                    )
                    GhostCircleButton(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = "Tafsir for verse ${ayah.ayahNumber}",
                        onClick = onTafsirClick
                    )
                    GhostCircleButton(
                        icon = Icons.Default.Share,
                        contentDescription = "Share ayah",
                        onClick = onShareClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Uthmani text — 100% white, all Normal (single Regular file).
            // Rosette is plain text: the font's digit ligature draws the ring
            // around the digits, so never emit a bare "۝" — that U+06DD is an
            // empty ring and would double up.
            val arabicAnnotated = buildAnnotatedString {
                val base = GhaisNoir.TextPrimary

                if (isTajweed && ayah.tajweedPart != null) {
                    if (ayah.tajweedPrefix) {
                        withStyle(SpanStyle(color = base, fontWeight = FontWeight.Normal)) {
                            append(ayah.tajweedPart)
                        }
                        withStyle(SpanStyle(color = base)) {
                            append(ayah.textUthmani)
                        }
                    } else {
                        withStyle(SpanStyle(color = base)) {
                            append(ayah.textUthmani)
                        }
                        withStyle(SpanStyle(color = base, fontWeight = FontWeight.Normal)) {
                            append(ayah.tajweedPart)
                        }
                    }
                } else {
                    withStyle(SpanStyle(color = base)) {
                        append(ayah.textUthmani)
                        if (ayah.tajweedPart != null) {
                            append(ayah.tajweedPart)
                        }
                    }
                }

                appendAyahMark(ayah.ayahNumber)
            }

            val showBasmalah = ayah.ayahNumber == 1 && QuranAyahRepository.hasBasmalahHeader(surahId)

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                if (showBasmalah) {
                    Text(
                        text = QuranAyahRepository.BASMALAH,
                        fontSize = (arabicFontSize * 0.85f).sp,
                        fontWeight = FontWeight.Normal,
                        style = GhaisTypography.quranScript,
                        color = GhaisNoir.TextPrimary,
                        textAlign = TextAlign.Center,
                        lineHeight = (arabicFontSize * 0.85f * GhaisTypography.QURAN_LINE_HEIGHT_RATIO).sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    )
                }
                Text(
                    text = arabicAnnotated,
                    fontSize = arabicFontSize.sp,
                    fontWeight = FontWeight.Normal,
                    style = GhaisTypography.quranScript,
                    // Start, not End: inside the Rtl provider above, End
                    // resolves to the LEFT edge.
                    textAlign = TextAlign.Start,
                    lineHeight = (arabicFontSize * GhaisTypography.QURAN_LINE_HEIGHT_RATIO).sp,
                    inlineContent = ayahMarkContent(
                        ringSize = (arabicFontSize * 0.75f).sp,
                        digitSize = (arabicFontSize * 0.35f).sp,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (showTranslation) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = ayah.translation,
                    fontSize = 14.sp,
                    color = if (isActive) GhaisNoir.TextPrimary else GhaisNoir.TextSecondary,
                    lineHeight = 22.sp
                )

                if (ayah.footnote != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ayah.footnote,
                        fontSize = 11.sp,
                        color = GhaisNoir.TextTertiary,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

/** Ghost circular micro-action — Fill2 wash + hairline, monochrome glyph. */
@Composable
private fun GhostCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = GhaisNoir.TextTertiary
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(GhaisNoir.Fill2)
            .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
            .noirClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
    }
}

/** Loading skeleton while the surah's verses resolve (cache → network). */
@Composable
private fun MushafLoadingCard(surahNameEn: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(GhaisNoir.cardFillSoft())
            .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(18.dp))
            .topSpecular(inset = 24.dp)
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = GhaisNoir.TextPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Loading $surahNameEn…",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = GhaisNoir.TextSecondary
            )
            Spacer(modifier = Modifier.height(14.dp))
            repeat(3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GhaisNoir.Fill2)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/** Error card with retry when the verse fetch throws. */
@Composable
private fun MushafErrorCard(surahNameEn: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(GhaisNoir.cardFillSoft())
            .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(18.dp))
            .topSpecular(inset = 24.dp)
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = GhaisNoir.TextTertiary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Couldn't load $surahNameEn",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = GhaisNoir.TextPrimary
            )
            Text(
                text = "Check your connection and try again.",
                fontSize = 12.sp,
                color = GhaisNoir.TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(GhaisNoir.chromeFill())
                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                    .noirClickable(onClick = onRetry)
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = GhaisNoir.OnChrome,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Retry",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = GhaisNoir.OnChrome
                )
            }
        }
    }
}

/** Honest offline notice when only placeholder verses are available. */
@Composable
private fun MushafOfflineNoticeCard(surahNameEn: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(GhaisNoir.cardFillSoft())
            .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(18.dp))
            .topSpecular(inset = 24.dp)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$surahNameEn isn't downloaded yet",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = GhaisNoir.TextPrimary
                )
                Text(
                    text = "Showing verse placeholders — connect to load the full text.",
                    fontSize = 12.sp,
                    color = GhaisNoir.TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(GhaisNoir.Fill4)
                    .border(1.dp, GhaisNoir.SpecularTop, RoundedCornerShape(16.dp))
                    .noirClickable(onClick = onRetry)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry loading surah",
                    tint = GhaisNoir.TextPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Retry",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GhaisNoir.TextPrimary
                )
            }
        }
    }
}

/**
 * Saved bookmarks across ALL surahs. Tap jumps to that surah/ayah; the X
 * removes the bookmark. Always visible so the feature is discoverable.
 */
@Composable
private fun MushafBookmarksSection(
    bookmarks: Set<Pair<Int, Int>>,
    onJump: (Int, Int) -> Unit,
    onRemove: (Int, Int) -> Unit
) {
    val sorted = remember(bookmarks) {
        bookmarks.sortedWith(compareBy({ it.first }, { it.second }))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(GhaisNoir.cardFillSoft())
            .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(18.dp))
            .topSpecular(inset = 24.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = GhaisNoir.TextPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Bookmarks",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = GhaisNoir.TextPrimary
                )
                Text(
                    text = sorted.size.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GhaisNoir.TextTertiary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(GhaisNoir.Fill2)
                        .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (sorted.isEmpty()) {
                Text(
                    text = "No bookmarks yet — tap the bookmark icon on any verse.",
                    fontSize = 12.sp,
                    color = GhaisNoir.TextTertiary,
                    lineHeight = 17.sp
                )
            } else {
                sorted.forEach { (s, a) ->
                    val name = QuranData.SURAHS.firstOrNull { it.id == s }?.nameEn ?: "Surah $s"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(GhaisNoir.Fill2)
                            .border(1.dp, GhaisNoir.BorderGhost, RoundedCornerShape(12.dp))
                            .noirClickable(onClick = { onJump(s, a) })
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(GhaisNoir.wellFill())
                                .border(1.dp, GhaisNoir.BorderCard, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = a.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GhaisNoir.TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GhaisNoir.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Ayah $a",
                                fontSize = 11.sp,
                                color = GhaisNoir.TextTertiary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Jump to $name ayah $a",
                            tint = GhaisNoir.TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        GhostCircleButton(
                            icon = Icons.Default.Close,
                            contentDescription = "Remove bookmark $name ayah $a",
                            onClick = { onRemove(s, a) }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * Reading-mode bottom utilities capsule in noir glass: ghost repeat + tafsir
 * pills, chromium page/stream toggle. State reads through fill elevation.
 */
@Composable
private fun MushafBottomToolsCapsule(
    repeatLabel: String,
    onCycleRepeat: () -> Unit,
    onOpenTafsir: () -> Unit,
    isPageView: Boolean,
    onTogglePageView: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(32.dp))
                .background(GhaisNoir.cardFillSoft())
                .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(32.dp))
                .topSpecular(inset = 30.dp)
                .padding(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Repeat cycle pill — elevated when looping.
                val repeatOff = repeatLabel.contains("Off")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (repeatOff) GhaisNoir.Fill2 else GhaisNoir.Fill4)
                        .border(
                            1.dp,
                            if (repeatOff) GhaisNoir.BorderGhost else GhaisNoir.SpecularTop,
                            RoundedCornerShape(20.dp)
                        )
                        .noirClickable(onClick = onCycleRepeat)
                        .padding(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Audio Repeat Loop",
                        tint = if (repeatOff) GhaisNoir.TextTertiary else GhaisNoir.TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = repeatLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (repeatOff) GhaisNoir.TextTertiary else GhaisNoir.TextPrimary
                    )
                }

                // Tafsir trigger — ghost pill.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(GhaisNoir.Fill2)
                        .border(1.dp, GhaisNoir.BorderGhost, RoundedCornerShape(20.dp))
                        .noirClickable(onClick = onOpenTafsir)
                        .padding(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = "Open Tafsir",
                        tint = GhaisNoir.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tafsir",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GhaisNoir.TextPrimary
                    )
                }

                // Page/stream toggle — chromium disc.
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(GhaisNoir.chromeFill())
                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                        .noirClickable(onClick = onTogglePageView),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPageView) Icons.Default.ViewAgenda else Icons.Default.Layers,
                        contentDescription = if (isPageView) "Switch to Ayah stream" else "Switch to Mushaf page",
                        tint = GhaisNoir.OnChrome,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/** Near-black dark paper for the Mushaf page — eye comfort, no pure white. */
private val NoirDarkPaper = Color(0xFF0E0E10)

// Basmalah text + rule live in QuranAyahRepository (single source of truth).
// Surah 1 IS the Basmalah (verse 1) and Surah 9 has no header; all others do.

/**
 * Traditional Mushaf page view on dark paper (near-black, not pure white):
 * engraved header, continuous uthmani at 100% white, hairline rules.
 * Tajweed distinction is weight-only at 100% white — zero hue.
 */
@Composable
private fun MushafPageViewCard(
    ayahs: List<MushafAyahData>,
    activeAyah: Int,
    arabicFontSize: Int,
    isTajweed: Boolean,
    surahId: Int = 18,
    surahNameAr: String = "الكهف",
    juzNo: Int = 15,
    pageNo: Int = 293,
    onSelectAyah: (MushafAyahData) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(NoirDarkPaper)
            .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(20.dp))
            .topSpecular(inset = 28.dp)
            .noirClickable(onClick = {})
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page header — engraved, monochrome. Runs RTL so the juz lands
            // top-RIGHT and the page number top-LEFT, per printed Mushaf.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "جُزْء ${toArabicDigits(juzNo)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp,
                        color = GhaisNoir.TextSecondary
                    )
                    Text(
                        text = "سُورَةُ $surahNameAr",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        style = GhaisTypography.quranScript,
                        lineHeight = (13 * GhaisTypography.QURAN_LINE_HEIGHT_RATIO).sp,
                        color = GhaisNoir.TextPrimary
                    )
                    Text(
                        text = "صَفْحَة ${toArabicDigits(pageNo)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp,
                        color = GhaisNoir.TextSecondary
                    )
                }
            }

            HorizontalDivider(color = GhaisNoir.BorderGhost, thickness = 1.dp)

            Spacer(modifier = Modifier.height(14.dp))

            // Continuous uthmani — all Normal (single Regular file; no synthetic bold).
            // Rosette is plain text: the font's digit ligature draws the ring
            // around the digits, so never emit a bare "۝" — that U+06DD is an
            // empty ring and would double up.
            val pageText = buildAnnotatedString {
                ayahs.forEach { ayah ->
                    if (isTajweed && ayah.tajweedPart != null) {
                        if (ayah.tajweedPrefix) {
                            withStyle(SpanStyle(color = GhaisNoir.TextPrimary, fontWeight = FontWeight.Normal)) {
                                append(ayah.tajweedPart)
                            }
                            withStyle(SpanStyle(color = GhaisNoir.TextPrimary, fontWeight = FontWeight.Normal)) {
                                append(ayah.textUthmani)
                            }
                        } else {
                            withStyle(SpanStyle(color = GhaisNoir.TextPrimary, fontWeight = FontWeight.Normal)) {
                                append(ayah.textUthmani)
                            }
                            withStyle(SpanStyle(color = GhaisNoir.TextPrimary, fontWeight = FontWeight.Normal)) {
                                append(ayah.tajweedPart)
                            }
                        }
                    } else {
                        withStyle(SpanStyle(color = GhaisNoir.TextPrimary, fontWeight = FontWeight.Normal)) {
                            append(ayah.textUthmani)
                            if (ayah.tajweedPart != null) {
                                append(ayah.tajweedPart)
                            }
                        }
                    }

                    appendAyahMark(ayah.ayahNumber)
                }
            }

            val showBasmalah = ayahs.firstOrNull()?.ayahNumber == 1 && QuranAyahRepository.hasBasmalahHeader(surahId)

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                if (showBasmalah) {
                    Text(
                        text = QuranAyahRepository.BASMALAH,
                        fontSize = ((arabicFontSize - 2) * 0.85f).sp,
                        fontWeight = FontWeight.Normal,
                        style = GhaisTypography.quranScript,
                        color = GhaisNoir.TextPrimary,
                        textAlign = TextAlign.Center,
                        lineHeight = ((arabicFontSize - 2) * 0.85f * GhaisTypography.QURAN_LINE_HEIGHT_RATIO).sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    )
                }
                Text(
                    text = pageText,
                    fontSize = (arabicFontSize - 2).sp,
                    style = GhaisTypography.quranScript,
                    lineHeight = ((arabicFontSize - 2) * GhaisTypography.QURAN_LINE_HEIGHT_RATIO).sp,
                    // Required, not decorative: `style =` replaces LocalTextStyle
                    // and the rosette is appended outside any span, so without
                    // this the ring is the only glyph falling back to
                    // LocalContentColor.
                    color = GhaisNoir.TextPrimary,
                    // Start, not End: inside the Rtl provider above, End
                    // resolves to the LEFT edge.
                    textAlign = TextAlign.Start,
                    inlineContent = ayahMarkContent(
                        ringSize = ((arabicFontSize - 2) * 0.75f).sp,
                        digitSize = ((arabicFontSize - 2) * 0.35f).sp,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .noirClickable(onClick = {
                            ayahs.find { it.ayahNumber == activeAyah }?.let(onSelectAyah)
                        })
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = GhaisNoir.BorderGhost, thickness = 1.dp)

            // Page footer.
            Text(
                text = "— ${toArabicDigits(pageNo)} —",
                fontSize = 12.sp,
                letterSpacing = 0.sp,
                color = GhaisNoir.TextTertiary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * Tafsir sheet dialog in noir glass: dark card, 100%-white quote Arabic,
 * ghost rule, secondary body, white Close affordance. Verse commentary is
 * not bundled on this screen, so the dialog shows the translation with an
 * honest note instead of a hardcoded snippet.
 */
@Composable
private fun TafsirDialog(
    ayah: MushafAyahData,
    surahNameEn: String,
    onDismiss: () -> Unit
) {
    val body = ayah.tafsirSnippet.ifBlank {
        "Full tafsir commentary isn't bundled on this screen — showing the translation for $surahNameEn ayah ${ayah.ayahNumber}.\n\n\"${ayah.translation}\""
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tafsir Ibn Kathir",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = GhaisNoir.TextPrimary
                )
                Text(
                    text = "$surahNameEn ${ayah.ayahNumber}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GhaisNoir.TextTertiary
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = buildAnnotatedString {
                        append(ayah.textUthmani)
                        if (ayah.tajweedPart != null) {
                            append(ayah.tajweedPart)
                        }
                        appendAyahMark(ayah.ayahNumber)
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    style = GhaisTypography.quranScript,
                    color = GhaisNoir.TextPrimary,
                    // This dialog sits OUTSIDE the Rtl provider, so End is
                    // already the right edge here — leave it alone.
                    textAlign = TextAlign.End,
                    lineHeight = 36.sp,
                    inlineContent = ayahMarkContent(ringSize = 14.sp, digitSize = 6.5.sp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = GhaisNoir.BorderGhost)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = body,
                    fontSize = 14.sp,
                    color = GhaisNoir.TextSecondary,
                    lineHeight = 21.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = GhaisNoir.TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF131316),
        shape = RoundedCornerShape(20.dp)
    )
}
