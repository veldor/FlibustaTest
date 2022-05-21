package net.veldor.flibusta_test

import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import net.veldor.flibusta_test.model.handler.OpdsResultsHandler
import net.veldor.flibusta_test.model.handler.PreferencesHandler


class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        // got instance
        instance = this
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