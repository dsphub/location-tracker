package com.dsp.ping.ui

import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        // Имя файла prefs должно быть выставлено ДО super.onCreate(): внутри него
        // вызывается onCreatePreferences, и при инфляции preferences.xml преференсы
        // читают начальные значения (summary) уже из этого файла.
        preferenceManager.sharedPreferencesName = SettingsStore.PREFS_NAME
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = super.onCreateView(inflater, container, savedInstanceState).apply {
        // Активити прозрачная (Theme.Ping.Transparent), поэтому фон красит каждый
        // экран сам — как fragment_status/fragment_setup (?android:attr/colorBackground).
        val background = TypedValue()
        requireContext().theme.resolveAttribute(android.R.attr.colorBackground, background, true)
        setBackgroundColor(background.data)
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
