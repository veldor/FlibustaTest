package net.veldor.flibusta_test.model.handler

import android.content.Context
import android.util.Log
import androidx.work.*
import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.model.managers.WorkerManager
import net.veldor.flibusta_test.model.web.UniversalWebClient
import net.veldor.flibusta_test.model.worker.StartTorWorker
import java.lang.Exception
import java.util.concurrent.TimeUnit

class TorHandler private constructor() {

    val lastLog: String?
        get() {
            if (tor != null) {
                return tor!!.lastLog
            }
            return null
        }


    private var tor: AndroidOnionProxyManager? = null
    private var startInProgress: Boolean = false

    fun launch(context: Context) {
        if (tor == null || !tor!!.isRunning) {
            // проверю, не запущен ли уже экземляр рабочего
            if (!WorkerManager().isWorkScheduled(StartTorWorker.TAG, context)) {
                val startTorWork = OneTimeWorkRequest.Builder(StartTorWorker::class.java)
                    .addTag(StartTorWorker.TAG)
                    .build()
                WorkManager.getInstance(context)
                    .enqueueUniqueWork(
                        StartTorWorker.TAG,
                        ExistingWorkPolicy.REPLACE,
                        startTorWork
                    )
            } else {
                Log.d("surprise", "launch: work in progress")
            }
        }
    }

    fun start() {
        if (!startInProgress) {
            try {
                startInProgress = true
                if (tor == null) {
                    tor = AndroidOnionProxyManager(App.instance, TOR_FILES_LOCATION)
                }
                // get bridges
                BridgesHandler().getBridges()
                tor!!.startWithRepeat(
                    TOTAL_SECONDS_PER_TOR_STARTUP,
                    TOTAL_TRIES_PER_TOR_STARTUP
                )
            } catch (e: Exception) {
                UniversalWebClient.connectionError.postValue(e)
            }
            startInProgress = false
        }
    }

    fun isTorWorks(): Boolean {
        if (tor != null) {
            return tor!!.isBootstrapped
        }
        return false
    }

    fun getPort(): Int {
        if (tor == null) {
            start()
        }
        return tor!!.iPv4LocalHostSocksPort
    }

    fun checkTorConnection() :Boolean{
        if (tor == null || tor?.isRunning != true || tor?.isNetworkEnabled != true || tor?.isBootstrapped != true) {
            Log.d("surprise", "checkTorConnection: restart tor")
            start()
            return false
        }
        return true
    }


    companion object {
        const val TOR_FILES_LOCATION = "torfiles"
        const val TOTAL_TRIES_PER_TOR_STARTUP = 3
        val TOTAL_SECONDS_PER_TOR_STARTUP = TimeUnit.MINUTES.toSeconds(1).toInt()
        var instance: TorHandler = TorHandler()
            private set
    }
}