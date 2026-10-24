package com.example.chromecasttv

import android.content.ComponentName
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken

/** Telefon ve tablet iskeletlerinin ortak kullandığı durum ve eylemler. */
class MobileEnv(
    val viewModel: TvViewModel,
    val host: RedoXHost,
    val state: TvUiState,
    val features: FeatureState,
    val nav: MobileNav,
    val actions: MChannelActions,
    val snackbar: SnackbarHostState,
    val isTablet: Boolean
)

/**
 * Ortak kabuk: durum akışlarını toplar; tam ekran oynatıcıyı, PIN penceresini, durum mesajlarını (Snackbar),
 * arka plan oynatma servisini ve yaşam döngüsü kurallarını yönetir. Görünür iskelet [scaffold] ile gelir
 * (telefon: alt gezinti + mini oynatıcı • tablet: gezinti rayı).
 */
@Composable
fun MobileShell(
    viewModel: TvViewModel,
    host: RedoXHost,
    isTablet: Boolean,
    scaffold: @Composable (MobileEnv) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val features by viewModel.features.state.collectAsState()
    val nav = remember { MobileNav(MobileTab.HOME) }
    val snackbar = remember { SnackbarHostState() }
    val actions = rememberMChannelActions(viewModel, state, features)

    // Durum mesajları (favori eklendi, hatırlatma, güncelleme…) kısa bir Snackbar olarak gösterilir
    LaunchedEffect(state.statusMessage) {
        val message = state.statusMessage
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.clearStatusMessage()
        }
    }

    // Hiç oynatma yoksa ortak oynatıcıyı durdur (sesin takılı kalmasını önler)
    val hasPlayback = state.activePlayingChannel != null || state.pipChannel != null
    val fullscreen = state.activePlayingChannel != null
    LaunchedEffect(hasPlayback) {
        if (!hasPlayback) PlaybackHub.stop()
    }

    // Radyo (demo): liste bir kez yüklenir; bir TV kanalı başlayınca radyo susar
    val radioContext = LocalContext.current
    LaunchedEffect(Unit) { RadioHub.ensureLoaded(radioContext) }
    val radio by RadioHub.state.collectAsState()
    LaunchedEffect(hasPlayback) {
        if (hasPlayback) RadioHub.stop()
    }

    MobileMediaSessionLink(active = hasPlayback)
    MobilePlaybackLifecycle(
        host = host,
        prefs = features.prefs,
        hasPlayback = hasPlayback,
        fullscreen = fullscreen
    )

    val env = MobileEnv(viewModel, host, state, features, nav, actions, snackbar, isTablet)

    Box(modifier = Modifier.fillMaxSize()) {
        scaffold(env)

        state.activePlayingChannel?.let { channel ->
            MobilePlayerScreen(
                channel = channel,
                viewModel = viewModel,
                state = state,
                features = features,
                host = host,
                isTablet = isTablet,
                onMinimize = { viewModel.minimizeToPip() },
                onClose = { viewModel.closePlayer() }
            )
        }

        // Telefon: tam ekran radyo oynatıcı (tablette oynatıcı Radyo sekmesindeki panelde)
        if (radio.playerOpen && !isTablet) {
            MobileRadioPlayerOverlay()
        }

        state.pendingPinChannel?.let { channel ->
            MPinDialog(
                channel = channel,
                onVerify = { pin -> viewModel.verifyPinAndPlay(pin) },
                onDismiss = { viewModel.dismissPinDialog() }
            )
        }
    }
}

/** Seçili sekmenin ekranı (iskeletler bunu içerik alanına yerleştirir). */
@Composable
fun MobileContent(env: MobileEnv, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Crossfade(
            targetState = env.nav.current,
            animationSpec = tween(durationMillis = 140),
            label = "mobile_tab"
        ) { tab ->
            when (tab) {
                MobileTab.HOME -> MobileHomeScreen(
                    env.viewModel, env.state, env.features, env.host, env.isTablet, env.nav, env.actions
                )
                MobileTab.LIVE -> MobileLiveScreen(
                    env.viewModel, env.state, env.isTablet, env.nav, env.actions
                )
                MobileTab.RADIO -> MobileRadioScreen(isTablet = env.isTablet)
                MobileTab.GUIDE -> MobileGuideScreen(
                    env.viewModel, env.state, env.features, env.host, env.nav, env.actions
                )
                MobileTab.FAVORITES -> MobileFavoritesScreen(
                    env.viewModel, env.state, env.features, env.nav, env.actions
                )
                MobileTab.REMOTE -> MobileRemoteScreen(
                    env.viewModel, env.state, env.host, env.isTablet
                )
                MobileTab.SEARCH -> MobileSearchScreen(
                    env.viewModel, env.state, env.host, env.actions
                )
                MobileTab.MULTI -> MobileMultiViewScreen(
                    env.viewModel, env.state, env.isTablet, env.nav
                )
                MobileTab.SOURCES -> MobileSourcesScreen(
                    env.viewModel, env.state, env.host
                )
                MobileTab.SETTINGS -> MobileSettingsScreen(
                    env.viewModel, env.state, env.features, env.host
                )
            }
        }
    }
}

/**
 * Oynatma sürerken arka plan servisine ([PlaybackService]) bir Media3 denetleyicisiyle bağlanır;
 * böylece bildirim, kilit ekranı kontrolleri ve ön plan servisi çalışır. Oynatma bitince bağlantı bırakılır.
 */
@Composable
fun MobileMediaSessionLink(active: Boolean) {
    val context = LocalContext.current
    DisposableEffect(active) {
        val future = if (active) {
            try {
                val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
                MediaController.Builder(context, token).buildAsync()
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
        onDispose {
            if (future != null) {
                try {
                    MediaController.releaseFuture(future)
                } catch (_: Exception) {
                }
            }
        }
    }
}

/**
 * Uygulama arka plana alınınca: "arka planda ses" açıksa yalnızca görüntü kapatılır (veri/pil tasarrufu),
 * kapalıysa oynatma duraklatılır; öne dönünce eski haline getirilir.
 */
@Composable
fun MobilePlaybackLifecycle(
    host: RedoXHost,
    prefs: FeaturePrefs,
    hasPlayback: Boolean,
    fullscreen: Boolean
) {
    val owner = LocalContext.current as? LifecycleOwner ?: return
    val currentPrefs by rememberUpdatedState(prefs)
    val currentHas by rememberUpdatedState(hasPlayback)
    val currentFullscreen by rememberUpdatedState(fullscreen)
    DisposableEffect(owner) {
        var pausedByUs = false
        val observer = LifecycleEventObserver { _, event ->
            val player = PlaybackHub.peek()
            if (player != null && currentHas) {
                when (event) {
                    Lifecycle.Event.ON_STOP -> {
                        if (!host.isInPip) {
                            if (currentPrefs.backgroundAudio) {
                                PlaybackHub.setVideoEnabled(false)
                            } else if (player.isPlaying) {
                                player.pause()
                                pausedByUs = true
                            }
                        }
                    }
                    Lifecycle.Event.ON_START -> {
                        if (currentFullscreen) PlaybackHub.setVideoEnabled(true)
                        if (pausedByUs) {
                            player.play()
                            pausedByUs = false
                        }
                    }
                    else -> {}
                }
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}
