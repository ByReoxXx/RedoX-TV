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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.ui.graphics.lerp
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

// ============================================================================
// KARŞILAMA EKRANI (LANDING PAGE) — "Kızıl Halka" tasarım dili
// Logo + hızlı eylemler + "devam et" rafı. Küçük ekranlarda (< 660 dp yükseklik) ölçüler küçülür.
// ============================================================================
@Composable
fun RedoXLandingScreen(
    state: TvUiState,
    shelves: List<ChannelShelf>,
    onStartLiveTv: () -> Unit,
    onResumeLastChannel: (TvChannel) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenMultiView: () -> Unit,
    onOpenM3uManager: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleAutoStart: () -> Unit = {}
) {
    val primaryFocusRequester = remember { FocusRequester() }
    val lastChannel = state.recentChannels.firstOrNull() ?: state.builtInChannels.firstOrNull()

    LaunchedEffect(Unit) {
        try { primaryFocusRequester.requestFocus() } catch (_: Exception) {}
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(RedoXColors.Background)
            .background(
                Brush.radialGradient(
                    colors = listOf(RedoXColors.Wine.copy(alpha = 0.72f), Color.Transparent),
                    radius = 1000f
                )
            )
            .padding(horizontal = 56.dp, vertical = 26.dp)
    ) {
        val compact = maxHeight < 660.dp
        val markSize = if (compact) 108.dp else 156.dp
        val wordWidth = if (compact) 232.dp else 300.dp

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(RedoXColors.Mint)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${state.builtInChannels.size + state.customM3uChannels.size} kanal hazır",
                        color = TvThemeColors.TextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TvCompactChipButton(
                        text = if (state.autoStartLastChannel) "Otomatik başlat: Açık" else "Otomatik başlat: Kapalı",
                        icon = RedoXIcons.Bolt,
                        onClick = onToggleAutoStart
                    )
                    TvCompactChipButton(
                        text = "Ayarlar",
                        icon = RedoXIcons.Settings,
                        onClick = onOpenSettings
                    )
                    Text(
                        text = "${BuildConfig.APP_DISPLAY_NAME} • v${BuildConfig.VERSION_NAME}",
                        color = RedoXColors.PrimaryBright,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(markSize)) {
                    Box(
                        modifier = Modifier
                            .requiredSize(markSize * 1.9f)
                            .background(
                                Brush.radialGradient(
                                    listOf(RedoXColors.Gold.copy(alpha = 0.14f), Color.Transparent)
                                ),
                                CircleShape
                            )
                    )
                    RedoXMark(markSize = markSize)
                }
                Spacer(modifier = Modifier.height(4.dp))
                RedoXWordmarkImage(width = wordWidth)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "KESİNTİSİZ CANLI TELEVİZYON • IPTV OYNATICI",
                    color = TvThemeColors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.4.sp
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LandingActionCard(
                    title = "Canlı TV'ye Başla",
                    subtitle = "${state.builtInChannels.size} kanal",
                    icon = RedoXIcons.LiveTv,
                    accentColor = RedoXColors.Gold,
                    isPrimary = true,
                    onClick = onStartLiveTv,
                    compact = compact,
                    modifier = Modifier.focusRequester(primaryFocusRequester)
                )

                if (lastChannel != null) {
                    LandingActionCard(
                        title = "Hemen İzle",
                        subtitle = "${lastChannel.name} • CH ${lastChannel.number}",
                        icon = RedoXIcons.PlayCircleFilled,
                        accentColor = RedoXColors.Gold,
                        isPrimary = false,
                        onClick = { onResumeLastChannel(lastChannel) },
                        compact = compact
                    )
                }

                LandingActionCard(
                    title = "Favori Kanallarım",
                    subtitle = "${state.favoriteOrderList.size} kanal",
                    icon = RedoXIcons.Star,
                    accentColor = RedoXColors.Gold,
                    isPrimary = false,
                    onClick = onOpenFavorites,
                    compact = compact
                )

                LandingActionCard(
                    title = "Bölünmüş Ekran",
                    subtitle = "4 kanalı birlikte izle",
                    icon = RedoXIcons.GridView,
                    accentColor = RedoXColors.Gold,
                    isPrimary = false,
                    onClick = onOpenMultiView,
                    compact = compact
                )

                LandingActionCard(
                    title = "M3U Yükle",
                    subtitle = "${state.customM3uChannels.size} özel kanal",
                    icon = RedoXIcons.UploadFile,
                    accentColor = RedoXColors.Gold,
                    isPrimary = false,
                    onClick = onOpenM3uManager,
                    compact = compact
                )
            }

            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val shelfTitle = shelves.firstOrNull()?.title ?: "Hızlı kanal başlat"
                val shelfChannels = (shelves.firstOrNull()?.channels ?: state.builtInChannels)
                    .distinctBy { it.id }
                    .take(10)
                Text(
                    text = shelfTitle,
                    color = TvThemeColors.TextSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(shelfChannels, key = { "landing_shelf_${it.id}" }) { ch ->
                        LandingShelfCard(channel = ch, onClick = { onResumeLastChannel(ch) })
                    }
                }
            }
        }
    }
}

/** Karşılama ekranındaki raf kartı: logo + kanal adı + şu an yayındaki program (+ ilerleme). */
@Composable
fun LandingShelfCard(
    channel: TvChannel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .width(300.dp)
            .height(72.dp)
            .tvSurface(focused = isFocused, shape = shape, ringWidth = 2.5.dp)
            .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
            .clickable { onClick() }
            .padding(horizontal = 12.dp)
    ) {
        ChannelBrandLogoBox(channel = channel, boxSize = 46.dp, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = channel.name,
                color = TvThemeColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = channel.nowTitleOrLive(),
                color = TvThemeColors.TextSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (channel.hasRealEpg) {
                TvProgressBar(progress = channel.progressForUi())
            }
        }
    }
}

