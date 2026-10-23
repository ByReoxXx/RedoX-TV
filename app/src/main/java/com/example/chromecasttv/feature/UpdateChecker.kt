package com.example.chromecasttv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * GitHub Releases üzerinden sürüm denetimi. Katı HTTPS kullanılır; hiçbir kimlik/kullanım verisi gönderilmez.
 */
object UpdateChecker {
    suspend fun check(currentVersion: String, repo: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val text = HttpSecurity.readText(
                urlString = "https://api.github.com/repos/$repo/releases/latest",
                allowLenientFallback = false,
                userAgent = "RedoXTV-UpdateCheck",
                connectTimeoutMs = 8_000,
                readTimeoutMs = 10_000,
                maxBytes = 2_000_000L
            )
            val root = JSONObject(text)
            val tag = root.optString("tag_name", "").trim()
            if (tag.isEmpty()) {
                null
            } else {
                UpdateInfo(
                    currentVersion = currentVersion,
                    latestVersion = tag.removePrefix("v").removePrefix("V"),
                    isNewer = isNewerVersion(tag, currentVersion),
                    htmlUrl = root.optString("html_url", "https://github.com/$repo/releases"),
                    notes = root.optString("body", "").take(600)
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    fun isNewerVersion(candidate: String, current: String): Boolean {
        val a = versionParts(candidate)
        val b = versionParts(current)
        val size = maxOf(a.size, b.size)
        for (i in 0 until size) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    private fun versionParts(raw: String): List<Int> {
        return raw.trim()
            .removePrefix("v")
            .removePrefix("V")
            .split(".", "-", "+")
            .mapNotNull { piece -> piece.takeWhile { it.isDigit() }.toIntOrNull() }
    }
}
