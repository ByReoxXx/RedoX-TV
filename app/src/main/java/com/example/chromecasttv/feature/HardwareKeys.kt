package com.example.chromecasttv

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * Fiziksel klavye kısayolları (telefon/tablet/ChromeOS). Etkin ekranlar işleyici kaydeder; en son kaydolan
 * (en üstteki ekran, örn. oynatıcı) önce denenir. İşleyici olayı tükettiyse true döndürür.
 */
object HardwareKeys {
    private val handlers = ArrayList<(KeyEvent) -> Boolean>()

    fun register(handler: (KeyEvent) -> Boolean): () -> Unit {
        handlers.add(handler)
        return { handlers.remove(handler) }
    }

    fun dispatch(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        for (i in handlers.indices.reversed()) {
            if (handlers[i](event)) return true
        }
        return false
    }
}

/** Bu bileşim açıkken klavye kısayollarını dinler; her çağrıda güncel [handler] kullanılır. */
@Composable
fun HardwareKeyHandler(handler: (KeyEvent) -> Boolean) {
    val current by rememberUpdatedState(handler)
    DisposableEffect(Unit) {
        val unregister = HardwareKeys.register { current(it) }
        onDispose { unregister() }
    }
}
