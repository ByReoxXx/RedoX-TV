package com.example.chromecasttv

import android.content.Context
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.security.MessageDigest
import java.security.SecureRandom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/** TV uygulamasının, yerel sunucudan gelen istekleri karşıladığı yüzey. */
interface ControlServerHandler {
    // --- Telefondan dosya/liste yükleme (QR web sayfası) ---
    suspend fun onAddM3u(name: String, url: String, rawContent: String)
    suspend fun onAddXtream(name: String, server: String, username: String, password: String)
    suspend fun onRestoreBackup(data: String): Boolean
    fun onExportBackup(): String

    // --- Telefon/tablet kumandası ---
    fun remoteDeviceName(): String
    fun remoteSnapshot(): RemoteSnapshot
    fun remoteChannels(): List<RemoteChannelInfo>
    fun remotePlay(channelId: String): Boolean
    fun remoteZap(delta: Int)
    fun remoteKey(key: String): Boolean

    /** [absoluteLevel] < 0 ise yalnızca [delta] uygulanır. */
    fun remoteVolume(delta: Int, absoluteLevel: Int): RemoteSnapshot
    fun remotePlayPause()
    fun remoteStop()
    fun remoteCast(name: String, url: String, logoUrl: String): Boolean
    fun remotePaired(deviceName: String)
}

/**
 * Tek bir yerel HTTP sunucusu (8585-8595 arası ilk boş port):
 *  - Tarayıcı sayfaları (QR ile açılan "telefondan gönder"): 6 haneli eşleştirme kodu + çerez oturumu ister.
 *  - "/api" ile başlayan kumanda arayüzü: eşleştirmede verilen Bearer anahtarını ister.
 * Kod, TV ekranında gösterilir; 5 hatalı denemeden sonra 60 sn kilitlenir.
 */
