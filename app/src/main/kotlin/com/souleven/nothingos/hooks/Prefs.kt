package com.souleven.nothingos.hooks

import android.content.SharedPreferences
import de.robv.android.xposed.XSharedPreferences

class Prefs(private val delegate: SharedPreferences) {

    private companion object {
        const val RELOAD_INTERVAL_MS = 1_000L
    }

    @Volatile
    private var lastReload = 0L

    private fun maybeReload() {
        val now = System.currentTimeMillis()
        if (now - lastReload >= RELOAD_INTERVAL_MS) {
            lastReload = now
            (delegate as? XSharedPreferences)?.reload()
        }
    }

    fun getBoolean(key: String, default: Boolean): Boolean {
        maybeReload()
        return delegate.getBoolean(key, default)
    }

    fun getString(key: String, default: String): String {
        maybeReload()
        return delegate.getString(key, default) ?: default
    }

    fun getInt(key: String, default: Int): Int {
        maybeReload()
        return try {
            delegate.getInt(key, default)
        } catch (_: Throwable) {
            try {
                delegate.getString(key, null)?.toIntOrNull() ?: default
            } catch (_: Throwable) {
                default
            }
        }
    }

    fun forceReload() {
        lastReload = System.currentTimeMillis()
        (delegate as? XSharedPreferences)?.reload()
    }

    val file get() = (delegate as? XSharedPreferences)?.file
}
