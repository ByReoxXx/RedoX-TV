package com.example.chromecasttv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================================
// Telefon / tablet · Radyo (K2 "Kadran"): liste, mini oynatıcı çubuğu, tam ekran oynatıcı, seçenekler ve istasyon sayfaları.
// Telefonda oynatıcı tam ekran bir katmandır; tablette Radyo sekmesinde listenin yanında sürekli açık bir paneldir.
// Şimdilik DEMO: gerçek ses çalmaz ([RadioHub] örnek veriyle canlandırır).
// ============================================================================

private enum class RadioSheet { NONE, OPTIONS, STATIONS, HISTORY }

/** Radyo sekmesi: tür çipleri + istasyon listesi (tablette sağda oynatıcı paneli). */
@Composable
fun MobileRadioScreen(isTablet: Boolean) {
    val ui by RadioHub.state.collectAsState()
    val categories = remember(ui.stations) { radioCategories(ui.stations) }
    val active = if (ui.selectedCategory in categories) ui.selectedCategory else RADIO_CAT_ALL
    // Favoriler listesi seçildiği andaki görüntüdür: yıldızı kaldırınca satır birden kaybolmaz
    val favSnapshot = remember(active) { ui.favoriteIds }
    val byCategory = remember(ui.stations, active, favSnapshot) { radioFilter(ui.stations, favSnapshot, active) }
    var query by remember { mutableStateOf("") }
    val filtered = remember(byCategory, query) {
        val q = query.trim()
        if (q.isEmpty()) byCategory else byCategory.filter { it.name.contains(q, ignoreCase = true) }
    }

    if (isTablet) {
        Row(modifier = Modifier.fillMaxSize()) {
            RadioListPane(
                ui = ui,
                categories = categories,
                active = active,
                filtered = filtered,
                query = query,
                onQuery = { query = it },
                openOnPlayingTap = false,
                modifier = Modifier
                    .weight(0.46f)
                    .fillMaxHeight()
            )
            Box(
                modifier = Modifier
                    .weight(0.54f)
                    .fillMaxHeight()
                    .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
            ) {
                MobileRadioPlayerPanel()
            }
        }
    } else {
        RadioListPane(
            ui = ui,
            categories = categories,
            active = active,
            filtered = filtered,
            query = query,
            onQuery = { query = it },
            openOnPlayingTap = true,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun RadioListPane(
    ui: RadioUiState,
    categories: List<String>,
    active: String,
    filtered: List<TvChannel>,
    query: String,
    onQuery: (String) -> Unit,
    openOnPlayingTap: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        MTextField(
            value = query,
            onValueChange = onQuery,
            label = "İstasyon ara",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories, key = { it }) { category ->
                MChip(
                    text = "$category (${radioCategoryCount(ui.stations, ui.favoriteIds, category)})",
                    selected = category == active,
                    onClick = { RadioHub.selectCategory(category) },
                    icon = if (category == RADIO_CAT_FAVORITES) RedoXIcons.Star else null
                )
            }
        }
        if (!ui.loaded || filtered.isEmpty()) {
            MEmptyState(
                icon = RedoXIcons.Radio,
                title = if (!ui.loaded) "Radyo listesi yükleniyor…" else "Bu kategoride istasyon yok",
                message = if (!ui.loaded) "" else "Başka bir kategori seçin."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { "radio_${it.id}" }) { station ->
                    val playing = station.id == ui.currentId && ui.isActive
                    MRadioRow(
                        station = station,
                        playing = playing,
                        showBars = playing && ui.phase == RadioPhase.PLAYING,
                        favorite = station.id in ui.favoriteIds,
                        onClick = {
                            if (playing && openOnPlayingTap) RadioHub.openPlayer() else RadioHub.play(station.id)
                        },
                        onToggleFavorite = { RadioHub.toggleFavorite(station.id) }
                    )
                }
            }
        }
    }
}

