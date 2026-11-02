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

    private fun label(t: String, sp: Float = 15f, bold: Boolean = false): TextView = TextView(this).apply {
        text = t
        textSize = sp
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(8), 0, dp(4))
    }

    private fun button(t: String, onClick: () -> Unit): Button = Button(this).apply {
        text = t
        isAllCaps = false
        setOnClickListener { onClick() }
    }

    private fun update(change: (ButtonSettings) -> ButtonSettings) {
        repo.save(change(repo.load()))
    }

    private fun checkbox(t: String, checked: Boolean, change: (ButtonSettings, Boolean) -> ButtonSettings): CheckBox =
        CheckBox(this).apply {
            text = t
            isChecked = checked
            setOnCheckedChangeListener { _, c -> update { change(it, c) } }
        }

    private fun <T> spinner(values: List<T>, labels: List<String>, current: T, onPick: (T) -> Unit): Spinner {
        val sp = Spinner(this)
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
        col.addView(label("Recent-Recall", 26f, true))
        col.addView(label("Debug build, not a production release."))
        status = label("")
        col.addView(status)
        col.addView(button("Open Accessibility settings") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        col.addView(button("Open App info (Allow restricted settings)") {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")),
            )
        })

        col.addView(label("Button", 18f, true))
        col.addView(Switch(this).apply {
            text = "Show floating button"
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

        col.addView(label("Privacy", 18f, true))
        col.addView(label("Hide the button over sensitive screens: permission, install and fingerprint prompts, password managers, authenticators, wallets and banking apps. It matches package names only, so it cannot see inside an app. Use the blocklist for anything it misses. The list is a constant in PackageRules.kt, described in docs/SENSITIVE_SCREENS.md."))
        col.addView(checkbox("Hide over sensitive screens", s.hideOnSensitive) { cur, c -> cur.copy(hideOnSensitive = c) })

        col.addView(label("Which apps show the button", 18f, true))
        col.addView(label("Blocked apps never show it. With no allowed apps it shows over every app that is not blocked. With allowed apps it shows only over those."))
        col.addView(checkbox("Show system apps in the picker", s.showSystemApps) { cur, c -> cur.copy(showSystemApps = c) })
        col.addView(checkbox("Show hidden apps in the picker", s.showHiddenApps) { cur, c -> cur.copy(showHiddenApps = c) })
        col.addView(button("Edit app rules") { startActivity(Intent(this, AppRulesActivity::class.java)) })

        col.addView(label("Launcher", 18f, true))
        col.addView(label("A second launcher icon named Recents opens Recents directly. You can place it on the home screen or dock yourself. It does not replace your launcher's app drawer button."))
        col.addView(button("Add a Recents shortcut to the home screen") { pinShortcut() })
        col.addView(label("Android cannot turn the accessibility service on for you, and it may need switching on again after a restart on some devices. The button hides on the lock screen and over sensitive screens."))
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
            running -> "ON and running"
            enabled -> "ON in Settings, not connected yet"
            else -> "OFF (turn it on in Accessibility settings)"
        }
        status.text = "Permission status\n" +
            "Accessibility service: $a\n" +
            "Display over other apps: not needed (the button is an accessibility overlay)\n" +
            "Usage access: not needed\n" +
            "App list access: declared at install, used only by the app rules picker\n" +
            "Network: none"
    }
}
