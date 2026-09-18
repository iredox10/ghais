package com.quranify.ui.screens.reciters

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.quranify.data.repository.QuranDataRepository
import com.quranify.data.seed.StitchAssets
import com.quranify.domain.model.Reciter
import com.quranify.domain.model.Surah
import com.quranify.domain.model.TrackItem
import com.quranify.player.AudioEngine
import com.quranify.ui.theme.QuranifyColors

/**
 * Screen displaying the profile of a verified reciter, their metadata badges,
 * action controls ("Play All", "Shuffle", "Follow"), and their full discography of Surahs.
 *
 * Fully styled in Black & White + Trending Purple theme (#A855F7).
 */
data class ReciterProfileScreen(val reciterSlug: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // 1 & 2: Look up reciter with fallback to Mishary
        val reciter: Reciter = remember(reciterSlug) {
            QuranDataRepository.getReciterBySlug(reciterSlug)
        }
        val surahs: List<Surah> = remember {
            QuranDataRepository.getSurahs()
        }

        val meta = remember(reciter) {
            resolveReciterMetadata(reciter)
        }

        // Playback state observation from AudioEngine
        val currentTrack by AudioEngine.currentTrack.collectAsState()
        val isPlaying by AudioEngine.isPlaying.collectAsState()

        // Follow state
        var isFollowing by remember { mutableStateOf(false) }

        Scaffold(
            containerColor = QuranifyColors.PitchBlack,
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(
                            onClick = { navigator.pop() },
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x22FFFFFF))
                                    .border(1.dp, Color(0x1AFFFFFF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 120.dp) // 120.dp padding for MiniPlayer & dock
            ) {
                // Header item
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        // Avatar container with Glowing Purple Ring & Verified Badge
                        ReciterAvatarHeader(
                            photoUrl = meta.photoUrl,
                            nameEn = reciter.nameEn
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Arabic Calligraphy Name in Bold White
                        Text(
                            text = reciter.nameAr,
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // English Name in High-Contrast Text
                        Text(
                            text = reciter.nameEn,
                            color = QuranifyColors.HighContrast,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Badges: Country, Riwayah, Style in Frosted Glass Pills with Purple Micro-Borders
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FrostedGlassBadge(text = meta.country)
                            FrostedGlassBadge(text = meta.riwayah)
                            FrostedGlassBadge(text = meta.style)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Follower Count Text
                        Text(
                            text = meta.followers,
                            color = Color(0xFF9CA3AF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        // Action Buttons Row: "Play All", "Shuffle", "Follow"
                        ReciterActionButtonsRow(
                            reciter = reciter,
                            surahs = surahs,
                            isFollowing = isFollowing,
                            onToggleFollow = { isFollowing = !isFollowing }
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Recitations Section Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recitations",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x1FA855F7),
                                border = BorderStroke(1.dp, Color(0x33A855F7))
                            ) {
                                Text(
                                    text = "${surahs.size} Surahs",
                                    color = Color(0xFFA855F7),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                // Discography / Surahs List
                items(
                    items = surahs,
                    key = { it.id }
                ) { surah ->
                    val isCurrentSurah = currentTrack?.surahId == surah.id && currentTrack?.reciterSlug == reciter.slug
                    val isCurrentSurahPlaying = isCurrentSurah && isPlaying

                    ReciterSurahListItem(
                        surah = surah,
                        reciter = reciter,
                        isCurrentTrack = isCurrentSurah,
                        isPlaying = isCurrentSurahPlaying,
                        onItemClick = {
                            val track = TrackItem(
                                reciterSlug = reciter.slug,
                                reciterName = reciter.nameEn,
                                surahId = surah.id,
                                surahNameEn = surah.nameEn,
                                surahNameAr = surah.nameAr,
                                ayahNo = 1,
                                audioUrl = reciter.getAyahAudioUrl(surah.id, 1),
                                durationMs = surah.ayahsCount * 15_000L
                            )
                            if (isCurrentSurahPlaying) {
                                AudioEngine.pause()
                            } else if (isCurrentSurah) {
                                AudioEngine.resume()
                            } else {
                                AudioEngine.playTrack(track)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Avatar with Glowing Purple Ring (#A855F7) and verified badge.
 */
@Composable
private fun ReciterAvatarHeader(
    photoUrl: String?,
    nameEn: String
) {
    Box(
        modifier = Modifier.size(118.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glowing purple ring & frosted halo
        Box(
            modifier = Modifier
                .size(118.dp)
                .clip(CircleShape)
                .background(Color(0x33A855F7))
                .border(
                    width = 2.5.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            Color(0xFF8B5CF6),
                            Color(0xFFA855F7),
                            Color(0xFFC084FC),
                            Color(0xFF8B5CF6)
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Inner Avatar photo or initials
        Box(
            modifier = Modifier
                .size(106.dp)
                .clip(CircleShape)
                .background(Color(0xFF161821)),
            contentAlignment = Alignment.Center
        ) {
            if (!photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = nameEn,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = nameEn.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Verified Badge at bottom end
        Box(
            modifier = Modifier
                .size(28.dp)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(Color(0xFFA855F7))
                .border(2.5.dp, QuranifyColors.PitchBlack, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Verified Reciter",
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

/**
 * Frosted glass badge pill with Trending Purple micro-border.
 */
@Composable
private fun FrostedGlassBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0x1FA855F7), // subtle purple glass tint
        border = BorderStroke(1.dp, Color(0x4DA855F7)) // purple micro-border
    ) {
        Text(
            text = text,
            color = Color(0xFFF3F4F6),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

/**
 * Action buttons: "Play All" (Trending Purple gradient), "Shuffle" (frosted dark glass),
 * and "Follow" (toggle state with purple outline).
 */
@Composable
private fun ReciterActionButtonsRow(
    reciter: Reciter,
    surahs: List<Surah>,
    isFollowing: Boolean,
    onToggleFollow: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "Play All" button with Trending Purple gradient
        Button(
            onClick = {
                val firstSurah = surahs.firstOrNull() ?: QuranDataRepository.getSurahs().first()
                val firstTrack = TrackItem(
                    reciterSlug = reciter.slug,
                    reciterName = reciter.nameEn,
                    surahId = firstSurah.id,
                    surahNameEn = firstSurah.nameEn,
                    surahNameAr = firstSurah.nameAr,
                    ayahNo = 1,
                    audioUrl = reciter.getAyahAudioUrl(firstSurah.id, 1),
                    durationMs = firstSurah.ayahsCount * 15_000L
                )
                AudioEngine.playTrack(firstTrack)
            },
            modifier = Modifier
                .weight(1.2f)
                .height(46.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(23.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF8B5CF6), Color(0xFFA855F7))
                        ),
                        shape = RoundedCornerShape(23.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play All",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Play All",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // "Shuffle" button in frosted dark glass
        Surface(
            onClick = {
                val randomSurah = surahs.randomOrNull() ?: QuranDataRepository.getSurahs().first()
                val randomTrack = TrackItem(
                    reciterSlug = reciter.slug,
                    reciterName = reciter.nameEn,
                    surahId = randomSurah.id,
                    surahNameEn = randomSurah.nameEn,
                    surahNameAr = randomSurah.nameAr,
                    ayahNo = 1,
                    audioUrl = reciter.getAyahAudioUrl(randomSurah.id, 1),
                    durationMs = randomSurah.ayahsCount * 15_000L
                )
                AudioEngine.playTrack(randomTrack)
            },
            modifier = Modifier
                .weight(1f)
                .height(46.dp),
            shape = RoundedCornerShape(23.dp),
            color = Color(0xFF161822),
            border = BorderStroke(1.dp, Color(0x33A855F7))
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Shuffle",
                    color = Color(0xFFE1E3E4),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        // "Follow" toggle button with purple outline
        Surface(
            onClick = onToggleFollow,
            modifier = Modifier
                .weight(1f)
                .height(46.dp),
            shape = RoundedCornerShape(23.dp),
            color = if (isFollowing) Color(0x2FA855F7) else Color(0x12A855F7),
            border = BorderStroke(1.5.dp, Color(0xFFA855F7))
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isFollowing) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = if (isFollowing) "Following" else "Follow",
                    tint = if (isFollowing) Color(0xFFC084FC) else Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isFollowing) "Following" else "Follow",
                    color = if (isFollowing) Color(0xFFC084FC) else Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

/**
 * Single Surah item displaying Surah number pill, English name, Arabic name,
 * ayah count ("7 Ayahs • 1:45"), and play button or Trending Purple equalizer.
 */
@Composable
private fun ReciterSurahListItem(
    surah: Surah,
    reciter: Reciter,
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    onItemClick: () -> Unit
) {
    val durationText = remember(surah.ayahsCount) {
        formatSurahDuration(surah.ayahsCount)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Surah Number Pill
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isCurrentTrack) Color(0x26A855F7) else Color(0xFF151821)
                )
                .border(
                    width = 1.dp,
                    color = if (isCurrentTrack) Color(0xFFA855F7) else Color(0x1FFFFFFF),
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = surah.id.toString(),
                color = if (isCurrentTrack) Color(0xFFA855F7) else Color(0xFFE1E3E4),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // English Name & Ayah Count with Duration
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = surah.nameEn,
                color = if (isCurrentTrack) Color(0xFFC084FC) else Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${surah.ayahsCount} Ayahs • $durationText",
                color = Color(0xFF9CA3AF),
                fontSize = 12.sp,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Arabic Name
        Text(
            text = surah.nameAr,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Play Button or Active Trending Purple Equalizer Bar
        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isPlaying) {
                ActiveTrendingPurpleEqualizer()
            } else if (isCurrentTrack) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x26A855F7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        tint = Color(0xFFA855F7),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x14FFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color(0xFFE1E3E4),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Animated live equalizer bars pulsing in Trending Purple (#A855F7).
 */
@Composable
private fun ActiveTrendingPurpleEqualizer(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ReciterEq")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 19f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eqBar1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eqBar2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 21f,
        animationSpec = infiniteRepeatable(
            animation = tween(360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eqBar3"
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 17f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eqBar4"
    )

    Row(
        modifier = modifier.height(24.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(h1.dp)
                .background(Color(0xFFA855F7), RoundedCornerShape(1.5.dp))
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(h2.dp)
                .background(Color(0xFFA855F7), RoundedCornerShape(1.5.dp))
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(h3.dp)
                .background(Color(0xFFA855F7), RoundedCornerShape(1.5.dp))
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(h4.dp)
                .background(Color(0xFFA855F7), RoundedCornerShape(1.5.dp))
        )
    }
}

/**
 * Helper to compute formatted duration from ayah count (e.g. 7 ayahs -> "1:45").
 */
private fun formatSurahDuration(ayahsCount: Int): String {
    val totalSeconds = ayahsCount * 15
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

/**
 * Helper metadata class for reciter profile presentation.
 */
private data class ReciterDisplayMeta(
    val photoUrl: String?,
    val followers: String,
    val country: String,
    val riwayah: String,
    val style: String
)

/**
 * Resolves full profile presentation metadata including avatar photo, follower count,
 * country, Riwayah, and recitation style.
 */
private fun resolveReciterMetadata(reciter: Reciter): ReciterDisplayMeta {
    val cleanSlug = reciter.slug.lowercase()

    val verifiedFromList = ALL_VERIFIED_RECITERS.find {
        it.slug.lowercase() == cleanSlug ||
        (cleanSlug == "alafasy" && it.slug == "mishary") ||
        (cleanSlug == "sudais" && it.slug == "al-sudais") ||
        (cleanSlug == "muaiqly" && it.slug == "al-muaiqly") ||
        (cleanSlug == "dossari" && it.slug == "al-dossari") ||
        (cleanSlug.startsWith("abdulbaset") && it.slug.startsWith("abdul")) ||
        (cleanSlug == "shuraym" && it.slug == "shuraim") ||
        (cleanSlug.contains("minshawi") && it.slug.contains("minshawi"))
    }

    val photoUrl = verifiedFromList?.photoUrl
        ?: reciter.imageUrl
        ?: StitchAssets.VerifiedReciters.find {
            it.slug.lowercase() == cleanSlug ||
            (cleanSlug == "alafasy" && it.slug == "mishary") ||
            (cleanSlug == "sudais" && it.slug == "al-sudais") ||
            (cleanSlug == "muaiqly" && it.slug == "al-muaiqly") ||
            (cleanSlug == "dossari" && it.slug == "al-dossari") ||
            (cleanSlug.startsWith("abdulbaset") && it.slug.startsWith("abdul"))
        }?.photoUrl
        ?: StitchAssets.LibraryMisharyAvatar

    val followers = verifiedFromList?.followers ?: when {
        cleanSlug.contains("alafasy") || cleanSlug == "mishary" -> "4.8M followers"
        cleanSlug.startsWith("abdul") -> "5.1M followers"
        cleanSlug.contains("minshawi") -> "4.6M followers"
        cleanSlug.contains("husary") -> "4.2M followers"
        cleanSlug.contains("sudais") -> "3.9M followers"
        cleanSlug.contains("shuraym") || cleanSlug == "shuraim" -> "3.4M followers"
        cleanSlug.contains("muaiqly") -> "3.2M followers"
        cleanSlug.contains("dossari") -> "2.7M followers"
        cleanSlug.contains("ghamdi") -> "2.5M followers"
        cleanSlug.contains("ajamy") -> "2.3M followers"
        cleanSlug.contains("sobhi") -> "2.1M followers"
        cleanSlug.contains("rifai") -> "1.9M followers"
        cleanSlug.contains("hisham") -> "1.5M followers"
        else -> "2.4M followers"
    }

    val country = verifiedFromList?.country ?: when {
        cleanSlug.contains("alafasy") || cleanSlug == "mishary" -> "Kuwait"
        cleanSlug.contains("husary") || cleanSlug.contains("minshawi") ||
            cleanSlug.startsWith("abdul") || cleanSlug.contains("sobhi") ||
            cleanSlug.contains("hisham") -> "Egypt"
        else -> "Saudi Arabia"
    }

    val riwayah = if (reciter.riwayah.isNotBlank()) reciter.riwayah else "Hafs"

    val style = verifiedFromList?.style ?: when {
        reciter.style.contains("mujawwad", ignoreCase = true) -> "Mujawwad"
        reciter.style.contains("taraweeh", ignoreCase = true) -> "Taraweeh"
        else -> "Murattal"
    }

    return ReciterDisplayMeta(
        photoUrl = photoUrl,
        followers = followers,
        country = country,
        riwayah = riwayah,
        style = style
    )
}
