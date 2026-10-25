@file:OptIn(
    androidx.tv.material3.ExperimentalTvMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class
)

package com.example.chromecasttv

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================================
// TV · Radyo (K2 "Kadran"). Liste ekranı, ince mini çubuk ve tam ekran radyo oynatıcı.
// Kumanda yalnızca yön tuşları, OK ve Geri: kadranda ◄ ► istasyon çevirir, ▲ çekmece, OK duraklat,
// OK basılı tut seçenekler, ▼ düğmeler, Geri listeye döner (ses sürer).
// Şimdilik DEMO: gerçek ses çalmaz ([RadioHub] örnek veriyle canlandırır).
// ============================================================================

private enum class RadioOverlay { NONE, DRAWER, OPTIONS }

/** Uzun basışta OK hâlâ basılıdır: odak seçeneklere geçince bırakma olayı bir satıra ulaşıp işlem yapmasın diye tüketilir. */
private class RadioOkGate {
    var swallowUntilRelease = false

    fun consume(event: KeyEvent): Boolean {
        if (!swallowUntilRelease) return false
        val code = event.keyCode
        if (code != KeyEvent.KEYCODE_DPAD_CENTER && code != KeyEvent.KEYCODE_ENTER && code != KeyEvent.KEYCODE_NUMPAD_ENTER) return false
        return when (event.action) {
            KeyEvent.ACTION_UP -> {
                swallowUntilRelease = false
                true
            }
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) {
                    swallowUntilRelease = false
                    false
                } else {
                    true
                }
            }
            else -> false
        }
    }
}

/** Tasarım 960 dp genişlikli ekrana göre çizildi; geniş ekranlarda orantılı büyür. */
@Composable
private fun rememberRadioScale(): Float {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return (widthDp / 960f).coerceIn(0.9f, 1.6f)
}

@Composable
private fun TvRadioClockPill() {
    var timeText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            timeText = format.format(Date())
            delay(15_000L)
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(TvThemeColors.SurfaceCard)
            .border(1.dp, RedoXColors.Line, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp)
    ) {
        Icon(imageVector = RedoXIcons.Schedule, contentDescription = null, tint = RedoXColors.Gold, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = timeText, color = TvThemeColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
    }
}

/** Radyo sekmesinin üst başlığı: başlık, istasyon sayısı, uyku zamanlayıcısı ve saat. */
@Composable
fun TvRadioHeader(stationCount: Int, sleepMinutes: Int?, onCycleSleep: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(imageVector = RedoXIcons.Radio, contentDescription = null, tint = RedoXColors.PrimaryBright, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "Radyo", color = TvThemeColors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false)
            Spacer(modifier = Modifier.width(12.dp))
            TvStatPill(text = "$stationCount istasyon", color = RedoXColors.PrimaryBright)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TvIconPill(
                icon = RedoXIcons.Bedtime,
                label = if (sleepMinutes != null) "Uyku: $sleepMinutes dk" else "Uyku zamanlayıcı",
                onClick = onCycleSleep,
                active = sleepMinutes != null
            )
            TvRadioClockPill()
        }
    }
}

@Composable
private fun RadioPhaseBadge(phase: RadioPhase, k: Float = 1f) {
    val (bg, fg) = when (phase) {
        RadioPhase.PLAYING -> RedoXColors.Primary to Color.White
        RadioPhase.FAILOVER, RadioPhase.ERROR -> RedoXColors.Danger to Color(0xFF1B0A00)
        else -> Color(0xFF3A3950) to Color.White
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp * k))
            .background(bg)
            .padding(horizontal = 9.dp * k, vertical = 3.dp * k)
    ) {
        if (phase == RadioPhase.PLAYING) {
            Box(modifier = Modifier.size(6.dp * k).clip(CircleShape).background(Color.White))
            Spacer(modifier = Modifier.width(6.dp * k))
        }
        Text(text = radioBadgeText(phase), color = fg, fontSize = 12.sp * k, fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun TvRadioChip(
    label: String,
    count: Int,
    selected: Boolean,
    icon: ImageVector?,
    requester: FocusRequester?,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val pill = RoundedCornerShape(50)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(40.dp)
            .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
            .tvSurface(
                focused = isFocused,
                shape = pill,
                color = if (selected) RedoXColors.Primary.copy(alpha = 0.18f) else RedoXColors.Surface,
                idleBorder = if (selected) RedoXColors.Primary else RedoXColors.Line,
                ringWidth = 2.5.dp
            )
            .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
            .clickable { onClick() }
            .padding(horizontal = 18.dp)
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = RedoXColors.Gold, modifier = Modifier.size(17.dp))
            Spacer(modifier = Modifier.width(7.dp))
        }
        Text(
            text = label,
            color = if (selected || isFocused) TvThemeColors.TextPrimary else TvThemeColors.TextSecondary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 15.sp,
            maxLines = 1,
            softWrap = false
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = count.toString(), color = RedoXColors.TextTertiary, fontSize = 13.sp, maxLines = 1, softWrap = false)
    }
}

