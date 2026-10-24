package com.example.chromecasttv

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Kaynaklar: M3U adresi, Xtream hesabı, dosyadan içe aktarma, eklenen listeler ve dosyaya yedekleme (SAF). */
@Composable
fun MobileSourcesScreen(
    viewModel: TvViewModel,
    state: TvUiState,
    host: RedoXHost
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var m3uName by remember { mutableStateOf("") }
    var m3uUrl by remember { mutableStateOf("") }
    var m3uEpg by remember { mutableStateOf("") }
    var xtName by remember { mutableStateOf("") }
    var xtServer by remember { mutableStateOf("") }
    var xtUser by remember { mutableStateOf("") }
    var xtPass by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<M3uPlaylistSource?>(null) }
    var confirmRestore by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) viewModel.importLocalM3uUri(uri)
    }
    val saveBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try {
                        val text = viewModel.exportBackupText()
                        context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                        true
                    } catch (_: Exception) {
                        false
                    }
                }
                viewModel.showStatusMessage(if (ok) "Yedek dosyaya kaydedildi." else "Yedek kaydedilemedi.")
            }
        }
    }
    val openBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val text = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    } catch (_: Exception) {
                        null
                    }
                }
                if (text.isNullOrBlank()) {
                    viewModel.showStatusMessage("Yedek dosyası okunamadı.")
                } else {
                    confirmRestore = text
                }
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "m3u") {
            MCard(modifier = Modifier.fillMaxWidth()) {
                MCardTitle("M3U / M3U8 adresi ekle")
                MTextField(m3uName, { m3uName = it }, "Liste adı (isteğe bağlı)", placeholder = "Örn: Ev IPTV")
                Spacer(modifier = Modifier.height(8.dp))
                MTextField(m3uUrl, { m3uUrl = it }, "M3U / M3U8 adresi", placeholder = "https://ornek.com/liste.m3u8", keyboardType = KeyboardType.Uri)
                Spacer(modifier = Modifier.height(8.dp))
                MTextField(m3uEpg, { m3uEpg = it }, "XMLTV rehber adresi (isteğe bağlı)", placeholder = "https://ornek.com/epg.xml.gz", keyboardType = KeyboardType.Uri)
                Spacer(modifier = Modifier.height(12.dp))
                MPrimaryButton(
                    text = "Ekle ve kanalları yükle",
                    icon = RedoXIcons.CloudDownload,
                    enabled = m3uUrl.isNotBlank(),
                    onClick = {
                        viewModel.addRemoteM3uPlaylist(m3uName, m3uUrl, m3uEpg)
                        m3uName = ""; m3uUrl = ""; m3uEpg = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item(key = "xtream") {
            MCard(modifier = Modifier.fillMaxWidth()) {
                MCardTitle("Xtream Codes hesabı")
                MTextField(xtName, { xtName = it }, "Paket adı (isteğe bağlı)")
                Spacer(modifier = Modifier.height(8.dp))
                MTextField(xtServer, { xtServer = it }, "Sunucu adresi", placeholder = "http://ornek.com:8080", keyboardType = KeyboardType.Uri)
                Spacer(modifier = Modifier.height(8.dp))
                MTextField(xtUser, { xtUser = it }, "Kullanıcı adı")
                Spacer(modifier = Modifier.height(8.dp))
                MTextField(xtPass, { xtPass = it }, "Şifre", password = true, keyboardType = KeyboardType.Password)
                Spacer(modifier = Modifier.height(12.dp))
                MPrimaryButton(
                    text = "Hesabı bağla",
                    icon = RedoXIcons.Verified,
                    enabled = xtServer.isNotBlank() && xtUser.isNotBlank(),
                    onClick = {
                        viewModel.addXtreamCodesAccount(xtName, xtServer, xtUser, xtPass)
                        xtName = ""; xtServer = ""; xtUser = ""; xtPass = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item(key = "file") {
            MCard(modifier = Modifier.fillMaxWidth()) {
                MCardTitle("Dosyadan içe aktar")
                Text(
                    "Cihazdaki bir .m3u / .m3u8 dosyasını seçin (Dosyalar uygulaması açılır, ek izin gerekmez).",
                    color = RedoXColors.TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                MGhostButton(
                    text = "Dosya seç",
                    icon = RedoXIcons.FolderOpen,
                    onClick = { importLauncher.launch(arrayOf("*/*")) }
                )
            }
        }

        item(key = "playlists") {
            MCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Eklenen listeler (${state.playlists.size})",
                        color = RedoXColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    MGhostButton(
                        text = if (state.isLoadingM3u) "Yükleniyor…" else "Yenile",
                        icon = RedoXIcons.Refresh,
                        onClick = { viewModel.reloadAllChannelsAndEpg() }
                    )
                }
                if (state.playlists.isEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Henüz özel liste eklenmedi.", color = RedoXColors.TextSecondary, fontSize = 12.sp)
                }
                state.playlists.forEach { playlist ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                color = RedoXColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (playlist.isLocalFile) "Yerel dosya" else playlist.locationOrUrl,
                                color = RedoXColors.TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        MGhostButton(
                            text = "Sil",
                            icon = RedoXIcons.DeleteOutline,
                            onClick = { deleteTarget = playlist },
                            danger = true
                        )
                    }
                }
                if (state.hiddenChannelIds.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    MGhostButton(
                        text = "Gizlenen ${state.hiddenChannelIds.size} kanalı geri getir",
                        icon = RedoXIcons.VisibilityOff,
                        onClick = { viewModel.restoreAllHiddenChannels() }
                    )
                }
            }
        }

        item(key = "backup") {
            MCard(modifier = Modifier.fillMaxWidth()) {
                MCardTitle("Yedekle / geri yükle")
                Text(
                    "Favoriler, favori listeleri, M3U listeleri, kanal adları ve rehber adresleri tek bir metin dosyasına yazılır. " +
                        "Dosyayı istediğiniz yere (Drive, Dosyalar…) kaydedebilirsiniz.",
                    color = RedoXColors.TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MGhostButton(
                        text = "Yedeği kaydet",
                        icon = RedoXIcons.Save,
                        onClick = { saveBackupLauncher.launch("redox_tv2_yedek.txt") }
                    )
                    MGhostButton(
                        text = "Yedekten yükle",
                        icon = RedoXIcons.Refresh,
                        onClick = { openBackupLauncher.launch(arrayOf("*/*")) }
                    )
                }
            }
        }
    }

    deleteTarget?.let { playlist ->
        MConfirmDialog(
            title = "Listeyi sil",
            message = "\"${playlist.name}\" listesi ve kanalları kaldırılacak.",
            confirmText = "Sil",
            danger = true,
            onConfirm = {
                viewModel.removePlaylist(playlist.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }

    confirmRestore?.let { text ->
        MConfirmDialog(
            title = "Yedekten geri yükle",
            message = "Mevcut favoriler, listeler ve ayarlar yedekteki verilerle değiştirilecek. Devam edilsin mi?",
            confirmText = "Geri yükle",
            danger = true,
            onConfirm = {
                viewModel.importBackupText(text)
                confirmRestore = null
            },
            onDismiss = { confirmRestore = null }
        )
    }
}
