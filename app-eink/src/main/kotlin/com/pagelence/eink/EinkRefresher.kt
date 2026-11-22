package com.pagelence.eink

/**
 * 整页刷新（GC16）的接入点。
 *
 * 墨水屏的「全刷」不是 Android 标准 API：ONYX/Boox、汉王、掌阅等各自有厂商 SDK
 * （例如 ONYX 的 `EpdController.refreshScreen(UpdateMode.GC)`）。`:ui-eink` 只决定
 * 「什么时候该刷」（`EinkRefreshRules`），真正调用放在这里，避免把厂商依赖带进
 * 共享层。接真机时替换 [NoopEinkRefresher] 即可，其余代码不动。
 */
interface EinkRefresher {
    fun fullRefresh()
}

/** 没有厂商 SDK 时的实现：不做事，保证调用点存在。 */
object NoopEinkRefresher : EinkRefresher {
    override fun fullRefresh() = Unit
}
