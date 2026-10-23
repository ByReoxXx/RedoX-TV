package com.example.chromecasttv

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector

/** Telefon ve tabletin ortak hedef ekranları. Telefon ilk beşini alt çubukta, tablet hepsini ray üzerinde gösterir. */
enum class MobileTab(val title: String, val icon: ImageVector) {
    HOME("Ana Sayfa", Icons.Rounded.Home),
    LIVE("Canlı TV", Icons.Rounded.LiveTv),
    GUIDE("Rehber", Icons.Rounded.CalendarMonth),
    FAVORITES("Favoriler", Icons.Rounded.Star),
    REMOTE("Kumanda", Icons.Rounded.SettingsRemote),
    SEARCH("Ara", Icons.Rounded.Search),
    MULTI("Bölünmüş Ekran", Icons.Rounded.GridView),
    SOURCES("Kaynaklar", Icons.Rounded.UploadFile),
    SETTINGS("Ayarlar", Icons.Rounded.Settings)
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
