package net.veldor.flibusta_test.model.worker

import android.content.Context
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.model.handler.NotificationHandler
import net.veldor.flibusta_test.model.handler.TorHandler
import net.veldor.flibusta_test.model.web.UniversalWebClient

class StartTorWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    private var lastLog: String? = null

    override fun doWork(): Result {
        val info = createForegroundInfo()
        setForegroundAsync(info)

        // handle tor load status change
        val h = Handler(App.instance.mainLooper)
        var myTimer: CountDownTimer? = null
        val myRunnable = Runnable {
            kotlin.run {
                myTimer = object : CountDownTimer(60000, 100) {
                    override fun onTick(millisUntilFinished: Long) {
                        if(!this@StartTorWorker.isStopped){
                            if (TorHandler.instance.lastLog != null && lastLog != TorHandler.instance.lastLog) {
                                if (TorHandler.instance.lastLog!!.contains("Proxy Client: unable to connect OR connection (handshaking (proxy)) with")) {
                                    NotificationHandler.instance.showBridgesError()
                                }
                                NotificationHandler.instance.updateTorStarter(TorHandler.instance.lastLog!!)
                                lastLog = TorHandler.instance.lastLog
                            }
                        }
                        else{
                            NotificationHandler.instance.cancelTorLoadNotification()
                        }
                    }

                    override fun onFinish() {}
                }
                myTimer?.start()
            }
        }
        h.post(myRunnable)

        try {
            Log.d("surprise", "doWork: launch TOR")
            TorHandler.instance.start()
            Log.d("surprise", "StartTorWorker.kt 14: tor is work")
        } catch (e: Throwable) {
            Log.d("surprise", "StartTorWorker.kt 17: tor start error")
            // не получилось запустить
            e.printStackTrace()
            UniversalWebClient.connectionError.postValue(e)
        }
        myTimer?.onFinish()
        myTimer?.cancel()
        NotificationHandler.instance.cancelTorLoadNotification()
        if(isStopped){
            return Result.failure()
        }
        return Result.success()
    }

    companion object {
        const val TAG = "start tor"
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notification = NotificationHandler.instance.startTorNotification
        return ForegroundInfo(
            NotificationHandler.START_TOR_WORKER_NOTIFICATION,
            notification
        )
    }
}