package com.pagelence.uieink.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 墨水屏调色板：纯白纸面 + 黑字 + 少量灰阶。
 *
 * **禁止黑底**（2026-11-22 用户明确要求）：任何面——按钮、表头、选中态、提示条——
 * 都保持白底黑字，强调只用「更粗的描边 + 更重的字重」，不用反色块。
 * 不提供深色模式；不用阴影；选中态不用浅色块，避免 16 级灰糊成一片。
 */
@Immutable
data class InkColors(
    val paper: Color = Color(0xFFFFFFFF),
    val ink: Color = Color(0xFF111111),
    val inkSecondary: Color = Color(0xFF3B3B3B),
    val inkMuted: Color = Color(0xFF6B6B6B),
    /** 1px 描边；比正文浅但比留白重，墨水屏上仍可辨。 */
    val rule: Color = Color(0xFF9C9C9C),
    /** 区块底色；只用在中性提示条与选中词上。 */
    val fill: Color = Color(0xFFECECEC),
    /** 划线标记的底色；比 [fill] 深一点，和「选中词」区分开。 */
    val markFill: Color = Color(0xFFD6D6D6),
) {
    companion object {
        val Light = InkColors()
    }
}

/**
 * 字号与行高。正文衬线 ≥18sp、行高 ≥1.6；界面文字无衬线加粗用于层级，
 * 不做小字号密排（墨水屏 14sp 以下容易发虚）。
 */
@Immutable
data class InkTypography(
    val display: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 24.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
    ),
    val title: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
    ),
    val ui: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    val uiStrong: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Bold,
    ),
    val caption: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    /** 正文：衬线 19sp / 行高 33sp，折合约 1.74 倍。 */
    val body: TextStyle = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 19.sp,
        lineHeight = 33.sp,
    ),
    /** 句译与注释：比正文小一号，仍 ≥16sp。 */
    val bodySmall: TextStyle = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 17.sp,
        lineHeight = 28.sp,
    ),
) {
    companion object {
        val Default = InkTypography()
    }
}

val LocalInkColors = staticCompositionLocalOf { InkColors.Light }
val LocalInkTypography = staticCompositionLocalOf { InkTypography.Default }

/** 主题只做取值与白底；不做转场、不做动画。 */
@Composable
fun InkTheme(content: @Composable () -> Unit) {
    val colors = InkColors.Light
    CompositionLocalProvider(
        LocalInkColors provides colors,
        LocalInkTypography provides InkTypography.Default,
    ) {
        Box(Modifier.fillMaxSize().background(colors.paper)) { content() }
    }
}
