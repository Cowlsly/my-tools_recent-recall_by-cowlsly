package com.cowslylabs.recentrecall

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Emulator smoke test for the settings screen.
 * It proves the screen builds and persists on a stock emulator image.
 * It does not prove anything about the Samsung Galaxy A17 5G, the real accessibility
 * service, the floating button or Recents.
 */
class SmokeTest {

    private val prefsName = "recent_recall_settings"

    @Before
    fun clearSettings() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        ctx.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun allViews(root: View, out: MutableList<View> = mutableListOf()): List<View> {
        out.add(root)
        if (root is ViewGroup) for (i in 0 until root.childCount) allViews(root.getChildAt(i), out)
        return out
    }

    private fun ActivityScenario<MainActivity>.texts(): List<String> {
        var r = emptyList<String>()
        onActivity { a ->
            r = allViews(a.window.decorView).filterIsInstance<TextView>().map { it.text.toString() }
        }
        return r
    }

    private fun ActivityScenario<MainActivity>.checkbox(label: String): Boolean {
        var checked: Boolean? = null
        onActivity { a ->
            checked = allViews(a.window.decorView).filterIsInstance<CheckBox>()
                .firstOrNull { it.text.toString() == label }?.isChecked
        }
        assertNotNull("Checkbox not found: $label", checked)
        return checked!!
    }

    private fun ActivityScenario<MainActivity>.click(label: String) {
        onActivity { a ->
            val cb = allViews(a.window.decorView).filterIsInstance<CheckBox>()
                .first { it.text.toString() == label }
            cb.performClick()
        }
    }

    @Test
    fun appLaunchesAndSettingsScreenRenders() {
        ActivityScenario.launch(MainActivity::class.java).use { s ->
            val t = s.texts()
            assertTrue(t.contains("Recent-Recall"))
            assertTrue(t.contains("Permission status"))
            assertTrue(t.contains("Button"))
            assertTrue(t.contains("Privacy"))
            assertTrue(t.contains("Which apps show the button"))
        }
    }

    @Test
    fun permissionStatusShowsServiceOffWhenNotEnabled() {
        ActivityScenario.launch(MainActivity::class.java).use { s ->
            val status = s.texts().first { it.startsWith("Accessibility service:") }
            assertTrue(status, status.startsWith("Accessibility service: Off"))
            assertTrue(status.contains("Display over other apps: not needed"))
        }
    }

    @Test
    fun systemAndHiddenCheckboxesDefaultOff() {
        ActivityScenario.launch(MainActivity::class.java).use { s ->
            assertFalse(s.checkbox("Show system apps in the picker"))
            assertFalse(s.checkbox("Show hidden apps in the picker"))
        }
    }

    @Test
    fun systemAndHiddenCheckboxesPersistAfterRelaunch() {
        ActivityScenario.launch(MainActivity::class.java).use { s ->
            s.click("Show system apps in the picker")
            s.click("Show hidden apps in the picker")
            assertTrue(s.checkbox("Show system apps in the picker"))
            assertTrue(s.checkbox("Show hidden apps in the picker"))
        }
        ActivityScenario.launch(MainActivity::class.java).use { s ->
            assertTrue(s.checkbox("Show system apps in the picker"))
            assertTrue(s.checkbox("Show hidden apps in the picker"))
            s.click("Show system apps in the picker")
        }
        ActivityScenario.launch(MainActivity::class.java).use { s ->
            assertFalse(s.checkbox("Show system apps in the picker"))
            assertTrue(s.checkbox("Show hidden apps in the picker"))
        }
    }

    @Test
    fun hideOnSensitiveDefaultsOnAndPersistsOff() {
        ActivityScenario.launch(MainActivity::class.java).use { s ->
            assertTrue(s.checkbox("Hide over sensitive screens"))
            s.click("Hide over sensitive screens")
        }
        ActivityScenario.launch(MainActivity::class.java).use { s ->
            assertEquals(false, s.checkbox("Hide over sensitive screens"))
        }
    }
}
