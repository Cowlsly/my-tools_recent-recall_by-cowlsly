package com.cowslylabs.recentrecall

import com.cowslylabs.recentrecall.logic.ForegroundTracker
import com.cowslylabs.recentrecall.logic.PackageRules
import com.cowslylabs.recentrecall.logic.PickerFilter
import com.cowslylabs.recentrecall.logic.RuleState
import com.cowslylabs.recentrecall.logic.VisibilityRules
import com.cowslylabs.recentrecall.settings.ButtonSettings
import com.cowslylabs.recentrecall.settings.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Covers clamping, the sensitive-screen filter, filter precedence and settings persistence. */
class NewBehaviourTest {

    // ---- Clamping when size or opacity is changed and saved ----

    @Test
    fun sizeBelowMinimumIsClampedToMinimumAfterSaveAndReload() {
        val repo = SettingsRepository(MapStore())
        repo.save(ButtonSettings(sizeDp = 1))
        assertEquals(ButtonSettings.MIN_SIZE_DP, repo.load().sizeDp)
    }

    @Test
    fun sizeAboveMaximumIsClampedToMaximumAfterSaveAndReload() {
        val repo = SettingsRepository(MapStore())
        repo.save(ButtonSettings(sizeDp = 9999))
        assertEquals(ButtonSettings.MAX_SIZE_DP, repo.load().sizeDp)
    }

    @Test
    fun sizeBoundariesAreKeptExactly() {
        val repo = SettingsRepository(MapStore())
        repo.save(ButtonSettings(sizeDp = ButtonSettings.MIN_SIZE_DP))
        assertEquals(ButtonSettings.MIN_SIZE_DP, repo.load().sizeDp)
        repo.save(ButtonSettings(sizeDp = ButtonSettings.MAX_SIZE_DP))
        assertEquals(ButtonSettings.MAX_SIZE_DP, repo.load().sizeDp)
    }

    @Test
    fun opacityIsClampedBothWays() {
        val repo = SettingsRepository(MapStore())
        repo.save(ButtonSettings(opacityPercent = 0))
        assertEquals(ButtonSettings.MIN_OPACITY, repo.load().opacityPercent)
        repo.save(ButtonSettings(opacityPercent = 500))
        assertEquals(ButtonSettings.MAX_OPACITY, repo.load().opacityPercent)
    }

    @Test
    fun sizeChangeKeepsPositionFractionsInRange() {
        val repo = SettingsRepository(MapStore())
        repo.save(ButtonSettings(sizeDp = 96, posX = 2f, posY = -3f))
        val s = repo.load()
        assertEquals(1f, s.posX, 0f)
        assertEquals(0f, s.posY, 0f)
    }

    // ---- Sensitive-screen filter ----

    @Test
    fun knownSensitivePackagesAreSensitive() {
        assertTrue(PackageRules.isSensitive("com.android.permissioncontroller"))
        assertTrue(PackageRules.isSensitive("com.android.packageinstaller"))
        assertTrue(PackageRules.isSensitive("com.bitwarden.authenticator"))
        assertTrue(PackageRules.isSensitive("com.example.mybank.app"))
    }

    @Test
    fun sensitiveMatchIgnoresCase() {
        assertTrue(PackageRules.isSensitive("COM.ANDROID.PACKAGEINSTALLER"))
    }

    @Test
    fun ordinaryPackagesAreNotSensitive() {
        assertFalse(PackageRules.isSensitive("com.example.notes"))
        assertFalse(PackageRules.isSensitive("com.android.chrome"))
    }

    @Test
    fun settingsAppIsNotSensitiveByNameBecauseOnlyPackageNamesAreSeen() {
        // Known gap: a password page inside Settings cannot be told from any other Settings page.
        assertFalse(PackageRules.isSensitive("com.android.settings"))
    }

    @Test
    fun nullPackageAndDisabledToggleNeverHide() {
        assertFalse(PackageRules.isSensitive(null))
        assertFalse(PackageRules.isSensitive("com.android.permissioncontroller", enabled = false))
    }

    @Test
    fun helperWindowsAreIgnoredByTheTracker() {
        assertTrue(PackageRules.isIgnored("com.android.systemui"))
        assertTrue(PackageRules.isIgnored("com.google.android.inputmethod.latin"))
        assertTrue(PackageRules.isIgnored("com.vendor.honeyboard.extra"))
        assertFalse(PackageRules.isIgnored("com.example.notes"))
    }

