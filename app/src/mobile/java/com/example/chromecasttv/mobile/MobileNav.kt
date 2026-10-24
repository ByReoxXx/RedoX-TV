package com.example.chromecasttv

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector

/** Telefon ve tabletin ortak hedef ekranları. Telefon ilk beşini alt çubukta, tablet hepsini ray üzerinde gösterir. */
enum class MobileTab(val title: String, val icon: ImageVector) {
    HOME("Ana Sayfa", RedoXIcons.Home),
    LIVE("Canlı TV", RedoXIcons.LiveTv),
    GUIDE("Rehber", RedoXIcons.CalendarMonth),
    FAVORITES("Favoriler", RedoXIcons.Star),
    REMOTE("Kumanda", RedoXIcons.SettingsRemote),
    SEARCH("Ara", RedoXIcons.Search),
    MULTI("Bölünmüş Ekran", RedoXIcons.GridView),
    SOURCES("Kaynaklar", RedoXIcons.UploadFile),
    SETTINGS("Ayarlar", RedoXIcons.Settings)
}

/** Telefon alt çubuğundaki kök sekmeler. */
val PhoneRootTabs: List<MobileTab> = listOf(
    MobileTab.HOME,
    MobileTab.LIVE,
    MobileTab.GUIDE,
    MobileTab.FAVORITES,
    MobileTab.REMOTE
)

/** Tablet gezinti rayındaki sekmeler (hepsi köktür). */
val TabletRailTabs: List<MobileTab> = MobileTab.entries.toList()

/** Rayın dibine sabitlenen yardımcı sekmeler (ana gezintiden ayrı): Kaynaklar ve Ayarlar. */
val TabletRailFooterTabs: List<MobileTab> = listOf(MobileTab.SOURCES, MobileTab.SETTINGS)

/** Rayın üst bölümündeki ana gezinti sekmeleri. */
val TabletRailMainTabs: List<MobileTab> = TabletRailTabs.filter { it !in TabletRailFooterTabs }

/**
 * Basit gezinme durumu: bir "geri yığını" tutar. Kök sekme seçimi yığını sıfırlar;
 * ikincil ekranlar (Ara, Ayarlar, Kaynaklar…) [push] ile açılır ve Geri ile kapanır.
 */
class MobileNav(start: MobileTab = MobileTab.HOME) {
    var current by mutableStateOf(start)
        private set

    private val stack = mutableStateListOf<MobileTab>()

    val canGoBack: Boolean get() = stack.isNotEmpty()

    fun selectRoot(tab: MobileTab) {
        stack.clear()
        current = tab
    }

    fun push(tab: MobileTab) {
        if (tab == current) return
        stack.add(current)
        current = tab
    }

    /** @return bir önceki ekrana dönüldüyse true */
    fun back(): Boolean {
        if (stack.isEmpty()) return false
        current = stack.removeAt(stack.lastIndex)
        return true
    }
}