class LocalControlServer(
    context: Context,
    private val handler: ControlServerHandler
) {
    private val appContext = context.applicationContext
    private val tokenStore = RemoteTokenStore(appContext)
    private val random = SecureRandom()
    private val webSessions = HashSet<String>()

    @Volatile
    var pin: String = newPin()
        private set

    @Volatile
    var port: Int = 0
        private set

    @Volatile
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var failedAttempts = 0

    @Volatile
    private var lockedUntilMillis = 0L

    val isRunning: Boolean
        get() {
            val s = serverSocket
            return s != null && !s.isClosed && s.isBound
        }

    val pairedCount: Int
        get() = tokenStore.count()

    private fun newPin(): String {
        val n = random.nextInt(1_000_000)
        return n.toString().padStart(6, '0')
    }

    fun regeneratePin() {
        pin = newPin()
        failedAttempts = 0
        synchronized(webSessions) { webSessions.clear() }
    }

    fun revokeAllRemotes() {
        tokenStore.clear()
    }

    fun start(scope: CoroutineScope, onBound: (Int) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                serverSocket?.close()
            } catch (_: Throwable) {
            }
            var bound: ServerSocket? = null
            var boundPort = 0
            for (candidate in 8585..8595) {
                try {
                    val s = ServerSocket()
                    s.reuseAddress = true
                    s.bind(InetSocketAddress(candidate))
                    bound = s
                    boundPort = candidate
                    break
                } catch (_: Throwable) {
                }
            }
            val server = bound ?: return@launch
            serverSocket = server
            port = boundPort
            onBound(boundPort)

            while (isActive && !server.isClosed) {
                val client = try {
                    server.accept()
                } catch (_: Throwable) {
                    break
                }
                launch(Dispatchers.IO) { handleClient(client) }
            }
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
        } catch (_: Throwable) {
        }
        serverSocket = null
    }

    // ------------------------------------------------------------------ HTTP

    private class HttpRequest(
        val method: String,
        val path: String,
        val query: Map<String, String>,
        val headers: Map<String, String>,
        val body: ByteArray
    ) {
        fun header(name: String): String? = headers[name.lowercase()]
        fun bodyText(): String = String(body, Charsets.UTF_8)
    }

    private suspend fun handleClient(client: Socket) {
        try {
            client.soTimeout = 10_000
            val request = readRequest(client.getInputStream()) ?: return
            route(request, client.getOutputStream())
        } catch (_: Throwable) {
        } finally {
            try {
                client.close()
            } catch (_: Throwable) {
            }
        }
    }

    private fun readRequest(raw: InputStream): HttpRequest? {
        val input = BufferedInputStream(raw)
        val head = ByteArrayOutputStream()
        var window = 0
        var count = 0
        while (true) {
            val b = input.read()
            if (b < 0) return null
            head.write(b)
            window = (window shl 8) or (b and 0xFF)
            count++
            if (window == 0x0D0A0D0A) break
            if (count > 16 * 1024) return null
        }
        val headText = String(head.toByteArray(), Charsets.ISO_8859_1)
        val lines = headText.split("\r\n")
        val requestLine = lines.firstOrNull() ?: return null
        val parts = requestLine.split(" ")
        if (parts.size < 2) return null

        val method = parts[0].uppercase()
        val target = parts[1]
        val qIdx = target.indexOf('?')
        val path = if (qIdx >= 0) target.substring(0, qIdx) else target
        val query = if (qIdx >= 0) parseForm(target.substring(qIdx + 1)) else emptyMap()

        val headers = HashMap<String, String>()
        for (i in 1 until lines.size) {
            val line = lines[i]
            val idx = line.indexOf(':')
            if (idx > 0) {
                headers[line.substring(0, idx).trim().lowercase()] = line.substring(idx + 1).trim()
            }
        }
        val length = headers["content-length"]?.toIntOrNull() ?: 0
        if (length < 0 || length > MAX_BODY_BYTES) return null
        val body = ByteArray(length)
        var read = 0
        while (read < length) {
            val n = input.read(body, read, length - read)
            if (n < 0) return null
            read += n
        }
        return HttpRequest(method, path, query, headers, body)
    }

    private fun parseForm(body: String): Map<String, String> {
        val map = HashMap<String, String>()
        if (body.isBlank()) return map
        for (pair in body.split("&")) {
            val kv = pair.split("=", limit = 2)
            if (kv.size == 2) {
                try {
                    map[URLDecoder.decode(kv[0], "UTF-8")] = URLDecoder.decode(kv[1], "UTF-8")
                } catch (_: Exception) {
                }
            }
        }
        return map
    }

    private fun respond(
        out: OutputStream,
        status: Int,
        reason: String,
        contentType: String,
        body: ByteArray,
        extraHeaders: List<String> = emptyList()
    ) {
        val head = StringBuilder()
        head.append("HTTP/1.1 ").append(status).append(' ').append(reason).append("\r\n")
        head.append("Content-Type: ").append(contentType).append("\r\n")
        head.append("Content-Length: ").append(body.size).append("\r\n")
        head.append("Connection: close\r\n")
        head.append("Cache-Control: no-store\r\n")
        for (h in extraHeaders) head.append(h).append("\r\n")
        head.append("\r\n")
        out.write(head.toString().toByteArray(Charsets.ISO_8859_1))
        out.write(body)
        out.flush()
    }

    private fun respondHtml(out: OutputStream, html: String, status: Int = 200) {
        val reason = if (status == 200) "OK" else "Error"
        respond(out, status, reason, "text/html; charset=utf-8", html.toByteArray(Charsets.UTF_8))
    }

    private fun respondJson(out: OutputStream, status: Int, json: JSONObject) {
        val reason = when (status) {
            200 -> "OK"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            403 -> "Forbidden"
            404 -> "Not Found"
            429 -> "Too Many Requests"
            else -> "Error"
        }
        respond(out, status, reason, "application/json; charset=utf-8", json.toString().toByteArray(Charsets.UTF_8))
    }

    private fun redirect(out: OutputStream, location: String, extraHeaders: List<String> = emptyList()) {
        val headers = ArrayList<String>()
        headers.add("Location: $location")
        headers.addAll(extraHeaders)
        respond(out, 303, "See Other", "text/plain; charset=utf-8", ByteArray(0), headers)
    }

    // ------------------------------------------------------------------ Web (tarayıcı) rotaları

    private fun lockedRemainingSeconds(): Int {
        val remaining = lockedUntilMillis - System.currentTimeMillis()
        return if (remaining > 0) ((remaining + 999) / 1000).toInt() else 0
    }

    private fun isWebAuthorized(req: HttpRequest): Boolean {
        val cookie = req.header("cookie") ?: return false
        for (piece in cookie.split(";")) {
            val kv = piece.trim().split("=", limit = 2)
            if (kv.size == 2 && kv[0] == "redox_sid") {
                val sid = kv[1]
                synchronized(webSessions) {
                    return webSessions.contains(sid)
                }
            }
        }
        return false
    }

    private fun pinMatches(candidate: String): Boolean {
        return MessageDigest.isEqual(candidate.toByteArray(Charsets.UTF_8), pin.toByteArray(Charsets.UTF_8))
    }

    /** @return true = kod doğru. Hatalı denemeleri sayar ve gerekirse kilitler. */
    private fun checkPin(candidate: String): Boolean {
        if (candidate.isNotEmpty() && pinMatches(candidate)) {
            failedAttempts = 0
            return true
        }
        failedAttempts += 1
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            lockedUntilMillis = System.currentTimeMillis() + LOCK_MILLIS
            failedAttempts = 0
        }
        return false
    }

    private fun randomToken(): String {
        val bytes = ByteArray(24)
        random.nextBytes(bytes)
        val sb = StringBuilder(48)
        for (b in bytes) {
            sb.append("%02x".format(b))
        }
        return sb.toString()
    }

    private suspend fun route(req: HttpRequest, out: OutputStream) {
        if (req.path.startsWith("/api/")) {
            routeApi(req, out)
            return
        }

        if (req.path == "/login" && req.method == "POST") {
            if (lockedRemainingSeconds() > 0) {
                respondHtml(out, LocalTransferWebServer.buildLoginHtml(lockedRemainingSeconds(), false), 429)
                return
            }
            val form = parseForm(req.bodyText())
            if (checkPin(form["pin"].orEmpty().trim())) {
                val sid = randomToken()
                synchronized(webSessions) {
                    webSessions.add(sid)
                    while (webSessions.size > 8) {
                        val first = webSessions.firstOrNull() ?: break
                        webSessions.remove(first)
                    }
                }
                redirect(out, "/", listOf("Set-Cookie: redox_sid=$sid; Path=/; HttpOnly; SameSite=Lax"))
            } else {
                respondHtml(out, LocalTransferWebServer.buildLoginHtml(lockedRemainingSeconds(), true), 403)
            }
            return
        }

        if (!isWebAuthorized(req)) {
            respondHtml(out, LocalTransferWebServer.buildLoginHtml(lockedRemainingSeconds(), false))
            return
        }

        when {
            req.path == "/backup" -> {
                val bytes = handler.onExportBackup().toByteArray(Charsets.UTF_8)
                respond(
                    out, 200, "OK", "text/plain; charset=utf-8", bytes,
                    listOf("Content-Disposition: attachment; filename=\"redox_tv_backup.txt\"")
                )
            }
            req.path == "/add_m3u" && req.method == "POST" -> {
                val form = parseForm(req.bodyText())
                handler.onAddM3u(
                    form["name"].orEmpty().ifBlank { "Telefondan M3U" },
                    form["url"].orEmpty().trim(),
                    form["raw_content"].orEmpty().trim()
                )
                redirect(out, "/?ok=1")
            }
            req.path == "/add_xtream" && req.method == "POST" -> {
                val form = parseForm(req.bodyText())
                handler.onAddXtream(
                    form["name"].orEmpty().ifBlank { "Xtream Paketi" },
                    form["server"].orEmpty(),
                    form["username"].orEmpty(),
                    form["password"].orEmpty()
                )
                redirect(out, "/?ok=1")
            }
            req.path == "/restore_backup" && req.method == "POST" -> {
                val form = parseForm(req.bodyText())
                val ok = handler.onRestoreBackup(form["backup_data"].orEmpty())
                redirect(out, if (ok) "/?ok=1" else "/?ok=0")
            }
            else -> {
                val notice = when (req.query["ok"]) {
                    "1" -> "TV'ye gönderildi."
                    "0" -> "İşlem başarısız: içerik geçerli değil."
                    else -> ""
                }
                respondHtml(out, LocalTransferWebServer.buildMobileWebHtml(notice))
            }
        }
    }

    // ------------------------------------------------------------------ Kumanda API'si

    private fun tryJson(text: String): JSONObject? {
        if (text.isBlank()) return null
        return try {
            JSONObject(text)
        } catch (_: Exception) {
            null
        }
    }

    private fun isApiAuthorized(req: HttpRequest): Boolean {
        val auth = req.header("authorization") ?: return false
        val token = auth.removePrefix("Bearer ").trim()
        if (token.isEmpty()) return false
        return tokenStore.tokens().contains(token)
    }

    private fun okJson(): JSONObject {
        val o = JSONObject()
        o.put("ok", true)
        return o
    }

    private fun errorJson(code: String): JSONObject {
        val o = JSONObject()
        o.put("error", code)
        return o
    }

    private suspend fun routeApi(req: HttpRequest, out: OutputStream) {
        val path = req.path

        if (path == "/api/hello" && req.method == "GET") {
            val o = JSONObject()
            o.put("app", RemoteProtocol.APP_ID)
            o.put("version", BuildConfig.VERSION_NAME)
            o.put("name", handler.remoteDeviceName())
            respondJson(out, 200, o)
            return
        }

        if (path == "/api/pair" && req.method == "POST") {
            handlePair(req, out)
            return
        }

        if (!isApiAuthorized(req)) {
            respondJson(out, 401, errorJson("unauthorized"))
            return
        }

        val json = tryJson(req.bodyText()) ?: JSONObject()
        when (path) {
            "/api/state" -> respondJson(out, 200, RemoteProtocol.snapshotToJson(handler.remoteSnapshot()))
            "/api/channels" -> {
                val arr = JSONArray()
                for (c in handler.remoteChannels()) arr.put(RemoteProtocol.channelToJson(c))
                val o = JSONObject()
                o.put("channels", arr)
                respondJson(out, 200, o)
            }
            "/api/play" -> {
                val ok = handler.remotePlay(json.optString("id", ""))
                respondJson(out, if (ok) 200 else 404, if (ok) okJson() else errorJson("not_found"))
            }
            "/api/zap" -> {
                handler.remoteZap(json.optInt("delta", 1))
                respondJson(out, 200, okJson())
            }
            "/api/key" -> {
                val ok = handler.remoteKey(json.optString("key", ""))
                respondJson(out, if (ok) 200 else 400, if (ok) okJson() else errorJson("bad_key"))
            }
            "/api/volume" -> {
                val absolute = if (json.has("level")) json.optInt("level", -1) else -1
                val snapshot = handler.remoteVolume(json.optInt("delta", 0), absolute)
                respondJson(out, 200, RemoteProtocol.snapshotToJson(snapshot))
            }
            "/api/playpause" -> {
                handler.remotePlayPause()
                respondJson(out, 200, okJson())
            }
            "/api/stop" -> {
                handler.remoteStop()
                respondJson(out, 200, okJson())
            }
            "/api/cast" -> {
                val url = json.optString("url", "").trim()
                val ok = url.startsWith("http://", ignoreCase = true) ||
                    url.startsWith("https://", ignoreCase = true)
                if (ok) {
                    handler.remoteCast(
                        json.optString("name", "Telefondan Yayın").ifBlank { "Telefondan Yayın" },
                        url,
                        json.optString("logo", "")
                    )
                    respondJson(out, 200, okJson())
                } else {
                    respondJson(out, 400, errorJson("bad_url"))
                }
            }
            else -> respondJson(out, 404, errorJson("not_found"))
        }
    }

    private fun handlePair(req: HttpRequest, out: OutputStream) {
        val now = System.currentTimeMillis()
        if (now < lockedUntilMillis) {
            val o = errorJson("locked")
            o.put("retry", lockedRemainingSeconds())
            respondJson(out, 429, o)
            return
        }
        val json = tryJson(req.bodyText())
        val pinInput = (json?.optString("pin", "") ?: "").trim()
        val deviceName = (json?.optString("name", "") ?: "").trim().ifEmpty { "Telefon" }.take(40)
        if (checkPin(pinInput)) {
            val token = randomToken()
            tokenStore.add(token, deviceName)
            handler.remotePaired(deviceName)
            val o = JSONObject()
            o.put("token", token)
            o.put("name", handler.remoteDeviceName())
            respondJson(out, 200, o)
        } else {
            respondJson(out, 403, errorJson("bad_pin"))
        }
    }

    companion object {
        private const val MAX_BODY_BYTES = 8 * 1024 * 1024
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCK_MILLIS = 60_000L
    }
}
