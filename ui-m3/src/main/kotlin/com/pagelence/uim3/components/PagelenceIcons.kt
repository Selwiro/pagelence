package com.pagelence.uim3.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * 关于页与设置入口用的描边图标。
 *
 * 与应用其余界面保持一致：24dp 视口、1.65dp 描边、圆头圆角，颜色交给 `Icon`
 * 的 tint。这里只放关于页需要的几个，不引入 material-icons-extended。
 */
object PagelenceIcons {

    /** 检查更新（刷新）。 */
    val Refresh: ImageVector by lazy {
        strokeIcon("Pagelence.Refresh", "M20 12a8 8 0 1 1-2.34-5.66M20 4v4h-4")
    }

    /** 官网（地球）。 */
    val Globe: ImageVector by lazy {
        strokeIcon(
            "Pagelence.Globe",
            "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z",
            "M3 12h18",
            "M12 3c2.5 2.5 3.5 6 3.5 9s-1 6.5-3.5 9c-2.5-2.5-3.5-6-3.5-9s1-6.5 3.5-9z",
        )
    }

    /** GitHub 仓库（代码）。 */
    val Code: ImageVector by lazy {
        strokeIcon("Pagelence.Code", "m8 7-5 5 5 5M16 7l5 5-5 5M14 4l-4 16")
    }

    /** 隐私与数据（锁）。 */
    val Lock: ImageVector by lazy {
        strokeIcon("Pagelence.Lock", "M6 10h12v11H6zM9 10V7a3 3 0 0 1 6 0v3")
    }

    /** 开源许可（信息）。 */
    val Info: ImageVector by lazy {
        strokeIcon(
            "Pagelence.Info",
            "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z",
            "M12 11v5M12 8h.01",
        )
    }

    /** 进入子页。 */
    val ChevronRight: ImageVector by lazy {
        strokeIcon("Pagelence.ChevronRight", "m9 5 7 7-7 7", autoMirror = true)
    }
}

private const val ICON_STROKE_WIDTH = 1.65f

private fun strokeIcon(
    name: String,
    vararg pathData: String,
    autoMirror: Boolean = false,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
    autoMirror = autoMirror,
).apply {
    pathData.forEach { path ->
        addPath(
            pathData = addPathNodes(path),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = ICON_STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
}.build()
