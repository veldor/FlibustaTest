package net.veldor.flibusta_test.model.handler

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.text.format.Formatter
import androidx.core.app.NotificationCompat
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.db.entity.DownloadError
import net.veldor.flibusta_test.model.receiver.DownloadBookProcessReceiver
import net.veldor.flibusta_test.model.receiver.MiscReceiver
import net.veldor.flibusta_test.model.selection.BooksDownloadProgress
import net.veldor.flibusta_test.model.selection.RootDownloadDir
import net.veldor.flibusta_test.view.DownloadScheduleActivity
import net.veldor.flibusta_test.view.DownloadedBooksActionsActivity
import net.veldor.flibusta_test.view.DownloadedBooksActionsActivity.Companion.EXTRA_NOTIFICATION_ID
import net.veldor.flibusta_test.view.DownloadedBooksViewActivity
import net.veldor.flibusta_test.view.SubscriptionActivity
import java.util.*

object NotificationHandler {

    private var bookLoadedId = 100
    private var myActionId = 100

    private const val START_APP_CODE = 1
    private const val DOWNLOAD_CANCEL_CODE = 2
    private const val DOWNLOAD_PAUSE_CODE = 3
    private const val DOWNLOAD_RESUME_CODE = 4

    const val LAUNCH_TOR_WORKER_NOTIFICATION = 1
    private const val BOOK_DOWNLOAD_PROGRESS = 11
    const val DOWNLOAD_PROGRESS_NOTIFICATION = 5
    const val SEND_TO_COMPANION_NOTIFICATION = 6
    const val CHECK_SUBSCRIBES_WORKER_NOTIFICATION = 10

    private const val FOREGROUND_CHANNEL_ID: String = "foreground"
    private const val BOOK_DOWNLOADS_CHANNEL_ID = "book downloads"
    private const val BOOKS_CHANNEL_ID = "books"
    private const val SUBSCRIBE_CHECK_SERVICE_CHANNEL_ID = "subscribes check"

    private const val DOWNLOADED_BOOKS_GROUP = "net.veldor.downloadedBooksGroup"
    private const val BOOKS_ERRORS_GROUP = "net.veldor.downloadedBooksErrorsGroup"


    private val mNotificationManager: NotificationManager =
        App.instance.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val startTorNotification: Notification
        @SuppressLint("UnspecifiedImmutableFlag")
        get() {
            val cancelIntent = Intent(App.instance, MiscReceiver::class.java)
            cancelIntent.putExtra(
                MiscReceiver.EXTRA_ACTION,
                MiscReceiver.ACTION_CANCEL_TOR_LAUNCH
            )
            val cancelMassDownloadPendingIntent =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.getBroadcast(
                        App.instance,
                        DOWNLOAD_CANCEL_CODE,
                        cancelIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                    )
                } else {
                    PendingIntent.getBroadcast(
                        App.instance,
                        DOWNLOAD_CANCEL_CODE,
                        cancelIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }
            val notificationBuilder =
                NotificationCompat.Builder(App.instance, FOREGROUND_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_app_logo)
                    .setContentTitle(
                        App.instance.getString(R.string.start_tor_message)
                    )
                    .addAction(
                        R.drawable.ic_baseline_cancel_24,
                        App.instance.getString(R.string.cancel_launch_tor),
                        cancelMassDownloadPendingIntent
                    )
                    .setProgress(0, 0, true)
                    .setOngoing(true)
            return notificationBuilder.build()
        }


