package net.veldor.flibusta_test.ui

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.documentfile.provider.DocumentFile
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.model.helper.BookActionsHelper

class DownloadedBooksActionsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        finish()
    }

    companion object {
        const val EXTRA_TYPE = "type"
        const val EXTRA_NOTIFICATION_ID = "notification id"
        const val TYPE_OPEN = "open"
        const val TYPE_SHARE = "share"
    }
}