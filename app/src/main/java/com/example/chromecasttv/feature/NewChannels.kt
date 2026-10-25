package com.example.chromecasttv

import android.content.Context

/**
 * "Yeni eklenen kanallar": kanal listesi her açılışta GitHub'dan güncellenir; kullanıcının daha önce görmediği
 * yerleşik kanallar karşılama ekranında bir kez duyurulur. İlk çalıştırmada yalnızca mevcut liste kaydedilir (duyuru yok).
 */
object NewChannelsTracker {
    private const val PREFS = "redox_seen_channels_v1"
    private const val KEY_SEEN = "seen_ids"

    /** Henüz görülmemiş yerleşik kanallar (ilk çalıştırmada boş; liste o anki halle kaydedilir). */
    fun pending(context: Context, builtIn: List<TvChannel>): List<TvChannel> {
        if (builtIn.isEmpty()) return emptyList()
        return try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (!prefs.contains(KEY_SEEN)) {
                prefs.edit().putStringSet(KEY_SEEN, builtIn.map { it.id }.toSet()).apply()
                emptyList()
            } else {
                val seen = prefs.getStringSet(KEY_SEEN, emptySet()) ?: emptySet()
                builtIn.filter { it.id !in seen }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    /** Listedeki tüm kanalları görülmüş sayar (kart kapatılınca ya da duyuru gösterilince). */
    fun markSeen(context: Context, builtIn: List<TvChannel>) {
        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val seen = (prefs.getStringSet(KEY_SEEN, emptySet()) ?: emptySet()) + builtIn.map { it.id }
            prefs.edit().putStringSet(KEY_SEEN, seen).apply()
        } catch (_: Throwable) {
        }
    }

    /** "A, B, C ve 4 kanal daha" biçiminde kısa özet. */
    fun summary(channels: List<TvChannel>, max: Int = 5): String {
        val names = channels.take(max).joinToString(", ") { it.name }
        return if (channels.size > max) "$names ve ${channels.size - max} kanal daha" else names
    }
}
