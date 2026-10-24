package com.example.chromecasttv

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Locale

// ============================================================================
// RedoX "Kızıl & Altın" tasarım sistemi — renk belirteçleri (design tokens)
// Renkler doğrudan kullanıcının logosundan ölçüldü:
//   Kızıl: #590823 · #96112F · #BD1436 · #F02848 · #FA6465     Altın: #A17844 · #DAA24F · #F3C05D · #FDDE7A
//   Zemin (lacivert-siyah): #0A0A14 · #161621 · #1F1D2A
// Tasarım dili "Kızıl Halka": logodaki altın halka = odak/seçim halkası; kırmızı = eylem ve canlı yayın.
// Tüm cihazlar aynı belirteçleri kullanır. Ad: Red + Ox = RedoX ("kırmızı boğa").
// ============================================================================

/** Kullanıcının seçebildiği vurgu paleti (logo her zaman kızıl-altın kalır; palet arayüz vurgularını değiştirir). */
@Immutable
data class RedoXPalette(
    val id: String,
    val label: String,
    val primary: Color,
    val primaryBright: Color,
    val primaryDeep: Color,
    val gradStart: Color,
    val gradEnd: Color,
    val secondary: Color,
    val onPrimary: Color
)

object RedoXPalettes {
    /** Varsayılan: logodan ölçülen kızıl (#F02848 ... #BD1436) + altın (#F3C05D). */
    val Crimson = RedoXPalette(
        id = "crimson", label = "Kızıl & Altın",
        primary = Color(0xFFF02848), primaryBright = Color(0xFFFA6465), primaryDeep = Color(0xFFBD1436),
        gradStart = Color(0xFFF02848), gradEnd = Color(0xFFBD1436),
        secondary = Color(0xFFF3C05D),
        onPrimary = Color.White
    )
    val Ocean = RedoXPalette(
        id = "ocean", label = "Okyanus",
        primary = Color(0xFF3B9CFF), primaryBright = Color(0xFF5FB4FF), primaryDeep = Color(0xFF1D4ED8),
        gradStart = Color(0xFF38BDF8), gradEnd = Color(0xFF2563EB),
        secondary = Color(0xFF2BD9C5),
        onPrimary = Color.White
    )
    val Forest = RedoXPalette(
        id = "forest", label = "Orman",
        primary = Color(0xFF22C55E), primaryBright = Color(0xFF4ADE80), primaryDeep = Color(0xFF047857),
        gradStart = Color(0xFF34D399), gradEnd = Color(0xFF059669),
        secondary = Color(0xFFF5C542),
        onPrimary = Color(0xFF04170C)
    )
    val Mono = RedoXPalette(
        id = "mono", label = "Mono",
        primary = Color(0xFFE5E7EB), primaryBright = Color(0xFFF3F4F6), primaryDeep = Color(0xFF6B7280),
        gradStart = Color(0xFFF3F4F6), gradEnd = Color(0xFF9CA3AF),
        secondary = Color(0xFF9CA3AF),
        onPrimary = Color(0xFF0B0E1A)
    )

    val all: List<RedoXPalette> = listOf(Crimson, Ocean, Forest, Mono)

    fun byId(id: String?): RedoXPalette = all.firstOrNull { it.id == id } ?: Crimson
}

/**
 * Çalışma anındaki tema seçimi. Bir Compose "snapshot state" olduğu için, bu değerleri
 * okuyan her bileşen (ör. [RedoXColors.Primary]) seçim değiştiğinde kendiliğinden yenilenir.
 */
object RedoXThemeState {
    var palette by mutableStateOf(RedoXPalettes.Crimson)
        private set
    var amoled by mutableStateOf(false)
        private set
    var textScale by mutableStateOf(1f)
        private set

    private const val PREFS_NAME = "redox_theme_v2"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(context: Context) {
        val sp = prefs(context)
        palette = RedoXPalettes.byId(sp.getString("palette", null))
        amoled = sp.getBoolean("amoled", false)
        textScale = sp.getFloat("text_scale", 1f).coerceIn(0.85f, 1.4f)
    }

    fun updatePalette(context: Context, value: RedoXPalette) {
        palette = value
        prefs(context).edit().putString("palette", value.id).apply()
    }

    fun updateAmoled(context: Context, value: Boolean) {
        amoled = value
        prefs(context).edit().putBoolean("amoled", value).apply()
    }

    fun updateTextScale(context: Context, value: Float) {
        val safe = value.coerceIn(0.85f, 1.4f)
        textScale = safe
        prefs(context).edit().putFloat("text_scale", safe).apply()
    }
}

object RedoXColors {
    // Sabit lacivert-siyah tonları (logodaki zemin: #0A0A14, #161621, #1F1D2A)
    val Ink950 = Color(0xFF05050B)
    val Ink900 = Color(0xFF0A0A14)
    val Ink850 = Color(0xFF0F0F1A)
    val Ink800 = Color(0xFF161621)
    val Ink700 = Color(0xFF1F1D2A)
    val Ink600 = Color(0xFF2A2838)
    val Line = Color(0xFF2E2C3E)

