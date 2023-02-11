package net.veldor.flibusta_test.view

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.model.helper.BookActionsHelper
import java.io.File
import java.net.URI

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
            Log.d(
                "surprise",
                "DownloadedBooksActionsActivity: 25 select option with ${intent.data}"
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val uriPath = intent.data!!.toString()
                if(uriPath.startsWith("content")){
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
                else{
                    val file = File(
                        URI.create(uriPath)
                    )

                    if (type == TYPE_OPEN) {
                        if (file.exists()) {
                            BookActionsHelper.openBook(file)
                        }
                    } else if (type == TYPE_SHARE) {
                        if (file.exists()) {
                            BookActionsHelper.shareBook(file)
                        }
                    }
                }
            }
            else{
                if (type == TYPE_OPEN) {
                    val file = File(URI.create(intent.data!!.toString()))
                    if (file.exists()) {
                        BookActionsHelper.openBook(file)
                    }
                } else if (type == TYPE_SHARE) {
                    val file = File(URI.create(intent.data!!.toString()))
                    if (file.exists()) {
                        BookActionsHelper.shareBook(file)
                    }
                }
            }
        }
        if(type == TYPE_SHARE_COMPAT){
            val file = File(URI.create(intent.data!!.toString()))
            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
            if (notificationId > 0) {
                val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(notificationId)
            }
            BookActionsHelper.shareBook(file)
        }
        else if(type == TYPE_OPEN_COMPAT){
            val file = File(URI.create(intent.data!!.toString()))
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