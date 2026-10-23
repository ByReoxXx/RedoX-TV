package com.example.chromecasttv

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Telefon/tablet için tek, ortak oynatıcı.
 *
 * Tam ekran oynatıcı, mini oynatıcı ve arka plan servisi ([PlaybackService]) aynı ExoPlayer örneğini kullanır;
 * böylece uygulama arka plana alındığında ses kesilmeden sürer, bildirim/kilit ekranı kontrolleri çalışır.
 * Tüm çağrılar ana (UI) iş parçacığından yapılmalıdır.
 */
object PlaybackHub {
    private var exo: ExoPlayer? = null
    private val night = NightModeEffect()
    private var nightEnabled = false

    /** Şu an hazırlanan/oynayan kanal ve akış adresi (aynı akışı yeniden başlatmamak için). */
    var currentChannelId: String? = null
        private set
    var currentUrl: String? = null
        private set

    fun player(context: Context): ExoPlayer {
        exo?.let { return it }
        val created = createTvExoPlayer(
            context.applicationContext,
            forceMaxBitrate = false,
            isMultiView = false,
            handleAudioFocus = true
        )
        created.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                reapplyNight()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) reapplyNight()
            }
        })
        exo = created
        return created
    }

    /** Oynatıcı oluşturulmuşsa döndürür; oluşturmaz (yaşam döngüsü gözlemcileri için). */
    fun peek(): ExoPlayer? = exo

    /**
     * Kanalı ve akış kaynağını çalar. Aynı akış zaten hazırsa yeniden başlatılmaz
     * (mini oynatıcıdan tam ekrana dönüşte kesinti olmaz).
     */
    fun play(
        context: Context,
        channel: TvChannel,
        sourceIndex: Int,
        quality: PlayerQuality,
        night: Boolean,
        force: Boolean = false
    ) {
        val p = player(context)
        val source = channel.sources.getOrNull(sourceIndex) ?: return
        applyPlayerQuality(p, quality)
        setNightMode(night)

        val state = p.playbackState
        val alreadyActive = !force &&
            currentUrl == source.url &&
            state != Player.STATE_IDLE &&
            state != Player.STATE_ENDED
        currentChannelId = channel.id
        if (alreadyActive) return

        currentUrl = source.url
        p.stop()
        p.setMediaSource(
            buildStreamMediaSource(
                context.applicationContext,
                source,
                title = channel.name,
                subtitle = channel.nowTitleOrLive(),
                artworkUrl = channel.logoUrl.takeIf { it.isNotBlank() }
            )
        )
        p.prepare()
        p.playWhenReady = true
    }

    fun setNightMode(enabled: Boolean) {
        nightEnabled = enabled
        reapplyNight()
    }

    private fun reapplyNight() {
        val p = exo ?: return
        night.update(p, nightEnabled)
    }

    /** Arka plan ses modunda görüntüyü kapatarak veri ve pil tasarrufu sağlar. */
    fun setVideoEnabled(enabled: Boolean) {
        val p = exo ?: return
        p.trackSelectionParameters = p.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, !enabled)
            .build()
    }

    /** Oynatmayı durdurur, kuyruğu temizler (oynatıcı örneği yeniden kullanılmak üzere saklanır). */
    fun stop() {
        val p = exo
        if (p != null) {
            p.stop()
            p.clearMediaItems()
            night.release()
            p.volume = 1f
        }
        currentChannelId = null
        currentUrl = null
    }

    val isPlaying: Boolean
        get() = exo?.isPlaying == true

    /** Süreç sonlanırken tüm kaynakları bırakır. */
    fun release() {
        night.release()
        exo?.release()
        exo = null
        currentChannelId = null
        currentUrl = null
    }
}
