package com.example.chromecasttv

import android.content.Context
import android.content.SharedPreferences
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Radyo listesi: kanal listesi gibi YALNIZCA şifreli dosyadan (`assets/radios.enc`, REDOX_ENC_V1) okunur.
 * Okunur liste ya da istasyon adı kuralları koda/depoya yazılmaz.
 */
object RadioRepository {
    private const val ASSET_ENC_FILENAME = "radios.enc"

    fun load(context: Context): List<TvChannel> {
        val raw = try {
            context.assets.open(ASSET_ENC_FILENAME).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: java.io.FileNotFoundException) {
            null          // asset hiç yok (herkese açık ekran görüntüsü derlemeleri)
        } catch (_: Throwable) {
            return emptyList()          // asset var ama okunamadı: sahte liste göstermek yerine boş bırak
        }
        // Asset yoksa uydurma örnek istasyonlar + demo istasyonlar kullanılır; varsa yalnızca gerçek liste (bozuksa boş)
        if (raw != null) return parse(raw)
        val sample = sampleStations()
        return sample + demoStations(sample.size + 1)
    }

    /** Uydurma örnek istasyonlar (gerçek yayın ya da gerçek marka değil): yalnızca arayüzü denemek içindir. */
    private fun sampleStations(): List<TvChannel> {
        val palette = listOf(
            0xFF1E3A8A to 0xFF3B82F6, 0xFF991B1B to 0xFFEF4444, 0xFF065F46 to 0xFF10B981, 0xFF5B21B6 to 0xFF8B5CF6,
            0xFF9A3412 to 0xFFF97316, 0xFF155E75 to 0xFF06B6D4, 0xFF831843 to 0xFFEC4899, 0xFF3F6212 to 0xFF84CC16
        )
        val entries = listOf(
            "Yıldız Pop" to "Pop", "Mavi Dalga" to "Pop", "Kıyı Hit" to "Pop", "Neşeli Pazar" to "Pop",
            "Gökyüzü Müzik" to "Pop", "Akşam Sefası" to "Pop", "Turuncu Hit" to "Pop", "Şehir Işıkları" to "Pop",
            "Anadolu Türküleri" to "Türkü & Halk", "Çınar Halk" to "Türkü & Halk", "Ova Radyo" to "Türkü & Halk",
            "Bozlak Saati" to "Türkü & Halk", "Yayla Sesleri" to "Türkü & Halk",
            "Gece Arabesk" to "Arabesk & Fantezi", "Yalnız Yürek" to "Arabesk & Fantezi", "Kader Hattı" to "Arabesk & Fantezi",
            "Altın Plak" to "Nostalji", "Eski Kasetler" to "Nostalji", "Seksenler Bahçesi" to "Nostalji", "Doksan Durağı" to "Nostalji",
            "Kaya Rock" to "Rock & Alternatif", "Alternatif Dalga" to "Rock & Alternatif", "Asi Gitar" to "Rock & Alternatif",
            "Kulüp Ritim" to "Dans & Elektronik", "Derin Ev" to "Dans & Elektronik", "Gece Dansı" to "Dans & Elektronik",
            "Nağme Salonu" to "Klasik & Sanat", "Saz ve Söz" to "Klasik & Sanat", "Klasik Köşe" to "Klasik & Sanat",
            "Gündem Radyo" to "Haber & Spor", "Spor Hattı" to "Haber & Spor", "Trafik ve Haber" to "Haber & Spor",
            "Huzur Radyo" to "Dini", "İlahi Saatler" to "Dini",
            "Ege Rüzgârı" to "Yerel", "Karadeniz Esintisi" to "Yerel"
        )
        return entries.mapIndexed { i, (name, category) ->
            val colors = palette[i % palette.size]
            val initials = name.split(' ').mapNotNull { it.firstOrNull() }.take(3).joinToString("").uppercase()
            val labels = buildList {
                add("Ana Yayın (MP3 128 kbps)")
                if (i % 3 == 0) add("Alternatif Akış 2 (AAC 64 kbps)")
                if (i % 5 == 0) add("Alternatif Akış 3 (HLS)")
            }
            TvChannel(
                id = "sample_${i + 1}",
                number = i + 1,
                name = name,
                shortLogoText = initials,
                brandPrimaryColor = colors.first,
                brandSecondaryColor = colors.second,
                category = category,
                logoUrl = "",
                sources = labels.mapIndexed { s, label -> StreamSource(label, "demo://sample_${i + 1}/$s", StreamType.PROGRESSIVE) },
                qualityBadge = "RADYO"
            )
        }
    }

