package com.example.chromecasttv

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Telefon/tablet tarafında eşleşmiş TV'lerin kalıcı listesi. */
class PairedTvStore(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences("redox_paired_tvs_v1", Context.MODE_PRIVATE)

    fun load(): List<PairedTv> {
        val raw = prefs.getString("json", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            val out = ArrayList<PairedTv>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val host = o.optString("h", "")
                val token = o.optString("t", "")
                if (host.isBlank() || token.isBlank()) continue
                out.add(PairedTv(o.optString("n", "RedoX TV 2"), host, o.optInt("p", 8585), token))
            }
            out
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun save(list: List<PairedTv>) {
        val arr = JSONArray()
        for (tv in list) {
            val o = JSONObject()
            o.put("n", tv.name)
            o.put("h", tv.host)
            o.put("p", tv.port)
            o.put("t", tv.token)
            arr.put(o)
        }
        prefs.edit().putString("json", arr.toString()).apply()
    }

    fun upsert(tv: PairedTv): List<PairedTv> {
        val updated = load().filterNot { it.host == tv.host && it.port == tv.port } + tv
        save(updated)
        return updated
    }

    fun remove(tv: PairedTv): List<PairedTv> {
        val updated = load().filterNot { it.host == tv.host && it.port == tv.port }
        save(updated)
        return updated
    }
}

class RemotePairResult(val token: String?, val tvName: String, val error: String?)

/** TV'nin yerel kumanda API'sine (LocalControlServer) bağlanan basit HTTP istemcisi. */
class RemoteClient(
    private val host: String,
    private val port: Int,
    private val token: String?
) {
    private class Reply(val code: Int, val text: String)

    private suspend fun call(method: String, path: String, body: JSONObject? = null): Reply? =
        withContext(Dispatchers.IO) {
            try {
                val connection = HttpSecurity.open("http://$host:$port$path", false, 3_000, 6_000)
                connection.requestMethod = method
                connection.instanceFollowRedirects = false
                if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
                if (body != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                connection.disconnect()
                Reply(code, text)
            } catch (_: Exception) {
                null
            }
        }

    private fun parse(reply: Reply?): JSONObject? {
        if (reply == null || reply.code !in 200..299) return null
        return try {
            JSONObject(reply.text)
        } catch (_: Exception) {
            null
        }
    }

    /** Kimlik doğrulamasız: TV'nin gerçekten RedoX olup olmadığını ve adını döndürür. */
    suspend fun hello(): String? {
        val json = parse(call("GET", "/api/hello")) ?: return null
        if (json.optString("app", "") != RemoteProtocol.APP_ID) return null
        return json.optString("name", "RedoX TV 2")
    }

    suspend fun pair(pin: String, deviceName: String): RemotePairResult {
        val body = JSONObject()
        body.put("pin", pin)
        body.put("name", deviceName)
        val reply = call("POST", "/api/pair", body)
            ?: return RemotePairResult(null, "", "TV'ye ulaşılamadı. Aynı Wi-Fi ağında olduğunuzdan emin olun.")
        val json = try {
            JSONObject(reply.text)
        } catch (_: Exception) {
            JSONObject()
        }
        return when {
            reply.code in 200..299 && json.optString("token", "").isNotEmpty() ->
                RemotePairResult(json.optString("token", ""), json.optString("name", "RedoX TV 2"), null)
            reply.code == 429 -> RemotePairResult(null, "", "Çok fazla hatalı deneme. Biraz bekleyip tekrar deneyin.")
            reply.code == 403 -> RemotePairResult(null, "", "Kod hatalı.")
            else -> RemotePairResult(null, "", "Eşleştirme başarısız (HTTP ${reply.code}).")
        }
    }

    suspend fun snapshot(): RemoteSnapshot? {
        val json = parse(call("GET", "/api/state")) ?: return null
        return RemoteProtocol.snapshotFromJson(json)
    }

    suspend fun channels(): List<RemoteChannelInfo>? {
        val json = parse(call("GET", "/api/channels")) ?: return null
        val arr = json.optJSONArray("channels") ?: return emptyList()
        val out = ArrayList<RemoteChannelInfo>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out.add(RemoteProtocol.channelFromJson(o))
        }
        return out
    }

    suspend fun play(channelId: String): Boolean {
        val body = JSONObject()
        body.put("id", channelId)
        return parse(call("POST", "/api/play", body)) != null
    }

    suspend fun zap(delta: Int): Boolean {
        val body = JSONObject()
        body.put("delta", delta)
        return parse(call("POST", "/api/zap", body)) != null
    }

    suspend fun key(name: String): Boolean {
        val body = JSONObject()
        body.put("key", name)
        return parse(call("POST", "/api/key", body)) != null
    }

    suspend fun volume(delta: Int): RemoteSnapshot? {
        val body = JSONObject()
        body.put("delta", delta)
        val json = parse(call("POST", "/api/volume", body)) ?: return null
        return RemoteProtocol.snapshotFromJson(json)
    }

    suspend fun playPause(): Boolean = parse(call("POST", "/api/playpause", JSONObject())) != null

    suspend fun stop(): Boolean = parse(call("POST", "/api/stop", JSONObject())) != null

    /** TV'deki radyo: action = state | now | play | toggle | step | stop. Yanıt: etkin mi, aşama, istasyon, şarkı (+ istasyon listesi). */
    suspend fun radio(action: String, id: String = "", delta: Int = 0): JSONObject? {
        val body = JSONObject()
        body.put("action", action)
        body.put("id", id)
        body.put("delta", delta)
        return parse(call("POST", "/api/radio", body))
    }

    suspend fun cast(name: String, url: String, logoUrl: String): Boolean {
        val body = JSONObject()
        body.put("name", name)
        body.put("url", url)
        body.put("logo", logoUrl)
        return parse(call("POST", "/api/cast", body)) != null
    }
}
