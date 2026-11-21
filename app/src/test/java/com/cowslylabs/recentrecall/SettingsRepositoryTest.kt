package com.cowslylabs.recentrecall

import com.cowslylabs.recentrecall.settings.Appearance
import com.cowslylabs.recentrecall.settings.ButtonAction
import com.cowslylabs.recentrecall.settings.ButtonSettings
import com.cowslylabs.recentrecall.settings.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SettingsRepositoryTest {
    @Test
    fun defaultsMatchDocumentedValues() {
        val s = SettingsRepository(MapStore()).load()
        assertEquals(ButtonSettings(), s)
        assertFalse(s.showSystemApps)
        assertFalse(s.showHiddenApps)
    }

    @Test
    fun roundTripSurvivesNewRepositoryOnSameStore() {
        val store = MapStore()
        val saved = ButtonSettings(
            enabled = false,
            tapAction = ButtonAction.QUICK_SETTINGS,
            longPressAction = ButtonAction.NONE,
            appearance = Appearance.DOT,
            opacityPercent = 40,
            sizeDp = 70,
            positionLocked = true,
            posX = 0.25f,
            posY = 0.75f,
            showSystemApps = true,
            showHiddenApps = true,
            allowList = setOf("a.b"),
            blockList = setOf("c.d"),
        )
        SettingsRepository(store).save(saved)
        // A fresh repository over the same store models a process restart.
        assertEquals(saved, SettingsRepository(store).load())
    }

    @Test
    fun outOfRangeValuesAreClamped() {
        val store = MapStore()
        store.map[SettingsRepository.K_OPACITY] = 5
        store.map[SettingsRepository.K_SIZE] = 500
        store.map[SettingsRepository.K_X] = 3f
        store.map[SettingsRepository.K_Y] = -1f
        val s = SettingsRepository(store).load()
        assertEquals(ButtonSettings.MIN_OPACITY, s.opacityPercent)
        assertEquals(ButtonSettings.MAX_SIZE_DP, s.sizeDp)
        assertEquals(1f, s.posX, 0f)
        assertEquals(0f, s.posY, 0f)
    }

    @Test
    fun unknownEnumNamesFallBackToDefaults() {
        val store = MapStore()
        store.map[SettingsRepository.K_TAP] = "GONE"
        assertEquals(ButtonAction.RECENTS, SettingsRepository(store).load().tapAction)
    }

    @Test
    fun savePositionDoesNotTouchOtherSettings() {
        val store = MapStore()
        val repo = SettingsRepository(store)
        repo.save(ButtonSettings(opacityPercent = 33, blockList = setOf("x")))
        repo.savePosition(0.1f, 0.2f)
        val s = repo.load()
        assertEquals(33, s.opacityPercent)
        assertEquals(setOf("x"), s.blockList)
        assertEquals(0.1f, s.posX, 0f)
        assertEquals(0.2f, s.posY, 0f)
    }

    @Test
    fun rulesForSystemAppsSurviveWhileCheckboxIsOff() {
        val repo = SettingsRepository(MapStore())
        repo.save(ButtonSettings(showSystemApps = true, blockList = setOf("com.android.settings")))
        repo.save(repo.load().copy(showSystemApps = false))
        assertEquals(setOf("com.android.settings"), repo.load().blockList)
    }
}
