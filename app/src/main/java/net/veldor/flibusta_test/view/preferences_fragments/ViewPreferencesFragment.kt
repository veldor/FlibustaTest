package net.veldor.flibusta_test.view.preferences_fragments

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.handler.PreferencesHandler

class ViewPreferencesFragment: PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_view, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        activity?.setTheme(R.style.AppTheme)
        activity?.setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.subtitle = getString(R.string.view_preferences)
        val switchViewPref = findPreference<Preference>("is eInk")
        val switchNightModePref = findPreference<DropDownPreference>("night theme")
        if (switchViewPref != null) {
            switchViewPref.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _: Preference?, _: Any? ->
                    requireActivity().recreate()
                    true
                }
        }
        if (switchNightModePref != null) {
            switchNightModePref.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, new_value ->
                    when (new_value as String) {
                        PreferencesHandler.NIGHT_THEME_SYSTEM -> {
                            PreferencesHandler.nightMode = PreferencesHandler.NIGHT_THEME_SYSTEM
                            AppCompatDelegate.setDefaultNightMode(
                                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                            )
                        }
                        PreferencesHandler.NIGHT_THEME_DAY -> {
                            PreferencesHandler.nightMode = PreferencesHandler.NIGHT_THEME_DAY
                            AppCompatDelegate.setDefaultNightMode(
                                AppCompatDelegate.MODE_NIGHT_NO
                            )
                        }
                        PreferencesHandler.NIGHT_THEME_NIGHT -> {
                            PreferencesHandler.nightMode = PreferencesHandler.NIGHT_THEME_NIGHT
                            AppCompatDelegate.setDefaultNightMode(
                                AppCompatDelegate.MODE_NIGHT_YES
                            )
                        }
                    }
                    requireActivity().recreate()
                    true
                }
        }
    }
}