    @SuppressLint("UnspecifiedImmutableFlag")
    fun createDownloadNotification(total: Int, loadedNow: Int, errors: Int): Notification {
        // интент отмены скачивания
        val cancelIntent = Intent(App.instance, DownloadBookProcessReceiver::class.java)
        cancelIntent.putExtra(
            DownloadBookProcessReceiver.EXTRA_ACTION,
            DownloadBookProcessReceiver.ACTION_DROP_DOWNLOAD_QUEUE
        )
        val cancelMassDownloadPendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                App.instance,
                DOWNLOAD_CANCEL_CODE,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                App.instance,
                DOWNLOAD_CANCEL_CODE,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        // интент паузы скачивания
        val pauseIntent = Intent(App.instance, DownloadBookProcessReceiver::class.java)
        pauseIntent.putExtra(
            DownloadBookProcessReceiver.EXTRA_ACTION,
            DownloadBookProcessReceiver.ACTION_PAUSE_MASS_DOWNLOAD
        )
        val pauseMassDownloadPendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                App.instance,
                DOWNLOAD_PAUSE_CODE,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                App.instance,
                DOWNLOAD_PAUSE_CODE,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        val openWindowIntent =
            Intent(App.instance, DownloadScheduleActivity::class.java)
        openWindowIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val showWindowPending = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(
                App.instance,
                START_APP_CODE,
                openWindowIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        } else {
            PendingIntent.getActivity(
                App.instance,
                START_APP_CODE,
                openWindowIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val downloadScheduleBuilder =
            NotificationCompat.Builder(App.instance, BOOK_DOWNLOADS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_app_logo)
                .setContentTitle(App.instance.getString(R.string.notification_downloader_header))
                .setOngoing(true)
                .setContentText(
                    String.format(
                        Locale.ENGLISH,
                        App.instance.getString(R.string.books_load_progress_notification),
                        loadedNow,
                        total,
                        errors
                    )
                )
                .setProgress(total, loadedNow, false)
                .addAction(
                    R.drawable.ic_baseline_cancel_24,
                    App.instance.getString(R.string.drop_download_queue_title),
                    cancelMassDownloadPendingIntent
                )
                .addAction(
                    R.drawable.ic_pause_white_24dp,
                    App.instance.getString(R.string.pause_load_title),
                    pauseMassDownloadPendingIntent
                )
                .setContentIntent(showWindowPending)
                .setAutoCancel(false)
        return downloadScheduleBuilder.build()
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
            Intent(App.instance, DownloadScheduleActivity::class.java)
        openWindowIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val showWindowPending = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(
                App.instance,
                START_APP_CODE,
                openWindowIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getActivity(
                App.instance,
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
            NotificationCompat.Builder(App.instance, BOOK_DOWNLOADS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cloud_download_white_24dp)
                .setContentTitle(App.instance.getString(R.string.downloading_book_title))
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        String.format(
                            Locale.ENGLISH,
                            App.instance.getString(R.string.book_load_progress_pattern),
                            name,
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

    @SuppressLint("UnspecifiedImmutableFlag")
    fun showDownloadPausedNotification() {
        // интент продолжения скачивания
        val resumeIntent = Intent(App.instance, DownloadBookProcessReceiver::class.java)
        resumeIntent.putExtra(
            DownloadBookProcessReceiver.EXTRA_ACTION,
            DownloadBookProcessReceiver.ACTION_RESUME_MASS_DOWNLOAD
        )
        val resumeMassDownloadPendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getBroadcast(
                App.instance,
                DOWNLOAD_RESUME_CODE,
                resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                App.instance,
                DOWNLOAD_RESUME_CODE,
                resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        val openWindowIntent =
            Intent(App.instance, DownloadScheduleActivity::class.java)
        openWindowIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val showWindowPending = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(
                App.instance,
                START_APP_CODE,
                openWindowIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getActivity(
                App.instance,
                START_APP_CODE,
                openWindowIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val downloadScheduleBuilder =
            NotificationCompat.Builder(App.instance, BOOK_DOWNLOADS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_app_logo)
                .setContentTitle(App.instance.getString(R.string.notification_downloader_header))
                .setOngoing(false)
                .setContentText(App.instance.getString(R.string.books_downloading_paused_title))
                .addAction(
                    R.drawable.ic_baseline_play_arrow_24,
                    App.instance.getString(R.string.resume_download_title),
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
            Intent(App.instance, DownloadedBooksViewActivity::class.java)
        openFileBrowserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val openBrowserPending = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(
                App.instance,
                myActionId,
                openFileBrowserIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getActivity(
                App.instance,
                myActionId,
                openFileBrowserIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        myActionId++

        val downloadScheduleBuilder =
            NotificationCompat.Builder(App.instance, BOOK_DOWNLOADS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_app_logo)
                .setContentTitle(App.instance.getString(R.string.download_finished_title))
                .setOngoing(false)
                .setContentText(
                    String.format(
                        Locale.ENGLISH,
                        App.instance.getString(
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
                                App.instance.getString(R.string.download_result_pattern),
                                currentProgress.booksInQueue,
                                currentProgress.successLoads,
                                currentProgress.loadErrors
                            )
                        )
                )
                .setContentIntent(openBrowserPending)
                .addAction(
                    R.drawable.ic_baseline_view_list_24,
                    App.instance.getString(R.string.show_browser_title),
                    openBrowserPending
                )
                .setAutoCancel(true)

        if (currentProgress.loadErrors > 0) {
            // create show errors action
            val openErrorsFragmentIntent =
                Intent(App.instance, DownloadScheduleActivity::class.java)
            openErrorsFragmentIntent.putExtra(
                DownloadScheduleActivity.EXTRA_TARGET_FRAGMENT,
                DownloadScheduleActivity.TARGET_ERRORS_FRAGMENT
            )
            openErrorsFragmentIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            val openErrorsPending = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.getActivity(
                    App.instance,
                    myActionId,
                    openErrorsFragmentIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getActivity(
                    App.instance,
                    myActionId,
                    openErrorsFragmentIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
            myActionId++
            downloadScheduleBuilder.addAction(
                R.drawable.ic_baseline_error_24,
                App.instance.getString(R.string.show_errors_fragment_title),
                openErrorsPending
            )
        }

        mNotificationManager.notify(bookLoadedId, downloadScheduleBuilder.build())
        ++bookLoadedId
    }


    fun createDownloadNotification(): Notification {
        return createDownloadNotification(0, 0, 0)
    }

    fun createSendToCompanionNotification(): Notification {
        return createSendToCompanionNotification("", 0, 0)
    }

    fun createSendToCompanionNotification(status: String, size: Int, done: Int): Notification {
        val downloadScheduleBuilder =
            NotificationCompat.Builder(App.instance, BOOK_DOWNLOADS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_app_logo)
                .setContentTitle(App.instance.getString(R.string.notification_send_to_companion_app_header))
                .setOngoing(true)
                .setContentText(status)
                .setProgress(size, done, size == 0)
                .setAutoCancel(false)
        return downloadScheduleBuilder.build()
    }

    fun closeBookLoadingProgressNotification() {
        mNotificationManager.cancel(BOOK_DOWNLOAD_PROGRESS)
    }


    @SuppressLint("UnspecifiedImmutableFlag")
    fun showBookDownloadErrorNotification(error: DownloadError) {
        // add reload intent
        val reloadIntent =
            Intent(App.instance, DownloadBookProcessReceiver::class.java)
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
                App.instance,
                myActionId,
                reloadIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                App.instance,
                myActionId,
                reloadIntent,
                PendingIntent.FLAG_ONE_SHOT
            )
        }
        myActionId++
        // add open intent
        val deleteIntent =
            Intent(App.instance, DownloadBookProcessReceiver::class.java)
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
                App.instance,
                myActionId,
                deleteIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                App.instance,
                myActionId,
                deleteIntent,
                PendingIntent.FLAG_ONE_SHOT
            )
        }
        myActionId++
        val openWindowIntent =
            Intent(App.instance, DownloadScheduleActivity::class.java)
        openWindowIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val showWindowPending = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(
                App.instance,
                START_APP_CODE,
                openWindowIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getActivity(
                App.instance,
                START_APP_CODE,
                openWindowIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        val downloadScheduleBuilder =
            NotificationCompat.Builder(App.instance, BOOKS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_app_logo)
                .setContentTitle(App.instance.getString(R.string.book_download_failed_title))
                .setOngoing(false)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("${error.name}\n${error.error}")
                )
                .setContentText(error.name)
                .setContentIntent(showWindowPending)
                .addAction(
                    R.drawable.ic_baseline_refresh_24,
                    App.instance.getString(R.string.reload_item_title),
                    reloadPending
                )
                .addAction(
                    R.drawable.ic_baseline_delete_24,
                    App.instance.getString(R.string.delete_item_title),
                    deletePending
                )
                .setGroup(BOOKS_ERRORS_GROUP)
                .setAutoCancel(true)
        mNotificationManager.notify(bookLoadedId, downloadScheduleBuilder.build())
        ++bookLoadedId
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    fun createSuccessBookLoadNotification(rootDir: RootDownloadDir) {
        // add open browser intent
        val openFileBrowserIntent =
            Intent(App.instance, DownloadedBooksViewActivity::class.java)
        openFileBrowserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val openBrowserPending = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(
                App.instance,
                myActionId,
                openFileBrowserIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getActivity(
                App.instance,
                myActionId,
                openFileBrowserIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        myActionId
        // add share intent
        val shareIntent =
            Intent(App.instance, DownloadedBooksActionsActivity::class.java)
        shareIntent.setDataAndType(rootDir.destinationFileUri, rootDir.getDestinationFileType())
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        shareIntent.putExtra(EXTRA_NOTIFICATION_ID, bookLoadedId)
        shareIntent.putExtra(
            DownloadedBooksActionsActivity.EXTRA_TYPE,
            DownloadedBooksActionsActivity.TYPE_SHARE
        )
        val sharePending = if (Build.VERSION.SDK_INT >= 23) {
            PendingIntent.getActivity(
                App.instance,
                myActionId,
                shareIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getActivity(
                App.instance,
                myActionId,
                shareIntent,
                PendingIntent.FLAG_ONE_SHOT
            )
        }
        myActionId++
        // add open intent
        val openIntent =
            Intent(App.instance, DownloadedBooksActionsActivity::class.java)
        openIntent.setDataAndType(rootDir.destinationFileUri, rootDir.getDestinationFileType())
        openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        openIntent.putExtra(EXTRA_NOTIFICATION_ID, bookLoadedId)
        openIntent.putExtra(
            DownloadedBooksActionsActivity.EXTRA_TYPE,
            DownloadedBooksActionsActivity.TYPE_OPEN
        )
        val openPending = if (Build.VERSION.SDK_INT >= 23) {
            PendingIntent.getActivity(
                App.instance,
                myActionId,
                openIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getActivity(
                App.instance,
                myActionId,
                openIntent,
                PendingIntent.FLAG_ONE_SHOT
            )
        }
        myActionId++
        val notificationBuilder =
            NotificationCompat.Builder(App.instance, BOOKS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_book_black_24dp)
                .setContentTitle(
                    App.instance.getString(R.string.success_load_title)
                )
                .setContentIntent(openBrowserPending)
                .setContentText(rootDir.getDestinationFileName())
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(rootDir.getDestinationFileName())
                )
                .setDefaults(Notification.DEFAULT_ALL)
                .setGroup(DOWNLOADED_BOOKS_GROUP)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addAction(
                    R.drawable.ic_open_black_24dp,
                    App.instance.getString(R.string.open_title),
                    openPending
                )
                .addAction(
                    R.drawable.ic_share_white_24dp,
                    App.instance.getString(R.string.share_title),
                    sharePending
                )
                .setAutoCancel(true)
        mNotificationManager.notify(bookLoadedId, notificationBuilder.build())
        ++bookLoadedId
        //showSuccessBookLoadGroupNotification()
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // создам канал фоновых уведомлений
            var nc = NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                App.instance.getString(R.string.foreground_channel),
                NotificationManager.IMPORTANCE_MIN
            )
            nc.description =
                App.instance.getString(R.string.foreground_channel_description)
            nc.enableLights(false)
            nc.enableVibration(false)
            mNotificationManager.createNotificationChannel(nc)

            nc = NotificationChannel(
                BOOK_DOWNLOADS_CHANNEL_ID,
                App.instance.getString(R.string.books_download_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            nc.enableVibration(false)
            nc.enableLights(false)
            nc.setSound(null, null)
            nc.description =
                App.instance.getString(R.string.books_download_channel_description)
            mNotificationManager.createNotificationChannel(nc)

            nc = NotificationChannel(
                BOOKS_CHANNEL_ID,
                App.instance.getString(R.string.books_loaded_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nc.description =
                App.instance.getString(R.string.books_loaded_channel_description)
            nc.enableLights(true)
            nc.lightColor = Color.RED
            nc.enableVibration(true)
            mNotificationManager.createNotificationChannel(nc)

            // создам канал уведомления о проверке подписок
            nc = NotificationChannel(
                SUBSCRIBE_CHECK_SERVICE_CHANNEL_ID,
                App.instance.getString(R.string.subscribes_check_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nc.enableVibration(false)
            nc.enableLights(false)
            nc.setSound(null, null)
            nc.description = App.instance.getString(R.string.subscribes_check_channel_description)
            mNotificationManager.createNotificationChannel(nc)
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    fun notifySubscriptionsCheck(resultsSize: Int) {
        val openFileBrowserIntent =
            Intent(App.instance, SubscriptionActivity::class.java)
        openFileBrowserIntent.putExtra("tab", "results")
        openFileBrowserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val openBrowserPending = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(
                App.instance,
                myActionId,
                openFileBrowserIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getActivity(
                App.instance,
                myActionId,
                openFileBrowserIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        myActionId++

        val downloadScheduleBuilder =
            NotificationCompat.Builder(App.instance, BOOK_DOWNLOADS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_app_logo)
                .setContentTitle(App.instance.getString(R.string.subscriptions_check_finished_title))
                .setOngoing(false)
                .setContentText(resultsSize.toString())
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(
                            String.format(
                                Locale.ENGLISH,
                                App.instance.getString(R.string.subscriptions_check_pattern),
                                resultsSize
                            )
                        )
                )
                .setContentIntent(openBrowserPending)
                .addAction(
                    R.drawable.ic_baseline_view_list_24,
                    App.instance.getString(R.string.show_browser_title),
                    openBrowserPending
                )
                .setAutoCancel(true)

        mNotificationManager.notify(bookLoadedId, downloadScheduleBuilder.build())
        ++bookLoadedId
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    fun updateTorLoadState(currentState: String?) {
        val cancelIntent = Intent(App.instance, MiscReceiver::class.java)
        cancelIntent.putExtra(
            MiscReceiver.EXTRA_ACTION,
            MiscReceiver.ACTION_CANCEL_TOR_LAUNCH
        )
        val cancelMassDownloadPendingIntent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getBroadcast(
                    App.instance,
                    DOWNLOAD_CANCEL_CODE,
                    cancelIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
            } else {
                PendingIntent.getBroadcast(
                    App.instance,
                    DOWNLOAD_CANCEL_CODE,
                    cancelIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
        val notificationBuilder =
            NotificationCompat.Builder(App.instance, FOREGROUND_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_app_logo)
                .setContentTitle(
                    App.instance.getString(R.string.start_tor_message)
                )
                .setContentText(currentState)
                .addAction(
                    R.drawable.ic_baseline_cancel_24,
                    App.instance.getString(R.string.cancel_launch_tor),
                    cancelMassDownloadPendingIntent
                )
                .setProgress(0, 0, true)
                .setOngoing(true)
        mNotificationManager.notify(LAUNCH_TOR_WORKER_NOTIFICATION, notificationBuilder.build())
    }

    fun showCompanionTransferFinishedNotification(fileName: String) {
        val builder =
            NotificationCompat.Builder(App.instance, BOOK_DOWNLOADS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_app_logo)
                .setContentTitle(App.instance.getString(R.string.book_successfully_send_to_companion_title))
                .setOngoing(false)
                .setContentText(
                    String.format(
                        Locale.ENGLISH,
                        App.instance.getString(R.string.book_sent_title),
                        fileName
                    )
                )
                .setAutoCancel(true)
        mNotificationManager.notify(bookLoadedId, builder.build())
        ++bookLoadedId
    }

    fun showCompanionTransferErrorNotification(fileName: String, reason: String) {
        val builder =
            NotificationCompat.Builder(App.instance, BOOK_DOWNLOADS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_app_logo)
                .setContentTitle(App.instance.getString(R.string.book_send_to_companion_error_title))
                .setOngoing(false)
                .setContentText(
                    String.format(
                        Locale.ENGLISH,
                        App.instance.getString(R.string.book_send_error_pattern),
                        fileName,
                        reason
                    )
                )
                .setAutoCancel(true)
        mNotificationManager.notify(bookLoadedId, builder.build())
        ++bookLoadedId
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    fun showCompanionTransferErrorNotification(
        path: String,
        fileName: String?,
        errorReason: String?
    ) {


        val repeatIntent = Intent(App.instance, MiscReceiver::class.java)
        repeatIntent.putExtra(
            MiscReceiver.EXTRA_ACTION,
            MiscReceiver.ACTION_RESEND_BOOK_TO_COMPANION_APP
        )

        repeatIntent.putExtra(EXTRA_NOTIFICATION_ID, bookLoadedId)
        repeatIntent.putExtra("path", path)
        repeatIntent.putExtra("fileName", fileName)
        val repeatPendingIntent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getBroadcast(
                    App.instance,
                    myActionId,
                    repeatIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
            } else {
                PendingIntent.getBroadcast(
                    App.instance,
                    myActionId,
                    repeatIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
        myActionId++

        val builder =
            NotificationCompat.Builder(App.instance, BOOK_DOWNLOADS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_app_logo)
                .setContentTitle(App.instance.getString(R.string.book_send_to_companion_error_title))
                .setOngoing(false)
                .setContentText(
                    String.format(
                        Locale.ENGLISH,
                        App.instance.getString(R.string.book_send_error_pattern),
                        fileName,
                        errorReason
                    )
                )
                .addAction(
                    R.drawable.ic_baseline_refresh_24,
                    App.instance.getString(R.string.resend_title),
                    repeatPendingIntent
                )
                .setAutoCancel(true)
        mNotificationManager.notify(bookLoadedId, builder.build())
        ++bookLoadedId
    }

    val checkSubscribesNotification: Notification
        get() {
            val notificationBuilder =
                NotificationCompat.Builder(App.instance, SUBSCRIBE_CHECK_SERVICE_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_book_black_24dp)
                    .setContentTitle(
                        App.instance.getString(R.string.check_flibusta_subscriptions_message)
                    )
                    .setProgress(0, 0, true)
                    .setOngoing(true)
            return notificationBuilder.build()
        }
}