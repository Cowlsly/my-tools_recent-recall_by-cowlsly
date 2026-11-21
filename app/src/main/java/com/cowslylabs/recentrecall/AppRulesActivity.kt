package com.cowslylabs.recentrecall

import android.app.Activity
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import com.cowslylabs.recentrecall.logic.PickerFilter
import com.cowslylabs.recentrecall.logic.RuleState
import com.cowslylabs.recentrecall.logic.VisibilityRules
import com.cowslylabs.recentrecall.settings.SettingsRepository
import com.cowslylabs.recentrecall.settings.SharedPrefsStore

class AppRulesActivity : Activity() {
    private data class Row(val pkg: String, val label: String, val system: Boolean, val hidden: Boolean)

    private lateinit var repo: SettingsRepository
    private lateinit var summary: TextView
    private var all: List<Row> = emptyList()
    private var shown: List<Row> = emptyList()

    private val adapter = object : BaseAdapter() {
        override fun getCount(): Int = shown.size
        override fun getItem(position: Int): Any = shown[position]
        override fun getItemId(position: Int): Long = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val tv = (convertView as? TextView) ?: TextView(this@AppRulesActivity).apply {
                setPadding(dp(16), dp(10), dp(16), dp(10))
                textSize = 15f
            }
            val row = shown[position]
            val s = repo.load()
            val state = VisibilityRules.ruleFor(row.pkg, s.allowList, s.blockList)
            val tag = when (state) {
                RuleState.ALLOW -> "ALLOW"
                RuleState.BLOCK -> "BLOCK"
                RuleState.NONE -> "no rule"
            }
            tv.text = "${row.label}  [$tag]\n${row.pkg}"
            return tv
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = SettingsRepository(SharedPrefsStore(applicationContext))
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(16), dp(8), dp(8))
        }
        col.addView(TextView(this).apply {
            text = "App rules"
            textSize = 22f
            setPadding(dp(8), 0, dp(8), dp(4))
        })
        col.addView(TextView(this).apply {
            text = "Tap an app to cycle: no rule, ALLOW, BLOCK. Blocked apps never show the button. With no allowed apps, every app that is not blocked shows it. With allowed apps, only those do.\n\nThis list may be incomplete: Android filters which apps an app can see, and some devices or work profiles limit it further. Saved rules keep working even for apps that are not listed."
            textSize = 13f
            setPadding(dp(8), 0, dp(8), dp(8))
        })
        summary = TextView(this).apply { setPadding(dp(8), 0, dp(8), dp(8)) }
        col.addView(summary)
        val list = ListView(this)
        list.adapter = adapter
        list.setOnItemClickListener { _, _, position, _ ->
            val row = shown[position]
            val s = repo.load()
            val next = VisibilityRules.nextState(VisibilityRules.ruleFor(row.pkg, s.allowList, s.blockList))
            val (a, b) = VisibilityRules.applyRule(row.pkg, next, s.allowList, s.blockList)
            repo.save(s.copy(allowList = a, blockList = b))
            adapter.notifyDataSetChanged()
            updateSummary()
        }
        col.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(col)
        load()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun load() {
        Thread {
            val pm = packageManager
            val rows = pm.getInstalledApplications(0)
                .filter { it.packageName != packageName }
                .map {
                    Row(
                        pkg = it.packageName,
                        label = pm.getApplicationLabel(it).toString(),
                        system = (it.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0,
                        hidden = pm.getLaunchIntentForPackage(it.packageName) == null,
                    )
                }
                .sortedBy { it.label.lowercase() }
            runOnUiThread {
                all = rows
                refreshList()
            }
        }.start()
    }

    private fun refreshList() {
        val s = repo.load()
        shown = all.filter { PickerFilter.include(it.system, it.hidden, s.showSystemApps, s.showHiddenApps) }
        adapter.notifyDataSetChanged()
        updateSummary()
    }

    private fun updateSummary() {
        val s = repo.load()
        val listed = shown.map { it.pkg }.toSet()
        val offList = (s.allowList + s.blockList).count { it !in listed }
        summary.text = "${shown.size} apps listed. $offList saved rules are for apps not listed and stay active."
    }
}
