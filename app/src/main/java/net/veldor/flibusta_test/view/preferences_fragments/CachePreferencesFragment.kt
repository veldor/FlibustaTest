package net.veldor.flibusta_test.view.preferences_fragments

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.util.CacheUtils
import net.veldor.flibusta_test.model.view_model.CachePreferencesViewModel
import net.veldor.flibusta_test.model.view_model.SubscriptionsViewModel
import java.util.*

class CachePreferencesFragment: PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_cache, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        activity?.setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.subtitle = getString(R.string.cache_preferences)
        val viewModel = ViewModelProvider(this)[CachePreferencesViewModel::class.java]
        val clearCachePref =
            findPreference<Preference>("clear cache now")
        clearCachePref?.summary = String.format(Locale.ENGLISH, getString(R.string.fill_pattern), CacheUtils.getTotalCacheSize(requireContext()))
        clearCachePref?.setOnPreferenceClickListener {
            Toast.makeText(requireContext(), getString(R.string.clean_initiated_title), Toast.LENGTH_SHORT).show()
            viewModel.clearCache(requireContext()){
                activity?.runOnUiThread{
                    Toast.makeText(requireContext(), getString(R.string.cache_clean_finished_title), Toast.LENGTH_SHORT).show()
                    clearCachePref.summary = String.format(Locale.ENGLISH, getString(R.string.fill_pattern), CacheUtils.getTotalCacheSize(requireContext()))
                }
            }
            return@setOnPreferenceClickListener true
        }

        val maxCacheSizePref =
            findPreference<SeekBarPreference>("max cache size")
        maxCacheSizePref?.summary = "${PreferencesHandler.maxCacheSize} мб"
        maxCacheSizePref?.setOnPreferenceChangeListener { _: Preference?, value: Any? ->
            maxCacheSizePref.summary = (value as Int).toString() + " мб"
            true
        }
        maxCacheSizePref?.showSeekBarValue = true
    }
}