/** İstasyon satırı: dokun çal (çalıyorsa oynatıcıyı aç), sağdaki yıldız favori. */
@Composable
private fun MRadioRow(
    station: TvChannel,
    playing: Boolean,
    showBars: Boolean,
    favorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(shape)
            .background(if (playing) RedoXColors.Primary.copy(alpha = 0.12f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        ChannelBrandLogoBox(channel = station, boxSize = 46.dp, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = station.name,
                color = if (playing) RedoXColors.PrimaryBright else RedoXColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = radioSubtitle(station, 0),
                color = RedoXColors.TextSecondary,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (showBars) {
            RadioEqBars(color = RedoXColors.Primary, height = 18.dp)
            Spacer(modifier = Modifier.width(8.dp))
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (favorite) RedoXIcons.StarFilled else RedoXIcons.StarBorder,
                contentDescription = if (favorite) "Favoriden çıkar" else "Favori",
                tint = if (favorite) RedoXColors.Gold else RedoXColors.TextTertiary
            )
        }
    }
}

// ============================================================================
// Mini oynatıcı çubuğu (alt gezintinin üstünde)
// ============================================================================

@Composable
fun MobileRadioMiniBar(onExpand: () -> Unit) {
    val ui by RadioHub.state.collectAsState()
    val station = ui.current ?: return
    val shape = RoundedCornerShape(20.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clip(shape)
            .background(RedoXColors.SurfaceHigh)
            .border(1.dp, RedoXColors.Line, shape)
            .clickable { onExpand() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        ChannelBrandLogoBox(channel = station, boxSize = 44.dp, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = station.name, color = RedoXColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (ui.phase == RadioPhase.PLAYING) RadioEqBars(color = RedoXColors.Primary, height = 13.dp, barWidth = 2.5.dp)
            }
            Text(
                text = radioStatusText(ui, compact = true),
                color = if (ui.phase == RadioPhase.FAILOVER || ui.phase == RadioPhase.ERROR) RedoXColors.Danger else RedoXColors.Gold,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        MRadioRound(
            icon = if (ui.phase == RadioPhase.PLAYING) RedoXIcons.Pause else RedoXIcons.PlayArrow,
            description = "Duraklat / sürdür",
            size = 42.dp,
            primary = true
        ) { RadioHub.togglePause() }
        Spacer(modifier = Modifier.width(6.dp))
        IconButton(onClick = { RadioHub.stepNow(1) }) {
            Icon(imageVector = RedoXIcons.SkipNext, contentDescription = "Sonraki istasyon", tint = RedoXColors.TextPrimary)
        }
    }
}

@Composable
private fun MRadioRound(
    icon: ImageVector,
    description: String,
    size: Dp,
    primary: Boolean = false,
    active: Boolean = false,
    onClick: () -> Unit
) {
    val shape = if (primary) CircleShape else RoundedCornerShape(size * 0.3f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(shape)
            .then(
                if (primary) {
                    Modifier.background(RedoXColors.PrimaryBrush, shape)
                } else {
                    Modifier
                        .background(RedoXColors.SurfaceHigh, shape)
                        .border(1.dp, RedoXColors.Line, shape)
                }
            )
            .clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = when {
                primary -> Color.White
                active -> RedoXColors.Gold
                else -> RedoXColors.TextPrimary
            },
            modifier = Modifier.size(size * 0.46f)
        )
    }
}

// ============================================================================
// Tam ekran oynatıcı (telefon) ve sürekli açık panel (tablet)
// ============================================================================

/** Telefon: tam ekran radyo oynatıcı katmanı (Geri ya da aşağı ok ile kapanır; ses sürer). */
@Composable
fun MobileRadioPlayerOverlay() {
    BackHandler { RadioHub.closePlayer() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RedoXColors.Background)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        MobileRadioPlayerContent(onClose = { RadioHub.closePlayer() })
    }
}

/** Tablet: Radyo sekmesindeki oynatıcı paneli. */
@Composable
fun MobileRadioPlayerPanel() {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF1A1D3C), Color(0xFF1A1530), Color(0xFF3B0F24))))
            .border(1.dp, RedoXColors.Line, shape)
    ) {
        MobileRadioPlayerContent(onClose = null)
    }
}

