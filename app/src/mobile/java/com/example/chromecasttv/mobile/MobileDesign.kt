package com.example.chromecasttv

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================================
// Telefon / tablet ortak tasarım bileşenleri (RedoX "Kızıl & Altın" belirteçleriyle)
// Tüm renkler RedoXColors'tan okunur; palet/AMOLED seçimi anında yansır.
// ============================================================================

/** Bölüm başlığı (+ isteğe bağlı sağ eylem). */
@Composable
fun MSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = RedoXColors.TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (action != null) action()
    }
}

/** Boş durum: simge + başlık + açıklama + isteğe bağlı eylem. */
@Composable
fun MEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 28.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(RedoXColors.Primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = RedoXColors.Primary,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            color = RedoXColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            color = RedoXColors.TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(14.dp))
            action()
        }
    }
}

/** Yüzey kartı: koyu lacivert zemin, ince çizgi, isteğe bağlı tıklama. */
@Composable
fun MCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(RedoXColors.Surface)
            .border(1.dp, RedoXColors.Line.copy(alpha = 0.7f), shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(14.dp),
        content = content
    )
}

/** Kart içi başlık. */
@Composable
fun MCardTitle(text: String) {
    Text(
        text = text,
        color = RedoXColors.TextPrimary,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

/** Seçilebilir hap düğme (kategori, liste, kalite seçimi). */
@Composable
fun MChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val shape = RoundedCornerShape(50)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(shape)
            .background(if (selected) RedoXColors.Primary.copy(alpha = 0.22f) else RedoXColors.Surface)
            .border(
                width = 1.dp,
                color = if (selected) RedoXColors.Primary else RedoXColors.Line,
                shape = shape
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) RedoXColors.Primary else RedoXColors.TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            color = if (selected) RedoXColors.TextPrimary else RedoXColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

/** Ana eylem düğmesi: kızıl gradyan. */
@Composable
fun MPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = RedoXColors.GradEnd,
            contentColor = RedoXColors.OnPrimary,
            disabledContainerColor = RedoXColors.SurfaceHigh,
            disabledContentColor = RedoXColors.TextTertiary
        ),
        modifier = modifier
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
    }
}

/** İkincil (çizgili) düğme. */
@Composable
fun MGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    danger: Boolean = false
) {
    val tint = if (danger) RedoXColors.Danger else RedoXColors.TextPrimary
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (danger) RedoXColors.Danger.copy(alpha = 0.7f) else RedoXColors.Line),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = tint),
        modifier = modifier
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
    }
}

/** Anahtarlı ayar satırı. */
@Composable
fun MSettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = RedoXColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = RedoXColors.TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = RedoXColors.Primary,
                uncheckedThumbColor = RedoXColors.TextSecondary,
                uncheckedTrackColor = RedoXColors.SurfaceHigh,
                uncheckedBorderColor = RedoXColors.Line
            )
        )
    }
}

/** İnce ilerleme çubuğu (Material3'ün sürüme bağlı API'sinden bağımsız). */
@Composable
fun MProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = RedoXColors.Primary,
    height: Dp = 3.dp
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.12f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(clamped)
                .background(color)
        )
    }
}

/** "CANLI" rozeti. */
@Composable
fun MLiveBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(RedoXColors.Primary)
            .padding(horizontal = 6.dp, vertical = 1.dp)
    ) {
        Text(
            text = "CANLI",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}

/** Kanal satırı (liste görünümü). Dokun: izle • Uzun bas: seçenekler. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MChannelRow(
    channel: TvChannel,
    isFavorite: Boolean,
    isLocked: Boolean,
    isBroken: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isPlaying) RedoXColors.Primary.copy(alpha = 0.14f) else RedoXColors.Surface)
            .border(
                width = 1.dp,
                color = if (isPlaying) RedoXColors.Primary.copy(alpha = 0.6f) else RedoXColors.Line.copy(alpha = 0.5f),
                shape = shape
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        ChannelBrandLogoBox(channel = channel, boxSize = 46.dp, fontSize = 11.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${channel.number}",
                    color = RedoXColors.categoryColor(channel.category),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = channel.name,
                    color = RedoXColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isLocked) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "PIN kilitli",
                        tint = RedoXColors.Gold,
                        modifier = Modifier.size(14.dp)
                    )
                }
                if (isBroken) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = "Yanıt vermiyor",
                        tint = RedoXColors.Danger,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Text(
                text = if (isBroken) "Bu kanalın akışı şu an yanıt vermiyor" else channel.nowTitleOrLive(),
                color = if (isBroken) RedoXColors.Danger else RedoXColors.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (channel.hasRealEpg && !isBroken) {
                Spacer(modifier = Modifier.height(5.dp))
                MProgressBar(progress = channel.progressForUi(), color = RedoXColors.categoryColor(channel.category))
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = if (isFavorite) "Favoride" else "Favori değil",
                tint = if (isFavorite) RedoXColors.Gold else RedoXColors.TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Dikey kanal kartı (raflarda yatay kaydırma, tablet ızgarası). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MChannelCard(
    channel: TvChannel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false
) {
    val shape = RoundedCornerShape(16.dp)
    val accent = RedoXColors.categoryColor(channel.category)
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(listOf(accent.copy(alpha = 0.20f), RedoXColors.Surface))
            )
            .border(1.dp, RedoXColors.Line.copy(alpha = 0.6f), shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            ChannelBrandLogoBox(channel = channel, boxSize = 42.dp, fontSize = 10.sp)
            if (isFavorite) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = "Favoride",
                    tint = RedoXColors.Gold,
                    modifier = Modifier.size(18.dp)
                )
            } else if (channel.hasRealEpg) {
                MLiveBadge()
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = channel.name,
            color = RedoXColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = channel.nowTitleOrLive(),
            color = RedoXColors.TextSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (channel.hasRealEpg) {
            Spacer(modifier = Modifier.height(6.dp))
            MProgressBar(progress = channel.progressForUi(), color = accent)
        }
    }
}

/** Mevcut saate göre kısa selamlama. */
fun mGreeting(nowMillis: Long = System.currentTimeMillis()): String {
    val hour = java.util.Calendar.getInstance().apply { timeInMillis = nowMillis }.get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Günaydın"
        in 12..17 -> "İyi günler"
        in 18..22 -> "İyi akşamlar"
        else -> "İyi geceler"
    }
}

/** "dd.MM HH:mm" biçimli kısa tarih-saat. */
fun mFormatDateTime(millis: Long): String =
    java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date(millis))