@Composable
fun LandingActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        animationSpec = tween(durationMillis = 140),
        label = "landing_card_scale"
    )
    val shape = RoundedCornerShape(18.dp)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .width(if (compact) 170.dp else 196.dp)
            .height(if (compact) 120.dp else 140.dp)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .tvSurface(
                focused = isFocused,
                shape = shape,
                brush = if (isPrimary) RedoXColors.PrimaryBrush else null,
                ringWidth = 3.dp
            )
            .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 40.dp else 48.dp)
                .clip(CircleShape)
                .background(if (isPrimary) Color.White.copy(alpha = 0.2f) else accentColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isPrimary) Color.White else accentColor,
                modifier = Modifier.size(if (compact) 22.dp else 26.dp)
            )
        }
        Spacer(modifier = Modifier.height(if (compact) 6.dp else 9.dp))
        Text(
            text = title,
            color = if (isPrimary) Color.White else TvThemeColors.TextPrimary,
            fontSize = if (compact) 14.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            color = if (isPrimary) Color(0xFFFFE3E8) else TvThemeColors.TextSecondary,
            fontSize = if (compact) 12.sp else 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RedoXTvRootScreen(
    viewModel: TvViewModel,
    host: RedoXHost
) {
    val state by viewModel.uiState.collectAsState()
    val features by viewModel.features.state.collectAsState()
    val shelves = remember(state, features) { ShelfBuilder.build(state, features) }

    var showBuiltInFilePicker by remember { mutableStateOf(false) }
    var editingChannel by remember { mutableStateOf<TvChannel?>(null) }
    var lastWatchedChannelId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.activePlayingChannel?.id) {
        state.activePlayingChannel?.id?.let { lastWatchedChannelId = it }
    }

    // Kumanda: yalnızca yön tuşları, OK ve Geri olan kumandalarda (Chromecast) Geri bir üst düzeye çıkar:
    // içerik -> yan menü -> (uyarı görünürken ikinci basış) uygulamadan çık.
    val railFocusRequester = remember { FocusRequester() }
    var railHasFocus by remember { mutableStateOf(false) }
    var exitArmed by remember { mutableStateOf(false) }
    val onMainScreen = state.activePlayingChannel == null && !state.showLandingScreen
    BackHandler(enabled = onMainScreen && !railHasFocus) {
        try { railFocusRequester.requestFocus() } catch (_: Exception) {}
    }
    BackHandler(enabled = onMainScreen && railHasFocus && !exitArmed) {
        exitArmed = true
        viewModel.showStatusMessage("Çıkmak için Geri'ye tekrar basın")
    }
    LaunchedEffect(exitArmed) {
        if (exitArmed) {
            delay(3400L)
            exitArmed = false
        }
    }
    LaunchedEffect(onMainScreen) {
        if (!onMainScreen) {
            railHasFocus = false
            exitArmed = false
        }
    }

    if (state.activePlayingChannel != null) {
        RedoXPlayerScreen(
            playingChannel = state.activePlayingChannel!!,
            previousChannel = state.previousPlayingChannel,
            allChannels = (state.builtInChannels + state.customM3uChannels).ifEmpty { state.builtInChannels },
            favoriteIds = state.favoriteIds,
            lockedIds = state.lockedChannelIds,
            brokenIds = state.brokenChannelIds,
            preferredSources = state.preferredSources,
            preferredScales = state.preferredScales,
            epgMap = state.epgMap,
            sleepTimerRemaining = state.sleepTimerRemainingMinutes,
            onZapChannel = { delta -> viewModel.zapChannel(delta) },
            onRecallPreviousChannel = { viewModel.recallPreviousChannel() },
            onSelectChannelDirect = { ch -> viewModel.requestPlayChannel(ch) },
            onSelectChannelByNumber = { num -> viewModel.playChannelByNumber(num) },
            onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
            onToggleLock = { id -> viewModel.toggleChannelLock(id) },
            onHideChannel = { id -> viewModel.hideChannel(id) },
            onSavePreferredSource = { id, idx -> viewModel.saveChannelPreferredSource(id, idx) },
            onSavePreferredScale = { id, scale -> viewModel.saveChannelPreferredScale(id, scale) },
            onMarkBroken = { id -> viewModel.markChannelAsBroken(id) },
            onClearBroken = { id -> viewModel.clearChannelBrokenStatus(id) },
            onMinimizeToPip = { viewModel.minimizeToPip() },
            onEnterSystemPip = { host.enterPip() },
            onOpenMultiView = { viewModel.openMultiViewFromPlayer() },
            onCycleSleepTimer = { viewModel.cycleSleepTimer() },
            onExitPlayer = { viewModel.closePlayer() },
            featurePrefs = features.prefs,
            onUpdatePrefs = { transform -> viewModel.features.updatePrefs(transform) },
            statusMessage = state.statusMessage,
            onClearStatus = { viewModel.clearStatusMessage() }
        )
        return
    }

    if (state.showLandingScreen) {
        RedoXLandingScreen(
            state = state,
            shelves = shelves,
            onStartLiveTv = { viewModel.dismissLandingAndOpenTab(MainNavTab.ALL_CHANNELS) },
            onResumeLastChannel = { ch -> viewModel.requestPlayChannel(ch) },
            onOpenFavorites = { viewModel.dismissLandingAndOpenTab(MainNavTab.FAVORITES) },
            onOpenMultiView = { viewModel.dismissLandingAndOpenTab(MainNavTab.MULTI_VIEW) },
            onOpenM3uManager = { viewModel.dismissLandingAndOpenTab(MainNavTab.M3U_MANAGER) },
            onOpenSettings = { viewModel.dismissLandingAndOpenTab(MainNavTab.SETTINGS) },
            onToggleAutoStart = { viewModel.toggleAutoStartLastChannel() }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvThemeColors.BackgroundDeep)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            TvSideNavigationRail(
                selectedTab = state.selectedTab,
                builtInCount = state.builtInChannels.size,
                customM3uCount = state.customM3uChannels.size,
                favoriteCount = state.favoriteOrderList.size,
                sleepTimerMinutes = state.sleepTimerRemainingMinutes,
                onSelectTab = { viewModel.selectTab(it) },
                onOpenLanding = { viewModel.openLandingScreen() },
                onCycleSleepTimer = { viewModel.cycleSleepTimer() },
                selectedItemFocusRequester = railFocusRequester,
                onFocusInside = { railHasFocus = it }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 36.dp, vertical = 20.dp)
            ) {
                TvTopStatusHeader(
                    state = state,
                    onQuickHealthCheckClick = { viewModel.runChannelHealthCheck() },
                    onQuickMultiViewClick = { viewModel.selectTab(MainNavTab.MULTI_VIEW) },
                    onQuickSearchClick = { viewModel.selectTab(MainNavTab.SEARCH) },
                    onQuickAddM3uClick = { viewModel.selectTab(MainNavTab.M3U_MANAGER) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Sekme geçişi anında yapılır (çapraz solma yok): zayıf TV işlemcilerinde takılmayı önler
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val targetTab = state.selectedTab
                    when (targetTab) {
                        MainNavTab.ALL_CHANNELS -> {
                            ChannelShowcaseScreen(
                                title = "RedoX Yerleşik Canlı TV",
                                channels = state.builtInChannels,
                                categories = state.categories,
                                selectedCategory = state.selectedCategory,
                                favoriteIds = state.favoriteIds,
                                lockedIds = state.lockedChannelIds,
                                brokenIds = state.brokenChannelIds,
                                healthyIds = state.healthyChannelIds,
                                hiddenCount = state.hiddenChannelIds.size,
                                recentChannels = state.recentChannels,
                                lastWatchedChannelId = lastWatchedChannelId,
                                onSelectCategory = { viewModel.selectCategory(it) },
                                onPlayChannel = { viewModel.requestPlayChannel(it) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onToggleLock = { viewModel.toggleChannelLock(it) },
                                onHideChannel = { viewModel.hideChannel(it) },
                                onRestoreHidden = { viewModel.restoreAllHiddenChannels() },
                                onEditChannel = { editingChannel = it }
                            )
                        }
                        MainNavTab.FAVORITES -> {
                            FavoritesReorderScreen(
                                favoriteChannels = state.orderedFavoriteChannels,
                                lockedIds = state.lockedChannelIds,
                                brokenIds = state.brokenChannelIds,
                                onPlayChannel = { viewModel.requestPlayChannel(it) },
                                onMoveChannel = { id, delta -> viewModel.moveFavoriteChannel(id, delta) },
                                onSortFavorites = { mode -> viewModel.sortFavoritesBy(mode) },
                                onRemoveFavorite = { id -> viewModel.toggleFavorite(id) },
                                listNames = features.favoriteListNames,
                                activeListName = features.activeFavoriteList,
                                onSwitchList = { name -> viewModel.features.switchFavoriteList(name) },
                                onCreateList = { name -> viewModel.features.createFavoriteList(name) },
                                onRenameList = { old, new -> viewModel.features.renameFavoriteList(old, new) },
                                onDeleteList = { name -> viewModel.features.deleteFavoriteList(name) }
                            )
                        }
                        MainNavTab.CUSTOM_M3U -> {
                            // Eklenen her liste, verilen adla bir klasör olarak görünür (OK: aç, Geri: klasörlere dön)
                            CustomM3uFoldersScreen(
                                channels = state.customM3uChannels,
                                playlists = state.playlists,
                                selectedCategory = state.selectedCategory,
                                favoriteIds = state.favoriteIds,
                                lockedIds = state.lockedChannelIds,
                                brokenIds = state.brokenChannelIds,
                                healthyIds = state.healthyChannelIds,
                                hiddenCount = state.hiddenChannelIds.size,
                                lastWatchedChannelId = lastWatchedChannelId,
                                onSelectCategory = { viewModel.selectCategory(it) },
                                onPlayChannel = { viewModel.requestPlayChannel(it) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onToggleLock = { viewModel.toggleChannelLock(it) },
                                onHideChannel = { viewModel.hideChannel(it) },
                                onRestoreHidden = { viewModel.restoreAllHiddenChannels() },
                                onEditChannel = { editingChannel = it },
                                emptyMessage = "Henüz özel bir M3U listesi eklemediniz. 'M3U ve Dosya' sekmesinden dosya veya link ekleyebilirsiniz."
                            )
                        }
                        MainNavTab.MULTI_VIEW -> {
                            MultiViewSplitScreen(
                                state = state,
                                onToggleGridMode = { viewModel.toggleMultiViewMode() },
                                onChangeSlotChannel = { slot, delta ->
                                    viewModel.changeMultiViewSlotChannel(slot, delta)
                                },
                                onExpandSlotToFull = { ch -> viewModel.requestPlayChannel(ch) }
                            )
                        }
                        MainNavTab.EPG_GUIDE -> {
                            EpgTimelineGuideScreen(
                                channels = state.builtInChannels + state.customM3uChannels,
                                epgMap = state.epgMap,
                                favoriteIds = state.favoriteIds,
                                epgStatus = features.epg,
                                reminders = features.reminders,
                                onPlayChannel = { viewModel.requestPlayChannel(it) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onToggleReminder = { channel, program -> viewModel.features.toggleReminder(channel, program) },
                                onRefreshEpg = { viewModel.features.refreshEpg(true) },
                                onOpenSettings = { viewModel.selectTab(MainNavTab.SETTINGS) }
                            )
                        }
                        MainNavTab.SEARCH -> {
                            ChannelSearchAndNumpadScreen(
                                allChannels = state.builtInChannels + state.customM3uChannels,
                                searchQuery = state.searchQuery,
                                favoriteIds = state.favoriteIds,
                                lockedIds = state.lockedChannelIds,
                                brokenIds = state.brokenChannelIds,
                                onQueryChange = { viewModel.updateSearchQuery(it) },
                                onPlayChannel = { viewModel.requestPlayChannel(it) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                epgMap = state.epgMap,
                                onVoiceSearch = { host.startVoiceSearch { text -> viewModel.updateSearchQuery(text) } }
                            )
                        }
                        MainNavTab.M3U_MANAGER -> {
                            M3uPlaylistManagerScreen(
                                state = state,
                                remoteHost = features.remoteHost,
                                onRegenerateRemotePin = { viewModel.regenerateRemotePin() },
                                onAddRemotePlaylist = { name, m3u, epg ->
                                    viewModel.addRemoteM3uPlaylist(name, m3u, epg)
                                },
                                onAddXtreamAccount = { name, srv, usr, pwd ->
                                    viewModel.addXtreamCodesAccount(name, srv, usr, pwd)
                                },
                                onPickLocalFile = {
                                    showBuiltInFilePicker = true
                                },
                                onScanDownloadFolder = {
                                    viewModel.scanAndImportDownloadFolderM3u()
                                },
                                onUpdatePin = { newPin -> viewModel.updateParentalPin(newPin) },
                                onToggleAutoStart = { viewModel.toggleAutoStartLastChannel() },
                                onBackupToDevice = { viewModel.backupSettingsToDevice() },
                                onRestoreFromDevice = { viewModel.restoreSettingsFromDevice() },
                                onRestoreHiddenChannels = { viewModel.restoreAllHiddenChannels() },
                                onRefreshQrServer = { viewModel.refreshWifiTransferServer() },
                                onRemovePlaylist = { id -> viewModel.removePlaylist(id) },
                                onRefreshAll = { viewModel.reloadAllChannelsAndEpg() }
                            )
                        }
                        MainNavTab.SETTINGS -> {
                            TvSettingsScreen(
                                viewModel = viewModel,
                                state = state,
                                features = features,
                                host = host
                            )
                        }
                    }
                }
            }
        }

        if (state.pipChannel != null && state.selectedTab != MainNavTab.MULTI_VIEW) {
            FloatingMiniPipPlayer(
                channel = state.pipChannel!!,
                onExpand = { viewModel.expandPipToFullScreen() },
                onClose = { viewModel.closePip() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
            )
        }

        if (state.pendingPinChannel != null) {
            ParentalPinDialog(
                channel = state.pendingPinChannel!!,
                onVerifyPin = { pin -> viewModel.verifyPinAndPlay(pin) },
                onDismiss = { viewModel.dismissPinDialog() }
            )
        }

        if (showBuiltInFilePicker) {
            TvBuiltInM3uFilePickerDialog(
                onScanM3uFiles = { viewModel.scanAllM3uFilesOnDevice() },
                onSelectFile = { file -> viewModel.importLocalM3uFile(file) },
                onDismiss = { showBuiltInFilePicker = false }
            )
        }

        editingChannel?.let { channel ->
            TvChannelEditDialog(
                channel = channel,
                hasOverride = features.channelOverrides.containsKey(channel.id),
                onSave = { name, number ->
                    viewModel.features.setChannelOverride(channel.id, name, number)
                    editingChannel = null
                },
                onReset = {
                    viewModel.features.clearChannelOverride(channel.id)
                    editingChannel = null
                },
                onDismiss = { editingChannel = null }
            )
        }

        state.statusMessage?.let { message ->
            var toastShown by remember(message) { mutableStateOf(false) }
            LaunchedEffect(message) {
                toastShown = true
                delay(3200)
                toastShown = false
                delay(300)
                viewModel.clearStatusMessage()
            }
            AnimatedVisibility(
                visible = toastShown,
                enter = fadeIn(tween(160)) + slideInVertically(tween(220)) { it / 2 },
                exit = fadeOut(tween(200)) + slideOutVertically(tween(220)) { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(TvThemeColors.SurfaceElevated)
                        .border(2.dp, RedoXColors.Gold, RoundedCornerShape(14.dp))
                        .padding(horizontal = 20.dp, vertical = 11.dp)
                ) {
                    Icon(
                        imageVector = RedoXIcons.NotificationsActive,
                        contentDescription = null,
                        tint = RedoXColors.Gold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = message,
                        color = TvThemeColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** Canlı TV kategori çiplerindeki sanal kategoriler (gerçek kategori adlarıyla çakışmasın diye özel anahtar). */
private const val TV_CAT_FAVORITES = "__redox_tv_favorites__"
private const val TV_CAT_RECENT = "__redox_tv_recent__"

/**
 * Uzun basışta OK tuşu hâlâ basılıdır: odak "İzle" düğmesine geçince tuş tekrarları ve bırakma olayı
 * düğmeye ulaşıp kanalı açmasın diye, tuş bırakılana kadar OK olaylarını tüketir.
 */
private class TvOkReleaseGate {
    var swallowUntilRelease = false

    /** true dönerse olay tüketilmiştir. */
    fun consume(event: KeyEvent): Boolean {
        if (!swallowUntilRelease) return false
        val code = event.keyCode
        if (code != KeyEvent.KEYCODE_DPAD_CENTER &&
            code != KeyEvent.KEYCODE_ENTER &&
            code != KeyEvent.KEYCODE_NUMPAD_ENTER
        ) return false
        return when (event.action) {
            KeyEvent.ACTION_UP -> {
                swallowUntilRelease = false
                true
            }
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) {
                    // Yeni bir basış: önceki bırakma olayı kaçmış demektir
                    swallowUntilRelease = false
                    false
                } else {
                    true
                }
            }
            else -> false
        }
    }
}

@Composable
fun ChannelShowcaseScreen(
    title: String,
    channels: List<TvChannel>,
    categories: List<String>,
    selectedCategory: String,
    favoriteIds: Set<String>,
    lockedIds: Set<String>,
    brokenIds: Set<String>,
    healthyIds: Set<String> = emptySet(),
    hiddenCount: Int = 0,
    recentChannels: List<TvChannel>,
    lastWatchedChannelId: String? = null,
    onSelectCategory: (String) -> Unit,
    onPlayChannel: (TvChannel) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onHideChannel: (String) -> Unit = {},
    onRestoreHidden: () -> Unit = {},
    onEditChannel: (TvChannel) -> Unit = {},
    emptyMessage: String = "Bu kategoride kanal bulunamadı.",
    /** Bir Özel M3U klasörünün içindeyken klasörün adı: ipucu çubuğunda gösterilir, Geri klasör listesine döner. */
    folderName: String? = null
) {
    // "Favoriler" ve "Son izlenenler" kategori çiplerinin arasında sanal kategorilerdir. Çip sayıları canlıdır;
    // Favoriler listesi seçildiği andaki görüntüdür (yıldızı kaldırınca kart listeden birden kaybolmaz).
    val favoriteCount = remember(channels, favoriteIds) { channels.count { favoriteIds.contains(it.id) } }
    val recentInList = remember(channels, recentChannels) {
        val ids = HashSet<String>(channels.size * 2)
        channels.forEach { ids.add(it.id) }
        recentChannels.filter { ids.contains(it.id) }
    }
    val chipCategories = remember(categories, favoriteCount, recentInList.size) {
        buildList {
            categories.firstOrNull()?.let { add(it) }
            if (favoriteCount > 0) add(TV_CAT_FAVORITES)
            if (recentInList.isNotEmpty()) add(TV_CAT_RECENT)
            addAll(categories.drop(1))
        }
    }
    val activeCategory = when {
        selectedCategory == TV_CAT_FAVORITES && favoriteCount == 0 -> "Tümü"
        selectedCategory == TV_CAT_RECENT && recentInList.isEmpty() -> "Tümü"
        else -> selectedCategory
    }
    val favoriteSnapshot = remember(activeCategory) { favoriteIds }
    val filtered = remember(channels, activeCategory, recentInList) {
        when (activeCategory) {
            "Tümü" -> channels
            TV_CAT_FAVORITES -> channels.filter { favoriteSnapshot.contains(it.id) }
            TV_CAT_RECENT -> recentInList
            else -> channels.filter { it.category.equals(activeCategory, ignoreCase = true) }
        }
    }

    // Odaktaki kanalın yalnızca kimliği tutulur; nesne her seferinde güncel listeden alınır. Böylece rehber
    // (EPG) yenilenip liste yeniden üretildiğinde üstteki panel başka bir kanala sıçramaz.
    var focusedChannelId by remember(activeCategory) {
        val initial = filtered.firstOrNull { it.id == lastWatchedChannelId } ?: filtered.firstOrNull()
        mutableStateOf(initial?.id)
    }
    val localFocusedChannel: TvChannel? =
        filtered.firstOrNull { it.id == focusedChannelId } ?: filtered.firstOrNull()

    val gridState = rememberLazyGridState()
    val cardFocusRequesters = remember { HashMap<String, FocusRequester>() }
    val scope = rememberCoroutineScope()

    // Kumanda (Chromecast: yalnızca yön tuşları, OK ve Geri): kanal kartında OK'a basılı tutmak odağı
    // üstteki afişte "İzle" düğmesine taşır; Favori / Kilit / Düzenle / Gizle düğmeleri hemen yanındadır.
    val watchButtonRequester = remember { FocusRequester() }
    var bannerHasFocus by remember { mutableStateOf(false) }
    var returnChannelId by remember { mutableStateOf<String?>(null) }
    val okGate = remember { TvOkReleaseGate() }
    // Geri: liste -> kategori çipleri -> yan menü. Listenin derinlerindeyken kategori değiştirmek için
    // yukarı yön tuşuna onlarca kez basmak gerekmesin.
    val chipsState = rememberLazyListState()
    val chipRequester = remember { FocusRequester() }
    var gridHasFocus by remember { mutableStateOf(false) }

    fun jumpToWatchButton(channel: TvChannel) {
        returnChannelId = channel.id
        okGate.swallowUntilRelease = true
        try {
            watchButtonRequester.requestFocus()
        } catch (_: Exception) {
            okGate.swallowUntilRelease = false
        }
    }

    // Geri: afişteyken uzun basışla çıkılan karta döner (sol menüye ya da uygulamadan çıkışa gitmez).
    BackHandler(enabled = bannerHasFocus) {
        val targetId = returnChannelId ?: localFocusedChannel?.id
        val idx = filtered.indexOfFirst { it.id == targetId }.coerceAtLeast(0)
        val target = filtered.getOrNull(idx) ?: return@BackHandler
        scope.launch {
            try {
                cardFocusRequesters[target.id]?.requestFocus()
            } catch (_: Exception) {
                try { gridState.scrollToItem(idx) } catch (_: Exception) {}
                delay(60L)
                try { cardFocusRequesters[target.id]?.requestFocus() } catch (_: Exception) {}
            }
        }
    }

    BackHandler(enabled = gridHasFocus && chipCategories.size > 1) {
        scope.launch {
            try {
                chipRequester.requestFocus()
            } catch (_: Exception) {
                // Seçili çip ekran dışında kaldıysa önce görünür kıl
                try { chipsState.scrollToItem(chipCategories.indexOf(activeCategory).coerceAtLeast(0)) } catch (_: Exception) {}
                delay(60L)
                try { chipRequester.requestFocus() } catch (_: Exception) {}
            }
        }
    }

    // Ekran açılınca ya da kanaldan çıkıp (Geri tuşu) listeye dönünce sol menü yerine doğrudan son izlenen
    // kanalın kartına kaydır ve kumanda odağını o kanalın üstüne ver. Kategori çipine basınca odak çipte kalır
    // (bir sonraki kategoriye geçmek ya da aşağı yönle listeye girmek tek tuş); liste yalnızca başa sarılır.
    var initialFocusDone by remember { mutableStateOf(false) }
    LaunchedEffect(lastWatchedChannelId, filtered.isNotEmpty()) {
        if (filtered.isNotEmpty() && !initialFocusDone) {
            val targetId = lastWatchedChannelId ?: localFocusedChannel?.id ?: filtered.first().id
            val targetIdx = filtered.indexOfFirst { it.id == targetId }.coerceAtLeast(0)
            try {
                gridState.scrollToItem(targetIdx)
            } catch (_: Exception) {}
            delay(60L)
            try {
                cardFocusRequesters[filtered[targetIdx].id]?.requestFocus()
            } catch (_: Exception) {}
            initialFocusDone = true
        }
    }
    LaunchedEffect(activeCategory) {
        if (initialFocusDone) {
            try { gridState.scrollToItem(0) } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { ev -> okGate.consume(ev.nativeKeyEvent) }
    ) {
        localFocusedChannel?.let { previewChannel ->
            FocusedChannelHeroBanner(
                channel = previewChannel,
                isFavorite = favoriteIds.contains(previewChannel.id),
                isLocked = lockedIds.contains(previewChannel.id),
                isBroken = brokenIds.contains(previewChannel.id) || previewChannel.isBroken,
                isHealthy = healthyIds.contains(previewChannel.id),
                hiddenCount = hiddenCount,
                onWatchClick = { onPlayChannel(previewChannel) },
                onToggleFavorite = { onToggleFavorite(previewChannel.id) },
                onToggleLock = { onToggleLock(previewChannel.id) },
                onHideChannel = { onHideChannel(previewChannel.id) },
                onRestoreHidden = onRestoreHidden,
                onEditChannel = { onEditChannel(previewChannel) },
                watchFocusRequester = watchButtonRequester,
                onFocusInside = { bannerHasFocus = it }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (chipCategories.size > 1) {
            LazyRow(
                state = chipsState,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                itemsIndexed(chipCategories, key = { idx, c -> "cat_${c}_$idx" }) { _, category ->
                    var isFocused by remember { mutableStateOf(false) }
                    val isSelected = activeCategory == category
                    val count = when (category) {
                        "Tümü" -> channels.size
                        TV_CAT_FAVORITES -> favoriteCount
                        TV_CAT_RECENT -> recentInList.size
                        else -> channels.count { it.category.equals(category, ignoreCase = true) }
                    }
                    val label = when (category) {
                        TV_CAT_FAVORITES -> "Favoriler"
                        TV_CAT_RECENT -> "Son izlenenler"
                        else -> category
                    }
                    val chipIcon: ImageVector? = when (category) {
                        TV_CAT_FAVORITES -> RedoXIcons.Star
                        TV_CAT_RECENT -> RedoXIcons.History
                        else -> null
                    }
                    val pillShape = RoundedCornerShape(50)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .height(40.dp)
                            .then(if (isSelected) Modifier.focusRequester(chipRequester) else Modifier)
                            .tvSurface(
                                focused = isFocused,
                                shape = pillShape,
                                color = if (isSelected) RedoXColors.Primary.copy(alpha = 0.18f) else RedoXColors.Surface,
                                idleBorder = if (isSelected) RedoXColors.Primary else RedoXColors.Line,
                                ringWidth = 2.5.dp
                            )
                            .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
                            .clickable { onSelectCategory(category) }
                            .padding(horizontal = 18.dp)
                    ) {
                        if (chipIcon != null) {
                            Icon(
                                imageVector = chipIcon,
                                contentDescription = null,
                                tint = RedoXColors.Gold,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                        }
                        Text(
                            text = label,
                            color = if (isSelected || isFocused) TvThemeColors.TextPrimary else TvThemeColors.TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 15.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = count.toString(),
                            color = RedoXColors.TextTertiary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(TvThemeColors.SurfaceCard),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emptyMessage,
                    color = TvThemeColors.TextSecondary,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = 200.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onFocusChanged { gridHasFocus = it.hasFocus }
            ) {
                itemsIndexed(filtered, key = { idx, ch -> "${ch.id}_$idx" }) { _, channel ->
                    val isBroken = brokenIds.contains(channel.id) || channel.isBroken
                    val requester = cardFocusRequesters.getOrPut(channel.id) { FocusRequester() }
                    TvChannelGridCard(
                        channel = channel,
                        isFavorite = favoriteIds.contains(channel.id),
                        isLocked = lockedIds.contains(channel.id),
                        isBroken = isBroken,
                        isHealthy = healthyIds.contains(channel.id),
                        focusRequester = requester,
                        onFocused = { focusedChannelId = channel.id },
                        onClick = { onPlayChannel(channel) },
                        onLongClick = { jumpToWatchButton(channel) }
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(26.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            ) {
                TvKeyHint(key = "OK", label = "İzle")
                TvKeyHint(key = "OK basılı tut", label = "Kanal özellikleri")
                TvKeyHint(key = "Geri", label = if (folderName != null) "Klasörler" else "Yan menü")
                if (folderName != null) {
                    Spacer(modifier = Modifier.weight(1f))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = RedoXIcons.FolderOpen,
                            contentDescription = null,
                            tint = RedoXColors.Gold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = folderName,
                            color = TvThemeColors.TextSecondary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 280.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FocusedChannelHeroBanner(
    channel: TvChannel,
    isFavorite: Boolean,
    isLocked: Boolean,
    isBroken: Boolean,
    isHealthy: Boolean = false,
    hiddenCount: Int = 0,
    onWatchClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleLock: () -> Unit,
    onHideChannel: () -> Unit = {},
    onRestoreHidden: () -> Unit = {},
    onEditChannel: () -> Unit = {},
    /** Kanal kartında OK'a basılı tutulunca odağın gideceği "İzle" düğmesi. */
    watchFocusRequester: FocusRequester? = null,
    /** Afişteki herhangi bir düğme odağı aldığında / bıraktığında çağrılır. */
    onFocusInside: (Boolean) -> Unit = {}
) {
    val shape = RoundedCornerShape(22.dp)
    // Odaktaki kanalın marka rengi, logonun arkasında hafif ve sabit bir ışıma olarak görünür
    val glow = Color(channel.brandPrimaryColor).copy(alpha = 0.18f)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { onFocusInside(it.hasFocus) }
    ) {
        // Dar alanda (≈ 960 dp genişlikli ekranlarda içerik < 860 dp) ölçüler küçülür, ikincil etiketler gizlenir
        val compact = maxWidth < 860.dp

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 140.dp else 168.dp)
                .clip(shape)
                .background(
                    Brush.horizontalGradient(
                        listOf(RedoXColors.SurfaceHigh, RedoXColors.Surface, RedoXColors.WineGlow)
                    )
                )
                .drawBehind {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(glow, Color.Transparent),
                            center = Offset(size.height * 0.95f, size.height * 0.5f),
                            radius = size.width * 0.5f
                        )
                    )
                }
                .border(1.dp, RedoXColors.Line, shape)
                .padding(horizontal = if (compact) 18.dp else 26.dp, vertical = if (compact) 14.dp else 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChannelBrandLogoBox(
                channel = channel,
                boxSize = if (compact) 76.dp else 112.dp,
                fontSize = if (compact) 20.sp else 28.sp
            )

            Spacer(modifier = Modifier.width(if (compact) 16.dp else 24.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TvLiveBadge(text = if (isBroken) "ÇALIŞMIYOR" else "CANLI", broken = isBroken)
                    Text(
                        text = "CH ${channel.number}",
                        color = TvThemeColors.TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(TvThemeColors.categoryColor(channel.category))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = channel.category,
                            color = TvThemeColors.TextSecondary,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!compact) {
                        TvStatPill(
                            text = "${channel.qualityBadge} · ${channel.sources.size} akış" + if (channel.hasRealEpg) " · rehber" else ""
                        )
                        if (isHealthy && !isBroken) {
                            TvStatPill(text = "Aktif akış", color = RedoXColors.Mint)
                        }
                        if (isLocked) {
                            TvStatPill(text = "PIN kilitli")
                        }
                    }
                }

                Text(
                    text = channel.name,
                    color = TvThemeColors.TextPrimary,
                    fontSize = if (compact) 26.sp else 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isBroken) {
                    Text(
                        text = "Bu kanalın akışı şu an yanıt vermiyor. Başka bir akış denemek için İzle'ye basın.",
                        color = TvThemeColors.BrokenRed,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = channel.nowTitleOrLive(),
                            color = TvThemeColors.TextPrimary,
                            fontSize = if (compact) 16.sp else 18.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (channel.hasRealEpg) {
                            Text(
                                text = channel.currentProgramTime,
                                color = TvThemeColors.TextSecondary,
                                fontSize = 14.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    if (channel.hasRealEpg) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TvProgressBar(
                                progress = channel.progressForUi(),
                                height = 6.dp,
                                modifier = Modifier.width(if (compact) 150.dp else 320.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Sırada: ${channel.nextProgramTitle}",
                                color = TvThemeColors.TextSecondary,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else if (!compact) {
                        Text(
                            text = "Bu kanal için rehber bilgisi yok.",
                            color = RedoXColors.TextTertiary,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
            ) {
                TvActionButton(
                    text = "İzle",
                    icon = RedoXIcons.PlayArrow,
                    isPrimary = true,
                    onClick = onWatchClick,
                    modifier = if (watchFocusRequester != null) Modifier.focusRequester(watchFocusRequester) else Modifier
                )
                Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)) {
                    TvIconPill(
                        icon = if (isFavorite) RedoXIcons.StarFilled else RedoXIcons.StarBorder,
                        label = if (isFavorite) "Favoride" else "Favori ekle",
                        onClick = onToggleFavorite,
                        active = isFavorite
                    )
                    TvIconPill(
                        icon = RedoXIcons.Lock,
                        label = if (isLocked) "Kilitli" else "Kilitle",
                        onClick = onToggleLock,
                        active = isLocked
                    )
                    TvIconPill(
                        icon = RedoXIcons.Edit,
                        label = "Düzenle",
                        onClick = onEditChannel
                    )
                    TvIconPill(
                        icon = RedoXIcons.VisibilityOff,
                        label = "Gizle",
                        onClick = onHideChannel
                    )
                    if (hiddenCount > 0) {
                        TvIconPill(
                            icon = RedoXIcons.Refresh,
                            label = "Geri yükle ($hiddenCount)",
                            onClick = onRestoreHidden
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TvChannelGridCard(
    channel: TvChannel,
    isFavorite: Boolean,
    isLocked: Boolean = false,
    isBroken: Boolean = false,
    isHealthy: Boolean = false,
    focusRequester: FocusRequester? = null,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    var cardFocused by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (cardFocused) 1.045f else 1f,
        animationSpec = tween(durationMillis = 135),
        label = "grid_card_scale"
    )
    val shape = RoundedCornerShape(14.dp)
    // Kartın solunda kanalın marka renginden çok hafif bir ton (odakta zemin bir kademe açılır)
    val cardBase = if (cardFocused) RedoXColors.SurfaceHigh else RedoXColors.Surface
    val cardBrush = Brush.horizontalGradient(
        listOf(lerp(cardBase, Color(channel.brandPrimaryColor).copy(alpha = 1f), 0.13f), cardBase)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(124.dp)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .tvSurface(
                focused = cardFocused,
                shape = shape,
                brush = cardBrush,
                idleBorder = if (isBroken) RedoXColors.Danger.copy(alpha = 0.6f) else RedoXColors.Line
            )
            .onFocusChanged {
                cardFocused = it.isFocused || it.hasFocus
                if (it.isFocused || it.hasFocus) onFocused()
            }
            .tvLongPressFavoriteModifier(
                itemKey = channel.id,
                onShortClick = onClick,
                onLongPress = onLongClick
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 13.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(RedoXColors.Ink600)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${channel.number}",
                            color = TvThemeColors.TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = channel.qualityBadge,
                        color = RedoXColors.TextTertiary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isHealthy && !isBroken) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(RedoXColors.Mint)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                    }
                    if (isBroken) {
                        Icon(
                            imageVector = RedoXIcons.WarningAmber,
                            contentDescription = "Yanıt vermiyor",
                            tint = RedoXColors.Danger,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                    }
                    if (isLocked) {
                        Icon(
                            imageVector = RedoXIcons.Lock,
                            contentDescription = "PIN kilitli",
                            tint = RedoXColors.Gold,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                    }
                    Icon(
                        imageVector = if (isFavorite) RedoXIcons.StarFilled else RedoXIcons.StarBorder,
                        contentDescription = if (isFavorite) "Favoride" else "Favori değil",
                        tint = if (isFavorite) RedoXColors.Gold else RedoXColors.TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                ChannelBrandLogoBox(
                    channel = channel,
                    boxSize = 46.dp,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channel.name,
                        color = TvThemeColors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isBroken) "Yanıt vermiyor" else channel.nowTitleOrLive(),
                        color = if (isBroken) RedoXColors.Danger else TvThemeColors.TextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (channel.hasRealEpg && !isBroken) {
                TvProgressBar(progress = channel.progressForUi())
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun ChannelSearchAndNumpadScreen(
    allChannels: List<TvChannel>,
    searchQuery: String,
    favoriteIds: Set<String>,
    lockedIds: Set<String>,
    brokenIds: Set<String>,
    onQueryChange: (String) -> Unit,
    onPlayChannel: (TvChannel) -> Unit,
    onToggleFavorite: (String) -> Unit = {},
    epgMap: Map<String, List<EpgProgram>> = emptyMap(),
    onVoiceSearch: () -> Unit = {}
) {
    // Kanal adı / kategori / numara + gerçek program adları; küçük yazım hataları tolere edilir.
    val results = remember(allChannels, searchQuery, epgMap) {
        ChannelSearch.search(searchQuery, allChannels, epgMap)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.weight(1f)) {
                TvInputField(
                    label = "Kanal adı, kategori, program veya numara yazın (OK basılı tut: favori ekle / çıkar)",
                    value = searchQuery,
                    placeholder = "Örn: haber, spor, belgesel veya 14",
                    onValueChange = onQueryChange
                )
            }
            TvActionButton(
                text = "Sesli Ara",
                icon = RedoXIcons.Mic,
                isPrimary = false,
                onClick = onVoiceSearch
            )
            if (searchQuery.isNotEmpty()) {
                TvActionButton(
                    text = "Temizle",
                    icon = RedoXIcons.Clear,
                    isPrimary = false,
                    onClick = { onQueryChange("") }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chromecast 4K Kumandası İçin Ekran Üstü Sanal Rakam Tuş Takımı (0-9) + Hızlı Kategori Etiketleri
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Numaratör:",
                color = TvThemeColors.FavoriteGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            (1..9).forEach { digit ->
                TvCompactChipButton(
                    text = digit.toString(),
                    onClick = {
                        val currentDigits = searchQuery.filter { it.isDigit() }
                        onQueryChange((currentDigits + digit.toString()).take(4))
                    }
                )
            }
            TvCompactChipButton(
                text = "0",
                onClick = {
                    val currentDigits = searchQuery.filter { it.isDigit() }
                    onQueryChange((currentDigits + "0").take(4))
                }
            )
            TvCompactChipButton(
                text = "Sil",
                icon = RedoXIcons.Clear,
                onClick = {
                    if (searchQuery.isNotEmpty()) onQueryChange(searchQuery.dropLast(1))
                }
            )
            val exactNumberMatch = searchQuery.trim().toIntOrNull()?.let { num ->
                allChannels.find { it.number == num }
            }
            if (exactNumberMatch != null) {
                TvActionButton(
                    text = "CH ${exactNumberMatch.number} · ${exactNumberMatch.name} aç",
                    icon = RedoXIcons.PlayArrow,
                    isPrimary = true,
                    onClick = { onPlayChannel(exactNumberMatch) }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val quickTags = listOf("Ulusal", "Spor", "Haber", "Belgesel", "Müzik", "Kıbrıs", "Yerel", "Dünya")
            items(quickTags) { tag ->
                TvCompactChipButton(
                    text = "#$tag",
                    onClick = { onQueryChange(tag) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 190.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            itemsIndexed(results, key = { idx, hit -> "search_${hit.channel.id}_$idx" }) { _, hit ->
                val ch = hit.channel
                TvChannelGridCard(
                    channel = hit.displayChannel(),
                    isFavorite = favoriteIds.contains(ch.id),
                    isLocked = lockedIds.contains(ch.id),
                    isBroken = brokenIds.contains(ch.id) || ch.isBroken,
                    onFocused = {},
                    onClick = { onPlayChannel(ch) },
                    onLongClick = { onToggleFavorite(ch.id) }
                )
            }
        }
    }
}
