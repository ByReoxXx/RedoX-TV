package com.example.chromecasttv

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Basit, yerel hata günlüğü. Hiçbir veri dışarı gönderilmez; kullanıcı isterse
 * Ayarlar > Tanılama ekranından paylaşabilir.
 */
object CrashLogger {
    private const val FILE_NAME = "redox_crash_log.txt"
    private const val MAX_BYTES = 200_000L

    @Volatile
    private var installed = false

    fun install(context: Context) {
        if (installed) return
        installed = true
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                append(appContext, "ÇÖKME (iş parçacığı: ${thread.name})", throwable)
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun log(context: Context, message: String, throwable: Throwable? = null) {
        try {
            append(context.applicationContext, message, throwable)
        } catch (_: Throwable) {
        }
    }

    fun logFile(context: Context): File = File(context.applicationContext.filesDir, FILE_NAME)

    fun readLog(context: Context): String {
        return try {
            val f = logFile(context)
            if (f.exists()) f.readText() else ""
        } catch (_: Throwable) {
            ""
        }
    }

    fun clear(context: Context) {
        try {
            logFile(context).delete()
        } catch (_: Throwable) {
        }
    }

    @Synchronized
    private fun append(context: Context, title: String, throwable: Throwable?) {
        val file = logFile(context)
        if (file.exists() && file.length() > MAX_BYTES) {
            // Dosya çok büyüdüyse eski yarısını at
            val text = file.readText()
            file.writeText(text.takeLast((MAX_BYTES / 2).toInt()))
        }
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val stack = if (throwable == null) {
            ""
        } else {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            sw.toString()
        }
        file.appendText("[$stamp] $title\n$stack\n")
    }
}
