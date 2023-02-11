package net.veldor.flibusta_test.view.preferences_fragments

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.view.components.BackupPreferencesDialog
import net.veldor.flibusta_test.view.components.TorBridgesSetupDialog

class ConnectionPreferencesFragment: PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_connection, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        activity?.setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        (activity as AppCompatActivity?)?.supportActionBar?.subtitle = getString(R.string.connection_preferences)
        val switchConnectModePreference = findPreference<DropDownPreference>("connect mode")
        if(PreferencesHandler.connectionType == PreferencesHandler.CONNECTION_MODE_TOR){
            switchConnectModePreference?.setDefaultValue("Tor")
        }
        else{
            switchConnectModePreference?.setDefaultValue("VPN")
        }
        switchConnectModePreference?.setOnPreferenceChangeListener { preference, newValue ->
            if(newValue == 1){
                PreferencesHandler.connectionType = PreferencesHandler.CONNECTION_MODE_VPN
            }
            else{
                PreferencesHandler.connectionType = PreferencesHandler.CONNECTION_MODE_TOR
            }
            return@setOnPreferenceChangeListener true
        }

        val bridgesSetupPreference = findPreference<Preference>("setup bridges")
        bridgesSetupPreference?.setOnPreferenceClickListener {
            val dialog = TorBridgesSetupDialog()
            TorBridgesSetupDialog.callback = null
            dialog.showNow(requireActivity().supportFragmentManager, TorBridgesSetupDialog.TAG)
            return@setOnPreferenceClickListener true
        }
    }
}