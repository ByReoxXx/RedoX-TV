package com.example.chromecasttv

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================================
// RedoX logosu: kırmızı faset (low-poly) boğa başı + altın halka, parlayan altın gözler, altın burun halkası,
// kırmızı italik "RedoX" + altın "TV". Ad = Red + Ox ("kırmızı boğa").
// Logo her yerde "RedoX TV" der; uygulamanın adı (başlatıcı etiketi) "RedoX TV 2"dir.
// Görseller kullanıcının logosundan üretilmiştir (zemin saydamdır, koyu zeminlerde kenar izi bırakmaz):
// redox_mark (512), redox_mark_small (192), redox_wordmark (960 px genişlik).
// ============================================================================

/** Zemin (karo) olmadan logo: boğa başı + altın halka. Küçük boyutlarda daha keskin olan 192 px'lik kopya kullanılır. */
@Composable
fun RedoXMark(
    markSize: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val resId = if (markSize <= 72.dp) R.drawable.redox_mark_small else R.drawable.redox_mark
    Image(
        painter = painterResource(resId),
        contentDescription = "RedoX",
        contentScale = ContentScale.Fit,
        modifier = modifier.size(markSize)
    )
}

/** Logodaki lacivert-siyah karo üzerinde logo (uygulama içi amblem; sol üst köşe, kenar çubuğu, vb.). */
@Composable
fun RedoXEmblem(
    boxSize: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(boxSize * 0.26f)
    Box(
        modifier = modifier
            .size(boxSize)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF12131F), Color(0xFF04040C))))
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape),
        contentAlignment = Alignment.Center
    ) {
        RedoXMark(markSize = boxSize * 0.92f)
    }
}

/** Logodaki "RedoX TV" yazısı (resim): kırmızı italik "RedoX" + altın "TV", parıltılı. */
@Composable
fun RedoXWordmarkImage(
    width: Dp = 220.dp,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(R.drawable.redox_wordmark),
        contentDescription = "RedoX TV",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .width(width)
            .aspectRatio(960f / 204f)
    )
}

/** Yazı olarak: "RedoX" kırmızı italik, "TV" altın — ölçeklenebilir, çok küçük alanlar için. */
@Composable
fun RedoXWordmark(
    fontSize: TextUnit = 22.sp,
    modifier: Modifier = Modifier,
    showTv: Boolean = true
) {
    val red = RedoXColors.Primary
    val gold = RedoXColors.Gold
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = red)) { append("RedoX") }
        if (showTv) {
            withStyle(SpanStyle(color = gold)) { append(" TV") }
        }
    }
    BasicText(
        text = text,
        style = TextStyle(
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            fontStyle = FontStyle.Italic,
            letterSpacing = (-0.5).sp
        ),
        modifier = modifier
    )
}
