package net.veldor.flibusta_test.view.preferences_fragments

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.view.components.SelectDirDialog
import java.util.*

class DownloadPreferencesFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_download, rootKey)
    }

    private fun showCurrentDirPath(dirPreference: Preference?) {
        Log.d("surprise", "DownloadPreferencesFragment: 17 update storage info")
        if (PreferencesHandler.storageAccessDenied) {
            dirPreference?.summary = getString(R.string.access_revoced_title)
        } else {
            dirPreference?.summary = String.format(
                Locale.ENGLISH,
                getString(R.string.current_dir_pattern),
                PreferencesHandler.rootDownloadDirPath
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        activity?.setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.subtitle = getString(R.string.download_preferences)
        // handle download dir selector
        val changeDownloadDirPreference = findPreference<Preference>("download dir location")
        showCurrentDirPath(changeDownloadDirPreference)
        changeDownloadDirPreference?.setOnPreferenceClickListener {
            val dialog = SelectDirDialog()
            dialog.showNow(requireActivity().supportFragmentManager, SelectDirDialog.TAG) {
                showCurrentDirPath(changeDownloadDirPreference)
            }
            return@setOnPreferenceClickListener true
        }
    }
}