    /** Şifreli içeriği çözer ve `radios` dizisini kanal modeline çevirir (rozet: RADYO). */
    fun parse(rawOrEncrypted: String): List<TvChannel> {
        val plain = DynamicChannelsJsonService.decryptPayloadIfNeeded(rawOrEncrypted)
        if (plain.isEmpty() || plain.first() != '{') return emptyList()
        return try {
            val array = JSONObject(plain).optJSONArray("radios") ?: return emptyList()
            DynamicChannelsJsonService.parseChannelsJson(array.toString())
        } catch (_: Throwable) {
            emptyList()
        }
    }

    /** Aşamaları denemek için üç örnek istasyon (gerçek yayın değil): yavaş bağlanan, yedek yayına geçen, çalışmayan. */
    private fun demoStations(startNumber: Int): List<TvChannel> {
        fun demo(id: String, number: Int, name: String, logo: String, c1: Long, c2: Long, labels: List<String>) = TvChannel(
            id = id,
            number = number,
            name = name,
            shortLogoText = logo,
            brandPrimaryColor = c1,
            brandSecondaryColor = c2,
            category = "Demo",
            logoUrl = "",
            sources = labels.mapIndexed { i, label -> StreamSource(label, "demo://$id/$i", StreamType.PROGRESSIVE) },
            qualityBadge = "RADYO"
        )
        return listOf(
            demo("demo_slow", startNumber, "Demo · Yavaş bağlanan", "DEMO", 0xFF3F6212, 0xFF84CC16, listOf("Ana Yayın (MP3 128 kbps)")),
            demo(
                "demo_failover", startNumber + 1, "Demo · Yedek yayına geçen", "YEDEK", 0xFF9A3412, 0xFFF97316,
                listOf("Ana Yayın (MP3 128 kbps)", "Alternatif Akış 2 (AAC 64 kbps)", "Alternatif Akış 3 (HLS)")
            ),
            demo("demo_dead", startNumber + 2, "Demo · Çalışmayan", "YOK", 0xFF991B1B, 0xFFEF4444, listOf("Ana Yayın (MP3 128 kbps)")),
            // Gerçek ses hattını emülatörde sınamak için: demo:// değil, gerçek (kişisel dinleme için serbest) SomaFM akışı
            TvChannel(
                id = "demo_live", number = startNumber + 3, name = "Demo · Gerçek yayın", shortLogoText = "CANLI",
                brandPrimaryColor = 0xFF155E75, brandSecondaryColor = 0xFF06B6D4, category = "Demo", logoUrl = "",
                sources = listOf(
                    StreamSource("Ana Yayın (MP3 128 kbps)", "https://ice1.somafm.com/groovesalad-128-mp3", StreamType.PROGRESSIVE),
                    StreamSource("Alternatif Akış 2 (MP3 128 kbps)", "https://ice2.somafm.com/groovesalad-128-mp3", StreamType.PROGRESSIVE)
                ),
                qualityBadge = "RADYO"
            )
        )
    }
}

/**
 * Radyo denetleyicisi: arayüz yalnızca bu nesneyle konuşur. Şifreli listedeki gerçek istasyonlar [RadioPlayer] ile
 * çalınır (bağlanma, çalma, duraklatma, kaynak yedekleme, ICY şarkı bilgisi, uyku zamanlayıcısı). `demo://` adresli
 * örnek istasyonlar (yalnız herkese açık ekran görüntüsü derlemelerinde) gerçek ses yerine örnek veriyle canlandırılır.
 */
