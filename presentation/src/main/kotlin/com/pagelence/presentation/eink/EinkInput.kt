package com.pagelence.presentation.eink

/**
 * 墨水屏壳的按键语义。
 *
 * Android 的 `KeyEvent.keyCode` 是整数常量，映射规则本身与平台无关，所以放在
 * `:presentation` 单测；壳只负责把 `keyCode` 传进来、把动作分发到翻页或确认。
 *
 * 取舍：方向键与翻页键都按「左右＝前后页」处理，方向上下也给出翻页语义，因为
 * 墨水屏设备常只带方向键；返回键保持返回，音量键当翻页键用（不带实体翻页键的
 * 机器靠它翻页）。
 */
enum class EinkAction {
    PREVIOUS_PAGE,
    NEXT_PAGE,
    CONFIRM,
    BACK,
    NONE,
}

object EinkKeyMap {
    // Values mirror android.view.KeyEvent; kept here so tests run on the JVM.
    const val KEY_BACK = 4
    const val KEY_DPAD_UP = 19
    const val KEY_DPAD_DOWN = 20
    const val KEY_DPAD_LEFT = 21
    const val KEY_DPAD_RIGHT = 22
    const val KEY_DPAD_CENTER = 23
    const val KEY_VOLUME_UP = 24
    const val KEY_VOLUME_DOWN = 25
    const val KEY_ENTER = 66
    const val KEY_PAGE_UP = 92
    const val KEY_PAGE_DOWN = 93

    fun action(keyCode: Int): EinkAction = when (keyCode) {
        KEY_DPAD_LEFT, KEY_DPAD_UP, KEY_VOLUME_UP, KEY_PAGE_UP -> EinkAction.PREVIOUS_PAGE
        KEY_DPAD_RIGHT, KEY_DPAD_DOWN, KEY_VOLUME_DOWN, KEY_PAGE_DOWN -> EinkAction.NEXT_PAGE
        KEY_ENTER, KEY_DPAD_CENTER -> EinkAction.CONFIRM
        KEY_BACK -> EinkAction.BACK
        else -> EinkAction.NONE
    }

    /** Only page-turn actions are consumed while a text page is open. */
    fun isPageTurn(action: EinkAction): Boolean =
        action == EinkAction.PREVIOUS_PAGE || action == EinkAction.NEXT_PAGE
}
