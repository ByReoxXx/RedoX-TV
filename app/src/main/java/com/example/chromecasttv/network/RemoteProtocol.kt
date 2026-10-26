package com.example.chromecasttv

import org.json.JSONObject

// ============================================================================
// Telefon/Tablet = Kumanda protokolü (TV tarafı sunucu, telefon/tablet tarafı istemci)
// Yerel ağda düz HTTP + JSON; her istek eşleştirmede alınan Bearer anahtarıyla yetkilendirilir.
// ============================================================================

data class RemoteChannelInfo(
    val id: String,
    val number: Int,
    val name: String,
    val category: String,
    val logoUrl: String,
    val isFavorite: Boolean,
    val isLocked: Boolean
)

data class RemoteSnapshot(
    val appVersion: String,
    val deviceName: String,
    val playing: RemoteChannelInfo?,
    val isPlaying: Boolean,
    val volume: Int,
    val maxVolume: Int,
    val sleepMinutes: Int?
)

/** Telefonun eşleştiği bir TV. */
data class PairedTv(
    val name: String,
    val host: String,
    val port: Int,
    val token: String
)

/** Ağda (NSD ile) bulunan, henüz eşleşmemiş olabilecek bir TV. */
data class DiscoveredTv(
    val name: String,
    val host: String,
    val port: Int
)

object RemoteProtocol {
    // "RedoX TV" kendi servis türünü kullanır; eski RedoX TV ile karışmaz.
    const val SERVICE_TYPE = "_redoxtv2._tcp."
    const val APP_ID = "redoxtv2"

    fun channelToJson(c: RemoteChannelInfo): JSONObject {
        val o = JSONObject()
        o.put("id", c.id)
        o.put("no", c.number)
        o.put("name", c.name)
        o.put("cat", c.category)
        o.put("logo", c.logoUrl)
        o.put("fav", c.isFavorite)
        o.put("lock", c.isLocked)
        return o
    }

    fun channelFromJson(o: JSONObject): RemoteChannelInfo {
        return RemoteChannelInfo(
            id = o.optString("id", ""),
            number = o.optInt("no", 0),
            name = o.optString("name", ""),
            category = o.optString("cat", ""),
            logoUrl = o.optString("logo", ""),
            isFavorite = o.optBoolean("fav", false),
            isLocked = o.optBoolean("lock", false)
        )
    }

    fun snapshotToJson(s: RemoteSnapshot): JSONObject {
        val o = JSONObject()
        o.put("version", s.appVersion)
        o.put("name", s.deviceName)
        val playing = s.playing
        if (playing != null) o.put("playing", channelToJson(playing))
        o.put("isPlaying", s.isPlaying)
        o.put("volume", s.volume)
        o.put("maxVolume", s.maxVolume)
        val sleep = s.sleepMinutes
        if (sleep != null) o.put("sleep", sleep)
        return o
    }

    fun snapshotFromJson(o: JSONObject): RemoteSnapshot {
        val playingObj = o.optJSONObject("playing")
        return RemoteSnapshot(
            appVersion = o.optString("version", ""),
            deviceName = o.optString("name", "RedoX TV"),
            playing = if (playingObj != null) channelFromJson(playingObj) else null,
            isPlaying = o.optBoolean("isPlaying", false),
            volume = o.optInt("volume", 0),
            maxVolume = o.optInt("maxVolume", 15),
            sleepMinutes = if (o.has("sleep")) o.optInt("sleep", 0) else null
        )
    }
}
