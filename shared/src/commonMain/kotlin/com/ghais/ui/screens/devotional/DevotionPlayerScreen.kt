package com.ghais.ui.screens.devotional

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import com.ghais.data.repository.DevotionalRepository
import com.ghais.domain.model.DEVOTIONAL_TRACK_SLUG
import com.ghais.domain.model.DevotionKind
import com.ghais.domain.model.DevotionalItem
import com.ghais.domain.model.RepeatMode
import com.ghais.domain.model.TrackItem
import com.ghais.domain.model.ayahSpan
import com.ghais.player.AudioEngine
import com.ghais.player.PlayerBackHandler
import com.ghais.ui.components.noir.GhostPillButton
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.player.PlayerControls
import com.ghais.ui.screens.player.SpeedsList
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography
import com.ghais.ui.util.AyahEndMark
import com.ghais.ui.util.appendAyahMark
import com.ghais.ui.util.ayahMarkContent
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Bottom inset reserved for the overlay MiniPlayer.
 *
 * `App.kt` computes `isPushedScreen` from `NowPlayingScreen` alone, so this
 * screen counts as a pushed screen and the MiniPlayer is drawn ON TOP of it
 * (zIndex 1.f inside the same Box). The reader keeps its own content above that
 * line instead of being covered by it. Drop this spacer only together with the
 * `App.kt` special case that stops the MiniPlayer overlaying a full-screen
 * reader.
 */
private val MINI_PLAYER_CLEARANCE = 120.dp

/** Verse text size for the Uthmani (ruqiyah) passage — NowPlayingLyricsCard's 25/50. */
private val QURANIC_TEXT_SIZE = 25.sp

/** Supplication (non-Uthmani) text size, with tashkeel headroom. */
private val NASKH_TEXT_SIZE = 23.sp

/** Lifecycle of the reader: resolve -> read, or resolve -> admit the id is dead. */
private enum class ReaderPhase { Loading, Ready, Missing }

/**
 * Devotional reader + player — one screen for both collections.
 *
 * READ and LISTEN are the same surface here because a devotional entry is the
 * same three things in both catalogues: Arabic, a translation, a citation. The
 * only thing that varies is whether audio resolves, and that is read from the
 * RESOLVED queue, never from [DevotionalItem.hasAudio] — the ruqiyah seed
 * ships `audioUrl = null` and gets its everyayah url built at read time, so
 * keying off `hasAudio` would mute all 20 ruqiyah entries. This is the same
 * correction `DevotionListScreen` makes on its rows.
 *
 * SOURCE OF TRUTH — the engine owns the reader, the reader never owns the engine:
 * - The engine's queue (`playQueue`) is the single place a collection is queued,
 *   so prev/next are `AudioEngine.skipPrevious()` / `skipNext()` and the text
 *   follows `AudioEngine.currentTrack` by matching the track back to its item.
 * - When nothing devotional is loaded (the entire dua collection, or a screen
 *   opened over a surah that the first-open effect has not replaced yet), the
 *   reader falls back to the id it was opened with, and prev/next page that id
 *   through the catalogue without touching the engine at all. A collection with
 *   no audio must not be able to stop a surah that is already playing.
 * - `playQueue` is called at most once per screen instance, and only when the
 *   engine is not already on this exact entry — that second condition is what
 *   makes reopening this screen from the MiniPlayer a no-op instead of a
 *   restart. (MiniPlayer.openPlayerOnce() only used to recognise
 *   NowPlayingScreen; it now knows this screen too.)
 *
 * Arabic rendering is per KIND, decided by [DevotionalItem.isQuranic] ("has
 * Quran coordinates" — true for every ruqiyah row, false for every dua):
 * - Ruqiyah text is Uthmani Quranic text, so it gets
 *   [GhaisTypography.quranScript] (QCF Hafs) and the inline end-of-ayah rosette,
 *   exactly as `NowPlayingLyricsCard` renders a verse.
 * - Dua text is scripture in the literary sense but NOT Uthmani Quran text (it
 *   carries ordinary Naskh orthography), so it gets [GhaisTypography.arabicBody]
 *   with `letterSpacing = 0.sp` — QCF Hafs' `rlig` substitution and its
 *   Uthmani glyph inventory are for Quran text, not for adhkar.
 * - Both blocks are wrapped in an RTL `CompositionLocalProvider` and neither
 *   rides a Latin display style: the ambient MaterialTheme style carries
 *   negative tracking, and non-zero tracking breaks cursive joins.
 * - No Arabic-Indic digit is ever rendered through `quranScript`. The ayah
 *   rosettes come from `appendAyahMark` / `AyahEndMark`, which draw the ring
 *   themselves and use `rosetteDigitFont` for the digits — see the trap note in
 *   `ui/util/AyahRosette.kt`.
 *
 * Drag-down (and the handle, and system back) all run through one
 * [animateOutAndDismiss]: the content animates itself off-screen first and the
 * pop happens after, so the stack transition never animates the same pixels.
 * That is the invariant `NowPlayingScreen` established; see the note on the
 * App.kt transition for what this screen still needs there.
 */
