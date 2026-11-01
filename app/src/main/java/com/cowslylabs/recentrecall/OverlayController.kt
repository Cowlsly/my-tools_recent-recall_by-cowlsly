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

    fun apply(s: ButtonSettings, visible: Boolean) {
        latest = s
        if (!visible) {
            remove()
            return
        }
        val v = view ?: create().also { view = it }
        val p = params ?: return
        style(v, s)
        val size = dp(s.sizeDp)
        p.width = size
        p.height = size
        if (!dragging) {
            val (sw, sh) = screenSize()
            p.x = Geometry.fractionToPx(s.posX, sw, size)
            p.y = Geometry.fractionToPx(s.posY, sh, size)
        }
        try {
            if (v.parent == null) wm.addView(v, p) else wm.updateViewLayout(v, p)
        } catch (e: RuntimeException) {
            // Window could not be added (for example the service is shutting down).
            view = null
            params = null
            lastAppearance = null
        }
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
                    val (sw, sh) = screenSize()
                    p.x = Geometry.clampPx(startX + (e.rawX - startRawX).toInt(), sw, p.width)
                    p.y = Geometry.clampPx(startY + (e.rawY - startRawY).toInt(), sh, p.height)
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
        val (sw, sh) = screenSize()
        repo.savePosition(
            Geometry.pxToFraction(p.x, sw, p.width),
            Geometry.pxToFraction(p.y, sh, p.height),
        )
    }

    private fun screenSize(): Pair<Int, Int> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val b = wm.currentWindowMetrics.bounds
            return b.width() to b.height()
        }
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(dm)
        return dm.widthPixels to dm.heightPixels
    }

    private fun dp(v: Int): Int = (v * service.resources.displayMetrics.density).toInt()
}
