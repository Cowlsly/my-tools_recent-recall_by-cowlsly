package com.cowslylabs.recentrecall

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.cowslylabs.recentrecall.logic.ForegroundTracker
import com.cowslylabs.recentrecall.logic.PackageRules
import com.cowslylabs.recentrecall.logic.VisibilityRules
import com.cowslylabs.recentrecall.settings.ButtonAction
import com.cowslylabs.recentrecall.settings.SettingsRepository
import com.cowslylabs.recentrecall.settings.SharedPrefsStore

/**
 * Reads only the package name of the window that came to the front (canRetrieveWindowContent is false),
 * decides whether the button should show, and performs the global actions.
 *
 * Lifecycle: the system binds this service when the user has it switched on, and calls
 * onServiceConnected again after a disconnect, a rebind or a process restart. Setup is therefore
 * idempotent (teardown first), state lives in SharedPreferences, and a slow watchdog re-checks
 * lock and screen state in case a broadcast or window event was missed.
 */
class RecentRecallService : AccessibilityService() {

    private var repo: SettingsRepository? = null
    private var overlay: OverlayController? = null
    private var unsubscribe: (() -> Unit)? = null
    private var receiverRegistered = false
    private var displayListenerRegistered = false
    private val tracker = ForegroundTracker()
    private var screenOn = true
    private var addFailures = 0
    private val handler = Handler(Looper.getMainLooper())

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            screenOn = action != Intent.ACTION_SCREEN_OFF
            // After the screen was off the next window is unknown; do not trust the old app.
            if (action == Intent.ACTION_SCREEN_OFF) tracker.reset()
            refresh()
        }
    }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) {
            // Rotation and display size changes can arrive here even when no configuration callback does.
            overlay?.onConfigurationChanged()
            refresh()
        }
    }

    private val watchdog = object : Runnable {
        override fun run() {
            if (overlay == null) return
            screenOn = (getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive
            refresh()
            handler.postDelayed(this, WATCHDOG_MS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Reconnects and process restarts call this again: clear anything from the previous connection.
        teardown()
        instance = this
        val r = SettingsRepository(SharedPrefsStore(applicationContext))
        repo = r
        overlay = OverlayController(this, r) { perform(it) }
        unsubscribe = r.store.observe { refresh() }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }
        receiverRegistered = true
        (getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).registerDisplayListener(displayListener, handler)
        displayListenerRegistered = true
        screenOn = (getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive
        addFailures = 0
        refresh()
        handler.removeCallbacks(watchdog)
        handler.postDelayed(watchdog, WATCHDOG_MS)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (tracker.onWindowState(event.packageName?.toString())) refresh()
    }

    override fun onInterrupt() = Unit

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Rotation, display size or font size change: re-place and re-clamp the button.
        overlay?.onConfigurationChanged()
        handler.post { refresh() }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        teardown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        teardown()
        super.onDestroy()
    }

    private fun teardown() {
        handler.removeCallbacksAndMessages(null)
        unsubscribe?.invoke()
        unsubscribe = null
        if (receiverRegistered) {
            try {
                unregisterReceiver(screenReceiver)
            } catch (e: IllegalArgumentException) {
                // Already unregistered.
            }
            receiverRegistered = false
        }
        if (displayListenerRegistered) {
            (getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).unregisterDisplayListener(displayListener)
            displayListenerRegistered = false
        }
        overlay?.remove()
        overlay = null
        repo = null
        tracker.reset()
        if (instance === this) instance = null
    }

    private fun refresh() {
        val r = repo ?: return
        val o = overlay ?: return
        val s = r.load()
        val pkg = tracker.current
        val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val visible = s.enabled &&
            screenOn &&
            !keyguard.isKeyguardLocked &&
            !PackageRules.isSensitive(pkg, s.hideOnSensitive) &&
            VisibilityRules.shouldShow(pkg, s.allowList, s.blockList)
        val ok = o.apply(s, visible)
        if (!ok && addFailures < MAX_ADD_RETRIES) {
            addFailures++
            handler.postDelayed({ refresh() }, RETRY_MS)
        } else if (ok) {
            addFailures = 0
        }
    }

    fun openRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)

    private fun perform(action: ButtonAction) {
        when (action) {
            ButtonAction.RECENTS -> openRecents()
            ButtonAction.NOTIFICATIONS -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            ButtonAction.QUICK_SETTINGS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
                } else {
                    Toast.makeText(this, "Quick Settings needs Android 12 or newer", Toast.LENGTH_SHORT).show()
                }
            }
            ButtonAction.APP_SETTINGS -> startActivity(
                Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            ButtonAction.NONE -> Unit
        }
    }

    companion object {
        private const val WATCHDOG_MS = 3000L
        private const val RETRY_MS = 1000L
        private const val MAX_ADD_RETRIES = 3

        @Volatile
        var instance: RecentRecallService? = null
            private set
    }
}
