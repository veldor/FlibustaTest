package net.veldor.flibusta_test.view.preferences_fragments

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.view.components.BackupPreferencesDialog
import net.veldor.flibusta_test.view.components.RestorePreferencesDialog

class BackupPreferencesFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_backup, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        activity?.setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.subtitle = getString(R.string.backup_preferences_title)

        val settingsBackupPref = findPreference<Preference>("backup settings")
        val settingsRestorePref = findPreference<Preference>("restore settings")

        settingsBackupPref?.setOnPreferenceClickListener {
            val dialog = BackupPreferencesDialog()
            dialog.showNow(requireActivity().supportFragmentManager, BackupPreferencesDialog.TAG)
            return@setOnPreferenceClickListener true
        }
        settingsRestorePref?.setOnPreferenceClickListener {
            val dialog = RestorePreferencesDialog()
            dialog.showNow(requireActivity().supportFragmentManager, RestorePreferencesDialog.TAG)
            return@setOnPreferenceClickListener true
        }
    }
}