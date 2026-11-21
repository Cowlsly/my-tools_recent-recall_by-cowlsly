package com.cowslylabs.recentrecall

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.ImageView
import com.cowslylabs.recentrecall.logic.Geometry
import com.cowslylabs.recentrecall.logic.GestureInterpreter
import com.cowslylabs.recentrecall.settings.Appearance
import com.cowslylabs.recentrecall.settings.ButtonAction
import com.cowslylabs.recentrecall.settings.ButtonSettings
import com.cowslylabs.recentrecall.settings.SettingsRepository

/**
 * Owns the floating button window. It uses TYPE_ACCESSIBILITY_OVERLAY, which an enabled
 * accessibility service may add without the separate "Display over other apps" permission.
 *
 * The button is always placed inside the usable area (screen minus system bars and display cutout)
 * and is re-placed from the saved fraction on every apply(), so rotation, display size and
 * font size changes cannot leave it off screen.
 */
class OverlayController(
    private val service: AccessibilityService,
    private val repo: SettingsRepository,
    private val onAction: (ButtonAction) -> Unit,
) {
    private val wm = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private val interpreter = GestureInterpreter(ViewConfiguration.get(service).scaledTouchSlop * 2f)

    private var view: ImageView? = null
    private var params: WindowManager.LayoutParams? = null
    private var latest = ButtonSettings()
    private var lastAppearance: Appearance? = null
    private var dragging = false
    private var startRawX = 0f
    private var startRawY = 0f
    private var startX = 0
    private var startY = 0

    private val longPressRunnable = Runnable {
        if (interpreter.longPress()) {
            view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            onAction(latest.longPressAction)
        }
    }

    /** Returns false only when the window could not be added, so the caller can retry. */
    fun apply(s: ButtonSettings, visible: Boolean): Boolean {
        latest = s
        if (!visible) {
            remove()
            return true
        }
        val v = view ?: create().also { view = it }
        val p = params ?: return false
        style(v, s)
        val size = dp(s.sizeDp)
        p.width = size
        p.height = size
        if (!dragging) {
            val a = area()
            p.x = Geometry.fractionToPxInRegion(s.posX, a[0], a[2], size)
            p.y = Geometry.fractionToPxInRegion(s.posY, a[1], a[3], size)
        }
        return try {
            if (v.parent == null) wm.addView(v, p) else wm.updateViewLayout(v, p)
            true
        } catch (e: RuntimeException) {
            // Window could not be added (for example the service is shutting down).
            view = null
            params = null
            lastAppearance = null
            false
        }
    }

    /** Call when rotation, display size or font size changed. Drops any half-finished drag. */
    fun onConfigurationChanged() {
        handler.removeCallbacks(longPressRunnable)
        interpreter.cancel()
        dragging = false
    }

    fun remove() {
        handler.removeCallbacks(longPressRunnable)
        interpreter.cancel()
        dragging = false
        view?.let {
            try {
                wm.removeViewImmediate(it)
            } catch (e: IllegalArgumentException) {
                // Already removed.
            }
        }
        view = null
        params = null
        lastAppearance = null
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun create(): ImageView {
        val v = ImageView(service)
        v.scaleType = ImageView.ScaleType.FIT_CENTER
        v.contentDescription = "Recent-Recall button"
        v.setOnTouchListener { view, e -> onTouch(view, e) }
        params = WindowManager.LayoutParams(
            0,
            0,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START }
        return v
    }

    private fun style(v: ImageView, s: ButtonSettings) {
        if (lastAppearance != s.appearance) {
            when (s.appearance) {
                Appearance.ICON_DETAILED -> v.setImageResource(R.drawable.recent_recall_floating_detailed)
                Appearance.ICON_SIMPLE -> v.setImageResource(R.drawable.recent_recall_floating_common)
                Appearance.DOT -> v.setImageDrawable(
                    GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(0xFF5B3FD6.toInt())
                        setStroke(dp(2), 0xFFFFFFFF.toInt())
                    },
                )
            }
            lastAppearance = s.appearance
        }
        v.alpha = s.opacityPercent / 100f
    }

    private fun onTouch(v: View, e: MotionEvent): Boolean {
        val p = params ?: return false
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                interpreter.down(e.rawX, e.rawY, latest.positionLocked)
                startRawX = e.rawX
                startRawY = e.rawY
                startX = p.x
                startY = p.y
                handler.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE -> {
                if (interpreter.move(e.rawX, e.rawY)) {
                    dragging = true
                    handler.removeCallbacks(longPressRunnable)
                    val a = area()
                    p.x = Geometry.clampInRegion(startX + (e.rawX - startRawX).toInt(), a[0], a[2], p.width)
                    p.y = Geometry.clampInRegion(startY + (e.rawY - startRawY).toInt(), a[1], a[3], p.height)
                    try {
                        wm.updateViewLayout(v, p)
                    } catch (ex: RuntimeException) {
                        // View already gone.
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(longPressRunnable)
                when (interpreter.up()) {
                    GestureInterpreter.Result.TAP -> {
                        v.performClick()
                        onAction(latest.tapAction)
                    }
                    GestureInterpreter.Result.DRAG_END -> savePosition(p)
                    GestureInterpreter.Result.NONE -> Unit
                }
                dragging = false
            }
            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRunnable)
                val wasDragging = interpreter.isDragging
                interpreter.cancel()
                if (wasDragging) savePosition(p)
                dragging = false
            }
            else -> return false
        }
        return true
    }

    private fun savePosition(p: WindowManager.LayoutParams) {
        val a = area()
        repo.savePosition(
            Geometry.pxToFractionInRegion(p.x, a[0], a[2], p.width),
            Geometry.pxToFractionInRegion(p.y, a[1], a[3], p.height),
        )
    }

    /** Usable area as [left, top, right, bottom] in screen pixels: screen minus system bars and cutout. */
    private fun area(): IntArray {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val m = wm.currentWindowMetrics
            val b = m.bounds
            val i = m.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout(),
            )
            return intArrayOf(b.left + i.left, b.top + i.top, b.right - i.right, b.bottom - i.bottom)
        }
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(dm)
        val id = service.resources.getIdentifier("status_bar_height", "dimen", "android")
        val top = if (id > 0) service.resources.getDimensionPixelSize(id) else 0
        return intArrayOf(0, top, dm.widthPixels, dm.heightPixels)
    }

    private fun dp(v: Int): Int = (v * service.resources.displayMetrics.density).toInt()
}
