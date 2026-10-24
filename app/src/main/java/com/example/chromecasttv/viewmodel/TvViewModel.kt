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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
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

import android.media.AudioManager

class TvViewModel(
    private val repository: TvRepository,
    private val appContext: Context
) : ViewModel() {
    private val initialBuiltIn = repository.getBuiltInChannels()

    private val _showLandingScreen = MutableStateFlow(true)
    private val _selectedTab = MutableStateFlow(MainNavTab.ALL_CHANNELS)
    // ANINDA AÇILIŞ: 222 Kanal 0. milisaniyede doğrudan hazır!
    private val _builtInChannels = MutableStateFlow<List<TvChannel>>(initialBuiltIn)
    private val _customM3uChannels = MutableStateFlow<List<TvChannel>>(emptyList())
    private val _selectedCategory = MutableStateFlow("Tümü")
    private val _favoriteOrder = MutableStateFlow<List<String>>(repository.loadFavoriteOrderList())
    private val _lockedIds = MutableStateFlow<Set<String>>(repository.loadLockedChannelIds())
    private val _brokenIds = MutableStateFlow<Set<String>>(repository.loadBrokenChannelIds())
    private val _parentalPin = MutableStateFlow(repository.loadParentalPin())
    private val _pendingPinChannel = MutableStateFlow<TvChannel?>(null)
    private val _recentIds = MutableStateFlow<List<String>>(repository.loadRecentChannelIds())
    private val _epgMap = MutableStateFlow<Map<String, List<EpgProgram>>>(emptyMap())
    private val _playlists = MutableStateFlow<List<M3uPlaylistSource>>(repository.loadSavedPlaylists())
    private val _activePlayingChannel = MutableStateFlow<TvChannel?>(null)
    private val _pipChannel = MutableStateFlow<TvChannel?>(null)
    private val _multiViewChannels = MutableStateFlow<List<TvChannel>>(initialBuiltIn.take(4))
    private val _isMultiViewFourGrid = MutableStateFlow(true)
    private val _searchQuery = MutableStateFlow("")
    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    private val _localWebServerUrl = MutableStateFlow("http://${LocalTransferWebServer.getLocalWifiIpAddress()}:8585")
    private val _isLoadingM3u = MutableStateFlow(false)
    private val _statusMessage = MutableStateFlow<String?>(null)
    private val _extraState = MutableStateFlow(
        TvExtraSettingsState(
            preferredSources = repository.loadPreferredSources(),
            preferredScales = repository.loadPreferredScales(),
            autoStartLastChannel = repository.loadAutoStartLastChannel(),
            hiddenChannelIds = repository.loadHiddenChannelIds()
        )
    )

    private var sleepTimerJob: Job? = null
    private var healthCheckJob: Job? = null
    private var lastZapElapsedMillis: Long = 0L

    // --- Yeni özellikler: gerçek EPG, hatırlatıcılar, çoklu favori listeleri, özel kanal adları ... ---
    private val _tick = MutableStateFlow(0L)
    private var controlServer: LocalControlServer? = null
    private var remoteDiscovery: RemoteDiscovery? = null

    /** TV arayüzü (Activity) tarafından atanır: telefon kumandasından gelen tuşları uygulamaya enjekte eder. */
    var remoteKeyInjector: ((Int) -> Unit)? = null

    val features: FeatureHub = FeatureHub(
        appContext,
        viewModelScope,
        object : FeatureHub.Bridge {
            override fun allChannels(): List<TvChannel> = _builtInChannels.value + _customM3uChannels.value

            override fun playlistEpgUrls(): List<String> =
                _playlists.value.map { it.epgXmlUrl }.filter { it.isNotBlank() }

            override fun activeFavoriteOrder(): List<String> = _favoriteOrder.value

            override fun applyFavoriteOrder(ids: List<String>) {
                _favoriteOrder.value = ids
                repository.saveFavoriteOrderList(ids)
            }

            override fun showStatus(message: String) {
                _statusMessage.value = message
            }
        }
    )

    val uiState: StateFlow<TvUiState> = combine(
        _showLandingScreen,
        _selectedTab,
        _builtInChannels,
        _customM3uChannels,
        _selectedCategory,
        _favoriteOrder,
        _lockedIds,
        _brokenIds,
        _parentalPin,
        _pendingPinChannel,
        _recentIds,
        _epgMap,
        _playlists,
        _activePlayingChannel,
        _pipChannel,
        _multiViewChannels,
        _isMultiViewFourGrid,
        _searchQuery,
        _sleepTimerMinutes,
        _localWebServerUrl,
        _isLoadingM3u,
        _statusMessage,
        _extraState,
        features.epgByChannelId,
        features.overrides,
        _tick
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val showLanding = values[0] as Boolean
        val tab = values[1] as MainNavTab
        val builtIn = values[2] as List<TvChannel>
        val customCh = values[3] as List<TvChannel>
        val category = values[4] as String
        val favOrder = values[5] as List<String>
        val locked = values[6] as Set<String>
        val broken = values[7] as Set<String>
        val pin = values[8] as String
        val pendingPinCh = values[9] as TvChannel?
        val recentIds = values[10] as List<String>
        val epg = values[11] as Map<String, List<EpgProgram>>
        val playlists = values[12] as List<M3uPlaylistSource>
        val playing = values[13] as TvChannel?
        val pip = values[14] as TvChannel?
        val multiList = values[15] as List<TvChannel>
        val isFourGrid = values[16] as Boolean
        val query = values[17] as String
        val sleepMin = values[18] as Int?
        val webUrl = values[19] as String
        val loading = values[20] as Boolean
        val status = values[21] as String?
        val extra = values[22] as TvExtraSettingsState
        val realEpg = values[23] as Map<String, List<EpgProgram>>
        val channelOverrides = values[24] as Map<String, ChannelOverride>

        val hidden = extra.hiddenChannelIds
        val visibleBuiltIn = if (hidden.isEmpty()) builtIn else builtIn.filterNot { hidden.contains(it.id) }
        val visibleCustom = if (hidden.isEmpty()) customCh else customCh.filterNot { hidden.contains(it.id) }

        val filteredBuiltIn = if (extra.onlyShowHealthy && extra.healthyChannelIds.isNotEmpty()) {
            visibleBuiltIn.filter { extra.healthyChannelIds.contains(it.id) }
        } else visibleBuiltIn

        val filteredCustom = if (extra.onlyShowHealthy && extra.healthyChannelIds.isNotEmpty()) {
            visibleCustom.filter { extra.healthyChannelIds.contains(it.id) }
        } else visibleCustom

        val favSet = favOrder.toSet()
        val distinctBuiltInCats = listOf("Tümü") + visibleBuiltIn.map { it.category }.distinct()
        val distinctCustomCats = listOf("Tümü") + visibleCustom.map { it.category }.distinct()
        val defaultMulti = if (multiList.isNotEmpty()) multiList else visibleBuiltIn.take(4)

        val builtInMarked = if (broken.isEmpty()) filteredBuiltIn else filteredBuiltIn.map { it.copy(isBroken = broken.contains(it.id)) }
        val customMarked = if (broken.isEmpty()) filteredCustom else filteredCustom.map { it.copy(isBroken = broken.contains(it.id)) }

        // Gerçek (XMLTV) rehber ve kullanıcının verdiği özel ad/numara bindirmeleri
        val nowMillis = System.currentTimeMillis()
        val builtInFinal = FeatureOverlays.applyChannels(builtInMarked, realEpg, channelOverrides, nowMillis)
        val customFinal = FeatureOverlays.applyChannels(customMarked, realEpg, channelOverrides, nowMillis)
        val epgFinal = FeatureOverlays.mergeEpgMap(epg, builtInFinal + customFinal, realEpg)

        fun resolve(channel: TvChannel?): TvChannel? {
            if (channel == null) return null
            return builtInFinal.firstOrNull { it.id == channel.id }
                ?: customFinal.firstOrNull { it.id == channel.id }
                ?: channel
        }

        TvUiState(
            showLandingScreen = showLanding,
            selectedTab = tab,
            builtInChannels = builtInFinal,
            customM3uChannels = customFinal,
            categories = distinctBuiltInCats,
            customCategories = distinctCustomCats,
            selectedCategory = category,
            favoriteOrderList = favOrder,
            favoriteIds = favSet,
            lockedChannelIds = locked,
            brokenChannelIds = broken,
            hiddenChannelIds = hidden,
            healthyChannelIds = extra.healthyChannelIds,
            isScanningHealth = extra.isScanningHealth,
            healthScanProgress = extra.healthScanProgress,
            onlyShowHealthy = extra.onlyShowHealthy,
            preferredSources = extra.preferredSources,
            preferredScales = extra.preferredScales,
            autoStartLastChannel = extra.autoStartLastChannel,
            previousPlayingChannel = resolve(extra.previousPlayingChannel),
            parentalPin = pin,
            pendingPinChannel = resolve(pendingPinCh),
            recentChannels = recentIds.mapNotNull { id ->
                builtInFinal.firstOrNull { it.id == id } ?: customFinal.firstOrNull { it.id == id }
            },
            epgMap = epgFinal,
            playlists = playlists,
            activePlayingChannel = resolve(playing),
            pipChannel = resolve(pip),
            multiViewChannels = defaultMulti.map { resolve(it) ?: it },
            isMultiViewFourGrid = isFourGrid,
            searchQuery = query,
            sleepTimerRemainingMinutes = sleepMin,
            localWebServerUrl = webUrl,
            isLoadingM3u = loading,
            statusMessage = status
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        TvUiState(
            builtInChannels = initialBuiltIn,
            categories = listOf("Tümü") + initialBuiltIn.map { it.category }.distinct(),
            favoriteOrderList = _favoriteOrder.value,
            favoriteIds = _favoriteOrder.value.toSet(),
            hiddenChannelIds = _extraState.value.hiddenChannelIds,
            preferredSources = _extraState.value.preferredSources,
            preferredScales = _extraState.value.preferredScales,
            autoStartLastChannel = _extraState.value.autoStartLastChannel,
            localWebServerUrl = _localWebServerUrl.value
        )
    )

    // Yerel sunucunun (QR web sayfası + telefon kumandası) TV uygulamasına bağlandığı yüzey.
    // NOT: init bloğundan ÖNCE tanımlanmalıdır; sunucu init içinde başlatılır.
    private val controlHandler = object : ControlServerHandler {
        override suspend fun onAddM3u(name: String, url: String, rawContent: String) {
            if (rawContent.isNotBlank()) {
                val saved = repository.saveRawM3uTextAsLocalPlaylist(rawContent, name)
                if (saved != null) {
                    _playlists.value = _playlists.value + saved
                    repository.savePlaylists(_playlists.value)
                    reloadAllChannelsAndEpg()
                    _statusMessage.value = "Telefondan '$name' yüklendi!"
                }
            } else if (url.isNotBlank()) {
                addRemoteM3uPlaylist(name, url, "")
                _statusMessage.value = "Telefondan '$name' eklendi!"
            }
        }

        override suspend fun onAddXtream(name: String, server: String, username: String, password: String) {
            if (server.isNotBlank() && username.isNotBlank()) {
                addXtreamCodesAccount(name, server, username, password)
                _statusMessage.value = "Telefondan '$name' Xtream bağlandı!"
            }
        }

        override suspend fun onRestoreBackup(data: String): Boolean {
            val ok = restoreFromBackupString(data)
            if (ok) {
                _statusMessage.value = "Telefondan yedek başarıyla geri yüklendi!"
            }
            return ok
        }

        override fun onExportBackup(): String = repository.exportBackupPayload()

        override fun remoteDeviceName(): String = remoteDeviceLabel()

        override fun remoteSnapshot(): RemoteSnapshot = buildRemoteSnapshot()

        override fun remoteChannels(): List<RemoteChannelInfo> {
            val ui = uiState.value
            return (ui.builtInChannels + ui.customM3uChannels).map { toRemoteInfo(it, ui) }
        }

        override fun remotePlay(channelId: String): Boolean {
            val ui = uiState.value
            val channel = (ui.builtInChannels + ui.customM3uChannels).firstOrNull { it.id == channelId }
                ?: return false
            viewModelScope.launch { requestPlayChannel(channel) }
            return true
        }

        override fun remoteZap(delta: Int) {
            viewModelScope.launch { zapChannel(delta) }
        }

        override fun remoteKey(key: String): Boolean {
            val code = when {
                key == "up" -> KeyEvent.KEYCODE_DPAD_UP
                key == "down" -> KeyEvent.KEYCODE_DPAD_DOWN
                key == "left" -> KeyEvent.KEYCODE_DPAD_LEFT
                key == "right" -> KeyEvent.KEYCODE_DPAD_RIGHT
                key == "ok" -> KeyEvent.KEYCODE_DPAD_CENTER
                key == "back" -> KeyEvent.KEYCODE_BACK
                key == "menu" -> KeyEvent.KEYCODE_MENU
                key == "channel_up" -> KeyEvent.KEYCODE_CHANNEL_UP
                key == "channel_down" -> KeyEvent.KEYCODE_CHANNEL_DOWN
                key == "play_pause" -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                key.length == 1 && key[0] in '0'..'9' -> KeyEvent.KEYCODE_0 + (key[0] - '0')
                else -> -1
            }
            val injector = remoteKeyInjector
            if (code < 0 || injector == null) return false
            injector(code)
            return true
        }

        override fun remoteVolume(delta: Int, absoluteLevel: Int): RemoteSnapshot {
            val audio = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audio != null) {
                try {
                    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                    val target = if (absoluteLevel >= 0) absoluteLevel else current + delta
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, target.coerceIn(0, max), 0)
                } catch (_: Throwable) {
                }
            }
            return buildRemoteSnapshot()
        }

        override fun remotePlayPause() {
            remoteKeyInjector?.invoke(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        }

        override fun remoteStop() {
            viewModelScope.launch { closePlayer() }
        }

        override fun remoteCast(name: String, url: String, logoUrl: String): Boolean {
            val lower = url.lowercase(Locale.US)
            val type = when {
                lower.contains(".mpd") -> StreamType.DASH
                lower.endsWith(";") -> StreamType.PROGRESSIVE
                else -> StreamType.HLS
            }
            val castId = "cast_" + url.hashCode().toString().replace('-', 'n')
            val channel = TvChannel(
                id = castId,
                number = 9000,
                name = name,
                shortLogoText = M3uAndEpgParser.makeShortLogoText(name),
                brandPrimaryColor = 0xFF1E3A8AL,
                brandSecondaryColor = 0xFF3B82F6L,
                category = "Telefondan",
                logoUrl = logoUrl,
                sources = listOf(StreamSource("Telefondan Yayın", url, type)),
                epgChannelId = castId,
                sourcePlaylistName = "Telefon",
                isCustomM3u = true
            )
            viewModelScope.launch { requestPlayChannel(channel, bypassPin = true) }
            return true
        }

        override fun remotePaired(deviceName: String) {
            _statusMessage.value = "Kumanda eşleşti: $deviceName"
            controlServer?.let { publishRemoteHostState(it) }
        }
    }

    init {
        if (_extraState.value.autoStartLastChannel) {
            val lastId = _recentIds.value.firstOrNull()
            val targetCh = initialBuiltIn.find { it.id == lastId } ?: initialBuiltIn.firstOrNull()
            if (targetCh != null && !_lockedIds.value.contains(targetCh.id)) {
                _showLandingScreen.value = false
                _activePlayingChannel.value = targetCh
            }
        }
        reloadAllChannelsAndEpg()
        val isTv = BuildConfig.DEVICE_KIND == "tv"
        if (isTv) {
            startLocalWifiTransferServer()
        }
        features.start(isTv)

        // İzleme istatistikleri: aktif kanal her değiştiğinde süre/sayaç güncellenir
        viewModelScope.launch {
            _activePlayingChannel.collect { channel ->
                features.onActiveChannelChanged(channel?.id)
            }
        }
        // Her dakika "şu an / sıradaki" bilgisi ve ilerleme çubukları tazelenir
        viewModelScope.launch {
            while (true) {
                delay(60_000L)
                _tick.value = System.currentTimeMillis()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try { controlServer?.stop() } catch (_: Throwable) {}
        try { remoteDiscovery?.unregister() } catch (_: Throwable) {}
    }

    fun onAppBackgrounded() {
        _pipChannel.value = null
        _activePlayingChannel.value = null
    }

    fun dismissLandingAndOpenTab(tab: MainNavTab = MainNavTab.ALL_CHANNELS) {
        _selectedTab.value = tab
        _showLandingScreen.value = false
        if (tab == MainNavTab.M3U_MANAGER) {
            refreshWifiTransferServer()
        }
    }

    fun openLandingScreen() {
        _activePlayingChannel.value = null
        _pipChannel.value = null
        _showLandingScreen.value = true
    }

    private fun remoteDeviceLabel(): String = "RedoX TV 2 ${Build.MODEL}".trim().take(40)

    private fun toRemoteInfo(channel: TvChannel, ui: TvUiState): RemoteChannelInfo {
        return RemoteChannelInfo(
            id = channel.id,
            number = channel.number,
            name = channel.name,
            category = channel.category,
            logoUrl = channel.logoUrl,
            isFavorite = ui.favoriteIds.contains(channel.id),
            isLocked = ui.lockedChannelIds.contains(channel.id)
        )
    }

    private fun buildRemoteSnapshot(): RemoteSnapshot {
        val audio = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val volume = try { audio?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0 } catch (_: Throwable) { 0 }
        val maxVolume = try { audio?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15 } catch (_: Throwable) { 15 }
        val ui = uiState.value
        val playing = ui.activePlayingChannel
        return RemoteSnapshot(
            appVersion = BuildConfig.VERSION_NAME,
            deviceName = remoteDeviceLabel(),
            playing = if (playing != null) toRemoteInfo(playing, ui) else null,
            isPlaying = playing != null,
            volume = volume,
            maxVolume = maxVolume,
            sleepMinutes = ui.sleepTimerRemainingMinutes
        )
    }

    private fun publishRemoteHostState(server: LocalControlServer) {
        val ip = LocalTransferWebServer.getLocalWifiIpAddress()
        _localWebServerUrl.value = "http://$ip:${server.port}"
        features.setRemoteHost(
            RemoteHostState(
                isRunning = server.isRunning,
                ip = ip,
                port = server.port,
                pin = server.pin,
                pairedCount = server.pairedCount
            )
        )
    }

    fun refreshWifiTransferServer() {
        if (BuildConfig.DEVICE_KIND != "tv") return
        val server = controlServer
        if (server != null && server.isRunning) {
            publishRemoteHostState(server)
        } else {
            startLocalWifiTransferServer()
        }
    }

    private fun startLocalWifiTransferServer() {
        if (BuildConfig.DEVICE_KIND != "tv") return
        try { controlServer?.stop() } catch (_: Throwable) {}
        val server = LocalControlServer(appContext, controlHandler)
        controlServer = server
        server.start(viewModelScope) { boundPort ->
            publishRemoteHostState(server)
            try {
                val discovery = remoteDiscovery ?: RemoteDiscovery(appContext).also { remoteDiscovery = it }
                discovery.register(remoteDeviceLabel(), boundPort)
            } catch (_: Throwable) {
            }
        }
    }

    /** Bağlantı (eşleştirme) kodunu yeniler; tarayıcı oturumları sona erer, eşleşmiş kumandalar etkilenmez. */
    fun regenerateRemotePin() {
        val server = controlServer ?: return
        server.regeneratePin()
        publishRemoteHostState(server)
        _statusMessage.value = "Yeni bağlantı kodu oluşturuldu."
    }

    /** Eşleşmiş tüm telefon/tablet kumandalarının erişimini kaldırır. */
    fun revokeRemotePairings() {
        val server = controlServer ?: return
        server.revokeAllRemotes()
        publishRemoteHostState(server)
        _statusMessage.value = "Eşleşmiş tüm kumandaların erişimi kaldırıldı."
    }

    fun selectTab(tab: MainNavTab) {
        _selectedTab.value = tab
        _selectedCategory.value = "Tümü"
        _showLandingScreen.value = false
        if (tab == MainNavTab.M3U_MANAGER) {
            refreshWifiTransferServer()
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun requestPlayChannel(channel: TvChannel, bypassPin: Boolean = false) {
        if (!bypassPin && _lockedIds.value.contains(channel.id)) {
            _pendingPinChannel.value = channel
            return
        }
        val currentActive = _activePlayingChannel.value
        if (currentActive != null && currentActive.id != channel.id) {
            _extraState.value = _extraState.value.copy(previousPlayingChannel = currentActive)
        }
        _showLandingScreen.value = false
        _pendingPinChannel.value = null
        _pipChannel.value = null
        _recentIds.value = repository.pushRecentChannelId(channel.id)
        _activePlayingChannel.value = channel
    }

    fun recallPreviousChannel() {
        val prev = _extraState.value.previousPlayingChannel
        if (prev != null) {
            requestPlayChannel(prev)
        } else {
            val secondRecentId = _recentIds.value.drop(1).firstOrNull()
            if (secondRecentId != null) {
                val all = _builtInChannels.value + _customM3uChannels.value
                all.find { it.id == secondRecentId }?.let { requestPlayChannel(it) }
            }
        }
    }

    fun saveChannelPreferredSource(channelId: String, sourceIndex: Int) {
        val updated = _extraState.value.preferredSources.toMutableMap()
        updated[channelId] = sourceIndex
        _extraState.value = _extraState.value.copy(preferredSources = updated)
        repository.savePreferredSources(updated)
    }

    fun saveChannelPreferredScale(channelId: String, scaleModeName: String) {
        val updated = _extraState.value.preferredScales.toMutableMap()
        updated[channelId] = scaleModeName
        _extraState.value = _extraState.value.copy(preferredScales = updated)
        repository.savePreferredScales(updated)
    }

    fun toggleAutoStartLastChannel() {
        val next = !_extraState.value.autoStartLastChannel
        _extraState.value = _extraState.value.copy(autoStartLastChannel = next)
        repository.saveAutoStartLastChannel(next)
        _statusMessage.value = if (next) {
            "Açılışta son izlenen kanaldan başla: Açık"
        } else {
            "Açılışta son izlenen kanaldan başla: Kapalı (karşılama ekranı açılır)"
        }
    }

    fun hideChannel(channelId: String) {
        val all = _builtInChannels.value + _customM3uChannels.value
        val chName = all.find { it.id == channelId }?.name ?: "Kanal"
        val updated = _extraState.value.hiddenChannelIds.toMutableSet()
        if (updated.add(channelId)) {
            _extraState.value = _extraState.value.copy(hiddenChannelIds = updated)
            repository.saveHiddenChannelIds(updated)
            if (_activePlayingChannel.value?.id == channelId) {
                zapChannel(+1)
            }
            _statusMessage.value = "$chName listeden gizlendi (M3U ve Ayarlar'dan geri yüklenebilir)."
        }
    }

    fun restoreAllHiddenChannels() {
        val count = _extraState.value.hiddenChannelIds.size
        if (count == 0) {
            _statusMessage.value = "Gizlenmiş kanal bulunmuyor."
            return
        }
        _extraState.value = _extraState.value.copy(hiddenChannelIds = emptySet())
        repository.saveHiddenChannelIds(emptySet())
        _statusMessage.value = "Gizlenen $count kanal listeye geri yüklendi."
    }

    fun toggleOnlyShowHealthy() {
        val next = !_extraState.value.onlyShowHealthy
        _extraState.value = _extraState.value.copy(onlyShowHealthy = next)
        _statusMessage.value = if (next) {
            "Yalnızca sağlık testinden geçen aktif kanallar gösteriliyor."
        } else {
            "Tüm kanallar gösteriliyor."
        }
    }

    fun runChannelHealthCheck() {
        if (_extraState.value.isScanningHealth) {
            healthCheckJob?.cancel()
            _extraState.value = _extraState.value.copy(
                isScanningHealth = false,
                healthScanProgress = ""
            )
            _statusMessage.value = "Kanal sağlık taraması durduruldu."
            return
        }
        healthCheckJob = viewModelScope.launch(Dispatchers.IO) {
            val hidden = _extraState.value.hiddenChannelIds
            val pool = (_builtInChannels.value + _customM3uChannels.value)
                .filterNot { hidden.contains(it.id) }
            if (pool.isEmpty()) return@launch

            _extraState.value = _extraState.value.copy(
                isScanningHealth = true,
                healthScanProgress = "0/${pool.size}"
            )
            _statusMessage.value = "Tüm kanallar (${pool.size} kanal) paralel olarak taranıyor..."

            val healthy = _extraState.value.healthyChannelIds.toMutableSet()
            val broken = _brokenIds.value.toMutableSet()
            var okCount = 0
            var failCount = 0
            var scannedCount = 0

            try {
                pool.chunked(16).forEach { batch ->
                    val results = coroutineScope {
                        batch.map { ch ->
                            async(Dispatchers.IO) {
                                val ok = kotlinx.coroutines.withTimeoutOrNull(2_800L) {
                                    repository.probeChannelStreamHealth(ch)
                                } ?: false
                                ch.id to ok
                            }
                        }.awaitAll()
                    }
                    results.forEach { (id, isOk) ->
                        if (isOk) {
                            healthy.add(id)
                            broken.remove(id)
                            okCount += 1
                        } else {
                            healthy.remove(id)
                            broken.add(id)
                            failCount += 1
                        }
                    }
                    scannedCount += batch.size
                    _extraState.value = _extraState.value.copy(
                        healthyChannelIds = healthy.toSet(),
                        healthScanProgress = "${scannedCount.coerceAtMost(pool.size)}/${pool.size}"
                    )
                    _brokenIds.value = broken.toSet()
                }
                repository.saveBrokenChannelIds(broken)
                _statusMessage.value = "Tüm kanallar tarandı (${pool.size}/${pool.size}): $okCount aktif • $failCount yanıt vermiyor"
            } finally {
                _extraState.value = _extraState.value.copy(
                    healthyChannelIds = healthy.toSet(),
                    isScanningHealth = false,
                    healthScanProgress = ""
                )
            }
        }
    }

    fun backupSettingsToDevice() {
        viewModelScope.launch(Dispatchers.IO) {
            val savedPath = repository.saveBackupFileToDevice()
            if (savedPath != null) {
                _statusMessage.value = "Favoriler, M3U ve kanal hafızası yedeklendi: $savedPath"
            } else {
                _statusMessage.value = "Yedek dosyası oluşturulamadı."
            }
        }
    }

    fun restoreSettingsFromDevice() {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = repository.restoreBackupFileFromDevice()
            if (ok) {
                applyRestoredPreferences()
                _statusMessage.value = "Yedek başarıyla geri yüklendi."
            } else {
                _statusMessage.value = "Download klasöründe 'redox_tv_backup.txt' bulunamadı."
            }
        }
    }

    private fun restoreFromBackupString(raw: String): Boolean {
        val ok = repository.importBackupPayload(raw)
        if (ok) {
            applyRestoredPreferences()
        }
        return ok
    }

    private fun applyRestoredPreferences() {
        _favoriteOrder.value = repository.loadFavoriteOrderList()
        _lockedIds.value = repository.loadLockedChannelIds()
        _parentalPin.value = repository.loadParentalPin()
        _playlists.value = repository.loadSavedPlaylists()
        _extraState.value = _extraState.value.copy(
            preferredSources = repository.loadPreferredSources(),
            preferredScales = repository.loadPreferredScales(),
            autoStartLastChannel = repository.loadAutoStartLastChannel(),
            hiddenChannelIds = repository.loadHiddenChannelIds()
        )
        features.reloadFromStorage()
        reloadAllChannelsAndEpg()
    }

    fun markChannelAsBroken(channelId: String) {
        val updated = _brokenIds.value.toMutableSet()
        if (updated.add(channelId)) {
            _brokenIds.value = updated
            repository.saveBrokenChannelIds(updated)
        }
    }

    fun clearChannelBrokenStatus(channelId: String) {
        val updated = _brokenIds.value.toMutableSet()
        if (updated.remove(channelId)) {
            _brokenIds.value = updated
            repository.saveBrokenChannelIds(updated)
        }
    }

    fun dismissPinDialog() {
        _pendingPinChannel.value = null
    }

    fun verifyPinAndPlay(enteredPin: String): Boolean {
        val target = _pendingPinChannel.value ?: return false
        return if (enteredPin == _parentalPin.value) {
            requestPlayChannel(target, bypassPin = true)
            true
        } else {
            _statusMessage.value = "Hatalı PIN! Tekrar deneyin."
            false
        }
    }

    fun toggleChannelLock(channelId: String) {
        val updated = _lockedIds.value.toMutableSet()
        val lockedNow = if (updated.add(channelId)) true else {
            updated.remove(channelId)
            false
        }
        _lockedIds.value = updated
        repository.saveLockedChannelIds(updated)
        _statusMessage.value = if (lockedNow) {
            "Kanal ebeveyn PIN'i ile kilitlendi."
        } else {
            "Kanal kilidi kaldırıldı."
        }
    }

    fun updateParentalPin(newPin: String) {
        if (newPin.length == 4 && newPin.all { it.isDigit() }) {
            _parentalPin.value = newPin
            repository.saveParentalPin(newPin)
            _statusMessage.value = "Ebeveyn Kilidi PIN kodu güncellendi."
        }
    }

    /** Bildirim / TV ana ekranı bağlantısı gibi dış kaynaklardan kanal kimliğiyle oynatır. */
    fun playChannelById(channelId: String) {
        val all = _builtInChannels.value + _customM3uChannels.value
        val match = all.find { it.id == channelId }
        if (match != null) {
            requestPlayChannel(match)
        } else {
            _statusMessage.value = "Kanal bulunamadı."
        }
    }

    fun playChannelByNumber(channelNumber: Int) {
        // Özel numaralar ve gizlenen kanallar dikkate alınsın diye arayüz durumundaki (işlenmiş) listeyi kullan
        val ui = uiState.value
        val all = ui.builtInChannels + ui.customM3uChannels
        val match = all.find { it.number == channelNumber }
        if (match != null) {
            requestPlayChannel(match)
        } else {
            _statusMessage.value = "Kanal $channelNumber bulunamadı."
        }
    }

    fun minimizeToPip() {
        val current = _activePlayingChannel.value ?: return
        _activePlayingChannel.value = null
        _pipChannel.value = current
    }

    fun closePip() {
        _pipChannel.value = null
    }

    fun expandPipToFullScreen() {
        val pip = _pipChannel.value ?: return
        _pipChannel.value = null
        _activePlayingChannel.value = pip
    }

    fun openMultiViewFromPlayer() {
        val current = _activePlayingChannel.value
        _activePlayingChannel.value = null
        _pipChannel.value = null
        if (current != null) {
            val all = _builtInChannels.value + _customM3uChannels.value
            val others = all.filterNot { it.id == current.id }.take(3)
            _multiViewChannels.value = listOf(current) + others
        }
        _selectedTab.value = MainNavTab.MULTI_VIEW
    }

    fun toggleMultiViewMode() {
        _isMultiViewFourGrid.value = !_isMultiViewFourGrid.value
    }

    fun changeMultiViewSlotChannel(slotIndex: Int, delta: Int) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastZapElapsedMillis < 420L) return
        lastZapElapsedMillis = now

        val all = _builtInChannels.value + _customM3uChannels.value
        if (all.isEmpty()) return
        val currentSlots = (_multiViewChannels.value.ifEmpty { all.take(4) }).toMutableList()
        while (currentSlots.size < 4) {
            currentSlots.add(all[currentSlots.size % all.size])
        }
        val currentCh = currentSlots[slotIndex]
        val idx = all.indexOfFirst { it.id == currentCh.id }.coerceAtLeast(0)
        currentSlots[slotIndex] = all[(idx + delta).mod(all.size)]
        _multiViewChannels.value = currentSlots
    }

    fun closePlayer() {
        _activePlayingChannel.value = null
    }

    fun zapChannel(delta: Int) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastZapElapsedMillis < 450L) return
        lastZapElapsedMillis = now

        val current = _activePlayingChannel.value ?: return
        val currentList = when (_selectedTab.value) {
            MainNavTab.FAVORITES -> {
                val all = _builtInChannels.value + _customM3uChannels.value
                val byId = all.associateBy { it.id }
                _favoriteOrder.value.mapNotNull { byId[it] }.ifEmpty { _builtInChannels.value }
            }
            MainNavTab.CUSTOM_M3U -> _customM3uChannels.value.ifEmpty { _builtInChannels.value }
            else -> _builtInChannels.value
        }
        if (currentList.isEmpty()) return

        val currentIndex = currentList.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
        val nextIndex = (currentIndex + delta).mod(currentList.size)
        requestPlayChannel(currentList[nextIndex])
    }

    fun toggleFavorite(channelId: String) {
        val all = _builtInChannels.value + _customM3uChannels.value
        val chName = all.find { it.id == channelId }?.name ?: "Kanal"
        val updated = _favoriteOrder.value.toMutableList()
        if (updated.contains(channelId)) {
            updated.remove(channelId)
            _statusMessage.value = "$chName favorilerden çıkarıldı."
        } else {
            updated.add(channelId)
            _statusMessage.value = "$chName favorilere eklendi (#${updated.size})."
        }
        _favoriteOrder.value = updated
        repository.saveFavoriteOrderList(updated)
    }

    fun moveFavoriteChannel(channelId: String, delta: Int) {
        val current = _favoriteOrder.value.toMutableList()
        val idx = current.indexOf(channelId)
        if (idx == -1) return
        val targetIdx = (idx + delta).coerceIn(0, current.lastIndex)
        if (idx == targetIdx) return
        val item = current.removeAt(idx)
        current.add(targetIdx, item)
        _favoriteOrder.value = current
        repository.saveFavoriteOrderList(current)
        val all = _builtInChannels.value + _customM3uChannels.value
        val chName = all.find { it.id == channelId }?.name ?: "Kanal"
        _statusMessage.value = "$chName favori sırası #${targetIdx + 1} konumuna taşındı."
    }

    fun sortFavoritesBy(mode: String) {
        val all = _builtInChannels.value + _customM3uChannels.value
        val byId = all.associateBy { it.id }
        val currentFavChannels = _favoriteOrder.value.mapNotNull { byId[it] }
        if (currentFavChannels.size <= 1) return
        val sorted = when (mode) {
            "AZ" -> currentFavChannels.sortedBy { it.name.lowercase(Locale.getDefault()) }
            "NUMBER" -> currentFavChannels.sortedBy { it.number }
            "CATEGORY" -> currentFavChannels.sortedWith(compareBy<TvChannel> { it.category }.thenBy { it.number })
            else -> currentFavChannels
        }.map { it.id }
        _favoriteOrder.value = sorted
        repository.saveFavoriteOrderList(sorted)
        val modeLabel = when (mode) {
            "AZ" -> "İsme Göre (A-Z)"
            "NUMBER" -> "Kanal Numarasına Göre"
            else -> "Kategoriye Göre"
        }
        _statusMessage.value = "Favoriler $modeLabel otomatik sıralandı."
    }

    fun cycleSleepTimer() {
        val options = listOf(null, 30, 60, 90, 120)
        val currentIdx = options.indexOf(_sleepTimerMinutes.value)
        val next = options[(currentIdx + 1) % options.size]
        _sleepTimerMinutes.value = next
        sleepTimerJob?.cancel()

        if (next != null) {
            _statusMessage.value = "Uyku zamanlayıcısı: $next dk."
            sleepTimerJob = viewModelScope.launch {
                var remaining = next
                while (remaining > 0) {
                    delay(60_000L)
                    remaining -= 1
                    _sleepTimerMinutes.value = remaining
                }
                _sleepTimerMinutes.value = null
                _activePlayingChannel.value = null
                _pipChannel.value = null
            }
        } else {
            _statusMessage.value = "Uyku zamanlayıcısı kapatıldı."
        }
    }

    fun addRemoteM3uPlaylist(name: String, m3uUrl: String, epgUrl: String) {
        if (m3uUrl.isBlank()) return
        val cleanName = name.ifBlank { "Özel M3U Paketi (${_playlists.value.size + 1})" }
        val newSource = M3uPlaylistSource(
            name = cleanName.trim(),
            locationOrUrl = m3uUrl.trim(),
            epgXmlUrl = epgUrl.trim(),
            isLocalFile = false
        )
        val updated = _playlists.value + newSource
        _playlists.value = updated
        repository.savePlaylists(updated)
        reloadAllChannelsAndEpg()
    }

    fun addXtreamCodesAccount(name: String, serverUrl: String, username: String, password: String) {
        if (serverUrl.isBlank() || username.isBlank()) return
        val (m3uUrl, epgUrl) = M3uAndEpgParser.buildXtreamUrls(serverUrl, username, password)
        val displayName = name.ifBlank { "Xtream: $username" }
        addRemoteM3uPlaylist(displayName, m3uUrl, epgUrl)
    }

    fun importLocalM3uUri(uri: Uri) {
        viewModelScope.launch {
            _isLoadingM3u.value = true
            val imported = repository.importM3uFromUri(uri, "")
            if (imported != null) {
                val updated = _playlists.value + imported
                _playlists.value = updated
                repository.savePlaylists(updated)
                _statusMessage.value = "'${imported.name}' Özel M3U kanallarına eklendi."
                reloadAllChannelsAndEpg()
            } else {
                _isLoadingM3u.value = false
                _statusMessage.value = "Seçilen dosya geçerli bir M3U/M3U8 değil."
            }
        }
    }

    fun importLocalM3uFile(file: File) {
        viewModelScope.launch {
            _isLoadingM3u.value = true
            val imported = repository.importM3uFromFile(file, file.name)
            if (imported != null) {
                val updated = _playlists.value + imported
                _playlists.value = updated
                repository.savePlaylists(updated)
                _statusMessage.value = "'${imported.name}' Özel M3U kanallarına eklendi."
                reloadAllChannelsAndEpg()
            } else {
                _isLoadingM3u.value = false
                _statusMessage.value = "'${file.name}' içinde geçerli M3U/M3U8 yayın linki bulunamadı."
            }
        }
    }

    fun scanAllM3uFilesOnDevice(): List<File> = repository.scanDownloadFolderForM3uFiles()

    fun scanAndImportDownloadFolderM3u() {
        viewModelScope.launch {
            val foundFiles = repository.scanDownloadFolderForM3uFiles()
            if (foundFiles.isEmpty()) {
                _statusMessage.value = "Download klasöründe .m3u/.m3u8 bulunamadı."
                return@launch
            }
            val existingPaths = _playlists.value.map { it.locationOrUrl }.toSet()
            val newSources = foundFiles.filterNot { existingPaths.contains(it.absolutePath) }.map { file ->
                M3uPlaylistSource(
                    name = "Download: ${file.name}",
                    locationOrUrl = file.absolutePath,
                    isLocalFile = true
                )
            }
            if (newSources.isEmpty()) {
                _statusMessage.value = "Download klasöründeki M3U dosyaları zaten ekli."
            } else {
                val updated = _playlists.value + newSources
                _playlists.value = updated
                repository.savePlaylists(updated)
                _statusMessage.value = "${newSources.size} M3U dosyası eklendi!"
                reloadAllChannelsAndEpg()
            }
        }
    }

    fun removePlaylist(playlistId: String) {
        val updated = _playlists.value.filterNot { it.id == playlistId }
        _playlists.value = updated
        repository.savePlaylists(updated)
        reloadAllChannelsAndEpg()
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    /** Arayüzün (ör. Ayarlar) kısa bir bilgi/uyarı metni göstermesi için. */
    fun showStatusMessage(message: String) {
        _statusMessage.value = message
    }

    /** Dosyaya yedekleme (SAF) için tüm ayarların metin hali. */
    fun exportBackupText(): String = repository.exportBackupPayload()

    /** Dosyadan geri yükleme (SAF): favoriler, M3U listeleri, favori listeleri, kanal adları, rehber adresleri… */
    fun importBackupText(raw: String): Boolean {
        val ok = restoreFromBackupString(raw.trim())
        _statusMessage.value = if (ok) "Yedek başarıyla geri yüklendi." else "Yedek dosyası geçersiz veya bozuk."
        return ok
    }

    fun reloadAllChannelsAndEpg() {
        viewModelScope.launch(Dispatchers.Default) {
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val baseHourStart = cal.timeInMillis
            val currentHourOfDay = cal.get(Calendar.HOUR_OF_DAY)
            val oneHour = 60 * 60 * 1000L
            val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
            val preformattedWindows = (-1..3).map { offset ->
                val s = baseHourStart + (offset * oneHour)
                val e = s + oneHour
                "${timeFmt.format(Date(s))} - ${timeFmt.format(Date(e))}"
            }

            val currentBuiltInBase = repository.getBuiltInChannels()
            val builtInList = ArrayList<TvChannel>(currentBuiltInBase.size)
            val combinedEpg = HashMap<String, List<EpgProgram>>(currentBuiltInBase.size + 64)

            for (ch in currentBuiltInBase) {
                val sched = repository.generateScheduleForChannelFast(ch, baseHourStart, currentHourOfDay, preformattedWindows)
                combinedEpg[ch.epgChannelId] = sched
                val cur = sched[1]
                val nxt = sched[2]
                builtInList.add(
                    ch.copy(
                        currentProgramTitle = cur.title,
                        currentProgramTime = cur.timeWindowText,
                        nextProgramTitle = nxt.title,
                        currentProgress = cur.progressFraction(now),
                        isCustomM3u = false
                    )
                )
            }

            _builtInChannels.value = builtInList
            _epgMap.value = combinedEpg

            // [1] Arka planda GitHub üzerindeki dinamik `channel/channels.json` ve `channel/channels.m3u` dosyalarını kontrol et
            // [2] Ardından ACIPTV Türk kanallarını ana listeye ve yedek akışlara birleştir
            launch(Dispatchers.IO) {
                val remoteJsonChannels = repository.syncRemoteGitHubChannelsJson()
                val remoteRepoM3u = repository.syncRemoteGitHubRepoM3u()
                val baseAfterGitHub = buildList {
                    val base = remoteJsonChannels ?: repository.getStaticBuiltInChannels()
                    if (remoteRepoM3u.isNotBlank()) {
                        addAll(repository.mergeTurkishM3uIntoBuiltIn(base, remoteRepoM3u))
                    } else {
                        addAll(base)
                    }
                }
                if (baseAfterGitHub.isNotEmpty()) {
                    val jsonUpdatedList = ArrayList<TvChannel>(baseAfterGitHub.size)
                    for (ch in baseAfterGitHub) {
                        val sched = repository.generateScheduleForChannelFast(ch, baseHourStart, currentHourOfDay, preformattedWindows)
                        combinedEpg[ch.epgChannelId] = sched
                        val cur = sched[1]
                        val nxt = sched[2]
                        jsonUpdatedList.add(
                            ch.copy(
                                currentProgramTitle = cur.title,
                                currentProgramTime = cur.timeWindowText,
                                nextProgramTitle = nxt.title,
                                currentProgress = cur.progressFraction(now),
                                isCustomM3u = false
                            )
                        )
                    }
                    _builtInChannels.value = jsonUpdatedList
                    _epgMap.value = combinedEpg.toMap()
                }

                val remoteTrM3u = repository.fetchAndCacheAciptvTurkishM3u()
                if (remoteTrM3u.isNotBlank()) {
                    val mergedBase = repository.mergeTurkishM3uIntoBuiltIn(baseAfterGitHub.ifEmpty { repository.getStaticBuiltInChannels() }, remoteTrM3u)
                    val refreshedBuiltIn = ArrayList<TvChannel>(mergedBase.size)
                    for (ch in mergedBase) {
                        val sched = repository.generateScheduleForChannelFast(ch, baseHourStart, currentHourOfDay, preformattedWindows)
                        combinedEpg[ch.epgChannelId] = sched
                        val cur = sched[1]
                        val nxt = sched[2]
                        refreshedBuiltIn.add(
                            ch.copy(
                                currentProgramTitle = cur.title,
                                currentProgramTime = cur.timeWindowText,
                                nextProgramTitle = nxt.title,
                                currentProgress = cur.progressFraction(now),
                                isCustomM3u = false
                            )
                        )
                    }
                    _builtInChannels.value = refreshedBuiltIn
                    _epgMap.value = combinedEpg.toMap()
                }
                features.onChannelsReloaded()
            }

            if (_playlists.value.isEmpty()) {
                _customM3uChannels.value = emptyList()
                _isLoadingM3u.value = false
            } else {
                _isLoadingM3u.value = true
                val customList = mutableListOf<TvChannel>()
                var nextCustomNumber = 1
                for (source in _playlists.value) {
                    try {
                        val rawM3u = if (source.isLocalFile) {
                            val f = File(source.locationOrUrl)
                            if (f.exists() && f.length() < 12_000_000L) {
                                withContext(Dispatchers.IO) { f.readText() }
                            } else ""
                        } else {
                            M3uAndEpgParser.downloadText(source.locationOrUrl)
                        }
                        if (rawM3u.isNotBlank()) {
                            val parsed = M3uAndEpgParser.parseM3uContent(rawM3u, source.name, nextCustomNumber, true)
                            for (ch in parsed) {
                                val sched = repository.generateScheduleForChannelFast(ch, baseHourStart, currentHourOfDay, preformattedWindows)
                                combinedEpg[ch.epgChannelId] = sched
                                val cur = sched[1]
                                customList.add(
                                    ch.copy(
                                        currentProgramTitle = cur.title,
                                        currentProgramTime = cur.timeWindowText,
                                        currentProgress = cur.progressFraction(now),
                                        isCustomM3u = true
                                    )
                                )
                            }
                            nextCustomNumber += parsed.size
                        }
                    } catch (_: Throwable) {}
                }
                _customM3uChannels.value = customList
                _epgMap.value = combinedEpg.toMap()
                _isLoadingM3u.value = false
                features.onChannelsReloaded()
            }
        }
    }
}

class TvViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return TvViewModel(TvRepository(context.applicationContext), context.applicationContext) as T
    }
}