@Composable
private fun MobileRadioPlayerContent(onClose: (() -> Unit)?) {
    val ui by RadioHub.state.collectAsState()
    var sheet by remember { mutableStateOf(RadioSheet.NONE) }
    BackHandler(enabled = sheet != RadioSheet.NONE) { sheet = RadioSheet.NONE }

    // Boştayken (tablet paneli) kadrandaki ilk istasyon gösterilir; başlık boş kalmaz
    val shown = ui.current ?: ui.tuning ?: ui.stations.firstOrNull()
    val tuning = ui.tuningId != null
    val phase = ui.phase
    val dim = if (tuning) 0.42f else 1f
    val isFav = shown != null && shown.id in ui.favoriteIds

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(RedoXColors.Primary.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.12f),
                        radius = size.width * 0.9f
                    )
                )
            }
    ) {
        val wide = maxWidth > 480.dp
        val tileCurrent = if (wide) 96.dp else 76.dp
        val tileSide = if (wide) 64.dp else 54.dp
        val pitch = if (wide) 150.dp else 112.dp
        val dialHeight = if (wide) 190.dp else 168.dp
        val density = LocalDensity.current
        val stepPx = with(density) { (pitch * 0.55f).toPx() }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            if (onClose != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    IconButton(onClick = onClose) {
                        Icon(imageVector = RedoXIcons.ExpandMore, contentDescription = "Küçült", tint = RedoXColors.TextPrimary)
                    }
                    Text(
                        text = "ŞİMDİ ÇALIYOR",
                        color = RedoXColors.TextSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { sheet = RadioSheet.OPTIONS }) {
                        Icon(imageVector = RedoXIcons.MoreHoriz, contentDescription = "Seçenekler", tint = RedoXColors.TextPrimary)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(18.dp))
            }
            // Uzun ekranlarda kalan boşluk dört aralığa dağılır (içerik üste yığılmaz); kısa ekranda bu aralıklar 0 olur
            Spacer(modifier = Modifier.weight(0.4f))

            // Rozet satırı
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .alpha(dim)
            ) {
                MRadioPhaseBadge(phase)
                if (phase == RadioPhase.PLAYING) RadioEqBars(color = RedoXColors.PrimaryBright, height = 16.dp, barWidth = 3.dp)
            }
            Text(
                text = shown?.name ?: "",
                color = RedoXColors.TextPrimary,
                fontSize = if (wide) 40.sp else 34.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                    .alpha(dim)
            )
            Text(
                text = (shown?.let { radioSubtitle(it, ui.sourceIndex) } ?: "") + (if (shown != null && radioIsDemo(shown)) " · demo" else ""),
                color = RedoXColors.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.alpha(dim)
            )
            // Şarkı ya da durum
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                    .height(52.dp)
                    .alpha(dim)
            ) {
                when (phase) {
                    RadioPhase.PLAYING, RadioPhase.PAUSED -> {
                        val song = ui.song
                        if (song != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = song.title, color = RedoXColors.Gold, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(text = song.artist, color = RedoXColors.TextSecondary, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        } else {
                            Text(text = "Canlı yayın", color = RedoXColors.Gold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    RadioPhase.CONNECTING -> Text(text = radioStatusText(ui), color = RedoXColors.TextSecondary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    RadioPhase.FAILOVER, RadioPhase.ERROR -> Text(
                        text = radioStatusText(ui),
                        color = RedoXColors.Danger,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    RadioPhase.STOPPED -> Unit
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Kadran: sağa/sola kaydır ya da yandaki karoya dokun
            val dialStation = ui.dialStation
            RadioDial(
                stations = ui.stations,
                centerIndex = ui.stations.indexOfFirst { it.id == dialStation?.id }.coerceAtLeast(0),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dialHeight)
                    .pointerInput(stepPx) {
                        var acc = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { acc = 0f },
                            onDragEnd = { acc = 0f },
                            onDragCancel = { acc = 0f }
                        ) { change, dragAmount ->
                            change.consume()
                            acc += dragAmount
                            while (acc <= -stepPx) {
                                RadioHub.zap(1)
                                acc += stepPx
                            }
                            while (acc >= stepPx) {
                                RadioHub.zap(-1)
                                acc -= stepPx
                            }
                        }
                    },
                tileCurrent = tileCurrent,
                tileSide = tileSide,
                pitch = pitch,
                cornerRadius = 0.dp,
                nameCurrent = 13.sp,
                nameSide = 11.5.sp,
                playingId = if (phase == RadioPhase.PLAYING) ui.currentId else null,
                onTileClick = { offset -> if (offset == 0) RadioHub.onOk() else RadioHub.zap(offset) }
            )
            Text(
                text = if (tuning) "${ui.tuning?.name ?: ""}'e ayarlanıyor…" else "◄  kadranı kaydırarak istasyon değiştir  ►",
                color = RedoXColors.TextTertiary,
                fontSize = 12.5.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            if (ui.spectrumOn) {
                RadioSpectrum(
                    active = phase == RadioPhase.PLAYING && !tuning,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .width(300.dp)
                        .height(32.dp),
                    barCount = 40,
                    barWidth = 4.dp
                )
            } else {
                Spacer(modifier = Modifier.height(40.dp))
            }

            Spacer(modifier = Modifier.weight(0.6f))
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                MRadioRound(
                    icon = if (isFav) RedoXIcons.StarFilled else RedoXIcons.StarBorder,
                    description = "Favori", size = 46.dp, active = isFav
                ) { shown?.let { RadioHub.toggleFavorite(it.id) } }
                MRadioRound(icon = RedoXIcons.SkipPrevious, description = "Önceki istasyon", size = 54.dp) { RadioHub.stepNow(-1) }
                MRadioRound(
                    icon = if (phase == RadioPhase.PLAYING) RedoXIcons.Pause else RedoXIcons.PlayArrow,
                    description = "Duraklat / sürdür", size = 76.dp, primary = true
                ) { RadioHub.togglePause() }
                MRadioRound(icon = RedoXIcons.SkipNext, description = "Sonraki istasyon", size = 54.dp) { RadioHub.stepNow(1) }
                MRadioRound(icon = RedoXIcons.Bedtime, description = "Uyku zamanlayıcı", size = 46.dp, active = ui.sleepMinutes != null) { RadioHub.cycleSleep() }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                MRadioPill(icon = RedoXIcons.Menu, label = "İstasyonlar") { sheet = RadioSheet.STATIONS }
                MRadioPill(
                    icon = RedoXIcons.Refresh,
                    label = shown?.let { radioFormatText(it, ui.sourceIndex).substringBefore(' ') } ?: "Kaynak"
                ) { RadioHub.cycleSource() }
                MRadioPill(icon = RedoXIcons.Bedtime, label = if (ui.sleepMinutes != null) "${ui.sleepMinutes} dk" else "Uyku") { RadioHub.cycleSleep() }
                if (onClose == null) {
                    MRadioPill(icon = RedoXIcons.MoreHoriz, label = "Seçenekler") { sheet = RadioSheet.OPTIONS }
                }
            }
            Spacer(modifier = Modifier.weight(0.3f))
        }

        RadioToast(
            message = ui.toast,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        )

        when (sheet) {
            RadioSheet.OPTIONS -> MRadioOptionsSheet(ui = ui, onDismiss = { sheet = RadioSheet.NONE }, onHistory = { sheet = RadioSheet.HISTORY })
            RadioSheet.HISTORY -> MRadioHistorySheet(ui = ui, onDismiss = { sheet = RadioSheet.NONE })
            RadioSheet.STATIONS -> MRadioStationsSheet(ui = ui, onDismiss = { sheet = RadioSheet.NONE })
            RadioSheet.NONE -> Unit
        }
    }
}

@Composable
private fun MRadioPhaseBadge(phase: RadioPhase) {
    val (bg, fg) = when (phase) {
        RadioPhase.PLAYING -> RedoXColors.Primary to Color.White
        RadioPhase.FAILOVER, RadioPhase.ERROR -> RedoXColors.Danger to Color(0xFF1B0A00)
        else -> Color(0xFF3A3950) to Color.White
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        if (phase == RadioPhase.PLAYING) {
            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color.White))
            Spacer(modifier = Modifier.width(5.dp))
        }
        Text(text = radioBadgeText(phase), color = fg, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

@Composable
private fun MRadioPill(icon: ImageVector, label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(40.dp)
            .clip(shape)
            .background(RedoXColors.SurfaceHigh, shape)
            .border(1.dp, RedoXColors.Line, shape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = RedoXColors.Gold, modifier = Modifier.size(17.dp))
        Spacer(modifier = Modifier.width(7.dp))
        Text(text = label, color = RedoXColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

// ----------------------------------------------------------------------------
// Alt sayfalar
// ----------------------------------------------------------------------------

@Composable
private fun BoxScope.MRadioSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() }
    )
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(RedoXColors.Surface)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(RedoXColors.Line)
        )
        Spacer(modifier = Modifier.height(10.dp))
        content()
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun MRadioSectionLabel(text: String) {
    Text(
        text = text,
        color = RedoXColors.TextTertiary,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.6.sp,
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
    )
}

@Composable
private fun MRadioSheetItem(icon: ImageVector, label: String, danger: Boolean = false, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable { onClick() }
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = if (danger) RedoXColors.Danger else RedoXColors.Gold, modifier = Modifier.size(21.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(text = label, color = if (danger) RedoXColors.Danger else RedoXColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BoxScope.MRadioOptionsSheet(ui: RadioUiState, onDismiss: () -> Unit, onHistory: () -> Unit) {
    val station = ui.current
    val fav = station != null && station.id in ui.favoriteIds
    MRadioSheet(onDismiss = onDismiss) {
        Text(text = "Seçenekler", color = RedoXColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        if (station != null && station.sources.size > 1) {
            MRadioSectionLabel("YAYIN KAYNAĞI")
            station.sources.forEachIndexed { index, source ->
                val selected = index == ui.sourceIndex
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) RedoXColors.SurfaceHigh else Color.Transparent)
                        .border(1.dp, if (selected) RedoXColors.Gold else RedoXColors.Line, RoundedCornerShape(14.dp))
                        .clickable { RadioHub.selectSource(index) }
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                ) {
                    Text(
                        text = source.label.substringBefore(" ("),
                        color = RedoXColors.TextPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = radioFormatText(station, index), color = RedoXColors.TextSecondary, fontSize = 12.5.sp)
                }
            }
        }
        MRadioSectionLabel("UYKU ZAMANLAYICI")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf<Int?>(null, 30, 60, 90).forEach { minutes ->
                MChip(
                    text = if (minutes == null) "Kapalı" else "$minutes dk",
                    selected = ui.sleepChoice == minutes,
                    onClick = { RadioHub.setSleep(minutes) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        MRadioSheetItem(
            icon = if (fav) RedoXIcons.StarFilled else RedoXIcons.StarBorder,
            label = if (fav) "Favorilerden çıkar" else "Favorilere ekle"
        ) { station?.let { RadioHub.toggleFavorite(it.id) } }
        MRadioSheetItem(icon = RedoXIcons.MusicNote, label = if (ui.spectrumOn) "Ses dalgasını kapat" else "Ses dalgasını aç") { RadioHub.toggleSpectrum() }
        MRadioSheetItem(icon = RedoXIcons.VolumeUp, label = if (ui.staticOn) "Radyo cızırtısını kapat" else "Radyo cızırtısını aç") { RadioHub.toggleStatic() }
        MRadioSheetItem(icon = RedoXIcons.History, label = "Şarkı geçmişi (${ui.history.size})") { onHistory() }
        MRadioSheetItem(icon = RedoXIcons.VisibilityOff, label = "İstasyonu gizle") {
            station?.let { RadioHub.hide(it.id) }
            onDismiss()
        }
        if (ui.hiddenIds.isNotEmpty()) {
            MRadioSheetItem(icon = RedoXIcons.Refresh, label = "Gizlenenleri geri getir (${ui.hiddenIds.size})") {
                RadioHub.restoreHidden()
                onDismiss()
            }
        }
        MRadioSheetItem(icon = RedoXIcons.Close, label = "Radyoyu kapat", danger = true) { RadioHub.stop() }
        if (ui.current?.let { radioIsDemo(it) } == true) {
            Text(
                text = "Örnek veri: gerçek ses çalmıyor.",
                color = RedoXColors.TextTertiary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun BoxScope.MRadioHistorySheet(ui: RadioUiState, onDismiss: () -> Unit) {
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val clock = remember { java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale("tr")) }
    MRadioSheet(onDismiss = onDismiss) {
        Text(text = "Şarkı geçmişi", color = RedoXColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "Yayından okunan şarkı bilgisi; yalnızca bu cihazda saklanır.", color = RedoXColors.TextTertiary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        if (ui.history.isEmpty()) {
            Text(text = "Henüz şarkı bilgisi yok. Şarkı adı yayınlayan bir istasyonu dinleyince burada birikir.", color = RedoXColors.TextSecondary, fontSize = 14.sp)
        } else {
            LazyColumn(modifier = Modifier.height(300.dp)) {
                items(ui.history.size) { i ->
                    val h = ui.history[i]
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(
                            text = if (h.artist.isBlank()) h.title else "${h.title} — ${h.artist}",
                            color = RedoXColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                        Text(text = "${h.station} · ${clock.format(java.util.Date(h.timeMillis))}", color = RedoXColors.TextSecondary, fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MChip(text = "Listeyi kopyala", selected = false, onClick = {
                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(ui.history.joinToString("
") { h ->
                        (if (h.artist.isBlank()) h.title else "${h.artist} - ${h.title}") + " (${h.station})"
                    }))
                    RadioHub.toast("Liste kopyalandı")
                })
                MChip(text = "Temizle", selected = false, onClick = { RadioHub.clearHistory() })
            }
        }
    }
}

@Composable
private fun BoxScope.MRadioStationsSheet(ui: RadioUiState, onDismiss: () -> Unit) {
    val categories = remember(ui.stations) { radioCategories(ui.stations) }
    val active = if (ui.selectedCategory in categories) ui.selectedCategory else RADIO_CAT_ALL
    val filtered = remember(ui.stations, active) { radioFilter(ui.stations, ui.favoriteIds, active) }
    MRadioSheet(onDismiss = onDismiss) {
        Text(text = "İstasyonlar", color = RedoXColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories, key = { it }) { category ->
                MChip(
                    text = "$category (${radioCategoryCount(ui.stations, ui.favoriteIds, category)})",
                    selected = category == active,
                    onClick = { RadioHub.selectCategory(category) },
                    icon = if (category == RADIO_CAT_FAVORITES) RedoXIcons.Star else null
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        LazyColumn(modifier = Modifier.height(340.dp)) {
            items(filtered, key = { "sheet_${it.id}" }) { station ->
                val playing = station.id == ui.currentId && ui.isActive
                MRadioRow(
                    station = station,
                    playing = playing,
                    showBars = playing && ui.phase == RadioPhase.PLAYING,
                    favorite = station.id in ui.favoriteIds,
                    onClick = {
                        RadioHub.play(station.id)
                        onDismiss()
                    },
                    onToggleFavorite = { RadioHub.toggleFavorite(station.id) }
                )
            }
        }
    }
}
