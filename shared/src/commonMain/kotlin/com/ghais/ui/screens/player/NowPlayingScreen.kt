package com.ghais.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import com.ghais.ui.navigation.LocalRootNavigator
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import coil3.compose.AsyncImage
import com.ghais.data.repository.FavoritesStore
import com.ghais.data.repository.QuranAyahRepository
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.seed.GhaisAssets
import com.ghais.domain.model.RepeatMode
import com.ghais.player.AmbientMixer
import com.ghais.player.AudioEngine
import com.ghais.player.PlayerBackHandler
import com.ghais.player.displayName
import com.ghais.player.videoKeys
import com.ghais.ui.components.noir.ChromeFab
import com.ghais.ui.components.noir.NoirHeroCard
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.screens.home.NoirStatChip
import com.ghais.ui.screens.player.components.NowPlayingLyricsCard
import com.ghais.ui.screens.player.components.NowPlayingVolumePanel
import com.ghais.ui.screens.reciters.NoirReciterAvatar
import com.ghais.ui.screens.reciters.photoForSlug
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * Now Playing — strict Noir Glass monochrome.
 *
 * Presentation only. Screen signature, drag-to-dismiss, AudioEngine / queue /
 * repeat / sleep / favorite wiring, sheet wiring and navigation are untouched.
 * Zero hue: canvas #050506 + glow + grain, grayscale artwork with scrim and
 * chromium ring, chrome/ghost controls, NoirSegmentedProgress meter language,
 * text ladder 100 / 62 / 38 / 24%.
 */
class NowPlayingScreen : Screen {
    // Unique key per pushed instance: Voyager's default Screen.key is the
    // class name, so two stacked players alias one SaveableState slot and the
    // revealed duplicate renders blank. A stacked push is still possible via
    // rapid taps, but each instance now owns isolated state.
    private val instanceTag = nextInstanceTag()

    override val key: ScreenKey
        get() = "NowPlayingScreen#$instanceTag"

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
        // Responsive dismiss: ~100dp travel, or a >500px/s downward fling
        // (with a small 24dp min-travel guard against tap jitter).
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

