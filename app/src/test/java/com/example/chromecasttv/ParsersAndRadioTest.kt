package com.example.chromecasttv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Saf Kotlin mantığı için hızlı JVM testleri (Android sınıfı gerektirmeyen parçalar). */
class ParsersAndRadioTest {

    private fun ch(id: String, name: String, category: String = "Pop", vararg urls: String) = TvChannel(
        id = id, number = 1, name = name, shortLogoText = name.take(2), brandPrimaryColor = 0xFF111111, brandSecondaryColor = 0xFF222222,
        category = category, sources = urls.map { StreamSource("Ana Yayın", it) }
    )

    // ---------------------------------------------------------------- M3U
    @Test
    fun m3u_sameNameBecomesAlternativeSource() {
        val raw = """
            #EXTM3U
            #EXTINF:-1 group-title="Haber",Örnek Haber
            https://ornek.example/a/playlist.m3u8
            #EXTINF:-1 group-title="Haber",Örnek Haber
            https://ornek.example/b/playlist.m3u8
        """.trimIndent()
        val channels = M3uAndEpgParser.parseM3uContent(raw, "Deneme")
        assertEquals(1, channels.size)
        assertEquals(2, channels[0].sources.size)
        assertEquals("Haber", channels[0].category)
    }

    @Test
    fun m3u_detectsStreamTypes() {
        val raw = """
            #EXTM3U
            #EXTINF:-1,Radyo Bir
            https://ornek.example:8000/yayin;
            #EXTINF:-1,Dash Kanal
            https://ornek.example/manifest.mpd
            #EXTINF:-1,Hls Kanal
            https://ornek.example/index.m3u8
        """.trimIndent()
        val byName = M3uAndEpgParser.parseM3uContent(raw, "Deneme").associateBy { it.name }
        assertEquals(StreamType.PROGRESSIVE, byName.getValue("Radyo Bir").sources[0].type)
        assertEquals(StreamType.DASH, byName.getValue("Dash Kanal").sources[0].type)
        assertEquals(StreamType.HLS, byName.getValue("Hls Kanal").sources[0].type)
    }

    @Test
    fun m3u_skipsLinesThatAreNotStreams() {
        val raw = "#EXTM3U\n#EXTINF:-1,Bozuk\nftp://ornek.example/x\n#EXTINF:-1,Sağlam\nhttp://ornek.example/s.m3u8\n"
        val channels = M3uAndEpgParser.parseM3uContent(raw, "Deneme")
        assertEquals(listOf("Sağlam"), channels.map { it.name })
    }

    // ---------------------------------------------------------------- EPG
    @Test
    fun xmltvTime_appliesOffset() {
        val utc = XmltvParser.parseTime("20261024000800 +0000")
        val istanbul = XmltvParser.parseTime("20261024030800 +0300")
        assertEquals(1792800480000L, utc)
        assertEquals(utc, istanbul)
        assertEquals(0L, XmltvParser.parseTime("bozuk"))
    }

    @Test
    fun epgMatcher_ignoresPrefixAndNoiseWords() {
        assertEquals(EpgMatcher.normalize("A Haber"), EpgMatcher.normalize("TR - A Haber"))
        assertEquals(EpgMatcher.normalize("TV8"), EpgMatcher.normalize("TV8 HD"))
    }

    // ---------------------------------------------------------------- Radyo
    @Test
    fun radio_statusText_perPhase() {
        val station = ch("r1", "Deneme FM", "Pop", "https://ornek.example/a", "https://ornek.example/b")
        val base = RadioUiState(stations = listOf(station), currentId = "r1")
        assertEquals("Bu istasyon şu an çalışmıyor", radioStatusText(base.copy(phase = RadioPhase.ERROR)))
        assertEquals("Yayına bağlanılıyor…", radioStatusText(base.copy(phase = RadioPhase.CONNECTING)))
        assertEquals("Canlı yayın", radioStatusText(base.copy(phase = RadioPhase.PLAYING)))
        assertEquals("Şarkı", radioStatusText(base.copy(phase = RadioPhase.PLAYING, song = RadioSong("", "Şarkı"))))
        assertEquals("Şarkı — Sanatçı", radioStatusText(base.copy(phase = RadioPhase.PLAYING, song = RadioSong("Sanatçı", "Şarkı"))))
        assertTrue(radioStatusText(base.copy(phase = RadioPhase.FAILOVER, failoverAttempt = 2), compact = true).startsWith("Yedek yayına"))
    }

    @Test
    fun radio_filterAndDemoDetection() {
        val a = ch("a", "Pop Bir", "Pop", "https://ornek.example/a")
        val b = ch("b", "Türkü Bir", "Türkü & Halk", "demo://b/0")
        val stations = listOf(a, b)
        assertEquals(listOf(a), radioFilter(stations, emptySet(), "Pop"))
        assertEquals(listOf(b), radioFilter(stations, setOf("b"), RADIO_CAT_FAVORITES))
        assertEquals(2, radioFilter(stations, emptySet(), RADIO_CAT_ALL).size)
        assertFalse(radioIsDemo(a))
        assertTrue(radioIsDemo(b))
    }

    @Test
    fun newChannels_summaryShortens() {
        val list = (1..7).map { ch("c$it", "Kanal $it", "Ulusal", "https://ornek.example/$it") }
        assertEquals("Kanal 1, Kanal 2 ve 5 kanal daha", NewChannelsTracker.summary(list, 2))
        assertEquals("Kanal 1, Kanal 2", NewChannelsTracker.summary(list.take(2), 5))
    }
}
