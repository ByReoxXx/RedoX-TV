package com.example.chromecasttv

import android.content.Context
import android.util.Xml
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.GZIPInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser

// ============================================================================
// Gerçek yayın rehberi (XMLTV)
// Eski sürümlerde rehber, kategori şablonlarından üretilen uydurma verilerdi.
// Bu katman XMLTV (.xml / .xml.gz) dosyalarını akışla okur, kanallarla eşleştirir ve önbelleğe alır.
// ============================================================================

data class XmltvProgramme(
    val channelId: String,
    val title: String,
    val description: String,
    val category: String,
    val startMillis: Long,
    val endMillis: Long
)

class XmltvParseResult(
    /** xmltv kanal kimliği -> görünen adlar */
    val channelNames: Map<String, List<String>>,
    /** xmltv kanal kimliği -> (yalnızca istenen kanallar ve zaman penceresi) programlar */
    val programmes: Map<String, List<XmltvProgramme>>
)

object XmltvParser {
    /**
     * @param wantedSelector kanal listesi okunduktan sonra bir kez çağrılır ve hangi xmltv kanallarının
     *        saklanacağını döndürür (belleği küçük tutmak için).
     */
    fun parse(
        input: InputStream,
        windowStartMillis: Long,
        windowEndMillis: Long,
        wantedSelector: (Map<String, List<String>>) -> Set<String>
    ): XmltvParseResult {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)

