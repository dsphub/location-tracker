package com.dsp.ping.data

import android.content.Context
import android.content.SharedPreferences

class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getHost(): String? = prefs.getString(KEY_HOST, null)

    fun setHost(value: String) {
        prefs.edit().putString(KEY_HOST, value).apply()
    }

    fun clearHost() {
        prefs.edit().remove(KEY_HOST).apply()
    }

    /**
     * Период пинга в секундах. По умолчанию — [DEFAULT_INTERVAL_SEC] (1 минута).
     * Пишется Settings-экраном как строка (контракт EditTextPreference) и всегда
     * зажимается диапазоном [MIN_INTERVAL_SEC]..[MAX_INTERVAL_SEC] — на случай
     * ручной правки файла prefs или изменения границ между версиями.
     */
    fun getIntervalSec(): Long =
        (prefs.getString(KEY_INTERVAL_SEC, null)?.toLongOrNull() ?: DEFAULT_INTERVAL_SEC)
            .coerceIn(MIN_INTERVAL_SEC, MAX_INTERVAL_SEC)

    fun setIntervalSec(value: Long) {
        val clamped = value.coerceIn(MIN_INTERVAL_SEC, MAX_INTERVAL_SEC)
        prefs.edit().putString(KEY_INTERVAL_SEC, clamped.toString()).apply()
    }

    /**
     * Подписка на смену периода: слушатель вызывается на потоке, изменившем
     * значение (Settings-экран — main). Сервис использует это для перезапуска
     * Rx-таймера без рестарта. Передача null снимает подписку.
     */
    fun onIntervalChange(listener: (() -> Unit)?) {
        intervalListener = listener
    }

    private var intervalListener: (() -> Unit)? = null

    init {
        // Экран Settings пишет в prefs напрямую (контракт PreferenceFragment),
        // поэтому отслеживаем изменение ключа на уровне SharedPreferences.
        prefs.registerOnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_INTERVAL_SEC) intervalListener?.invoke()
        }
    }

    companion object {
        const val PREFS_NAME = "ping_settings"
        const val KEY_HOST = "host"
        const val KEY_INTERVAL_SEC = "interval_sec"

        /** Минимальный период пинга: 1 секунда. */
        const val MIN_INTERVAL_SEC = 1L

        /** Максимальный период пинга: 1 день (86400 секунд). */
        const val MAX_INTERVAL_SEC = 24L * 60L * 60L

        /** Период пинга по умолчанию: 1 минута (60 секунд). */
        const val DEFAULT_INTERVAL_SEC = 60L
    }
}
