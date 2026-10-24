package com.example.chromecasttv

import androidx.compose.runtime.Immutable

/**
 * Radyo sekmesinin ortak durum modeli (TV, telefon ve tablet aynı durumu çizer).
 *
 * Bu aşamada ses çalınmaz: [RadioHub] örnek veriyle bağlanma / çalma / yedek yayın / hata aşamalarını canlandırır.
 * İstasyon listesi yalnızca şifreli asset'ten ([RadioRepository]) okunur; koda okunur istasyon listesi yazılmaz.
 */
const val RADIO_CAT_ALL = "Tümü"
const val RADIO_CAT_FAVORITES = "Favoriler"

/** Radyo oynatıcısının görünür aşamaları; kadran arayüzü bu aşamalara göre çizilir. */
enum class RadioPhase { STOPPED, CONNECTING, PLAYING, PAUSED, FAILOVER, ERROR }

@Immutable
data class RadioSong(val artist: String, val title: String)

@Immutable
data class RadioUiState(
    val loaded: Boolean = false,
    /** Görünür istasyonlar (gizlenenler çıkarılmış), liste sırasıyla. */
    val stations: List<TvChannel> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val hiddenIds: Set<String> = emptySet(),
    val selectedCategory: String = RADIO_CAT_ALL,
    /** Çalan (ya da bağlanılan) istasyon. */
    val currentId: String? = null,
    /** Kadranda gezinilen, henüz çalmayan istasyon (kısa bir gecikmeden sonra çalınır). */
    val tuningId: String? = null,
    val phase: RadioPhase = RadioPhase.STOPPED,
    val sourceIndex: Int = 0,
    /** Alternatif yayın denemesi (1 tabanlı; 2 = ikinci yayın deneniyor). */
    val failoverAttempt: Int = 0,
    val song: RadioSong? = null,
    val sleepMinutes: Int? = null,
    /** Seçilen uyku süresi (30/60/90/120); [sleepMinutes] ise kalan süredir. */
    val sleepChoice: Int? = null,
    val playerOpen: Boolean = false,
    val spectrumOn: Boolean = true,
    val toast: String? = null
) {
    val current: TvChannel? get() = currentId?.let { id -> stations.firstOrNull { it.id == id } }
    val tuning: TvChannel? get() = tuningId?.let { id -> stations.firstOrNull { it.id == id } }

    /** Kadranın ortasındaki istasyon: gezinilen varsa o, yoksa çalan. */
    val dialStation: TvChannel? get() = tuning ?: current

    val isActive: Boolean get() = phase != RadioPhase.STOPPED && current != null
}

private val RadioCategoryOrder = listOf(
    "Pop", "Türkü & Halk", "Arabesk & Fantezi", "Nostalji", "Rock & Alternatif",
    "Dans & Elektronik", "Klasik & Sanat", "Haber & Spor", "Dini", "Yerel", "Karışık", "Demo"
)

/** Kategori çipleri: Tümü, Favoriler, sonra sabit sırayla türler (listede bulunanlar). */
fun radioCategories(stations: List<TvChannel>): List<String> {
    val present = stations.map { it.category }.toSet()
    val ordered = RadioCategoryOrder.filter { it in present && it != "Demo" } + present.filter { it !in RadioCategoryOrder }.sorted()
    // "Demo" (aşama denemesi için üç örnek istasyon) üçüncü çiptir: kaydırmadan erişilir
    val demo = if ("Demo" in present) listOf("Demo") else emptyList()
    return listOf(RADIO_CAT_ALL, RADIO_CAT_FAVORITES) + demo + ordered
}

fun radioCategoryCount(stations: List<TvChannel>, favoriteIds: Set<String>, category: String): Int = when (category) {
    RADIO_CAT_ALL -> stations.size
    RADIO_CAT_FAVORITES -> stations.count { it.id in favoriteIds }
    else -> stations.count { it.category == category }
}

fun radioFilter(stations: List<TvChannel>, favoriteIds: Set<String>, category: String): List<TvChannel> = when (category) {
    RADIO_CAT_ALL -> stations
    RADIO_CAT_FAVORITES -> stations.filter { it.id in favoriteIds }
    else -> stations.filter { it.category == category }
}

/** Yayın etiketinden ("Ana Yayın (MP3 128 kbps)") biçim metni: "MP3 128 kbps". */
fun radioFormatText(station: TvChannel, sourceIndex: Int): String {
    val label = station.sources.getOrNull(sourceIndex)?.label ?: return "Canlı"
    val open = label.lastIndexOf('(')
    val close = label.lastIndexOf(')')
    return if (open in 0 until close) label.substring(open + 1, close).trim() else "Canlı"
}

/** Alt başlık: "Pop · MP3 128 kbps". */
fun radioSubtitle(station: TvChannel, sourceIndex: Int): String = "${station.category} · ${radioFormatText(station, sourceIndex)}"

/** `demo://` adresli örnek istasyon mu (yalnız ekran görüntüsü derlemelerinde; gerçek ses çalmaz). */
fun radioIsDemo(station: TvChannel): Boolean = station.sources.firstOrNull()?.url?.startsWith("demo://") == true

/** Uyku zamanlayıcısı için görünen metin. */
fun radioSleepText(minutes: Int?): String = if (minutes == null) "Kapalı" else "$minutes dk"

/** Aşama rozetinin yazısı. */
fun radioBadgeText(phase: RadioPhase): String = when (phase) {
    RadioPhase.PLAYING -> "ÇALIYOR"
    RadioPhase.PAUSED -> "DURAKLADI"
    RadioPhase.CONNECTING -> "BAĞLANIYOR"
    RadioPhase.FAILOVER -> "YENİDEN BAĞLANIYOR"
    RadioPhase.ERROR -> "ÇALIŞMIYOR"
    RadioPhase.STOPPED -> "DURDU"
}

/** Aşamaya göre tek satırlık durum: çalan şarkı, bağlanma, yedek yayın ya da hata. [compact] dar alanlarda (mini çubuk) yedek yayın metnini kısaltır. */
fun radioStatusText(state: RadioUiState, compact: Boolean = false): String {
    val station = state.current
    return when (state.phase) {
        RadioPhase.PLAYING, RadioPhase.PAUSED -> state.song?.let { if (it.artist.isBlank()) it.title else "${it.title} — ${it.artist}" } ?: "Canlı yayın"
        RadioPhase.CONNECTING -> "Yayına bağlanılıyor…"
        RadioPhase.FAILOVER -> {
            val total = station?.sources?.size ?: state.failoverAttempt
            if (compact) "Yedek yayına geçiliyor (${state.failoverAttempt}/$total)"
            else "Yayın kesildi · alternatif yayına geçiliyor (${state.failoverAttempt}/$total)"
        }
        RadioPhase.ERROR -> "Bu istasyon şu an çalışmıyor"
        RadioPhase.STOPPED -> ""
    }
}