    // AMOLED seçeneğine duyarlı yüzeyler
    val Background: Color get() = if (RedoXThemeState.amoled) Color.Black else Ink900
    val Sidebar: Color get() = if (RedoXThemeState.amoled) Color(0xFF040408) else Ink850
    val Surface: Color get() = if (RedoXThemeState.amoled) Color(0xFF0A0A10) else Ink800
    val SurfaceHigh: Color get() = if (RedoXThemeState.amoled) Color(0xFF13131B) else Ink700
    val Overlay = Color(0xFA0A0A14)
    val Scrim = Color(0xB3000000)

    // Seçili palete bağlı vurgu renkleri
    val Primary: Color get() = RedoXThemeState.palette.primary
    val PrimaryBright: Color get() = RedoXThemeState.palette.primaryBright
    val PrimaryDeep: Color get() = RedoXThemeState.palette.primaryDeep
    val GradStart: Color get() = RedoXThemeState.palette.gradStart
    val GradEnd: Color get() = RedoXThemeState.palette.gradEnd
    val Secondary: Color get() = RedoXThemeState.palette.secondary
    val OnPrimary: Color get() = RedoXThemeState.palette.onPrimary
    val PrimaryBrush: Brush get() = Brush.linearGradient(listOf(GradStart, GradEnd))

    // Altın (logodaki halka, burun halkası ve "TV" yazısı): odak halkası, favori yıldızı, ilerleme çubukları
    val Gold = Color(0xFFF3C05D)
    val GoldLight = Color(0xFFFDDE7A)
    val GoldDeep = Color(0xFFDAA24F)
    val Bronze = Color(0xFFA17844)

    // Logodaki koyu kızıl tonlar: zemin parıltısı ve basılı durumlar
    val Wine = Color(0xFF590823)
    val CrimsonDeep = Color(0xFF96112F)
    /** Hero ve karşılama zeminindeki hafif şarap parıltısı. */
    val WineGlow = Color(0xFF2A0A18)

    /** TV odak halkası (logodaki altın halka). */
    val FocusRing = Gold
    /** Odaktaki öğenin etrafındaki yumuşak altın ışıma. */
    val FocusGlow = Gold.copy(alpha = 0.42f)

    // Yardımcı (kategori / durum) renkleri
    val Aqua = Color(0xFF2BD9C5)
    val Mint = Color(0xFF34D399)
    val Violet = Color(0xFF8B7CFF)
    val Sky = Color(0xFF4DA3FF)
    val Pink = Color(0xFFF472B6)
    val Rose = Color(0xFFFF6B80)
    val Orange = Color(0xFFFF8A3D)
    val Lime = Color(0xFF9BE15D)
    val Teal = Color(0xFF14B8A6)
    val Warn = Color(0xFFFFB020)
    /** Hata/çalışmıyor durumu: birincil kırmızıdan ayırt edilsin diye turuncu-kırmızı. */
    val Danger = Color(0xFFFF7A3D)

    val TextPrimary = Color(0xFFF5F4F2)
    val TextSecondary = Color(0xFFB9BAC8)
    val TextTertiary = Color(0xFF8C8EA3)

    fun categoryColor(category: String): Color {
        val lower = category.lowercase(Locale.getDefault())
        return when {
            lower.contains("spor") -> Mint
            lower.contains("haber") -> Rose
            lower.contains("belgesel") -> Aqua
            lower.contains("sinema") || lower.contains("film") -> Violet
            lower.contains("müzik") || lower.contains("muzik") -> Gold
            lower.contains("çocuk") || lower.contains("cocuk") -> Pink
            lower.contains("ulusal") -> Primary
            lower.contains("dünya") -> Sky
            lower.contains("yerel") -> Teal
            lower.contains("kıbrıs") -> Orange
            lower.contains("dini") -> Lime
            else -> Primary
        }
    }

    fun categoryIcon(category: String): ImageVector {
        val lower = category.lowercase(Locale.getDefault())
        return when {
            lower == "tümü" -> Icons.Rounded.GridView
            lower.contains("ulusal") -> Icons.Rounded.Tv
            lower.contains("haber") -> Icons.Rounded.Newspaper
            lower.contains("spor") -> Icons.Rounded.SportsSoccer
            lower.contains("belgesel") -> Icons.Rounded.Public
            lower.contains("müzik") || lower.contains("muzik") -> Icons.Rounded.MusicNote
            lower.contains("sinema") || lower.contains("film") -> Icons.Rounded.Movie
            lower.contains("çocuk") || lower.contains("cocuk") -> Icons.Rounded.ChildCare
            lower.contains("dünya") -> Icons.Rounded.Language
            lower.contains("yerel") -> Icons.Rounded.Public
            lower.contains("kıbrıs") -> Icons.Rounded.Public
            lower.contains("dini") -> Icons.Rounded.History
            else -> Icons.Rounded.LiveTv
        }
    }
}
