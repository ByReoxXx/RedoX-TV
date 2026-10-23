package com.example.chromecasttv

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================================
// TV Ayarlar ekranı: görünüm, oynatma, yayın rehberi, telefon kumandası/güvenlik, bakım, hakkında.
// Tüm satırlar kumanda (D-Pad) ile odaklanır; OK ile değiştirilir.
// ============================================================================

@Composable
fun TvSettingsScreen(
    viewModel: TvViewModel,
    state: TvUiState,
    features: FeatureState,
    host: RedoXHost
) {
    val context = LocalContext.current
    val hub = viewModel.features
    val prefs = features.prefs
    val epg = features.epg
    val remote = features.remoteHost
    val currentPalette = RedoXThemeState.palette
    var epgUrlInput by remember { mutableStateOf("") }
    var showLog by remember { mutableStateOf(false) }
    var logText by remember { mutableStateOf("") }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(end = 4.dp, bottom = 12.dp)
    ) {
        // ------------------------------------------------------------ Görünüm
        TvSettingsSection(title = "Görünüm", icon = Icons.Rounded.Palette) {
            Text(
                text = "Renk paleti (logo her zaman kızıl-altın kalır; palet arayüz vurgularını değiştirir)",
                color = TvThemeColors.TextSecondary,
                fontSize = 11.sp
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RedoXPalettes.all, key = { "pal_${it.id}" }) { palette ->
                    TvCompactChipButton(
                        text = if (palette.id == currentPalette.id) "✓ ${palette.label}" else palette.label,
                        icon = Icons.Rounded.Palette,
                        onClick = { RedoXThemeState.updatePalette(context, palette) }
                    )
                }
            }
            TvSettingToggleRow(
                title = "AMOLED siyah",
                subtitle = "Arka planı saf siyah yapar (OLED ekranlarda pil ve kontrast kazancı).",
                checked = RedoXThemeState.amoled,
                onToggle = { RedoXThemeState.updateAmoled(context, !RedoXThemeState.amoled) }
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Yazı boyutu: %${(RedoXThemeState.textScale * 100f).toInt()}",
                    color = TvThemeColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                TvCompactChipButton(
                    text = "Küçült",
                    icon = Icons.Rounded.Remove,
                    onClick = { RedoXThemeState.updateTextScale(context, RedoXThemeState.textScale - 0.05f) }
                )
                TvCompactChipButton(
                    text = "Büyüt",
                    icon = Icons.Rounded.Add,
                    onClick = { RedoXThemeState.updateTextScale(context, RedoXThemeState.textScale + 0.05f) }
                )
                TvCompactChipButton(
                    text = "Sıfırla",
                    icon = Icons.Rounded.Refresh,
                    onClick = { RedoXThemeState.updateTextScale(context, 1f) }
                )
            }
        }

        // ------------------------------------------------------------ Oynatma
        TvSettingsSection(title = "Oynatma", icon = Icons.Rounded.PlayCircle) {
            Text(
                text = "Varsayılan görüntü kalitesi (oynatıcıda da değiştirilebilir)",
                color = TvThemeColors.TextSecondary,
                fontSize = 11.sp
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PlayerQuality.entries.toList(), key = { "q_${it.name}" }) { quality ->
                    TvCompactChipButton(
                        text = if (quality == prefs.defaultQuality) "✓ ${quality.label}" else quality.label,
                        icon = Icons.Rounded.PlayCircle,
                        onClick = { hub.updatePrefs { it.copy(defaultQuality = quality) } }
                    )
                }
            }
            TvSettingToggleRow(
                title = "Gece modu",
                subtitle = "Konuşmaları belirginleştirir, ani yüksek sesleri baskılar (desteklemeyen cihazlarda etkisizdir).",
                checked = prefs.nightMode,
                onToggle = { hub.updatePrefs { it.copy(nightMode = !it.nightMode) } }
            )
        }

        // ------------------------------------------------------------ Yayın rehberi
        TvSettingsSection(title = "Yayın Rehberi (EPG)", icon = Icons.Rounded.CalendarMonth) {
            Text(
                text = when {
                    epg.isLoading -> "Rehber güncelleniyor…"
                    epg.hasRealData -> "${epg.matchedChannels} kanal eşleşti • ${epg.programmeCount} program" +
                        if (epg.lastUpdatedMillis > 0L) " • Son güncelleme: ${formatDateTime(epg.lastUpdatedMillis)}" else ""
                    epg.message.isNotBlank() -> epg.message
                    else -> "Henüz gerçek rehber verisi yok. Aşağıya bir XMLTV adresi ekleyin."
                },
                color = if (epg.hasRealData) TvThemeColors.AccentEmerald else TvThemeColors.FavoriteGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Uydurma program adı gösterilmez; rehber verisi olmayan kanallarda yalnızca \"Canlı yayın\" yazar. " +
                    "M3U listelerinizdeki XMLTV adresleri de otomatik kullanılır (${state.playlists.count { it.epgXmlUrl.isNotBlank() }} adres).",
                color = TvThemeColors.TextSecondary,
                fontSize = 10.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TvCompactChipButton(
                    text = if (epg.isLoading) "Güncelleniyor…" else "Şimdi Güncelle",
                    icon = Icons.Rounded.Refresh,
                    onClick = { hub.refreshEpg(true) }
                )
            }
            prefs.epgUrls.forEach { url ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = url,
                        color = TvThemeColors.TextPrimary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    TvCompactChipButton(
                        text = "Sil",
                        icon = Icons.Rounded.DeleteOutline,
                        onClick = { hub.updatePrefs { p -> p.copy(epgUrls = p.epgUrls.filterNot { it == url }) } }
                    )
                }
            }
            TvInputField(
                label = "Yeni XMLTV adresi (.xml veya .xml.gz)",
                value = epgUrlInput,
                placeholder = "https://ornek.com/epg.xml.gz",
                onValueChange = { epgUrlInput = it },
                showUrlQuickChips = true
            )
            TvActionButton(
                text = "Adresi Ekle ve Rehberi Yükle",
                icon = Icons.Rounded.Add,
                isPrimary = true,
                onClick = {
                    val url = epgUrlInput.trim()
                    if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
                        hub.updatePrefs { p -> if (p.epgUrls.contains(url)) p else p.copy(epgUrls = p.epgUrls + url) }
                        epgUrlInput = ""
                        hub.refreshEpg(true)
                    } else {
                        viewModel.showStatusMessage("Adres http:// veya https:// ile başlamalıdır.")
                    }
                }
            )

            Text(
                text = "Hatırlatıcılar (${features.reminders.size})",
                color = TvThemeColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            if (features.reminders.isEmpty()) {
                Text(
                    text = "Yayın Rehberi sekmesinde bir programa OK ile basarak hatırlatma ekleyebilirsiniz.",
                    color = TvThemeColors.TextSecondary,
                    fontSize = 10.sp
                )
            }
            features.reminders.sortedBy { it.startMillis }.take(8).forEach { reminder ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${formatDateTime(reminder.startMillis)} • ${reminder.channelName} • ${reminder.title}",
                        color = TvThemeColors.TextPrimary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    TvCompactChipButton(
                        text = "Sil",
                        icon = Icons.Rounded.DeleteOutline,
                        onClick = { hub.removeReminder(reminder) }
                    )
                }
            }
        }

        // ------------------------------------------------------------ Telefon kumandası ve güvenlik
        TvSettingsSection(title = "Telefon Kumandası & Güvenlik", icon = Icons.Rounded.SettingsRemote) {
            Text(
                text = if (remote.isRunning) "Adres: http://${remote.ip}:${remote.port}" else "Kumanda sunucusu başlatılıyor…",
                color = TvThemeColors.AccentCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            RemotePinLine(remoteHost = remote, large = true)
            Text(
                text = "Telefon uygulamasında \"Kumanda\" sekmesinden bu TV'yi seçip kodu girin. Kod yalnızca bu ekranda görünür; " +
                    "yanlış kodla 5 denemeden sonra 60 sn beklenir. Eşleşen telefonlar kodu bir daha sormaz.",
                color = TvThemeColors.TextSecondary,
                fontSize = 10.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TvCompactChipButton(
                    text = "Kodu Yenile",
                    icon = Icons.Rounded.Refresh,
                    onClick = { viewModel.regenerateRemotePin() }
                )
                TvCompactChipButton(
                    text = "Tüm Eşleşmeleri Kaldır (${remote.pairedCount})",
                    icon = Icons.Rounded.DeleteOutline,
                    onClick = { viewModel.revokeRemotePairings() }
                )
            }
        }

        // ------------------------------------------------------------ Bakım
        TvSettingsSection(title = "Bakım", icon = Icons.Rounded.Build) {
            val update = features.update
            Text(
                text = when {
                    update == null -> "Sürüm: v${BuildConfig.VERSION_NAME} • Henüz denetlenmedi"
                    update.isNewer -> "Yeni sürüm var: v${update.latestVersion} (kurulu: v${update.currentVersion})"
                    else -> "Uygulama güncel: v${update.currentVersion}"
                },
                color = if (update?.isNewer == true) TvThemeColors.FavoriteGold else TvThemeColors.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TvCompactChipButton(
                    text = "Güncellemeyi Denetle",
                    icon = Icons.Rounded.Refresh,
                    onClick = { hub.checkForUpdates(true) }
                )
                if (update?.isNewer == true) {
                    TvCompactChipButton(
                        text = "Sürüm Sayfasını Aç",
                        icon = Icons.Rounded.Info,
                        onClick = { host.openUrl(update.htmlUrl) }
                    )
                }
            }
            TvSettingToggleRow(
                title = "Açılışta güncelleme denetle",
                subtitle = "Yalnızca GitHub'daki sürüm numarası okunur; hiçbir kişisel veri gönderilmez.",
                checked = prefs.updateCheckOnStart,
                onToggle = { hub.updatePrefs { it.copy(updateCheckOnStart = !it.updateCheckOnStart) } }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TvCompactChipButton(
                    text = "Yedek Al (Download)",
                    icon = Icons.Rounded.Build,
                    onClick = { viewModel.backupSettingsToDevice() }
                )
                TvCompactChipButton(
                    text = "Yedekten Geri Yükle",
                    icon = Icons.Rounded.Refresh,
                    onClick = { viewModel.restoreSettingsFromDevice() }
                )
                TvCompactChipButton(
                    text = "İzleme İstatistiklerini Temizle",
                    icon = Icons.Rounded.DeleteOutline,
                    onClick = {
                        hub.clearWatchStats()
                        viewModel.showStatusMessage("İzleme istatistikleri temizlendi.")
                    }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TvCompactChipButton(
                    text = if (showLog) "Günlüğü Gizle" else "Tanılama Günlüğünü Göster",
                    icon = Icons.Rounded.Info,
                    onClick = {
                        showLog = !showLog
                        if (showLog) logText = CrashLogger.readLog(context)
                    }
                )
                if (showLog) {
                    TvCompactChipButton(
                        text = "Paylaş",
                        icon = Icons.Rounded.Add,
                        onClick = { host.shareText(logText.ifBlank { "Günlük boş." }) }
                    )
                    TvCompactChipButton(
                        text = "Günlüğü Temizle",
                        icon = Icons.Rounded.DeleteOutline,
                        onClick = {
                            CrashLogger.clear(context)
                            logText = ""
                        }
                    )
                }
            }
            if (showLog) {
                Text(
                    text = logText.takeLast(1500).ifBlank { "Günlük boş — kaydedilmiş bir hata yok." },
                    color = TvThemeColors.TextSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(8.dp)
                )
            }
        }

        // ------------------------------------------------------------ Hakkında
        TvSettingsSection(title = "Hakkında", icon = Icons.Rounded.Info) {
            Text(
                text = "${BuildConfig.APP_DISPLAY_NAME} • v${BuildConfig.VERSION_NAME}",
                color = TvThemeColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "RedoX = Red + Ox: \"kırmızı boğa\". Bu uygulama eski RedoX TV'den bağımsız, ayrı bir uygulamadır; " +
                    "ayarlarınız ve verileriniz paylaşılmaz.",
                color = TvThemeColors.TextSecondary,
                fontSize = 11.sp
            )
            Text(
                text = "Cihaz sınıfı: ${host.deviceKind.uppercase(Locale.ROOT)} • ${Build.MANUFACTURER} ${Build.MODEL} • Android ${Build.VERSION.RELEASE}",
                color = TvThemeColors.TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

private fun formatDateTime(millis: Long): String =
    SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(millis))

@Composable
private fun TvSettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TvThemeColors.SurfaceCard)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TvThemeColors.BullRedBright,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = TvThemeColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        content()
    }
}

