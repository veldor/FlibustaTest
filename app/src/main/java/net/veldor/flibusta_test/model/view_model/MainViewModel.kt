package net.veldor.flibusta_test.model.view_model

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.veldor.flibusta_test.model.handler.TorHandler
import net.veldor.flibusta_test.model.handler.UpdateHandler
import net.veldor.flibusta_test.model.worker.LaunchTorWorker
import net.veldor.flibusta_test.view.BaseActivity
import net.veldor.tor_client.model.exceptions.InvalidParsedCaptchaException
import net.veldor.tor_client.model.managers.BridgesManager
import java.net.Socket

class MainViewModel : ViewModel() {
    fun launchTor(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            // if not launch client work in progress- launch it
            val currentWorks =
                WorkManager.getInstance(context).getWorkInfosByTag(LaunchTorWorker.WORKER_TAG)
            val workInfoList: List<WorkInfo> = currentWorks.get()
            for (workInfo in workInfoList) {
                val state = workInfo.state
                Log.d("surprise", "launchTor 24:  have work with state $state")
                if (state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED) {
                    Log.d("surprise", "launchTor 22:  launch worker in progress")
                    return@launch
                }
            }
            val launchWork = OneTimeWorkRequest.Builder(LaunchTorWorker::class.java)
                .addTag(LaunchTorWorker.WORKER_TAG)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    LaunchTorWorker.WORKER_TAG,
                    ExistingWorkPolicy.REPLACE,
                    launchWork
                )
        }
    }

    fun requestOfficialBridges(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = BridgesManager(context).requestOfficialBridgesCaptcha()
                mutableOfficialBridgesCaptcha.postValue(Pair(true, result))
            } catch (t: Throwable) {
                t.printStackTrace()
                mutableOfficialBridgesCaptcha.postValue(Pair(false, null))
            }
        }
    }


    fun sendCaptchaAnswer(parsedValue: String, secretCode: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (BridgesManager(context).getOfficialBridges(parsedValue, secretCode, context)) {
                    launchTor(context)
                }
            } catch (e: InvalidParsedCaptchaException) {
                requestOfficialBridges(context)
            } catch (t: Throwable) {
                mutableOfficialBridgesCaptcha.postValue(Pair(false, null))
                t.printStackTrace()
            }
        }
    }

    fun requestUnofficialBridges(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            if (BridgesManager(context).loadTgBridges(context)) {
                launchTor(context)
            } else {
                requestOfficialBridges(context)
            }
        }
    }

    fun interruptLaunchTor(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            TorHandler.stopTor()
            LaunchTorWorker.isInterrupted = true
        }
    }

    fun relaunchTor(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            WorkManager.getInstance(context).cancelUniqueWork(LaunchTorWorker.WORKER_TAG)
            LaunchTorWorker.isInterrupted = true
            TorHandler.stopTor()
            launchTor(context)
        }
    }

    fun relaunch(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            WorkManager.getInstance(context).cancelUniqueWork(LaunchTorWorker.WORKER_TAG)
            LaunchTorWorker.isInterrupted = true
            TorHandler.stopTor()
            Handler(context.mainLooper).postDelayed(BaseActivity.ResetApp(), 500)
        }
    }

    fun checkTorStatus(callback: (String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            if(TorHandler.liveTorBootstrapped.value == true){
                callback("Tor ready to work")
            }
            else if(TorHandler.liveTorLaunchInProgress.value == true){
                callback("Tor bootstrapping now")
            }
            else{
                callback("Tor stopped")
            }
        }
    }

    fun checkUpdate(callback: (haveUpdate: Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try{
                if (UpdateHandler.checkUpdate()) {
                    callback(true)
                }
                else{
                    callback(false)
                }
            }
            catch (t: Throwable){
                t.printStackTrace()
                callback(false)
            }
        }
    }

    companion object {
        private val mutableOfficialBridgesCaptcha =
            MutableLiveData<Pair<Boolean, Pair<Bitmap?, String?>?>>()
        val liveOfficialBridgesCaptcha: LiveData<Pair<Boolean, Pair<Bitmap?, String?>?>> =
            mutableOfficialBridgesCaptcha
    }
}