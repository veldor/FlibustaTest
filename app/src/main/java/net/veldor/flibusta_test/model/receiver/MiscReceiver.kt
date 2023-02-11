package net.veldor.flibusta_test.model.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.model.handler.TorHandler
import net.veldor.flibusta_test.model.util.MyFileReader
import net.veldor.flibusta_test.model.worker.LaunchTorWorker
import net.veldor.flibusta_test.model.worker.SendBookToCompanionWorker
import net.veldor.flibusta_test.view.DownloadedBooksActionsActivity
import java.io.File

class MiscReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getStringExtra(DownloadBookProcessReceiver.EXTRA_ACTION)) {
            ACTION_CANCEL_TOR_LAUNCH -> {
                GlobalScope.async(Dispatchers.Main) {
                    TorHandler.stopTor()
                    LaunchTorWorker.isInterrupted = true
                }
            }
            ACTION_RESEND_BOOK_TO_COMPANION_APP -> {

                val notificationId = intent.getIntExtra(DownloadedBooksActionsActivity.EXTRA_NOTIFICATION_ID, 0)
                if (notificationId > 0) {
                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(notificationId)
                }
                val builder = Data.Builder()
                builder.putString(SendBookToCompanionWorker.DATA_BOOK, intent.getStringExtra("path"))
                builder.putString(SendBookToCompanionWorker.DATA_BOOK_NAME, intent.getStringExtra("fileName"))
                val worker = OneTimeWorkRequest.Builder(
                    SendBookToCompanionWorker::class.java
                )
                    .addTag(SendBookToCompanionWorker.TAG)
                    .setInputData(builder.build())
                    .build()
                WorkManager.getInstance(context)
                    .enqueue(worker)
            }
        }
    }

    companion object {
        const val EXTRA_ACTION = "action"
        const val ACTION_CANCEL_TOR_LAUNCH = "cancel tor launch"
        const val ACTION_RESEND_BOOK_TO_COMPANION_APP = "resend book to companion app"
    }
}