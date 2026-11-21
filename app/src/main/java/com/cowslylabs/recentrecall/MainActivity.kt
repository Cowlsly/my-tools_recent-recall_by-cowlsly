package com.cowslylabs.recentrecall

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.cowslylabs.recentrecall.settings.Appearance
import com.cowslylabs.recentrecall.settings.ButtonAction
import com.cowslylabs.recentrecall.settings.ButtonSettings
import com.cowslylabs.recentrecall.settings.SettingsRepository
import com.cowslylabs.recentrecall.settings.SharedPrefsStore

class MainActivity : Activity() {

    private lateinit var repo: SettingsRepository
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = SettingsRepository(SharedPrefsStore(applicationContext))
        setContentView(buildUi())
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun label(t: String, sp: Float = 16f, bold: Boolean = false): TextView = TextView(this).apply {
        text = t
        textSize = sp
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(8), 0, dp(4))
    }

    /** Section title in the accent colour, which switches for dark mode. */
    private fun header(t: String): TextView = label(t, 20f, true).apply {
        setTextColor(getColor(R.color.rr_accent))
        setPadding(0, dp(24), 0, dp(4))
    }

    /** Smaller, softer text that explains the control above or below it. */
    private fun helper(t: String): TextView = label(t, 14f).apply {
        alpha = 0.8f
        setPadding(0, 0, 0, dp(8))
    }

    private fun button(t: String, onClick: () -> Unit): Button = Button(this).apply {
        text = t
        isAllCaps = false
        minHeight = dp(48)
        setOnClickListener { onClick() }
    }

    private fun update(change: (ButtonSettings) -> ButtonSettings) {
        repo.save(change(repo.load()))
    }

    private fun checkbox(t: String, checked: Boolean, change: (ButtonSettings, Boolean) -> ButtonSettings): CheckBox =
        CheckBox(this).apply {
            text = t
            textSize = 16f
            minHeight = dp(48)
            isChecked = checked
            setOnCheckedChangeListener { _, c -> update { change(it, c) } }
        }

    private fun <T> spinner(values: List<T>, labels: List<String>, current: T, onPick: (T) -> Unit): Spinner {
        val sp = Spinner(this)
        sp.minimumHeight = dp(48)
        sp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        var idx = values.indexOf(current).coerceAtLeast(0)
        sp.setSelection(idx)
        sp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (pos != idx) {
                    idx = pos
                    onPick(values[pos])
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        return sp
    }

    private fun actionLabel(a: ButtonAction): String = when (a) {
        ButtonAction.RECENTS -> "Open Recents"
        ButtonAction.APP_SETTINGS -> "Open Recent-Recall settings"
        ButtonAction.QUICK_SETTINGS -> "Open Quick Settings (Android 12+)"
        ButtonAction.NOTIFICATIONS -> "Open notification shade"
        ButtonAction.NONE -> "Do nothing"
    }

    private fun appearanceLabel(a: Appearance): String = when (a) {
        Appearance.ICON_DETAILED -> "Icon (detailed)"
        Appearance.ICON_SIMPLE -> "Icon (simple)"
        Appearance.DOT -> "Plain dot"
    }

    private fun buildUi(): View {
        val s = repo.load()
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(24), dp(16), dp(24))
        }
        col.addView(label("Recent-Recall", 28f, true))
        col.addView(helper("Debug build, not a production release."))

        col.addView(header("Permission status"))
        status = label("")
        col.addView(status)
        col.addView(helper("Android does not let an app switch its own accessibility service on. Turn it on here, then come back."))
        col.addView(button("Open Accessibility settings") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        col.addView(helper("If Android says the setting is restricted, open App info, tap the three dots, choose Allow restricted settings, then try again."))
        col.addView(button("Open App info (Allow restricted settings)") {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")),
            )
        })

        col.addView(header("Button"))
        col.addView(Switch(this).apply {
            text = "Show floating button"
            textSize = 16f
            minHeight = dp(48)
            isChecked = s.enabled
            setOnCheckedChangeListener { _, c -> update { it.copy(enabled = c) } }
        })
        col.addView(label("Appearance"))
        col.addView(
            spinner(Appearance.values().toList(), Appearance.values().map { appearanceLabel(it) }, s.appearance) { v ->
                update { it.copy(appearance = v) }
            },
        )
        val opacityLabel = label("Opacity: ${s.opacityPercent}%")
        col.addView(opacityLabel)
        col.addView(SeekBar(this).apply {
            max = ButtonSettings.MAX_OPACITY - ButtonSettings.MIN_OPACITY
            progress = s.opacityPercent - ButtonSettings.MIN_OPACITY
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    val v = p + ButtonSettings.MIN_OPACITY
                    opacityLabel.text = "Opacity: $v%"
                    if (fromUser) update { it.copy(opacityPercent = v) }
                }

                override fun onStartTrackingTouch(sb: SeekBar?) = Unit
                override fun onStopTrackingTouch(sb: SeekBar?) = Unit
            })
        })
        val sizeLabel = label("Size: ${s.sizeDp} dp")
        col.addView(sizeLabel)
        col.addView(SeekBar(this).apply {
            max = ButtonSettings.MAX_SIZE_DP - ButtonSettings.MIN_SIZE_DP
            progress = s.sizeDp - ButtonSettings.MIN_SIZE_DP
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    val v = p + ButtonSettings.MIN_SIZE_DP
                    sizeLabel.text = "Size: $v dp"
                    if (fromUser) update { it.copy(sizeDp = v) }
                }

                override fun onStartTrackingTouch(sb: SeekBar?) = Unit
                override fun onStopTrackingTouch(sb: SeekBar?) = Unit
            })
        })
        col.addView(label("Tap"))
        col.addView(
            spinner(ButtonAction.values().toList(), ButtonAction.values().map { actionLabel(it) }, s.tapAction) { v ->
                update { it.copy(tapAction = v) }
            },
        )
        col.addView(label("Long press"))
        col.addView(
            spinner(ButtonAction.values().toList(), ButtonAction.values().map { actionLabel(it) }, s.longPressAction) { v ->
                update { it.copy(longPressAction = v) }
            },
        )
        col.addView(checkbox("Lock button position", s.positionLocked) { cur, c -> cur.copy(positionLocked = c) })
        col.addView(button("Reset button position") { update { it.copy(posX = 1f, posY = 0.6f) } })

        col.addView(header("Privacy"))
        col.addView(checkbox("Hide over sensitive screens", s.hideOnSensitive) { cur, c -> cur.copy(hideOnSensitive = c) })
        col.addView(helper("Hides the button over permission, install and fingerprint prompts, password managers, authenticators, wallets and banking apps. It matches app names only, so it cannot see inside an app. Use the blocked list for anything it misses. Details are in docs/SENSITIVE_SCREENS.md."))

        col.addView(header("Which apps show the button"))
        col.addView(helper("Blocked apps never show it. With no allowed apps it shows over every app that is not blocked. With allowed apps it shows only over those."))
        col.addView(checkbox("Show system apps in the picker", s.showSystemApps) { cur, c -> cur.copy(showSystemApps = c) })
        col.addView(checkbox("Show hidden apps in the picker", s.showHiddenApps) { cur, c -> cur.copy(showHiddenApps = c) })
        col.addView(helper("The picker list may be incomplete. Android limits which apps an app can see, and some phones or work profiles limit it further. Saved rules keep working for apps that are not listed."))
        col.addView(button("Edit app rules") { startActivity(Intent(this, AppRulesActivity::class.java)) })

        col.addView(header("Launcher"))
        col.addView(helper("A second launcher icon named Recents opens Recents directly. You can place it on the home screen or dock yourself. It does not replace your launcher's app drawer button."))
        col.addView(button("Add a Recents shortcut to the home screen") { pinShortcut() })
        col.addView(helper("The button hides on the lock screen and over sensitive screens. On some phones the accessibility service needs switching on again after a restart."))
        return ScrollView(this).apply { addView(col) }
    }

    private fun pinShortcut() {
        val sm = getSystemService(ShortcutManager::class.java)
        if (sm != null && sm.isRequestPinShortcutSupported) {
            val info = ShortcutInfo.Builder(this, "recents_pinned")
                .setShortLabel("Recents")
                .setLongLabel("Open recent apps")
                .setIcon(Icon.createWithResource(this, R.drawable.recent_recall_launcher))
                .setIntent(Intent(this, ShortcutActivity::class.java).setAction(Intent.ACTION_VIEW))
                .build()
            sm.requestPinShortcut(info, null)
        } else {
            Toast.makeText(this, "Your launcher does not support pinned shortcuts. Use the Recents launcher icon instead.", Toast.LENGTH_LONG).show()
        }
    }

    private fun serviceEnabledInSettings(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val me = ComponentName(this, RecentRecallService::class.java)
        return flat.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    private fun updateStatus() {
        val running = RecentRecallService.instance != null
        val enabled = serviceEnabledInSettings()
        val a = when {
            running -> "On and running"
            enabled -> "On in Settings, not connected yet"
            else -> "Off. Turn it on in Accessibility settings"
        }
        status.text = "Accessibility service: $a\n" +
            "This is the one permission the button needs. It lets the button open Recents. It reads only the name of the app in front, never what is on screen.\n\n" +
            "Display over other apps: not needed\n" +
            "Usage access: not needed\n" +
            "App list access: used only by the app rules picker\n" +
            "Network: none, nothing leaves your phone"
    }
}
