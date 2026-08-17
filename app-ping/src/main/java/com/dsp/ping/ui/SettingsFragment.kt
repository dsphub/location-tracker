package com.dsp.ping.ui

import android.os.Bundle
import android.text.InputType
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import com.dsp.ping.R
import com.dsp.ping.data.SettingsStore
import com.dsp.ping.ping.HostNormalizer

/**
 * Экран настроек: смена сайта. Хост валидируется [HostNormalizer]'ом и пишется
 * в общий с сервисом [SettingsStore] (файл [PREFS_NAME]), поэтому следующий
 * пинг пойдёт уже на новый адрес.
 */
class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Тот же файл, что у SettingsStore: сервис и SetupFragment читают его.
        preferenceManager.sharedPreferencesName = SettingsStore.PREFS_NAME
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<EditTextPreference>(HOST_KEY)?.apply {
            setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_TEXT_VARIATION_URI
                editText.hint = getString(R.string.host_hint)
            }
            // Возвращаем false при невалидном вводе — значение не сохраняется,
            // диалог остаётся открытым на введённом тексте.
            setOnPreferenceChangeListener { _, newValue ->
                HostNormalizer.normalize(newValue.toString()) != null
            }
        }
    }

    companion object {
        private const val HOST_KEY = SettingsStore.KEY_HOST

        fun newInstance() = SettingsFragment()
    }
}
