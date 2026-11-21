package com.pagelence.uim3.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import android.graphics.BitmapFactory
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * 默认书籍封面：没有自带封面图时，用书名、作者和一套按书固定的配色/装饰排版。
 *
 * 同一本书的 `seed`（book id）始终得到同一配色，所以书架和书城看起来是同一本；
 * 书名长度决定字号档位，英文长书名最多四行，作者最多两行。封面上的字号用 dp 换算
 * 成 sp，跟随系统字体缩放时不撑破版面；可读性由卡片上方的书名与作者文字承担。
 */
private data class CoverPalette(
    val background: Color,
    val foreground: Color,
    val accent: Color,
)

private val CoverPalettes = listOf(
    CoverPalette(Color(0xFF2D4658), Color(0xFFF2F6F8), Color(0xFFA9C7DE)),
    CoverPalette(Color(0xFF36474E), Color(0xFFF2F5F4), Color(0xFFBFD1CC)),
    CoverPalette(Color(0xFF453B4C), Color(0xFFF4F0F4), Color(0xFFD3C4D8)),
    CoverPalette(Color(0xFF4B3D3B), Color(0xFFF5F0EC), Color(0xFFD6BEB2)),
    CoverPalette(Color(0xFF3D4A3E), Color(0xFFF1F4EE), Color(0xFFC6D3BF)),
)

/** Curated palette per built-in title; unknown/downloaded books fall back to a stable hash. */
private val KnownCoverPalettes = mapOf(
    "great-gatsby-1925" to 0,
    "sign-of-four-1890" to 1,
    "alice-1866" to 2,
    "scarlet-letter-1850" to 3,
    "doctor-dolittle-1920" to 4,
)

/** Load a package `cover.jpg` once; null or unreadable files fall back to the typographic cover. */
@Composable
fun rememberCoverPainter(path: String?): Painter? = remember(path) {
    path?.let { file ->
        runCatching { BitmapFactory.decodeFile(file)?.asImageBitmap()?.let(::BitmapPainter) }.getOrNull()
    }
}

@Composable
fun BookCover(
    title: String,
    author: String?,
    seed: String,
    modifier: Modifier = Modifier,
    monogram: String? = null,
    /** Book-supplied cover art; null falls back to the typographic cover. */
    cover: Painter? = null,
) {
    val hash = seed.hashCode() and 0x7fffffff
    val paletteIndex = KnownCoverPalettes[seed] ?: (hash % CoverPalettes.size)
    val palette = CoverPalettes[paletteIndex]
    val shape = RoundedCornerShape(6.dp)
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(0.70f)
            .clip(shape)
            .background(palette.background)
            .border(0.5.dp, palette.foreground.copy(alpha = 0.22f), shape)
            .clearAndSetSemantics {
                contentDescription = listOfNotNull(title, author).joinToString(", ")
            },
    ) {
        if (cover != null) {
            Image(
                painter = cover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            return@BoxWithConstraints
        }
        val compact = maxWidth < 72.dp
        val ornament = hash / CoverPalettes.size % 3
        CoverOrnament(
            ornament = ornament,
            accent = palette.accent,
            compact = compact,
        )
        val padding = if (compact) 7.dp else 9.dp
        val innerWidth = maxWidth - padding * 2
        val baseTitleSize = when {
            title.length <= 6 -> if (compact) 11.dp else 13.dp
            title.length <= 12 -> if (compact) 9.dp else 11.dp
            else -> if (compact) 6.5.dp else 7.2.dp
        }
        // Short titles should stay on one line; shrink only enough to fit, never
        // below the long-title size. Longer titles are allowed to wrap.
        val titleSize = if (title.length <= 7 && title.isNotEmpty()) {
            minOf(baseTitleSize, innerWidth / title.length - 0.5.dp)
        } else {
            baseTitleSize
        }
        val authorSize = when {
            author == null -> if (compact) 6.5.dp else 7.5.dp
            compact && author.length > 16 -> 5.5.dp
            compact -> 6.5.dp
            author.length > 20 -> 6.5.dp
            else -> 7.5.dp
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "PAGELENCE",
                color = palette.foreground.copy(alpha = 0.55f),
                fontSize = with(density) { (if (compact) 4.dp else 4.5.dp).toSp() },
                letterSpacing = with(density) { 0.8.dp.toSp() },
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Column {
                Text(
                    text = title,
                    color = palette.foreground,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = with(density) { titleSize.toSp() },
                    lineHeight = with(density) { (titleSize * 1.18f).toSp() },
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(if (compact) 5.dp else 7.dp))
                Spacer(
                    Modifier
                        .width(if (compact) 14.dp else 18.dp)
                        .height(1.dp)
                        .background(palette.accent),
                )
                author?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(if (compact) 3.dp else 4.dp))
                    Text(
                        text = it,
                        color = palette.foreground.copy(alpha = 0.82f),
                        fontSize = with(density) { authorSize.toSp() },
                        lineHeight = with(density) { (authorSize * 1.15f).toSp() },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        // A tiny edition mark: enough to tell two same-palette books apart
        // without covering the title.
        Text(
            text = monogram?.takeIf { it.isNotBlank() } ?: title.take(1).ifBlank { "·" },
            color = palette.foreground.copy(alpha = 0.55f),
            fontFamily = FontFamily.Serif,
            fontSize = with(density) { (if (compact) 7.dp else 8.dp).toSp() },
            maxLines = 1,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = padding - 1.dp, end = padding),
        )
    }
}

/** A small geometric mark so two books with the same palette still differ. */
@Composable
private fun CoverOrnament(ornament: Int, accent: Color, compact: Boolean) {
    Canvas(Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        when (ornament) {
            0 -> {
                drawCircle(
                    color = accent.copy(alpha = 0.30f),
                    radius = width * 0.34f,
                    center = Offset(width * 0.92f, height * 0.13f),
                    style = Stroke(width = 0.7.dp.toPx()),
                )
                drawCircle(
                    color = accent.copy(alpha = 0.18f),
                    radius = width * 0.05f,
                    center = Offset(width * 0.14f, height * 0.86f),
                )
            }
            1 -> {
                drawLine(
                    color = accent.copy(alpha = 0.30f),
                    start = Offset(width * 0.12f, height * 0.04f),
                    end = Offset(width * 0.12f, height * 0.42f),
                    strokeWidth = 0.7.dp.toPx(),
                )
                drawLine(
                    color = accent.copy(alpha = 0.18f),
                    start = Offset(width * 0.86f, height * 0.55f),
                    end = Offset(width * 0.86f, height * 0.96f),
                    strokeWidth = 0.7.dp.toPx(),
                )
            }
            else -> {
                drawCircle(
                    color = accent.copy(alpha = 0.22f),
                    radius = width * 0.42f,
                    center = Offset(width * 0.12f, height * 1.02f),
                    style = Stroke(width = 0.7.dp.toPx()),
                )
                if (!compact) {
                    drawCircle(
                        color = accent.copy(alpha = 0.16f),
                        radius = width * 0.06f,
                        center = Offset(width * 0.84f, height * 0.16f),
                    )
                }
            }
        }
    }
}
