package net.veldor.flibusta_test

import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import net.veldor.flibusta_test.model.handler.LogHandler
import net.veldor.flibusta_test.model.handler.OpdsResultsHandler
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.handler.SubscribesHandler
import net.veldor.flibusta_test.model.utils.CacheUtils
import net.veldor.flibusta_test.model.web.TOR_BROWSER_USER_AGENT


class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        // got instance
        instance = this

        if(PreferencesHandler.instance.forbidden){
            Toast.makeText(this, "Не сегодня", Toast.LENGTH_SHORT).show()
            System.exit(0)
        }

        if(PreferencesHandler.instance.savingLogs){
            LogHandler.getInstance()!!.initLog()
        }

        // set night mode
        when (PreferencesHandler.instance.nightMode) {
            PreferencesHandler.NIGHT_THEME_SYSTEM -> AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            )
            PreferencesHandler.NIGHT_THEME_DAY -> AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_NO
            )
            PreferencesHandler.NIGHT_THEME_NIGHT -> AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_YES
            )
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        CacheUtils.requestClearCache()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        OpdsResultsHandler.instance.clear()
    }

    companion object {
        //todo switch to false on release
        const val isTestVersion = true
        lateinit var instance: App
            private set
    }
}