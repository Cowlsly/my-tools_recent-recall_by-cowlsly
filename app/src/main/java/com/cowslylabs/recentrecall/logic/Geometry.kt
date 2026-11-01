package com.cowslylabs.recentrecall.logic

import kotlin.math.roundToInt

/** Pure position maths so the button always stays inside the visible screen. */
object Geometry {
    fun maxOffset(screen: Int, size: Int): Int = maxOf(0, screen - size)

    fun clampPx(px: Int, screen: Int, size: Int): Int = px.coerceIn(0, maxOffset(screen, size))

    fun fractionToPx(f: Float, screen: Int, size: Int): Int =
        (f.coerceIn(0f, 1f) * maxOffset(screen, size)).roundToInt()

    fun pxToFraction(px: Int, screen: Int, size: Int): Float {
        val m = maxOffset(screen, size)
        return if (m == 0) 0f else clampPx(px, screen, size).toFloat() / m
    }
}
