package com.cowslylabs.recentrecall

import com.cowslylabs.recentrecall.settings.KeyValueStore

class MapStore : KeyValueStore {
    val map = mutableMapOf<String, Any>()
    var notified = 0

    override fun getString(key: String, def: String) = map[key] as? String ?: def
    override fun getInt(key: String, def: Int) = map[key] as? Int ?: def
    override fun getBoolean(key: String, def: Boolean) = map[key] as? Boolean ?: def
    override fun getFloat(key: String, def: Float) = map[key] as? Float ?: def
    override fun getStringSet(key: String): Set<String> =
        (map[key] as? Set<*>)?.filterIsInstance<String>()?.toSet() ?: emptySet()

    override fun putAll(values: Map<String, Any>) {
        map.putAll(values)
        notified++
    }

    override fun observe(listener: () -> Unit): () -> Unit = {}
}
