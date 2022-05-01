package net.veldor.flibusta_test.model.handler

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_ONE_SHOT
import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.text.format.Formatter
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.db.entity.DownloadError
import net.veldor.flibusta_test.model.receiver.DownloadBookProcessReceiver
import net.veldor.flibusta_test.model.selections.BooksDownloadProgress
import net.veldor.flibusta_test.ui.DownloadDirContentActivity
import net.veldor.flibusta_test.ui.DownloadScheduleActivity
import net.veldor.flibusta_test.ui.DownloadedBooksActionsActivity
import net.veldor.flibusta_test.ui.DownloadedBooksActionsActivity.Companion.EXTRA_NOTIFICATION_ID
import java.util.*

class NotificationHandler private constructor(val context: Context) {


    companion object {
        private const val BOOKS_CHANNEL_ID = "books"
        private const val FOREGROUND_CHANNEL_ID = "foreground"
        private const val MISC_CHANNEL_ID = "misc"
        private const val BOOK_DOWNLOADS_CHANNEL_ID = "book downloads"

        private const val DOWNLOADED_BOOKS_GROUP = "net.veldor.downloadedBooksGroup"
        private const val BOOKS_ERRORS_GROUP = "download errors"

        const val LOADED_BOOKS_GROUP_NOTIFICATION = 1
        const val DOWNLOAD_PROGRESS_NOTIFICATION = 5
        private const val BOOK_DOWNLOAD_PROGRESS = 11
        const val START_TOR_WORKER_NOTIFICATION = 19
        const val TOR_BRIDGES_ERROR_NOTIFICATION = 23


        private const val START_APP_CODE = 1
        private const val DOWNLOAD_CANCEL_CODE = 2
        private const val DOWNLOAD_PAUSE_CODE = 3
        private const val DOWNLOAD_RESUME_CODE = 4


        @SuppressLint("StaticFieldLeak")
        var instance: NotificationHandler = NotificationHandler(App.instance.applicationContext)
            private set
    }