@Composable
private fun TvSettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) TvThemeColors.SurfaceElevated else Color.Black.copy(alpha = 0.22f))
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) TvThemeColors.BullRedBright else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp)
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable { onToggle() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TvThemeColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = subtitle,
                color = TvThemeColors.TextSecondary,
                fontSize = 10.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (checked) TvThemeColors.AccentEmerald.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.08f))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (checked) "AÇIK" else "KAPALI",
                color = if (checked) TvThemeColors.AccentEmerald else TvThemeColors.TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}

/** Kanal adı / numarasını kullanıcı isteğine göre değiştiren pencere (özgün veri korunur, "Varsayılana Dön" ile geri alınır). */
@Composable
fun TvChannelEditDialog(
    channel: TvChannel,
    hasOverride: Boolean,
    onSave: (name: String?, number: Int?) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var nameInput by remember(channel.id) { mutableStateOf(channel.name) }
    var numberInput by remember(channel.id) { mutableStateOf(channel.number.toString()) }
    val nameRequester = remember { FocusRequester() }
    val numberRequester = remember { FocusRequester() }
    val saveRequester = remember { FocusRequester() }

    LaunchedEffect(channel.id) {
        delay(120L)
        try { nameRequester.requestFocus() } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .width(470.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(TvThemeColors.SurfaceElevated)
                    .border(2.dp, TvThemeColors.BullRedBright, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Text(
                    text = "Kanalı düzenle",
                    color = TvThemeColors.TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Ad ve numara yalnızca bu cihazda değişir; M3U listeniz bozulmaz.",
                    color = TvThemeColors.TextSecondary,
                    fontSize = 11.sp
                )
                TvInputField(
                    label = "Kanal adı",
                    value = nameInput,
                    placeholder = channel.name,
                    onValueChange = { nameInput = it.take(48) },
                    focusRequester = nameRequester,
                    downFocusRequester = numberRequester
                )
                TvInputField(
                    label = "Kanal numarası",
                    value = numberInput,
                    placeholder = "Örn: 7",
                    onValueChange = { numberInput = it.filter { c -> c.isDigit() }.take(4) },
                    focusRequester = numberRequester,
                    upFocusRequester = nameRequester,
                    downFocusRequester = saveRequester,
                    numericOnly = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TvActionButton(
                        text = "Kaydet",
                        icon = Icons.Rounded.CheckCircle,
                        isPrimary = true,
                        onClick = {
                            onSave(nameInput.trim().ifEmpty { null }, numberInput.toIntOrNull())
                        },
                        modifier = Modifier.focusRequester(saveRequester)
                    )
                    if (hasOverride) {
                        TvActionButton(
                            text = "Varsayılana Dön",
                            icon = Icons.Rounded.Refresh,
                            isPrimary = false,
                            onClick = onReset
                        )
                    }
                    TvActionButton(
                        text = "İptal",
                        icon = Icons.Rounded.Clear,
                        isPrimary = false,
                        onClick = onDismiss
                    )
                }
            }
        }
    }
}
