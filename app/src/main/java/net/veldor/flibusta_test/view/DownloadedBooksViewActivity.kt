package net.veldor.flibusta_test.view

import android.os.Bundle
import android.view.KeyEvent
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.ActivityDownloadedBookViewBinding
import net.veldor.flibusta_test.view.download_files_fragments.FilesInDirFragment
import net.veldor.flibusta_test.view.search_fragment.OpdsFragment
import net.veldor.flibusta_test.view.search_fragment.WebViewFragment

class DownloadedBooksViewActivity: BaseActivity() {
    private lateinit var mBinding: ActivityDownloadedBookViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        mBinding = ActivityDownloadedBookViewBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        rootView = mBinding.rootView
        setupUI()
        // setup bottom menu
        val fragment: NavHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController: NavController = fragment.navController
        mBinding.includedBnv.bottomNavView.setupWithNavController(navController)
        mBinding.includedBnv.bottomNavView.menu.clear()
        mBinding.includedBnv.bottomNavView.inflateMenu(R.menu.downloaded_files_nav_menu)
        mBinding.includedBnv.bottomNavView.setOnItemReselectedListener {}
        navController.graph = navController.navInflater.inflate(R.navigation.download_files_navigation)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mBinding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                mBinding.drawerLayout.closeDrawer(GravityCompat.START) //CLOSE Nav Drawer!
                return true
            }
        }
        val fragment = getCurrentFragment()
        if (fragment is FilesInDirFragment) {
            return fragment.goBack()
        }
        return super.onKeyDown(keyCode, event)
    }


    fun getCurrentFragment(): Fragment? {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        return navHostFragment?.childFragmentManager?.findFragmentById(R.id.nav_host_fragment)
    }
}