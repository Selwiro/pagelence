package com.pagelence.presentation.eink

/**
 * 「刷新强度」设置。墨水屏全刷（GC16）清晰但会闪，局部刷新（DU/A2）快但累积
 * 残影；这里只给策略，真正调用厂商刷新接口的是壳。
 *
 * 默认：换单元必全刷，连续翻 [fullRefreshEveryPages] 页全刷一次，距上次全刷超过
 * [fullRefreshEverySeconds] 秒也全刷一次。两个阈值填 0 表示关闭该项。
 */
data class EinkRefreshSettings(
    val fullRefreshOnUnitChange: Boolean = true,
    val fullRefreshEveryPages: Int = 12,
    val fullRefreshEverySeconds: Int = 90,
) {
    companion object {
        val Default = EinkRefreshSettings()
    }
}

/** 距上次全刷翻了多少页、以及上次全刷的时刻（毫秒，壳传入单调时钟）。 */
data class EinkRefreshState(
    val pagesSinceFullRefresh: Int = 0,
    val lastFullRefreshAtMillis: Long = 0L,
) {
    companion object {
        /** 打开一个单元时的基线；避免第一次翻页就因「从未刷新」而全刷。 */
        fun opened(nowMillis: Long): EinkRefreshState =
            EinkRefreshState(pagesSinceFullRefresh = 0, lastFullRefreshAtMillis = nowMillis)
    }
}

data class EinkRefreshDecision(
    val fullRefresh: Boolean,
    val state: EinkRefreshState,
)

object EinkRefreshRules {
    fun onPageTurn(
        state: EinkRefreshState,
        settings: EinkRefreshSettings,
        nowMillis: Long,
    ): EinkRefreshDecision {
        val pages = state.pagesSinceFullRefresh + 1
        val byPages = settings.fullRefreshEveryPages > 0 && pages >= settings.fullRefreshEveryPages
        val byTime = settings.fullRefreshEverySeconds > 0 &&
            nowMillis - state.lastFullRefreshAtMillis >= settings.fullRefreshEverySeconds * 1000L
        return decide(byPages || byTime, pages, state, nowMillis)
    }

    fun onUnitChange(
        state: EinkRefreshState,
        settings: EinkRefreshSettings,
        nowMillis: Long,
    ): EinkRefreshDecision =
        decide(settings.fullRefreshOnUnitChange, state.pagesSinceFullRefresh + 1, state, nowMillis)

    private fun decide(
        refresh: Boolean,
        pages: Int,
        state: EinkRefreshState,
        nowMillis: Long,
    ): EinkRefreshDecision = if (refresh) {
        EinkRefreshDecision(
            fullRefresh = true,
            state = EinkRefreshState(pagesSinceFullRefresh = 0, lastFullRefreshAtMillis = nowMillis),
        )
    } else {
        EinkRefreshDecision(
            fullRefresh = false,
            state = state.copy(pagesSinceFullRefresh = pages),
        )
    }
}
