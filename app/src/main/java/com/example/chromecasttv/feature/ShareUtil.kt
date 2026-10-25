package com.example.chromecasttv

import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Düz metni sistemin paylaşım menüsüyle paylaşır (hata raporu, yayın bildirimi). */
fun shareTextIntent(context: Context, text: String, title: String = "Paylaş") {
    try {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Throwable) {
    }
}

/** Kanal listesi bilgisi: kaç kanal var, en son ne zaman GitHub'dan indirildi. */
fun channelListInfoText(context: Context, channelCount: Int): String {
    return try {
        val cached = java.io.File(context.applicationContext.filesDir, "github_channels_v17_cached.enc")
        val whenText = if (cached.exists() && cached.length() > 50L) {
            "son güncelleme " + SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr")).format(Date(cached.lastModified()))
        } else {
            "henüz indirilmedi (uygulamayla gelen liste)"
        }
        "$channelCount kanal • $whenText"
    } catch (_: Throwable) {
        "$channelCount kanal"
    }
}
