package net.veldor.flibusta_test.ui.filter_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceFragmentCompat
import net.veldor.flibusta_test.R


class FilterPrefsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_filter, rootKey)
        activity?.invalidateOptionsMenu()
        setHasOptionsMenu(true)
    }
}