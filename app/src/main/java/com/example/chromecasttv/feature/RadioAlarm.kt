package com.example.chromecasttv

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar

/** Radyo alarmı ayarı: belirli saatte seçilen istasyonu çalarak uyandırır (telefon/tablet). */
data class RadioAlarmConfig(
    val enabled: Boolean = false,
    val hour: Int = 7,
    val minute: Int = 0,
    val stationId: String? = null,
    val stationName: String = "",
    val daily: Boolean = true
)

object RadioAlarm {
    const val EXTRA_STATION_ID = "radio_alarm_station_id"
    private const val PREFS = "redox_radio_alarm"
    private const val NOTIF_CHANNEL = "redox_radio_alarm"
    private const val NOTIF_ID = 1003
    private const val REQ_ALARM = 7301
    private const val REQ_SHOW = 7302
    private const val REQ_OPEN = 7303

    fun load(context: Context): RadioAlarmConfig {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return RadioAlarmConfig(
            enabled = p.getBoolean("enabled", false),
            hour = p.getInt("hour", 7),
            minute = p.getInt("minute", 0),
            stationId = p.getString("station_id", null),
            stationName = p.getString("station_name", "").orEmpty(),
            daily = p.getBoolean("daily", true)
        )
    }

    private fun save(context: Context, c: RadioAlarmConfig) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("enabled", c.enabled).putInt("hour", c.hour).putInt("minute", c.minute)
            .putString("station_id", c.stationId).putString("station_name", c.stationName)
            .putBoolean("daily", c.daily).apply()
    }

    /** Ayarı kaydeder; açıksa alarmı kurar, kapalıysa iptal eder. */
    fun apply(context: Context, config: RadioAlarmConfig) {
        val ok = config.enabled && config.stationId != null
        val c = config.copy(enabled = ok)
        save(context, c)
        if (ok) schedule(context, c) else cancel(context)
    }

    /** Bir sonraki tetiklenme zamanı (şimdiden sonraki ilk hh:mm). */
    fun nextTrigger(hour: Int, minute: Int, now: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    fun schedule(context: Context, config: RadioAlarmConfig = load(context)) {
        if (!config.enabled || config.stationId == null) return
        val app = context.applicationContext
        val manager = app.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        try {
            val trigger = nextTrigger(config.hour, config.minute)
            val show = PendingIntent.getActivity(
                app, REQ_SHOW, Intent(app, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            // setAlarmClock: tam zamanlı alarm izni istemez, Doze'da da zamanında çalışır
            manager.setAlarmClock(AlarmManager.AlarmClockInfo(trigger, show), receiverIntent(app))
        } catch (_: Exception) {
        }
    }

    fun cancel(context: Context) {
        val app = context.applicationContext
        val manager = app.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        try {
            manager.cancel(receiverIntent(app))
        } catch (_: Exception) {
        }
    }

    private fun receiverIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, REQ_ALARM, Intent(context, RadioAlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun fire(context: Context) {
        val config = load(context)
        if (!config.enabled || config.stationId == null) return
        // Günlükse yarına kur, tek seferlikse kapat
        if (config.daily) schedule(context, config) else save(context, config.copy(enabled = false))
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (manager.getNotificationChannel(NOTIF_CHANNEL) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(NOTIF_CHANNEL, "Radyo alarmı", NotificationManager.IMPORTANCE_HIGH)
                )
            }
            val open = Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_STATION_ID, config.stationId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val pi = PendingIntent.getActivity(
                context, REQ_OPEN, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, NOTIF_CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_redox)
                .setContentTitle("Radyo alarmı")
                .setContentText(config.stationName.ifBlank { "Radyo" } + " çalmak için dokunun")
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setFullScreenIntent(pi, true)
                .build()
            val nm = NotificationManagerCompat.from(context)
            if (nm.areNotificationsEnabled()) nm.notify(NOTIF_ID, notification)
        } catch (_: Exception) {
        }
    }
}

class RadioAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        RadioAlarm.fire(context)
    }
}