object RadioHub {
    /** Kadranda gezindikten sonra yeni istasyonun çalmaya başlama gecikmesi. */
    const val TUNE_DELAY_MS = 700L

    /** Bir yayın bu sürede çalmaya başlamazsa sıradaki kaynağa geçilir. */
    private const val CONNECT_TIMEOUT_MS = 15_000L

    // Dosya adı ilk (demo) sürümden kalma; değiştirilirse mevcut favoriler kaybolur
    private const val PREFS = "redox_radio_demo"
    private const val KEY_FAVS = "favorites"
    private const val KEY_HIDDEN = "hidden"
    private const val KEY_STATIC = "static_noise"
    private const val KEY_HISTORY = "song_history"
    private const val KEY_LAST = "last_station"
    private const val KEY_AUTOSTART = "auto_start"
    private const val MAX_HISTORY = 100

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(RadioUiState())
    val state: StateFlow<RadioUiState> = _state.asStateFlow()

    private var started = false
    private var all: List<TvChannel> = emptyList()
    private var prefs: SharedPreferences? = null
    private var connectJob: Job? = null
    private var songJob: Job? = null
    private var tuneJob: Job? = null
    private var sleepJob: Job? = null
    private var toastJob: Job? = null
    private var sleepChoice: Int? = null
    private var appContext: Context? = null
    private var attemptJob: Job? = null
    private var listenerAttached = false

    /** Çalarken kopan yayın için aynı kaynağa yeniden bağlanma hakkı (istasyon/kaynak değişince sıfırlanır). */
    private var retriesLeft = 2

    private val sleepOptions = listOf<Int?>(null, 30, 60, 90, 120)

    // Örnek şarkı bilgisi (gerçek yayın verisi değildir)
    private val demoSongs = listOf(
        RadioSong("Tarkan", "Kuzu Kuzu"), RadioSong("Sezen Aksu", "Firuze"), RadioSong("Ajda Pekkan", "Yaz Yaz Yaz"),
        RadioSong("Mustafa Sandal", "Araba"), RadioSong("Kenan Doğulu", "Yaparım Bilirsin"), RadioSong("Sertab Erener", "Everyway That I Can"),
        RadioSong("Teoman", "Paramparça"), RadioSong("Mor ve Ötesi", "Bir Derdim Var"), RadioSong("Yaşar", "Ben Seni Sevdiğimi"),
        RadioSong("Hande Yener", "Kırmızı"), RadioSong("Murat Boz", "Aşkı Bulamam"), RadioSong("Sıla", "Yaz Gülüm")
    )

    /** Listeyi bir kez yükler (şifreli asset, ana iş parçacığı dışında). Çağrılar tekrarlanabilir. */
    fun ensureLoaded(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        appContext = app
        prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        scope.launch {
            val list = withContext(Dispatchers.IO) { RadioRepository.load(app) }
            all = list
            val favs = prefs?.getStringSet(KEY_FAVS, emptySet())?.toSet() ?: emptySet()
            val hidden = prefs?.getStringSet(KEY_HIDDEN, emptySet())?.toSet() ?: emptySet()
            val staticOn = prefs?.getBoolean(KEY_STATIC, true) ?: true
            val history = readHistory()
            val lastId = prefs?.getString(KEY_LAST, null)
            val autoStart = prefs?.getBoolean(KEY_AUTOSTART, false) ?: false
            _state.update {
                it.copy(
                    loaded = true, stations = list.filter { s -> s.id !in hidden }, favoriteIds = favs, hiddenIds = hidden,
                    staticOn = staticOn, history = history, lastStationId = lastId, autoStart = autoStart
                )
            }
            // İsteğe bağlı: uygulama açılınca son radyoyu çal (bir TV kanalı başlarsa radyo zaten susar)
            if (autoStart && lastId != null && list.any { it.id == lastId && it.id !in hidden }) play(lastId)
        }
    }

