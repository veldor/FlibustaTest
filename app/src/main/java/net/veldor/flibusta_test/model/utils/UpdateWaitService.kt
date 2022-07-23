package net.veldor.flibusta_test.model.utils

import android.app.DownloadManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import net.veldor.flibusta_test.BuildConfig
import java.io.File

class UpdateWaitService : Service() {
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // Регистрирую сервис для приёма статуса загрузки обновления
        val downloadObserver = DownloadReceiver()
        this.registerReceiver(
            downloadObserver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private inner class DownloadReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val finishedDownloadId: Long =
                intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            Log.d("surprise", "UpdateWaitService.kt 37: download finished, id is $finishedDownloadId")
            if (finishedDownloadId == Updater.updateDownloadIdentification) {
                val query: DownloadManager.Query = DownloadManager.Query()
                query.setFilterById(finishedDownloadId)
                val manager: DownloadManager = application.getSystemService(
                    DOWNLOAD_SERVICE
                ) as DownloadManager
                val cursor: Cursor = manager.query(query)
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val status = cursor.getInt(columnIndex)
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        //open the downloaded file
                        val install = Intent(Intent.ACTION_VIEW)
                        install.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        var downloadUri: Uri? = Updater.updateDownloadUri
                        if (Build.VERSION.SDK_INT >= 24) {
                            downloadUri = FileProvider.getUriForFile(
                                context, BuildConfig.APPLICATION_ID + ".provider",
                                Updater.downloadedApkFile!!
                            )
                        }
                        install.setDataAndType(
                            downloadUri,
                            manager.getMimeTypeForDownloadedFile(Updater.updateDownloadIdentification)
                        )
                        Log.d("surprise", "DownloadReceiver onReceive: trying install update")
                        context.startActivity(install)
                    } else {
                        clearFile()
                    }
                } else {
                    clearFile()
                }
                Updater.downloadedApkFile = null
                Updater.updateDownloadUri = null
                context.unregisterReceiver(this)
                stopSelf()
            }
        }

        private fun clearFile() {
            // удалю файл, если он создался
            val file: File? = Updater.downloadedApkFile
            if (file != null) {
                if (file.exists()) {
                    val deleteResult = file.delete()
                    if (!deleteResult) {
                        Log.d(
                            "surprise",
                            "DownloadReceiver onReceive: не удалось удалить загруженный файл"
                        )
                    }
                }
            }
            Toast.makeText(applicationContext, "Flibusta loader update failed", Toast.LENGTH_LONG)
                .show()
        }
    }
}