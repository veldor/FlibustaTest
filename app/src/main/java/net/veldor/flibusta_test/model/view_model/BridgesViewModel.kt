package net.veldor.flibusta_test.model.view_model

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.veldor.flibusta_test.model.handler.*

class BridgesViewModel : ViewModel() {

    var currentBridgesCheckWork: Job? = null
    val liveCheckState = MutableLiveData(STATE_PENDING)

    fun checkBridges(bridgesData: CharSequence?) {
        if (!bridgesData.isNullOrEmpty()) {
            currentBridgesCheckWork = viewModelScope.launch(Dispatchers.IO) {
                TorHandler.instance.stop()
                liveCheckState.postValue(STATE_RUNNING)
                // try to connect tor with new bridges
                BridgesHandler().saveBridgesToFile(bridgesData.toString())
                PreferencesHandler.instance.useCustomBridges = true
                try {
                    TorHandler.instance.start()
                    if (TorHandler.instance.isTorWorks()) {
                        Log.d("surprise", "checkBridges: TOR IS RUNNING")
                        PreferencesHandler.instance.customBridges = bridgesData.toString()
                        liveCheckState.postValue(STATE_SUCCESS)
                        return@launch
                    }
                } catch (e: Throwable) {
                    Log.d("surprise", "checkBridges: i catch error!")
                }
                PreferencesHandler.instance.useCustomBridges = false
                BridgesHandler().getBridges()
                liveCheckState.postValue(STATE_FAILED)
            }
        }
    }

    fun cancelTorLaunch(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            TorHandler.instance.cancelLaunch(context)
        }
    }

    companion object {
        const val STATE_PENDING = 0
        const val STATE_RUNNING = 1
        const val STATE_SUCCESS = 2
        const val STATE_FAILED = 3
    }

}