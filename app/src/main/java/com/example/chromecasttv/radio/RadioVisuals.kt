package com.example.chromecasttv

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

// ============================================================================
// Radyo "Kadran" görselleri (TV, telefon ve tablet ortak): tuner kadranı, ses dalgası, ekolayzer.
// Yalnızca foundation bileşenleri kullanılır; renkler RedoXColors'tan okunur.
// ============================================================================

private fun mix(a: Float, b: Float, t: Float): Float = a + (b - a) * t

private val DialTop = Color(0xFF0E0E18)
private val DialBottom = Color(0xFF171724)
private val DialEdge = Color(0xFF12121E)

/**
 * Yatay tuner kadranı: ibre sabit, istasyonlar kayar. [centerIndex] ortadaki istasyonun [stations] içindeki sırası;
 * değişince kadran en kısa yoldan (liste başı ile sonu birbirine bağlı) kısa bir animasyonla kayar.
 * [focused] true ise altın odak halkası çizilir. [onTileClick] verilirse karolara dokunmak ortadaki karoya göre
 * göreli bir adım bildirir (0 = ortadaki).
 */
@Composable
fun RadioDial(
    stations: List<TvChannel>,
    centerIndex: Int,
    modifier: Modifier = Modifier,
    tileCurrent: Dp = 79.dp,
    tileSide: Dp = 50.dp,
    pitch: Dp = 168.dp,
    focused: Boolean = false,
    cornerRadius: Dp = 25.dp,
    nameCurrent: TextUnit = 16.sp,
    nameSide: TextUnit = 13.sp,
    showNames: Boolean = true,
    playingId: String? = null,
    onTileClick: ((Int) -> Unit)? = null
) {
    val n = stations.size
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(DialTop, DialBottom)), shape)
            .border(if (focused) 3.dp else 1.dp, if (focused) RedoXColors.Gold else RedoXColors.Line, shape)
    ) {
        if (n == 0) return@Box

        // Sürekli konum: liste sınırında kısa yoldan kayar
        var target by remember { mutableFloatStateOf(centerIndex.toFloat()) }
        var last by remember { mutableIntStateOf(centerIndex) }
        LaunchedEffect(centerIndex, n) {
            var delta = centerIndex - last
            if (delta > n / 2) delta -= n else if (delta < -n / 2) delta += n
            target += delta
            last = centerIndex
        }
        val pos by animateFloatAsState(targetValue = target, animationSpec = tween(170), label = "radio_dial_pos")

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val wPx = constraints.maxWidth.toFloat()
            val hPx = constraints.maxHeight.toFloat()
            val cx = wPx / 2f
            val pitchPx = with(density) { pitch.toPx() }
            val curPx = with(density) { tileCurrent.toPx() }
            val sidePx = with(density) { tileSide.toPx() }
            val base = floor(pos).toInt()
            val gold = RedoXColors.Gold

            // Üstte hafif cam parlaması
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.07f), Color.Transparent), endY = hPx * 0.38f))
            )

            // Cetvel çizgileri: istasyon başına 10 aralık; istasyon çentiği uzun, yarım adım orta boy
            Canvas(modifier = Modifier.fillMaxSize()) {
                val baseline = size.height * 0.877f
                val stroke = 1.5.dp.toPx()
                for (j in (base - 4)..(base + 4)) {
                    val xj = cx + (j - pos) * pitchPx
                    for (m in -5..4) {
                        val x = xj + m * pitchPx / 10f
                        if (x < -4f || x > size.width + 4f) continue
                        val major = m == 0
                        val mid = m == -5
                        val len = size.height * (if (major) 0.167f else if (mid) 0.109f else 0.065f)
                        val color = when {
                            major -> gold
                            mid -> gold.copy(alpha = 0.7f)
                            else -> Color(0xFFB9BAC8).copy(alpha = 0.45f)
                        }
                        drawLine(color, Offset(x, baseline - len), Offset(x, baseline), strokeWidth = stroke, cap = StrokeCap.Round)
                    }
                }
            }

            // İbre (karoların arkasında kalır; ortadaki karo onu örter)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val x = size.width / 2f
                drawLine(
                    brush = Brush.verticalGradient(listOf(RedoXColors.PrimaryBright, RedoXColors.PrimaryDeep)),
                    start = Offset(x, size.height * 0.036f),
                    end = Offset(x, size.height * 0.86f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                val top = size.height * 0.014f
                val tri = Path().apply {
                    moveTo(x - 6.dp.toPx(), top)
                    lineTo(x + 6.dp.toPx(), top)
                    lineTo(x, top + 8.dp.toPx())
                    close()
                }
                drawPath(tri, RedoXColors.Primary)
            }

            // Karolar ve adlar
            val roundedCenter = pos.roundToInt()
            for (j in (base - 3)..(base + 3)) {
                val d = abs(j - pos)
                val t = d.coerceAtMost(1f)
                val sizePx = mix(curPx, sidePx, t)
                val alphaV = mix(1f, 0.55f, t) * (1f - (d - 2f).coerceIn(0f, 1f))
                if (alphaV <= 0.02f) continue
                val xCenter = cx + (j - pos) * pitchPx
                val topPx = mix(hPx * 0.17f, hPx * 0.275f, t)
                val station = stations[((j % n) + n) % n]
                val isCurrent = d < 0.5f
                val sizeDp = with(density) { sizePx.toDp() }
                val tileModifier = Modifier
                    .offset { IntOffset((xCenter - sizePx / 2f).roundToInt(), topPx.roundToInt()) }
                    .alpha(alphaV)
                    .then(
                        if (isCurrent) Modifier.border(3.dp, gold, RoundedCornerShape(sizeDp * 0.27f)) else Modifier
                    )
                    .then(
                        if (onTileClick != null) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTileClick(j - roundedCenter) }
                        } else {
                            Modifier
                        }
                    )
                Box(modifier = tileModifier) {
                    ChannelBrandLogoBox(
                        channel = station,
                        boxSize = sizeDp,
                        fontSize = (sizeDp.value * 0.25f).sp
                    )
                    if (playingId != null && station.id == playingId && !isCurrent) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 6.dp, y = (-8).dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(RedoXColors.Background)
                                .padding(horizontal = 5.dp, vertical = 4.dp)
                        ) {
                            RadioEqBars(color = RedoXColors.Primary, height = 11.dp, barWidth = 2.dp)
                        }
                    }
                }
                if (showNames) {
                    val labelW = pitchPx * 0.78f
                    BasicText(
                        text = station.name,
                        style = TextStyle(
                            color = if (isCurrent) RedoXColors.TextPrimary else RedoXColors.TextTertiary,
                            fontSize = if (isCurrent) nameCurrent else nameSide,
                            fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .width(with(density) { labelW.toDp() })
                            .offset { IntOffset((xCenter - labelW / 2f).roundToInt(), (hPx * 0.885f).roundToInt()) }
                            .alpha(alphaV)
                    )
                }
            }

            // Kenarlarda solma
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(maxWidth * 0.135f)
                    .background(Brush.horizontalGradient(listOf(DialEdge, Color.Transparent)))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(maxWidth * 0.135f)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, DialEdge)))
            )
        }
    }
}