        val dismissPlayer: () -> Unit = remember(rootNavigator, navigator) {
            {
                if (!isDismissed) {
                    isDismissed = true
                    (rootNavigator ?: navigator)?.pop()
                }
            }
        }
        // Every dismissal (swipe, back button, close button) runs through this
        // one exit: the content animates itself off-screen and only then pops.
        // The stack transition for NowPlaying is a no-op (see App.kt), so this
        // is the only motion — previously the drag tween and the stack exit
        // both animated the same content and reset the offset mid-flight,
        // which is what exposed the empty frame between player and mini bar.
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
                    dismissPlayer()
                }
            }
        }
        val currentTrack by AudioEngine.currentTrack.collectAsState()
        val isPlaying by AudioEngine.isPlaying.collectAsState()
        val isAyahMode by AudioEngine.isAyahMode.collectAsState()
        val queue by AudioEngine.queue.collectAsState()
        val currentIndex by AudioEngine.currentIndex.collectAsState()
        // Verse text resolves through the repository cache: placeholders swap
        // to real text when the fetch lands (cacheGen), not on track change.
        val cacheGen by QuranAyahRepository.cacheGen.collectAsState()
        val currentVerse = remember(currentTrack, cacheGen) { currentTrack?.let { QuranAyahRepository.getAyahImmediate(it.surahId, it.ayahNo.coerceAtLeast(1)) } }
        val upcomingVerse = remember(currentTrack, queue, currentIndex, cacheGen) {
            val track = currentTrack ?: return@remember null
            val totalAyahs = QuranDataRepository.getSurahById(track.surahId)?.ayahsCount ?: 7
            val currentAyah = track.ayahNo.coerceAtLeast(1)
            if (currentAyah < totalAyahs) {
                QuranAyahRepository.getAyahImmediate(track.surahId, currentAyah + 1)
            } else {
                // On last Ayah of Surah: determine next Surah from the Surah queue
                val idx = if (currentIndex in queue.indices && queue[currentIndex].surahId == track.surahId) {
                    currentIndex
                } else {
                    queue.indexOfFirst { it.surahId == track.surahId }
                }
                val nextSurah = if (idx != -1 && idx + 1 < queue.size) queue[idx + 1] else null
                if (nextSurah != null) {
                    QuranAyahRepository.getAyahImmediate(nextSurah.surahId, 1)
                } else {
                    null
                }
            }
        }
        val progress by AudioEngine.progress.collectAsState()
        val currentPositionMs by AudioEngine.currentPositionMs.collectAsState()
        val durationMs by AudioEngine.durationMs.collectAsState()
        val speed by AudioEngine.playbackSpeed.collectAsState()
        val volume by AudioEngine.volume.collectAsState()
        val playbackState by AudioEngine.playbackState.collectAsState()
        val sleepTimerState by com.ghais.player.SleepTimer.state.collectAsState()
        val favorites by FavoritesStore.favoriteTracks.collectAsState()
        val repeatMode = playbackState.settings.repeatMode

        var showSleepTimer by remember { mutableStateOf(false) }
        var showQueue by remember { mutableStateOf(false) }
        var showAmbient by remember { mutableStateOf(false) }
        var showVolume by remember { mutableStateOf(false) }
        var showTafseer by remember { mutableStateOf(false) }

        // System-back routes through the guarded dismissPlayer() (idempotent
        // via isDismissed) instead of Voyager's raw pop — prevents a
        // double-pop when the settle tween and a back-press race. Open sheets
        // consume the press first.
        PlayerBackHandler(enabled = true) {
            if (showSleepTimer || showQueue || showAmbient || showVolume || showTafseer) {
                showSleepTimer = false
                showQueue = false
                showAmbient = false
                showVolume = false
                showTafseer = false
            } else {
                animateOutAndDismiss()
            }
        }

        // Cinematic idle fade — mirrors Media3's controllerShowTimeoutMs:
        // 10s without interaction melts all chrome except video + transport.
        // Any tap/drag/scrub pokes the timer; sheets, scrubbing and the
        // volume panel suspend hiding while the user is active.
        var controlsVisible by remember { mutableStateOf(true) }
        var idleTick by remember { mutableStateOf(0) }
        var isScrubbing by remember { mutableStateOf(false) }
        var scrubFraction by remember { mutableStateOf(0f) }
        val poke: () -> Unit = { idleTick++; controlsVisible = true }
        val uiBusy = showSleepTimer || showQueue || showAmbient || showVolume || showTafseer || isScrubbing || isAyahMode
        androidx.compose.runtime.LaunchedEffect(controlsVisible, idleTick, uiBusy) {
            if (controlsVisible && !uiBusy) {
                kotlinx.coroutines.delay(10_000L)
                controlsVisible = false
            }
        }
        val mixer = remember { AmbientMixer }
        val ambientChannels by mixer.channels.collectAsState()
        val ambientVolume by mixer.masterAmbientVolume.collectAsState()
        val selectedAmbientType = ambientChannels.firstOrNull { it.isEnabled }?.type
        val hasAmbientVideo = selectedAmbientType?.videoKeys()?.isNotEmpty() == true

        val track = currentTrack
        val title = track?.surahNameEn ?: "Ar-Rahman"
        val surahNameAr = track?.surahNameAr ?: "الرحمن"
        val reciterName = track?.reciterName ?: "Mishary Rashid Alafasy"
        val isFav = track?.let { t -> favorites.any { it.audioUrl == t.audioUrl } } == true

        val canSkipNext = remember(queue, currentIndex, repeatMode, track, isAyahMode) {
            if (track == null || queue.isEmpty()) false
            // Ayah mode steps verse-by-verse: enabled while a next ayah (or a
            // next surah to cross into) exists — the surah queue alone can't tell.
            else if (isAyahMode) {
                val totalAyahs = QuranDataRepository.getSurahById(track.surahId)?.ayahsCount ?: 0
                val ayahNo = track.ayahNo.coerceAtLeast(1)
                repeatMode != com.ghais.domain.model.RepeatMode.OFF ||
                    ayahNo < totalAyahs || currentIndex < queue.size - 1
            }
            else if (repeatMode != com.ghais.domain.model.RepeatMode.OFF) true
            else currentIndex < queue.size - 1
        }

        val canSkipPrevious = remember(queue, currentIndex, repeatMode, track, currentPositionMs, isAyahMode) {
            if (track == null) false
            // Ayah mode: prev restarts the verse (or steps back), so it is
            // always meaningful while a track is loaded.
            else if (isAyahMode) true
            else if (currentPositionMs > 3_000L) true
            else if (queue.isEmpty()) false
            else if (repeatMode != com.ghais.domain.model.RepeatMode.OFF) true
            else currentIndex > 0
        }

        val totalMs = if (durationMs > 0L) durationMs
            else (track?.durationMs?.takeIf { it > 0L } ?: 0L)
        // Time labels derive from the scrub-aware active position below.

        NoirScreenRoot(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { screenHeightPx = it.height.toFloat() }
                .pointerInput(controlsVisible, uiBusy) {
                    detectTapGestures(
                        onTap = { poke() },
                        onDoubleTap = { if (controlsVisible && !uiBusy) controlsVisible = false }
                    )
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        // requireUnconsumed=false so the drag can start anywhere —
                        // over video, cards, buttons — before children consume.
                        val down = awaitFirstDown(requireUnconsumed = false)
                        lastDragTime = 0L
                        dragVelocityY = 0f
                        // Wait for vertical touch-slop only: horizontal scrubs
                        // (progress bar) stay unconsumed so the child keeps them,
                        // vertical swipes are claimed here first.
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
                        poke()
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
                            // If a child consumed (e.g. nested scroll), yield.
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
                            // Upper-clamped so a long pull can never park the
                            // content fully off-screen past the dismiss target.
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // The canvas, glow and grain above stay full-screen and
                        // static; only the content shrinks. Transforming the
                        // whole NoirScreenRoot instead left a visible black
                        // rectangle around the receding player — the backdrop
                        // the gesture was supposed to hide.
                        //
                        // Responsive mapping: the gesture completes over ~40% of
                        // the screen height (was a full-height pull) and the
                        // content tracks the finger at half rate, so a short
                        // swipe reads as an immediate response instead of lag.
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
            // Ambient video is the hero — only a light veil + bottom grade
            // for legibility, so the video stays the focus of the player.
            AmbientVideoView(selectedType = selectedAmbientType, modifier = Modifier.fillMaxSize())
            if (hasAmbientVideo) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GhaisNoir.NoirBlack.copy(alpha = 0.22f))
                )
            }
            // Bottom legibility grade — monochrome melt into the canvas.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                GhaisNoir.NoirBlack.copy(alpha = 0.80f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(14.dp))

                // Top pill bar handle — melts away with the idle fade.
                // Grows + highlights while dragging for tactile feedback.
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { -it },
                    exit = fadeOut(tween(350)) + slideOutVertically(tween(350)) { -it }
                ) {
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
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Video-first: clear the middle so the ambient video breathes.
                Spacer(modifier = Modifier.weight(1f))

                // Synchronized Ayah Lyrics card — animated when Ayah mode is active.
                AnimatedVisibility(
                    visible = isAyahMode,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    NowPlayingLyricsCard(
                        currentTrack = currentTrack,
                        currentAyahVerse = currentVerse,
                        upcomingAyahVerse = upcomingVerse,
                        isPlaying = isPlaying,
                        onNextAyah = { poke(); AudioEngine.nextAyah() },
                        onPreviousAyah = { poke(); AudioEngine.previousAyah() },
                        onToggleAyahMode = { poke(); AudioEngine.toggleAyahMode() },
                        onOpenTafseer = { poke(); showTafseer = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                }

                // Identity + ambience melt away with the idle fade.
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 2 },
                    exit = fadeOut(tween(350)) + slideOutVertically(tween(350)) { it / 2 }
                ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Minimal identity: qari photo + titles + like, floating over video.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                    NoirReciterAvatar(
                        photoUrl = track?.let { photoForSlug(it.reciterSlug) },
                        nameEn = reciterName,
                        size = 46.dp,
                        shape = CircleShape,
                         monogramSize = 20.sp,
                         ring = isPlaying,
                         slug = track?.reciterSlug,
                         grayscale = false,
                         scrimAlpha = 0f
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                color = GhaisNoir.TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (surahNameAr.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = surahNameAr,
                                    color = GhaisNoir.TextSecondary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                        Text(
                            text = reciterName,
                            color = GhaisNoir.TextTertiary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isFav) GhaisNoir.Fill4 else GhaisNoir.Fill1)
                            .border(
                                1.dp,
                                if (isFav) GhaisNoir.SpecularTop else GhaisNoir.BorderGhost,
                                CircleShape
                            )
                                    .noirClickable { poke(); track?.let { FavoritesStore.toggle(it) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Like",
                            tint = if (isFav) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Ambience as explicit text — a new concept, so it reads as one
                // slim labeled line users can find and understand at a glance.
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.Fill2)
                        .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
                        .noirClickable { poke(); showAmbient = true }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Ambience",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = selectedAmbientType?.displayName() ?: "Off",
                        color = GhaisNoir.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "›",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Core transport never fades: prev / play / next only.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NoirTransportWell(
                        icon = Icons.Filled.FastRewind,
                        contentDescription = if (isAyahMode) "Previous Ayah" else "Previous",
                        enabled = canSkipPrevious,
                        onClick = { poke(); AudioEngine.previous() },
                        iconSize = 26.dp
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    ChromeFab(
                        icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        onClick = { poke(); AudioEngine.togglePlayPause() },
                        size = 68.dp,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    NoirTransportWell(
                        icon = Icons.Filled.FastForward,
                        contentDescription = if (isAyahMode) "Next Ayah" else "Next",
                        enabled = canSkipNext,
                        onClick = { poke(); AudioEngine.next() },
                        iconSize = 26.dp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress, times + overflow melt away with the idle fade.
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 2 },
                    exit = fadeOut(tween(350)) + slideOutVertically(tween(350)) { it / 2 }
                ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                // Progress scrubber (tap & horizontal scrub with live preview) —
                // NoirSegmentedProgress language: engraved track + chrome fill.
                // isScrubbing/scrubFraction live at screen level so scrubbing
                // suspends the idle fade (see uiBusy above).
                val activeProgress = if (isScrubbing) scrubFraction else progress.coerceIn(0f, 1f)
                val activePositionMs = if (isScrubbing && totalMs > 0L) {
                    (scrubFraction * totalMs).toLong()
                } else {
                    currentPositionMs
                }
                val activeElapsedText = formatMs(activePositionMs)
                val activeRemainingText = if (totalMs > 0L) {
                    "-${formatMs((totalMs - activePositionMs).coerceAtLeast(0L))}"
                } else {
                    "--:--"
                }

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val width = maxWidth
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .pointerInput(totalMs) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val widthPx = size.width.toFloat()
                                    if (widthPx <= 0f) return@awaitEachGesture

                                    down.consume()
                                    scrubFraction = (down.position.x / widthPx).coerceIn(0f, 1f)
                                    isScrubbing = true

                                    val pointerId = down.id
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                        if (change.changedToUp()) {
                                            change.consume()
                                            if (totalMs > 0L) {
                                                AudioEngine.seekTo((scrubFraction * totalMs).toLong())
                                            }
                                            break
                                        }
                                        if (change.isConsumed) {
                                            break
                                        }
                                        change.consume()
                                        scrubFraction = (change.position.x / widthPx).coerceIn(0f, 1f)
                                    }
                                    isScrubbing = false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Engraved track (mirrors NoirSegmentedProgress).
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(GhaisNoir.insetFill())
                                .border(1.dp, GhaisNoir.InsetBorder, CircleShape)
                        ) {
                            // Chrome fill.
                            if (activeProgress > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(activeProgress)
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .background(GhaisNoir.chromeFill())
                                )
                            }
                        }

                        // Chrome thumb (visible during scrubbing/drag).
                        if (isScrubbing) {
                            val thumbOffset = ((width - 14.dp) * activeProgress).coerceAtLeast(0.dp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.CenterStart)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = thumbOffset)
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(GhaisNoir.chromeFill())
                                        .border(1.dp, GhaisNoir.NoirBlack, CircleShape)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = activeElapsedText,
                        color = GhaisNoir.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = activeRemainingText,
                        color = GhaisNoir.TextTertiary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Overflow: speed, volume (expandable), ayah mode, queue, sleep, repeat.
                // Melts away with the idle fade — core transport above never does.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        8.dp,
                        Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 48.dp, height = 44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GhaisNoir.Fill1)
                            .border(1.dp, GhaisNoir.BorderGhost, RoundedCornerShape(12.dp))
                            .noirClickable {
                                poke()
                                val idx = PlayerSpeeds.indexOf(speed).takeIf { it >= 0 } ?: 0
                                AudioEngine.setPlaybackSpeed(PlayerSpeeds[(idx + 1) % PlayerSpeeds.size])
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = formatSpeedLabel(speed),
                            color = GhaisNoir.TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    NoirUtilityWell(
                        icon = if (volume > 0f) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                        contentDescription = "Volume",
                        active = showVolume,
                        tint = if (volume > 0f) GhaisNoir.TextTertiary else GhaisNoir.TextDisabled,
                        onClick = { poke(); showVolume = !showVolume }
                    )
                    if (isAyahMode) {
                        NoirUtilityWell(
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = "Ayah Mode",
                            active = true,
                            tint = GhaisNoir.TextPrimary,
                            onClick = { poke(); AudioEngine.toggleAyahMode() }
                        )
                    }
                    NoirUtilityWell(
                        icon = Icons.Filled.QueueMusic,
                        contentDescription = "Queue",
                        active = false,
                        tint = GhaisNoir.TextTertiary,
                        onClick = { poke(); showQueue = true }
                    )
                    NoirUtilityWell(
                        icon = Icons.Filled.Bedtime,
                        contentDescription = "Sleep timer",
                        active = sleepTimerState.isActive,
                        tint = GhaisNoir.TextTertiary,
                        onClick = { poke(); showSleepTimer = true }
                    )
                    val repeatActive = repeatMode != RepeatMode.OFF
                    NoirUtilityWell(
                        icon = if (repeatMode == RepeatMode.SURAH) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = "Repeat",
                        active = repeatActive,
                        tint = if (repeatActive) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                        onClick = { poke(); AudioEngine.setRepeatMode(nextRepeatMode(repeatMode)) }
                    )
                }

                AnimatedVisibility(
                    visible = showVolume,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    NowPlayingVolumePanel(
                        quranVolume = volume,
                        onQuranVolumeChange = { mixer.setQuranVolume(it) },
                        ambientVolume = ambientVolume,
                        onAmbientVolumeChange = { mixer.setMasterAmbientVolume(it) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp)
                    )
                }
                }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            if (showSleepTimer) SleepTimerSheet(onDismiss = { showSleepTimer = false })
            if (showQueue) QueueSheet(onDismiss = { showQueue = false })
            if (showAmbient) AmbientMixerSheet(mixer = mixer, onDismissRequest = { showAmbient = false })
            if (showTafseer) TafseerSheet(onDismiss = { showTafseer = false })
        }
        }
    }
}

@Composable
private fun NoirTransportWell(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    iconSize: androidx.compose.ui.unit.Dp = 26.dp
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(56.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (enabled) GhaisNoir.Fill2 else GhaisNoir.Fill1)
                .border(1.dp, GhaisNoir.BorderCard, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) GhaisNoir.TextPrimary else GhaisNoir.TextDisabled,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
private fun NoirUtilityWell(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (active) GhaisNoir.Fill4 else GhaisNoir.Fill1)
                .border(
                    1.dp,
                    if (active) GhaisNoir.SpecularTop else GhaisNoir.BorderGhost,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private val PlayerSpeeds = listOf(1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 0.75f)

private fun formatMs(ms: Long): String {
    val s = (ms.coerceAtLeast(0L) / 1000).toInt()
    return "${(s / 60).toString().padStart(2, '0')}:${(s % 60).toString().padStart(2, '0')}"
}