    @Test
    fun trackerKeepsLastRealAppWhenHelperWindowAppears() {
        val t = ForegroundTracker()
        assertTrue(t.onWindowState("com.example.notes"))
        assertFalse(t.onWindowState("com.android.systemui"))
        assertFalse(t.onWindowState(null))
        assertFalse(t.onWindowState(""))
        assertEquals("com.example.notes", t.current)
        t.reset()
        assertEquals(null, t.current)
    }

    // ---- Filter precedence ----

    @Test
    fun blocklistBeatsAllowlist() {
        val both = setOf("a.b")
        assertFalse(VisibilityRules.shouldShow("a.b", both, both))
    }

    @Test
    fun emptyAllowlistShowsOverEverythingNotBlocked() {
        assertTrue(VisibilityRules.shouldShow("any.app", emptySet(), setOf("x.y")))
        assertFalse(VisibilityRules.shouldShow("x.y", emptySet(), setOf("x.y")))
    }

    @Test
    fun nonEmptyAllowlistShowsOnlyAllowedApps() {
        val allow = setOf("a.b")
        assertTrue(VisibilityRules.shouldShow("a.b", allow, emptySet()))
        assertFalse(VisibilityRules.shouldShow("c.d", allow, emptySet()))
    }

    @Test
    fun unknownForegroundOnlyShowsWhenAllowlistIsEmpty() {
        assertTrue(VisibilityRules.shouldShow(null, emptySet(), setOf("x")))
        assertFalse(VisibilityRules.shouldShow(null, setOf("a"), emptySet()))
    }

    @Test
    fun applyRulePutsAppInExactlyOneList() {
        val (a1, b1) = VisibilityRules.applyRule("p", RuleState.ALLOW, emptySet(), setOf("p"))
        assertEquals(setOf("p"), a1)
        assertEquals(emptySet<String>(), b1)
        val (a2, b2) = VisibilityRules.applyRule("p", RuleState.BLOCK, a1, b1)
        assertEquals(emptySet<String>(), a2)
        assertEquals(setOf("p"), b2)
        val (a3, b3) = VisibilityRules.applyRule("p", RuleState.NONE, a2, b2)
        assertTrue(a3.isEmpty() && b3.isEmpty())
    }

    @Test
    fun ruleCyclesNoneAllowBlockNone() {
        assertEquals(RuleState.ALLOW, VisibilityRules.nextState(RuleState.NONE))
        assertEquals(RuleState.BLOCK, VisibilityRules.nextState(RuleState.ALLOW))
        assertEquals(RuleState.NONE, VisibilityRules.nextState(RuleState.BLOCK))
    }

    @Test
    fun pickerFilterTruthTable() {
        // normal app
        assertTrue(PickerFilter.include(false, false, false, false))
        // system app
        assertFalse(PickerFilter.include(true, false, false, false))
        assertTrue(PickerFilter.include(true, false, true, false))
        // hidden app
        assertFalse(PickerFilter.include(false, true, false, false))
        assertTrue(PickerFilter.include(false, true, false, true))
        // system and hidden needs both
        assertFalse(PickerFilter.include(true, true, true, false))
        assertFalse(PickerFilter.include(true, true, false, true))
        assertTrue(PickerFilter.include(true, true, true, true))
    }

    // ---- Settings persistence ----

    @Test
    fun systemAndHiddenFlagsPersistIndependently() {
        val store = MapStore()
        SettingsRepository(store).save(ButtonSettings(showSystemApps = true))
        var s = SettingsRepository(store).load()
        assertTrue(s.showSystemApps)
        assertFalse(s.showHiddenApps)

        SettingsRepository(store).save(s.copy(showSystemApps = false, showHiddenApps = true))
        s = SettingsRepository(store).load()
        assertFalse(s.showSystemApps)
        assertTrue(s.showHiddenApps)
    }

    @Test
    fun hideOnSensitiveOffPersistsAndDefaultsOn() {
        val store = MapStore()
        assertTrue(SettingsRepository(store).load().hideOnSensitive)
        SettingsRepository(store).save(ButtonSettings(hideOnSensitive = false))
        assertFalse(SettingsRepository(store).load().hideOnSensitive)
    }

    @Test
    fun listsPersistAcrossRepositoryInstances() {
        val store = MapStore()
        SettingsRepository(store).save(ButtonSettings(allowList = setOf("a", "b"), blockList = setOf("c")))
        val s = SettingsRepository(store).load()
        assertEquals(setOf("a", "b"), s.allowList)
        assertEquals(setOf("c"), s.blockList)
    }
}
