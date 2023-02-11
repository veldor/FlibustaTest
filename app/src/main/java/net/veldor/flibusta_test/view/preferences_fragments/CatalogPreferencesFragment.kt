package net.veldor.flibusta_test.view.preferences_fragments

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import net.veldor.flibusta_test.R

class CatalogPreferencesFragment: PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_catalog, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        activity?.setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.subtitle = getString(R.string.catalog_preferences_title)
    }
}