    // ------------------------------------------------------------------ liste
    fun selectCategory(name: String) {
        _state.update { it.copy(selectedCategory = name) }
    }

    fun toggleFavorite(id: String) {
        val added = id !in _state.value.favoriteIds
        _state.update { s -> s.copy(favoriteIds = if (added) s.favoriteIds + id else s.favoriteIds - id) }
        prefs?.edit()?.putStringSet(KEY_FAVS, _state.value.favoriteIds)?.apply()
        toast(if (added) "Favorilere eklendi" else "Favorilerden çıkarıldı")
    }

    /** İstasyonu listeden gizler; çalıyorsa sıradakine geçer. */
    fun hide(id: String) {
        val s = _state.value
        val wasCurrent = s.currentId == id
        val oldIdx = s.stations.indexOfFirst { it.id == id }.coerceAtLeast(0)
        val hidden = s.hiddenIds + id
        val visible = all.filter { it.id !in hidden }
        _state.update { it.copy(hiddenIds = hidden, stations = visible, tuningId = if (it.tuningId == id) null else it.tuningId) }
        prefs?.edit()?.putStringSet(KEY_HIDDEN, hidden)?.apply()
        if (wasCurrent) {
            if (visible.isEmpty()) stop() else play(visible[oldIdx.coerceAtMost(visible.size - 1)].id)
        }
        toast("İstasyon gizlendi")
    }

    fun restoreHidden() {
        _state.update { it.copy(hiddenIds = emptySet(), stations = all) }
        prefs?.edit()?.putStringSet(KEY_HIDDEN, emptySet())?.apply()
        toast("Gizlenen istasyonlar geri getirildi")
    }

    // ------------------------------------------------------------------ oynatma (demo)
    /** İstasyonu çalar. [openPlayer] true ise tam ekran oynatıcı da açılır. */
    fun play(id: String, openPlayer: Boolean = false) {
        val station = _state.value.stations.firstOrNull { it.id == id } ?: return
        tuneJob?.cancel()
        connectJob?.cancel()
        songJob?.cancel()
        attemptJob?.cancel()
        retriesLeft = 2
        prefs?.edit()?.putString(KEY_LAST, id)?.apply()
        _state.update {
            it.copy(
                currentId = id, tuningId = null, phase = RadioPhase.CONNECTING, sourceIndex = 0, failoverAttempt = 0,
                song = null, playerOpen = it.playerOpen || openPlayer, lastStationId = id
            )
        }
        if (!isDemo(station)) {
            realPlay(station, 0, failover = false)
            return
        }
        RadioStatic.stop()
        RadioPlayer.stop()
        connectJob = scope.launch {
            val behavior = demoBehavior(station)
            delay(behavior.connectMs)
            if (behavior.dead) {
                _state.update { it.copy(phase = RadioPhase.ERROR) }
                return@launch
            }
            if (behavior.failFirst) {
                _state.update { it.copy(phase = RadioPhase.FAILOVER, failoverAttempt = 2) }
                delay(6000L)
                _state.update { it.copy(sourceIndex = 1) }
            }
            _state.update { it.copy(phase = RadioPhase.PLAYING) }
            startSongs(station)
        }
    }

    // ------------------------------------------------------------------ gerçek ses
    private fun isDemo(station: TvChannel): Boolean = radioIsDemo(station)

    /** [index]. yayını çalar. [failover] true ise "yedek yayına geçiliyor" aşaması gösterilir. */
    private fun realPlay(station: TvChannel, index: Int, failover: Boolean) {
        val ctx = appContext ?: return
        val source = station.sources.getOrNull(index)
        if (source == null) {
            _state.update { it.copy(phase = RadioPhase.ERROR, song = null) }
            return
        }
        attachListener(ctx)
        _state.update {
            it.copy(
                sourceIndex = index, song = null,
                phase = if (failover) RadioPhase.FAILOVER else RadioPhase.CONNECTING,
                failoverAttempt = if (failover) index + 1 else 0
            )
        }
        RadioPlayer.play(ctx, source.url, station.name, radioSubtitle(station, index), station.logoUrl.takeIf { it.startsWith("http") })
        if (_state.value.staticOn) RadioStatic.start()      // yayın açılana kadar nostaljik cızırtı
        armTimeout()
    }

