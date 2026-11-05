package com.cowslylabs.recentrecall.settings

import android.content.Context
import android.content.SharedPreferences

/** Small storage seam so settings persistence can be unit tested without Android. */
interface KeyValueStore {
    fun getString(key: String, def: String): String
    fun getInt(key: String, def: Int): Int
    fun getBoolean(key: String, def: Boolean): Boolean
    fun getFloat(key: String, def: Float): Float
    fun getStringSet(key: String): Set<String>

    /** Values may be String, Int, Boolean, Float or Set of String. Written together. */
    fun putAll(values: Map<String, Any>)

    /** Registers a change listener. Returns a function that unregisters it. */
    fun observe(listener: () -> Unit): () -> Unit
}

class SharedPrefsStore(context: Context) : KeyValueStore {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("recent_recall_settings", Context.MODE_PRIVATE)

    override fun getString(key: String, def: String): String = prefs.getString(key, def) ?: def
    override fun getInt(key: String, def: Int): Int = prefs.getInt(key, def)
    override fun getBoolean(key: String, def: Boolean): Boolean = prefs.getBoolean(key, def)
    override fun getFloat(key: String, def: Float): Float = prefs.getFloat(key, def)
    override fun getStringSet(key: String): Set<String> =
        prefs.getStringSet(key, null)?.toSet() ?: emptySet()

    override fun putAll(values: Map<String, Any>) {
        val edit = prefs.edit()
        for ((k, v) in values) {
            when (v) {
                is String -> edit.putString(k, v)
                is Int -> edit.putInt(k, v)
                is Boolean -> edit.putBoolean(k, v)
                is Float -> edit.putFloat(k, v)
                is Set<*> -> edit.putStringSet(k, v.filterIsInstance<String>().toSet())
                else -> Unit
            }
        }
        edit.apply()
    }

    override fun observe(listener: () -> Unit): () -> Unit {
        // SharedPreferences keeps listeners weakly, so the caller holds the returned function.
        val l = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> listener() }
        prefs.registerOnSharedPreferenceChangeListener(l)
        return { prefs.unregisterOnSharedPreferenceChangeListener(l) }
    }
}
