package com.example.chromecasttv

import androidx.compose.runtime.Immutable
import kotlin.math.abs

// ============================================================================
// Tüm arayüzlerin (TV / tablet / telefon) ortak kullandığı yardımcılar
// ============================================================================

/** Gerçek rehber verisi yoksa uydurma program adı göstermeyiz. */
fun TvChannel.nowTitleOrLive(): String = if (hasRealEpg) currentProgramTitle else "Canlı yayın"

fun TvChannel.nowNextLine(): String {
    return if (hasRealEpg) {
        "Şu An: $currentProgramTitle ($currentProgramTime)  •  Sıradaki: $nextProgramTitle"
    } else {
        "Canlı yayın  •  ${sources.size} akış"
    }
}

fun TvChannel.progressForUi(): Float = if (hasRealEpg) currentProgress.coerceIn(0f, 1f) else 0f

enum class ShelfKind { CONTINUE, MOST_WATCHED, FAVORITES }

@Immutable
data class ChannelShelf(
    val kind: ShelfKind,
    val title: String,
    val channels: List<TvChannel>
)

/** "İzlemeye devam et", "En çok izlediklerin", "Favorilerinde şimdi yayında" raflarını üretir. */
object ShelfBuilder {
    fun build(ui: TvUiState, features: FeatureState, maxPerShelf: Int = 10): List<ChannelShelf> {
        val all = ui.builtInChannels + ui.customM3uChannels
        val byId = HashMap<String, TvChannel>(all.size * 2)
        for (c in all) byId[c.id] = c
        val shelves = ArrayList<ChannelShelf>(3)

        val activeId = ui.activePlayingChannel?.id
        val recent = ui.recentChannels.filter { it.id != activeId }.take(maxPerShelf)
        if (recent.isNotEmpty()) {
            shelves.add(ChannelShelf(ShelfKind.CONTINUE, "İzlemeye devam et", recent))
        }

        val top = features.watchStats.values
            .filter { it.totalMillis >= 120_000L || it.playCount >= 3 }
            .sortedByDescending { it.totalMillis }
            .mapNotNull { byId[it.channelId] }
            .take(maxPerShelf)
        if (top.size >= 2) {
            shelves.add(ChannelShelf(ShelfKind.MOST_WATCHED, "En çok izlediklerin", top))
        }

        val favorites = ui.orderedFavoriteChannels
        val liveFavorites = favorites.filter { it.hasRealEpg }.take(maxPerShelf)
        if (liveFavorites.isNotEmpty()) {
            shelves.add(ChannelShelf(ShelfKind.FAVORITES, "Favorilerinde şimdi yayında", liveFavorites))
        } else if (favorites.isNotEmpty()) {
            shelves.add(ChannelShelf(ShelfKind.FAVORITES, "Favorilerin", favorites.take(maxPerShelf)))
        }
        return shelves
    }
}

@Immutable
data class SearchHit(
    val channel: TvChannel,
    /** Arama bir program adıyla eşleştiyse o program (yalnızca gerçek rehberi olan kanallarda). */
    val program: EpgProgram?
) {
    /** Programla eşleşen sonucu kartta göstermek için kanal kopyası. */
    fun displayChannel(): TvChannel {
        val p = program ?: return channel
        return channel.copy(
            currentProgramTitle = p.title,
            currentProgramTime = p.timeWindowText,
            hasRealEpg = true
        )
    }
}

/** Kanal adı / kategori / numara + gerçek program adlarında, küçük yazım hatalarını tolere eden arama. */
object ChannelSearch {
    fun search(
        query: String,
        channels: List<TvChannel>,
        epgMap: Map<String, List<EpgProgram>>,
        limit: Int = 200
    ): List<SearchHit> {
        val q = query.trim()
        if (q.isEmpty()) return channels.map { SearchHit(it, null) }

        val number = q.toIntOrNull()
        val key = EpgMatcher.normalize(q)
        val out = ArrayList<SearchHit>()
        val added = HashSet<String>()

        for (ch in channels) {
            val nameKey = EpgMatcher.normalize(ch.name)
            val direct = ch.name.contains(q, ignoreCase = true) ||
                ch.category.contains(q, ignoreCase = true) ||
                (number != null && ch.number == number) ||
                (key.isNotEmpty() && nameKey.contains(key))
            if (direct || (key.length >= 4 && fuzzyTokenMatch(ch.name, key))) {
                out.add(SearchHit(ch, null))
                added.add(ch.id)
            }
        }

        if (key.length >= 3) {
            for (ch in channels) {
                if (!ch.hasRealEpg || added.contains(ch.id)) continue
                val programmes = epgMap[ch.epgChannelId] ?: continue
                val found = programmes.firstOrNull { p -> EpgMatcher.normalize(p.title).contains(key) }
                if (found != null) {
                    out.add(SearchHit(ch, found))
                    added.add(ch.id)
                }
            }
        }
        return if (out.size > limit) out.subList(0, limit) else out
    }

    private fun fuzzyTokenMatch(text: String, key: String): Boolean {
        for (token in text.split(" ", "-", ".", "_")) {
            val norm = EpgMatcher.normalize(token)
            if (norm.length < 3) continue
            if (abs(norm.length - key.length) <= 1 && editDistanceAtMostOne(norm, key)) return true
        }
        return false
    }

    /** İki metin arasındaki Levenshtein uzaklığı ≤ 1 mi? */
    private fun editDistanceAtMostOne(a: String, b: String): Boolean {
        if (a == b) return true
        val la = a.length
        val lb = b.length
        if (abs(la - lb) > 1) return false
        var i = 0
        var j = 0
        var edits = 0
        while (i < la && j < lb) {
            if (a[i] == b[j]) {
                i++
                j++
            } else {
                edits++
                if (edits > 1) return false
                if (la > lb) {
                    i++
                } else if (la < lb) {
                    j++
                } else {
                    i++
                    j++
                }
            }
        }
        edits += (la - i) + (lb - j)
        return edits <= 1
    }
}
