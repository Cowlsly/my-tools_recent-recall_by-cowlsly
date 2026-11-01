package com.cowslylabs.recentrecall

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.cowslylabs.recentrecall.logic.PackageRules
import com.cowslylabs.recentrecall.logic.VisibilityRules
import com.cowslylabs.recentrecall.settings.ButtonAction
import com.cowslylabs.recentrecall.settings.SettingsRepository
import com.cowslylabs.recentrecall.settings.SharedPrefsStore

/**
 * Reads only the package name of the window that came to the front (canRetrieveWindowContent is false),
 * decides whether the button should show, and performs the global actions.
 */
class RecentRecallService : AccessibilityService() {

    private var repo: SettingsRepository? = null
    private var overlay: OverlayController? = null
    private var unsubscribe: (() -> Unit)? = null
    private var receiverRegistered = false
    private var foregroundPkg: String? = null
    private var screenOn = true
    private val handler = Handler(Looper.getMainLooper())

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            screenOn = intent?.action != Intent.ACTION_SCREEN_OFF
            refresh()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
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
        screenOn = (getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive
        refresh()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (PackageRules.isIgnored(pkg)) return
        foregroundPkg = pkg
        refresh()
    }

    override fun onInterrupt() = Unit

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Rotation or display size change: re-place and re-clamp the button.
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
        overlay?.remove()
        overlay = null
        if (instance === this) instance = null
    }

    private fun refresh() {
        val r = repo ?: return
        val o = overlay ?: return
        val s = r.load()
        val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val visible = s.enabled &&
            screenOn &&
            !keyguard.isKeyguardLocked &&
            !PackageRules.isSensitive(foregroundPkg) &&
            VisibilityRules.shouldShow(foregroundPkg, s.allowList, s.blockList)
        o.apply(s, visible)
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
        @Volatile
        var instance: RecentRecallService? = null
            private set
    }
}