    /** Yayın [CONNECT_TIMEOUT_MS] içinde çalmaya başlamazsa sıradaki kaynağa geçilir. */
    private fun armTimeout() {
        attemptJob?.cancel()
        attemptJob = scope.launch {
            delay(CONNECT_TIMEOUT_MS)
            onSourceFailed()
        }
    }

    private fun markPlaying() {
        attemptJob?.cancel()
        RadioStatic.stop()
        // PAUSED da dahil: dışarıdan (bildirim, ses odağı) sürdürülen yayın hazır olunca arayüz "çalıyor" olur
        _state.update {
            if (it.phase == RadioPhase.CONNECTING || it.phase == RadioPhase.FAILOVER || it.phase == RadioPhase.PAUSED) it.copy(phase = RadioPhase.PLAYING) else it
        }
    }

    /** Geçerli yayın çalmadı ya da koptu: sıradaki kaynağa geçer; kaynak kalmadıysa hata gösterir. */
    private fun onSourceFailed() {
        val s = _state.value
        val station = s.current ?: return
        if (s.phase == RadioPhase.STOPPED || s.phase == RadioPhase.ERROR || s.phase == RadioPhase.PAUSED || isDemo(station)) return
        // Çalarken kopan yayın: önce aynı kaynağa yeniden bağlan (ağ kısa süre kesilmiş olabilir), sonra yedeğe geç
        if (s.phase == RadioPhase.PLAYING && retriesLeft > 0) {
            retriesLeft--
            realPlay(station, s.sourceIndex, failover = false)
            return
        }
        val next = s.sourceIndex + 1
        if (next < station.sources.size) {
            realPlay(station, next, failover = true)
        } else {
            attemptJob?.cancel()
            RadioStatic.stop()
            RadioPlayer.stop()
            _state.update { it.copy(phase = RadioPhase.ERROR, song = null) }
        }
    }

