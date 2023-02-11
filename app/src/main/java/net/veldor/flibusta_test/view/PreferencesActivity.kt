package net.veldor.flibusta_test.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.preference.*
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.ActivityPreferencesBinding
import net.veldor.flibusta_test.model.view_model.PreferencesViewModel
import java.util.*


@Suppress("unused")
class PreferencesActivity : BaseActivity() {
    private lateinit var binding: ActivityPreferencesBinding
    private lateinit var viewModel: PreferencesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(PreferencesViewModel::class.java)
        binding = ActivityPreferencesBinding.inflate(layoutInflater)
        setContentView(binding.drawerLayout)
        setupUI()
        // добавлю главный фрагмент
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.preferences, SettingsFragment())
                .commit()
        }
    }

    override fun setupUI() {
        super.setupUI()
        // скрою переход на данное активити
        val menuNav = mNavigationView.menu
        val item = menuNav.findItem(R.id.goToSettings)
        item.isEnabled = false
        item.isChecked = true
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_root, rootKey)
        }

        override fun onResume() {
            super.onResume()
            (activity as AppCompatActivity?)?.supportActionBar?.title = getString(R.string.preferences_title)
            (activity as AppCompatActivity?)?.supportActionBar?.subtitle = null
        }
    }
}