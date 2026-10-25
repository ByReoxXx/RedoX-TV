package com.example.chromecasttv

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

/**
 * Radyo için tek, ortak ses oynatıcısı (TV, telefon ve tablet).
 *
 * Video oynatıcısından ayrıdır: yalnızca ses, kısa tampon, müzik odağı. Shoutcast/Icecast sunucuları tarayıcı gibi
 * görünen istemciye durum sayfası (HTML) verdiği için oynatıcı kimlikli bir User-Agent kullanılır. ICY şarkı bilgisi
 * Media3 tarafından `MediaMetadata.title` olarak iletilir. Telefon/tablette arka plan servisi [player] örneğini sarar.
 * Tüm çağrılar ana (UI) iş parçacığından yapılmalıdır.
 */
object RadioPlayer {
    const val USER_AGENT = "VLC/3.0.20 LibVLC/3.0.20"

    private var exo: ExoPlayer? = null

    fun player(context: Context): ExoPlayer {
        exo?.let { return it }
        val app = context.applicationContext
        // Icy-MetaData: 1 → Shoutcast/Icecast sunucusu yayına "Sanatçı - Şarkı" bilgisini gömer (Media3 ayıklar)
        val http = OkHttpDataSource.Factory(HttpSecurity.streamClient(8_000, 15_000))
            .setUserAgent(USER_AGENT)
            .setDefaultRequestProperties(mapOf("Icy-MetaData" to "1"))
        val load = DefaultLoadControl.Builder()
            .setBufferDurationsMs(5_000, 30_000, 1_500, 3_000)
            .build()
        val created = ExoPlayer.Builder(app)
            .setLoadControl(load)
            .setMediaSourceFactory(DefaultMediaSourceFactory(app).setDataSourceFactory(http))
            .build()
            .apply {
                setWakeMode(C.WAKE_MODE_NETWORK)
                setAudioAttributes(
                    AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                    true
                )
                setHandleAudioBecomingNoisy(true)
            }
        exo = created
        return created
    }

    /** Oynatıcı oluşturulmuşsa döndürür; oluşturmaz. */
    fun peek(): ExoPlayer? = exo

    /** Adresi çalar (önceki yayın bırakılır). [title] bildirim/kilit ekranında istasyon adı olarak görünür. */
    fun play(context: Context, url: String, title: String, subtitle: String?, artworkUrl: String?) {
        val p = player(context)
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(subtitle)
            .apply { if (!artworkUrl.isNullOrBlank()) setArtworkUri(Uri.parse(artworkUrl)) }
            .build()
        p.stop()
        p.setMediaItem(MediaItem.Builder().setUri(Uri.parse(url.trim())).setMediaMetadata(metadata).build())
        p.prepare()
        p.playWhenReady = true
    }

    fun stop() {
        exo?.let {
            it.stop()
            it.clearMediaItems()
        }
    }
}
