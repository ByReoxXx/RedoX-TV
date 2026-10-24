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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Ana sayfa: karşılama, "devam et" kartı (yoksa marka kartı), hızlı eylemler,
 * raflar (şimdi yayında / en çok izlenenler / favoriler) ve yaklaşan hatırlatmalar.
 */
@Composable
fun MobileHomeScreen(
    viewModel: TvViewModel,
    state: TvUiState,
    features: FeatureState,
    host: RedoXHost,
    isTablet: Boolean,
    nav: MobileNav,
    actions: MChannelActions
) {
    val shelves = remember(state, features) { ShelfBuilder.build(state, features) }
    val lastChannel = state.recentChannels.firstOrNull()
    val upcoming = remember(features.reminders) {
        val now = System.currentTimeMillis()
        features.reminders.filter { it.startMillis > now - 60_000L }.sortedBy { it.startMillis }.take(3)
    }
    val cardWidth = if (isTablet) 190.dp else 160.dp

    LazyColumn(
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // ------------------------------------------------------------ Karşılama
        item(key = "greeting") {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            ) {
                Text(
                    text = mGreeting(),
                    color = RedoXColors.TextSecondary,
                    fontSize = 14.sp
                )
                Text(
                    text = "Ne izlemek istersin?",
                    color = RedoXColors.TextPrimary,
                    fontSize = if (isTablet) 32.sp else 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // ------------------------------------------------------------ Devam et (büyük kart) ya da marka kartı
        if (lastChannel != null) {
            item(key = "resume") {
                MResumeHero(
                    channel = lastChannel,
                    isTablet = isTablet,
                    onPlay = { viewModel.requestPlayChannel(lastChannel) }
                )
            }
        } else {
            item(key = "welcome") {
                val shape = RoundedCornerShape(24.dp)
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(shape)
                        .background(
                            Brush.linearGradient(
                                listOf(RedoXColors.WineGlow, RedoXColors.SurfaceHigh, RedoXColors.Surface)
                            )
                        )
                        .border(1.dp, RedoXColors.Line, shape)
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RedoXMark(markSize = if (isTablet) 112.dp else 84.dp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            RedoXWordmarkImage(width = if (isTablet) 220.dp else 170.dp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${state.builtInChannels.size + state.customM3uChannels.size} kanal hazır" +
                                    if (features.epg.hasRealData) " • gerçek rehber açık" else "",
                                color = RedoXColors.TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                    MPrimaryButton(
                        text = "Canlı TV'yi aç",
                        onClick = { nav.selectRoot(MobileTab.LIVE) },
                        icon = RedoXIcons.LiveTv,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ------------------------------------------------------------ Güncelleme duyurusu
        val update = features.update
        if (update != null && update.isNewer) {
            item(key = "update") {
                MCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = RedoXIcons.SystemUpdate,
                            contentDescription = null,
                            tint = RedoXColors.Gold,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Yeni sürüm: v${update.latestVersion}",
                                color = RedoXColors.TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Kurulu sürüm: v${update.currentVersion}",
                                color = RedoXColors.TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                        MGhostButton(text = "Aç", onClick = { host.openUrl(update.htmlUrl) })
                    }
                }
            }
        }

        // ------------------------------------------------------------ Rehber ipucu
        if (!features.epg.hasRealData) {
            item(key = "epg_hint") {
                MCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onClick = { nav.push(MobileTab.SETTINGS) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = RedoXIcons.CalendarMonth,
                            contentDescription = null,
                            tint = RedoXColors.PrimaryBright,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Gerçek yayın rehberini açın",
                                color = RedoXColors.TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Ayarlar > Yayın Rehberi'ne bir XMLTV adresi ekleyin. Uydurma program adı göstermiyoruz; " +
                                    "rehber olmayan kanallarda \"Canlı yayın\" yazar.",
                                color = RedoXColors.TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // ------------------------------------------------------------ Hızlı eylemler
        item(key = "quick") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { MChip("Canlı TV", false, { nav.selectRoot(MobileTab.LIVE) }, icon = RedoXIcons.LiveTv) }
                item { MChip("Rehber", false, { nav.selectRoot(MobileTab.GUIDE) }, icon = RedoXIcons.CalendarMonth) }
                item { MChip("Favoriler", false, { nav.selectRoot(MobileTab.FAVORITES) }, icon = RedoXIcons.Star) }
                item { MChip("Ara", false, { nav.push(MobileTab.SEARCH) }, icon = RedoXIcons.Search) }
                item { MChip("Kumanda", false, { nav.selectRoot(MobileTab.REMOTE) }, icon = RedoXIcons.SettingsRemote) }
                item { MChip("Bölünmüş Ekran", false, { nav.push(MobileTab.MULTI) }, icon = RedoXIcons.GridView) }
                item { MChip("Kaynaklar", false, { nav.push(MobileTab.SOURCES) }, icon = RedoXIcons.UploadFile) }
                item { MChip("Ayarlar", false, { nav.push(MobileTab.SETTINGS) }, icon = RedoXIcons.Settings) }
            }
        }

        // ------------------------------------------------------------ Raflar
        shelves.forEach { shelf ->
            item(key = "shelf_${shelf.kind}") {
                Column {
                    MSectionTitle(
                        text = shelf.title,
                        action = {
                            Text(
                                text = "Tümü",
                                color = RedoXColors.Gold,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(role = Role.Button) {
                                        nav.selectRoot(
                                            if (shelf.kind == ShelfKind.FAVORITES) MobileTab.FAVORITES else MobileTab.LIVE
                                        )
                                    }
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                            )
                        }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(shelf.channels.distinctBy { it.id }, key = { "sh_${shelf.kind}_${it.id}" }) { ch ->
                            MChannelCard(
                                channel = ch,
                                isFavorite = state.favoriteIds.contains(ch.id),
                                onClick = { viewModel.requestPlayChannel(ch) },
                                onLongClick = { actions.onLongPress(ch) },
                                modifier = Modifier.width(cardWidth)
                            )
                        }
                    }
                }
            }
        }

        // ------------------------------------------------------------ Yaklaşan hatırlatmalar
        if (upcoming.isNotEmpty()) {
            item(key = "reminders") {
                Column {
                    MSectionTitle("Yaklaşan hatırlatmalar")
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        upcoming.forEach { reminder ->
                            MCard(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(RedoXColors.Gold.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = RedoXIcons.NotificationsActive,
                                            contentDescription = null,
                                            tint = RedoXColors.Gold,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = reminder.title,
                                            color = RedoXColors.TextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${reminder.channelName} • ${mFormatDateTime(reminder.startMillis)}",
                                            color = RedoXColors.TextSecondary,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** "Kaldığın yerden devam et": son izlenen kanal, program bilgisi, ilerleme ve büyük oynat düğmesi. */
@Composable
private fun MResumeHero(
    channel: TvChannel,
    isTablet: Boolean,
    onPlay: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(RedoXColors.WineGlow, RedoXColors.SurfaceHigh, RedoXColors.Surface)
                )
            )
            .border(1.dp, RedoXColors.Line, shape)
            .clickable(role = Role.Button, onClick = onPlay)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Kaldığın yerden devam et",
                color = RedoXColors.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            MLiveBadge()
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChannelBrandLogoBox(
                channel = channel,
                boxSize = if (isTablet) 84.dp else 60.dp,
                fontSize = if (isTablet) 22.sp else 16.sp
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    color = RedoXColors.TextPrimary,
                    fontSize = if (isTablet) 30.sp else 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = channel.nowTitleOrLive() + if (channel.hasRealEpg) " • ${channel.currentProgramTime}" else "",
                    color = RedoXColors.TextSecondary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(if (isTablet) 68.dp else 56.dp)
                    .clip(CircleShape)
                    .background(RedoXColors.PrimaryBrush),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = RedoXIcons.PlayArrow,
                    contentDescription = "İzle",
                    tint = Color.White,
                    modifier = Modifier.size(if (isTablet) 34.dp else 28.dp)
                )
            }
        }
        if (channel.hasRealEpg) {
            MProgressBar(progress = channel.progressForUi(), height = 5.dp)
        }
    }
}
