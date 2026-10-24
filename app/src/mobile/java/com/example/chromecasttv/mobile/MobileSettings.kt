package com.example.chromecasttv

import android.os.Build
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/** Ayarlar: görünüm, oynatma, yayın rehberi, bakım/güvenlik ve hakkında. */
@Composable
fun MobileSettingsScreen(
    viewModel: TvViewModel,
    state: TvUiState,
    features: FeatureState,
    host: RedoXHost
) {
    val context = LocalContext.current
    val hub = viewModel.features
    val prefs = features.prefs
    val epg = features.epg
    val palette = RedoXThemeState.palette
    var epgUrlInput by remember { mutableStateOf("") }
    var showLog by remember { mutableStateOf(false) }
    var logText by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // ------------------------------------------------------------ Görünüm
        item(key = "appearance") {
            MCard(modifier = Modifier.fillMaxWidth()) {
                MCardTitle("Görünüm")
                Text(
                    "Renk paleti (logo her zaman kızıl-altın kalır; palet arayüz vurgularını değiştirir)",
                    color = RedoXColors.TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(RedoXPalettes.all, key = { "pal_${it.id}" }) { p ->
                        MChip(
                            text = p.label,
                            selected = p.id == palette.id,
                            onClick = { RedoXThemeState.updatePalette(context, p) },
                            icon = RedoXIcons.Palette
                        )
                    }
                }
                MSettingSwitch(
                    title = "AMOLED siyah",
                    subtitle = "Arka planı saf siyah yapar (OLED ekranlarda pil tasarrufu).",
                    checked = RedoXThemeState.amoled,
                    onCheckedChange = { RedoXThemeState.updateAmoled(context, it) }
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Yazı boyutu: %${(RedoXThemeState.textScale * 100f).toInt()}",
                        color = RedoXColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    MGhostButton("Küçült", { RedoXThemeState.updateTextScale(context, RedoXThemeState.textScale - 0.05f) }, icon = RedoXIcons.Remove)
                    MGhostButton("Büyüt", { RedoXThemeState.updateTextScale(context, RedoXThemeState.textScale + 0.05f) }, icon = RedoXIcons.Add)
                }
            }
        }

        // ------------------------------------------------------------ Oynatma
        item(key = "playback") {
            MCard(modifier = Modifier.fillMaxWidth()) {
                MCardTitle("Oynatma")
                MSettingSwitch(
                    title = "Arka planda ses",
                    subtitle = "Uygulamadan çıkınca veya ekran kapanınca ses devam eder; bildirimden durdurabilirsiniz.",
                    checked = prefs.backgroundAudio,
                    onCheckedChange = { v -> hub.updatePrefs { it.copy(backgroundAudio = v) } }
                )
                MSettingSwitch(
                    title = "Otomatik küçük pencere (PiP)",
                    subtitle = "Oynatırken ana ekrana çıkınca görüntü küçük bir pencerede sürer.",
                    checked = prefs.autoPip,
                    onCheckedChange = { v -> hub.updatePrefs { it.copy(autoPip = v) } }
                )
                MSettingSwitch(
                    title = "Dokunma hareketleri",
                    subtitle = "Sol kenar: parlaklık • Sağ kenar: ses • Yatay kaydır: kanal değiştir • Çift dokun: duraklat.",
                    checked = prefs.gesturesEnabled,
                    onCheckedChange = { v -> hub.updatePrefs { it.copy(gesturesEnabled = v) } }
                )
                MSettingSwitch(
                    title = "Mobil veri tasarrufu",
                    subtitle = "Yalnızca mobil veride çalışırken görüntü kalitesini otomatik düşürür.",
                    checked = prefs.mobileDataSaver,
                    onCheckedChange = { v -> hub.updatePrefs { it.copy(mobileDataSaver = v) } }
                )
                MSettingSwitch(
                    title = "Gece modu",
                    subtitle = "Konuşmaları belirginleştirir, ani yüksek sesleri baskılar.",
                    checked = prefs.nightMode,
                    onCheckedChange = { v -> hub.updatePrefs { it.copy(nightMode = v) } }
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text("Varsayılan görüntü kalitesi", color = RedoXColors.TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PlayerQuality.entries.toList(), key = { "q_${it.name}" }) { q ->
                        MChip(
                            text = q.label,
                            selected = q == prefs.defaultQuality,
                            onClick = { hub.updatePrefs { it.copy(defaultQuality = q) } }
                        )
                    }
                }
            }
        }

        // ------------------------------------------------------------ Yayın rehberi
        item(key = "epg") {
            MCard(modifier = Modifier.fillMaxWidth()) {
                MCardTitle("Yayın rehberi (EPG)")
                Text(
                    text = when {
                        epg.isLoading -> "Rehber güncelleniyor…"
                        epg.hasRealData -> "${epg.matchedChannels} kanal eşleşti • ${epg.programmeCount} program" +
                            if (epg.lastUpdatedMillis > 0L) " • ${mFormatDateTime(epg.lastUpdatedMillis)}" else ""
                        epg.message.isNotBlank() -> epg.message
                        else -> "Henüz gerçek rehber verisi yok. Bir XMLTV adresi ekleyin."
                    },
                    color = if (epg.hasRealData) RedoXColors.Mint else RedoXColors.Gold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Uydurma program adı gösterilmez; rehber verisi olmayan kanallarda yalnızca \"Canlı yayın\" yazar. " +
                        "M3U listelerinizdeki XMLTV adresleri de otomatik kullanılır.",
                    color = RedoXColors.TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
                MGhostButton(
                    text = if (epg.isLoading) "Güncelleniyor…" else "Şimdi güncelle",
                    icon = RedoXIcons.Refresh,
                    onClick = { hub.refreshEpg(true) }
                )
                prefs.epgUrls.forEach { url ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = url,
                            color = RedoXColors.TextPrimary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        MGhostButton(
                            text = "Sil",
                            icon = RedoXIcons.DeleteOutline,
                            danger = true,
                            onClick = { hub.updatePrefs { p -> p.copy(epgUrls = p.epgUrls.filterNot { it == url }) } }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                MTextField(
                    value = epgUrlInput,
                    onValueChange = { epgUrlInput = it },
                    label = "Yeni XMLTV adresi (.xml / .xml.gz)",
                    placeholder = "https://ornek.com/epg.xml.gz",
                    keyboardType = KeyboardType.Uri
                )
                Spacer(modifier = Modifier.height(8.dp))
                MPrimaryButton(
                    text = "Ekle ve rehberi yükle",
                    icon = RedoXIcons.Add,
                    enabled = epgUrlInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
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

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Hatırlatıcılar (${features.reminders.size})",
                    color = RedoXColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                if (features.reminders.isEmpty()) {
                    Text(
                        "Rehber ekranında ileri bir programa dokunarak hatırlatma ekleyebilirsiniz.",
                        color = RedoXColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }
                features.reminders.sortedBy { it.startMillis }.take(10).forEach { reminder ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${mFormatDateTime(reminder.startMillis)} • ${reminder.channelName} • ${reminder.title}",
                            color = RedoXColors.TextPrimary,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        MGhostButton(
                            text = "Sil",
                            icon = RedoXIcons.DeleteOutline,
                            danger = true,
                            onClick = { hub.removeReminder(reminder) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                MGhostButton(
                    text = "Bildirim izni iste",
                    icon = RedoXIcons.NotificationsActive,
                    onClick = { host.requestNotificationPermission() }
                )
            }
        }

        // ------------------------------------------------------------ Bakım ve güvenlik
        item(key = "maintenance") {
            MCard(modifier = Modifier.fillMaxWidth()) {
                MCardTitle("Bakım ve güvenlik")
                Text(
                    "Bağlantılar varsayılan olarak sertifika doğrulamalıdır. Yalnızca yayın akışları, bölgesel yayıncıların " +
                        "kendinden imzalı sertifikalarını kabul eder. Hiçbir kişisel veri dışarı gönderilmez.",
                    color = RedoXColors.TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                val update = features.update
                Text(
                    text = when {
                        update == null -> "Sürüm: v${BuildConfig.VERSION_NAME} • Henüz denetlenmedi"
                        update.isNewer -> "Yeni sürüm var: v${update.latestVersion} (kurulu: v${update.currentVersion})"
                        else -> "Uygulama güncel: v${update.currentVersion}"
                    },
                    color = if (update?.isNewer == true) RedoXColors.Gold else RedoXColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MGhostButton(
                        text = "Güncellemeyi denetle",
                        icon = RedoXIcons.SystemUpdate,
                        onClick = { hub.checkForUpdates(true) }
                    )
                    if (update?.isNewer == true) {
                        MGhostButton(text = "Sürümü aç", onClick = { host.openUrl(update.htmlUrl) })
                    }
                }
                MSettingSwitch(
                    title = "Açılışta güncelleme denetle",
                    subtitle = "Yalnızca GitHub'daki sürüm numarası okunur.",
                    checked = prefs.updateCheckOnStart,
                    onCheckedChange = { v -> hub.updatePrefs { it.copy(updateCheckOnStart = v) } }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MGhostButton(
                        text = "İzleme istatistiklerini temizle",
                        icon = RedoXIcons.DeleteOutline,
                        danger = true,
                        onClick = {
                            hub.clearWatchStats()
                            viewModel.showStatusMessage("İzleme istatistikleri temizlendi.")
                        }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MGhostButton(
                        text = if (showLog) "Günlüğü gizle" else "Tanılama günlüğü",
                        icon = RedoXIcons.Info,
                        onClick = {
                            showLog = !showLog
                            if (showLog) logText = CrashLogger.readLog(context)
                        }
                    )
                    if (showLog) {
                        MGhostButton(text = "Paylaş", onClick = { host.shareText(logText.ifBlank { "Günlük boş." }) })
                        MGhostButton(
                            text = "Temizle",
                            danger = true,
                            onClick = {
                                CrashLogger.clear(context)
                                logText = ""
                            }
                        )
                    }
                }
                if (showLog) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = logText.takeLast(1800).ifBlank { "Günlük boş — kaydedilmiş bir hata yok." },
                        color = RedoXColors.TextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(10.dp)
                    )
                }
            }
        }

        // ------------------------------------------------------------ Hakkında
        item(key = "about") {
            MCard(modifier = Modifier.fillMaxWidth()) {
                MCardTitle("Hakkında")
                Text(
                    "${BuildConfig.APP_DISPLAY_NAME} • v${BuildConfig.VERSION_NAME}",
                    color = RedoXColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "RedoX = Red + Ox: \"kırmızı boğa\". Bu uygulama eski RedoX TV'den bağımsız, ayrı bir uygulamadır; " +
                        "ayarlarınız ve verileriniz paylaşılmaz.",
                    color = RedoXColors.TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    "Cihaz sınıfı: ${host.deviceKind.uppercase(Locale.ROOT)} • ${Build.MANUFACTURER} ${Build.MODEL} • Android ${Build.VERSION.RELEASE}",
                    color = RedoXColors.TextTertiary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}
