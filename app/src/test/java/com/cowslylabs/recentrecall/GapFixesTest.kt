package com.cowslylabs.recentrecall

import com.cowslylabs.recentrecall.logic.ForegroundTracker
import com.cowslylabs.recentrecall.logic.Geometry
import com.cowslylabs.recentrecall.logic.PackageRules
import com.cowslylabs.recentrecall.settings.ButtonSettings
import com.cowslylabs.recentrecall.settings.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GapFixesTest {
    @Test
    fun sensitiveListCoversPromptsPasswordManagersAndBanks() {
        assertTrue(PackageRules.isSensitive("com.google.android.permissioncontroller"))
        assertTrue(PackageRules.isSensitive("com.samsung.android.packageinstaller"))
        assertTrue(PackageRules.isSensitive("com.samsung.android.biometrics.app.setting"))
        assertTrue(PackageRules.isSensitive("com.x8bit.bitwarden"))
        assertTrue(PackageRules.isSensitive("com.example.passwordvault"))
        assertTrue(PackageRules.isSensitive("com.commbank.netbank"))
        assertTrue(PackageRules.isSensitive("au.com.example.mobilebanking"))
        assertFalse(PackageRules.isSensitive("com.example.notes"))
        assertFalse(PackageRules.isSensitive(null))
    }

    @Test
    fun toggleOffNeverHides() {
        assertFalse(PackageRules.isSensitive("com.commbank.netbank", false))
    }

    @Test
    fun toggleDefaultsOnAndPersists() {
        val store = MapStore()
        assertTrue(SettingsRepository(store).load().hideOnSensitive)
        SettingsRepository(store).save(ButtonSettings(hideOnSensitive = false))
        assertFalse(SettingsRepository(store).load().hideOnSensitive)
    }

    @Test
    fun trackerIgnoresKeyboardsAndEmptyNames() {
        val t = ForegroundTracker()
        assertTrue(t.onWindowState("com.example.app"))
        assertFalse(t.onWindowState("com.samsung.android.honeyboard"))
        assertFalse(t.onWindowState("com.android.systemui"))
        assertFalse(t.onWindowState(null))
        assertFalse(t.onWindowState(""))
        assertFalse(t.onWindowState("com.example.app"))
        assertEquals("com.example.app", t.current)
        assertTrue(t.onWindowState("com.other"))
        t.reset()
        assertNull(t.current)
    }

    @Test
    fun regionClampKeepsButtonInsideBarsAfterRotation() {
        // Portrait 1080x2340 with 80px status bar and 120px nav bar, button 150px.
        val size = 150
        val px = Geometry.fractionToPxInRegion(1f, 0, 1080, size)
        val py = Geometry.fractionToPxInRegion(1f, 80, 2340 - 120, size)
        assertEquals(930, px)
        assertEquals(80 + (2340 - 120 - 80 - size), py)
        // Rotate to landscape 2340x1080 with a 0 top inset and 120 bottom: same fractions stay inside.
        val lx = Geometry.fractionToPxInRegion(1f, 0, 2340, size)
        val ly = Geometry.fractionToPxInRegion(1f, 0, 1080 - 120, size)
        assertTrue(lx + size <= 2340)
        assertTrue(ly + size <= 1080 - 120)
        assertEquals(0, Geometry.clampInRegion(-50, 0, 1000, size))
        assertEquals(850, Geometry.clampInRegion(5000, 0, 1000, size))
    }

    @Test
    fun regionSmallerThanButtonPinsToStart() {
        assertEquals(10, Geometry.clampInRegion(500, 10, 100, 150))
        assertEquals(0f, Geometry.pxToFractionInRegion(10, 10, 100, 150), 0f)
    }

    @Test
    fun regionFractionRoundTrips() {
        val px = Geometry.fractionToPxInRegion(0.5f, 80, 2220, 150)
        assertEquals(0.5f, Geometry.pxToFractionInRegion(px, 80, 2220, 150), 0.01f)
    }
}
