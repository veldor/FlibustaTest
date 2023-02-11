package net.veldor.flibusta_test

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import com.adobe.dp.fb2.convert.Converter
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.tor_client.model.control.AndroidOnionProxyManager


class App : MultiDexApplication() {

    val torManager: AndroidOnionProxyManager by lazy {
        AndroidOnionProxyManager(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // store TorManager instance
        setupNightMode()
    }

    private fun setupNightMode() {
        when (PreferencesHandler.nightMode) {
            PreferencesHandler.NIGHT_THEME_DAY -> {
                Log.d("surprise", "App: 24 it's day")
                AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_NO
                )
            }
            PreferencesHandler.NIGHT_THEME_NIGHT -> {
                Log.d("surprise", "App: 31 good night")
                AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES
                )
            }
            else -> {
                Log.d("surprise", "App: 37 use system color theme")
                AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                )
            }
        }
    }

    companion object {
        lateinit var instance: App
            private set
    }
}