package com.example.chromecasttv

import android.content.Context
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class MPlayerPanel { NONE, SETTINGS, CHANNELS }

/**
 * Tam ekran oynatıcı (telefon ve tablet).
 * Hareketler: sol kenarda dikey kaydır = parlaklık • sağ kenarda dikey kaydır = ses •
 * yatay kaydır = kanal değiştir • çift dokun = duraklat/oynat • dokun = kontroller.
 * Oynatıcı ortak [PlaybackHub] örneğidir; küçültünce veya arka plana alınınca ses (ayara göre) sürer.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun MobilePlayerScreen(
    channel: TvChannel,
    viewModel: TvViewModel,
    state: TvUiState,
    features: FeatureState,
    host: RedoXHost,
    isTablet: Boolean,
    onMinimize: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = features.prefs
    val player = remember { PlaybackHub.player(context) }
    val remoteHub = remember { RemoteHub.get(context) }
    val activeTv by remoteHub.active.collectAsState()
    val inPip = host.isInPip
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    val allChannels = remember(state.builtInChannels, state.customM3uChannels) {
        state.builtInChannels + state.customM3uChannels
    }
    val lastSourceIndex = (channel.sources.size - 1).coerceAtLeast(0)
    val initialSource = (state.preferredSources[channel.id] ?: 0).coerceIn(0, lastSourceIndex)
    var sourceIndex by remember(channel.id) { mutableIntStateOf(initialSource) }
    var savedSource by remember(channel.id) { mutableIntStateOf(initialSource) }
    var autoTried by remember(channel.id) { mutableIntStateOf(0) }
    var buffering by remember(channel.id) { mutableStateOf(true) }
    var errorText by remember(channel.id) { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var controlsVisible by remember { mutableStateOf(true) }
    var controlsTick by remember { mutableIntStateOf(0) }
    var panel by remember { mutableStateOf(MPlayerPanel.NONE) }
    var hud by remember { mutableStateOf<String?>(null) }
    var banner by remember { mutableStateOf<String?>(null) }
    var stats by remember { mutableStateOf(PlayerStats()) }
    var audioTracks by remember { mutableStateOf<List<TrackOption>>(emptyList()) }
    var subtitleTracks by remember { mutableStateOf<List<TrackOption>>(emptyList()) }
    var scaleMode by remember(channel.id) {
        mutableStateOf(
            state.preferredScales[channel.id]
                ?.let { name -> PlayerScaleMode.entries.firstOrNull { it.name == name } }
                ?: PlayerScaleMode.FIT
        )
    }

    // ------------------------------------------------------------------ Tam ekran düzeni
    DisposableEffect(Unit) {
        host.setImmersive(true)
        host.setKeepScreenOn(true)
        PlaybackHub.setVideoEnabled(true)
        onDispose {
            host.setImmersive(false)
            host.setKeepScreenOn(false)
            PlaybackHub.setVideoEnabled(false)
        }
    }
    DisposableEffect(prefs.autoPip) {
        host.setPlayerForeground(prefs.autoPip)
        onDispose { host.setPlayerForeground(false) }
    }

    // ------------------------------------------------------------------ Kalite ve gece modu
    LaunchedEffect(prefs.defaultQuality, prefs.mobileDataSaver) {
        applyPlayerQuality(player, effectiveQuality(context, prefs))
    }
    LaunchedEffect(prefs.nightMode) {
        PlaybackHub.setNightMode(prefs.nightMode)
    }

    // ------------------------------------------------------------------ Oynatma
    LaunchedEffect(channel.id, sourceIndex) {
        errorText = null
        PlaybackHub.play(context, channel, sourceIndex, effectiveQuality(context, prefs), prefs.nightMode)
        buffering = player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_IDLE
        controlsVisible = true
        controlsTick += 1
        delay(6_000L)
        if (buffering && player.playbackState != Player.STATE_READY &&
            channel.sources.size > 1 && autoTried + 1 < channel.sources.size
        ) {
            autoTried += 1
            banner = "Yayın gecikti, alternatif akışa geçildi"
            sourceIndex = (sourceIndex + 1) % channel.sources.size
        }
    }

    DisposableEffect(player, channel.id) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    errorText = null
                    autoTried = 0
                    viewModel.clearChannelBrokenStatus(channel.id)
                    if (savedSource != sourceIndex) {
                        savedSource = sourceIndex
                        viewModel.saveChannelPreferredSource(channel.id, sourceIndex)
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                if (channel.sources.size > 1 && autoTried + 1 < channel.sources.size) {
                    autoTried += 1
                    banner = "Kaynak yanıt vermedi, alternatif akışa geçildi"
                    sourceIndex = (sourceIndex + 1) % channel.sources.size
                } else {
                    buffering = false
                    errorText = "Yayın açılamadı. Başka bir kaynak veya kanal deneyin."
                    viewModel.markChannelAsBroken(channel.id)
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                val audio = ArrayList<TrackOption>()
                val text = ArrayList<TrackOption>()
                tracks.groups.forEachIndexed { groupIndex, group ->
                    if (group.type == C.TRACK_TYPE_AUDIO) {
                        for (t in 0 until group.length) {
                            val format = group.getTrackFormat(t)
                            val label = format.label ?: format.language?.uppercase(Locale.ROOT) ?: "SES ${audio.size + 1}"
                            audio.add(TrackOption(label, groupIndex, t, group.isTrackSelected(t)))
                        }
                    } else if (group.type == C.TRACK_TYPE_TEXT) {
                        for (t in 0 until group.length) {
                            val format = group.getTrackFormat(t)
                            val label = format.label ?: format.language?.uppercase(Locale.ROOT) ?: "ALTYAZI ${text.size + 1}"
                            text.add(TrackOption(label, groupIndex, t, group.isTrackSelected(t)))
                        }
                    }
                }
                audioTracks = audio
                subtitleTracks = text
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // ------------------------------------------------------------------ Yardımcı etkiler
    LaunchedEffect(controlsVisible, controlsTick, panel, isPlaying) {
        if (controlsVisible && panel == MPlayerPanel.NONE && isPlaying) {
            delay(4_000L)
            controlsVisible = false
        }
    }
    LaunchedEffect(hud) {
        if (hud != null) {
            delay(900L)
            hud = null
        }
    }
    LaunchedEffect(banner) {
        if (banner != null) {
            delay(2_800L)
            banner = null
        }
    }
    LaunchedEffect(player, controlsVisible, panel) {
        while (controlsVisible || panel == MPlayerPanel.SETTINGS) {
            stats = samplePlayerStats(context, player)
            delay(2_000L)
        }
    }

    BackHandler { onMinimize() }

    fun zap(delta: Int) {
        if (allChannels.isEmpty()) return
        val index = allChannels.indexOfFirst { it.id == channel.id }.coerceAtLeast(0)
        viewModel.requestPlayChannel(allChannels[(index + delta).mod(allChannels.size)])
    }

    fun togglePlay() {
        if (player.isPlaying) player.pause() else player.play()
        controlsTick += 1
    }

    fun goLive() {
        player.seekToDefaultPosition()
        player.play()
        banner = "Canlı yayına dönüldü"
    }

    fun castToTv() {
        val tv = activeTv
        if (tv == null) {
            banner = "Önce Kumanda sekmesinden bir TV ile eşleşin"
            return
        }
        if (state.lockedChannelIds.contains(channel.id)) {
            banner = "Kilitli kanal TV'ye gönderilemez"
            return
        }
        val source = channel.sources.getOrNull(sourceIndex) ?: channel.sources.firstOrNull() ?: return
        scope.launch {
            val ok = remoteHub.client(tv).cast(channel.name, source.url, channel.logoUrl)
            banner = if (ok) "TV'de açılıyor: ${tv.name}" else "TV'ye gönderilemedi"
        }
    }

    fun cycleAudio() {
        if (audioTracks.size <= 1) {
            banner = "Bu yayında tek ses izi var"
            return
        }
        val current = audioTracks.indexOfFirst { it.isSelected }.coerceAtLeast(0)
        val next = audioTracks[(current + 1) % audioTracks.size]
        val group = player.currentTracks.groups.getOrNull(next.groupIndex) ?: return
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, next.trackIndex))
            .build()
        banner = "Ses: ${next.label}"
    }

    fun cycleSubtitle() {
        if (subtitleTracks.isEmpty()) {
            banner = "Bu yayında altyazı yok"
            return
        }
        val current = subtitleTracks.indexOfFirst { it.isSelected }
        if (current == -1 || current + 1 < subtitleTracks.size) {
            val next = subtitleTracks[(current + 1).coerceAtLeast(0)]
            val group = player.currentTracks.groups.getOrNull(next.groupIndex) ?: return
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, next.trackIndex))
                .build()
            banner = "Altyazı: ${next.label}"
        } else {
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            banner = "Altyazı kapalı"
        }
    }

    val zapAction: (Int) -> Unit = { delta -> zap(delta) }
    val toggleAction: () -> Unit = { togglePlay() }
    val currentZap by rememberUpdatedState(zapAction)
    val currentToggle by rememberUpdatedState(toggleAction)
    val gesturesOn = prefs.gesturesEnabled && !inPip

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    keepScreenOn = true
                    this.player = player
                    resizeMode = scaleMode.resizeMode
                }
            },
            update = { view ->
                if (view.player !== player) view.player = player
                view.resizeMode = scaleMode.resizeMode
            },
            onRelease = { view -> view.player = null },
            modifier = Modifier.fillMaxSize()
        )

        // ------------------------------------------------------------ Dokunma ve hareket katmanı
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(gesturesOn) {
                    detectTapGestures(
                        onTap = {
                            controlsVisible = !controlsVisible
                            controlsTick += 1
                        },
                        onDoubleTap = { if (gesturesOn) currentToggle() }
                    )
                }
                .pointerInput(gesturesOn) {
                    if (!gesturesOn) return@pointerInput
                    var mode = 0 // 0: yok, 1: parlaklık, 2: ses, 3: yatay (kanal)
                    var startX = 0f
                    var accumX = 0f
                    var accumY = 0f
                    var baseBrightness = 0.5f
                    var baseVolume = 0
                    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                    detectDragGestures(
                        onDragStart = { offset ->
                            mode = 0
                            startX = offset.x
                            accumX = 0f
                            accumY = 0f
                        },
                        onDragEnd = {
                            if (mode == 3 && abs(accumX) > size.width * 0.18f) {
                                if (accumX < 0f) {
                                    hud = "Sonraki kanal"
                                    currentZap(+1)
                                } else {
                                    hud = "Önceki kanal"
                                    currentZap(-1)
                                }
                            }
                            mode = 0
                        },
                        onDragCancel = { mode = 0 },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            accumX += dragAmount.x
                            accumY += dragAmount.y
                            if (mode == 0 && (abs(accumX) > 28f || abs(accumY) > 28f)) {
                                if (abs(accumX) > abs(accumY)) {
                                    mode = 3
                                } else {
                                    mode = if (startX < size.width / 2f) 1 else 2
                                    baseBrightness = host.currentScreenBrightness()
                                    baseVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    accumY = 0f
                                }
                            }
                            if (mode == 1) {
                                val level = (baseBrightness - accumY / size.height * 1.4f).coerceIn(0.02f, 1f)
                                host.setScreenBrightness(level)
                                hud = "Parlaklık %${(level * 100f).roundToInt()}"
                            } else if (mode == 2) {
                                val target = (baseVolume - accumY / size.height * maxVolume * 1.4f)
                                    .roundToInt().coerceIn(0, maxVolume)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
                                hud = "Ses %${(target * 100f / maxVolume).roundToInt()}"
                            }
                        }
                    )
                }
        )

        // ------------------------------------------------------------ Durum göstergeleri
        if (buffering && errorText == null) {
            CircularProgressIndicator(
                color = RedoXColors.Primary,
                modifier = Modifier.align(Alignment.Center).size(46.dp)
            )
        }

        errorText?.let { message ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.78f))
                    .padding(20.dp)
            ) {
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.padding(top = 12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (channel.sources.size > 1) {
                        MGhostButton(
                            text = "Alternatif kaynak",
                            onClick = {
                                autoTried = 0
                                errorText = null
                                sourceIndex = (sourceIndex + 1) % channel.sources.size
                            }
                        )
                    }
                    MGhostButton(
                        text = "Tekrar dene",
                        onClick = {
                            errorText = null
                            PlaybackHub.play(
                                context, channel, sourceIndex,
                                effectiveQuality(context, prefs), prefs.nightMode, force = true
                            )
                        }
                    )
                }
            }
        }

        hud?.let { text ->
            Text(
                text = text,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            )
        }

        banner?.let { text ->
            Text(
                text = text,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .systemBarsPadding()
                    .padding(top = 56.dp, start = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(RedoXColors.SurfaceHigh.copy(alpha = 0.92f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // ------------------------------------------------------------ Kontroller
        AnimatedVisibility(
            visible = controlsVisible && !inPip,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Üst çubuk
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.78f), Color.Transparent)))
                        .systemBarsPadding()
                        .displayCutoutPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    IconButton(onClick = onMinimize) {
                        Icon(Icons.Rounded.ExpandMore, contentDescription = "Küçült", tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${channel.number}. ${channel.name}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = channel.nowNextLine(),
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (activeTv != null) {
                        IconButton(onClick = { castToTv() }) {
                            Icon(Icons.Rounded.Cast, contentDescription = "TV'de oynat", tint = Color.White)
                        }
                    }
                    IconButton(onClick = { host.enterPip() }) {
                        Icon(Icons.Rounded.PictureInPictureAlt, contentDescription = "Küçük pencere", tint = Color.White)
                    }
                    IconButton(onClick = { panel = MPlayerPanel.SETTINGS }) {
                        Icon(Icons.Rounded.Tune, contentDescription = "Oynatma ayarları", tint = Color.White)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, contentDescription = "Kapat", tint = Color.White)
                    }
                }

                // Orta düğmeler
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isTablet) 40.dp else 28.dp),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    IconButton(onClick = { zap(-1) }, modifier = Modifier.size(54.dp)) {
                        Icon(Icons.Rounded.SkipPrevious, contentDescription = "Önceki kanal", tint = Color.White, modifier = Modifier.size(34.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(if (isTablet) 84.dp else 72.dp)
                            .clip(CircleShape)
                            .background(RedoXColors.PrimaryBrush)
                            .pointerInput(Unit) { detectTapGestures(onTap = { togglePlay() }) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Duraklat" else "Oynat",
                            tint = Color.White,
                            modifier = Modifier.size(if (isTablet) 48.dp else 42.dp)
                        )
                    }
                    IconButton(onClick = { zap(+1) }, modifier = Modifier.size(54.dp)) {
                        Icon(Icons.Rounded.SkipNext, contentDescription = "Sonraki kanal", tint = Color.White, modifier = Modifier.size(34.dp))
                    }
                }

                // Alt çubuk
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))))
                        .systemBarsPadding()
                        .displayCutoutPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    if (channel.hasRealEpg) {
                        MProgressBar(
                            progress = channel.progressForUi(),
                            color = RedoXColors.Primary,
                            height = 4.dp
                        )
                        Spacer(modifier = Modifier.padding(top = 6.dp))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MLiveBadge()
                        Text(
                            text = channel.nowTitleOrLive(),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = effectiveQuality(context, prefs).label,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.padding(top = 8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            MChip(
                                text = "Kanallar",
                                selected = false,
                                onClick = { panel = MPlayerPanel.CHANNELS },
                                icon = Icons.Rounded.LiveTv
                            )
                        }
                        item {
                            val fav = state.favoriteIds.contains(channel.id)
                            MChip(
                                text = if (fav) "Favoride" else "Favori",
                                selected = fav,
                                onClick = { viewModel.toggleFavorite(channel.id) },
                                icon = if (fav) Icons.Rounded.Star else Icons.Rounded.StarBorder
                            )
                        }
                        item {
                            MChip(
                                text = "Canlıya dön",
                                selected = false,
                                onClick = { goLive() },
                                icon = Icons.Rounded.FiberManualRecord
                            )
                        }
                        if (channel.sources.size > 1) {
                            item {
                                MChip(
                                    text = "Akış ${sourceIndex + 1}/${channel.sources.size}",
                                    selected = false,
                                    onClick = {
                                        autoTried = 0
                                        sourceIndex = (sourceIndex + 1) % channel.sources.size
                                    }
                                )
                            }
                        }
                        item {
                            val remaining = state.sleepTimerRemainingMinutes
                            MChip(
                                text = if (remaining != null) "Uyku: $remaining dk" else "Uyku zamanlayıcı",
                                selected = remaining != null,
                                onClick = { viewModel.cycleSleepTimer() },
                                icon = Icons.Rounded.Bedtime
                            )
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ Ayarlar sayfası
    if (panel == MPlayerPanel.SETTINGS) {
        ModalBottomSheet(
            onDismissRequest = { panel = MPlayerPanel.NONE },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = RedoXColors.SurfaceHigh,
            contentColor = RedoXColors.TextPrimary
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text("Oynatma ayarları", color = RedoXColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)

                Text("Görüntü kalitesi", color = RedoXColors.TextSecondary, fontSize = 12.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PlayerQuality.entries.toList(), key = { "pq_${it.name}" }) { q ->
                        MChip(
                            text = q.label,
                            selected = q == prefs.defaultQuality,
                            onClick = { viewModel.features.updatePrefs { it.copy(defaultQuality = q) } }
                        )
                    }
                }
                if (prefs.mobileDataSaver && isOnCellular(context)) {
                    Text(
                        "Mobil veri algılandı: veri tasarrufu açık olduğu için kalite otomatik düşürüldü.",
                        color = RedoXColors.Gold,
                        fontSize = 12.sp
                    )
                }

                if (channel.sources.size > 1) {
                    Text("Akış kaynağı", color = RedoXColors.TextSecondary, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(channel.sources) { index, source ->
                            MChip(
                                text = "${index + 1}. ${source.label}",
                                selected = index == sourceIndex,
                                onClick = {
                                    autoTried = 0
                                    sourceIndex = index
                                }
                            )
                        }
                    }
                }

                Text("Görüntü oranı", color = RedoXColors.TextSecondary, fontSize = 12.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PlayerScaleMode.entries.toList(), key = { "sm_${it.name}" }) { mode ->
                        MChip(
                            text = mode.label,
                            selected = mode == scaleMode,
                            onClick = {
                                scaleMode = mode
                                viewModel.saveChannelPreferredScale(channel.id, mode.name)
                            }
                        )
                    }
                }

                MSettingSwitch(
                    title = "Gece modu",
                    subtitle = "Konuşmaları belirginleştirir, ani yüksek sesleri baskılar.",
                    checked = prefs.nightMode,
                    onCheckedChange = { v -> viewModel.features.updatePrefs { it.copy(nightMode = v) } }
                )
                MSettingSwitch(
                    title = "Arka planda ses",
                    subtitle = "Uygulamadan çıkınca ses devam eder.",
                    checked = prefs.backgroundAudio,
                    onCheckedChange = { v -> viewModel.features.updatePrefs { it.copy(backgroundAudio = v) } }
                )
                MSettingSwitch(
                    title = "Dokunma hareketleri",
                    subtitle = "Parlaklık, ses ve kanal değiştirme kaydırmaları.",
                    checked = prefs.gesturesEnabled,
                    onCheckedChange = { v -> viewModel.features.updatePrefs { it.copy(gesturesEnabled = v) } }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MGhostButton(
                        text = audioTracks.firstOrNull { it.isSelected }?.let { "Ses: ${it.label}" } ?: "Ses izi",
                        onClick = { cycleAudio() }
                    )
                    MGhostButton(
                        text = if (subtitleTracks.isEmpty()) "Altyazı yok" else "Altyazı",
                        onClick = { cycleSubtitle() }
                    )
                }

                // Gerçek (ölçülen) yayın bilgileri; tahmin yoktur
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(12.dp)
                ) {
                    Text("Yayın bilgisi (ölçülen)", color = RedoXColors.TextSecondary, fontSize = 11.sp)
                    Text(
                        text = buildString {
                            append(if (stats.height > 0) "${stats.resolutionLabel} (${stats.width}×${stats.height})" else "Çözünürlük: —")
                            if (stats.codec.isNotBlank()) append(" • ${stats.codec}")
                        },
                        color = RedoXColors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = buildString {
                            append(if (stats.videoBitrateKbps > 0) String.format(Locale.US, "Video %.1f Mbps", stats.videoBitrateKbps / 1000f) else "Video bit hızı: —")
                            append(if (stats.bandwidthKbps > 0) String.format(Locale.US, " • Ağ ~%.1f Mbps", stats.bandwidthKbps / 1000f) else " • Ağ: —")
                        },
                        color = RedoXColors.TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = String.format(Locale.US, "Tampon %.1f sn • %d kare düştü", stats.bufferedSeconds, stats.droppedFrames),
                        color = RedoXColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.padding(bottom = 8.dp))
            }
        }
    }

    // ------------------------------------------------------------------ Kanal listesi sayfası
    if (panel == MPlayerPanel.CHANNELS) {
        var filter by remember { mutableStateOf("") }
        val list = remember(filter, allChannels) {
            val q = filter.trim()
            (if (q.isEmpty()) allChannels
            else allChannels.filter { it.name.contains(q, ignoreCase = true) || it.number.toString() == q })
                .distinctBy { it.id }
        }
        ModalBottomSheet(
            onDismissRequest = { panel = MPlayerPanel.NONE },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = RedoXColors.SurfaceHigh,
            contentColor = RedoXColors.TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
            ) {
                MTextField(value = filter, onValueChange = { filter = it }, label = "Kanal ara")
                Spacer(modifier = Modifier.padding(top = 8.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                ) {
                    items(list, key = { "pl_${it.id}" }) { item ->
                        MChannelRow(
                            channel = item,
                            isFavorite = state.favoriteIds.contains(item.id),
                            isLocked = state.lockedChannelIds.contains(item.id),
                            isBroken = state.brokenChannelIds.contains(item.id) || item.isBroken,
                            isPlaying = item.id == channel.id,
                            onClick = {
                                panel = MPlayerPanel.NONE
                                viewModel.requestPlayChannel(item)
                            },
                            onLongClick = {}
                        )
                    }
                }
            }
        }
    }
}

/** Küçültülmüş oynatıcı: ses (veya arka plan sesi) sürerken ekranın altında gösterilen çubuk. */
@Composable
fun MobileMiniPlayer(
    channel: TvChannel,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val player = remember { PlaybackHub.player(context) }
    var playing by remember { mutableStateOf(player.isPlaying) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    val shape = RoundedCornerShape(16.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clip(shape)
            .background(RedoXColors.SurfaceHigh)
            .pointerInput(Unit) { detectTapGestures(onTap = { onExpand() }) }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        ChannelBrandLogoBox(channel = channel, boxSize = 40.dp, fontSize = 10.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel.name,
                color = RedoXColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = channel.nowTitleOrLive(),
                color = RedoXColors.TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = { if (player.isPlaying) player.pause() else player.play() }) {
            Icon(
                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (playing) "Duraklat" else "Oynat",
                tint = RedoXColors.TextPrimary
            )
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Rounded.Close, contentDescription = "Kapat", tint = RedoXColors.TextSecondary)
        }
    }
}
