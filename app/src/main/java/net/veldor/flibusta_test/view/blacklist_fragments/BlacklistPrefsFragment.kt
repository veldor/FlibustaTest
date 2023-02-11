package net.veldor.flibusta_test.view.blacklist_fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import net.veldor.flibusta_test.R


class BlacklistPrefsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_filter, rootKey)
        activity?.invalidateOptionsMenu()
    }
}