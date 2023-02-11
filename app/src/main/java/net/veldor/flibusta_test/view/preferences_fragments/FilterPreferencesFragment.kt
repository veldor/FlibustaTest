package net.veldor.flibusta_test.view.preferences_fragments

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.view.FilterActivity
import net.veldor.flibusta_test.view.SearchActivity

class FilterPreferencesFragment: PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_filter, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        activity?.setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.subtitle = getString(R.string.filter_preferences)
        val showFilterRulesPref = findPreference<Preference>("show filter rules")
        showFilterRulesPref?.setOnPreferenceClickListener {
        val intent = Intent(requireContext(), FilterActivity::class.java)
        startActivity(intent)
        activity?.finish()
            return@setOnPreferenceClickListener true
        }
    }
}