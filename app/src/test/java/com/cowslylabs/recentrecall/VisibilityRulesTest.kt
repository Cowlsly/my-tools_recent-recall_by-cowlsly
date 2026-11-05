package com.cowslylabs.recentrecall

import com.cowslylabs.recentrecall.logic.PackageRules
import com.cowslylabs.recentrecall.logic.PickerFilter
import com.cowslylabs.recentrecall.logic.RuleState
import com.cowslylabs.recentrecall.logic.VisibilityRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisibilityRulesTest {
    private val none = emptySet<String>()

    @Test
    fun emptyListsShowEverywhere() {
        assertTrue(VisibilityRules.shouldShow("a", none, none))
        assertTrue(VisibilityRules.shouldShow(null, none, none))
    }

    @Test
    fun blocklistHidesApp() {
        assertFalse(VisibilityRules.shouldShow("a", none, setOf("a")))
        assertTrue(VisibilityRules.shouldShow("b", none, setOf("a")))
    }

    @Test
    fun nonEmptyAllowlistShowsOnlyAllowed() {
        assertTrue(VisibilityRules.shouldShow("a", setOf("a"), none))
        assertFalse(VisibilityRules.shouldShow("b", setOf("a"), none))
        assertFalse(VisibilityRules.shouldShow(null, setOf("a"), none))
    }

    @Test
    fun blocklistWinsOverAllowlist() {
        assertFalse(VisibilityRules.shouldShow("a", setOf("a"), setOf("a")))
    }

    @Test
    fun applyRuleKeepsAppInOneListOnly() {
        var allow = setOf<String>()
        var block = setOf<String>()
        var state = RuleState.NONE
        val seen = mutableListOf<RuleState>()
        repeat(4) {
            state = VisibilityRules.nextState(state)
            val (a, b) = VisibilityRules.applyRule("p", state, allow, block)
            allow = a
            block = b
            seen.add(VisibilityRules.ruleFor("p", allow, block))
            assertFalse("p" in allow && "p" in block)
        }
        assertEquals(listOf(RuleState.ALLOW, RuleState.BLOCK, RuleState.NONE, RuleState.ALLOW), seen)
    }

    @Test
    fun pickerFilterHonoursCheckboxes() {
        assertTrue(PickerFilter.include(false, false, false, false))
        assertFalse(PickerFilter.include(true, false, false, false))
        assertTrue(PickerFilter.include(true, false, true, false))
        assertFalse(PickerFilter.include(false, true, true, false))
        assertTrue(PickerFilter.include(false, true, false, true))
        assertFalse(PickerFilter.include(true, true, true, false))
        assertTrue(PickerFilter.include(true, true, true, true))
    }

    @Test
    fun sensitiveAndIgnoredPackages() {
        assertTrue(PackageRules.isSensitive("com.google.android.permissioncontroller"))
        assertFalse(PackageRules.isSensitive("com.example"))
        assertFalse(PackageRules.isSensitive(null))
        assertTrue(PackageRules.isIgnored("com.android.systemui"))
    }
}
