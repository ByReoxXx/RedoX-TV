package com.example.chromecasttv

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import java.util.Random

/**
 * Nostaljik radyo cızırtısı: istasyon değişirken yayın açılana kadar çalan, kodla üretilmiş ses (dosya gerekmez).
 * Alçak geçiren süzülmüş beyaz gürültü + yavaş dalgalanan şiddet + ara sıra çıtırtı. Açılınca kısa sürede söner.
 * Ana iş parçacığından çağrılır; hata olursa sessizce atlanır.
 */
object RadioStatic {
    private const val RATE = 22_050
    private const val FRAMES = RATE * 3 / 2
    private const val VOLUME = 0.09f

    private val handler = Handler(Looper.getMainLooper())
    private var track: AudioTrack? = null
    private var fade: Runnable? = null

    fun start() {
        cancelFade()
        try {
            val t = track ?: build().also { track = it }
            t.setVolume(VOLUME)
            if (t.playState != AudioTrack.PLAYSTATE_PLAYING) t.play()
        } catch (_: Throwable) {
            release()
        }
    }

    /** Cızırtıyı kısa bir solmayla keser. */
    fun stop() {
        val t = track ?: return
        cancelFade()
        var step = 0
        val r = object : Runnable {
            override fun run() {
                step++
                try {
                    if (step >= 4) {
                        release()
                        return
                    }
                    t.setVolume(VOLUME * (1f - step / 4f))
                    handler.postDelayed(this, 45L)
                } catch (_: Throwable) {
                    release()
                }
            }
        }
        fade = r
        handler.postDelayed(r, 45L)
    }

    private fun cancelFade() {
        fade?.let { handler.removeCallbacks(it) }
        fade = null
    }

    private fun release() {
        cancelFade()
        try {
            track?.stop()
        } catch (_: Throwable) {
        }
        try {
            track?.release()
        } catch (_: Throwable) {
        }
        track = null
    }

    private var pcmCache: ShortArray? = null

    private fun build(): AudioTrack {
        val pcm = pcmCache ?: makePcm().also { pcmCache = it }
        val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
        val format = AudioFormat.Builder()
            .setSampleRate(RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val t = AudioTrack.Builder()
            .setAudioAttributes(attrs)
            .setAudioFormat(format)
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        t.write(pcm, 0, pcm.size)
        t.setLoopPoints(0, FRAMES, -1)
        return t
    }

    /** Cızırtı örnekleri bir kez üretilir ve saklanır (her istasyon değişiminde yeniden hesaplanmaz). */
    private fun makePcm(): ShortArray {
        val rnd = Random(7)
        val pcm = ShortArray(FRAMES)
        var envelope = 0.7f
        var target = 0.7f
        var low = 0f
        for (i in 0 until FRAMES) {
            if (i % 1_100 == 0) target = 0.55f + rnd.nextFloat() * 0.45f          // yavaş, yumuşak şiddet dalgalanması
            envelope += 0.004f * (target - envelope)
            val white = rnd.nextFloat() * 2f - 1f
            low += 0.12f * (white - low)                                          // güçlü alçak geçiren: tiz "cızz" yerine yumuşak "şşş"
            var s = (0.18f * white + 1.6f * low) * envelope
            if (rnd.nextInt(6_000) == 0) s += if (rnd.nextBoolean()) 0.3f else -0.3f   // seyrek, hafif çıtırtı
            pcm[i] = (s.coerceIn(-1f, 1f) * 0.6f * Short.MAX_VALUE).toInt().toShort()
        }
        return pcm
    }
}
