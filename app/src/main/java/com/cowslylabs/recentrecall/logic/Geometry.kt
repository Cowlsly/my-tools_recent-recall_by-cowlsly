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

    /**
     * Region versions: the button may only sit between [start] and [end] (for example the screen minus
     * status bar, navigation bar and display cutout). If the region is smaller than the button it sits at [start].
     */
    fun clampInRegion(px: Int, start: Int, end: Int, size: Int): Int =
        px.coerceIn(start, maxOf(start, end - size))

    fun fractionToPxInRegion(f: Float, start: Int, end: Int, size: Int): Int =
        start + (f.coerceIn(0f, 1f) * maxOf(0, end - start - size)).roundToInt()

    fun pxToFractionInRegion(px: Int, start: Int, end: Int, size: Int): Float {
        val m = maxOf(0, end - start - size)
        return if (m == 0) 0f else (clampInRegion(px, start, end, size) - start).toFloat() / m
    }
}
