package net.veldor.flibusta_test.view.preferences_fragments

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.view.components.ConnectCompanionAppDialog
import java.util.*

class CompanionAppPreferencesFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_companion_app, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        activity?.setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.subtitle = getString(R.string.companion_app_preferences)
        val connectPref = findPreference<Preference>("pair companion app")

        val savedConnection = PreferencesHandler.companionAppCoordinates
        if(savedConnection.isNotEmpty()){
            connectPref?.summary = String.format(Locale.ENGLISH, getString(R.string.current_coordinates_pattern), savedConnection)
        }

        connectPref?.setOnPreferenceClickListener {
            val dialog = ConnectCompanionAppDialog { coordinates ->
                activity?.runOnUiThread {
                    if(coordinates.isNotEmpty()){
                        connectPref.summary = String.format(Locale.ENGLISH, getString(R.string.current_coordinates_pattern), coordinates)
                    }
                }
            }
            dialog.showNow(requireActivity().supportFragmentManager, ConnectCompanionAppDialog.TAG)
            return@setOnPreferenceClickListener true
        }
    }
}