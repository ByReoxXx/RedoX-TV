package com.example.chromecasttv

import android.content.Context
import android.media.audiofx.LoudnessEnhancer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter

// ============================================================================
// Oynatıcı yardımcıları: kalite sınırı, gece modu, gerçek yayın istatistikleri
// ============================================================================

/** Seçilen kaliteyi oynatıcıya uygular (AUTO = sınırsız, uyarlanabilir). */
fun applyPlayerQuality(player: ExoPlayer, quality: PlayerQuality) {
    val builder = player.trackSelectionParameters.buildUpon()
    if (quality == PlayerQuality.AUTO) {
        builder.clearVideoSizeConstraints()
        builder.setMaxVideoBitrate(Int.MAX_VALUE)
        builder.setForceHighestSupportedBitrate(false)
    } else {
        val height = quality.maxHeight
        val width = height * 16 / 9
        builder.setMaxVideoSize(width, height)
        builder.setMaxVideoBitrate(if (quality.maxBitrate > 0) quality.maxBitrate else Int.MAX_VALUE)
        // 1080p: bant genişliğine bakmadan 1080p sınırına kadar en yüksek kaliteyi seç.
        builder.setForceHighestSupportedBitrate(quality == PlayerQuality.P1080)
    }
    player.trackSelectionParameters = builder.build()
}

/** Cihaz şu an yalnızca hücresel (mobil) veri kullanıyor mu? */
fun isOnCellular(context: Context): Boolean {
    return try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
            !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
            !caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    } catch (_: Exception) {
        false
    }
}

/** Kullanıcı tercihine ve ağ türüne göre uygulanacak etkin kalite. */
fun effectiveQuality(context: Context, prefs: FeaturePrefs): PlayerQuality {
    if (prefs.mobileDataSaver && isOnCellular(context)) return PlayerQuality.SAVER
    return prefs.defaultQuality
}

/**
 * Gece modu: konuşmaları belirginleştirir, ani yüksek sesleri baskılar.
 * Android'in LoudnessEnhancer efekti (yumuşak sınırlayıcı içerir) ile kazancı artırıp çıkış seviyesini
 * düşürerek sessiz/gürültülü bölümler arasındaki farkı azaltır. Desteklemeyen cihazlarda sessizce devre dışı kalır.
 */
class NightModeEffect {
    private var enhancer: LoudnessEnhancer? = null
    private var boundSession = C.AUDIO_SESSION_ID_UNSET

    /** @return efekt gerçekten etkinse true */
    fun update(player: ExoPlayer, enabled: Boolean): Boolean {
        val session = player.audioSessionId
        if (!enabled || session == C.AUDIO_SESSION_ID_UNSET) {
            release()
            player.volume = 1f
            return false
        }
        return try {
            if (enhancer == null || boundSession != session) {
                release()
                enhancer = LoudnessEnhancer(session)
                boundSession = session
            }
            val effect = enhancer
            if (effect == null) {
                player.volume = 1f
                false
            } else {
                effect.setTargetGain(800)
                effect.setEnabled(true)
                player.volume = 0.5f
                true
            }
        } catch (_: Throwable) {
            release()
            player.volume = 1f
            false
        }
    }

    fun release() {
        try {
            enhancer?.release()
        } catch (_: Throwable) {
        }
        enhancer = null
        boundSession = C.AUDIO_SESSION_ID_UNSET
    }
}

/** Gerçek (ölçülen) yayın bilgileri. -1 değeri "bilinmiyor" demektir. */
data class PlayerStats(
    val width: Int = 0,
    val height: Int = 0,
    val videoBitrateKbps: Int = -1,
    val bandwidthKbps: Int = -1,
    val droppedFrames: Int = 0,
    val bufferedSeconds: Float = 0f,
    val codec: String = ""
) {
    val resolutionLabel: String
        get() = when {
            height >= 2160 -> "4K UHD"
            height >= 1440 -> "1440p"
            height >= 1080 -> "1080p"
            height >= 720 -> "720p"
            height > 0 -> "${height}p"
            else -> "—"
        }
}

/** Oynatıcıdan anlık, gerçek istatistikleri okur (tahmin yok). */
fun samplePlayerStats(context: Context, player: ExoPlayer): PlayerStats {
    return try {
        val format = player.videoFormat
        var dropped = 0
        try {
            val counters = player.videoDecoderCounters
            if (counters != null) {
                counters.ensureUpdated()
                dropped = counters.droppedBufferCount
            }
        } catch (_: Throwable) {
        }
        val estimate = DefaultBandwidthMeter.getSingletonInstance(context).bitrateEstimate
        PlayerStats(
            width = format?.width?.takeIf { it > 0 } ?: 0,
            height = format?.height?.takeIf { it > 0 } ?: 0,
            videoBitrateKbps = if (format != null && format.bitrate > 0) format.bitrate / 1000 else -1,
            bandwidthKbps = if (estimate > 0L) (estimate / 1000L).toInt() else -1,
            droppedFrames = dropped,
            bufferedSeconds = player.totalBufferedDuration / 1000f,
            codec = format?.sampleMimeType.orEmpty().removePrefix("video/")
        )
    } catch (_: Throwable) {
        PlayerStats()
    }
}
