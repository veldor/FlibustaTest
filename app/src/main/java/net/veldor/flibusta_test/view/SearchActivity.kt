package net.veldor.flibusta_test.view

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.ActivitySearchBinding
import net.veldor.flibusta_test.view.search_fragment.OpdsFragment
import net.veldor.flibusta_test.view.search_fragment.WebViewFragment

class SearchActivity : BaseActivity() {
     var openedFromOpds: Boolean = false
    internal lateinit var mBinding: ActivitySearchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        mBinding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        rootView = mBinding.rootView

        setupUI()
        // setup bottom menu
        val fragment: NavHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController: NavController = fragment.navController
        mBinding.includedBnv.bottomNavView.setupWithNavController(navController)
        mBinding.includedBnv.bottomNavView.menu.clear()
        mBinding.includedBnv.bottomNavView.inflateMenu(R.menu.search_bottom_nav_menu)
        mBinding.includedBnv.bottomNavView.setOnItemReselectedListener {}
        navController.graph = navController.navInflater.inflate(R.navigation.browser_navigation)
    }

    override fun setupUI() {
        super.setupUI()
        val menuNav = mNavigationView.menu
        val item = menuNav.findItem(R.id.goBrowse)
        item.isEnabled = false
        item.isChecked = true
        anchorView = mBinding.includedBnv.bottomNavView
    }

    fun launchWebViewFromOpds() {
        Log.d("surprise", "BrowserActivity.kt 86: launching from OPDS")
        openedFromOpds = true
        mBinding.includedBnv.bottomNavView.selectedItemId =
            R.id.navigation_web_view
    }
    fun returnToOpds() {
        Log.d("surprise", "BrowserActivity.kt 93: return to OPDS")
        openedFromOpds = false
        mBinding.includedBnv.bottomNavView.selectedItemId =
            R.id.navigation_opds
    }

    fun getCurrentFragment(): Fragment? {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        return navHostFragment?.childFragmentManager?.findFragmentById(R.id.nav_host_fragment)
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mBinding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                mBinding.drawerLayout.closeDrawer(GravityCompat.START) //CLOSE Nav Drawer!
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

    fun openInOpds(url: String) {
        val fragment = getCurrentFragment()
        if (fragment is OpdsFragment) {
            fragment.open(url)
        }
    }

    companion object {
        const val EXTERNAL_LINK = "external link"
    }
}