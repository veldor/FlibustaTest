package net.veldor.flibusta_test.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.db.DatabaseInstance
import net.veldor.flibusta_test.model.handler.NavigatorSelectHandler
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import java.util.*


open class BaseActivity : AppCompatActivity() {

    companion object {
        private var lastDownloadScheduleSize: Int = -1
    }

    private var bookAddedSnackbar: Snackbar? = null
    var rootView: View? = null
    var anchorView: View? = null
    protected lateinit var mNavigationView: NavigationView
    private lateinit var mDownloadsListTextView: TextView
    private lateinit var mSubscriptionsListTextView: TextView
    private var mDrawer: DrawerLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
    }

    protected open fun setupUI() {
        // включу поддержку тулбара
        val toolbar = if (PreferencesHandler.instance.isEInk) {
            findViewById(R.id.einkToolbar)
        } else {
            findViewById<Toolbar>(R.id.toolbar)
        }
        toolbar?.visibility = View.VISIBLE
        toolbar?.let { setSupportActionBar(it) }

        if (PreferencesHandler.instance.isEInk) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (App.instance.resources.configuration.uiMode and
                        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO
            ) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
            val myColorStateList = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(),
                ), intArrayOf(
                    ResourcesCompat.getColor(resources, R.color.invertable_black, theme),
                    ResourcesCompat.getColor(resources, R.color.dark_gray, theme),
                )
            )
            val bottomMenu = findViewById<BottomNavigationView?>(R.id.bottom_nav_view)
            bottomMenu?.setBackgroundColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.white,
                    theme
                )
            )
            bottomMenu?.itemIconTintList = myColorStateList
            bottomMenu?.itemTextColor = myColorStateList
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimary)
                window.navigationBarColor = ContextCompat.getColor(this, R.color.colorPrimary)
            }
        }

        mDrawer = findViewById(R.id.drawer_layout)
        if (mDrawer != null) {
            val toggle = ActionBarDrawerToggle(
                this,
                mDrawer,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
            )
            mDrawer!!.addDrawerListener(toggle)
            toggle.isDrawerIndicatorEnabled = true
            toggle.syncState()
            mNavigationView = findViewById(R.id.reusable_navigation)
            mNavigationView.setNavigationItemSelectedListener(NavigatorSelectHandler(this))
            // отображу бейджи в меню
            mNavigationView.menu.findItem(R.id.goToDownloadsList).actionView
            mDownloadsListTextView =
                mNavigationView.menu.findItem(R.id.goToDownloadsList).actionView as TextView
            mSubscriptionsListTextView =
                mNavigationView.menu.findItem(R.id.goToSubscriptions).actionView as TextView
            mNavigationView.menu.findItem(R.id.appVersion).title = String.format(
                Locale.ENGLISH,
                getString(R.string.application_version_message),
                PreferencesHandler.instance.appVersion
            )
            if (!PreferencesHandler.instance.savingLogs) {
                mNavigationView.menu.findItem(R.id.sendLog).isVisible = false
            }
            // метод для счетчиков
            initializeCountDrawer()
        }

        val jokeText = mNavigationView.getHeaderView(0)

        if(PreferencesHandler.instance.isEInk){
            jokeText.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.white, theme))
        }

        jokeText?.setOnClickListener {
            Toast.makeText(this, "ММ, звёздочки! Что бы это могло означать ¯\\_(ツ)_/¯", Toast.LENGTH_LONG).show()
            //showDisableJokeDialog()
        }
    }

    private fun showDisableJokeDialog() {
        val view = layoutInflater.inflate(R.layout.disable_joke_dialog, null)
        val counter = view.findViewById<TextView>(R.id.counter)
        val dialog = AlertDialog.Builder(this, R.style.dialogTheme)
            .setTitle("Подтвердите действие")
            .setView(view)
            .setPositiveButton("Да", null)
            .setNegativeButton("Нет", null)
            .create()

        dialog.setOnShowListener {
            val button: Button =
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                PreferencesHandler.instance.forbidden = true
                counter.visibility = View.VISIBLE
                val timer = object : CountDownTimer(3000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        counter.text = (counter.text.toString().toInt() - 1).toString()
                    }
                    override fun onFinish() {
                        throw Throwable()
                    }
                }
                timer.start()
            }
        }
        dialog.show()
    }

    private fun initializeCountDrawer() {
        DatabaseInstance.instance.mDatabase.booksDownloadScheduleDao().allBooksLive?.observe(this) {
            if (it.isNotEmpty()) {
                if (lastDownloadScheduleSize < it.size) {
                    showBookAddedSnackbar()
                }
                lastDownloadScheduleSize = it.size
                mDownloadsListTextView.visibility = View.VISIBLE
                mDownloadsListTextView.gravity = Gravity.CENTER_VERTICAL
                mDownloadsListTextView.setTypeface(null, Typeface.BOLD)
                mDownloadsListTextView.setTextColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.book_name_color,
                        null
                    )
                )
                mDownloadsListTextView.text = it.size.toString()
            } else {
                mDownloadsListTextView.visibility = View.INVISIBLE
            }
        }
    }

    private fun showBookAddedSnackbar() {
        if (rootView != null) {
            if (bookAddedSnackbar == null) {
                bookAddedSnackbar =
                    Snackbar.make(
                        rootView!!,
                        R.string.book_added_to_download_title,
                        3000
                    )
                bookAddedSnackbar?.setAction(getString(R.string.show_download_schedule_message)) {
                    val intent = Intent(this, DownloadScheduleActivity::class.java)
                    startActivity(intent)
                }
            }

            if (PreferencesHandler.instance.isEInk) {
                bookAddedSnackbar?.setBackgroundTint(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.white,
                        theme
                    )
                )
                bookAddedSnackbar?.setActionTextColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.invertable_black,
                        theme
                    )
                )
            } else {
                bookAddedSnackbar?.setActionTextColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.always_white,
                        theme
                    )
                )
            }

            if (anchorView != null) {
                bookAddedSnackbar?.anchorView = anchorView!!
            }
            if (bookAddedSnackbar?.isShown != true) {
                bookAddedSnackbar?.show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (PreferencesHandler.instance.hardwareAcceleration) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            )
        } else {
            window.clearFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            )
        }
    }


    override fun onBackPressed() {
        if (mDrawer != null) {
            if (mDrawer!!.isDrawerOpen(GravityCompat.START)) {
                mDrawer!!.closeDrawer(GravityCompat.START)
            } else {
                super.onBackPressed()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mDrawer != null) {
                if (mDrawer!!.isDrawerOpen(GravityCompat.START)) {
                    mDrawer!!.closeDrawer(GravityCompat.START)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun getTheme(): Resources.Theme {
        if (PreferencesHandler.instance.isEInk) {
            val theme = super.getTheme()
            theme.applyStyle(R.style.EInkAppTheme, true)
            return theme
        }
        return super.getTheme()
    }

    class ResetApp : Runnable {
        override fun run() {
            val intent = Intent(App.instance, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            App.instance.startActivity(intent)
            Runtime.getRuntime().exit(0)
        }
    }
}