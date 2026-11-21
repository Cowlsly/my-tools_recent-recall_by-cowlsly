package com.cowslylabs.recentrecall.logic

import kotlin.math.hypot

/**
 * Turns raw touch points into one deliberate outcome: tap, long press or drag.
 * A drag only starts after the finger moves past [slopPx], and only when the position is unlocked.
 * Once a drag, a long press or a cancel has happened, the gesture can never become a tap.
 */
class GestureInterpreter(private val slopPx: Float) {
    enum class Result { NONE, TAP, DRAG_END }

    private enum class State { IDLE, PRESSED, DRAGGING, LONG_PRESSED, CANCELLED }

    private var state = State.IDLE
    private var startX = 0f
    private var startY = 0f
    private var locked = false

    val isDragging: Boolean get() = state == State.DRAGGING

    fun down(x: Float, y: Float, locked: Boolean) {
        state = State.PRESSED
        startX = x
        startY = y
        this.locked = locked
    }

    /** Returns true while a drag is in progress. */
    fun move(x: Float, y: Float): Boolean {
        if (state == State.PRESSED && hypot(x - startX, y - startY) > slopPx) {
            state = if (locked) State.CANCELLED else State.DRAGGING
        }
        return state == State.DRAGGING
    }

    /** Called when the long-press timeout fires. Returns true if it counts as a long press. */
    fun longPress(): Boolean {
        if (state == State.PRESSED) {
            state = State.LONG_PRESSED
            return true
        }
        return false
    }

    fun up(): Result {
        val r = when (state) {
            State.PRESSED -> Result.TAP
            State.DRAGGING -> Result.DRAG_END
            else -> Result.NONE
        }
        state = State.IDLE
        return r
    }

    fun cancel() {
        state = State.IDLE
    }
}
