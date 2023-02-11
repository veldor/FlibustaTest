package net.veldor.flibusta_test.model.handler

import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import net.veldor.flibusta_test.App
import net.veldor.tor_client.model.listeners.BootstrapLoadProgressListener

object TorHandler {

    val currentBootstrapState: String?
    get() {
        val manager = App.instance.torManager
        return manager.lastBootstrapLog
    }
    val isTorRunning: Boolean
        get() {
            val manager = App.instance.torManager
            return manager.isRunning
        }
    private val mutableTorLaunchInProgress: MutableLiveData<Boolean> = MutableLiveData(false)
    private val mutableTorBootstrapped: MutableLiveData<Boolean> = MutableLiveData(false)
    val liveTorLaunchInProgress: LiveData<Boolean> = mutableTorLaunchInProgress
    val liveTorBootstrapped: LiveData<Boolean> = mutableTorBootstrapped

    fun launchTor(): Boolean {
        val manager = App.instance.torManager
        if (manager.isLaunchInProgress) {
            manager.stop()
            Thread.sleep(3000)
        }
        if (manager.isRunning) {
            manager.stop()
            Thread.sleep(3000)
        }
        mutableTorLaunchInProgress.postValue(true)
        val launchResult = manager.startWithRepeat(getTorConnectionTime(), 2)
        mutableTorLaunchInProgress.postValue(false)
        if (launchResult) {
            mutableTorBootstrapped.postValue(true)
        } else {
            mutableTorBootstrapped.postValue(false)
        }
        return launchResult
    }

    fun getTorConnectionTime(): Int {
        if(PreferencesHandler.isEInk){
            return 240
        }
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            return 120
        }
        return 60
    }

    fun stopTor() {
        val manager = App.instance.torManager
        manager.interrupt()
        manager.stop()
        mutableTorBootstrapped.postValue(false)
        Thread.sleep(3000)
    }

    fun setBootstrapLoadProgressListener(listener: BootstrapLoadProgressListener) {
        val manager = App.instance.torManager
        manager.bootstrapLoadProgressListener = listener
    }
}