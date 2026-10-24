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

class TvRepository(private val appContext: Context) {
    private val prefs = appContext.getSharedPreferences("redoxtv_clean_prefs_v5", Context.MODE_PRIVATE)

    fun loadFavoriteOrderList(): List<String> {
        val raw = prefs.getString("fav_order_csv", "") ?: ""
        return raw.split(",").filter { it.isNotBlank() }.distinct()
    }

    fun saveFavoriteOrderList(ids: List<String>) {
        prefs.edit().putString("fav_order_csv", ids.distinct().joinToString(",")).apply()
    }

    fun loadHiddenChannelIds(): Set<String> {
        val raw = prefs.getString("hidden_ids_csv", "") ?: ""
        return raw.split(",").filter { it.isNotBlank() }.toSet()
    }

    fun saveHiddenChannelIds(ids: Set<String>) {
        prefs.edit().putString("hidden_ids_csv", ids.joinToString(",")).apply()
    }

    fun loadAutoStartLastChannel(): Boolean = prefs.getBoolean("auto_start_last_channel", false)

    fun saveAutoStartLastChannel(enabled: Boolean) {
        prefs.edit().putBoolean("auto_start_last_channel", enabled).apply()
    }

    fun loadPreferredSources(): Map<String, Int> {
        val raw = prefs.getString("preferred_sources_map", "") ?: ""
        if (raw.isBlank()) return emptyMap()
        val map = mutableMapOf<String, Int>()
        raw.split(";").forEach { entry ->
            val parts = entry.split("=")
            if (parts.size == 2) {
                val idx = parts[1].toIntOrNull()
                if (parts[0].isNotBlank() && idx != null) {
                    map[parts[0]] = idx
                }
            }
        }
        return map
    }

    fun savePreferredSources(map: Map<String, Int>) {
        val raw = map.entries.joinToString(";") { "${it.key}=${it.value}" }
        prefs.edit().putString("preferred_sources_map", raw).apply()
    }

