package com.dsp.ping.ui

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.widget.Toast
import androidx.preference.EditTextPreference
import com.dsp.ping.R
import com.dsp.ping.data.SettingsStore
import com.dsp.ping.ping.PingInterval

/**
 * Период пинга в секундах как [EditTextPreference] с числовым вводом.
 *
 * Значение хранится как строка (контракт [EditTextPreference]), но при чтении
 * сервисом через [SettingsStore.getIntervalSec] зажимается диапазоном
 * [SettingsStore.MIN_INTERVAL_SEC]..[SettingsStore.MAX_INTERVAL_SEC].
 * Здесь же — валидация при вводе и человекочитаемый summary.
 */
class IntervalPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.editTextPreferenceStyle
) : EditTextPreference(context, attrs, defStyleAttr) {

    init {
        // Числовой ввод без знака/дроби.
        setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            editText.hint = context.getString(R.string.pref_interval_hint)
        }
        // Возвращаем false при невалидном вводе — значение не сохраняется;
        // тост подсказывает допустимый диапазон (диалог закрывается без записи).
        setOnPreferenceChangeListener { _, newValue ->
            val valid = PingInterval.parse(newValue?.toString(), MIN_SEC, MAX_SEC) != null
            if (!valid) {
                Toast.makeText(context, R.string.pref_interval_invalid, Toast.LENGTH_SHORT).show()
            }
            valid
        }
    }

    /**
     * Summary в человекочитаемом виде ("1 мин", "2 ч 30 мин" и т.п.).
     * [useSimpleSummaryProvider] не подходит — он выводит сырую строку.
     */
    override fun getSummary(): CharSequence {
        val parsed = PingInterval.parse(text, MIN_SEC, MAX_SEC)
            ?: SettingsStore.DEFAULT_INTERVAL_SEC
        return PingInterval.format(parsed)
    }

    private companion object {
        val MIN_SEC = SettingsStore.MIN_INTERVAL_SEC
        val MAX_SEC = SettingsStore.MAX_INTERVAL_SEC
    }
}
