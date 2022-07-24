package net.veldor.flibusta_test.model.handler

import android.app.Activity
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.net.Uri
import android.os.Build
import android.view.MenuItem
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.android.material.navigation.NavigationView
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.dialog.DonationDialog
import net.veldor.flibusta_test.model.worker.SendLogWorker
import net.veldor.flibusta_test.ui.*

class NavigatorSelectHandler(private val mContext: Activity) :
    NavigationView.OnNavigationItemSelectedListener {
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.goBrowse -> {
                val intent = Intent(mContext, BrowserActivity::class.java)
                intent.flags = FLAG_ACTIVITY_CLEAR_TOP
                mContext.startActivity(intent)
                mContext.finish()
            }
            R.id.goToDownloadsList -> {
                val intent = Intent(mContext, DownloadScheduleActivity::class.java)
                mContext.startActivity(intent)
                tryCloseDrawer()
            }
            R.id.goToFileList -> {
                val intent = Intent(mContext, DownloadDirContentActivity::class.java)
                mContext.startActivity(intent)
                tryCloseDrawer()
            }
            R.id.goToSubscriptions -> {
                val intent = Intent(mContext, SubscribesActivity::class.java)
                mContext.startActivity(intent)
                tryCloseDrawer()
            }
            R.id.goToBlacklist -> {
                val intent = Intent(mContext, FilterActivity::class.java)
                mContext.startActivity(intent)
                tryCloseDrawer()
            }
            R.id.goToBookmarks -> {
                val intent = Intent(mContext, BookmarksActivity::class.java)
                mContext.startActivity(intent)
                tryCloseDrawer()
            }
            R.id.goToSettings -> {
                val intent = Intent(mContext, PreferencesActivity::class.java)
                mContext.startActivity(intent)
                tryCloseDrawer()
            }
            R.id.buyCoffee -> {
                DonationDialog.Builder(mContext).build().show()
                tryCloseDrawer()
            }
            R.id.testAppInvite -> {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://t.me/flibusta_downloader_beta")
                mContext.startActivity(intent)
            }
            R.id.sendLog -> {
                val work = OneTimeWorkRequest.Builder(SendLogWorker::class.java).build()
                WorkManager.getInstance(App.instance).enqueue(work)
                tryCloseDrawer()
            }
            R.id.exitApp -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mContext.finishAndRemoveTask()
                } else {
                    mContext.finishAffinity()
                }
                Runtime.getRuntime().exit(0)
            }
        }
        return false
    }

    private fun tryCloseDrawer() {
        val drawer: DrawerLayout = mContext.findViewById(R.id.drawer_layout)
        drawer.closeDrawer(GravityCompat.START)
    }
}