/** İstasyon satırı (liste ve çekmece). OK: çal • OK basılı tut: favori. */
@Composable
private fun TvRadioRow(
    station: TvChannel,
    playing: Boolean,
    eq: Boolean,
    favorite: Boolean,
    requester: FocusRequester,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .focusRequester(requester)
            .tvSurface(
                focused = focused,
                shape = shape,
                color = if (playing) RedoXColors.Primary.copy(alpha = 0.13f) else Color.Transparent,
                idleBorder = Color.Transparent,
                ringWidth = 2.5.dp
            )
            .onFocusChanged {
                focused = it.isFocused || it.hasFocus
                if (it.isFocused || it.hasFocus) onFocused()
            }
            .tvLongPressFavoriteModifier(itemKey = station.id, onShortClick = onClick, onLongPress = onLongPress)
            .clickable { onClick() }
            .padding(horizontal = 14.dp)
    ) {
        Text(
            text = "${station.number}",
            color = RedoXColors.TextTertiary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(30.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        ChannelBrandLogoBox(channel = station, boxSize = 38.dp, fontSize = 11.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = station.name,
                color = if (playing) RedoXColors.PrimaryBright else TvThemeColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = radioSubtitle(station, 0),
                color = TvThemeColors.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        when {
            playing && eq -> RadioEqBars(color = RedoXColors.Primary, height = 16.dp)
            favorite -> Icon(imageVector = RedoXIcons.StarFilled, contentDescription = "Favoride", tint = RedoXColors.Gold, modifier = Modifier.size(18.dp))
            focused -> Icon(imageVector = RedoXIcons.StarBorder, contentDescription = null, tint = RedoXColors.TextTertiary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun TvRadioRoundButton(
    icon: ImageVector,
    description: String,
    size: Dp,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    active: Boolean = false,
    enabled: Boolean = true,
    requester: FocusRequester? = null,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = if (primary) CircleShape else RoundedCornerShape(size * 0.3f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
            .tvSurface(
                focused = focused && enabled,
                shape = shape,
                color = RedoXColors.SurfaceHigh,
                brush = if (primary) RedoXColors.PrimaryBrush else null,
                idleBorder = if (primary) Color.Transparent else RedoXColors.Line,
                ringWidth = 2.5.dp
            )
            .onFocusChanged { focused = it.isFocused || it.hasFocus }
            .clickable(enabled = enabled) { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = when {
                primary -> Color.White
                active -> RedoXColors.Gold
                else -> TvThemeColors.TextPrimary
            },
            modifier = Modifier.size(size * 0.46f)
        )
    }
}

// ============================================================================
// Liste ekranı
// ============================================================================

@Composable
fun TvRadioScreen(onOpenPlayer: () -> Unit) {
    val ui by RadioHub.state.collectAsState()
    val categories = remember(ui.stations) { radioCategories(ui.stations) }
    val activeCategory = if (ui.selectedCategory in categories) ui.selectedCategory else RADIO_CAT_ALL
    // Favoriler listesi seçildiği andaki görüntüdür: yıldızı kaldırınca satır birden kaybolmaz
    val favSnapshot = remember(activeCategory) { ui.favoriteIds }
    val filtered = remember(ui.stations, activeCategory, favSnapshot) { radioFilter(ui.stations, favSnapshot, activeCategory) }

    val listState = rememberLazyListState()
    val requesters = remember { HashMap<String, FocusRequester>() }
    var focusedId by remember(activeCategory) {
        mutableStateOf(filtered.firstOrNull { it.id == ui.currentId }?.id ?: filtered.firstOrNull()?.id)
    }
    val focusedStation = filtered.firstOrNull { it.id == focusedId } ?: filtered.firstOrNull()

    // Sekme açılınca odağı çalan (yoksa ilk) istasyonun satırına ver
    var initialDone by remember { mutableStateOf(false) }
    LaunchedEffect(filtered.isNotEmpty()) {
        if (filtered.isNotEmpty() && !initialDone) {
            val idx = filtered.indexOfFirst { it.id == ui.currentId }.coerceAtLeast(0)
            try { listState.scrollToItem(idx) } catch (_: Exception) {}
            delay(60L)
            try { requesters[filtered[idx].id]?.requestFocus() } catch (_: Exception) {}
            initialDone = true
        }
    }
    LaunchedEffect(activeCategory) {
        if (initialDone) {
            try { listState.scrollToItem(0) } catch (_: Exception) {}
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                items(categories, key = { it }) { category ->
                    TvRadioChip(
                        label = category,
                        count = radioCategoryCount(ui.stations, ui.favoriteIds, category),
                        selected = category == activeCategory,
                        icon = if (category == RADIO_CAT_FAVORITES) RedoXIcons.Star else null,
                        requester = null,
                        onClick = { RadioHub.selectCategory(category) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            if (!ui.loaded || filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(TvThemeColors.SurfaceCard),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (!ui.loaded) "Radyo listesi yükleniyor…" else "Bu kategoride istasyon yok.",
                        color = TvThemeColors.TextSecondary,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    itemsIndexed(filtered, key = { _, station -> station.id }) { _, station ->
                        val requester = requesters.getOrPut(station.id) { FocusRequester() }
                        TvRadioRow(
                            station = station,
                            playing = station.id == ui.currentId && ui.isActive,
                            eq = ui.phase == RadioPhase.PLAYING,
                            favorite = station.id in ui.favoriteIds,
                            requester = requester,
                            onFocused = { focusedId = station.id },
                            onClick = {
                                if (station.id == ui.currentId && ui.isActive) onOpenPlayer() else RadioHub.play(station.id)
                            },
                            onLongPress = { RadioHub.toggleFavorite(station.id) }
                        )
                    }
                }
            }

            // Dar listede (yüksek yoğunluklu ekran) ipuçları ikinci satıra sarılır, kesilmez
            FlowRow(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 32.dp)
                    .padding(vertical = 4.dp)
            ) {
                TvKeyHint(key = "OK", label = "Çal")
                TvKeyHint(key = "OK basılı tut", label = "Favori")
                TvKeyHint(key = "►", label = "Çalan kart")
                TvKeyHint(key = "Geri", label = "Yan menü")
            }
        }

        Spacer(modifier = Modifier.width(18.dp))

        Column(
            modifier = Modifier
                .width(290.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TvRadioNowCard(ui = ui, onOpenPlayer = onOpenPlayer)
            TvRadioSelectedCard(ui = ui, station = focusedStation)
            Spacer(modifier = Modifier.weight(1f))
            RadioToast(message = ui.toast, fontSize = 13.sp)
        }
    }
}

/** "Çalıyor" kartı: istasyon, şarkı, küçük kadran ve tam ekran oynatıcıyı açan düğme. */
@Composable
private fun TvRadioNowCard(ui: RadioUiState, onOpenPlayer: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    val current = ui.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF1A1D3C), Color(0xFF1A1530), Color(0xFF3B0F24))))
            .border(1.dp, RedoXColors.Line, shape)
            .padding(16.dp)
    ) {
        if (current == null || !ui.isActive) {
            Text(text = "ŞİMDİ ÇALIYOR", color = RedoXColors.TextTertiary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "Henüz bir istasyon çalmıyor", color = TvThemeColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Listeden bir istasyon seçip OK'a basın.", color = TvThemeColors.TextSecondary, fontSize = 13.sp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RadioPhaseBadge(phase = ui.phase)
                if (ui.phase == RadioPhase.PLAYING) RadioEqBars(color = RedoXColors.PrimaryBright, height = 14.dp)
                Text(text = radioFormatText(current, ui.sourceIndex), color = TvThemeColors.TextSecondary, fontSize = 12.sp, maxLines = 1)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChannelBrandLogoBox(channel = current, boxSize = 54.dp, fontSize = 15.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = current.name, color = TvThemeColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = radioStatusText(ui),
                        color = if (ui.phase == RadioPhase.FAILOVER || ui.phase == RadioPhase.ERROR) RedoXColors.Danger else RedoXColors.Gold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            RadioDial(
                stations = ui.stations,
                centerIndex = ui.stations.indexOfFirst { it.id == current.id }.coerceAtLeast(0),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                tileCurrent = 38.dp,
                tileSide = 28.dp,
                pitch = 78.dp,
                cornerRadius = 14.dp,
                showNames = false
            )
            Spacer(modifier = Modifier.height(12.dp))
            var openFocused by remember { mutableStateOf(false) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .tvSurface(focused = openFocused, shape = RoundedCornerShape(14.dp), ringWidth = 2.5.dp)
                    .onFocusChanged { openFocused = it.isFocused || it.hasFocus }
                    .clickable { onOpenPlayer() }
                    .padding(horizontal = 14.dp)
            ) {
                Text(text = "Oynatıcıyı aç", color = TvThemeColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(imageVector = RedoXIcons.KeyboardArrowRight, contentDescription = null, tint = RedoXColors.Gold, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** Listede odaktaki istasyonun ayrıntısı ve hızlı eylemler. */
@Composable
private fun TvRadioSelectedCard(ui: RadioUiState, station: TvChannel?) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(RedoXColors.Surface)
            .border(1.dp, RedoXColors.Line, shape)
            .padding(16.dp)
    ) {
        Text(text = "SEÇİLİ İSTASYON", color = RedoXColors.TextTertiary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(10.dp))
        if (station == null) {
            Text(text = "—", color = TvThemeColors.TextSecondary, fontSize = 15.sp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChannelBrandLogoBox(channel = station, boxSize = 48.dp, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = station.name, color = TvThemeColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = radioSubtitle(station, 0) + if (station.sources.size > 1) " · ${station.sources.size} yayın" else "",
                        color = TvThemeColors.TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                TvActionButton(text = "Çal", icon = RedoXIcons.PlayArrow, isPrimary = true, onClick = { RadioHub.play(station.id) })
                TvIconPill(
                    icon = if (station.id in ui.favoriteIds) RedoXIcons.StarFilled else RedoXIcons.StarBorder,
                    label = if (station.id in ui.favoriteIds) "Favoriden çıkar" else "Favori",
                    onClick = { RadioHub.toggleFavorite(station.id) },
                    active = station.id in ui.favoriteIds
                )
            }
        }
    }
}

// ============================================================================
// Diğer sekmelerde: ince "Şimdi çalıyor" çubuğu
// ============================================================================

@Composable
fun TvRadioMiniBar(onOpenRadio: () -> Unit) {
    val ui by RadioHub.state.collectAsState()
    val station = ui.current ?: return
    val shape = RoundedCornerShape(18.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(shape)
            .background(RedoXColors.Overlay)
            .border(1.dp, RedoXColors.Line, shape)
            .padding(horizontal = 10.dp)
    ) {
        var infoFocused by remember { mutableStateOf(false) }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .tvSurface(focused = infoFocused, shape = RoundedCornerShape(14.dp), color = Color.Transparent, idleBorder = Color.Transparent, ringWidth = 2.5.dp)
                .onFocusChanged { infoFocused = it.isFocused || it.hasFocus }
                .clickable { onOpenRadio() }
                .padding(horizontal = 6.dp)
        ) {
            ChannelBrandLogoBox(channel = station, boxSize = 38.dp, fontSize = 11.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = station.name, color = TvThemeColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (ui.phase == RadioPhase.PLAYING) RadioEqBars(color = RedoXColors.Primary, height = 13.dp)
                }
                Text(
                    text = radioStatusText(ui),
                    color = if (ui.phase == RadioPhase.FAILOVER || ui.phase == RadioPhase.ERROR) RedoXColors.Danger else RedoXColors.Gold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (infoFocused) {
                Text(text = "OK: Radyo'ya git", color = RedoXColors.TextTertiary, fontSize = 12.sp, maxLines = 1, softWrap = false)
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TvRadioRoundButton(icon = RedoXIcons.SkipPrevious, description = "Önceki istasyon", size = 40.dp) { RadioHub.stepNow(-1) }
            TvRadioRoundButton(
                icon = if (ui.phase == RadioPhase.PLAYING) RedoXIcons.Pause else RedoXIcons.PlayArrow,
                description = "Duraklat / sürdür",
                size = 46.dp,
                primary = true
            ) { RadioHub.togglePause() }
            TvRadioRoundButton(icon = RedoXIcons.SkipNext, description = "Sonraki istasyon", size = 40.dp) { RadioHub.stepNow(1) }
        }
    }
}

// ============================================================================
// Tam ekran radyo oynatıcı (K2 Kadran)
// ============================================================================

@Composable
fun TvRadioPlayerScreen() {
    val ui by RadioHub.state.collectAsState()
    val k = rememberRadioScale()
    var overlay by remember { mutableStateOf(RadioOverlay.NONE) }
    val interactive = overlay == RadioOverlay.NONE
    val dialRequester = remember { FocusRequester() }
    var dialFocused by remember { mutableStateOf(false) }
    val gate = remember { RadioOkGate() }

    // Geri: önce katman, sonra oynatıcı (ses sürer, radyo listesine dönülür)
    BackHandler {
        if (overlay != RadioOverlay.NONE) overlay = RadioOverlay.NONE else RadioHub.closePlayer()
    }
    LaunchedEffect(overlay) {
        if (overlay == RadioOverlay.NONE) {
            delay(60L)
            try { dialRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    val dialStation = ui.dialStation
    val shown = ui.current ?: ui.tuning
    val tuning = ui.tuningId != null
    val phase = ui.phase
    val dim = if (tuning) 0.42f else 1f
    val isFav = shown != null && shown.id in ui.favoriteIds

    val dialKeys = Modifier.onPreviewKeyEvent { ev ->
        if (ev.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) {
            false
        } else {
            when (ev.nativeKeyEvent.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> { RadioHub.zap(-1); true }
                KeyEvent.KEYCODE_DPAD_RIGHT -> { RadioHub.zap(1); true }
                KeyEvent.KEYCODE_DPAD_UP -> { overlay = RadioOverlay.DRAWER; true }
                else -> false
            }
        }
    }
    val dialOk = Modifier.tvLongPressFavoriteModifier(
        itemKey = "radio_dial",
        onShortClick = { RadioHub.onOk() },
        onLongPress = {
            gate.swallowUntilRelease = true
            overlay = RadioOverlay.OPTIONS
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RedoXColors.Background)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(RedoXColors.Primary.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.2f),
                        radius = size.width * 0.7f
                    )
                )
            }
            .onPreviewKeyEvent { gate.consume(it.nativeKeyEvent) }
    ) {
        // Sol üst etiket
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 36.dp * k, top = 24.dp * k)
        ) {
            Icon(imageVector = RedoXIcons.Radio, contentDescription = null, tint = RedoXColors.PrimaryBright, modifier = Modifier.size(22.dp * k))
            Spacer(modifier = Modifier.width(8.dp * k))
            Text(text = "RADYO", color = TvThemeColors.TextSecondary, fontSize = 14.sp * k, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
        // Sağ üst: uyku göstergesi ve saat
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 36.dp * k, top = 18.dp * k)
        ) {
            if (ui.sleepMinutes != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(TvThemeColors.SurfaceCard)
                        .border(1.dp, RedoXColors.Line, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp)
                ) {
                    Icon(imageVector = RedoXIcons.Bedtime, contentDescription = null, tint = RedoXColors.Gold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "${ui.sleepMinutes} dk", color = RedoXColors.Gold, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
            TvRadioClockPill()
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 30.dp * k)
        ) {
            // Rozet satırı
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp * k),
                modifier = Modifier.alpha(dim)
            ) {
                RadioPhaseBadge(phase = phase, k = k)
                if (phase == RadioPhase.PLAYING) RadioEqBars(color = RedoXColors.PrimaryBright, height = 16.dp * k, barWidth = 3.dp * k)
                if (shown != null) {
                    Text(
                        text = radioSubtitle(shown, ui.sourceIndex) + (if (radioIsDemo(shown)) " · demo" else ""),
                        color = TvThemeColors.TextSecondary,
                        fontSize = 14.sp * k,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
            // Ad
            Text(
                text = shown?.name ?: "",
                color = TvThemeColors.TextPrimary,
                fontSize = 47.sp * k,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(dim)
                    .padding(top = 6.dp * k)
                    .fillMaxWidth(0.9f)
            )
            // Şarkı ya da durum satırı
            Box(modifier = Modifier.alpha(dim).padding(top = 6.dp * k).height(36.dp * k), contentAlignment = Alignment.Center) {
                when (phase) {
                    RadioPhase.PLAYING, RadioPhase.PAUSED -> {
                        val song = ui.song
                        if (song != null) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(text = song.title, color = RedoXColors.Gold, fontSize = 26.sp * k, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                                Text(text = "  —  ${song.artist}", color = TvThemeColors.TextSecondary, fontSize = 19.sp * k, maxLines = 1)
                            }
                        } else {
                            Text(text = "Canlı yayın", color = RedoXColors.Gold, fontSize = 24.sp * k, fontWeight = FontWeight.Bold)
                        }
                    }
                    RadioPhase.CONNECTING -> Text(text = radioStatusText(ui), color = TvThemeColors.TextSecondary, fontSize = 24.sp * k, fontWeight = FontWeight.SemiBold)
                    RadioPhase.FAILOVER, RadioPhase.ERROR -> Text(text = radioStatusText(ui), color = RedoXColors.Danger, fontSize = 22.sp * k, fontWeight = FontWeight.Bold)
                    RadioPhase.STOPPED -> Unit
                }
            }

            Spacer(modifier = Modifier.height(18.dp * k))

            // Kadran
            val dialStations = ui.stations
            RadioDial(
                stations = dialStations,
                centerIndex = dialStations.indexOfFirst { it.id == dialStation?.id }.coerceAtLeast(0),
                modifier = Modifier
                    .width(840.dp * k)
                    .height(166.dp * k)
                    .focusRequester(dialRequester)
                    .onFocusChanged { dialFocused = it.isFocused || it.hasFocus }
                    .then(dialKeys)
                    .then(dialOk)
                    .then(if (interactive) Modifier.focusable() else Modifier),
                tileCurrent = 79.dp * k,
                tileSide = 50.dp * k,
                pitch = 168.dp * k,
                focused = dialFocused && interactive,
                cornerRadius = 25.dp * k,
                nameCurrent = 16.sp * k,
                nameSide = 13.sp * k,
                playingId = if (ui.phase == RadioPhase.PLAYING) ui.currentId else null
            )

            Spacer(modifier = Modifier.height(10.dp * k))
            RadioStatusArea(ui = ui, k = k, interactive = interactive, onBackToList = { RadioHub.closePlayer() })

            Spacer(modifier = Modifier.height(8.dp * k))
            Box(modifier = Modifier.height(24.dp * k), contentAlignment = Alignment.Center) {
                if (ui.spectrumOn) {
                    RadioSpectrum(
                        active = phase == RadioPhase.PLAYING && !tuning,
                        modifier = Modifier
                            .width(600.dp * k)
                            .height(24.dp * k),
                        barCount = 50,
                        barWidth = (3.dp * k)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp * k))
            Row(horizontalArrangement = Arrangement.spacedBy(13.dp * k), verticalAlignment = Alignment.CenterVertically) {
                TvRadioRoundButton(
                    icon = if (isFav) RedoXIcons.StarFilled else RedoXIcons.StarBorder,
                    description = "Favori", size = 40.dp * k, active = isFav, enabled = interactive
                ) { shown?.let { RadioHub.toggleFavorite(it.id) } }
                TvRadioRoundButton(icon = RedoXIcons.SkipPrevious, description = "Önceki istasyon", size = 44.dp * k, enabled = interactive) { RadioHub.stepNow(-1) }
                TvRadioRoundButton(
                    icon = if (phase == RadioPhase.PLAYING) RedoXIcons.Pause else RedoXIcons.PlayArrow,
                    description = "Duraklat / sürdür", size = 58.dp * k, primary = true, enabled = interactive
                ) { RadioHub.togglePause() }
                TvRadioRoundButton(icon = RedoXIcons.SkipNext, description = "Sonraki istasyon", size = 44.dp * k, enabled = interactive) { RadioHub.stepNow(1) }
                TvRadioRoundButton(icon = RedoXIcons.Refresh, description = "Yayın kaynağı", size = 40.dp * k, enabled = interactive) { RadioHub.cycleSource() }
            }
        }

        RadioToast(
            message = ui.toast,
            fontSize = 14.sp * k,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp * k)
        )

        if (overlay == RadioOverlay.DRAWER) {
            TvRadioDrawer(ui = ui, k = k, onPlay = { id ->
                RadioHub.play(id)
                overlay = RadioOverlay.NONE
            })
        }
        if (overlay == RadioOverlay.OPTIONS) {
            TvRadioOptions(ui = ui, k = k, onClose = { overlay = RadioOverlay.NONE })
        }
    }
}

/** Kadranın altındaki durum satırı: ipuçları, ayarlanıyor göstergesi, bağlanma/yedek yayın bilgisi ya da hata eylemleri. */
@Composable
private fun RadioStatusArea(ui: RadioUiState, k: Float, interactive: Boolean, onBackToList: () -> Unit) {
    val tuning = ui.tuning
    val shape = RoundedCornerShape(18.dp * k)
    val station = ui.current
    Box(modifier = Modifier.height(36.dp * k), contentAlignment = Alignment.Center) {
        when {
            tuning != null -> {
                val progress = remember(tuning.id) { Animatable(0f) }
                LaunchedEffect(tuning.id) { progress.animateTo(1f, tween(RadioHub.TUNE_DELAY_MS.toInt())) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp * k),
                    modifier = Modifier
                        .clip(shape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 18.dp * k, vertical = 7.dp * k)
                ) {
                    Text(text = tuning.name, color = TvThemeColors.TextPrimary, fontSize = 15.sp * k, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                    Text(text = "'e ayarlanıyor", color = TvThemeColors.TextSecondary, fontSize = 15.sp * k, maxLines = 1)
                    Box(
                        modifier = Modifier
                            .width(110.dp * k)
                            .height(5.dp * k)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.14f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress.value)
                                .background(RedoXColors.Gold)
                        )
                    }
                    Text(text = "OK: hemen çal", color = RedoXColors.TextTertiary, fontSize = 13.sp * k, maxLines = 1)
                }
            }
            ui.phase == RadioPhase.PLAYING || ui.phase == RadioPhase.PAUSED -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp * k),
                    modifier = Modifier
                        .clip(shape)
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(horizontal = 16.dp * k, vertical = 4.dp * k)
                ) {
                    TvKeyHint(key = "◄ ►", label = "İstasyon")
                    TvKeyHint(key = "OK", label = if (ui.phase == RadioPhase.PLAYING) "Duraklat" else "Sürdür")
                    TvKeyHint(key = "▲", label = "Liste")
                    TvKeyHint(key = "OK basılı tut", label = "Seçenekler")
                }
            }
            ui.phase == RadioPhase.CONNECTING -> {
                Text(
                    text = (station?.let { radioFormatText(it, ui.sourceIndex) } ?: "") + " · arabelleğe alınıyor",
                    color = TvThemeColors.TextSecondary,
                    fontSize = 15.sp * k
                )
            }
            ui.phase == RadioPhase.FAILOVER -> {
                val label = station?.sources?.getOrNull(1)?.label ?: "Yayın 2"
                Text(
                    text = "Ana yayın yanıt vermedi  →  ${label.substringBefore(" (")} deneniyor",
                    color = TvThemeColors.TextPrimary,
                    fontSize = 15.sp * k,
                    modifier = Modifier
                        .clip(shape)
                        .background(RedoXColors.Danger.copy(alpha = 0.14f))
                        .border(1.dp, RedoXColors.Danger, shape)
                        .padding(horizontal = 18.dp * k, vertical = 7.dp * k)
                )
            }
            ui.phase == RadioPhase.ERROR -> {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp * k), verticalAlignment = Alignment.CenterVertically) {
                    TvRadioActionChip(text = "Yeniden dene", enabled = interactive) { RadioHub.togglePause() }
                    TvRadioActionChip(text = "Sonraki istasyon ►", enabled = interactive) { RadioHub.stepNow(1) }
                    TvRadioActionChip(text = "Radyo listesi", enabled = interactive) { onBackToList() }
                }
            }
            else -> Unit
        }
    }
}

@Composable
private fun TvRadioActionChip(text: String, enabled: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val pill = RoundedCornerShape(50)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(34.dp)
            .tvSurface(focused = focused && enabled, shape = pill, color = RedoXColors.Surface, ringWidth = 2.5.dp)
            .onFocusChanged { focused = it.isFocused || it.hasFocus }
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp)
    ) {
        Text(text = text, color = TvThemeColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
    }
}

/** ▲ ile açılan istasyon çekmecesi: tür çipleri + liste; OK çalar, Geri kapatır. */
@Composable
private fun BoxScope.TvRadioDrawer(ui: RadioUiState, k: Float, onPlay: (String) -> Unit) {
    val categories = remember(ui.stations) { radioCategories(ui.stations) }
    val activeCategory = if (ui.selectedCategory in categories) ui.selectedCategory else RADIO_CAT_ALL
    val filtered = remember(ui.stations, activeCategory) { radioFilter(ui.stations, ui.favoriteIds, activeCategory) }
    val listState = rememberLazyListState()
    val requesters = remember { HashMap<String, FocusRequester>() }
    LaunchedEffect(Unit) {
        val idx = filtered.indexOfFirst { it.id == ui.currentId }.coerceAtLeast(0)
        try { listState.scrollToItem(idx) } catch (_: Exception) {}
        delay(80L)
        try { filtered.getOrNull(idx)?.let { requesters[it.id] }?.requestFocus() } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
    )
    Column(
        modifier = Modifier
            .align(Alignment.CenterStart)
            .fillMaxHeight()
            .width(380.dp * k)
            .background(RedoXColors.Overlay)
            .border(1.dp, RedoXColors.Line)
            .padding(horizontal = 14.dp * k, vertical = 20.dp * k)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 6.dp)) {
            Icon(imageVector = RedoXIcons.Radio, contentDescription = null, tint = RedoXColors.PrimaryBright, modifier = Modifier.size(26.dp * k))
            Spacer(modifier = Modifier.width(10.dp * k))
            Text(text = "İstasyonlar", color = TvThemeColors.TextPrimary, fontSize = 24.sp * k, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(modifier = Modifier.height(12.dp * k))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            items(categories, key = { it }) { category ->
                TvRadioChip(
                    label = category,
                    count = radioCategoryCount(ui.stations, ui.favoriteIds, category),
                    selected = category == activeCategory,
                    icon = if (category == RADIO_CAT_FAVORITES) RedoXIcons.Star else null,
                    requester = null,
                    onClick = { RadioHub.selectCategory(category) }
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp * k))
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            itemsIndexed(filtered, key = { _, station -> station.id }) { _, station ->
                val requester = requesters.getOrPut(station.id) { FocusRequester() }
                TvRadioRow(
                    station = station,
                    playing = station.id == ui.currentId && ui.isActive,
                    eq = ui.phase == RadioPhase.PLAYING,
                    favorite = station.id in ui.favoriteIds,
                    requester = requester,
                    onFocused = {},
                    onClick = { onPlay(station.id) },
                    onLongPress = { RadioHub.toggleFavorite(station.id) }
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
        ) {
            TvKeyHint(key = "▲▼", label = "Gez")
            TvKeyHint(key = "OK", label = "Çal")
            TvKeyHint(key = "Geri", label = "Kapat")
        }
    }
}

@Composable
private fun TvRadioOptionRow(
    icon: ImageVector,
    label: String,
    value: String = "",
    danger: Boolean = false,
    requester: FocusRequester? = null,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
            .tvSurface(focused = focused, shape = shape, color = Color.Transparent, idleBorder = Color.Transparent, ringWidth = 2.5.dp)
            .onFocusChanged { focused = it.isFocused || it.hasFocus }
            .clickable { onClick() }
            .padding(horizontal = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (danger) RedoXColors.Danger else RedoXColors.Gold,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = if (danger) RedoXColors.Danger else TvThemeColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.weight(1f)
        )
        if (value.isNotEmpty()) {
            Text(text = value, color = RedoXColors.Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        }
    }
}

/** OK basılı tutunca açılan Seçenekler menüsü (video oynatıcıdaki gibi sağ altta). */
@Composable
private fun BoxScope.TvRadioOptions(ui: RadioUiState, k: Float, onClose: () -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(80L)
        try { first.requestFocus() } catch (_: Exception) {}
    }
    val station = ui.current
    val fav = station != null && station.id in ui.favoriteIds
    val shape = RoundedCornerShape(24.dp * k)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.38f))
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 36.dp * k, bottom = 70.dp * k)
            .width(330.dp * k)
            .clip(shape)
            .background(RedoXColors.Overlay)
            .border(1.dp, Color.White.copy(alpha = 0.12f), shape)
            .padding(8.dp * k)
    ) {
        TvRadioOptionRow(
            icon = if (fav) RedoXIcons.StarFilled else RedoXIcons.StarBorder,
            label = if (fav) "Favorilerden çıkar" else "Favorilere ekle",
            requester = first
        ) { station?.let { RadioHub.toggleFavorite(it.id) } }
        TvRadioOptionRow(
            icon = RedoXIcons.Refresh,
            label = "Yayın kaynağı",
            value = station?.let { radioFormatText(it, ui.sourceIndex) } ?: ""
        ) { RadioHub.cycleSource() }
        TvRadioOptionRow(icon = RedoXIcons.Bedtime, label = "Uyku zamanlayıcı", value = radioSleepText(ui.sleepMinutes)) { RadioHub.cycleSleep() }
        TvRadioOptionRow(icon = RedoXIcons.MusicNote, label = "Ses dalgası", value = if (ui.spectrumOn) "Açık" else "Kapalı") { RadioHub.toggleSpectrum() }
        TvRadioOptionRow(icon = RedoXIcons.VolumeUp, label = "Radyo cızırtısı", value = if (ui.staticOn) "Açık" else "Kapalı") { RadioHub.toggleStatic() }
        TvRadioOptionRow(icon = RedoXIcons.PlayArrow, label = "Açılışta son radyoyu çal", value = if (ui.autoStart) "Açık" else "Kapalı") { RadioHub.toggleAutoStart() }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 8.dp).background(Color.White.copy(alpha = 0.10f)))
        TvRadioOptionRow(icon = RedoXIcons.Info, label = "Yayın bilgisi") {
            if (station != null) {
                RadioHub.toast("${station.name} · ${radioFormatText(station, ui.sourceIndex)} · örnek veri, gerçek ses çalmıyor")
            }
            onClose()
        }
        TvRadioOptionRow(icon = RedoXIcons.VisibilityOff, label = "İstasyonu gizle") {
            station?.let { RadioHub.hide(it.id) }
            onClose()
        }
        if (ui.hiddenIds.isNotEmpty()) {
            TvRadioOptionRow(icon = RedoXIcons.Refresh, label = "Gizlenenleri geri getir", value = "${ui.hiddenIds.size}") {
                RadioHub.restoreHidden()
                onClose()
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 8.dp).background(Color.White.copy(alpha = 0.10f)))
        TvRadioOptionRow(icon = RedoXIcons.Close, label = "Radyoyu kapat", danger = true) { RadioHub.stop() }
        Text(
            text = (station?.let { radioFormatText(it, ui.sourceIndex) } ?: "") + (if (station?.let { radioIsDemo(it) } == true) "  ·  demo (gerçek ses çalmıyor)" else ""),
            color = RedoXColors.TextTertiary,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 2.dp)
        )
    }
}
