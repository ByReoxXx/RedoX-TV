package com.example.chromecasttv

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Arka planda ses: bildirim ve kilit ekranı kontrolleri.
 *
 * Servis, [PlaybackHub]'daki ortak oynatıcıyı bir Media3 oturumuna sarar. Oynatıcının sahibi hub'dır;
 * bu yüzden servis kapanırken yalnızca oturum bırakılır, oynatıcı bırakılmaz.
 */
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = PlaybackHub.player(this)

        val openApp = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()

        val provider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId("redox_playback")
            .setChannelName(R.string.notif_channel_playback)
            .build()
        provider.setSmallIcon(R.drawable.ic_stat_redox)
        setMediaNotificationProvider(provider)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Kullanıcı uygulamayı son uygulamalardan kaldırdığında oynatma yoksa servis kapanır.
        val player: Player? = mediaSession?.player
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