class DevotionPlayerScreen(private val itemId: String) : Screen {
    // Per-instance tag, copied from NowPlayingScreen: Voyager's default
    // Screen.key is the class name, so two stacked instances alias one
    // SaveableState slot and the revealed duplicate renders blank. A stacked
    // push is still reachable (the MiniPlayer's own single-flight guard only
    // knows what it knows), so each instance owns isolated state.
    private val instanceTag = nextInstanceTag()

    override val key: ScreenKey
        get() = "DevotionPlayerScreen#$instanceTag"

    companion object {
        private var instanceCounter = 0L
        private fun nextInstanceTag(): Long = instanceCounter++
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val rootNavigator = LocalRootNavigator.current ?: LocalNavigator.current
        val coroutineScope = rememberCoroutineScope()
        val density = LocalDensity.current
        // Responsive dismiss, identical thresholds to NowPlayingScreen:
        // ~100dp travel, or a >500px/s downward fling with a 24dp min-travel
        // guard against tap jitter.
        val dismissThresholdPx = with(density) { 100.dp.toPx() }
        val flingVelocityThresholdPxPerSec = 500f
        val minTravelForFlingPx = with(density) { 24.dp.toPx() }

        var dragOffsetY by remember { mutableStateOf(0f) }
        var isDismissed by remember { mutableStateOf(false) }
        var isDismissing by remember { mutableStateOf(false) }
        var isDragging by remember { mutableStateOf(false) }
        var screenHeightPx by remember { mutableStateOf(0f) }
        var settleJob by remember { mutableStateOf<Job?>(null) }
        var dismissJob by remember { mutableStateOf<Job?>(null) }
        var lastDragTime by remember { mutableStateOf(0L) }
        var dragVelocityY by remember { mutableStateOf(0f) }

        val dismissReader: () -> Unit = remember(rootNavigator, navigator) {
            {
                if (!isDismissed) {
                    isDismissed = true
                    (rootNavigator ?: navigator)?.pop()
                }
            }
        }
        // Every dismissal (swipe, handle, back button, the not-found pill) runs
        // through this one exit: the content animates itself off-screen and
        // only then pops. Popping first would drop the frame between this
        // screen and whatever is underneath before the motion has finished.
        val animateOutAndDismiss: () -> Unit = remember(rootNavigator, navigator) {
            {
                if (isDismissed || isDismissing) return@remember
                isDismissing = true
                settleJob?.cancel()
                // Tracked so a new drag during the exit tween can take the
                // gesture back instead of being popped out from under the user.
                dismissJob?.cancel()
                dismissJob = coroutineScope.launch {
                    val start = dragOffsetY
                    val target = if (screenHeightPx > 0f) screenHeightPx else start + 1200f
                    animate(
                        initialValue = start,
                        targetValue = target,
                        animationSpec = tween(260, easing = FastOutSlowInEasing)
                    ) { value, _ ->
                        dragOffsetY = value
                    }
                    dismissReader()
                }
            }
        }

        // System back routes through the same guarded exit as the gesture
        // (idempotent via isDismissed) instead of Voyager's raw pop, so a
        // settle tween racing a back-press cannot double-pop.
        PlayerBackHandler(enabled = true) { animateOutAndDismiss() }

        // ------------------------------------------------------------------
        // Resolve the entry.
        //
        // LaunchedEffect, not remember: the catalogue accessors are
        // synchronous today, but a remember would bake a repository call into
        // composition and silently break the moment one suspends. `itemId` is
        // fixed for the life of the instance, so this runs exactly once.
        // ------------------------------------------------------------------
        var phase by remember { mutableStateOf(ReaderPhase.Loading) }
        // The entry this screen was opened with. Only the engine can move the
        // reader off it; the local paging below also writes it, and only while
        // no devotional track is loaded.
        var pinnedItem by remember { mutableStateOf<DevotionalItem?>(null) }
        var collection by remember { mutableStateOf<List<DevotionalItem>>(emptyList()) }
        var queueTracks by remember { mutableStateOf<List<TrackItem>>(emptyList()) }

        LaunchedEffect(itemId) {
            phase = ReaderPhase.Loading
            val resolved = DevotionalRepository.byId(itemId)
            if (resolved == null) {
                // An id that is not in the catalogue (stale deep link, or a
                // build whose seed dropped it) is a real answer, not a crash
                // and not a blank page: Missing renders a quiet state with a
                // way back.
                pinnedItem = null
                phase = ReaderPhase.Missing
                return@LaunchedEffect
            }
            val kind = resolved.kind
            pinnedItem = resolved
            collection = DevotionalRepository.all(kind)
            // `playableTracks`, NEVER `audioUrlFor`: a multi-ayah entry's
            // `audioUrlFor` is one string of per-ayah urls joined with
            // `MULTI_AUDIO_SEPARATOR`, and `PlayerBridge.play` takes a single
            // url, so that joined form is a queue description, not something
            // that can be fetched. `playableTracks` expands such an entry into
            // one real track per ayah, which is what this screen enqueues.
            queueTracks = DevotionalRepository.playableTracks(kind)
            phase = ReaderPhase.Ready
        }

        // ------------------------------------------------------------------
        // Engine state. Every transport pixel below is read from here, never
        // from local state, so a track change made by the MiniPlayer, a
        // notification or the auto-advance at the end of an ayah is reflected
        // here without this screen doing anything.
        // ------------------------------------------------------------------
        val currentTrack by AudioEngine.currentTrack.collectAsState()
        val isPlaying by AudioEngine.isPlaying.collectAsState()
        val progress by AudioEngine.progress.collectAsState()
        val positionMs by AudioEngine.currentPositionMs.collectAsState()
        val durationMs by AudioEngine.durationMs.collectAsState()
        val queue by AudioEngine.queue.collectAsState()
        val currentIndex by AudioEngine.currentIndex.collectAsState()
        val speed by AudioEngine.playbackSpeed.collectAsState()
        val engineError by AudioEngine.errorMessage.collectAsState()

        // Single-flight for the first-open queueing below: one playQueue per
        // screen instance, ever.
        var hasQueued by remember { mutableStateOf(false) }

        // True when the queue we are reading is exactly the queue we built —
        // the precondition for turning a queue index back into an entry or an
        // ayah number. Declared before `engineItem`, which reads it.
        val queueIsOurs = remember(queue, queueTracks) {
            queue.isNotEmpty() &&
                queue.size == queueTracks.size &&
                queue.zip(queueTracks).all { it.first == it.second }
        }
        // The loaded track, resolved back to its entry. Scoped to THIS
        // screen's collection on purpose: a devotional playing in the
        // background from the other collection must not yank the reader off the
        // entry the user opened.
        //
        // Resolved ARITHMETICALLY (queue index → catalogue index) whenever the
        // queue is ours, never by title-and-text: three "Al-Ikhlas" duas share
        // both, so content matching can only ever find the first twin and the
        // reader would jump collections' worth of entries. Content matching is
        // only the fallback for a foreign queue the engine brought with it.
        val engineItem = remember(currentTrack, currentIndex, collection, queueIsOurs) {
            val track = currentTrack ?: return@remember null
            if (track.reciterSlug != DEVOTIONAL_TRACK_SLUG) return@remember null
            if (queueIsOurs) {
                val kind = collection.firstOrNull()?.kind ?: return@remember null
                collection.getOrNull(
                    DevotionalRepository.collectionIndexForQueueIndex(kind, currentIndex)
                )
            } else {
                collection.firstOrNull { track.belongsTo(it) }
            }
        }
        // Before this screen has queued anything, the reader shows the entry it
        // was OPENED with — otherwise opening entry B while entry A is playing
        // would flash A's text for a frame before the queue lands on B. From
        // the first queueing onward the engine is authoritative, which is what
        // keeps the text correct when the MiniPlayer, a notification or the
        // end-of-ayah auto-advance moves the track.
        val readerItem = if (hasQueued) (engineItem ?: pinnedItem) else pinnedItem
        val readerIndex = remember(readerItem, collection) {
            readerItem?.let { item ->
                collection.indexOfFirst { it.id == item.id && it.title == item.title }
            } ?: -1
        }
        // Which ayah of a multi-ayah passage is being recited right now.
        val liveAyah = remember(readerItem, queue, currentIndex, queueIsOurs) {
            val item = readerItem ?: return@remember null
            if (!item.isQuranic || !queueIsOurs) return@remember null
            val track = queue.getOrNull(currentIndex) ?: return@remember null
            if (!track.belongsTo(item)) return@remember null
            val offset = currentIndex - DevotionalRepository.queueRange(item.kind, item).first
            if (offset < 0) null else item.startAyah + offset
        }

        // Audio availability comes from the resolved queue, not `hasAudio`
        // (see the class doc). An entry with no resolvable audio gets no play
        // control at all — and this is false for it.
        //
        // `queueRange` is an INDEX range, not the tracks themselves, so the
        // url has to be read back out of the queue: iterating the range alone
        // would hand `any` an Int and ask it for an `audioUrl`. An entry that is
        // not in the queue yields an empty range, so this is false.
        val itemHasAudio = remember(readerItem, queueTracks) {
            val item = readerItem ?: return@remember false
            val range = DevotionalRepository.queueRange(item.kind, item)
            range.any { queueTracks.getOrNull(it)?.audioUrl?.isNotBlank() == true }
        }

        // First open: queue the collection unless the engine is already on this
        // exact entry. Single-flight per instance via `hasQueued`, so a
        // recomposition (or a rotation) can never restart the passage.
        //
        // This effect is also the ONLY thing that starts devotional playback,
        // and it is unconditional — that is why the list screen has a single tap
        // target and no "start playing" intent to pass: there is no open-this-
        // entry-but-do-not-play mode, so an intent flag would have nothing to
        // switch on. An entry with audio starts at its own first ayah.
        LaunchedEffect(phase, readerItem, queueTracks) {
            if (hasQueued || phase != ReaderPhase.Ready) return@LaunchedEffect
            val item = readerItem
            if (item == null) return@LaunchedEffect
            if (!itemHasAudio) {
                // No resolvable audio: never touch the engine. playQueue on a
                // blank url would only produce a load error.
                hasQueued = true
                return@LaunchedEffect
            }
            val tracks = queueTracks
            if (tracks.isEmpty()) {
                hasQueued = true
                return@LaunchedEffect
            }
            val loaded = engineItem
            if (loaded != null && loaded.id == item.id && loaded.title == item.title) {
                // Already playing this entry (e.g. the MiniPlayer reopened this
                // screen): leave the session alone.
                hasQueued = true
                return@LaunchedEffect
            }
            AudioEngine.playQueue(
                tracks = tracks,
                startIndex = DevotionalRepository.queueRange(item.kind, item).first.coerceAtLeast(0),
                startPositionMs = 0L
            )
            hasQueued = true
        }

        // The transport is only built when a devotional track is actually
        // loaded, so there is never a lit play control that cannot play.
        val engineOwnsReader = engineItem != null
        val canPrev = remember(queue, currentIndex, positionMs, engineOwnsReader, readerIndex) {
            if (engineOwnsReader) AudioEngine.canSkipPrevious() else readerIndex > 0
        }
        val canNext = remember(queue, currentIndex, engineOwnsReader, readerIndex) {
            if (engineOwnsReader) AudioEngine.canSkipNext() else readerIndex in 0 until collection.lastIndex
        }
        val stepBy: (Int) -> Unit = { delta ->
            if (engineOwnsReader) {
                if (delta < 0) AudioEngine.skipPrevious() else AudioEngine.skipNext()
            } else {
                collection.getOrNull(readerIndex + delta)?.let { pinnedItem = it }
            }
        }

        NoirScreenRoot(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { screenHeightPx = it.height.toFloat() }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        lastDragTime = 0L
                        dragVelocityY = 0f
                        val slopChange = awaitVerticalTouchSlopOrCancellation(
                            pointerId = down.id
                        ) { change, _ ->
                            change.consume()
                        }
                        if (slopChange == null) return@awaitEachGesture
                        // Vertical drag won — claim it. Re-arm the release
                        // decision: a cancelled dismiss tween must never leave
                        // isDismissing latched with a stranded offset.
                        settleJob?.cancel()
                        dismissJob?.cancel()
                        isDismissing = false
                        isDragging = true
                        var pointerId = slopChange.id
                        var lastY = slopChange.position.y
                        val maxOffsetY =
                            if (screenHeightPx > 0f) screenHeightPx else Float.MAX_VALUE
                        dragOffsetY = (dragOffsetY + (slopChange.position.y - down.position.y))
                            .coerceIn(0f, maxOffsetY)
                        lastDragTime = slopChange.uptimeMillis
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (change.changedToUp()) {
                                change.consume()
                                break
                            }
                            // If a child consumed (the passage's own vertical
                            // scroll), yield: scrolling the text must never
                            // also drag the screen away.
                            if (change.isConsumed) break
                            val dy = change.position.y - lastY
                            lastY = change.position.y
                            val now = change.uptimeMillis
                            if (lastDragTime > 0L && dy != 0f) {
                                val dt = (now - lastDragTime).coerceAtLeast(1L)
                                val instantVelocity = (dy / dt.toFloat()) * 1000f
                                dragVelocityY = 0.7f * dragVelocityY + 0.3f * instantVelocity
                            }
                            lastDragTime = now
                            // Downward only — upward rubber-bands back to 0.
                            if (dy != 0f) {
                                dragOffsetY = (dragOffsetY + dy).coerceIn(0f, maxOffsetY)
                                change.consume()
                            }
                        }
                        isDragging = false
                        val shouldDismiss = dragOffsetY >= dismissThresholdPx ||
                            (dragVelocityY > flingVelocityThresholdPxPerSec &&
                                dragOffsetY > minTravelForFlingPx)
                        if (shouldDismiss && !isDismissing) {
                            animateOutAndDismiss()
                        } else if (!isDismissing) {
                            settleJob = coroutineScope.launch {
                                animate(
                                    initialValue = dragOffsetY,
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ) { value, _ ->
                                    dragOffsetY = value
                                }
                            }
                        }
                        dragVelocityY = 0f
                        lastDragTime = 0L
                    }
                }
        ) {
            // The canvas, glow and grain above stay full-screen and static;
            // only the content shrinks. Transforming the whole NoirScreenRoot
            // instead left a black rectangle around the receding reader.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .graphicsLayer {
                        val travel = if (screenHeightPx > 0f) screenHeightPx * 0.4f else 1f
                        val fraction = (dragOffsetY / travel).coerceIn(0f, 1f)
                        transformOrigin = TransformOrigin(0.5f, 1f)
                        val shrink = 1f - 0.16f * fraction
                        scaleX = shrink
                        scaleY = shrink
                        translationY = dragOffsetY * 0.5f
                        alpha = 1f - 0.55f * fraction
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(14.dp))

                    // Dismiss affordance: NowPlayingScreen's exact shape — a
                    // 64x36 clickable target wrapped around a 48x5 grey bar, so
                    // the tap target is real and the bar stays quiet. Grows and
                    // brightens while dragging.
                    val handleWidth by animateDpAsState(
                        targetValue = if (isDragging) 72.dp else 48.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "handleWidth"
                    )
                    val handleHeight by animateDpAsState(
                        targetValue = if (isDragging) 6.dp else 5.dp,
                        label = "handleHeight"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 64.dp, height = 36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { animateOutAndDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(handleWidth)
                                    .height(handleHeight)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (isDragging) GhaisNoir.TextPrimary
                                        else GhaisNoir.TextDisabled
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    when (phase) {
                        ReaderPhase.Loading -> QuietCenter(
                            headline = "Loading…",
                            hint = "Fetching this entry."
                        )

                        ReaderPhase.Missing -> QuietCenter(
                            headline = "Not available",
                            hint = "This entry is no longer in the collection.",
                            onBack = animateOutAndDismiss
                        )

                        ReaderPhase.Ready -> {
                            val item = readerItem
                            if (item == null) {
                                // Ready with nothing to read can only happen if
                                // the catalogue answered with an entry and then
                                // lost it. Say so rather than render an empty
                                // page.
                                QuietCenter(
                                    headline = "Not available",
                                    hint = "This entry is no longer in the collection.",
                                    onBack = animateOutAndDismiss
                                )
                            } else {
                                ReaderBody(
                                    item = item,
                                    readerIndex = readerIndex,
                                    collectionSize = collection.size,
                                    liveAyah = liveAyah
                                ) {
                                    when {
                                        // No audio for this entry: the reader only.
                                        !itemHasAudio -> ReadOnlyBody(
                                            kind = item.kind,
                                            canPrev = canPrev,
                                            canNext = canNext,
                                            onStep = stepBy
                                        )
                                        // Audio exists but the engine has not been
                                        // handed it yet (first frame). Say
                                        // "preparing" rather than the collection's
                                        // "no audio" line, which would be a lie
                                        // about this entry.
                                        !engineOwnsReader -> PreparingAudio()
                                        else -> ListeningBody(
                                            track = currentTrack,
                                            isPlaying = isPlaying,
                                            progress = progress,
                                            positionMs = positionMs,
                                            totalMs = resolveTotalMs(durationMs, item),
                                            speed = speed,
                                            errorMessage = engineError,
                                            canPrev = canPrev,
                                            canNext = canNext,
                                            onStep = stepBy,
                                            onReplay = { replayFromStart(item, queueTracks) }
                                        )
                                    }

                                    // The overlay MiniPlayer draws on top of this
                                    // screen (see MINI_PLAYER_CLEARANCE).
                                    Spacer(modifier = Modifier.height(MINI_PLAYER_CLEARANCE))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Identity + the passage itself + the trailing transport slot.
 *
 * A [ColumnScope] extension on purpose: the scrollable passage carries
 * `weight(1f)`, and a `weight` resolved inside a nested wrap-content Column is
 * the classic way to get a zero-height scroller. Extending the screen's own
 * Column scope keeps the weight meaningful, so the passage takes exactly the
 * space left between the title and the transport.
 *
 * The passage is the only vertical scroller here, and the drag-to-minimise
 * gesture yields to it (see the `change.isConsumed` break in the gesture loop),
 * so a long supplication scrolls instead of dismissing.
 */
@Composable
private fun ColumnScope.ReaderBody(
    item: DevotionalItem,
    readerIndex: Int,
    collectionSize: Int,
    liveAyah: Int?,
    transport: @Composable () -> Unit
) {
    // Collection + position. Latin caps with positive tracking: chrome
    // digits, deliberately not Arabic-Indic (those belong to the rosette).
    val positionLabel = if (readerIndex >= 0 && collectionSize > 0) {
        "${item.kind.label.uppercase()} · ${readerIndex + 1} OF $collectionSize"
    } else {
        item.kind.label.uppercase()
    }
    Text(
        text = positionLabel,
        color = GhaisNoir.TextTertiary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
    Spacer(modifier = Modifier.height(6.dp))
    // Titles are Latin in both catalogues, so the ambient display tracking
    // is safe here — unlike the Arabic below it.
    Text(
        text = item.title.ifBlank { item.kind.label },
        color = GhaisNoir.TextPrimary,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
    Spacer(modifier = Modifier.height(14.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
    ) {
        DevotionPassage(item = item)

        // Live ayah of a multi-ayah passage, drawn as a standalone rosette
        // OUTSIDE the Uthmani paragraph: the digits go through
        // rosetteDigitFont, never through quranScript's `rlig` ayah ornament.
        // Only shown when the entry spans more than one ayah — a single-ayah
        // entry's number is already the inline rosette at the end of the
        // passage, and repeating it would be noise.
        if (liveAyah != null && item.ayahSpan > 1) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AyahEndMark(
                    number = liveAyah,
                    ringSize = 18.sp,
                    digitSize = 8.sp,
                    tint = GhaisNoir.TextSecondary
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = "Reciting now",
                    color = GhaisNoir.TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (item.translation.isNotBlank()) {
            Spacer(modifier = Modifier.height(20.dp))
            Hairline()
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = item.translation,
                color = GhaisNoir.TextSecondary,
                fontSize = 15.sp,
                lineHeight = 24.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        Hairline()
        Spacer(modifier = Modifier.height(12.dp))

        if (item.reference.isNotBlank()) {
            Text(
                text = "REFERENCE",
                color = GhaisNoir.TextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.reference,
                color = GhaisNoir.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
        if (item.category.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "CATEGORY",
                color = GhaisNoir.TextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.category,
                color = GhaisNoir.TextSecondary,
                fontSize = 13.sp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    Spacer(modifier = Modifier.height(16.dp))

    transport()
}

/**
 * The Arabic block, styled per kind.
 *
 * Ruqiyah is Uthmani Quranic text, so it renders in QCF Hafs
 * ([GhaisTypography.quranScript], tracking already zeroed) with the inline
 * end-of-ayah rosette. Dua text is scripture in the literary sense but is NOT
 * Uthmani — it is ordinary Naskh orthography, which QCF Hafs is not the right
 * face for — so it renders in [GhaisTypography.arabicBody] with an explicit
 * `letterSpacing = 0.sp`.
 *
 * Both live inside an RTL provider. `textAlign = Start` resolves to the RIGHT
 * edge inside an RTL provider, which is the first line of an Arabic paragraph.
 */
@Composable
private fun DevotionPassage(item: DevotionalItem) {
    val arabic = item.arabic.trim()
    if (arabic.isEmpty()) {
        Text(
            text = "Arabic text unavailable for this entry.",
            color = GhaisNoir.TextTertiary,
            fontSize = 14.sp
        )
        return
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (item.isQuranic) {
            Text(
                text = buildAnnotatedString {
                    append(arabic)
                    appendAyahMark(item.startAyah)
                },
                style = GhaisTypography.quranScript,
                fontSize = QURANIC_TEXT_SIZE,
                lineHeight = QURANIC_TEXT_SIZE * 2f,
                fontWeight = FontWeight.Normal,
                color = GhaisNoir.TextPrimary,
                textAlign = TextAlign.Start,
                inlineContent = ayahMarkContent(ringSize = 19.sp, digitSize = 9.sp),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text = arabic,
                style = GhaisTypography.arabicBody,
                fontSize = NASKH_TEXT_SIZE,
                lineHeight = NASKH_TEXT_SIZE * 2f,
                letterSpacing = 0.sp,
                fontWeight = FontWeight.Normal,
                color = GhaisNoir.TextPrimary,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Listening block: engine-fed now-playing line, progress meter, error, and the
 * shared [PlayerControls] transport.
 *
 * The meter is DISPLAY-ONLY by design. `NowPlayingScrubber`'s concept is reused
 * (engraved inset track, chrome fill, text-ladder timestamps) but its
 * hard-coded fake waveform bars are dropped, and so is the drag: the AudioEngine
 * surface available to this screen has no `seekTo`, so a scrub gesture could not
 * be honoured and offering one would be a lie. A track that cannot be scrubbed
 * is better shown as a meter than faked as a scrubber.
 */
@Composable
private fun ListeningBody(
    track: TrackItem?,
    isPlaying: Boolean,
    progress: Float,
    positionMs: Long,
    totalMs: Long,
    speed: Float,
    errorMessage: String?,
    canPrev: Boolean,
    canNext: Boolean,
    onStep: (Int) -> Unit,
    onReplay: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            // The engine's own idea of the title — the same string the
            // notification shows, so the two can never disagree on screen.
            Text(
                text = track?.let { AudioEngine.trackDisplayTitle(it) } ?: "—",
                color = GhaisNoir.TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(GhaisNoir.insetFill())
                .border(1.dp, GhaisNoir.InsetBorder, CircleShape)
        ) {
            val fraction = progress.coerceIn(0f, 1f)
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(GhaisNoir.chromeFill())
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = formatDevotionMs(positionMs),
                color = GhaisNoir.TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (totalMs > 0L) {
                    "-${formatDevotionMs((totalMs - positionMs).coerceAtLeast(0L))}"
                } else {
                    "--:--"
                },
                color = GhaisNoir.TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        if (errorMessage != null) {
            // The engine keeps the session alive on a source error (it does not
            // stop playback), so this screen must too: say what happened, offer
            // a retry that re-queues the same entry, and leave the reader up.
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(GhaisShapes.row)
                    .background(GhaisNoir.Fill2)
                    .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.row)
                    .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
            ) {
                Text(
                    text = errorMessage,
                    color = GhaisNoir.TextSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                GhostPillButton(text = "Retry", onClick = onReplay)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        PlayerControls(
            isPlaying = isPlaying,
            speed = speed,
            canSkipPrevious = canPrev,
            canSkipNext = canNext,
            isSleepTimerActive = false,
            // The engine's repeat mode is not on this screen's AudioEngine
            // surface (no `playbackState`, no `setRepeatMode`), so the well is
            // never shown as engaged — a lit repeat the screen cannot honour is
            // worse than an unlit one. Tapping it replays the entry from the
            // start, which is the one repeat action available here. The sleep
            // well has no available action at all and stays inert.
            repeatMode = RepeatMode.OFF,
            onRepeatClick = onReplay,
            onTogglePlayPause = { AudioEngine.togglePlayPause() },
            onPrevious = { onStep(-1) },
            onNext = { onStep(1) },
            onSpeedChange = { current ->
                val idx = SpeedsList.indexOf(current).takeIf { it >= 0 } ?: 0
                AudioEngine.setSpeed(SpeedsList[(idx + 1) % SpeedsList.size])
            },
            onSleepTimerClick = {}
        )
    }
}

/**
 * Read-only block for an entry with no resolvable audio.
 *
 * Every catalogue entry ships audio today, so this is strictly a fallback
 * (a corrupt seed row, a future entry added without a clip). No transport is built at all — not a disabled one. A dimmed play disc still
 * reads as "broken play" and invites a tap that cannot do anything. What is
 * offered instead is the one thing the entry CAN do: read it, and page through
 * the collection.
 */
@Composable
private fun ReadOnlyBody(
    kind: DevotionKind,
    canPrev: Boolean,
    canNext: Boolean,
    onStep: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No audio for this ${kind.unitWord.dropLast(1)} yet — read along.",
            color = GhaisNoir.TextTertiary,
            fontSize = 12.5.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PagingWell(
                icon = Icons.Filled.FastRewind,
                contentDescription = "Previous",
                enabled = canPrev,
                onClick = { onStep(-1) }
            )
            Spacer(modifier = Modifier.width(24.dp))
            PagingWell(
                icon = Icons.Filled.FastForward,
                contentDescription = "Next",
                enabled = canNext,
                onClick = { onStep(1) }
            )
        }
    }
}

/**
 * The one frame between "this entry has audio" and "the engine has it".
 *
 * Deliberately NOT the read-only line: claiming there is no audio for a
 * passage that has one is the exact lie this screen exists to avoid. A quiet
 * "preparing" line, no transport, no pager.
 */
@Composable
private fun PreparingAudio() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Preparing audio…",
            color = GhaisNoir.TextDisabled,
            fontSize = 12.5.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/** Clay well used by the read-only pager; [IconWell] is presentational only. */
@Composable
private fun PagingWell(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.noirClickable { if (enabled) onClick() },
        contentAlignment = Alignment.Center
    ) {
        IconWell(
            icon = icon,
            size = 52.dp,
            iconSize = 24.dp,
            contentDescription = if (enabled) contentDescription else null,
            tint = if (enabled) GhaisNoir.TextPrimary else GhaisNoir.TextDisabled
        )
    }
}

/** Loading / missing states: quiet, centred, always with a way out. */
@Composable
private fun QuietCenter(
    headline: String,
    hint: String,
    onBack: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        IconWell(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            size = 56.dp,
            iconSize = 26.dp,
            contentDescription = null,
            tint = GhaisNoir.TextTertiary
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = headline,
            color = GhaisNoir.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = hint,
            color = GhaisNoir.TextSecondary,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center
        )
        if (onBack != null) {
            Spacer(modifier = Modifier.height(18.dp))
            GhostPillButton(text = "Go back", onClick = onBack)
        }
        Spacer(modifier = Modifier.height(MINI_PLAYER_CLEARANCE))
    }
}

/** 1.dp ghost separator, the list/lyrics-card separator language. */
@Composable
private fun Hairline() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(GhaisNoir.BorderGhost)
    )
}

/**
 * Re-queue one entry from its own first track — the retry / replay action.
 *
 * The arithmetic range is only valid against the repository-built queue, so a
 * foreign list falls back to the first content-matching row instead of
 * queueing from a wrong index.
 */
private fun replayFromStart(item: DevotionalItem, tracks: List<TrackItem>) {
    if (tracks.isEmpty()) return
    val start = if (tracks == DevotionalRepository.playableTracks(item.kind)) {
        DevotionalRepository.queueRange(item.kind, item).first
    } else {
        tracks.indexOfFirst { it.belongsTo(item) }
    }.coerceAtLeast(0)
    AudioEngine.playQueue(tracks = tracks, startIndex = start, startPositionMs = 0L)
}

/**
 * Total length for the timestamps: the engine's resolved duration, else the
 * duration the catalogue shipped with. Zero means "unknown" and the remaining
 * label says "--:--" rather than inventing a total.
 */
private fun resolveTotalMs(engineDurationMs: Long, item: DevotionalItem): Long =
    if (engineDurationMs > 0L) engineDurationMs else item.durationMs.coerceAtLeast(0L)

private fun formatDevotionMs(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000).toInt()
    val minutes = (totalSeconds / 60).toString().padStart(2, '0')
    val seconds = (totalSeconds % 60).toString().padStart(2, '0')
    return "$minutes:$seconds"
}

/**
 * Does this queued track belong to [item]?
 *
 * `DevotionalItem.asTrackItem()` is the only thing that adapts a devotional
 * into a queue row, and it carries the identity it has: the devotional slug in
 * `reciterSlug`, the title in `surahNameEn` and the passage in `textUthmani`.
 * Matching on those three is what maps a track back to a readable entry, and it
 * survives the multi-ayah expansion in `DevotionalRepository.playableTracks`
 * (where several consecutive tracks share one entry's title and text).
 */
private fun TrackItem.belongsTo(item: DevotionalItem): Boolean =
    reciterSlug == DEVOTIONAL_TRACK_SLUG &&
        surahNameEn == item.title.ifBlank { item.kind.label } &&
        textUthmani == item.arabic

/**
 * Plural unit for the read-only line: "supplication" / "verse".
 *
 * Deliberately a local copy rather than a reuse of the list screen's private
 * extension — one file, no cross-screen coupling, and this file may not touch
 * any existing file.
 */
private val DevotionKind.unitWord: String
    get() = when (this) {
        DevotionKind.DUA -> "supplications"
        DevotionKind.RUQIYAH -> "verses"
    }
