package com.example.chromecasttv

import android.content.Context
import android.os.SystemClock
import org.json.JSONArray
import org.json.JSONObject

// ============================================================================
// Yeni özelliklerin kalıcı depoları (SharedPreferences içinde küçük JSON dosyaları)
// ============================================================================

class FeaturePrefsStore(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences("redox_feature_prefs_v1", Context.MODE_PRIVATE)

    fun load(): FeaturePrefs {
        // TV'de eski sürümün davranışı korunur (1080p'ye kadar en yüksek kalite); telefon/tabletta otomatik.
        val defaultQuality = if (BuildConfig.DEVICE_KIND == "tv") PlayerQuality.P1080 else PlayerQuality.AUTO
        val qualityName = prefs.getString("quality", defaultQuality.name) ?: defaultQuality.name
        val quality = PlayerQuality.values().firstOrNull { it.name == qualityName } ?: defaultQuality
        val urls = (prefs.getString("epg_urls", "") ?: "")
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return FeaturePrefs(
            backgroundAudio = prefs.getBoolean("bg_audio", true),
            autoPip = prefs.getBoolean("auto_pip", true),
            gesturesEnabled = prefs.getBoolean("gestures", true),
            defaultQuality = quality,
            mobileDataSaver = prefs.getBoolean("data_saver", true),
            nightMode = prefs.getBoolean("night_mode", false),
            epgUrls = urls,
            updateCheckOnStart = prefs.getBoolean("update_check", true)
        )
    }

    fun save(p: FeaturePrefs) {
        prefs.edit()
            .putBoolean("bg_audio", p.backgroundAudio)
            .putBoolean("auto_pip", p.autoPip)
            .putBoolean("gestures", p.gesturesEnabled)
            .putString("quality", p.defaultQuality.name)
            .putBoolean("data_saver", p.mobileDataSaver)
            .putBoolean("night_mode", p.nightMode)
            .putString("epg_urls", p.epgUrls.joinToString("\n"))
            .putBoolean("update_check", p.updateCheckOnStart)
            .apply()
    }
}

class FavoriteListsStore(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences("redox_fav_lists_v1", Context.MODE_PRIVATE)

    fun rawJson(): String = prefs.getString("json", "") ?: ""

    fun restoreRawJson(json: String) {
        prefs.edit().putString("json", json).apply()
    }

    fun load(): FavoriteListsData? {
        val raw = rawJson()
        if (raw.isBlank()) return null
        return try {
            val root = JSONObject(raw)
            val active = root.optString("active", DEFAULT_FAVORITE_LIST)
            val listsObj = root.optJSONObject("lists") ?: JSONObject()
            val lists = LinkedHashMap<String, List<String>>()
            val keys = listsObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val arr = listsObj.optJSONArray(key) ?: JSONArray()
                val ids = ArrayList<String>(arr.length())
                for (i in 0 until arr.length()) {
                    val v = arr.optString(i, "")
                    if (v.isNotBlank()) ids.add(v)
                }
                lists[key] = ids
            }
            if (!lists.containsKey(DEFAULT_FAVORITE_LIST)) lists[DEFAULT_FAVORITE_LIST] = emptyList()
            FavoriteListsData(
                active = if (lists.containsKey(active)) active else DEFAULT_FAVORITE_LIST,
                lists = lists
            )
        } catch (_: Exception) {
            null
        }
    }

    fun save(data: FavoriteListsData) {
        val listsObj = JSONObject()
        for ((name, ids) in data.lists) {
            val arr = JSONArray()
            for (id in ids) arr.put(id)
            listsObj.put(name, arr)
        }
        val root = JSONObject()
        root.put("active", data.active)
        root.put("lists", listsObj)
        prefs.edit().putString("json", root.toString()).apply()
    }
}

