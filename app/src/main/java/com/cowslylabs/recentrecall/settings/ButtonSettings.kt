package com.cowslylabs.recentrecall.settings

enum class ButtonAction { RECENTS, APP_SETTINGS, QUICK_SETTINGS, NOTIFICATIONS, NONE }

enum class Appearance { ICON_DETAILED, ICON_SIMPLE, DOT }

data class ButtonSettings(
    val enabled: Boolean = true,
    val tapAction: ButtonAction = ButtonAction.RECENTS,
    val longPressAction: ButtonAction = ButtonAction.APP_SETTINGS,
    val appearance: Appearance = Appearance.ICON_DETAILED,
    val opacityPercent: Int = 85,
    val sizeDp: Int = 52,
    val positionLocked: Boolean = false,
    /** Horizontal position as a fraction (0..1) of the free screen width. */
    val posX: Float = 1f,
    /** Vertical position as a fraction (0..1) of the free screen height. */
    val posY: Float = 0.6f,
    val showSystemApps: Boolean = false,
    val showHiddenApps: Boolean = false,
    val allowList: Set<String> = emptySet(),
    val blockList: Set<String> = emptySet(),
) {
    companion object {
        const val MIN_OPACITY = 20
        const val MAX_OPACITY = 100
        const val MIN_SIZE_DP = 32
        const val MAX_SIZE_DP = 96
    }
}