    private val mNotificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var bookLoadedId = 100
    private var myActionId = 100

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // создам канал уведомлений о скачанных книгах
            var nc = NotificationChannel(
                BOOKS_CHANNEL_ID,
                context.getString(R.string.books_loaded_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nc.description =
                context.getString(R.string.books_loaded_channel_description)
            nc.enableLights(true)
            nc.lightColor = Color.RED
            nc.enableVibration(true)
            mNotificationManager.createNotificationChannel(nc)

            // создам канал фоновых уведомлений
            nc = NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                context.getString(R.string.foreground_channel),
                NotificationManager.IMPORTANCE_MIN
            )
            nc.description =
                context.getString(R.string.foreground_channel_description)
            nc.enableLights(false)
            nc.enableVibration(false)
            mNotificationManager.createNotificationChannel(nc)

            // создам канал различных уведомлений
            nc = NotificationChannel(
                MISC_CHANNEL_ID,
                context.getString(R.string.misc_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nc.description =
                context.getString(R.string.misc_channel_description)
            nc.enableLights(true)
            nc.lightColor = Color.BLUE
            nc.enableVibration(true)
            mNotificationManager.createNotificationChannel(nc)

            nc = NotificationChannel(
                BOOK_DOWNLOADS_CHANNEL_ID,
                context.getString(R.string.books_download_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            nc.enableVibration(false)
            nc.enableLights(false)
            nc.setSound(null, null)
            nc.description =
                context.getString(R.string.books_download_channel_description)
            mNotificationManager.createNotificationChannel(nc)
        }
    }

    val startTorNotification: Notification
        get() {
            val notificationBuilder =
                NotificationCompat.Builder(context, FOREGROUND_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_app_logo)
                    .setContentTitle(
                        App.instance.getString(R.string.start_tor_message)
                    )
                    .setProgress(0, 0, true)
                    .setOngoing(true)
            return notificationBuilder.build()
        }

    fun showBridgesError() {
        val notificationBuilder =
            NotificationCompat.Builder(context, MISC_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_app_logo)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(context.getString(R.string.bridge_not_fresh_notification))
                )
                .setContentTitle(context.getString(R.string.bridge_error_title))
        mNotificationManager.notify(TOR_BRIDGES_ERROR_NOTIFICATION, notificationBuilder.build())
    }

    fun updateTorStarter(lastLog: String) {
        val notificationBuilder =
            NotificationCompat.Builder(context, FOREGROUND_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_app_logo)
                .setProgress(0, 0, true)
                .setContentTitle(App.instance.getString(R.string.start_tor_message))
                .setContentText(lastLog)
                .setOngoing(true)
        mNotificationManager.notify(START_TOR_WORKER_NOTIFICATION, notificationBuilder.build())
    }

    fun cancelTorLoadNotification() {
        mNotificationManager.cancel(START_TOR_WORKER_NOTIFICATION)
    }

    fun createDownloadNotification(): Notification {
        return createDownloadNotification(0, 0, 0)
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    fun createBookLoadingProgressNotification(
        contentLength: Long,
        loaded: Long,
        name: String,
        startTime: Long,
        lastTickTime: Long
    ) {
        val openWindowIntent =
            Intent(context, DownloadScheduleActivity::class.java)
        openWindowIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val showWindowPending = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getActivity(
                context,
                START_APP_CODE,
                openWindowIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            getActivity(
                context,
                START_APP_CODE,
                openWindowIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        if (lastTickTime - startTime < 100) {
            return
        }
        // пересчитаю байты в килобайты
        val total = Formatter.formatFileSize(
            App.instance,
            contentLength
        )
        val nowLoaded = Formatter.formatFileSize(
            App.instance,
            loaded
        )
        var percentDone = 0.0
        if (loaded > 0) {
            percentDone = loaded.toDouble() / contentLength.toDouble() * 100
        }
        var timeLeftInMillis = 0L
        val left = lastTickTime - startTime
        if (percentDone >= 1) {
            val timeForPercent = left / percentDone
            val percentsLeft = 100 - percentDone
            timeLeftInMillis = (percentsLeft * timeForPercent).toLong()
        }
        val timeLeftInSeconds = timeLeftInMillis / 1000
        val textLeft: String = if (timeLeftInSeconds / 60 > 0) {
            (timeLeftInSeconds / 60).toString() + " мин. " + timeLeftInSeconds % 60 + " сек."
        } else {
            (timeLeftInSeconds % 60).toString() + " сек."
        }
        val notificationBuilder =
            NotificationCompat.Builder(context, BOOK_DOWNLOADS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cloud_download_white_24dp)
                .setContentTitle("Качаю $name")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        String.format(
                            Locale.ENGLISH,
                            App.instance.getString(R.string.loaded_message),
                            nowLoaded,
                            total,
                            percentDone,
                            textLeft
                        )
                    )
                )
                .setOngoing(true)
                .setContentIntent(showWindowPending)
                .setProgress(100, percentDone.toInt(), false)
                .setAutoCancel(false)
        if (percentDone > 0) {
            notificationBuilder.setProgress(100, percentDone.toInt(), false)
        } else {
            notificationBuilder.setProgress(100, 0, true)
        }
        mNotificationManager.notify(BOOK_DOWNLOAD_PROGRESS, notificationBuilder.build())
    }

    fun closeBookLoadingProgressNotification() {
        mNotificationManager.cancel(BOOK_DOWNLOAD_PROGRESS)
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    fun createSuccessBookLoadNotification(destinationFile: DocumentFile?) {
        if(destinationFile == null){
            // something wrong
            Log.d("surprise", "createSuccessBookLoadNotification: can't find destination file")
            return
        }
        // add open browser intent
        val openFileBrowserIntent =
            Intent(context, DownloadDirContentActivity::class.java)
        openFileBrowserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val openBrowserPending = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getActivity(
                context,
                myActionId,
                openFileBrowserIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            getActivity(
                context,
                myActionId,
                openFileBrowserIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        myActionId++
        // add share intent
        val shareIntent =
            Intent(context, DownloadedBooksActionsActivity::class.java)
        shareIntent.setDataAndType(destinationFile.uri, destinationFile.type)
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        shareIntent.putExtra(EXTRA_NOTIFICATION_ID, bookLoadedId)
        shareIntent.putExtra(
            DownloadedBooksActionsActivity.EXTRA_TYPE,
            DownloadedBooksActionsActivity.TYPE_SHARE
        )
        val sharePending = if (Build.VERSION.SDK_INT >= 23) {
            getActivity(
                context,
                myActionId,
                shareIntent,
                FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            getActivity(
                context,
                myActionId,
                shareIntent,
                FLAG_ONE_SHOT
            )
        }
        myActionId++
        // add open intent
        val openIntent =
            Intent(context, DownloadedBooksActionsActivity::class.java)
        openIntent.setDataAndType(destinationFile.uri, destinationFile.type)
        openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        openIntent.putExtra(EXTRA_NOTIFICATION_ID, bookLoadedId)
        openIntent.putExtra(
            DownloadedBooksActionsActivity.EXTRA_TYPE,
            DownloadedBooksActionsActivity.TYPE_OPEN
        )
        val openPending = if (Build.VERSION.SDK_INT >= 23) {
            getActivity(
                context,
                myActionId,
                openIntent,
                FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            getActivity(
                context,
                myActionId,
                openIntent,
                FLAG_ONE_SHOT
            )
        }
        myActionId++
        val notificationBuilder =
            NotificationCompat.Builder(context, BOOKS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_book_black_24dp)
                .setContentTitle(
                    context.getString(R.string.success_load_title)
                )
                .setContentIntent(openBrowserPending)
                .setContentText(destinationFile.name)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(destinationFile.name)
                )
                .setDefaults(Notification.DEFAULT_ALL)
                .setGroup(DOWNLOADED_BOOKS_GROUP)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addAction(
                    R.drawable.ic_open_black_24dp,
                    context.getString(R.string.open_title),
                    openPending
                )
                .addAction(
                    R.drawable.ic_share_white_24dp,
                    context.getString(R.string.share_title),
                    sharePending
                )
                .setAutoCancel(true)
        mNotificationManager.notify(bookLoadedId, notificationBuilder.build())
        ++bookLoadedId
        showSuccessBookLoadGroupNotification()
    }

    fun showSuccessBookLoadGroupNotification() {
        val notificationBuilder = NotificationCompat.Builder(context, BOOKS_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.loaded_books_title))
            //set content text to support devices running API level < 24
            .setContentText(context.getString(R.string.loaded_books_title))
            .setSmallIcon(R.drawable.ic_baseline_arrow_downward_24)
            //build summary info into InboxStyle template
            .setStyle(
                NotificationCompat.InboxStyle()
                    .addLine(context.getString(R.string.loaded_books_title))
            )
            //specify which group this notification belongs to
            .setGroup(DOWNLOADED_BOOKS_GROUP)
            //set this notification as the summary for the group
            .setGroupSummary(true)
        mNotificationManager.notify(LOADED_BOOKS_GROUP_NOTIFICATION, notificationBuilder.build())
    }


    @SuppressLint("UnspecifiedImmutableFlag")
    fun showBookDownloadErrorNotification(error: DownloadError) {
        // add reload intent
        val reloadIntent =
            Intent(context, DownloadBookProcessReceiver::class.java)
        reloadIntent.putExtra(EXTRA_NOTIFICATION_ID, bookLoadedId)
        reloadIntent.putExtra(
            DownloadBookProcessReceiver.EXTRA_ACTION,
            DownloadBookProcessReceiver.ACTION_RELOAD
        )
        reloadIntent.putExtra(
            DownloadBookProcessReceiver.EXTRA_BOOK_ID,
            error.bookId
        )
        val reloadPending = if (Build.VERSION.SDK_INT >= 23) {
            PendingIntent.getBroadcast(
                context,
                myActionId,
                reloadIntent,
                FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                context,
                myActionId,
                reloadIntent,
                FLAG_ONE_SHOT
            )
        }
        myActionId++
        // add open intent
        val deleteIntent =
            Intent(context, DownloadBookProcessReceiver::class.java)
        deleteIntent.putExtra(EXTRA_NOTIFICATION_ID, bookLoadedId)
        deleteIntent.putExtra(
            DownloadBookProcessReceiver.EXTRA_ACTION,
            DownloadBookProcessReceiver.ACTION_DELETE
        )
        deleteIntent.putExtra(
            DownloadBookProcessReceiver.EXTRA_BOOK_ID,
            error.bookId
        )
        val deletePending = if (Build.VERSION.SDK_INT >= 23) {
            PendingIntent.getBroadcast(
                context,
                myActionId,
                deleteIntent,
                FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                context,
                myActionId,
                deleteIntent,
                FLAG_ONE_SHOT
            )
        }
        myActionId++
        val openWindowIntent =
            Intent(context, DownloadScheduleActivity::class.java)
        openWindowIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val showWindowPending = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getActivity(
                context,
                START_APP_CODE,
                openWindowIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            getActivity(
                context,
                START_APP_CODE,
                openWindowIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        val downloadScheduleBuilder =
            NotificationCompat.Builder(context, BOOKS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_app_logo)
                .setContentTitle(context.getString(R.string.book_download_failed_title))
                .setOngoing(false)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("${error.name}\n${error.error}")
                )
                .setContentText(error.name)
                .setContentIntent(showWindowPending)
                .addAction(
                    R.drawable.ic_baseline_refresh_24,
                    context.getString(R.string.reload_item_title),
                    reloadPending
                )
                .addAction(
                    R.drawable.ic_baseline_delete_24,
                    context.getString(R.string.delete_item_title),
                    deletePending
                )
                .setGroup(BOOKS_ERRORS_GROUP)
                .setAutoCancel(true)
        mNotificationManager.notify(bookLoadedId, downloadScheduleBuilder.build())
        ++bookLoadedId
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    fun createDownloadNotification(total: Int, loadedNow: Int, errors: Int): Notification {
        // интент отмены скачивания
        val cancelIntent = Intent(context, DownloadBookProcessReceiver::class.java)
        cancelIntent.putExtra(
            DownloadBookProcessReceiver.EXTRA_ACTION,
            DownloadBookProcessReceiver.ACTION_DROP_DOWNLOAD_QUEUE
        )
        val cancelMassDownloadPendingIntent = PendingIntent.getBroadcast(
            context,
            DOWNLOAD_CANCEL_CODE,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        // интент паузы скачивания
        val pauseIntent = Intent(context, DownloadBookProcessReceiver::class.java)
        pauseIntent.putExtra(
            DownloadBookProcessReceiver.EXTRA_ACTION,
            DownloadBookProcessReceiver.ACTION_PAUSE_MASS_DOWNLOAD
        )
        val pauseMassDownloadPendingIntent = PendingIntent.getBroadcast(
            context,
            DOWNLOAD_PAUSE_CODE,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val openWindowIntent =
            Intent(context, DownloadScheduleActivity::class.java)
        openWindowIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val showWindowPending = getActivity(
            context,
            START_APP_CODE,
            openWindowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val downloadScheduleBuilder =
            NotificationCompat.Builder(context, BOOK_DOWNLOADS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_app_logo)
                .setContentTitle(context.getString(R.string.notification_downloader_header))
                .setOngoing(true)
                .setContentText(
                    String.format(
                        Locale.ENGLISH,
                        context.getString(R.string.books_load_progress_notification),
                        loadedNow,
                        total,
                        errors
                    )
                )
                .setProgress(total, loadedNow, false)
                .addAction(
                    R.drawable.fp_ic_action_cancel,
                    context.getString(R.string.drop_download_queue_title),
                    cancelMassDownloadPendingIntent
                )
                .addAction(
                    R.drawable.ic_pause_white_24dp,
                    context.getString(R.string.pause_load_title),
                    pauseMassDownloadPendingIntent
                )
                .setContentIntent(showWindowPending)
                .setAutoCancel(false)
        return downloadScheduleBuilder.build()
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    fun showDownloadPausedNotification() {
        // интент продолжения скачивания
        val resumeIntent = Intent(context, DownloadBookProcessReceiver::class.java)
        resumeIntent.putExtra(
            DownloadBookProcessReceiver.EXTRA_ACTION,
            DownloadBookProcessReceiver.ACTION_RESUME_MASS_DOWNLOAD
        )
        val resumeMassDownloadPendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getBroadcast(
                context,
                DOWNLOAD_RESUME_CODE,
                resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                context,
                DOWNLOAD_RESUME_CODE,
                resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        val openWindowIntent =
            Intent(context, DownloadScheduleActivity::class.java)
        openWindowIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val showWindowPending = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getActivity(
                context,
                START_APP_CODE,
                openWindowIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            getActivity(
                context,
                START_APP_CODE,
                openWindowIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val downloadScheduleBuilder =
            NotificationCompat.Builder(context, BOOK_DOWNLOADS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_app_logo)
                .setContentTitle(context.getString(R.string.notification_downloader_header))
                .setOngoing(false)
                .setContentText(context.getString(R.string.books_downloading_paused_title))
                .addAction(
                    R.drawable.ic_baseline_play_arrow_24,
                    context.getString(R.string.resume_download_title),
                    resumeMassDownloadPendingIntent
                )
                .setContentIntent(showWindowPending)
                .setAutoCancel(true)
        mNotificationManager.notify(bookLoadedId, downloadScheduleBuilder.build())
        ++bookLoadedId
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    fun showDownloadFinishedNotification(currentProgress: BooksDownloadProgress) {
        val openFileBrowserIntent =
            Intent(context, DownloadDirContentActivity::class.java)
        openFileBrowserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val openBrowserPending = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getActivity(
                context,
                myActionId,
                openFileBrowserIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            getActivity(
                context,
                myActionId,
                openFileBrowserIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        myActionId++

        val downloadScheduleBuilder =
            NotificationCompat.Builder(context, BOOK_DOWNLOADS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_app_logo)
                .setContentTitle(context.getString(R.string.download_finished_title))
                .setOngoing(false)
                .setContentText(
                    String.format(
                        Locale.ENGLISH,
                        context.getString(
                            R.string.success_loads_pattern,
                            currentProgress.successLoads
                        )
                    )
                )
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(
                            String.format(
                                Locale.ENGLISH,
                                context.getString(R.string.download_result_pattern),
                                currentProgress.booksInQueue,
                                currentProgress.successLoads,
                                currentProgress.loadErrors
                            )
                        )
                )
                .setContentIntent(openBrowserPending)
                .addAction(
                    R.drawable.ic_baseline_view_list_24,
                    context.getString(R.string.show_browser_title),
                    openBrowserPending
                )
                .setAutoCancel(true)

        if (currentProgress.loadErrors > 0) {
            // create show errors action
            val openErrorsFragmentIntent =
                Intent(context, DownloadScheduleActivity::class.java)
            openErrorsFragmentIntent.putExtra(
                DownloadScheduleActivity.EXTRA_TARGET_FRAGMENT,
                DownloadScheduleActivity.TARGET_ERRORS_FRAGMENT
            )
            openErrorsFragmentIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            val openErrorsPending = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getActivity(
                    context,
                    myActionId,
                    openErrorsFragmentIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                getActivity(
                    context,
                    myActionId,
                    openErrorsFragmentIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
            myActionId++
            downloadScheduleBuilder.addAction(
                R.drawable.ic_baseline_error_24,
                context.getString(R.string.show_errors_fragment_title),
                openErrorsPending
            )
        }

        mNotificationManager.notify(bookLoadedId, downloadScheduleBuilder.build())
        ++bookLoadedId
    }
}