package net.veldor.flibusta_test.model.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.model.db.DatabaseInstance
import net.veldor.flibusta_test.model.handler.DownloadHandler
import net.veldor.flibusta_test.model.handler.NotificationHandler
import net.veldor.flibusta_test.ui.DownloadedBooksActionsActivity

class DownloadBookProcessReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("surprise", "onReceive: received action")
        val intentId = intent.getIntExtra(DownloadedBooksActionsActivity.EXTRA_NOTIFICATION_ID, 0)
        if (intentId > 0) {
            val notificationManager =
                App.instance.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(intentId)
        }
        when (intent.getStringExtra(EXTRA_ACTION)) {
            ACTION_PAUSE_MASS_DOWNLOAD -> {
                DownloadHandler.instance.cancelDownload()
                NotificationHandler.instance.showDownloadPausedNotification()
            }
            ACTION_DROP_DOWNLOAD_QUEUE -> {
                DownloadHandler.instance.cancelDownload()
                DatabaseInstance.instance.dropDownloadQueue()
            }
            ACTION_RELOAD -> {
                DatabaseInstance.instance.reloadDownloadErrorByBookId(
                    intent.getStringExtra(
                        EXTRA_BOOK_ID
                    )
                )
            }
            ACTION_DELETE -> {
                DatabaseInstance.instance.deleteDownloadErrorByBookId(
                    intent.getStringExtra(
                        EXTRA_BOOK_ID
                    )
                )
            }
        }
    }

    companion object {
        const val EXTRA_ACTION = "action"
        const val EXTRA_BOOK_ID = "book id"
        const val ACTION_DROP_DOWNLOAD_QUEUE = "drop"
        const val ACTION_PAUSE_MASS_DOWNLOAD = "pause"
        const val ACTION_RESUME_MASS_DOWNLOAD = "resume"
        const val ACTION_RELOAD = "reload"
        const val ACTION_DELETE = "delete"
    }
}