class ChannelOverridesStore(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences("redox_channel_overrides_v1", Context.MODE_PRIVATE)

    fun rawJson(): String = prefs.getString("json", "") ?: ""

    fun restoreRawJson(json: String) {
        prefs.edit().putString("json", json).apply()
    }

    fun load(): Map<String, ChannelOverride> {
        val raw = rawJson()
        if (raw.isBlank()) return emptyMap()
        return try {
            val root = JSONObject(raw)
            val out = LinkedHashMap<String, ChannelOverride>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val o = root.optJSONObject(id) ?: continue
                val name = o.optString("n", "").trim().ifEmpty { null }
                val number = if (o.has("o")) o.optInt("o", -1).takeIf { it > 0 } else null
                if (name != null || number != null) out[id] = ChannelOverride(name, number)
            }
            out
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun save(map: Map<String, ChannelOverride>) {
        val root = JSONObject()
        for ((id, ov) in map) {
            val o = JSONObject()
            if (ov.name != null) o.put("n", ov.name)
            if (ov.number != null) o.put("o", ov.number)
            root.put(id, o)
        }
        prefs.edit().putString("json", root.toString()).apply()
    }
}

/** İzleme istatistikleri: yalnızca cihazda tutulur, hiçbir yere gönderilmez. */
class WatchStatsStore(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences("redox_watch_stats_v1", Context.MODE_PRIVATE)

    private var currentId: String? = null
    private var currentStartElapsed: Long = 0L

    fun load(): Map<String, WatchStat> {
        val raw = prefs.getString("json", "") ?: ""
        if (raw.isBlank()) return emptyMap()
        return try {
            val root = JSONObject(raw)
            val out = LinkedHashMap<String, WatchStat>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val o = root.optJSONObject(id) ?: continue
                out[id] = WatchStat(
                    channelId = id,
                    totalMillis = o.optLong("t", 0L),
                    playCount = o.optInt("c", 0),
                    lastPlayedMillis = o.optLong("l", 0L)
                )
            }
            out
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun save(map: Map<String, WatchStat>) {
        val root = JSONObject()
        // En son izlenen 300 kanalla sınırla
        val limited = map.values.sortedByDescending { it.lastPlayedMillis }.take(300)
        for (s in limited) {
            val o = JSONObject()
            o.put("t", s.totalMillis)
            o.put("c", s.playCount)
            o.put("l", s.lastPlayedMillis)
            root.put(s.channelId, o)
        }
        prefs.edit().putString("json", root.toString()).apply()
    }

    /** Aktif kanal değiştiğinde (veya kapandığında, [newId] = null) çağrılır; güncel istatistikleri döndürür. */
    @Synchronized
    fun switchTo(newId: String?): Map<String, WatchStat> {
        val now = SystemClock.elapsedRealtime()
        val stats = LinkedHashMap<String, WatchStat>(load())
        val previous = currentId
        val wallNow = System.currentTimeMillis()

        if (previous != null) {
            val duration = (now - currentStartElapsed).coerceAtLeast(0L)
            if (duration >= 3_000L) {
                val old = stats[previous]
                stats[previous] = WatchStat(
                    channelId = previous,
                    totalMillis = (old?.totalMillis ?: 0L) + duration,
                    playCount = old?.playCount ?: 0,
                    lastPlayedMillis = wallNow
                )
            }
        }
        if (newId != null && newId != previous) {
            val old = stats[newId]
            stats[newId] = WatchStat(
                channelId = newId,
                totalMillis = old?.totalMillis ?: 0L,
                playCount = (old?.playCount ?: 0) + 1,
                lastPlayedMillis = wallNow
            )
        }
        currentId = newId
        currentStartElapsed = now
        save(stats)
        return stats
    }

    fun clear() {
        prefs.edit().clear().apply()
        currentId = null
    }
}

/** TV tarafında eşleşmiş telefon/tablet kumandalarının erişim anahtarları. */
class RemoteTokenStore(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences("redox_remote_tokens_v1", Context.MODE_PRIVATE)

    private fun readAll(): MutableMap<String, String> {
        val raw = prefs.getString("json", "") ?: ""
        val out = LinkedHashMap<String, String>()
        if (raw.isBlank()) return out
        try {
            val root = JSONObject(raw)
            val keys = root.keys()
            while (keys.hasNext()) {
                val token = keys.next()
                out[token] = root.optString(token, "")
            }
        } catch (_: Exception) {
        }
        return out
    }

    private fun writeAll(map: Map<String, String>) {
        val root = JSONObject()
        for ((token, name) in map) root.put(token, name)
        prefs.edit().putString("json", root.toString()).apply()
    }

    @Synchronized
    fun tokens(): Set<String> = readAll().keys.toSet()

    @Synchronized
    fun count(): Int = readAll().size

    @Synchronized
    fun add(token: String, deviceName: String) {
        val all = readAll()
        all[token] = deviceName
        // En fazla 12 eşleşmiş cihaz; en eskisini at
        while (all.size > 12) {
            val first = all.keys.firstOrNull() ?: break
            all.remove(first)
        }
        writeAll(all)
    }

    @Synchronized
    fun clear() {
        prefs.edit().clear().apply()
    }
}
