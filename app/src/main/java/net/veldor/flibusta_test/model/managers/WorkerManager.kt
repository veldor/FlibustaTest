package net.veldor.flibusta_test.model.managers

import android.content.Context
import androidx.work.WorkInfo

import androidx.work.WorkManager
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException


class WorkerManager {
    fun isWorkScheduled(tag: String, context: Context): Boolean {
        val instance = WorkManager.getInstance(context)
        val statuses: ListenableFuture<List<WorkInfo>> = instance.getWorkInfosByTag(tag)
        return try {
            var running = false
            val workInfoList: List<WorkInfo> = statuses.get()
            for (workInfo in workInfoList) {
                val state = workInfo.state
                running = state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED
            }
            running
        } catch (e: ExecutionException) {
            e.printStackTrace()
            false
        } catch (e: InterruptedException) {
            e.printStackTrace()
            false
        }
    }
}