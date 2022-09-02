package net.veldor.flibusta_test.ui

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.ActivityBrowserBinding
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.view_model.WebViewViewModel
import net.veldor.flibusta_test.ui.browser_fragments.OpdsFragment
import net.veldor.flibusta_test.ui.browser_fragments.WebViewFragment


class BrowserActivity : BaseActivity() {
    var goFromOpds: Boolean = false
    lateinit var viewModel: WebViewViewModel
    lateinit var binding: ActivityBrowserBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(WebViewViewModel::class.java)
        binding = ActivityBrowserBinding.inflate(layoutInflater)
        setContentView(binding.rootView)
        rootView = binding.root
        setupUI()
        // setup bottom menu
        val fragment: NavHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController: NavController = fragment.navController
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_opds,
                R.id.navigation_web_view
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.includedBnv.bottomNavView.setupWithNavController(navController)
        binding.includedBnv.bottomNavView.menu.clear()
        binding.includedBnv.bottomNavView.inflateMenu(R.menu.browser_bottom_nav_menu)
        // disable reselect
        binding.includedBnv.bottomNavView.setOnItemReselectedListener {}
        navController.graph = navController.navInflater.inflate(R.navigation.browser_navigation)
    }

    companion object {
        var EXTERNAL_LINK: String? = null
    }

    override fun setupUI() {
        super.setupUI()
        // скрою переход на данное активити
        val menuNav = mNavigationView.menu
        val item = menuNav.findItem(R.id.goBrowse)
        item.isEnabled = false
        item.isChecked = true
        anchorView = binding.includedBnv.bottomNavView
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START) //CLOSE Nav Drawer!
                return true
            }
        }
        val fragment = getCurrentFragment()
        if (fragment is OpdsFragment) {
            return fragment.keyPressed(keyCode)
        } else if (fragment is WebViewFragment) {
            return fragment.keyPressed(keyCode)
        }
        return super.onKeyDown(keyCode, event)
    }


    fun getCurrentFragment(): Fragment? {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        return navHostFragment?.childFragmentManager?.findFragmentById(R.id.nav_host_fragment)
    }

    fun launchWebViewFromOpds() {
        Log.d("surprise", "BrowserActivity.kt 86: launching from OPDS")
        goFromOpds = true
        binding.includedBnv.bottomNavView.selectedItemId =
            R.id.navigation_web_view
    }

    fun returnToOpds() {
        Log.d("surprise", "BrowserActivity.kt 93: return to OPDS")
        goFromOpds = false
        binding.includedBnv.bottomNavView.selectedItemId =
            R.id.navigation_opds
    }

    override fun onResume() {
        super.onResume()
    }
}