    /** Oynatıcı olaylarını duruma çevirir (bir kez bağlanır). */
    private fun attachListener(ctx: Context) {
        if (listenerAttached) return
        listenerAttached = true
        RadioPlayer.player(ctx).addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val p = RadioPlayer.peek() ?: return
                when (playbackState) {
                    Player.STATE_READY -> if (p.playWhenReady) markPlaying()
                    Player.STATE_ENDED -> onSourceFailed()
                    else -> Unit
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                // Hata raporlarında görünsün (Ayarlar > Hata raporu paylaş)
                val st = _state.value
                appContext?.let { CrashLogger.log(it, "Radyo hatası: ${st.current?.name} • kaynak ${st.sourceIndex + 1} • ${error.errorCodeName}") }
                onSourceFailed()
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                // Dışarıdan duraklatma/sürdürme (ses odağı kaybı, kulaklık çıkarıldı, bildirim düğmesi)
                val s = _state.value
                val station = s.current ?: return
                if (isDemo(station)) return
                val p = RadioPlayer.peek() ?: return
                val active = s.phase == RadioPhase.PLAYING || s.phase == RadioPhase.CONNECTING || s.phase == RadioPhase.FAILOVER
                if (!playWhenReady && active) {
                    // Bağlanırken de duraklatılabilir: zaman aşımı ve cızırtı durur, yedeğe geçilmez
                    attemptJob?.cancel()
                    RadioStatic.stop()
                    _state.update { it.copy(phase = RadioPhase.PAUSED) }
                } else if (playWhenReady && s.phase == RadioPhase.PAUSED) {
                    if (p.playbackState == Player.STATE_READY) {
                        _state.update { it.copy(phase = RadioPhase.PLAYING) }
                    } else {
                        _state.update { it.copy(phase = RadioPhase.CONNECTING) }
                        armTimeout()
                    }
                }
            }

            // ICY şarkı bilgisi doğrudan yayından (MediaMetadata birleşimine bağlı kalmadan)
            override fun onMetadata(metadata: Metadata) {
                val station = _state.value.current ?: return
                if (isDemo(station)) return
                for (i in 0 until metadata.length()) {
                    val icy = metadata.get(i) as? IcyInfo ?: continue
                    val text = icy.title?.trim().orEmpty()
                    if (text.isNotEmpty()) setSongFromText(text)
                }
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                val s = _state.value
                val station = s.current ?: return
                if (isDemo(station)) return
                val text = mediaMetadata.title?.toString()?.trim().orEmpty()
                if (text.isEmpty() || text == station.name) return
                setSongFromText(text)
            }
        })
    }

    /** "Sanatçı - Şarkı" metnini şarkı bilgisine çevirir (ayraç yoksa tamamı şarkı adı). */
    private fun setSongFromText(text: String) {
        val sep = text.indexOf(" - ")
        val song = if (sep > 0) RadioSong(artist = text.substring(0, sep).trim(), title = text.substring(sep + 3).trim()) else RadioSong(artist = "", title = text)
        if (_state.value.song == song) return
        val stationName = _state.value.current?.name.orEmpty()
        _state.update {
            val last = it.history.firstOrNull()
            val fresh = last == null || last.title != song.title || last.artist != song.artist || last.station != stationName
            it.copy(
                song = song,
                history = if (fresh) (listOf(RadioHistoryItem(System.currentTimeMillis(), stationName, song.artist, song.title)) + it.history).take(MAX_HISTORY) else it.history
            )
        }
        prefs?.edit()?.putString(KEY_HISTORY, _state.value.history.joinToString("\n") { h ->
            listOf(h.timeMillis.toString(), h.station, h.artist, h.title).joinToString("\t") { f -> f.replace('\t', ' ').replace('\n', ' ') }
        })?.apply()
    }

    private fun readHistory(): List<RadioHistoryItem> {
        val raw = prefs?.getString(KEY_HISTORY, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split('\n').mapNotNull { line ->
            val p = line.split('\t')
            if (p.size < 4) null else RadioHistoryItem(p[0].toLongOrNull() ?: 0L, p[1], p[2], p[3])
        }
    }

    /** Şarkı geçmişini siler. */
    fun clearHistory() {
        _state.update { it.copy(history = emptyList()) }
        prefs?.edit()?.remove(KEY_HISTORY)?.apply()
        toast("Şarkı geçmişi temizlendi")
    }

    private class DemoBehavior(val connectMs: Long, val failFirst: Boolean, val dead: Boolean)

    private fun demoBehavior(station: TvChannel): DemoBehavior = when (station.id) {
        "demo_slow" -> DemoBehavior(15_000L, failFirst = false, dead = false)
        "demo_failover" -> DemoBehavior(900L, failFirst = true, dead = false)
        "demo_dead" -> DemoBehavior(1_200L, failFirst = false, dead = true)
        else -> DemoBehavior(900L, failFirst = false, dead = false)
    }

    private fun startSongs(station: TvChannel) {
        songJob?.cancel()
        songJob = scope.launch {
            var i = (station.id.hashCode() and 0x7fffffff) % demoSongs.size
            while (true) {
                _state.update { it.copy(song = demoSongs[i % demoSongs.size]) }
                delay(24_000L)
                i++
            }
        }
    }

    fun togglePause() {
        val s = _state.value
        val real = s.current?.let { !isDemo(it) } == true
        when (s.phase) {
            RadioPhase.PLAYING -> {
                songJob?.cancel()
                if (real) RadioPlayer.peek()?.pause()
                _state.update { it.copy(phase = RadioPhase.PAUSED) }
            }
            RadioPhase.PAUSED -> {
                val station = s.current
                if (real && station != null) {
                    // Canlı yayında duraklatma sonrası en güncel ana dönmek için yayın yeniden hazırlanır
                    retriesLeft = 2
                    realPlay(station, s.sourceIndex, failover = false)
                } else {
                    _state.update { it.copy(phase = RadioPhase.PLAYING) }
                    station?.let { startSongs(it) }
                }
            }
            RadioPhase.ERROR -> s.currentId?.let { play(it) }
            // Tablet paneli boştayken: kadrandaki (yoksa ilk) istasyonu çalar
            RadioPhase.STOPPED -> (s.dialStation ?: s.stations.firstOrNull())?.let { play(it.id) }
            else -> Unit
        }
    }

    /** Kadrandaki OK: gezinilen istasyon varsa hemen çalar, yoksa duraklat / sürdür. */
    fun onOk() {
        if (_state.value.tuningId != null) confirmTune() else togglePause()
    }

    /** Kadranı çevirir (◄ ►): yeni istasyon [TUNE_DELAY_MS] sonra çalar; ses o ana kadar kesilmez. */
    fun zap(delta: Int) {
        val s = _state.value
        if (s.stations.isEmpty()) return
        val baseId = s.tuningId ?: s.currentId ?: s.stations.first().id
        val idx = s.stations.indexOfFirst { it.id == baseId }.coerceAtLeast(0)
        val n = s.stations.size
        tuneTo(s.stations[((idx + delta) % n + n) % n].id)
    }

    fun tuneTo(id: String) {
        _state.update { it.copy(tuningId = id) }
        tuneJob?.cancel()
        tuneJob = scope.launch {
            delay(TUNE_DELAY_MS)
            play(id)
        }
    }

    fun confirmTune() {
        val id = _state.value.tuningId ?: return
        tuneJob?.cancel()
        play(id)
    }

    /** Önceki / sonraki düğmeleri: sıradaki istasyonu hemen çalar. */
    fun stepNow(delta: Int) {
        val s = _state.value
        if (s.stations.isEmpty()) return
        val baseId = s.tuningId ?: s.currentId ?: s.stations.first().id
        val idx = s.stations.indexOfFirst { it.id == baseId }.coerceAtLeast(0)
        val n = s.stations.size
        play(s.stations[((idx + delta) % n + n) % n].id)
    }

    fun cycleSource() {
        val s = _state.value
        val station = s.current ?: return
        val n = station.sources.size
        if (n <= 1) {
            toast("Bu istasyonun tek yayını var")
            return
        }
        connectJob?.cancel()
        songJob?.cancel()
        if (!isDemo(station)) {
            retriesLeft = 2
            realPlay(station, (s.sourceIndex + 1) % n, failover = false)
            return
        }
        _state.update { it.copy(sourceIndex = (it.sourceIndex + 1) % n, phase = RadioPhase.CONNECTING) }
        connectJob = scope.launch {
            delay(900L)
            _state.update { it.copy(phase = RadioPhase.PLAYING) }
            startSongs(station)
        }
    }

    /** Alt sayfadan doğrudan yayın seçimi (demo: kısa süre yeniden bağlanır). */
    fun selectSource(index: Int) {
        val s = _state.value
        val station = s.current ?: return
        if (index !in station.sources.indices || index == s.sourceIndex) return
        connectJob?.cancel()
        songJob?.cancel()
        if (!isDemo(station)) {
            retriesLeft = 2
            realPlay(station, index, failover = false)
            return
        }
        _state.update { it.copy(sourceIndex = index, phase = RadioPhase.CONNECTING) }
        connectJob = scope.launch {
            delay(900L)
            _state.update { it.copy(phase = RadioPhase.PLAYING) }
            startSongs(station)
        }
    }

    fun toggleSpectrum() {
        _state.update { it.copy(spectrumOn = !it.spectrumOn) }
    }

    /** Uygulama açılınca son radyoyu otomatik çalma (kalıcı). */
    fun toggleAutoStart() {
        val on = !_state.value.autoStart
        _state.update { it.copy(autoStart = on) }
        prefs?.edit()?.putBoolean(KEY_AUTOSTART, on)?.apply()
        toast(if (on) "Açılışta son radyo çalacak" else "Açılışta radyo başlamayacak")
    }

    /** Telefon kumandası için: çalan istasyon, şarkı ve (isteğe bağlı) istasyon listesi. */
    fun remoteState(includeStations: Boolean): org.json.JSONObject {
        val s = _state.value
        val o = JSONObject()
        o.put("active", s.isActive)
        o.put("phase", s.phase.name)
        o.put("stationId", s.currentId ?: "")
        o.put("station", s.current?.name ?: "")
        o.put("song", s.song?.let { if (it.artist.isBlank()) it.title else it.artist + " - " + it.title } ?: "")
        if (includeStations) {
            val arr = org.json.JSONArray()
            for (st in s.stations.take(200)) arr.put(JSONObject().put("id", st.id).put("name", st.name))
            o.put("stations", arr)
        }
        return o
    }

    /** "Bu yayını bildir": yayın bilgisini günlüğe yazar ve paylaşılabilir rapor metnini döndürür. */
    fun brokenStreamReport(context: Context, station: TvChannel): String {
        val s = _state.value
        val src = station.sources.getOrNull(s.sourceIndex)?.url ?: station.sources.firstOrNull()?.url ?: "-"
        val line = "Radyo yayın bildirimi: " + station.name + " • kaynak " + (s.sourceIndex + 1) + "/" + station.sources.size + " • " + s.phase.name + " • " + src
        CrashLogger.log(context, line)
        return line + "\n\n" + CrashLogger.report(context)
    }

    /** İstasyon değişirken çalan radyo cızırtısını açar/kapatır (kalıcı). */
    fun toggleStatic() {
        val on = !_state.value.staticOn
        _state.update { it.copy(staticOn = on) }
        prefs?.edit()?.putBoolean(KEY_STATIC, on)?.apply()
        if (!on) RadioStatic.stop()
        toast(if (on) "Radyo cızırtısı açık" else "Radyo cızırtısı kapalı")
    }

    // ------------------------------------------------------------------ uyku zamanlayıcısı
    fun cycleSleep() {
        val idx = sleepOptions.indexOf(sleepChoice)
        setSleep(sleepOptions[(idx + 1) % sleepOptions.size])
    }

    fun setSleep(minutes: Int?) {
        sleepJob?.cancel()
        sleepChoice = minutes
        _state.update { it.copy(sleepMinutes = minutes, sleepChoice = minutes) }
        if (minutes != null) {
            sleepJob = scope.launch {
                var left: Int = minutes
                while (left > 0) {
                    delay(60_000L)
                    left--
                    _state.update { it.copy(sleepMinutes = left) }
                }
                stop()
            }
        }
    }

    // ------------------------------------------------------------------ oynatıcı ekranı / durdurma
    fun openPlayer() {
        if (_state.value.isActive) _state.update { it.copy(playerOpen = true) }
    }

    fun closePlayer() {
        _state.update { it.copy(playerOpen = false) }
    }

    fun stop() {
        tuneJob?.cancel()
        connectJob?.cancel()
        songJob?.cancel()
        sleepJob?.cancel()
        attemptJob?.cancel()
        RadioStatic.stop()
        RadioPlayer.stop()
        sleepChoice = null
        _state.update {
            it.copy(
                currentId = null, tuningId = null, phase = RadioPhase.STOPPED, song = null, sourceIndex = 0,
                failoverAttempt = 0, sleepMinutes = null, sleepChoice = null, playerOpen = false
            )
        }
    }

    /** Kısa süreli bilgi mesajı (favori eklendi, gizlendi …). */
    fun toast(message: String) {
        toastJob?.cancel()
        _state.update { it.copy(toast = message) }
        toastJob = scope.launch {
            delay(2400L)
            _state.update { it.copy(toast = null) }
        }
    }
}
