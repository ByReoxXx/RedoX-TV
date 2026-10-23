package com.example.chromecasttv

import java.io.ByteArrayOutputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLException
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import okhttp3.OkHttpClient

/**
 * HTTPS güvenlik politikası.
 *
 * Eski sürümlerde sertifika doğrulaması TÜM uygulama için kapatılıyordu. Artık:
 *  - Kendi altyapımız (GitHub kanal listesi, güncelleme denetimi) her zaman KATI doğrulanır.
 *  - Kullanıcının eklediği oynatma listesi/rehber adresleri önce katı denenir; yalnızca sertifika hatası
 *    olursa ve izin verilmişse gevşek modda yeniden denenir.
 *  - Canlı yayın akışları (bölgesel yayıncıların kendinden imzalı sertifikaları için) gevşek modda oynatılır;
 *    bu yalnızca oynatıcının kendi HTTP istemcisine uygulanır, uygulama genelini etkilemez.
 */
object HttpSecurity {
    const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"

    private val trustAllManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    private val lenientSslContext: SSLContext by lazy {
        val ctx = SSLContext.getInstance("TLS")
        ctx.init(null, arrayOf<TrustManager>(trustAllManager), SecureRandom())
        ctx
    }

    private val lenientHostnameVerifier = HostnameVerifier { _, _ -> true }

    private val baseLenientClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .sslSocketFactory(lenientSslContext.socketFactory, trustAllManager)
            .hostnameVerifier(lenientHostnameVerifier)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private val streamClients = ConcurrentHashMap<Long, OkHttpClient>()

    /** Canlı yayın oynatıcısı için (gevşek TLS, yeniden yönlendirmeler açık) OkHttp istemcisi. */
    fun streamClient(connectTimeoutMs: Int, readTimeoutMs: Int): OkHttpClient {
        val key = connectTimeoutMs.toLong() * 1_000_000L + readTimeoutMs.toLong()
        return streamClients.getOrPut(key) {
            baseLenientClient.newBuilder()
                .connectTimeout(connectTimeoutMs.toLong(), TimeUnit.MILLISECONDS)
                .readTimeout(readTimeoutMs.toLong(), TimeUnit.MILLISECONDS)
                .build()
        }
    }

    /** Bağlantı nesnesi açar (henüz bağlanmaz). [lenient] true ise yalnızca BU bağlantıda doğrulama gevşetilir. */
    fun open(
        urlString: String,
        lenient: Boolean,
        connectTimeoutMs: Int = 8_000,
        readTimeoutMs: Int = 12_000
    ): HttpURLConnection {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        if (lenient && connection is HttpsURLConnection) {
            connection.sslSocketFactory = lenientSslContext.socketFactory
            connection.hostnameVerifier = lenientHostnameVerifier
        }
        connection.connectTimeout = connectTimeoutMs
        connection.readTimeout = readTimeoutMs
        connection.instanceFollowRedirects = true
        return connection
    }

    /**
     * Adresi açıp gövde akışını döndürür. Önce katı TLS dener; sertifika hatasında ve [allowLenientFallback]
     * true ise gevşek modda yeniden dener. Dönen akış kapatıldığında bağlantı da kapatılır.
     */
    fun openInputStream(
        urlString: String,
        allowLenientFallback: Boolean,
        userAgent: String = USER_AGENT,
        connectTimeoutMs: Int = 8_000,
        readTimeoutMs: Int = 12_000
    ): InputStream {
        try {
            return openOnce(urlString, false, userAgent, connectTimeoutMs, readTimeoutMs)
        } catch (e: SSLException) {
            if (!allowLenientFallback) throw e
        }
        return openOnce(urlString, true, userAgent, connectTimeoutMs, readTimeoutMs)
    }

    private fun openOnce(
        urlString: String,
        lenient: Boolean,
        userAgent: String,
        connectTimeoutMs: Int,
        readTimeoutMs: Int
    ): InputStream {
        val connection = open(urlString, lenient, connectTimeoutMs, readTimeoutMs)
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", userAgent)
        val code = connection.responseCode
        if (code !in 200..299) {
            connection.disconnect()
            throw IOException("HTTP $code")
        }
        val stream = connection.inputStream
        return object : FilterInputStream(stream) {
            override fun close() {
                try {
                    super.close()
                } finally {
                    connection.disconnect()
                }
            }
        }
    }

    /** Metni indirir; [maxBytes] sınırını aşan yanıtlar reddedilir (bellek koruması). */
    fun readText(
        urlString: String,
        allowLenientFallback: Boolean,
        userAgent: String = USER_AGENT,
        connectTimeoutMs: Int = 8_000,
        readTimeoutMs: Int = 12_000,
        maxBytes: Long = 30_000_000L
    ): String {
        openInputStream(urlString, allowLenientFallback, userAgent, connectTimeoutMs, readTimeoutMs).use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            var total = 0L
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > maxBytes) throw IOException("Yanıt çok büyük")
                out.write(buffer, 0, read)
            }
            return String(out.toByteArray(), Charsets.UTF_8)
        }
    }
}
