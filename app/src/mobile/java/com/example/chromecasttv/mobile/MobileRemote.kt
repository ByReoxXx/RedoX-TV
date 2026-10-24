package com.example.chromecasttv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Telefon = Kumanda: TV'yi ağda bulur, 6 haneli kodla eşleşir, yön tuşları / ses / kanal gönderir,
 * TV'deki kanalları listeler. Kod yalnızca TV'nin ekranında görünür.
 */
@Composable
fun MobileRemoteScreen(
    viewModel: TvViewModel,
    state: TvUiState,
    host: RedoXHost,
    isTablet: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hub = remember { RemoteHub.get(context) }
    val paired by hub.paired.collectAsState()
    val active by hub.active.collectAsState()
    val discovered by hub.discovery.found.collectAsState()

    var snapshot by remember { mutableStateOf<RemoteSnapshot?>(null) }
    var online by remember { mutableStateOf<Boolean?>(null) }
    var tvChannels by remember { mutableStateOf<List<RemoteChannelInfo>?>(null) }
    var channelFilter by remember { mutableStateOf("") }
    var pairTarget by remember { mutableStateOf<DiscoveredTv?>(null) }
    var manualPair by remember { mutableStateOf(false) }
    var removeTarget by remember { mutableStateOf<PairedTv?>(null) }

    // Ağda TV ara (ekran açıkken)
    DisposableEffect(hub) {
        hub.startDiscovery()
        onDispose { hub.stopDiscovery() }
    }

    // Etkin TV'nin durumunu 3 sn'de bir yenile
    LaunchedEffect(active) {
        val tv = active
        snapshot = null
        online = null
        tvChannels = null
        if (tv != null) {
            val client = hub.client(tv)
            while (true) {
                val snap = client.snapshot()
                snapshot = snap
                online = snap != null
                delay(3_000L)
            }
        }
    }

    fun send(block: suspend (RemoteClient) -> Boolean) {
        val tv = active ?: return
        scope.launch {
            val ok = block(hub.client(tv))
            if (!ok) viewModel.showStatusMessage("TV'ye ulaşılamadı. Aynı Wi-Fi'de olduğunuzdan emin olun.")
        }
    }

    fun sendKey(name: String) = send { it.key(name) }

    val unpaired = discovered.filter { d -> paired.none { it.host == d.host && it.port == d.port } }
    val shownChannels = remember(tvChannels, channelFilter) {
        val list = tvChannels.orEmpty()
        val q = channelFilter.trim()
        (if (q.isEmpty()) list else list.filter { it.name.contains(q, ignoreCase = true) || it.number.toString() == q })
            .distinctBy { it.id }
            .take(80)
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // ------------------------------------------------------------ Bağlı TV
        item(key = "active") {
            MCard(modifier = Modifier.fillMaxWidth()) {
                val tv = active
                if (tv == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = RedoXIcons.SettingsRemote,
                            contentDescription = null,
                            tint = RedoXColors.Primary,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Henüz eşleşmiş TV yok", color = RedoXColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                "TV'de RedoX TV 2'yi açın: Ayarlar > Telefon Kumandası'nda 6 haneli bağlantı kodu görünür.",
                                color = RedoXColors.TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    when (online) {
                                        true -> RedoXColors.Mint
                                        false -> RedoXColors.Danger
                                        null -> RedoXColors.TextTertiary
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tv.name, color = RedoXColors.TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, maxLines = 1)
                            Text(
                                text = when (online) {
                                    true -> snapshot?.playing?.let { "Şu an: ${it.name}" } ?: "Bağlı • şu an oynatma yok"
                                    false -> "Ulaşılamıyor (${tv.host}:${tv.port})"
                                    null -> "Bağlanıyor…"
                                },
                                color = RedoXColors.TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (paired.size > 1 || (tv != null && paired.isNotEmpty())) {
                    Spacer(modifier = Modifier.height(10.dp))
                    paired.forEach { p ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            MChip(
                                text = p.name,
                                selected = p.host == tv?.host && p.port == tv?.port,
                                onClick = { hub.select(p) },
                                icon = RedoXIcons.Wifi
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            MGhostButton(
                                text = "Kaldır",
                                icon = RedoXIcons.DeleteOutline,
                                danger = true,
                                onClick = { removeTarget = p }
                            )
                        }
                    }
                }
            }
        }

        // ------------------------------------------------------------ Eşleştirme
        item(key = "discover") {
            MCard(modifier = Modifier.fillMaxWidth()) {
                MCardTitle("Ağdaki TV'ler")
                if (unpaired.isEmpty()) {
                    Text(
                        "Aranıyor… TV ve bu cihaz aynı Wi-Fi ağında olmalı. Bulunamazsa adresi elle girebilirsiniz.",
                        color = RedoXColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }
                unpaired.forEach { d ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(d.name, color = RedoXColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                            Text("${d.host}:${d.port}", color = RedoXColors.TextSecondary, fontSize = 11.sp)
                        }
                        MPrimaryButton(text = "Eşleştir", onClick = { pairTarget = d })
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                MGhostButton(text = "Adresi elle gir", icon = RedoXIcons.Add, onClick = { manualPair = true })
            }
        }

        if (active != null) {
            // -------------------------------------------------------- Kumanda tuşları
            item(key = "pad") {
                MCard(modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            MRemoteButton(RedoXIcons.ArrowBack, "Geri") { sendKey("back") }
                            MRemoteButton(RedoXIcons.Menu, "Menü") { sendKey("menu") }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        MRemoteButton(RedoXIcons.KeyboardArrowUp, "Yukarı", size = 64.dp) { sendKey("up") }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MRemoteButton(RedoXIcons.KeyboardArrowLeft, "Sol", size = 64.dp) { sendKey("left") }
                            MRemoteButton(null, "OK", size = 78.dp, primary = true) { sendKey("ok") }
                            MRemoteButton(RedoXIcons.KeyboardArrowRight, "Sağ", size = 64.dp) { sendKey("right") }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        MRemoteButton(RedoXIcons.KeyboardArrowDown, "Aşağı", size = 64.dp) { sendKey("down") }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            MRemoteButton(RedoXIcons.VolumeDown, "Ses −") {
                                send { client ->
                                    val snap = client.volume(-1)
                                    if (snap != null) snapshot = snap
                                    snap != null
                                }
                            }
                            Text(
                                text = snapshot?.let { "${it.volume}/${it.maxVolume}" } ?: "—",
                                color = RedoXColors.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(56.dp)
                            )
                            MRemoteButton(RedoXIcons.VolumeUp, "Ses +") {
                                send { client ->
                                    val snap = client.volume(+1)
                                    if (snap != null) snapshot = snap
                                    snap != null
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            MRemoteButton(RedoXIcons.Remove, "Kanal −") { send { it.zap(-1) } }
                            MRemoteButton(RedoXIcons.Pause, "Duraklat") { send { it.playPause() } }
                            MRemoteButton(RedoXIcons.Stop, "Durdur") { send { it.stop() } }
                            MRemoteButton(RedoXIcons.Add, "Kanal +") { send { it.zap(+1) } }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { digit -> MDigitButton(digit) { sendKey(digit) } }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        MDigitButton("0") { sendKey("0") }
                    }
                }
            }

            // -------------------------------------------------------- TV'deki kanallar
            item(key = "tvchannels") {
                MCard(modifier = Modifier.fillMaxWidth()) {
                    MCardTitle("TV'de kanal aç")
                    if (tvChannels == null) {
                        Text(
                            "TV'deki kanal listesini yükleyip buradan seçerek TV'de açabilirsiniz.",
                            color = RedoXColors.TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        MGhostButton(
                            text = "TV kanallarını yükle",
                            icon = RedoXIcons.CloudDownload,
                            onClick = {
                                val tv = active
                                if (tv != null) {
                                    scope.launch {
                                        val list = hub.client(tv).channels()
                                        if (list == null) {
                                            viewModel.showStatusMessage("TV kanalları alınamadı.")
                                        } else {
                                            tvChannels = list
                                        }
                                    }
                                }
                            }
                        )
                    } else {
                        MTextField(
                            value = channelFilter,
                            onValueChange = { channelFilter = it },
                            label = "Kanal ara",
                            keyboardType = KeyboardType.Text
                        )
                    }
                }
            }
            if (tvChannels != null) {
                items(shownChannels, key = { "tvch_${it.id}" }) { ch ->
                    MCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (ch.isLocked) {
                                viewModel.showStatusMessage("Bu kanal TV'de PIN ile kilitli; TV ekranından açın.")
                            } else {
                                send { it.play(ch.id) }
                            }
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${ch.number}",
                                color = RedoXColors.Primary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                modifier = Modifier.width(44.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ch.name, color = RedoXColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(ch.category, color = RedoXColors.TextSecondary, fontSize = 11.sp, maxLines = 1)
                            }
                            Icon(RedoXIcons.PlayArrow, contentDescription = "TV'de aç", tint = RedoXColors.Primary)
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------ Pencereler
    pairTarget?.let { target ->
        MPairDialog(
            title = "${target.name} ile eşleş",
            hostInit = target.host,
            portInit = target.port,
            askAddress = false,
            onPair = { h, p, pin ->
                val result = hub.pair(h, p, pin)
                if (result.token != null) null else (result.error ?: "Eşleştirme başarısız.")
            },
            onDismiss = { pairTarget = null }
        )
    }
    if (manualPair) {
        MPairDialog(
            title = "TV'ye elle bağlan",
            hostInit = "",
            portInit = 8585,
            askAddress = true,
            onPair = { h, p, pin ->
                val result = hub.pair(h, p, pin)
                if (result.token != null) null else (result.error ?: "Eşleştirme başarısız.")
            },
            onDismiss = { manualPair = false }
        )
    }
    removeTarget?.let { tv ->
        MConfirmDialog(
            title = "Eşleşmeyi kaldır",
            message = "\"${tv.name}\" artık bu cihazdan kumanda edilemeyecek. TV'de de \"Tüm eşleşmeleri kaldır\" ile erişimi kapatabilirsiniz.",
            confirmText = "Kaldır",
            danger = true,
            onConfirm = {
                hub.remove(tv)
                removeTarget = null
            },
            onDismiss = { removeTarget = null }
        )
    }
}

@Composable
private fun MRemoteButton(
    icon: ImageVector?,
    label: String,
    size: Dp = 52.dp,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(if (primary) RedoXColors.PrimaryBrush else androidx.compose.ui.graphics.SolidColor(RedoXColors.SurfaceHigh))
                .border(1.dp, RedoXColors.Line, CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (primary) Color.White else RedoXColors.TextPrimary,
                    modifier = Modifier.size(size * 0.5f)
                )
            } else {
                Text(label, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
        }
        if (icon != null) {
            Text(label, color = RedoXColors.TextTertiary, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
private fun MDigitButton(digit: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 64.dp, height = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(RedoXColors.SurfaceHigh)
            .border(1.dp, RedoXColors.Line, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(digit, color = RedoXColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

/** Eşleştirme penceresi: (isteğe bağlı adres) + TV ekranındaki 6 haneli kod. */
@Composable
private fun MPairDialog(
    title: String,
    hostInit: String,
    portInit: Int,
    askAddress: Boolean,
    onPair: suspend (host: String, port: Int, pin: String) -> String?,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var hostInput by remember { mutableStateOf(hostInit) }
    var portInput by remember { mutableStateOf(portInit.toString()) }
    var pin by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = RedoXColors.SurfaceHigh,
        titleContentColor = RedoXColors.TextPrimary,
        textContentColor = RedoXColors.TextSecondary,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (askAddress) {
                    MTextField(hostInput, { hostInput = it.trim() }, "TV IP adresi", placeholder = "192.168.1.20", keyboardType = KeyboardType.Uri)
                    MTextField(portInput, { portInput = it.filter { c -> c.isDigit() }.take(5) }, "Bağlantı noktası", keyboardType = KeyboardType.Number)
                }
                Text("TV ekranındaki 6 haneli bağlantı kodunu girin.", fontSize = 13.sp)
                MTextField(
                    value = pin,
                    onValueChange = { pin = it.filter { c -> c.isDigit() }.take(6) },
                    label = "Bağlantı kodu",
                    keyboardType = KeyboardType.NumberPassword,
                    password = true
                )
                val message = error
                if (message != null) {
                    Text(message, color = RedoXColors.Danger, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && pin.length == 6 && hostInput.isNotBlank(),
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        val port = portInput.toIntOrNull() ?: 8585
                        val err = onPair(hostInput, port, pin)
                        busy = false
                        if (err == null) onDismiss() else error = err
                    }
                }
            ) {
                Text(if (busy) "Bağlanıyor…" else "Eşleştir", color = RedoXColors.Primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!busy) onDismiss() }) { Text("İptal", color = RedoXColors.TextSecondary) }
        }
    )
}
