package net.veldor.flibusta_test.ui

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.model.helper.BookActionsHelper
import java.io.File

class DownloadedBooksActionsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("surprise", "DownloadedBooksActionsActivity.kt 17: start activity")
        // share or open file
        val type = intent.getStringExtra(EXTRA_TYPE)
        if (type != null && intent.data != null) {
            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
            if (notificationId > 0) {
                val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(notificationId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (type == TYPE_OPEN) {
                    val file =
                        DocumentFile.fromSingleUri(App.instance.applicationContext, intent.data!!)
                    if (file != null) {
                        BookActionsHelper.openBook(file)
                    }
                } else if (type == TYPE_SHARE) {
                    val file =
                        DocumentFile.fromSingleUri(App.instance.applicationContext, intent.data!!)
                    if (file != null) {
                        BookActionsHelper.shareBook(file)
                    }
                }
            }
        }
        if(type == TYPE_SHARE_COMPAT){
            val file = File(intent.getStringExtra("path"))
            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
            if (notificationId > 0) {
                val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(notificationId)
            }
            BookActionsHelper.shareBook(file)
        }
        else if(type == TYPE_OPEN_COMPAT){
            val file = File(intent.getStringExtra("path"))
            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
            if (notificationId > 0) {
                val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(notificationId)
            }
            BookActionsHelper.openBook(file)
        }
        finish()
    }

    companion object {
        const val TYPE_OPEN_COMPAT = "open compat"
        const val TYPE_SHARE_COMPAT = "share compat"
        const val EXTRA_TYPE = "type"
        const val EXTRA_NOTIFICATION_ID = "notification id"
        const val TYPE_OPEN = "open"
        const val TYPE_SHARE = "share"
    }
}