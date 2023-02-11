package net.veldor.flibusta_test.view.search_fragment

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.preference.*
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.view.DownloadBookSetupActivity

class DownloadPreferencesFragment : PreferenceFragmentCompat() {
    var prefChangedDelegate: DownloadBookSetupActivity? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.quick_preferences_download, rootKey)

    }

    override fun onResume() {
        super.onResume()

        val skipBookSetupPref = findPreference<SwitchPreferenceCompat>("skip download setup")
        val rememberFavoriteMimePref =
            findPreference<SwitchPreferenceCompat>("remember favorite format")
        skipBookSetupPref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue == true) {
                rememberFavoriteMimePref?.isChecked = true
            }
            return@setOnPreferenceChangeListener true
        }

        val sendToKindlePref = findPreference<SwitchPreferenceCompat>("send to kindle")
        sendToKindlePref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue == true) {
                try {
                    App.instance.packageManager.getPackageInfo("com.amazon.kindle", 0)
                } catch (e: PackageManager.NameNotFoundException) {
                    Toast.makeText(
                        requireContext(),
                        "Install Amazon Kindle app",
                        Toast.LENGTH_SHORT
                    ).show()
                    try {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=com.amazon.kindle")
                            )
                        )
                    } catch (e: ActivityNotFoundException) {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=com.amazon.kindle")
                            )
                        )
                    }
                }
            }
            return@setOnPreferenceChangeListener true
        }

        if (prefChangedDelegate != null) {
            val prefScreen: PreferenceScreen = preferenceScreen
            val prefCount: Int = prefScreen.preferenceCount
            for (i in 0 until prefCount) {
                val pref: Preference = prefScreen.getPreference(i)
                if (pref is PreferenceCategory) {
                    recursiveAddListener(pref)
                } else {
                    pref.onPreferenceChangeListener = prefChangedDelegate
                }
            }
        }
    }

    private fun recursiveAddListener(pref: Preference) {
        val prefCount: Int = (pref as PreferenceCategory).preferenceCount
        for (i in 0 until prefCount) {
            val innerPref: Preference = pref.getPreference(i)
            if (innerPref is PreferenceCategory) {
                recursiveAddListener(innerPref)
            } else {
                innerPref.onPreferenceChangeListener = prefChangedDelegate
            }
        }
    }
}