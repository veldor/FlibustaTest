package net.veldor.flibusta_test.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.ActivityPreferencesBinding
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.view_model.PreferencesViewModel

@Suppress("unused")
class PreferencesActivity : BaseActivity() {
    private lateinit var binding: ActivityPreferencesBinding
    private lateinit var viewModel: PreferencesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        viewModel = ViewModelProvider(this).get(PreferencesViewModel::class.java)
        binding = ActivityPreferencesBinding.inflate(layoutInflater)
        setContentView(binding.drawerLayout)
        setupUI()
        // добавлю главный фрагмент
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.preferences, SettingsFragment())
                .commit()
        }
    }

    override fun setupUI() {
        super.setupUI()
        // скрою переход на данное активити
        val menuNav = mNavigationView.menu
        val item = menuNav.findItem(R.id.goToSettings)
        item.isEnabled = false
        item.isChecked = true
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_root, rootKey)
        }
    }

    class ViewPreferencesFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_view, rootKey)
        }

        override fun onResume() {
            super.onResume()
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
                        if(PreferencesHandler.instance.isEInk){
                            Toast.makeText(requireContext(), getString(R.string.only_light_theme_message), Toast.LENGTH_LONG).show()
                        }
                        else{
                            when (new_value as String) {
                                PreferencesHandler.NIGHT_THEME_SYSTEM -> AppCompatDelegate.setDefaultNightMode(
                                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                                )
                                PreferencesHandler.NIGHT_THEME_DAY -> AppCompatDelegate.setDefaultNightMode(
                                    AppCompatDelegate.MODE_NIGHT_NO
                                )
                                PreferencesHandler.NIGHT_THEME_NIGHT -> AppCompatDelegate.setDefaultNightMode(
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
}