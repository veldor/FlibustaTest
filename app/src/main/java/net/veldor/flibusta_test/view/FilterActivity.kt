package net.veldor.flibusta_test.view

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.ActivityBlacklistBinding
import net.veldor.flibusta_test.databinding.ActivitySubscriptionBinding

class FilterActivity: BaseActivity() {


    private lateinit var binding: ActivityBlacklistBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlacklistBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()

        // скрою переход на данное активити
        val menuNav = mNavigationView.menu
        val item = menuNav.findItem(R.id.goToBlacklist)
        item.isEnabled = false
        item.isChecked = true

        // setup bottom menu
        binding.includedBnv.bottomNavView
        val fragment: NavHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController: NavController = fragment.navController
        binding.includedBnv.bottomNavView.setupWithNavController(navController)
        binding.includedBnv.bottomNavView.menu.clear()
        binding.includedBnv.bottomNavView.inflateMenu(R.menu.blacklist_bottom_nav_menu)
        binding.includedBnv.bottomNavView.setOnItemReselectedListener {}
        navController.graph = navController.navInflater.inflate(R.navigation.blacklist_navigation)
    }
}