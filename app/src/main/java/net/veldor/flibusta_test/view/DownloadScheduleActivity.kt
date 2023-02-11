package net.veldor.flibusta_test.view

import android.os.Bundle
import androidx.core.content.res.ResourcesCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.ActivityDownloadScheduleBinding
import net.veldor.flibusta_test.model.db.DatabaseInstance
import net.veldor.flibusta_test.model.handler.PreferencesHandler

class DownloadScheduleActivity : BaseActivity() {
    private lateinit var binding: ActivityDownloadScheduleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDownloadScheduleBinding.inflate(layoutInflater)
        setContentView(binding.drawerLayout)
        setupUI()
        setupObservers()

        // setup bottom menu
        binding.includedBnv.bottomNavView
        val fragment: NavHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController: NavController = fragment.navController
        binding.includedBnv.bottomNavView.setupWithNavController(navController)
        binding.includedBnv.bottomNavView.menu.clear()
        binding.includedBnv.bottomNavView.inflateMenu(R.menu.download_schedule_bottom_nav_menu)
        binding.includedBnv.bottomNavView.setOnItemReselectedListener {}
        navController.graph = navController.navInflater.inflate(R.navigation.download_schedule_navigation)
    }

    override fun setupUI() {
        super.setupUI()
        // скрою переход на данное активити
        val menuNav = mNavigationView.menu
        val item = menuNav.findItem(R.id.goToDownloadsList)
        item.isEnabled = false
        item.isChecked = true
    }

    private fun setupObservers() {
        DatabaseInstance.mDatabase.booksDownloadScheduleDao().allBooksLive?.observe(this) {
            if (it.isNotEmpty()) {
                val badge = binding.includedBnv.bottomNavView.getOrCreateBadge(R.id.navigation_schedule_list)
                badge.number =
                    it.size
                if(PreferencesHandler.isEInk){
                    badge.backgroundColor = ResourcesCompat.getColor(resources, R.color.black, theme)
                    badge.badgeTextColor = ResourcesCompat.getColor(resources, R.color.white, theme)
                }
            } else {
                binding.includedBnv.bottomNavView.removeBadge(R.id.navigation_schedule_list)
            }
        }
        DatabaseInstance.mDatabase.downloadErrorDao().allBooksLive?.observe(this) {
            if (it.isNotEmpty()) {
                val badge = binding.includedBnv.bottomNavView.getOrCreateBadge(R.id.navigation_schedule_errors_list)
                badge.number =
                    it.size
                if(PreferencesHandler.isEInk){
                    badge.backgroundColor = ResourcesCompat.getColor(resources, R.color.black, theme)
                    badge.badgeTextColor = ResourcesCompat.getColor(resources, R.color.white, theme)
                }
            } else {
                binding.includedBnv.bottomNavView.removeBadge(R.id.navigation_schedule_errors_list)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // check target fragment
        if (intent.getStringExtra(EXTRA_TARGET_FRAGMENT) != null) {
            when (intent.getStringExtra(EXTRA_TARGET_FRAGMENT)) {
                TARGET_ERRORS_FRAGMENT -> {
                    binding.includedBnv.bottomNavView.selectedItemId = R.id.navigation_schedule_errors_list
                }
            }
        }
    }

    companion object {
        const val EXTRA_TARGET_FRAGMENT = "target fragment"
        const val TARGET_ERRORS_FRAGMENT = "target errors"
    }
}