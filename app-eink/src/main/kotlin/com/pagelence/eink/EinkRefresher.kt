package com.pagelence.eink

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/** 可用的刷新后端；设置页会显示它，便于真机核对到底走了哪条路。 */
enum class RefreshBackend {
    MOAAN_BROADCAST,
    NONE,
}

/**
 * 整页刷新（GC16）的接入点。
 *
 * 墨水屏的「全刷」不是 Android 标准 API，各厂商实现不同：
 * - 墨案（MOAAN / inkPalm / inkPad）系统监听 `android.eink.force.refresh` 广播，
 *   收到后由固件驱动执行 EINK_GC16_MODE 整屏刷新（Mini 阅 / inkPalm 5 上由
 *   KOReader 补丁实机验证；其他墨案机型未验证）。
 * - 文石（ONYX/Boox）走 `onyxsdk-base` 的 EpdController，需要额外依赖。
 * - 汉王、掌阅等又有各自的系统服务。
 *
 * `:ui-eink` 只决定「什么时候该刷」（`EinkRefreshRules`），真正调用放在这里，
 * 避免把厂商依赖带进共享层。
 */
interface EinkRefresher {
    val backend: RefreshBackend
    fun fullRefresh(): Boolean
}

/**
 * 墨案全刷：向系统发一条广播，不需要 SDK、不需要 root。
 *
 * 依据：墨案 Mini 阅 / inkPalm 5（Android 8.1）上，第三方阅读器用
 * `sendBroadcast("android.eink.force.refresh")` 触发了真正的 GC16 全屏刷新
 * （见 docs/app-eink.md 的「墨案全刷」一节）。广播没有接收方时是无害的，
 * 所以非墨案设备上发送也只会被忽略；返回 false 表示系统没有确认。
 */
class MoaanBroadcastRefresher(private val context: Context) : EinkRefresher {
    override val backend = RefreshBackend.MOAAN_BROADCAST

    override fun fullRefresh(): Boolean = runCatching {
        context.sendBroadcast(Intent(ACTION))
        Log.i(TAG, "已发送 $ACTION")
        true
    }.getOrElse { error ->
        Log.w(TAG, "全刷广播发送失败：${error.message}")
        false
    }

    companion object {
        const val ACTION = "android.eink.force.refresh"

        /**
         * 设备是否像墨案：`Build` 里出现 MOAAN / inkPalm / inkPad / Pantone / MIX7。
         * 小米生态链机型有时 manufacturer 报 Xiaomi，所以型号串也一起看。
         */
        fun looksLikeMoaan(): Boolean {
            val id = listOf(Build.MANUFACTURER, Build.BRAND, Build.MODEL, Build.DEVICE)
                .filterNotNull()
                .joinToString(" ")
                .lowercase()
            return HINTS.any { it in id }
        }

        private val HINTS = listOf("moaan", "inkpalm", "inkpad", "pantone", "mix7")
        private const val TAG = "EinkRefresh"
    }
}

/** 没有厂商刷新通道时的实现：不做事，保证调用点存在。 */
object NoopEinkRefresher : EinkRefresher {
    override val backend = RefreshBackend.NONE
    override fun fullRefresh(): Boolean = false
}

/**
 * 选一个后端。
 *
 * [forceMoaanBroadcast] 来自设置页的「强制发送墨案全刷广播（实验）」：有些墨案
 * 机型 `Build` 串看不出品牌，或者用户换过壳；打开后无论机型都发广播。
 */
object EinkRefreshers {
    fun create(context: Context, forceMoaanBroadcast: Boolean = false): EinkRefresher =
        if (forceMoaanBroadcast || MoaanBroadcastRefresher.looksLikeMoaan()) {
            MoaanBroadcastRefresher(context)
        } else {
            NoopEinkRefresher
        }
}
