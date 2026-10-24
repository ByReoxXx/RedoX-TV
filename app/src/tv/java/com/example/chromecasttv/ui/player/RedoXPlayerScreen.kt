@file:OptIn(
    androidx.tv.material3.ExperimentalTvMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class
)

package com.example.chromecasttv

import android.app.PictureInPictureParams
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.view.inputmethod.InputMethodManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.util.Rational
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.URL
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.*

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.TextureView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.DefaultRenderersFactory
import org.json.JSONArray
import org.json.JSONObject
import android.util.Base64
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun RedoXPlayerScreen(
    playingChannel: TvChannel,
    previousChannel: TvChannel? = null,
    allChannels: List<TvChannel>,
    favoriteIds: Set<String>,
    lockedIds: Set<String>,
    brokenIds: Set<String>,
    preferredSources: Map<String, Int> = emptyMap(),
    preferredScales: Map<String, String> = emptyMap(),
    epgMap: Map<String, List<EpgProgram>>,
    sleepTimerRemaining: Int?,
    onZapChannel: (delta: Int) -> Unit,
    onRecallPreviousChannel: () -> Unit = {},
    onSelectChannelDirect: (TvChannel) -> Unit,
    onSelectChannelByNumber: (Int) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onHideChannel: (String) -> Unit = {},
    onSavePreferredSource: (String, Int) -> Unit = { _, _ -> },
    onSavePreferredScale: (String, String) -> Unit = { _, _ -> },
    onMarkBroken: (String) -> Unit,
    onClearBroken: (String) -> Unit,
    onMinimizeToPip: () -> Unit,
    onEnterSystemPip: () -> Unit,
    onOpenMultiView: () -> Unit,
    onCycleSleepTimer: () -> Unit,
    onExitPlayer: () -> Unit,
    featurePrefs: FeaturePrefs = FeaturePrefs(),
    onUpdatePrefs: ((FeaturePrefs) -> FeaturePrefs) -> Unit = {},
    statusMessage: String? = null,
    onClearStatus: () -> Unit = {}
) {
    val context = LocalContext.current
    val playerSurfaceFocusRequester = remember { FocusRequester() }
    val primaryActionFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    var showOsd by remember { mutableStateOf(true) }
    var osdInteractiveFocused by remember { mutableStateOf(false) }
    var showChannelDrawer by remember { mutableStateOf(false) }
    var showVirtualNumpadDialog by remember { mutableStateOf(false) }
    var showMoreOptions by remember { mutableStateOf(false) }
    var focusedOsdLabel by remember { mutableStateOf("") }
    val drawerActiveItemRequester = remember { FocusRequester() }
    var osdInteractionTick by remember { mutableIntStateOf(0) }

    var playbackError by remember { mutableStateOf<String?>(null) }
    var autoFailoverBanner by remember { mutableStateOf<String?>(null) }
    var isBuffering by remember { mutableStateOf(true) }
    var isPausedByTimeshift by remember(playingChannel.id) { mutableStateOf(false) }
    var scaleMode by remember(playingChannel.id) {
        val savedScale = preferredScales[playingChannel.id]?.let { name ->
            PlayerScaleMode.entries.find { it.name == name }
        } ?: PlayerScaleMode.FIT
        mutableStateOf(savedScale)
    }
    var videoResolutionBadge by remember { mutableStateOf(playingChannel.qualityBadge) }
    // Gerçek (ölçülen) yayın bilgileri: çözünürlük, bit hızı, ağ tahmini, düşen kare. Uydurma değer yoktur.
    var stats by remember { mutableStateOf(PlayerStats()) }
    var nightActive by remember { mutableStateOf(false) }

    var currentSourceIndex by remember(playingChannel.id) {
        val savedIdx = (preferredSources[playingChannel.id] ?: 0)
            .coerceIn(0, (playingChannel.sources.size - 1).coerceAtLeast(0))
        mutableIntStateOf(savedIdx)
    }
    val activeSource = playingChannel.sources.getOrElse(currentSourceIndex) {
        StreamSource("FHD 1080p", playingChannel.primaryStreamUrl, StreamType.HLS)
    }

    var audioTracks by remember { mutableStateOf<List<TrackOption>>(emptyList()) }
    var subtitleTracks by remember { mutableStateOf<List<TrackOption>>(emptyList()) }
    var activeAudioLabel by remember { mutableStateOf("Ses") }
    var activeSubtitleLabel by remember { mutableStateOf("Altyazı") }

    var typedChannelDigits by remember { mutableStateOf("") }
    var digitJob by remember { mutableStateOf<Job?>(null) }

    fun bumpOsdTimer(focusControls: Boolean = false) {
        showOsd = true
        osdInteractionTick += 1
        if (focusControls) {
            osdInteractiveFocused = true
        }
    }

    LaunchedEffect(showOsd, showChannelDrawer, osdInteractionTick) {
        if (showOsd && !showChannelDrawer) {
            delay(3800L)
            showOsd = false
            osdInteractiveFocused = false
            try { playerSurfaceFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    LaunchedEffect(showOsd, osdInteractiveFocused) {
        if (showOsd && osdInteractiveFocused && !showChannelDrawer) {
            delay(60L)
            try { primaryActionFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    LaunchedEffect(showOsd) {
        if (!showOsd) {
            showMoreOptions = false
            focusedOsdLabel = ""
        }
    }

    LaunchedEffect(autoFailoverBanner) {
        if (autoFailoverBanner != null) {
            delay(3000L)
            autoFailoverBanner = null
        }
    }

    val exoPlayer = remember {
        createTvExoPlayer(context).apply {
            playWhenReady = true
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            applyPlayerQuality(this, featurePrefs.defaultQuality)
        }
    }
    val nightEffect = remember { NightModeEffect() }

    // Kalite sınırı (Ayarlar veya oynatıcıdaki "Kalite" düğmesi) anında uygulanır
    LaunchedEffect(exoPlayer, featurePrefs.defaultQuality) {
        applyPlayerQuality(exoPlayer, featurePrefs.defaultQuality)
    }
    // Gece modu: ses oturumu yayın başladıktan sonra hazır olur
    LaunchedEffect(exoPlayer, featurePrefs.nightMode, isBuffering, playingChannel.id) {
        if (!isBuffering) {
            nightActive = nightEffect.update(exoPlayer, featurePrefs.nightMode)
        }
    }
    // Gerçek istatistikler yalnızca bilgi çubuğu görünürken, 2 sn'de bir okunur
    LaunchedEffect(exoPlayer, showOsd) {
        while (showOsd) {
            stats = samplePlayerStats(context, exoPlayer)
            delay(2000L)
        }
    }

    fun switchToNextSourceManually() {
        if (playingChannel.sources.size > 1) {
            val nextIdx = (currentSourceIndex + 1) % playingChannel.sources.size
            currentSourceIndex = nextIdx
            onSavePreferredSource(playingChannel.id, nextIdx)
            val nextLabel = playingChannel.sources[nextIdx].label
            autoFailoverBanner = "Kaynak kaydedildi (${nextIdx + 1}/${playingChannel.sources.size}): $nextLabel"
            bumpOsdTimer()
        } else {
            exoPlayer.seekToDefaultPosition()
            exoPlayer.prepare()
            exoPlayer.play()
            isPausedByTimeshift = false
            autoFailoverBanner = "Yayın akışı yenilendi"
            bumpOsdTimer()
        }
    }

    fun toggleTimeshiftPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            isPausedByTimeshift = true
            autoFailoverBanner = "Canlı yayın duraklatıldı (tampon açık)"
        } else {
            exoPlayer.play()
            isPausedByTimeshift = false
            autoFailoverBanner = "Yayın devam ediyor"
        }
        bumpOsdTimer()
    }

    fun rewindTimeshift15Sec() {
        val targetPos = (exoPlayer.currentPosition - 15_000L).coerceAtLeast(0L)
        exoPlayer.seekTo(targetPos)
        exoPlayer.play()
        isPausedByTimeshift = false
        autoFailoverBanner = "15 sn geri sarıldı"
        bumpOsdTimer()
    }

    fun jumpToLiveEdge() {
        exoPlayer.seekToDefaultPosition()
        exoPlayer.play()
        isPausedByTimeshift = false
        autoFailoverBanner = "Canlı yayına dönüldü"
        bumpOsdTimer()
    }

    fun cycleQuality() {
        val all = PlayerQuality.entries
        val next = all[(all.indexOf(featurePrefs.defaultQuality) + 1) % all.size]
        onUpdatePrefs { it.copy(defaultQuality = next) }
        autoFailoverBanner = "Kalite: ${next.label}"
        bumpOsdTimer()
    }

    fun appendChannelDigit(digit: Char) {
        if (typedChannelDigits.length < 4) {
            typedChannelDigits += digit
            digitJob?.cancel()
            digitJob = coroutineScope.launch {
                delay(1100)
                val targetNum = typedChannelDigits.toIntOrNull()
                typedChannelDigits = ""
                if (targetNum != null) {
                    onSelectChannelByNumber(targetNum)
                }
            }
        }
    }

    fun cycleAudioTrack() {
        val groups = exoPlayer.currentTracks.groups
        if (audioTracks.size <= 1) {
            autoFailoverBanner = "Bu yayında tek ses izi var"
            bumpOsdTimer()
            return
        }
        val currentIdx = audioTracks.indexOfFirst { it.isSelected }.coerceAtLeast(0)
        val nextOpt = audioTracks[(currentIdx + 1) % audioTracks.size]
        val group = groups.getOrNull(nextOpt.groupIndex) ?: return
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, nextOpt.trackIndex))
            .build()
        activeAudioLabel = "Ses: ${nextOpt.label}"
        bumpOsdTimer()
    }

    fun cycleSubtitleTrack() {
        val groups = exoPlayer.currentTracks.groups
        if (subtitleTracks.isEmpty()) {
            autoFailoverBanner = "Bu yayında altyazı izi yok"
            bumpOsdTimer()
            return
        }
        val currentIdx = subtitleTracks.indexOfFirst { it.isSelected }
        if (currentIdx == -1 || currentIdx + 1 < subtitleTracks.size) {
            val nextOpt = subtitleTracks[(currentIdx + 1).coerceAtLeast(0)]
            val group = groups.getOrNull(nextOpt.groupIndex) ?: return
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, nextOpt.trackIndex))
                .build()
            activeSubtitleLabel = "Altyazı: ${nextOpt.label}"
        } else {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            activeSubtitleLabel = "Altyazı: Kapalı"
        }
        bumpOsdTimer()
    }

    var autoTriedCount by remember(playingChannel.id) { mutableIntStateOf(0) }

    DisposableEffect(exoPlayer, playingChannel.id, currentSourceIndex) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    playbackError = null
                    autoTriedCount = 0
                    onClearBroken(playingChannel.id)
                    if (preferredSources[playingChannel.id] != currentSourceIndex) {
                        onSavePreferredSource(playingChannel.id, currentSourceIndex)
                    }
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                val aList = mutableListOf<TrackOption>()
                val sList = mutableListOf<TrackOption>()
                tracks.groups.forEachIndexed { gIdx, group ->
                    if (group.type == C.TRACK_TYPE_AUDIO) {
                        for (tIdx in 0 until group.length) {
                            val fmt = group.getTrackFormat(tIdx)
                            val lbl = fmt.label ?: fmt.language?.uppercase() ?: "SES ${aList.size + 1}"
                            aList.add(TrackOption(lbl, gIdx, tIdx, group.isTrackSelected(tIdx)))
                        }
                    } else if (group.type == C.TRACK_TYPE_TEXT) {
                        for (tIdx in 0 until group.length) {
                            val fmt = group.getTrackFormat(tIdx)
                            val lbl = fmt.label ?: fmt.language?.uppercase() ?: "ALTYAZI ${sList.size + 1}"
                            sList.add(TrackOption(lbl, gIdx, tIdx, group.isTrackSelected(tIdx)))
                        }
                    }
                }
                audioTracks = aList
                subtitleTracks = sList
                aList.firstOrNull { it.isSelected }?.let { activeAudioLabel = "Ses: ${it.label}" }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.height > 0) {
                    videoResolutionBadge = when {
                        videoSize.height >= 2160 -> "4K UHD"
                        videoSize.height >= 1080 -> "1080p FHD"
                        videoSize.height >= 720 -> "720p HD"
                        else -> "${videoSize.height}p"
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (playingChannel.sources.size > 1 && autoTriedCount + 1 < playingChannel.sources.size) {
                    autoTriedCount += 1
                    val nextIdx = (currentSourceIndex + 1) % playingChannel.sources.size
                    autoFailoverBanner = "Kaynak yanıt vermedi; alternatif akışa (${nextIdx + 1}/${playingChannel.sources.size}) geçildi"
                    currentSourceIndex = nextIdx
                } else {
                    isBuffering = false
                    playbackError = "Tüm alternatif kaynaklar denendi. Kaynak düğmesiyle tekrar deneyebilirsiniz."
                    onMarkBroken(playingChannel.id)
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            nightEffect.release()
            exoPlayer.release()
        }
    }

    LaunchedEffect(playingChannel.id, currentSourceIndex) {
        playbackError = null
        isBuffering = true
        osdInteractiveFocused = false
        videoResolutionBadge = playingChannel.qualityBadge
        bumpOsdTimer(focusControls = false)
        try { playerSurfaceFocusRequester.requestFocus() } catch (_: Exception) {}

        if (activeSource.url.isNotBlank()) {
            exoPlayer.stop()
            exoPlayer.setMediaSource(buildStreamMediaSource(context, activeSource))
            exoPlayer.prepare()
            exoPlayer.play()
        }
        delay(2500L)
        if (isBuffering && exoPlayer.playbackState != Player.STATE_READY && playingChannel.sources.size > 1 && autoTriedCount + 1 < playingChannel.sources.size) {
            autoTriedCount += 1
            val nextIdx = (currentSourceIndex + 1) % playingChannel.sources.size
            autoFailoverBanner = "Yayın gecikti; alternatif akışa (${nextIdx + 1}/${playingChannel.sources.size}) geçildi"
            currentSourceIndex = nextIdx
        }
    }

    LaunchedEffect(Unit) {
        try { playerSurfaceFocusRequester.requestFocus() } catch (_: Exception) {}
    }

    BackHandler {
        when {
            showChannelDrawer -> {
                showChannelDrawer = false
                try { playerSurfaceFocusRequester.requestFocus() } catch (_: Exception) {}
            }
            showOsd -> {
                showOsd = false
                osdInteractiveFocused = false
                try { playerSurfaceFocusRequester.requestFocus() } catch (_: Exception) {}
            }
            else -> onExitPlayer()
        }
    }

    val isBrokenChannel = brokenIds.contains(playingChannel.id) || playingChannel.isBroken
    val catColor = if (isBrokenChannel) TvThemeColors.BrokenRed else TvThemeColors.categoryColor(playingChannel.category)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(playerSurfaceFocusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    val keyCode = event.nativeKeyEvent.keyCode
                    val isRepeat = event.nativeKeyEvent.repeatCount > 0

                    if (isRepeat && (
                                keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                                keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                                keyCode == KeyEvent.KEYCODE_CHANNEL_UP ||
                                keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN ||
                                keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                            )
                    ) {
                        return@onPreviewKeyEvent true
                    }

                    if (showOsd) {
                        osdInteractionTick += 1
                    }
                    if (!showChannelDrawer && !osdInteractiveFocused) {
                        when {
                            keyCode == KeyEvent.KEYCODE_0 && typedChannelDigits.isEmpty() && previousChannel != null -> {
                                autoFailoverBanner = "Önceki kanala dönüldü: ${previousChannel.name}"
                                onRecallPreviousChannel()
                                true
                            }
                            keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                                val digit = ('0'.code + (keyCode - KeyEvent.KEYCODE_0)).toChar()
                                appendChannelDigit(digit)
                                true
                            }
                            keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                            keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE ||
                            keyCode == KeyEvent.KEYCODE_MEDIA_PLAY -> {
                                toggleTimeshiftPause()
                                true
                            }
                            keyCode == KeyEvent.KEYCODE_MEDIA_REWIND -> {
                                rewindTimeshift15Sec()
                                true
                            }
                            keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_CHANNEL_UP -> {
                                onZapChannel(+1)
                                true
                            }
                            keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                                onZapChannel(-1)
                                true
                            }
                            keyCode == KeyEvent.KEYCODE_DPAD_LEFT -> {
                                showChannelDrawer = true
                                showOsd = false
                                true
                            }
                            keyCode == KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                switchToNextSourceManually()
                                true
                            }
                            keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER -> {
                                bumpOsdTimer(focusControls = true)
                                true
                            }
                            else -> false
                        }
                    } else false
                } else false
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    keepScreenOn = true
                    resizeMode = scaleMode.resizeMode
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                view.keepScreenOn = true
                view.resizeMode = scaleMode.resizeMode
            },
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    if (showChannelDrawer) {
                        showChannelDrawer = false
                    } else if (showOsd) {
                        showOsd = false
                    } else {
                        bumpOsdTimer(focusControls = true)
                    }
                }
        )

        if (typedChannelDigits.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(28.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TvThemeColors.GlassOverlay)
                    .border(2.dp, RedoXColors.Gold, RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "CH $typedChannelDigits",
                    color = RedoXColors.Gold,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        autoFailoverBanner?.let { bannerText ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp)
                    .clip(RoundedCornerShape(50))
                    .background(TvThemeColors.SurfaceElevated)
                    .border(1.5.dp, RedoXColors.Gold, RoundedCornerShape(50))
                    .padding(horizontal = 20.dp, vertical = 9.dp)
            ) {
                Icon(
                    imageVector = RedoXIcons.Sensors,
                    contentDescription = null,
                    tint = RedoXColors.Gold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(9.dp))
                Text(
                    text = bannerText,
                    color = TvThemeColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Hatırlatıcı / sistem bildirimi (ör. "Hatırlatma: Kanal • Program başlıyor")
        statusMessage?.let { message ->
            LaunchedEffect(message) {
                delay(4200L)
                onClearStatus()
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp)
                    .clip(RoundedCornerShape(50))
                    .background(TvThemeColors.SurfaceElevated)
                    .border(1.5.dp, TvThemeColors.FavoriteGold, RoundedCornerShape(50))
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = RedoXIcons.NotificationsActive,
                    contentDescription = null,
                    tint = TvThemeColors.FavoriteGold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message,
                    color = TvThemeColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        if (isBuffering && playbackError == null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(14.dp))
                    .background(TvThemeColors.GlassOverlay)
                    .border(1.dp, TvThemeColors.BullRedBright.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = RedoXIcons.Sensors,
                    contentDescription = null,
                    tint = TvThemeColors.BullRedBright,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${playingChannel.name} • ${activeSource.label} (${currentSourceIndex + 1}/${playingChannel.sources.size}) bağlanıyor...",
                    color = TvThemeColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        playbackError?.let { err ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TvThemeColors.GlassOverlay)
                    .border(2.dp, TvThemeColors.BrokenRed, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Icon(
                    imageVector = RedoXIcons.WifiOff,
                    contentDescription = null,
                    tint = TvThemeColors.BrokenRed,
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = err,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                TvCompactChipButton(
                    text = "Alternatif kaynağı dene (${currentSourceIndex + 1}/${playingChannel.sources.size})",
                    icon = RedoXIcons.Refresh,
                    onClick = { switchToNextSourceManually() }
                )
            }
        }

        // SOL HIZLI KANAL LİSTESİ ÇEKMECESİ (140ms HIZLI SLIDE)
        AnimatedVisibility(
            visible = showChannelDrawer,
            enter = slideInHorizontally(animationSpec = tween(140), initialOffsetX = { -it }) + fadeIn(tween(120)),
            exit = slideOutHorizontally(animationSpec = tween(120), targetOffsetX = { -it }) + fadeOut(tween(100)),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            var selectedDrawerCat by remember { mutableStateOf("Tümü") }
            val favKey = "__redox_favorites__"
            val drawerCategories = remember(allChannels, favoriteIds) {
                val list = mutableListOf("Tümü", favKey)
                list.addAll(allChannels.map { it.category }.distinct())
                list
            }
            val filteredDrawerChannels = remember(allChannels, selectedDrawerCat, favoriteIds) {
                when (selectedDrawerCat) {
                    "Tümü" -> allChannels
                    favKey -> allChannels.filter { favoriteIds.contains(it.id) }
                    else -> allChannels.filter { it.category.equals(selectedDrawerCat, ignoreCase = true) }
                }
            }
            val currentChannelIndex = remember(playingChannel.id, filteredDrawerChannels) {
                filteredDrawerChannels.indexOfFirst { it.id == playingChannel.id }.coerceAtLeast(0)
            }
            val listState = rememberLazyListState(
                initialFirstVisibleItemIndex = (currentChannelIndex - 3).coerceAtLeast(0)
            )
            LaunchedEffect(selectedDrawerCat) {
                val idx = filteredDrawerChannels.indexOfFirst { it.id == playingChannel.id }.coerceAtLeast(0)
                try { listState.scrollToItem((idx - 2).coerceAtLeast(0)) } catch (_: Exception) {}
                delay(70L)
                try { drawerActiveItemRequester.requestFocus() } catch (_: Exception) {}
            }

            Column(
                modifier = Modifier
                    .width(400.dp)
                    .fillMaxHeight()
                    .background(TvThemeColors.GlassOverlay)
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hızlı kanal listesi (${filteredDrawerChannels.size})",
                            color = TvThemeColors.TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "OK: izle  •  OK basılı tut: favori ekle / çıkar",
                            color = RedoXColors.Gold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                    TvCompactChipButton(
                        text = "Kapat",
                        icon = RedoXIcons.Clear,
                        onClick = {
                            showChannelDrawer = false
                            try { playerSurfaceFocusRequester.requestFocus() } catch (_: Exception) {}
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(drawerCategories, key = { "dcat_$it" }) { dCat ->
                        val isSel = selectedDrawerCat == dCat
                        val catLabel = if (dCat == favKey) "Favoriler" else dCat
                        TvCompactChipButton(
                            text = if (isSel) "✓ $catLabel" else catLabel,
                            icon = if (dCat == favKey) RedoXIcons.Star else RedoXIcons.Category,
                            onClick = { selectedDrawerCat = dCat }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    itemsIndexed(filteredDrawerChannels, key = { idx, ch -> "drawer_${ch.id}_$idx" }) { _, ch ->
                        var itemFocused by remember { mutableStateOf(false) }
                        val isCurrent = ch.id == playingChannel.id
                        val isFav = favoriteIds.contains(ch.id)
                        val isBroken = brokenIds.contains(ch.id) || ch.isBroken
                        val itemShape = RoundedCornerShape(12.dp)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (isCurrent) Modifier.focusRequester(drawerActiveItemRequester) else Modifier)
                                .onPreviewKeyEvent { ev ->
                                    if (ev.nativeKeyEvent.action == KeyEvent.ACTION_DOWN && ev.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                                        showChannelDrawer = false
                                        try { playerSurfaceFocusRequester.requestFocus() } catch (_: Exception) {}
                                        true
                                    } else false
                                }
                                .tvSurface(
                                    focused = itemFocused,
                                    shape = itemShape,
                                    color = if (isCurrent) RedoXColors.Primary.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f),
                                    idleBorder = if (isCurrent) RedoXColors.Primary.copy(alpha = 0.55f) else Color.Transparent,
                                    ringWidth = 2.5.dp
                                )
                                .onFocusChanged { itemFocused = it.isFocused }
                                .tvLongPressFavoriteModifier(
                                    itemKey = "drawer_fav_${ch.id}",
                                    onShortClick = {
                                        onSelectChannelDirect(ch)
                                        showChannelDrawer = false
                                        try { playerSurfaceFocusRequester.requestFocus() } catch (_: Exception) {}
                                    },
                                    onLongPress = {
                                        val wasFav = favoriteIds.contains(ch.id)
                                        onToggleFavorite(ch.id)
                                        autoFailoverBanner = if (wasFav) {
                                            "${ch.name} favorilerden çıkarıldı"
                                        } else {
                                            "${ch.name} favorilere eklendi"
                                        }
                                    }
                                )
                                .clickable {
                                    onSelectChannelDirect(ch)
                                    showChannelDrawer = false
                                    try { playerSurfaceFocusRequester.requestFocus() } catch (_: Exception) {}
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${ch.number}",
                                color = if (itemFocused || isCurrent) TvThemeColors.TextPrimary else RedoXColors.TextTertiary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                modifier = Modifier.width(34.dp)
                            )
                            ChannelBrandLogoBox(channel = ch, boxSize = 34.dp, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ch.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when {
                                        isBroken -> "Yanıt vermiyor"
                                        itemFocused -> if (isFav) "Favoride • basılı tut: çıkar" else "Basılı tut: favoriye ekle"
                                        else -> "${ch.sources.size} akış • ${ch.nowTitleOrLive()}"
                                    },
                                    color = when {
                                        itemFocused -> RedoXColors.Gold
                                        isBroken -> TvThemeColors.BrokenRed
                                        else -> TvThemeColors.TextSecondary
                                    },
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (isFav) RedoXIcons.StarFilled else RedoXIcons.StarBorder,
                                contentDescription = null,
                                tint = if (isFav) RedoXColors.Gold else RedoXColors.TextTertiary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }

        // ALT İNTERAKTİF OSD: bilgi satırı + ortada oynatma denetimleri + program ilerlemesi + yayın denetimleri
        // (140ms hafif slide, 3.8 sn hareketsizlikte otomatik gizlenir). Düğmeler yalnızca simgedir;
        // odaktaki düğmenin adı altta tek satırda gösterilir (tüm ekran genişliklerinde taşma olmaz).
        AnimatedVisibility(
            visible = showOsd && !showChannelDrawer,
            enter = slideInVertically(animationSpec = tween(140), initialOffsetY = { it }) + fadeIn(tween(120)),
            exit = slideOutVertically(animationSpec = tween(140), targetOffsetY = { it }) + fadeOut(tween(120)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 36.dp, vertical = 24.dp)
        ) {
            val osdShape = RoundedCornerShape(24.dp)
            val isFavorite = favoriteIds.contains(playingChannel.id)
            val compactOsd = isCompactOsd()
            Column(
                verticalArrangement = Arrangement.spacedBy(if (compactOsd) 10.dp else 14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(osdShape)
                    .background(TvThemeColors.GlassOverlay)
                    .border(1.dp, Color.White.copy(alpha = 0.10f), osdShape)
                    .padding(horizontal = 26.dp, vertical = if (compactOsd) 14.dp else 18.dp)
            ) {
                // ---- kanal bilgisi
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChannelBrandLogoBox(
                        channel = playingChannel,
                        boxSize = if (compactOsd) 52.dp else 64.dp,
                        fontSize = if (compactOsd) 16.sp else 18.sp
                    )
                    Spacer(modifier = Modifier.width(18.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(if (compactOsd) 4.dp else 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TvLiveBadge(
                                text = if (isBrokenChannel) "ÇALIŞMIYOR" else "CANLI",
                                broken = isBrokenChannel
                            )
                            val statsText = buildString {
                                append(if (stats.height > 0) stats.resolutionLabel else videoResolutionBadge)
                                if (stats.videoBitrateKbps > 0) {
                                    append(String.format(Locale.US, " · %.1f Mbps", stats.videoBitrateKbps / 1000f))
                                }
                            }
                            TvStatPill(text = statsText)
                            if (playingChannel.sources.size > 1) {
                                TvStatPill(
                                    text = "Akış ${currentSourceIndex + 1}/${playingChannel.sources.size}",
                                    color = TvThemeColors.TextSecondary
                                )
                            }
                            if (nightActive) {
                                TvStatPill(text = "Gece modu")
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "${playingChannel.number} · ${playingChannel.name}",
                                color = TvThemeColors.TextPrimary,
                                fontSize = if (compactOsd) 24.sp else 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(
                                text = playingChannel.nowNextLine(),
                                color = TvThemeColors.TextSecondary,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1.6f, fill = false)
                                    .padding(bottom = 3.dp)
                            )
                        }
                    }
                }

                // ---- oynatma denetimleri: kanal bilgisinin altında, ekranın ortasında
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        if (compactOsd) 12.dp else 16.dp,
                        Alignment.CenterHorizontally
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TvOsdButton(
                        icon = RedoXIcons.SkipPrevious,
                        label = "Önceki kanal",
                        onFocusLabel = { focusedOsdLabel = it },
                        onClick = { onZapChannel(-1) }
                    )
                    TvOsdButton(
                        icon = RedoXIcons.History,
                        label = "15 sn geri",
                        onFocusLabel = { focusedOsdLabel = it },
                        onClick = { rewindTimeshift15Sec() }
                    )
                    TvOsdButton(
                        icon = if (isPausedByTimeshift) RedoXIcons.PlayArrow else RedoXIcons.Pause,
                        label = if (isPausedByTimeshift) "Devam et" else "Duraklat",
                        primary = true,
                        onFocusLabel = { focusedOsdLabel = it },
                        onClick = { toggleTimeshiftPause() },
                        modifier = Modifier.focusRequester(primaryActionFocusRequester)
                    )
                    TvOsdButton(
                        icon = RedoXIcons.FiberManualRecord,
                        label = "Canlıya dön",
                        onFocusLabel = { focusedOsdLabel = it },
                        onClick = { jumpToLiveEdge() }
                    )
                    TvOsdButton(
                        icon = RedoXIcons.SkipNext,
                        label = "Sonraki kanal",
                        onFocusLabel = { focusedOsdLabel = it },
                        onClick = { onZapChannel(+1) }
                    )
                }

                if (playingChannel.hasRealEpg) {
                    TvProgressBar(progress = playingChannel.progressForUi(), height = 6.dp)
                }

                // ---- alt satır: sol (kanallar, favori) • sağ (yayın ayarları)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TvOsdButton(
                            icon = RedoXIcons.LiveTv,
                            label = "Kanal listesi",
                            onFocusLabel = { focusedOsdLabel = it },
                            onClick = {
                                showChannelDrawer = true
                                showOsd = false
                            }
                        )
                        TvOsdButton(
                            icon = if (isFavorite) RedoXIcons.StarFilled else RedoXIcons.StarBorder,
                            label = if (isFavorite) "Favoriden çıkar" else "Favori ekle",
                            active = isFavorite,
                            onFocusLabel = { focusedOsdLabel = it },
                            onClick = {
                                onToggleFavorite(playingChannel.id)
                                bumpOsdTimer(focusControls = true)
                            }
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TvOsdButton(
                            icon = RedoXIcons.Refresh,
                            label = if (playingChannel.sources.size > 1) {
                                "Alternatif yayın (${currentSourceIndex + 1}/${playingChannel.sources.size})"
                            } else {
                                "Yayını yenile"
                            },
                            onFocusLabel = { focusedOsdLabel = it },
                            onClick = { switchToNextSourceManually() }
                        )
                        TvOsdButton(
                            icon = RedoXIcons.HighQuality,
                            label = "Kalite: ${featurePrefs.defaultQuality.label}",
                            onFocusLabel = { focusedOsdLabel = it },
                            onClick = { cycleQuality() }
                        )
                        TvOsdButton(
                            icon = RedoXIcons.VolumeUp,
                            label = activeAudioLabel,
                            onFocusLabel = { focusedOsdLabel = it },
                            onClick = { cycleAudioTrack() }
                        )
                        TvOsdButton(
                            icon = RedoXIcons.ClosedCaption,
                            label = activeSubtitleLabel,
                            onFocusLabel = { focusedOsdLabel = it },
                            onClick = { cycleSubtitleTrack() }
                        )
                        TvOsdButton(
                            icon = RedoXIcons.AspectRatio,
                            label = "Ölçek: ${scaleMode.label}",
                            onFocusLabel = { focusedOsdLabel = it },
                            onClick = {
                                val modes = PlayerScaleMode.entries
                                val nextMode = modes[(modes.indexOf(scaleMode) + 1) % modes.size]
                                scaleMode = nextMode
                                onSavePreferredScale(playingChannel.id, nextMode.name)
                                bumpOsdTimer(focusControls = true)
                            }
                        )
                        TvOsdButton(
                            icon = RedoXIcons.MoreHoriz,
                            label = if (showMoreOptions) "Daha az" else "Daha fazla",
                            active = showMoreOptions,
                            onFocusLabel = { focusedOsdLabel = it },
                            onClick = {
                                showMoreOptions = !showMoreOptions
                                bumpOsdTimer(focusControls = true)
                            }
                        )
                    }
                }

                // ---- odaktaki düğmenin adı (yoksa kısayol ipucu) • sağda ölçülen ağ / kare bilgisi
                val diagText = buildList {
                    if (stats.bandwidthKbps > 0) add(String.format(Locale.US, "ağ ~%.1f Mbps", stats.bandwidthKbps / 1000f))
                    if (stats.droppedFrames > 0) add("${stats.droppedFrames} kare düştü")
                }.joinToString("  •  ")
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (focusedOsdLabel.isNotEmpty()) {
                            focusedOsdLabel
                        } else {
                            "Sol: kanal listesi  •  Sağ: kaynak değiştir  •  Yukarı / aşağı: kanal  •  OK: kontroller"
                        },
                        color = if (focusedOsdLabel.isNotEmpty()) RedoXColors.Gold else RedoXColors.TextTertiary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (diagText.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = diagText,
                            color = RedoXColors.TextTertiary,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                }

                // ---- daha fazla seçenek
                AnimatedVisibility(visible = showMoreOptions) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            TvCompactChipButton(
                                text = if (featurePrefs.nightMode) "Gece modu: Açık" else "Gece modu: Kapalı",
                                icon = RedoXIcons.Bedtime,
                                onClick = {
                                    onUpdatePrefs { it.copy(nightMode = !it.nightMode) }
                                    bumpOsdTimer(focusControls = true)
                                }
                            )
                        }
                        item {
                            TvCompactChipButton(
                                text = "Küçük pencere (PiP)",
                                icon = RedoXIcons.SmartDisplay,
                                onClick = onMinimizeToPip
                            )
                        }
                        item {
                            TvCompactChipButton(
                                text = "2x2 ekran",
                                icon = RedoXIcons.GridView,
                                onClick = onOpenMultiView
                            )
                        }
                        item {
                            TvCompactChipButton(
                                text = "Kanal no (0-9)",
                                icon = RedoXIcons.Tag,
                                onClick = { showVirtualNumpadDialog = true }
                            )
                        }
                        if (previousChannel != null) {
                            item {
                                TvCompactChipButton(
                                    text = "Önceki: ${previousChannel.name} (0)",
                                    icon = RedoXIcons.SwapHoriz,
                                    onClick = { onRecallPreviousChannel() }
                                )
                            }
                        }
                        item {
                            TvCompactChipButton(
                                text = "Kanalı gizle",
                                icon = RedoXIcons.VisibilityOff,
                                onClick = { onHideChannel(playingChannel.id) }
                            )
                        }
                        item {
                            TvCompactChipButton(
                                text = "Kanaldan çık",
                                icon = RedoXIcons.Clear,
                                onClick = onExitPlayer
                            )
                        }
                        item {
                            TvCompactChipButton(
                                text = "Bilgi çubuğunu gizle",
                                icon = RedoXIcons.Clear,
                                onClick = {
                                    showOsd = false
                                    osdInteractiveFocused = false
                                    try { playerSurfaceFocusRequester.requestFocus() } catch (_: Exception) {}
                                }
                            )
                        }
                    }
                }
            }
        }

        // CHROMECAST 4K KUMANDASI İÇİN EKRAN ÜSTÜ SANAL NUMARATÖR (0-9) PENCERESİ
        if (showVirtualNumpadDialog) {
            var dialDigits by remember { mutableStateOf("") }
            val firstDigitRequester = remember { FocusRequester() }
            val matchedChannel = remember(dialDigits, allChannels) {
                dialDigits.toIntOrNull()?.let { num -> allChannels.find { it.number == num } }
            }
            LaunchedEffect(Unit) {
                delay(60L)
                try { firstDigitRequester.requestFocus() } catch (_: Exception) {}
            }
            Dialog(
                onDismissRequest = { showVirtualNumpadDialog = false },
                properties = DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                    usePlatformDefaultWidth = false
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.78f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .width(420.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(TvThemeColors.SurfaceCard)
                            .border(2.dp, TvThemeColors.BullRedBright, RoundedCornerShape(20.dp))
                            .padding(22.dp)
                    ) {
                        Text(
                            text = "Kanal numarası girin",
                            color = TvThemeColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(TvThemeColors.BackgroundDeep)
                                .border(1.dp, TvThemeColors.AccentCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(vertical = 10.dp, horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (dialDigits.isEmpty()) "CH ---" else "CH $dialDigits",
                                    color = TvThemeColors.BullRedBright,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = matchedChannel?.let { "${it.name} (${it.category})" } ?: "Rakamları seçip 'Kanala git'e basın",
                                    color = if (matchedChannel != null) TvThemeColors.AccentEmerald else TvThemeColors.TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        val rows = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))
                        rows.forEachIndexed { rIdx, rowNums ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                rowNums.forEachIndexed { cIdx, num ->
                                    TvCompactChipButton(
                                        text = "   $num   ",
                                        icon = RedoXIcons.Tag,
                                        onClick = {
                                            if (dialDigits.length < 4) dialDigits += num.toString()
                                        },
                                        modifier = if (rIdx == 0 && cIdx == 0) Modifier.focusRequester(firstDigitRequester) else Modifier
                                    )
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TvCompactChipButton(
                                text = "Sil",
                                icon = RedoXIcons.Clear,
                                onClick = { if (dialDigits.isNotEmpty()) dialDigits = dialDigits.dropLast(1) }
                            )
                            TvCompactChipButton(
                                text = "   0   ",
                                icon = RedoXIcons.Tag,
                                onClick = { if (dialDigits.length < 4) dialDigits += "0" }
                            )
                            TvCompactChipButton(
                                text = "Kapat",
                                icon = RedoXIcons.Clear,
                                onClick = { showVirtualNumpadDialog = false }
                            )
                        }
                        if (matchedChannel != null) {
                            TvActionButton(
                                text = "CH ${matchedChannel.number} · ${matchedChannel.name} kanalına git",
                                icon = RedoXIcons.PlayArrow,
                                isPrimary = true,
                                onClick = {
                                    showVirtualNumpadDialog = false
                                    showOsd = false
                                    onSelectChannelDirect(matchedChannel)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Dar ekranlarda (< 1100 dp; ör. 4K/1080p'de 960 dp'lik TV arayüzü) OSD düğmeleri biraz küçülür. */
@Composable
private fun isCompactOsd(): Boolean = LocalConfiguration.current.screenWidthDp < 1100

/**
 * OSD denetim düğmesi (yalnızca simge). Odaktayken altın halka + ışıma; adı [onFocusLabel] ile üst bileşene
 * bildirilir ve OSD'nin altında tek satırda gösterilir. [primary] = büyük, kırmızı oynat/duraklat düğmesi.
 */
@Composable
private fun TvOsdButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    onFocusLabel: (String) -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    primary: Boolean = false
) {
    var focused by remember { mutableStateOf(false) }
    val compact = isCompactOsd()
    val interactionSource = remember { MutableInteractionSource() }
    val shape: Shape = if (primary) CircleShape else RoundedCornerShape(16.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(if (primary) (if (compact) 60.dp else 68.dp) else (if (compact) 46.dp else 52.dp))
            .tvSurface(
                focused = focused,
                shape = shape,
                brush = if (primary) RedoXColors.PrimaryBrush else null,
                ringWidth = 3.dp
            )
            .onFocusChanged {
                focused = it.isFocused || it.hasFocus
                if (focused) onFocusLabel(label)
            }
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current) { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = when {
                primary -> Color.White
                active -> RedoXColors.Gold
                focused -> TvThemeColors.TextPrimary
                else -> TvThemeColors.TextSecondary
            },
            modifier = Modifier.size(if (primary) (if (compact) 30.dp else 34.dp) else (if (compact) 22.dp else 24.dp))
        )
    }
}
