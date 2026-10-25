package com.example.chromecasttv

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.json.JSONArray
import org.json.JSONObject

object ReminderConstants {
    const val EXTRA_CHANNEL_ID = "redox_channel_id"
    const val EXTRA_CHANNEL_NAME = "redox_channel_name"
    const val EXTRA_TITLE = "redox_reminder_title"
    const val ACTION_REMIND = "com.redoxtv.two.ACTION_REMIND"
    const val NOTIF_CHANNEL_ID = "redox_reminders"
}

/** Kalıcı hatırlatıcı listesi (SharedPreferences içinde JSON). */
class ReminderStore(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences("redox_reminders_v1", Context.MODE_PRIVATE)

    fun load(): List<Reminder> {
        val raw = prefs.getString("json", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = ArrayList<Reminder>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val channelId = o.optString("c", "")
                if (channelId.isBlank()) continue
                list.add(
                    Reminder(
                        channelId = channelId,
                        channelName = o.optString("n", ""),
                        title = o.optString("t", ""),
                        startMillis = o.optLong("s", 0L),
                        leadMinutes = o.optInt("l", 2)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(list: List<Reminder>) {
        val arr = JSONArray()
        for (r in list) {
            val o = JSONObject()
            o.put("c", r.channelId)
            o.put("n", r.channelName)
            o.put("t", r.title)
            o.put("s", r.startMillis)
            o.put("l", r.leadMinutes)
            arr.put(o)
        }
        prefs.edit().putString("json", arr.toString()).apply()
    }
}

/** AlarmManager ile hatırlatıcı kurar. Tam zamanlı alarm izni gerektirmeyen "idle'da izinli" alarm kullanılır. */
object ReminderScheduler {
    fun schedule(context: Context, reminder: Reminder) {
        val appContext = context.applicationContext
        val now = System.currentTimeMillis()
        if (reminder.startMillis <= now) return
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAt = maxOf(reminder.triggerAtMillis, now + 1_000L)
        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntentFor(appContext, reminder)
            )
        } catch (_: Exception) {
        }
    }

    fun cancel(context: Context, reminder: Reminder) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        try {
            alarmManager.cancel(pendingIntentFor(appContext, reminder))
        } catch (_: Exception) {
        }
    }

    fun rescheduleAll(context: Context, reminders: List<Reminder>) {
        for (r in reminders) schedule(context, r)
    }

    private fun pendingIntentFor(context: Context, reminder: Reminder): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderConstants.ACTION_REMIND
            // Ekstralar PendingIntent eşitliğine dahil değildir; her hatırlatıcıyı benzersiz kılmak için data kullanılır.
            data = Uri.parse("redox-reminder://" + Uri.encode(reminder.id))
            putExtra(ReminderConstants.EXTRA_CHANNEL_ID, reminder.channelId)
            putExtra(ReminderConstants.EXTRA_CHANNEL_NAME, reminder.channelName)
            putExtra(ReminderConstants.EXTRA_TITLE, reminder.title)
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

object ReminderNotifier {
    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (manager.getNotificationChannel(ReminderConstants.NOTIF_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                ReminderConstants.NOTIF_CHANNEL_ID,
                context.getString(R.string.notif_channel_reminders),
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = context.getString(R.string.notif_channel_reminders_desc)
            manager.createNotificationChannel(channel)
        }
    }

    fun show(context: Context, channelId: String, channelName: String, title: String) {
        try {
            ensureChannel(context)
            val open = Intent(context, MainActivity::class.java).apply {
                putExtra(ReminderConstants.EXTRA_CHANNEL_ID, channelId)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
            val contentIntent = PendingIntent.getActivity(
                context,
                channelId.hashCode(),
                open,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, ReminderConstants.NOTIF_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_redox)
                .setContentTitle("$channelName: $title")
                .setContentText("Birazdan başlıyor — izlemek için dokunun")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(contentIntent)
                .build()
            val manager = NotificationManagerCompat.from(context)
            if (manager.areNotificationsEnabled()) {
                manager.notify((channelId + "|" + title).hashCode(), notification)
            }
        } catch (_: Exception) {
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val channelId = intent.getStringExtra(ReminderConstants.EXTRA_CHANNEL_ID) ?: return
        val channelName = intent.getStringExtra(ReminderConstants.EXTRA_CHANNEL_NAME).orEmpty()
        val title = intent.getStringExtra(ReminderConstants.EXTRA_TITLE).orEmpty()
        ReminderNotifier.show(context, channelId, channelName, title)
    }
}

/** Cihaz yeniden başlayınca sistem alarmları silinir; bu alıcı kayıtlı hatırlatıcıları yeniden kurar. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val now = System.currentTimeMillis()
        val alive = ReminderStore(context).load().filter { it.startMillis > now }
        ReminderScheduler.rescheduleAll(context, alive)
        RadioAlarm.schedule(context)
    }
}
