package net.veldor.flibusta_test.ui

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.ActivityDownloadScheduleBinding
import net.veldor.flibusta_test.model.db.DatabaseInstance
import androidx.navigation.ui.setupActionBarWithNavController

class DownloadScheduleActivity : BaseActivity() {
    private lateinit var binding: ActivityDownloadScheduleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDownloadScheduleBinding.inflate(layoutInflater)
        setContentView(binding.drawerLayout)
        setupUI()
        setupObservers()

        // setup bottom menu
        binding.bottomNavView
        val fragment: NavHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController: NavController = fragment.navController
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_schedule_list,
                R.id.navigation_schedule_errors_list,
                R.id.navigation_schedule_statement
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNavView.setupWithNavController(navController)
    }

    override fun setupUI() {
        super.setupUI()
        // скрою переход на данное активити
        val menuNav = mNavigationView.menu
        val item = menuNav.findItem(R.id.goToDownloadsList)
        item.isEnabled = false
        item.isChecked = true
        // активирую кнопку возвращения к предыдущему окну
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupObservers() {
        DatabaseInstance.instance.mDatabase.booksDownloadScheduleDao().allBooksLive?.observe(this) {
            if (it.isNotEmpty()) {
                binding.bottomNavView.getOrCreateBadge(R.id.navigation_schedule_list).number =
                    it.size
            } else {
                binding.bottomNavView.removeBadge(R.id.navigation_schedule_list)
            }
        }
        DatabaseInstance.instance.mDatabase.downloadErrorDao().allBooksLive?.observe(this) {
            if (it.isNotEmpty()) {
                binding.bottomNavView.getOrCreateBadge(R.id.navigation_schedule_errors_list).number =
                    it.size
            } else {
                binding.bottomNavView.removeBadge(R.id.navigation_schedule_errors_list)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // check target fragment
        if (intent.getStringExtra(EXTRA_TARGET_FRAGMENT) != null) {
            when (intent.getStringExtra(EXTRA_TARGET_FRAGMENT)) {
                TARGET_ERRORS_FRAGMENT -> {
                    binding.bottomNavView.selectedItemId = R.id.navigation_schedule_errors_list
                }
            }
        }
    }

    companion object {
        const val EXTRA_TARGET_FRAGMENT = "target fragment"
        const val TARGET_ERRORS_FRAGMENT = "target errors"
    }
}