package net.veldor.flibusta_test.model.worker

import android.content.Context
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import net.veldor.flibusta_test.model.handler.NotificationHandler
import net.veldor.flibusta_test.model.handler.SubscribesHandler

class CheckSubscriptionsWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        val info = createForegroundInfo()
        setForegroundAsync(info)
        SubscribesHandler.checkSubscribes(false)
        val resultsSize = SubscribesHandler.subscribeResults.size
        if(resultsSize > 0){
            NotificationHandler.notifySubscriptionsCheck(resultsSize)
        }
        return Result.success()
    }


    private fun createForegroundInfo(): ForegroundInfo {
        val notification = NotificationHandler.checkSubscribesNotification
        return ForegroundInfo(
            NotificationHandler.CHECK_SUBSCRIBES_WORKER_NOTIFICATION,
            notification
        )
    }

    companion object {
        const val PERIODIC_CHECK_TAG = "periodic check subscriptions"
    }
}