package net.veldor.flibusta_test.ui

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.ActivityFilterBinding
import net.veldor.flibusta_test.databinding.ActivitySubscribesBinding

class SubscribesActivity : BaseActivity() {
    private lateinit var binding: ActivitySubscribesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        binding = ActivitySubscribesBinding.inflate(layoutInflater)
        setContentView(binding.drawerLayout)
        setupUI()

        // скрою переход на данное активити
        val menuNav = mNavigationView.menu
        val item = menuNav.findItem(R.id.goToSubscriptions)
        item.isEnabled = false
        item.isChecked = true

        // setup bottom menu
        binding.includedBnv.bottomNavView
        val fragment: NavHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController: NavController = fragment.navController
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_subscribe,
                R.id.navigation_subscribe_results
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.includedBnv.bottomNavView.setupWithNavController(navController)
        binding.includedBnv.bottomNavView.menu.clear()
        binding.includedBnv.bottomNavView.inflateMenu(R.menu.subscribes_bottom_nav_menu)
        binding.includedBnv.bottomNavView.setOnItemReselectedListener {}
        navController.graph = navController.navInflater.inflate(R.navigation.subscribes_navigation)

        if (intent.hasExtra("tab")) {
            if (intent.getStringExtra("tab") == "results") {
                binding.includedBnv.bottomNavView.selectedItemId = R.id.navigation_subscribe_results
            }
        }
    }
}