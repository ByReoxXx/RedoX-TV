package com.example.chromecasttv

import android.content.Context
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight

/**
 * Güncelleme sonrası ilk açılışta bir kez gösterilen "Yenilikler" penceresi. İlk kurulumda gösterilmez;
 * yalnızca daha önce başka bir sürüm açılmışsa ve bu sürümün notu varsa çıkar. Not metni her sürümde güncellenir.
 */
object WhatsNew {
    private const val PREFS = "redox_whats_new"
    private const val KEY_SEEN = "seen_version"

    /** Bu sürümün kısa notu; boş bırakılırsa pencere gösterilmez. */
    private const val NOTES = "• Klavye ve fare desteği: oynatıcıda Boşluk, ok tuşları, M, N/P; fare tekerleği ses. Ctrl+1…0 sekmeler.\n" +
        "• Rehberde ızgara görünümü (kanal × zaman).\n" +
        "• Radyo alarmı ve \"TV'yi sessiz izle, radyo çalsın\" seçeneği (Radyo → Seçenekler).\n" +
        "• TV'de kanal listesi daha geniş."

    /** Gösterilecek not varsa döndürür ve bu sürümü "görüldü" olarak işaretlemez (kullanıcı kapatınca işaretlenir). */
    fun pending(context: Context): String? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val seen = prefs.getString(KEY_SEEN, null)
        val current = BuildConfig.VERSION_NAME
        if (seen == null) {
            // İlk kurulum: sessizce işaretle
            prefs.edit().putString(KEY_SEEN, current).apply()
            return null
        }
        return if (seen != current && NOTES.isNotBlank()) NOTES else null
    }

    fun markSeen(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_SEEN, BuildConfig.VERSION_NAME).apply()
    }
}

@Composable
fun WhatsNewDialog() {
    val context = LocalContext.current
    var notes by remember { mutableStateOf(WhatsNew.pending(context)) }
    val text = notes ?: return
    AlertDialog(
        onDismissRequest = {
            WhatsNew.markSeen(context)
            notes = null
        },
        containerColor = RedoXColors.SurfaceHigh,
        titleContentColor = RedoXColors.TextPrimary,
        textContentColor = RedoXColors.TextSecondary,
        title = { Text("Yenilikler • v${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.Bold) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = {
                WhatsNew.markSeen(context)
                notes = null
            }) { Text("Tamam", color = RedoXColors.Primary, fontWeight = FontWeight.Bold) }
        }
    )
}
