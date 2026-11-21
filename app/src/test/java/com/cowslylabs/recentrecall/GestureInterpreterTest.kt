package com.cowslylabs.recentrecall

import com.cowslylabs.recentrecall.logic.GestureInterpreter
import com.cowslylabs.recentrecall.logic.GestureInterpreter.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureInterpreterTest {
    private fun g() = GestureInterpreter(20f)

    @Test
    fun stillPressIsTap() {
        val g = g()
        g.down(100f, 100f, false)
        assertFalse(g.move(105f, 104f))
        assertEquals(Result.TAP, g.up())
    }

    @Test
    fun movementPastSlopIsDragNeverTap() {
        val g = g()
        g.down(100f, 100f, false)
        assertTrue(g.move(100f, 140f))
        assertEquals(Result.DRAG_END, g.up())
    }

    @Test
    fun lockedPositionNeverDragsAndNeverTapsAfterMoving() {
        val g = g()
        g.down(100f, 100f, true)
        assertFalse(g.move(100f, 200f))
        assertEquals(Result.NONE, g.up())
    }

    @Test
    fun longPressFiresOnceAndBlocksTap() {
        val g = g()
        g.down(0f, 0f, false)
        assertTrue(g.longPress())
        assertFalse(g.longPress())
        assertEquals(Result.NONE, g.up())
    }

    @Test
    fun longPressDoesNotFireAfterDragStarted() {
        val g = g()
        g.down(0f, 0f, false)
        g.move(0f, 50f)
        assertFalse(g.longPress())
    }

    @Test
    fun movingAfterLongPressDoesNotStartDrag() {
        val g = g()
        g.down(0f, 0f, false)
        g.longPress()
        assertFalse(g.move(0f, 90f))
        assertEquals(Result.NONE, g.up())
    }

    @Test
    fun cancelResetsAndNewGestureWorks() {
        val g = g()
        g.down(0f, 0f, false)
        g.cancel()
        assertEquals(Result.NONE, g.up())
        g.down(0f, 0f, false)
        assertEquals(Result.TAP, g.up())
    }
}
