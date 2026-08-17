package com.dsp.ping.ui

import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.EditTextPreference
import androidx.preference.Preference
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
        // Имя файла prefs — до setPreferencesFromResource: начальные значения
        // (summary) читаются при инфляции, поэтому здесь, а не после открытия экрана.
        // Раньше (в onCreate до super) нельзя: preferenceManager ещё не создан — NPE.
        // Тот же файл, что у SettingsStore: сервис и SetupFragment читают его.
        preferenceManager.sharedPreferencesName = SettingsStore.PREFS_NAME
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<EditTextPreference>(HOST_KEY)?.apply {
            // Summary — текущий хост (или описание опции, если хост не задан).
            // Провайдер опрашивается при каждом notifyChanged(), поэтому summary
            // обновляется сразу после сохранения значения из диалога.
            summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
                preference.text?.takeIf { it.isNotBlank() }
                    ?: preference.context.getString(R.string.pref_host_summary)
            }
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
