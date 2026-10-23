package com.example.chromecasttv

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import java.util.Locale

/**
 * Oynatıcı fabrikası.
 *
 * Not: Eski sürümlerde sertifika doğrulaması uygulama genelinde kapatılıyordu. Artık yalnızca oynatıcının
 * kendi HTTP istemcisi ([HttpSecurity.streamClient]) bölgesel yayıncıların kendinden imzalı sertifikalarını
 * kabul eder; uygulamanın geri kalanı katı doğrulama kullanır.
 */
fun createTvExoPlayer(
    context: Context,
    forceMaxBitrate: Boolean = false,
    isMultiView: Boolean = false,
    handleAudioFocus: Boolean = false
): ExoPlayer {
    val loadControl = if (isMultiView) {
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(4_000, 12_000, 600, 1_200)
            .setBackBuffer(5_000, false)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    } else {
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(10_000, 28_000, 650, 1_200)
            .setBackBuffer(30_000, true)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    val httpFactory = OkHttpDataSource.Factory(HttpSecurity.streamClient(4_500, 5_500))
        .setUserAgent(HttpSecurity.USER_AGENT)

    val mediaSourceFactory = DefaultMediaSourceFactory(context)
        .setDataSourceFactory(httpFactory)

    // setEnableDecoderFallback(true): 4'lü (2x2) Çoklu Ekranda 3. ve 4. ekranın donanım kodek
    // sınırına takılıp siyah ekranda kalmasını tamamen engeller!
    val renderersFactory = DefaultRenderersFactory(context)
        .setEnableDecoderFallback(true)

    return ExoPlayer.Builder(context, renderersFactory)
        .setLoadControl(loadControl)
        .setMediaSourceFactory(mediaSourceFactory)
        .build()
        .apply {
            setWakeMode(C.WAKE_MODE_NETWORK)
            if (handleAudioFocus) {
                val attributes = AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build()
                setAudioAttributes(attributes, true)
                setHandleAudioBecomingNoisy(true)
            }
            trackSelectionParameters = if (isMultiView) {
                trackSelectionParameters
                    .buildUpon()
                    .setMaxVideoSize(854, 480)
                    .setMaxVideoBitrate(1_250_000)
                    .setForceHighestSupportedBitrate(false)
                    .build()
            } else {
                trackSelectionParameters
                    .buildUpon()
                    .setForceHighestSupportedBitrate(forceMaxBitrate)
                    .setMaxVideoBitrate(Int.MAX_VALUE)
                    .build()
            }
        }
}

fun buildStreamMediaSource(
    context: Context,
    source: StreamSource,
    title: String? = null,
    subtitle: String? = null,
    artworkUrl: String? = null
): MediaSource {
    val url = source.url.trim()
    val lower = url.lowercase(Locale.US)

    val headers = HashMap<String, String>()
    when {
        lower.contains("trkvz") || lower.contains("atv") || lower.contains("ahaber") || lower.contains("aspor") -> {
            headers["Referer"] = "https://www.atv.com.tr/"
            headers["Origin"] = "https://www.atv.com.tr"
        }
        lower.contains("kanald") || lower.contains("duhnet") || lower.contains("demiroren") -> {
            headers["Referer"] = "https://www.kanald.com.tr/"
            headers["Origin"] = "https://www.kanald.com.tr"
        }
        lower.contains("showtv") || lower.contains("ciner") || lower.contains("haberturk") -> {
            headers["Referer"] = "https://www.showtv.com.tr/"
            headers["Origin"] = "https://www.showtv.com.tr"
        }
        lower.contains("startv") || lower.contains("ntv") || lower.contains("dogus") -> {
            headers["Referer"] = "https://www.startv.com.tr/"
            headers["Origin"] = "https://www.startv.com.tr"
        }
        lower.contains("tv8") -> {
            headers["Referer"] = "https://www.tv8.com.tr/"
            headers["Origin"] = "https://www.tv8.com.tr"
        }
        lower.contains("nowtv") -> {
            headers["Referer"] = "https://www.nowtv.com.tr/"
            headers["Origin"] = "https://www.nowtv.com.tr"
        }
        lower.contains("halktv") -> {
            headers["Referer"] = "https://halktv.com.tr/"
            headers["Origin"] = "https://halktv.com.tr"
        }
        lower.contains("trt.com.tr") -> {
            headers["Referer"] = "https://www.tabii.com/"
            headers["Origin"] = "https://www.tabii.com"
        }
    }

    val httpFactory = OkHttpDataSource.Factory(HttpSecurity.streamClient(5_000, 6_500))
        .setUserAgent(HttpSecurity.USER_AGENT)
        .setDefaultRequestProperties(headers)

    val mediaItemBuilder = MediaItem.Builder().setUri(Uri.parse(url))
    if (title != null) {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(subtitle)
            .setArtworkUri(if (artworkUrl.isNullOrBlank()) null else Uri.parse(artworkUrl))
            .build()
        mediaItemBuilder.setMediaMetadata(metadata)
    }
    if (source.type == StreamType.HLS && !lower.endsWith(".mp4") && !lower.endsWith(".mkv") && !lower.endsWith(";")) {
        mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
    } else if (source.type == StreamType.DASH || lower.contains(".mpd")) {
        mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD)
    }

    return DefaultMediaSourceFactory(context)
        .setDataSourceFactory(httpFactory)
        .createMediaSource(mediaItemBuilder.build())
}
