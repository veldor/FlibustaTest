package net.veldor.flibusta_test.model.worker

import android.content.Context
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import net.veldor.flibusta_test.model.db.DatabaseInstance
import net.veldor.flibusta_test.model.handler.DownloadHandler
import net.veldor.flibusta_test.model.handler.NotificationHandler
import net.veldor.flibusta_test.model.selection.BooksDownloadProgress
import net.veldor.flibusta_test.model.util.CacheUtils

class DownloadBooksWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        val currentProgress = BooksDownloadProgress()
        currentProgress.operationStartTime = System.currentTimeMillis()
        var errorsCounter = 0
        val info = createForegroundInfo()
        try {
            setForegroundAsync(info)
            var booksLoaded = 0
            var bookInProgress =
                DatabaseInstance.mDatabase.booksDownloadScheduleDao().firstQueuedBook
            setForegroundAsync(
                updateForegroundInfo(
                    DatabaseInstance.mDatabase.booksDownloadScheduleDao().queueSize,
                    1,
                    0
                )
            )
            currentProgress.currentlyLoadedBookName = bookInProgress?.name
            currentProgress.booksInQueue =
                DatabaseInstance.mDatabase.booksDownloadScheduleDao().queueSize
            DownloadHandler.liveBookDownloadProgress.postValue(currentProgress)
            while (bookInProgress != null && !isStopped) {
                currentProgress.booksInQueue =
                    DatabaseInstance.mDatabase.booksDownloadScheduleDao().queueSize + booksLoaded + errorsCounter
                if (!DownloadHandler.download(bookInProgress, currentProgress)) {
                    errorsCounter++
                    currentProgress.loadErrors++
                    DownloadHandler.liveBookDownloadProgress.postValue(currentProgress)
                } else {
                    booksLoaded++
                    currentProgress.successLoads++
                    DownloadHandler.liveBookDownloadProgress.postValue(currentProgress)
                }
                DatabaseInstance.mDatabase.booksDownloadScheduleDao()
                    .delete(bookInProgress)
                setForegroundAsync(
                    updateForegroundInfo(
                        DatabaseInstance.mDatabase.booksDownloadScheduleDao().queueSize + booksLoaded + errorsCounter,
                        booksLoaded + errorsCounter + 1,
                        errorsCounter
                    )
                )
                bookInProgress =
                    DatabaseInstance.mDatabase.booksDownloadScheduleDao().firstQueuedBook
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        DownloadHandler.downloadFinished()
        NotificationHandler.showDownloadFinishedNotification(currentProgress)
        CacheUtils.requestClearCache()
        return Result.success()
    }

    private fun createForegroundInfo(): ForegroundInfo {
        // Build a notification
        val notification = NotificationHandler.createDownloadNotification()
        return ForegroundInfo(NotificationHandler.DOWNLOAD_PROGRESS_NOTIFICATION, notification)
    }

    private fun updateForegroundInfo(total: Int, loadedNow: Int, errors: Int): ForegroundInfo {
        // Build a notification
        val notification =
            NotificationHandler.createDownloadNotification(total, loadedNow, errors)
        return ForegroundInfo(NotificationHandler.DOWNLOAD_PROGRESS_NOTIFICATION, notification)
    }
}