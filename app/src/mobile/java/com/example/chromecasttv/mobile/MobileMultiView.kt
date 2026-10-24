package com.example.chromecasttv

import android.view.LayoutInflater
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

/** Bölünmüş ekran: telefonda 2'li (üst üste) veya 4'lü, tablette 2x2 / yan yana. Ses yalnızca seçili ekrandadır. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun MobileMultiViewScreen(
    viewModel: TvViewModel,
    state: TvUiState,
    isTablet: Boolean,
    nav: MobileNav
) {
    // Tam ekran/mini oynatıcı açıksa kapat: aynı anda iki yayının sesi çıkmasın
    LaunchedEffect(Unit) {
        if (state.pipChannel != null) viewModel.closePip()
    }

    val pool = remember(state.builtInChannels, state.customM3uChannels) {
        (state.builtInChannels + state.customM3uChannels).ifEmpty { state.builtInChannels }
    }
    if (pool.isEmpty()) {
        MEmptyState(
            icon = RedoXIcons.GridView,
            title = "Kanal yok",
            message = "Bölünmüş ekran için önce kanal listesi yüklenmeli."
        )
        return
    }

    val four = state.isMultiViewFourGrid
    val slotCount = if (four) 4 else 2
    val slots = remember(state.multiViewChannels, pool, slotCount) {
        val base = state.multiViewChannels.ifEmpty { pool.take(4) }
        List(slotCount) { idx -> base.getOrElse(idx) { pool[idx % pool.size] } }
    }
    var audioSlot by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        ) {
            Text(
                text = "Ses için ekrana dokunun",
                color = RedoXColors.TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            MChip(
                text = if (four) "4'lü (2x2)" else "2'li",
                selected = true,
                onClick = { viewModel.toggleMultiViewMode() },
                icon = RedoXIcons.GridView
            )
        }

        val cell: @Composable (Int, Modifier) -> Unit = { idx, mod ->
            MMultiCell(
                slotIndex = idx,
                channel = slots[idx],
                audioActive = audioSlot == idx,
                preferredSourceIndex = state.preferredSources[slots[idx].id] ?: 0,
                onSelectAudio = { audioSlot = idx },
                onZap = { delta -> viewModel.changeMultiViewSlotChannel(idx, delta) },
                onExpand = { viewModel.requestPlayChannel(slots[idx]) },
                modifier = mod
            )
        }

        if (four) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f).fillMaxWidth()) {
                    cell(0, Modifier.weight(1f).fillMaxSize())
                    cell(1, Modifier.weight(1f).fillMaxSize())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f).fillMaxWidth()) {
                    cell(2, Modifier.weight(1f).fillMaxSize())
                    cell(3, Modifier.weight(1f).fillMaxSize())
                }
            }
        } else if (isTablet) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                cell(0, Modifier.weight(1f).fillMaxSize())
                cell(1, Modifier.weight(1f).fillMaxSize())
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                cell(0, Modifier.weight(1f).fillMaxWidth())
                cell(1, Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun MMultiCell(
    slotIndex: Int,
    channel: TvChannel,
    audioActive: Boolean,
    preferredSourceIndex: Int,
    onSelectAudio: () -> Unit,
    onZap: (Int) -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var sourceIndex by remember(channel.id) {
        mutableIntStateOf(preferredSourceIndex.coerceIn(0, (channel.sources.size - 1).coerceAtLeast(0)))
    }
    var autoTried by remember(channel.id) { mutableIntStateOf(0) }
    var buffering by remember(channel.id, sourceIndex) { mutableStateOf(true) }
    var failed by remember(channel.id) { mutableStateOf(false) }

    val source = remember(channel.id, sourceIndex) {
        channel.sources.getOrElse(sourceIndex) {
            channel.sources.firstOrNull() ?: StreamSource("Canlı", channel.primaryStreamUrl, StreamType.HLS)
        }
    }

    val exoPlayer = remember {
        createTvExoPlayer(context, forceMaxBitrate = false, isMultiView = true).apply {
            playWhenReady = true
            volume = if (audioActive) 1f else 0f
        }
    }

    LaunchedEffect(audioActive) {
        exoPlayer.volume = if (audioActive) 1f else 0f
    }

    DisposableEffect(exoPlayer, channel.id, sourceIndex) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    autoTried = 0
                    failed = false
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (channel.sources.size > 1 && autoTried + 1 < channel.sources.size) {
                    autoTried += 1
                    sourceIndex = (sourceIndex + 1) % channel.sources.size
                } else {
                    buffering = false
                    failed = true
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(channel.id, sourceIndex) {
        buffering = true
        failed = false
        if (source.url.isNotBlank()) {
            exoPlayer.stop()
            exoPlayer.setMediaSource(buildStreamMediaSource(context, source))
            exoPlayer.prepare()
            exoPlayer.play()
        }
        delay(3000L)
        if (buffering && exoPlayer.playbackState != Player.STATE_READY &&
            channel.sources.size > 1 && autoTried + 1 < channel.sources.size
        ) {
            autoTried += 1
            sourceIndex = (sourceIndex + 1) % channel.sources.size
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.Black)
            .border(
                width = if (audioActive) 2.dp else 1.dp,
                color = if (audioActive) RedoXColors.Primary else Color.White.copy(alpha = 0.16f),
                shape = shape
            )
            .clickable { onSelectAudio() }
    ) {
        AndroidView(
            factory = { ctx ->
                (LayoutInflater.from(ctx).inflate(R.layout.multiview_player_view, null, false) as PlayerView).apply {
                    player = exoPlayer
                    useController = false
                    keepScreenOn = true
                    isFocusable = false
                    isClickable = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            update = { view ->
                if (view.player !== exoPlayer) view.player = exoPlayer
            },
            modifier = Modifier.fillMaxSize()
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${slotIndex + 1}. ${channel.name}",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (audioActive) RedoXIcons.VolumeUp else RedoXIcons.VolumeOff,
                contentDescription = if (audioActive) "Ses açık" else "Sessiz",
                tint = if (audioActive) RedoXColors.Mint else RedoXColors.TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        if (buffering || failed) {
            Text(
                text = if (failed) "Yayın açılamadı" else "Bağlanıyor…",
                color = if (failed) RedoXColors.Danger else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.55f))
        ) {
            IconButton(onClick = { onZap(-1) }, modifier = Modifier.size(36.dp)) {
                Icon(RedoXIcons.SkipPrevious, contentDescription = "Önceki kanal", tint = Color.White)
            }
            IconButton(onClick = { onZap(+1) }, modifier = Modifier.size(36.dp)) {
                Icon(RedoXIcons.SkipNext, contentDescription = "Sonraki kanal", tint = Color.White)
            }
            IconButton(onClick = onExpand, modifier = Modifier.size(36.dp)) {
                Icon(RedoXIcons.Fullscreen, contentDescription = "Tam ekran", tint = Color.White)
            }
        }
    }
}
