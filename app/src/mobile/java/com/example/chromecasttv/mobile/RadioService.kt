package com.example.chromecasttv

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Radyo için arka plan sesi: bildirim ve kilit ekranı kontrolleri.
 *
 * Servis, [RadioPlayer]'daki ortak ses oynatıcısını ayrı bir Media3 oturumuna sarar (TV videosunun
 * [PlaybackService] oturumundan bağımsızdır). Oynatıcının sahibi [RadioPlayer]'dır; servis kapanırken yalnızca oturum bırakılır.
 */
class RadioService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = RadioPlayer.player(this)

        val openApp = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val sessionActivity = PendingIntent.getActivity(
            this,
            1,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Oturum kimliği benzersiz olmalı: video oturumu varsayılan ("") kimliği kullanır
        mediaSession = MediaSession.Builder(this, player)
            .setId("redox_radio")
            .setSessionActivity(sessionActivity)
            .build()

        val provider = DefaultMediaNotificationProvider.Builder(this)
            .setNotificationId(1002)
            .setChannelId("redox_radio")
            .setChannelName(R.string.notif_channel_playback)
            .build()
        provider.setSmallIcon(R.drawable.ic_stat_redox)
        setMediaNotificationProvider(provider)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Uygulama son uygulamalardan kaldırıldığında radyo çalmıyorsa servis kapanır
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
