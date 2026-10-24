package com.example.chromecasttv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Eski adlarla (TV arayüz kodunun kullandığı) yeni RedoX belirteçlerine köprü.
 * Değerler artık [RedoXColors]'tan okunur; böylece tema/palet seçimi TV arayüzüne de anında yansır.
 */
object TvThemeColors {
    val BackgroundDeep: Color get() = RedoXColors.Background
    val SidebarGlass: Color get() = RedoXColors.Sidebar
    val SurfaceCard: Color get() = RedoXColors.Surface
    val SurfaceElevated: Color get() = RedoXColors.SurfaceHigh
    val GlassOverlay: Color get() = RedoXColors.Overlay

    val BullRedPrimary: Color get() = RedoXColors.PrimaryBright
    val BullRedBright: Color get() = RedoXColors.Primary
    val BullRedDark: Color get() = RedoXColors.PrimaryDeep

    val AccentCyan: Color get() = RedoXColors.Secondary
    val AccentIndigo: Color get() = RedoXColors.Violet
    val AccentEmerald: Color get() = RedoXColors.Mint
    val AccentRose: Color get() = RedoXColors.Rose
    val AccentAmber: Color get() = RedoXColors.Warn
    val AccentPurple: Color get() = RedoXColors.Violet
    val FavoriteGold: Color get() = RedoXColors.Gold
    val LiveRed: Color get() = RedoXColors.Primary
    val BrokenRed: Color get() = RedoXColors.Danger
    val TextPrimary: Color get() = RedoXColors.TextPrimary
    val TextSecondary: Color get() = RedoXColors.TextSecondary

    fun categoryColor(category: String): Color = RedoXColors.categoryColor(category)

    fun categoryIcon(category: String): ImageVector = RedoXColors.categoryIcon(category)
}

@Composable
fun ChannelBrandLogoBox(
    channel: TvChannel,
    boxSize: Dp = 44.dp,
    fontSize: TextUnit = 12.sp
) {
    val context = LocalContext.current
    val primary = Color(channel.brandPrimaryColor)
    val secondary = Color(channel.brandSecondaryColor)
    val hasLogoUrl = channel.logoUrl.isNotBlank()
    var logoLoadFailed by remember(channel.logoUrl) { mutableStateOf(false) }
    val shape = RoundedCornerShape(boxSize * 0.27f)

    Box(
        modifier = Modifier
            .size(boxSize)
            .clip(shape)
            .background(
                if (hasLogoUrl && !logoLoadFailed) {
                    Brush.linearGradient(listOf(RedoXColors.Ink600, RedoXColors.Ink800))
                } else {
                    Brush.linearGradient(listOf(primary, secondary))
                }
            )
            .border(1.dp, Color.White.copy(alpha = 0.16f), shape),
        contentAlignment = Alignment.Center
    ) {
        if (!hasLogoUrl || logoLoadFailed) {
            BasicText(
                text = channel.shortLogoText,
                style = TextStyle(
                    color = Color.White,
                    fontSize = fontSize,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    lineHeight = (fontSize.value + 1f).sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Clip,
                modifier = Modifier.padding(2.dp)
            )
        } else {
            val req = remember(channel.logoUrl) {
                ImageRequest.Builder(context)
                    .data(channel.logoUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/125.0.0.0")
                    .crossfade(false)
                    .build()
            }
            AsyncImage(
                model = req,
                contentDescription = channel.name,
                contentScale = ContentScale.Fit,
                onError = { logoLoadFailed = true },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            )
        }
    }
}

/**
 * Uygulama teması: Material3 + Android TV Material3 birlikte, RedoX belirteçleriyle.
 * Yazı boyutu çarpanı (Ayarlar > Görünüm) tüm cihazlarda uygulanır.
 */
@Composable
fun RedoXAppTheme(content: @Composable () -> Unit) {
    val palette = RedoXThemeState.palette
    val m3Scheme = androidx.compose.material3.darkColorScheme(
        primary = palette.primary,
        onPrimary = palette.onPrimary,
        secondary = palette.secondary,
        onSecondary = Color(0xFF1B1400),
        background = RedoXColors.Background,
        onBackground = RedoXColors.TextPrimary,
        surface = RedoXColors.Surface,
        onSurface = RedoXColors.TextPrimary,
        surfaceVariant = RedoXColors.SurfaceHigh,
        onSurfaceVariant = RedoXColors.TextSecondary,
        outline = RedoXColors.Line,
        error = RedoXColors.Danger
    )
    val tvScheme = androidx.tv.material3.darkColorScheme(
        primary = palette.primary,
        onPrimary = palette.onPrimary,
        secondary = palette.secondary,
        background = RedoXColors.Background,
        surface = RedoXColors.Surface,
        onSurface = RedoXColors.TextPrimary
    )
    val baseDensity = LocalDensity.current
    val textScale = RedoXThemeState.textScale
    val scaledDensity = remember(baseDensity, textScale) {
        Density(baseDensity.density, baseDensity.fontScale * textScale)
    }
    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        androidx.compose.material3.MaterialTheme(colorScheme = m3Scheme) {
            androidx.tv.material3.MaterialTheme(colorScheme = tvScheme) {
                content()
            }
        }
    }
}
