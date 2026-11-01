package com.cowslylabs.recentrecall

import com.cowslylabs.recentrecall.logic.Geometry
import org.junit.Assert.assertEquals
import org.junit.Test

class GeometryTest {
    @Test
    fun clampKeepsButtonInsideScreen() {
        assertEquals(0, Geometry.clampPx(-50, 1000, 100))
        assertEquals(900, Geometry.clampPx(2000, 1000, 100))
        assertEquals(450, Geometry.clampPx(450, 1000, 100))
    }

    @Test
    fun buttonLargerThanScreenClampsToZero() {
        assertEquals(0, Geometry.clampPx(30, 80, 100))
        assertEquals(0, Geometry.fractionToPx(0.5f, 80, 100))
        assertEquals(0f, Geometry.pxToFraction(10, 80, 100), 0f)
    }

    @Test
    fun fractionMapsToPixelsAndBack() {
        assertEquals(450, Geometry.fractionToPx(0.5f, 1000, 100))
        assertEquals(0.5f, Geometry.pxToFraction(450, 1000, 100), 0.001f)
        assertEquals(900, Geometry.fractionToPx(1f, 1000, 100))
    }

    @Test
    fun rotationKeepsRelativePositionInsideNewBounds() {
        val f = Geometry.pxToFraction(900, 1000, 100)
        val landscapeX = Geometry.fractionToPx(f, 2000, 100)
        assertEquals(1900, landscapeX)
        val smallerY = Geometry.fractionToPx(0.6f, 480, 100)
        assertEquals(true, smallerY in 0..380)
    }

    @Test
    fun outOfRangeFractionsAreClamped() {
        assertEquals(900, Geometry.fractionToPx(7f, 1000, 100))
        assertEquals(0, Geometry.fractionToPx(-1f, 1000, 100))
    }
}
