package com.example.chromecasttv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Yayın rehberi ızgarası: satırlar kanallar, sütunlar zaman (yarım saatlik işaretler). Tüm satırlar tek bir
 * yatay kaydırmayı paylaşır; sol sütundaki kanal adı sabit kalır. Yayındaki programa dokunmak/OK kanalı açar,
 * ileridekine dokunmak/OK hatırlatma ekler ya da kaldırır. Yalnızca temel bileşenler kullanıldığından TV,
 * telefon ve tablette ortak çalışır (odak halkası TV kumandası içindir).
 */
@Composable
fun EpgGridView(
    channels: List<TvChannel>,
    epgMap: Map<String, List<EpgProgram>>,
    reminders: List<Reminder>,
    now: Long,
    onPlayChannel: (TvChannel) -> Unit,
    onToggleReminder: (TvChannel, EpgProgram) -> Unit,
    modifier: Modifier = Modifier,
    spanMinutes: Int = 270,
    dpPerMinute: Float = 3.4f,
    labelWidth: Dp = 118.dp,
    rowHeight: Dp = 66.dp
) {
    val halfHour = 30 * 60_000L
    val windowStart = (now / halfHour) * halfHour
    val windowEnd = windowStart + spanMinutes * 60_000L
    val totalWidth = (spanMinutes * dpPerMinute).dp
    val clock = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val scroll = rememberScrollState()

    Column(modifier = modifier.fillMaxWidth()) {
        // Zaman başlığı
        Row(modifier = Modifier.fillMaxWidth().height(28.dp)) {
            Spacer(modifier = Modifier.width(labelWidth))
            Row(modifier = Modifier.horizontalScroll(scroll)) {
                var t = windowStart
                while (t < windowEnd) {
                    val isNow = now >= t && now < t + halfHour
                    Box(
                        modifier = Modifier
                            .width((30 * dpPerMinute).dp)
                            .fillMaxHeight()
                            .padding(end = 2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isNow) RedoXColors.Primary.copy(alpha = 0.22f) else RedoXColors.SurfaceHigh),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = clock.format(Date(t)) + if (isNow) " • şimdi" else "",
                            color = if (isNow) RedoXColors.PrimaryBright else RedoXColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isNow) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    t += halfHour
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            items(channels, key = { "grid_${it.id}" }) { channel ->
                val programs = remember(channel.epgChannelId, epgMap, windowStart) {
                    epgMap[channel.epgChannelId].orEmpty()
                        .filter { it.endTimeMillis > windowStart && it.startTimeMillis < windowEnd }
                        .sortedBy { it.startTimeMillis }
                        .distinctBy { it.startTimeMillis }
                }
                Row(modifier = Modifier.fillMaxWidth().height(rowHeight)) {
                    // Sabit kanal etiketi
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .width(labelWidth)
                            .fillMaxHeight()
                            .padding(end = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(RedoXColors.SurfaceHigh)
                            .clickable { onPlayChannel(channel) }
                            .padding(horizontal = 6.dp)
                    ) {
                        ChannelBrandLogoBox(channel = channel, boxSize = 30.dp, fontSize = 9.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = channel.name,
                            color = RedoXColors.TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // Programlar (ortak yatay kaydırma)
                    Row(modifier = Modifier.horizontalScroll(scroll).width(totalWidth).fillMaxHeight()) {
                        var cursor = windowStart
                        if (programs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .width(totalWidth)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(RedoXColors.Surface),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "Bu saatler için rehber verisi yok",
                                    color = RedoXColors.TextTertiary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                        for (program in programs) {
                            val from = maxOf(program.startTimeMillis, windowStart, cursor)
                            val to = minOf(program.endTimeMillis, windowEnd)
                            if (to <= from) continue
                            if (from > cursor) {
                                Spacer(modifier = Modifier.width(((from - cursor) / 60_000f * dpPerMinute).dp))
                            }
                            cursor = to
                            val cellWidth = ((to - from) / 60_000f * dpPerMinute).dp
                            val live = program.isCurrentlyLive(now)
                            val hasReminder = reminders.any { it.channelId == channel.id && it.startMillis == program.startTimeMillis }
                            var focused by remember { mutableStateOf(false) }
                            val shape = RoundedCornerShape(10.dp)
                            Box(
                                modifier = Modifier
                                    .width(cellWidth)
                                    .fillMaxHeight()
                                    .padding(end = 2.dp)
                                    .clip(shape)
                                    .background(if (live) RedoXColors.Primary.copy(alpha = 0.20f) else RedoXColors.Surface)
                                    .border(
                                        if (focused) 2.dp else 1.dp,
                                        when {
                                            focused -> RedoXColors.Gold
                                            hasReminder -> RedoXColors.Gold.copy(alpha = 0.7f)
                                            else -> RedoXColors.Line.copy(alpha = 0.6f)
                                        },
                                        shape
                                    )
                                    .onFocusChanged { focused = it.isFocused }
                                    .clickable { if (live) onPlayChannel(channel) else onToggleReminder(channel, program) }
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = clock.format(Date(program.startTimeMillis)),
                                            color = if (live) RedoXColors.PrimaryBright else RedoXColors.TextSecondary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        if (hasReminder) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = RedoXIcons.NotificationsActive,
                                                contentDescription = "Hatırlatma ekli",
                                                tint = RedoXColors.Gold,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = program.title,
                                        color = RedoXColors.TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (live) {
                                    // Yayındaki programın ilerlemesi (alt kenarda ince çizgi)
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .fillMaxWidth(program.progressFraction(now))
                                            .height(3.dp)
                                            .background(RedoXColors.Primary)
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
