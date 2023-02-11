package net.veldor.flibusta_test.model.receiver

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import net.veldor.flibusta_test.BuildConfig
import net.veldor.flibusta_test.model.handler.UpdateHandler
import java.io.File

@Suppress("DEPRECATION")
class DownloadManagerReceiver(private val downloadManager: DownloadManager, private val downloadID: Long) :
    BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
            val finishedDownloadId: Long =
                intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if(finishedDownloadId != downloadID){
                Log.d("surprise", "DownloadManagerReceiver.kt 24: not same download")
                return
            }
            Log.d("surprise", "DownloadManagerReceiver.kt 27: it's same download")
            val extras = intent.extras
            if (extras != null) {
                Log.d("surprise", "DownloadManagerReceiver.kt 31: here")
                val q = DownloadManager.Query()
                q.setFilterById(extras.getLong(DownloadManager.EXTRA_DOWNLOAD_ID))
                val c: Cursor = downloadManager.query(q)
                if (c.moveToFirst()) {
                    Log.d("surprise", "DownloadManagerReceiver.kt 36: here1")
                    val columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val status = c.getInt(columnIndex)
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        Log.d("surprise", "DownloadManagerReceiver.kt 40: here2")
                        val fullPath: String?
                        var source: File? = null
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            val index = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            if(index >= 0){
                                fullPath = c.getString(index)
                                if(fullPath != null){
                                    source = Uri.parse(fullPath).path?.let { File(it) }
                                }
                            }
                        } else {
                            val index = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME)
                            if(index >= 0){
                                fullPath = c.getString(index)
                                if(fullPath != null){
                                    source = Uri.parse(fullPath).path?.let { File(it) }
                                }
                            }
                        }
                        if(source != null){
                            //open the downloaded file
                            val install = Intent(Intent.ACTION_VIEW)
                            install.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                            var downloadUri: Uri? = UpdateHandler.updateDownloadUri
                            if (Build.VERSION.SDK_INT >= 24) {
                                downloadUri = FileProvider.getUriForFile(
                                    context, BuildConfig.APPLICATION_ID + ".provider",
                                    source
                                )
                            }
                            install.setDataAndType(
                                downloadUri,
                                downloadManager.getMimeTypeForDownloadedFile(downloadID)
                            )
                            Log.d("surprise", "DownloadReceiver onReceive: trying install update")
                            context.startActivity(install)
                        }

                        context.unregisterReceiver(this)
                    }
                    else{
                        UpdateHandler.liveCurrentDownloadProgress.postValue(-2)
                    }
                }
                else{
                    UpdateHandler.liveCurrentDownloadProgress.postValue(-2)
                }
                c.close()
            }
        }
    }

    fun cancelDownload() {
        downloadManager.remove(downloadID)
    }
}