    fun loadPreferredScales(): Map<String, String> {
        val raw = prefs.getString("preferred_scales_map", "") ?: ""
        if (raw.isBlank()) return emptyMap()
        val map = mutableMapOf<String, String>()
        raw.split(";").forEach { entry ->
            val parts = entry.split("=")
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                map[parts[0]] = parts[1]
            }
        }
        return map
    }

    fun savePreferredScales(map: Map<String, String>) {
        val raw = map.entries.joinToString(";") { "${it.key}=${it.value}" }
        prefs.edit().putString("preferred_scales_map", raw).apply()
    }

    fun exportBackupPayload(): String {
        val favs = prefs.getString("fav_order_csv", "") ?: ""
        val locked = prefs.getString("locked_ids_csv", "") ?: ""
        val hidden = prefs.getString("hidden_ids_csv", "") ?: ""
        val autoStart = loadAutoStartLastChannel()
        val prefSources = prefs.getString("preferred_sources_map", "") ?: ""
        val prefScales = prefs.getString("preferred_scales_map", "") ?: ""
        val playlists = prefs.getString("custom_m3u_playlists_str", "") ?: ""
        return buildString {
            appendLine("REDOX_BACKUP_V9")
            appendLine("FAVS:$favs")
            appendLine("LOCKED:$locked")
            appendLine("HIDDEN:$hidden")
            // Ebeveyn PIN'i güvenlik için yedeğe YAZILMAZ (eski yedeklerdeki "PIN:" satırı geri yüklemede hâlâ okunur).
            appendLine("AUTO_START:$autoStart")
            appendLine("PREF_SOURCES:$prefSources")
            appendLine("PREF_SCALES:$prefScales")
            appendLine("PLAYLISTS:$playlists")
            appendLine("FAV_LISTS:${FavoriteListsStore(appContext).rawJson()}")
            appendLine("CH_OVERRIDES:${ChannelOverridesStore(appContext).rawJson()}")
            appendLine("EPG_URLS:${FeaturePrefsStore(appContext).load().epgUrls.joinToString("|")}")
        }
    }

    fun importBackupPayload(payload: String): Boolean {
        val trimmed = payload.trim()
        if (!trimmed.startsWith("REDOX_BACKUP_V9")) return false
        val editor = prefs.edit()
        trimmed.lines().drop(1).forEach { line ->
            val idx = line.indexOf(':')
            if (idx > 0) {
                val key = line.substring(0, idx).trim()
                val value = line.substring(idx + 1).trim()
                when (key) {
                    "FAVS" -> editor.putString("fav_order_csv", value)
                    "LOCKED" -> editor.putString("locked_ids_csv", value)
                    "HIDDEN" -> editor.putString("hidden_ids_csv", value)
                    "PIN" -> if (value.length == 4) editor.putString("parental_pin_code", value)
                    "AUTO_START" -> editor.putBoolean("auto_start_last_channel", value.toBoolean())
                    "PREF_SOURCES" -> editor.putString("preferred_sources_map", value)
                    "PREF_SCALES" -> editor.putString("preferred_scales_map", value)
                    "PLAYLISTS" -> editor.putString("custom_m3u_playlists_str", value)
                    "FAV_LISTS" -> if (value.isNotBlank()) FavoriteListsStore(appContext).restoreRawJson(value)
                    "CH_OVERRIDES" -> if (value.isNotBlank()) ChannelOverridesStore(appContext).restoreRawJson(value)
                    "EPG_URLS" -> {
                        val prefsStore = FeaturePrefsStore(appContext)
                        val urls = value.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                        prefsStore.save(prefsStore.load().copy(epgUrls = urls))
                    }
                }
            }
        }
        editor.apply()
        return true
    }

    fun saveBackupFileToDevice(): String? {
        return try {
            val payload = exportBackupPayload()
            val internalFile = File(appContext.filesDir, "redox_tv_backup.txt")
            internalFile.writeText(payload)
            try {
                val dlDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (dlDir != null && (dlDir.exists() || dlDir.mkdirs())) {
                    val dlFile = File(dlDir, "redox_tv_backup.txt")
                    dlFile.writeText(payload)
                    return dlFile.absolutePath
                }
            } catch (_: Exception) {}
            internalFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun restoreBackupFileFromDevice(): Boolean {
        return try {
            val candidates = listOfNotNull(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.let { File(it, "redox_tv_backup.txt") },
                File("/sdcard/Download/redox_tv_backup.txt"),
                File("/storage/emulated/0/Download/redox_tv_backup.txt"),
                File(appContext.filesDir, "redox_tv_backup.txt")
            )
            val existing = candidates.firstOrNull { it.exists() && it.isFile } ?: return false
            importBackupPayload(existing.readText())
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun probeSingleUrlWithHardTimeout(urlStr: String, timeoutMs: Long = 1_400L): Boolean {
        if (urlStr.isBlank()) return false
        // SHOUTcast / Icecast radyo akışları (; ile bitenler) sonsuz byte gönderdiği için
        // HttpURLConnection.responseCode içinde takılı kalmasını önle!
        if (urlStr.endsWith(";") || urlStr.startsWith("rtmp://", ignoreCase = true) || urlStr.startsWith("rtsp://", ignoreCase = true)) {
            return true
        }
        val deferred = kotlinx.coroutines.CompletableDeferred<Boolean>()
        var connRef: HttpURLConnection? = null
        val worker = Thread {
            try {
                // Yayın sağlık testi: bölgesel yayıncıların sertifikaları için yalnızca bu bağlantıda gevşek TLS
                val c = HttpSecurity.open(urlStr, true, 1_200, 1_200).apply {
                    connectTimeout = 1_200
                    readTimeout = 1_200
                    instanceFollowRedirects = true
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/125.0.0.0")
                    setRequestProperty("Range", "bytes=0-128")
                    setRequestProperty("Connection", "close")
                }
                connRef = c
                val code = c.responseCode
                deferred.complete(code in 200..399)
            } catch (_: Throwable) {
                deferred.complete(false)
            } finally {
                try { connRef?.disconnect() } catch (_: Throwable) {}
            }
        }.apply {
            isDaemon = true
            start()
        }

        val ok = kotlinx.coroutines.withTimeoutOrNull(timeoutMs) {
            deferred.await()
        } ?: false

        if (!deferred.isCompleted) {
            try { connRef?.disconnect() } catch (_: Throwable) {}
            try { worker.interrupt() } catch (_: Throwable) {}
        }
        return ok
    }

    suspend fun probeChannelStreamHealth(channel: TvChannel): Boolean = withContext(Dispatchers.IO) {
        // Her kanal için en fazla ilk 2 kaynağı kesin zaman aşımıyla (1.4 sn) test et;
        // böylece 308+ kanal taraması asla DNS/SSL veya radyo akışında takılı kalmaz!
        for (src in channel.sources.take(2)) {
            if (probeSingleUrlWithHardTimeout(src.url.trim(), 1_400L)) {
                return@withContext true
            }
        }
        false
    }

    fun loadLockedChannelIds(): Set<String> {
        val raw = prefs.getString("locked_ids_csv", "") ?: ""
        return raw.split(",").filter { it.isNotBlank() }.toSet()
    }

    fun saveLockedChannelIds(ids: Set<String>) {
        prefs.edit().putString("locked_ids_csv", ids.joinToString(",")).apply()
    }

    fun loadBrokenChannelIds(): Set<String> {
        val raw = prefs.getString("broken_ids_csv", "") ?: ""
        return raw.split(",").filter { it.isNotBlank() }.toSet()
    }

    fun saveBrokenChannelIds(ids: Set<String>) {
        prefs.edit().putString("broken_ids_csv", ids.joinToString(",")).apply()
    }

    fun loadParentalPin(): String = prefs.getString("parental_pin_code", "0000") ?: "0000"
    fun saveParentalPin(newPin: String) = prefs.edit().putString("parental_pin_code", newPin).apply()

    fun loadRecentChannelIds(): List<String> {
        val raw = prefs.getString("recent_channel_ids", "") ?: ""
        return raw.split(",").filter { it.isNotBlank() }
    }

    fun pushRecentChannelId(channelId: String): List<String> {
        val current = loadRecentChannelIds().toMutableList()
        current.remove(channelId)
        current.add(0, channelId)
        val trimmed = current.take(10)
        prefs.edit().putString("recent_channel_ids", trimmed.joinToString(",")).apply()
        return trimmed
    }

    fun loadSavedPlaylists(): List<M3uPlaylistSource> {
        val raw = prefs.getString("custom_m3u_playlists_str", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(";;;").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size >= 4) {
                M3uPlaylistSource(
                    id = parts[0],
                    name = parts[1],
                    locationOrUrl = parts[2],
                    epgXmlUrl = parts[3],
                    isLocalFile = parts.getOrElse(4) { "false" }.toBoolean()
                )
            } else null
        }
    }

    fun savePlaylists(playlists: List<M3uPlaylistSource>) {
        val serialized = playlists.joinToString(";;;") {
            "${it.id}|${it.name}|${it.locationOrUrl}|${it.epgXmlUrl}|${it.isLocalFile}"
        }
        prefs.edit().putString("custom_m3u_playlists_str", serialized).apply()
    }

    suspend fun saveRawM3uTextAsLocalPlaylist(rawContent: String, customName: String): M3uPlaylistSource? = withContext(Dispatchers.IO) {
        try {
            if (!rawContent.contains("#EXTINF", ignoreCase = true) && !rawContent.contains("http", ignoreCase = true)) {
                return@withContext null
            }
            val playlistDir = File(appContext.filesDir, "imported_playlists")
            playlistDir.mkdirs()
            val fileId = UUID.randomUUID().toString()
            val targetFile = File(playlistDir, "$fileId.m3u")
            targetFile.writeText(rawContent)

            M3uPlaylistSource(
                id = fileId,
                name = customName.ifBlank { "Telefondan Gönderilen M3U" },
                locationOrUrl = targetFile.absolutePath,
                isLocalFile = true
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun importM3uFromUri(uri: Uri, customName: String): M3uPlaylistSource? = withContext(Dispatchers.IO) {
        try {
            val playlistDir = File(appContext.filesDir, "imported_playlists")
            playlistDir.mkdirs()
            val fileId = UUID.randomUUID().toString()
            val targetFile = File(playlistDir, "$fileId.m3u")
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            val displayName = customName.ifBlank {
                uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "Yerel M3U Dosyası"
            }
            M3uPlaylistSource(
                id = fileId,
                name = displayName,
                locationOrUrl = targetFile.absolutePath,
                isLocalFile = true
            )
        } catch (_: Exception) {
            null
        }
    }


    suspend fun importM3uFromFile(sourceFile: File, customName: String = ""): M3uPlaylistSource? = withContext(Dispatchers.IO) {
        try {
            if (!sourceFile.exists() || !sourceFile.isFile) return@withContext null
            val rawText = sourceFile.readText()
            if (!rawText.contains("#EXTINF", ignoreCase = true) && !rawText.contains("http", ignoreCase = true)) {
                return@withContext null
            }
            val playlistDir = File(appContext.filesDir, "imported_playlists")
            playlistDir.mkdirs()
            val fileId = UUID.randomUUID().toString()
            val targetFile = File(playlistDir, "$fileId.m3u")
            targetFile.writeText(rawText)

            val displayName = customName.ifBlank { sourceFile.name }
            M3uPlaylistSource(
                id = fileId,
                name = displayName,
                locationOrUrl = targetFile.absolutePath,
                isLocalFile = true
            )
        } catch (_: Throwable) {
            null
        }
    }

    fun scanDownloadFolderForM3uFiles(): List<File> {
        val result = mutableListOf<File>()
        val seenPaths = HashSet<String>()

        fun collectFromDir(dir: File?, depth: Int = 0) {
            if (dir == null || depth > 2) return
            try {
                if (!dir.exists() || !dir.isDirectory || !dir.canRead()) return
                val children = dir.listFiles() ?: return
                for (f in children) {
                    if (f.isFile) {
                        val lower = f.name.lowercase(Locale.US)
                        if (lower.endsWith(".m3u") || lower.endsWith(".m3u8")) {
                            val abs = f.absolutePath
                            if (seenPaths.add(abs)) {
                                result.add(f)
                            }
                        }
                    } else if (f.isDirectory && depth < 1 && !f.name.startsWith(".") && f.name != "Android") {
                        collectFromDir(f, depth + 1)
                    }
                }
            } catch (_: Exception) {}
        }

        try {
            val candidateDirs = buildList {
                add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
                add(File("/sdcard/Download"))
                add(File("/sdcard/Downloads"))
                add(File("/storage/emulated/0/Download"))
                add(File("/storage/emulated/0/Downloads"))
                add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS))
                add(File("/sdcard/Documents"))
                add(File("/storage/emulated/0/Documents"))
                add(Environment.getExternalStorageDirectory())
                add(File("/sdcard"))
                add(File("/storage/emulated/0"))
                add(appContext.getExternalFilesDir(null))
                try {
                    File("/storage").listFiles()?.forEach { usbDir ->
                        if (usbDir.isDirectory && usbDir.name != "self" && usbDir.name != "emulated") {
                            add(usbDir)
                            add(File(usbDir, "Download"))
                        }
                    }
                } catch (_: Exception) {}
            }
            for (dir in candidateDirs) {
                collectFromDir(dir, 0)
            }
        } catch (_: Exception) {}
        return result
    }

    private fun ch(
        id: String,
        number: Int,
        name: String,
        logoText: String,
        c1: Long,
        c2: Long,
        cat: String,
        badge: String = "FHD",
        logoUrl: String = "",
        sources: List<StreamSource>
    ): TvChannel = TvChannel(
        id = id,
        number = number,
        name = name,
        shortLogoText = logoText,
        brandPrimaryColor = c1,
        brandSecondaryColor = c2,
        category = cat,
        logoUrl = logoUrl,
        sources = sources,
        qualityBadge = badge,
        isCustomM3u = false
    )


    @Volatile
    private var cachedStaticChannels: List<TvChannel>? = null

    fun loadCustomChannelsJsonUrl(): String =
        prefs.getString("custom_channels_json_url", "")?.trim().orEmpty()

    fun saveCustomChannelsJsonUrl(url: String) {
        prefs.edit().putString("custom_channels_json_url", url.trim()).apply()
    }

    fun getStaticBuiltInChannels(forceReload: Boolean = false): List<TvChannel> {
        val current = cachedStaticChannels
        if (!forceReload && !current.isNullOrEmpty()) return current
        val loaded = DynamicChannelsJsonService.loadInstantChannels(appContext)
        cachedStaticChannels = loaded
        return loaded
    }

    suspend fun syncRemoteGitHubChannelsJson(): List<TvChannel>? {
        val updated = DynamicChannelsJsonService.fetchAndCacheRemoteChannelsJson(
            context = appContext,
            customUrl = loadCustomChannelsJsonUrl()
        )
        if (!updated.isNullOrEmpty()) {
            cachedStaticChannels = updated
        }
        return updated
    }

    fun getBuiltInChannels(): List<TvChannel> {
        deleteLegacyThirdPartyCache()
        return getStaticBuiltInChannels()
    }

    /** Eski sürümlerde indirilen üçüncü taraf (ACIPTV) liste önbelleği artık kullanılmaz; varsa silinir. */
    private fun deleteLegacyThirdPartyCache() {
        try {
            File(appContext.filesDir, "aciptv_turkish_cached.m3u").delete()
        } catch (_: Exception) {
        }
    }


    fun generateScheduleForChannelFast(
        channel: TvChannel,
        baseHourStart: Long,
        currentHourOfDay: Int,
        preformattedWindows: List<String>
    ): List<EpgProgram> {
        val oneHour = 60 * 60 * 1000L
        val templates = when (channel.category.lowercase()) {
            "ulusal" -> listOf(
                "Ana Haber & Gündem" to "Türkiye ve dünyadan sıcak gelişmeler.",
                "Prime Time Dizisi" to "Yüksek çözünürlüklü ana yayın kuşağı.",
                "Gündüz Kuşağı & Yaşam" to "Stüdyo konukları ve güncel sohbetler.",
                "Gece Sineması" to "Ödüllü yerli ve yabancı sinema seçkisi."
            )
            "haber" -> listOf(
                "Canlı Haber Maratonu" to "Son dakika gelişmeleri ve canlı bağlantılar.",
                "Ekonomi ve Piyasalar" to "Borsa, döviz, altın ve piyasa analizleri.",
                "Gündem Özel Tartışma" to "Uzman konuklarla derinlemesine analizler.",
                "Gece Bülteni" to "Günün özeti ve yarının manşetleri."
            )
            "spor" -> listOf(
                "Stadyum Canlı & Maç Önü" to "Süper Lig ve Avrupa kupalarından canlı yayın.",
                "Maç Özeti & Teknik Analiz" to "Haftanın golleri ve değerlendirmeler.",
                "Transfer Raporu" to "Kulüplerden son dakika gelişmeleri.",
                "Dünya Spor Kuşağı" to "Uluslararası turnuvalar ve özetler."
            )
            "belgesel" -> listOf(
                "Vahşi Yaşam Atlası (4K)" to "Doğa harikaları ve nadir türlerin izinde.",
                "Uzay ve Bilim Kuşağı" to "Derin uzay gözlemleri ve keşifler.",
                "Antik Medeniyetler" to "Tarihin gizemli yapıları ve kazılar.",
                "Mühendislik Harikaları" to "Dev yapılar ve geleceğin teknolojileri."
            )
            "müzik" -> listOf(
                "Top 20 Hit Klipler" to "Haftanın en çok dinlenen video klipleri.",
                "Akustik Stüdyo" to "Canlı performanslar ve unutulmaz şarkılar.",
                "Nostalji Kuşağı" to "Geçmişten bugüne iz bırakan şarkılar.",
                "Gece Ritmi Non-Stop" to "Kesintisiz müzik performansları."
            )
            else -> listOf(
                "${channel.name} Canlı Yayın" to "${channel.category} kategorisinde kesintisiz yayın.",
                "Özel Kuşak" to "İzleyiciler için hazırlanan güncel içerikler.",
                "Günün Seçkisi" to "${channel.name} ekranlarında öne çıkan program.",
                "Gece Kuşağı" to "Günün özeti ve tekrar bölümler."
            )
        }

        return (-1..3).mapIndexed { i, offset ->
            val slotStart = baseHourStart + (offset * oneHour)
            val slotEnd = slotStart + oneHour
            val idx = (currentHourOfDay + offset).mod(templates.size)
            val (title, desc) = templates[idx]
            EpgProgram(
                channelId = channel.epgChannelId,
                title = title,
                description = desc,
                category = channel.category,
                startTimeMillis = slotStart,
                endTimeMillis = slotEnd,
                timeWindowText = preformattedWindows[i]
            )
        }
    }
}
