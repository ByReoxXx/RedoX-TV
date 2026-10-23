package com.example.chromecasttv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Yayın rehberi: gerçek XMLTV verisi olan kanallar ve programları.
 * Dokun: yayındaki programda kanalı aç • ileride başlayacak programda hatırlatma ekle/kaldır.
 */
@Composable
fun MobileGuideScreen(
    viewModel: TvViewModel,
    state: TvUiState,
    features: FeatureState,
    host: RedoXHost,
    nav: MobileNav,
    actions: MChannelActions
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = System.currentTimeMillis()
        }
    }

    val epg = features.epg
    val all = remember(state.builtInChannels, state.customM3uChannels) {
        state.builtInChannels + state.customM3uChannels
    }
    var onlyWithGuide by remember { mutableStateOf(true) }
    val shown = remember(all, onlyWithGuide) {
        (if (onlyWithGuide) all.filter { it.hasRealEpg }.ifEmpty { all } else all).distinctBy { it.id }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "status") {
            MCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = if (epg.hasRealData) RedoXColors.Mint else RedoXColors.Gold,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (epg.hasRealData) "Gerçek yayın rehberi açık" else "Gerçek yayın rehberi yüklenmedi",
                            color = RedoXColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = when {
                                epg.isLoading -> "Rehber güncelleniyor…"
                                epg.hasRealData -> "${epg.matchedChannels} kanal • ${epg.programmeCount} program" +
                                    if (epg.lastUpdatedMillis > 0L) " • ${mFormatDateTime(epg.lastUpdatedMillis)}" else ""
                                else -> "Uydurma program adı göstermiyoruz. Ayarlar'dan bir XMLTV adresi ekleyin."
                            },
                            color = RedoXColors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.padding(top = 10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MGhostButton(
                        text = if (epg.isLoading) "Güncelleniyor…" else "Rehberi güncelle",
                        onClick = { viewModel.features.refreshEpg(true) },
                        icon = Icons.Rounded.Refresh
                    )
                    if (!epg.hasRealData) {
                        MGhostButton(
                            text = "Ayarlar",
                            onClick = { nav.push(MobileTab.SETTINGS) },
                            icon = Icons.Rounded.Settings
                        )
                    } else {
                        MGhostButton(
                            text = if (onlyWithGuide) "Hepsini göster" else "Yalnız rehberli",
                            onClick = { onlyWithGuide = !onlyWithGuide }
                        )
                    }
                }
            }
        }

        items(shown, key = { "guide_${it.id}" }) { channel ->
            val programs = if (channel.hasRealEpg) {
                state.epgMap[channel.epgChannelId].orEmpty()
                    .filter { it.endTimeMillis > now }
                    .distinctBy { it.startTimeMillis }
                    .take(8)
            } else {
                emptyList()
            }
            MCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.requestPlayChannel(channel) }
                ) {
                    ChannelBrandLogoBox(channel = channel, boxSize = 38.dp, fontSize = 10.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${channel.number}. ${channel.name}",
                            color = RedoXColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = channel.category,
                            color = RedoXColors.categoryColor(channel.category),
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.padding(top = 8.dp))
                if (programs.isEmpty()) {
                    Text(
                        text = "Canlı yayın • bu kanal için rehber verisi yok",
                        color = RedoXColors.TextSecondary,
                        fontSize = 12.sp
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(programs, key = { "p_${channel.id}_${it.startTimeMillis}" }) { program ->
                            val isLive = program.isCurrentlyLive(now)
                            val hasReminder = features.reminders.any {
                                it.channelId == channel.id && it.startMillis == program.startTimeMillis
                            }
                            val shape = RoundedCornerShape(12.dp)
                            Column(
                                modifier = Modifier
                                    .width(176.dp)
                                    .clip(shape)
                                    .background(
                                        if (isLive) RedoXColors.categoryColor(channel.category).copy(alpha = 0.22f)
                                        else RedoXColors.SurfaceHigh
                                    )
                                    .border(
                                        1.dp,
                                        if (hasReminder) RedoXColors.Gold else RedoXColors.Line.copy(alpha = 0.6f),
                                        shape
                                    )
                                    .clickable {
                                        if (isLive) {
                                            viewModel.requestPlayChannel(channel)
                                        } else {
                                            host.requestNotificationPermission()
                                            viewModel.features.toggleReminder(channel, program)
                                        }
                                    }
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isLive) "CANLI • ${program.timeWindowText}" else program.timeWindowText,
                                        color = if (isLive) RedoXColors.Primary else RedoXColors.TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (hasReminder) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Rounded.NotificationsActive,
                                            contentDescription = "Hatırlatma ekli",
                                            tint = RedoXColors.Gold,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = program.title,
                                    color = RedoXColors.TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when {
                                        isLive -> "Dokun: izle"
                                        hasReminder -> "Dokun: hatırlatmayı kaldır"
                                        else -> "Dokun: hatırlat"
                                    },
                                    color = RedoXColors.TextTertiary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
