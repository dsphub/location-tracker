package com.dsp.ping.data

import android.content.Context

class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getHost(): String? = prefs.getString(KEY_HOST, null)

    fun setHost(value: String) {
        prefs.edit().putString(KEY_HOST, value).apply()
    }

    fun clearHost() {
        prefs.edit().remove(KEY_HOST).apply()
    }

    companion object {
        const val PREFS_NAME = "ping_settings"
        const val KEY_HOST = "host"
    }
}