/** Ses dalgası: çalarken ~0,4 sn'de bir adım değişen çubuklar (sürekli animasyon yok); durağanken düz noktalar. */
@Composable
fun RadioSpectrum(
    active: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 60,
    barWidth: Dp = 3.dp
) {
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(active) {
        if (active) {
            while (true) {
                delay(420L)
                step++
            }
        }
    }
    val red = RedoXColors.Primary
    val gold = RedoXColors.Gold
    Canvas(modifier = modifier) {
        val bw = barWidth.toPx()
        val gap = if (barCount > 1) (size.width - bw * barCount) / (barCount - 1) else 0f
        val minH = 4.dp.toPx()
        for (i in 0 until barCount) {
            val v = if (active) spectrumValue(i, barCount, step) else 0f
            val h = max(minH, size.height * v)
            val t = if (barCount > 1) i / (barCount - 1).toFloat() else 0f
            drawRoundRect(
                color = lerpColor(red, gold, t).copy(alpha = if (active) 0.55f + 0.35f * v else 0.30f),
                topLeft = Offset(i * (bw + gap), size.height - h),
                size = Size(bw, h),
                cornerRadius = CornerRadius(bw / 2f)
            )
        }
    }
}

private fun spectrumValue(i: Int, n: Int, step: Int): Float {
    val a = sin(i * 0.55f + step * 0.9f)
    val b = cos(i * 0.21f + step * 0.37f)
    val env = 0.62f + 0.38f * sin(i.toFloat() / n * PI.toFloat())
    return (0.25f + 0.75f * abs(a * b)) * env
}

/** Çalan istasyonu belirten küçük, sabit ekolayzer çubukları. */
@Composable
fun RadioEqBars(
    modifier: Modifier = Modifier,
    color: Color = RedoXColors.Primary,
    barWidth: Dp = 3.dp,
    height: Dp = 14.dp
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(barWidth * 0.6f),
        modifier = modifier.height(height)
    ) {
        listOf(0.45f, 1f, 0.65f, 0.85f).forEach { f ->
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(height * f)
                    .clip(RoundedCornerShape(barWidth / 2))
                    .background(color)
            )
        }
    }
}

/** Kısa bilgi mesajı hapı (favori eklendi, gizlendi …). */
@Composable
fun RadioToast(message: String?, modifier: Modifier = Modifier, fontSize: TextUnit = 14.sp) {
    if (message == null) return
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(RedoXColors.SurfaceHigh)
            .border(1.dp, RedoXColors.Line, RoundedCornerShape(50))
            .padding(horizontal = 18.dp, vertical = 9.dp)
    ) {
        BasicText(
            text = message,
            style = TextStyle(color = RedoXColors.TextPrimary, fontSize = fontSize, fontWeight = FontWeight.SemiBold),
            maxLines = 1
        )
    }
}
