package com.cowslylabs.recentrecall.settings

class SettingsRepository(val store: KeyValueStore) {

    fun load(): ButtonSettings {
        val d = ButtonSettings()
        return ButtonSettings(
            enabled = store.getBoolean(K_ENABLED, d.enabled),
            tapAction = parse(store.getString(K_TAP, d.tapAction.name), d.tapAction),
            longPressAction = parse(store.getString(K_LONG, d.longPressAction.name), d.longPressAction),
            appearance = parse(store.getString(K_APPEARANCE, d.appearance.name), d.appearance),
            opacityPercent = store.getInt(K_OPACITY, d.opacityPercent)
                .coerceIn(ButtonSettings.MIN_OPACITY, ButtonSettings.MAX_OPACITY),
            sizeDp = store.getInt(K_SIZE, d.sizeDp)
                .coerceIn(ButtonSettings.MIN_SIZE_DP, ButtonSettings.MAX_SIZE_DP),
            positionLocked = store.getBoolean(K_LOCKED, d.positionLocked),
            posX = store.getFloat(K_X, d.posX).coerceIn(0f, 1f),
            posY = store.getFloat(K_Y, d.posY).coerceIn(0f, 1f),
            showSystemApps = store.getBoolean(K_SHOW_SYSTEM, d.showSystemApps),
            showHiddenApps = store.getBoolean(K_SHOW_HIDDEN, d.showHiddenApps),
            allowList = store.getStringSet(K_ALLOW),
            blockList = store.getStringSet(K_BLOCK),
        )
    }

    fun save(s: ButtonSettings) {
        store.putAll(
            mapOf(
                K_ENABLED to s.enabled,
                K_TAP to s.tapAction.name,
                K_LONG to s.longPressAction.name,
                K_APPEARANCE to s.appearance.name,
                K_OPACITY to s.opacityPercent,
                K_SIZE to s.sizeDp,
                K_LOCKED to s.positionLocked,
                K_X to s.posX,
                K_Y to s.posY,
                K_SHOW_SYSTEM to s.showSystemApps,
                K_SHOW_HIDDEN to s.showHiddenApps,
                K_ALLOW to s.allowList,
                K_BLOCK to s.blockList,
            ),
        )
    }

    /** Writes only the position so a drag never overwrites other settings. */
    fun savePosition(x: Float, y: Float) {
        store.putAll(mapOf(K_X to x, K_Y to y))
    }

    private inline fun <reified T : Enum<T>> parse(name: String, def: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: def

    companion object {
        const val K_ENABLED = "enabled"
        const val K_TAP = "tap_action"
        const val K_LONG = "long_press_action"
        const val K_APPEARANCE = "appearance"
        const val K_OPACITY = "opacity_percent"
        const val K_SIZE = "size_dp"
        const val K_LOCKED = "position_locked"
        const val K_X = "pos_x"
        const val K_Y = "pos_y"
        const val K_SHOW_SYSTEM = "show_system_apps"
        const val K_SHOW_HIDDEN = "show_hidden_apps"
        const val K_ALLOW = "allow_list"
        const val K_BLOCK = "block_list"
    }
}