        val channelNames = LinkedHashMap<String, MutableList<String>>()
        val programmes = HashMap<String, MutableList<XmltvProgramme>>()
        var wanted: Set<String>? = null

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                val tag = parser.name
                if (tag == "channel") {
                    readChannel(parser, channelNames)
                } else if (tag == "programme") {
                    val selected = wanted ?: wantedSelector(channelNames).also { wanted = it }
                    readProgramme(parser, selected, windowStartMillis, windowEndMillis, programmes)
                }
            }
            event = parser.next()
        }
        return XmltvParseResult(channelNames, programmes)
    }

    private fun readChannel(parser: XmlPullParser, out: MutableMap<String, MutableList<String>>) {
        val id = parser.getAttributeValue(null, "id") ?: ""
        val names: MutableList<String>? = if (id.isBlank()) null else out.getOrPut(id) { ArrayList() }
        var depth = 1
        while (depth > 0) {
            val ev = parser.next()
            if (ev == XmlPullParser.START_TAG) {
                if (parser.name == "display-name") {
                    val text = safeNextText(parser)
                    if (text.isNotBlank() && names != null) names.add(text.trim())
                } else {
                    depth++
                }
            } else if (ev == XmlPullParser.END_TAG) {
                depth--
            } else if (ev == XmlPullParser.END_DOCUMENT) {
                return
            }
        }
    }

    private fun readProgramme(
        parser: XmlPullParser,
        wanted: Set<String>,
        windowStart: Long,
        windowEnd: Long,
        out: MutableMap<String, MutableList<XmltvProgramme>>
    ) {
        val channel = parser.getAttributeValue(null, "channel") ?: ""
        val start = parseTime(parser.getAttributeValue(null, "start"))
        val stop = parseTime(parser.getAttributeValue(null, "stop"))
        val end = if (stop > start) stop else start + 30 * 60_000L

        if (channel.isBlank() || start <= 0L || !wanted.contains(channel) || end < windowStart || start > windowEnd) {
            skipElement(parser)
            return
        }

        var title = ""
        var desc = ""
        var category = ""
        var depth = 1
        while (depth > 0) {
            val ev = parser.next()
            if (ev == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "title" -> {
                        val t = safeNextText(parser).trim()
                        if (title.isEmpty()) title = t
                    }
                    "desc" -> {
                        val t = safeNextText(parser).trim()
                        if (desc.isEmpty()) desc = t.take(320)
                    }
                    "category" -> {
                        val t = safeNextText(parser).trim()
                        if (category.isEmpty()) category = t
                    }
                    else -> depth++
                }
            } else if (ev == XmlPullParser.END_TAG) {
                depth--
            } else if (ev == XmlPullParser.END_DOCUMENT) {
                break
            }
        }
        if (title.isEmpty()) return
        out.getOrPut(channel) { ArrayList() }
            .add(XmltvProgramme(channel, title, desc, category, start, end))
    }

    private fun skipElement(parser: XmlPullParser) {
        var depth = 1
        while (depth > 0) {
            val ev = parser.next()
            if (ev == XmlPullParser.START_TAG) {
                depth++
            } else if (ev == XmlPullParser.END_TAG) {
                depth--
            } else if (ev == XmlPullParser.END_DOCUMENT) {
                return
            }
        }
    }

    private fun safeNextText(parser: XmlPullParser): String {
        return try {
            parser.nextText() ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    /** "20261023180000 +0300" biçimini (ofset yoksa UTC kabul edilir) milisaniyeye çevirir. */
    fun parseTime(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        val parts = raw.trim().split(" ").filter { it.isNotEmpty() }
        if (parts.isEmpty()) return 0L
        val digits = parts[0]
        if (digits.length < 12 || !digits.all { it.isDigit() }) return 0L
        val year = digits.substring(0, 4).toInt()
        val month = digits.substring(4, 6).toInt()
        val day = digits.substring(6, 8).toInt()
        val hour = digits.substring(8, 10).toInt()
        val minute = digits.substring(10, 12).toInt()
        val second = if (digits.length >= 14) digits.substring(12, 14).toInt() else 0

        var offsetMinutes = 0
        if (parts.size > 1) {
            val z = parts[1]
            if (z.length >= 5 && (z[0] == '+' || z[0] == '-')) {
                val h = z.substring(1, 3).toIntOrNull() ?: 0
                val m = z.substring(3, 5).toIntOrNull() ?: 0
                val sign = if (z[0] == '-') -1 else 1
                offsetMinutes = sign * (h * 60 + m)
            }
        }
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.clear()
        cal.set(year, month - 1, day, hour, minute, second)
        return cal.timeInMillis - offsetMinutes * 60_000L
    }
}

/** Uygulama kanallarını XMLTV kanallarıyla eşleştirir. */
object EpgMatcher {
    private val noiseWords = Regex("\\b(tv|hd|fhd|uhd|sd|4k|tr|turkiye|turk|kanali|kanal|live|backup|canli|yedek)\\b")
    private val bracketed = Regex("\\[.*?\\]|\\(.*?\\)")
    private val nonAlnum = Regex("[^a-z0-9]")

    fun normalize(name: String): String {
        return name
            .replace(bracketed, " ")
            .replace('İ', 'i').replace('I', 'i').replace('ı', 'i')
            .lowercase(Locale.ROOT)
            .replace('ş', 's').replace('ğ', 'g').replace('ü', 'u').replace('ö', 'o').replace('ç', 'c')
            .replace(noiseWords, " ")
            .replace(nonAlnum, "")
    }

    /** @return uygulama kanal kimliği -> xmltv kanal kimliği */
    fun matchChannels(appChannels: List<TvChannel>, xmltv: Map<String, List<String>>): Map<String, String> {
        if (xmltv.isEmpty()) return emptyMap()

        val lowerIdToId = HashMap<String, String>()
        val normIdToId = HashMap<String, String>()
        val normNameToId = HashMap<String, String>()
        for ((id, names) in xmltv) {
            lowerIdToId[id.lowercase(Locale.ROOT)] = id
            val nid = normalize(id)
            if (nid.isNotEmpty() && !normIdToId.containsKey(nid)) normIdToId[nid] = id
            for (n in names) {
                val key = normalize(n)
                if (key.isNotEmpty() && !normNameToId.containsKey(key)) normNameToId[key] = id
            }
        }

        val result = HashMap<String, String>()
        for (ch in appChannels) {
            var found: String? = null

            val idCandidates = ArrayList<String>(2)
            if (ch.tvgId.isNotBlank()) idCandidates.add(ch.tvgId)
            if (ch.epgChannelId.isNotBlank() && ch.epgChannelId != ch.id) idCandidates.add(ch.epgChannelId)

            for (c in idCandidates) {
                val hit = lowerIdToId[c.lowercase(Locale.ROOT)]
                if (hit != null) {
                    found = hit
                    break
                }
            }
            if (found == null) {
                for (c in idCandidates) {
                    val key = normalize(c)
                    val hit = if (key.isNotEmpty()) (normIdToId[key] ?: normNameToId[key]) else null
                    if (hit != null) {
                        found = hit
                        break
                    }
                }
            }
            if (found == null) {
                val key = normalize(ch.name)
                if (key.isNotEmpty()) {
                    found = normNameToId[key]
                    if (found == null && key.length >= 5) {
                        for ((candidateKey, id) in normNameToId) {
                            if (candidateKey.length >= 5 &&
                                (candidateKey.startsWith(key) || key.startsWith(candidateKey))
                            ) {
                                found = id
                                break
                            }
                        }
                    }
                }
            }
            if (found != null) result[ch.id] = found
        }
        return result
    }
}

/** XMLTV indirme, ayrıştırma ve diskte önbellekleme. */
class EpgRepository(context: Context) {
    private val appContext = context.applicationContext
    private val cacheFile = File(appContext.filesDir, "epg_cache_v1.json")

    class EpgLoad(val byChannelId: Map<String, List<EpgProgram>>, val status: EpgStatus)

    fun loadCache(channels: List<TvChannel>): EpgLoad? {
        return try {
            if (!cacheFile.exists()) return null
            val root = JSONObject(cacheFile.readText())
            val updated = root.optLong("updated", 0L)
            val data = root.optJSONObject("data") ?: return null
            val now = System.currentTimeMillis()
            val known = HashMap<String, TvChannel>()
            for (ch in channels) known[ch.id] = ch

            val out = HashMap<String, List<EpgProgram>>()
            var programmeCount = 0
            val keys = data.keys()
            while (keys.hasNext()) {
                val channelId = keys.next()
                val channel = known[channelId] ?: continue
                val arr = data.optJSONArray(channelId) ?: continue
                val list = ArrayList<EpgProgram>(arr.length())
                for (i in 0 until arr.length()) {
                    val row = arr.optJSONArray(i) ?: continue
                    val start = row.optLong(0, 0L)
                    val end = row.optLong(1, 0L)
                    if (end < now - 3 * HOUR_MS) continue
                    list.add(
                        EpgProgram(
                            channelId = channel.epgChannelId,
                            title = row.optString(2, ""),
                            description = row.optString(3, ""),
                            category = row.optString(4, ""),
                            startTimeMillis = start,
                            endTimeMillis = end,
                            timeWindowText = windowText(start, end)
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    out[channelId] = list
                    programmeCount += list.size
                }
            }
            if (out.isEmpty()) return null
            EpgLoad(
                out,
                EpgStatus(
                    sourceCount = 0,
                    matchedChannels = out.size,
                    programmeCount = programmeCount,
                    lastUpdatedMillis = updated,
                    message = "Önbellekten yüklendi"
                )
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun refresh(urls: List<String>, channels: List<TvChannel>): EpgLoad = withContext(Dispatchers.IO) {
        val cleanUrls = urls.map { it.trim() }
            .filter { it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true) }
            .distinct()
        if (cleanUrls.isEmpty()) {
            return@withContext EpgLoad(emptyMap(), EpgStatus(message = "Rehber (XMLTV) adresi eklenmedi."))
        }

        val now = System.currentTimeMillis()
        val windowStart = now - 3 * HOUR_MS
        val windowEnd = now + 48 * HOUR_MS
        val merged = HashMap<String, MutableList<EpgProgram>>()
        val channelById = HashMap<String, TvChannel>()
        for (ch in channels) channelById[ch.id] = ch
        var okSources = 0
        var lastError = ""

        for (url in cleanUrls) {
            try {
                val raw = HttpSecurity.openInputStream(
                    url,
                    allowLenientFallback = true,
                    userAgent = "RedoXTV/2.0 (XMLTV)",
                    connectTimeoutMs = 10_000,
                    readTimeoutMs = 30_000
                )
                raw.use { stream ->
                    val input = wrapMaybeGzip(stream)
                    var mapping: Map<String, String> = emptyMap()
                    val parsed = XmltvParser.parse(input, windowStart, windowEnd) { names ->
                        mapping = EpgMatcher.matchChannels(channels, names)
                        mapping.values.toSet()
                    }
                    for ((appId, xmltvId) in mapping) {
                        val channel = channelById[appId] ?: continue
                        if (merged.containsKey(appId)) continue
                        val items = parsed.programmes[xmltvId] ?: continue
                        if (items.isEmpty()) continue
                        val converted = items.sortedBy { it.startMillis }.map { p ->
                            EpgProgram(
                                channelId = channel.epgChannelId,
                                title = p.title,
                                description = p.description,
                                category = p.category,
                                startTimeMillis = p.startMillis,
                                endTimeMillis = p.endMillis,
                                timeWindowText = windowText(p.startMillis, p.endMillis)
                            )
                        }
                        merged[appId] = converted.toMutableList()
                    }
                    okSources++
                }
            } catch (e: Exception) {
                lastError = e.message ?: e.javaClass.simpleName
            }
        }

        if (merged.isEmpty()) {
            val msg = if (okSources == 0) {
                "Rehber indirilemedi: $lastError"
            } else {
                "Rehber indirildi ama kanallarla eşleşme bulunamadı."
            }
            return@withContext EpgLoad(emptyMap(), EpgStatus(sourceCount = cleanUrls.size, message = msg))
        }

        writeCache(now, merged)
        var programmeCount = 0
        for (list in merged.values) programmeCount += list.size
        EpgLoad(
            merged.mapValues { it.value.toList() },
            EpgStatus(
                sourceCount = cleanUrls.size,
                matchedChannels = merged.size,
                programmeCount = programmeCount,
                lastUpdatedMillis = now,
                message = "${merged.size} kanal eşleşti, $programmeCount program yüklendi."
            )
        )
    }

    private fun writeCache(updated: Long, data: Map<String, List<EpgProgram>>) {
        try {
            val dataObj = JSONObject()
            for ((channelId, list) in data) {
                val arr = JSONArray()
                for (p in list) {
                    val row = JSONArray()
                    row.put(p.startTimeMillis)
                    row.put(p.endTimeMillis)
                    row.put(p.title)
                    row.put(p.description)
                    row.put(p.category)
                    arr.put(row)
                }
                dataObj.put(channelId, arr)
            }
            val root = JSONObject()
            root.put("updated", updated)
            root.put("data", dataObj)
            cacheFile.writeText(root.toString())
        } catch (_: Exception) {
        }
    }

    private fun wrapMaybeGzip(raw: InputStream): InputStream {
        val buffered = BufferedInputStream(raw, 64 * 1024)
        buffered.mark(2)
        val b1 = buffered.read()
        val b2 = buffered.read()
        buffered.reset()
        return if (b1 == 0x1f && b2 == 0x8b) GZIPInputStream(buffered, 64 * 1024) else buffered
    }

    companion object {
        private const val HOUR_MS = 60L * 60L * 1000L

        fun windowText(startMillis: Long, endMillis: Long): String {
            val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
            return "${fmt.format(Date(startMillis))} - ${fmt.format(Date(endMillis))}"
        }
    }
}

/** Gerçek rehber ve özel ad/numara bindirmelerini kanal listelerine uygular. */
object FeatureOverlays {
    fun applyChannels(
        channels: List<TvChannel>,
        realEpg: Map<String, List<EpgProgram>>,
        overrides: Map<String, ChannelOverride>,
        nowMillis: Long
    ): List<TvChannel> {
        if (realEpg.isEmpty() && overrides.isEmpty()) return channels
        return channels.map { ch ->
            var out = ch

            val programmes = realEpg[ch.id]
            if (programmes != null && programmes.isNotEmpty()) {
                var current: EpgProgram? = null
                for (p in programmes) {
                    if (nowMillis >= p.startTimeMillis && nowMillis < p.endTimeMillis) {
                        current = p
                        break
                    }
                }
                val boundary = current?.endTimeMillis ?: nowMillis
                var next: EpgProgram? = null
                for (p in programmes) {
                    if (p.startTimeMillis >= boundary) {
                        next = p
                        break
                    }
                }
                if (current != null || next != null) {
                    out = out.copy(
                        currentProgramTitle = current?.title ?: "Canlı Yayın",
                        currentProgramTime = current?.timeWindowText ?: "Canlı",
                        nextProgramTitle = next?.title ?: "—",
                        currentProgress = current?.progressFraction(nowMillis) ?: 0f,
                        hasRealEpg = true
                    )
                }
            }

            val override = overrides[ch.id]
            if (override != null) {
                val newName = override.name?.trim()?.takeIf { it.isNotEmpty() } ?: out.name
                out = out.copy(
                    name = newName,
                    shortLogoText = if (newName != out.name) M3uAndEpgParser.makeShortLogoText(newName) else out.shortLogoText,
                    number = override.number ?: out.number
                )
            }
            out
        }
    }

    fun mergeEpgMap(
        epg: Map<String, List<EpgProgram>>,
        channels: List<TvChannel>,
        realEpg: Map<String, List<EpgProgram>>
    ): Map<String, List<EpgProgram>> {
        if (realEpg.isEmpty()) return epg
        val merged = HashMap<String, List<EpgProgram>>(epg)
        for (ch in channels) {
            val real = realEpg[ch.id]
            if (real != null && real.isNotEmpty()) merged[ch.epgChannelId] = real
        }
        return merged
    }
}
