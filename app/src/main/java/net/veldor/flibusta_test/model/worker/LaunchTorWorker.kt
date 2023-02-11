package net.veldor.flibusta_test.model.worker

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import net.veldor.flibusta_test.model.handler.NotificationHandler
import net.veldor.flibusta_test.model.handler.TorHandler

class LaunchTorWorker(private val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    var resultReady = false
    var launchResult: Boolean? = null

    override fun doWork(): Result {
        isInterrupted = false
        Thread.interrupted()
        val info = createForegroundInfo()
        setForegroundAsync(info)
        val h = Thread()
        mutableStatus.postValue(STATUS_LAUNCHED)
        GlobalScope.async {
            launchResult = TorHandler.launchTor()
            resultReady = true
        }
        while (!resultReady){
            if(isInterrupted){
                mutableStatus.postValue(STATUS_TIMEOUT)
                return Result.success()
            }
            val currentState = TorHandler.currentBootstrapState
            NotificationHandler.updateTorLoadState(currentState)
            Thread.sleep(1000)
        }
        if (launchResult == true) {
            mutableStatus.postValue(STATUS_SUCCESS)
        } else {
            mutableStatus.postValue(STATUS_TIMEOUT)
        }
        return Result.success()
    }

    companion object {
        var isInterrupted: Boolean = false
        const val STATUS_WAITING: Int = 0
        const val STATUS_LAUNCHED: Int = 1
        const val STATUS_TIMEOUT: Int = 2
        const val STATUS_ERROR_LOAD_TELEGRAM_BRIDGES: Int = 3
        const val STATUS_SUCCESS: Int = 4
        const val STATUS_CONNECTION_WRONG_ANSWER: Int = 5
        const val STATUS_CONNECTION_ERROR: Int = 6
        const val STATUS_LAUNCH_INTERRUPTED: Int = 7
        const val WORKER_TAG = "start TOR"
        private val mutableLaunchTime = MutableLiveData<Long>()
        val liveLaunchTime: LiveData<Long> = mutableLaunchTime

        private val mutableStatus = MutableLiveData(STATUS_WAITING)
        val liveStatus: LiveData<Int> = mutableStatus
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notification = NotificationHandler.startTorNotification
        return ForegroundInfo(
            NotificationHandler.LAUNCH_TOR_WORKER_NOTIFICATION, notification
        )
    }
}