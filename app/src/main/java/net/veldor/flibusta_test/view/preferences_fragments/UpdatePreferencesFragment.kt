package net.veldor.flibusta_test.view.preferences_fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.view.CheckUpdateActivity

class UpdatePreferencesFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_update, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.setTheme(R.style.AppTheme)
        (activity as AppCompatActivity?)?.supportActionBar?.subtitle = getString(R.string.update_preferences)
        val checkUpdatePref =
            findPreference<Preference>("check update now")

        checkUpdatePref?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), CheckUpdateActivity::class.java))
            return@setOnPreferenceClickListener true
        }

        val showBetaReleasesPref =
            findPreference<Preference>("show all beta updates")
        showBetaReleasesPref?.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://github.com/veldor/FlibustaTest/releases")
            startActivity(intent)
            return@setOnPreferenceClickListener true
        }
        val showStableReleasesPref =
            findPreference<Preference>("show all stable updates")
        showStableReleasesPref?.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://github.com/veldor/FlibustaBookLoader/releases")
            startActivity(intent)
            return@setOnPreferenceClickListener true
        }
    }
}