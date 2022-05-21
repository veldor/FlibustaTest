package net.veldor.flibusta_test.ui

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.ActivityFilterBinding

class FilterActivity : BaseActivity() {
    private lateinit var binding: ActivityFilterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        binding = ActivityFilterBinding.inflate(layoutInflater)
        setContentView(binding.drawerLayout)
        setupUI()

        // скрою переход на данное активити
        val menuNav = mNavigationView.menu
        val item = menuNav.findItem(R.id.goToBlacklist)
        item.isEnabled = false
        item.isChecked = true

        // setup bottom menu
        binding.bottomNavView
        val fragment: NavHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController: NavController = fragment.navController
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_filter_preferences,
                R.id.navigation_filter_rules
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNavView.setupWithNavController(navController)
    }
}