package net.veldor.flibusta_test.model.receiver

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import java.io.File
import java.util.*


private class DownloadFileReceiver private constructor(
    private val mDownloadManager: DownloadManager,
    private val mPath: String
) :
    BroadcastReceiver() {
    /** Override BroadcastReceiver Methods  */
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
            val extras = intent.extras
            val q = DownloadManager.Query()
            q.setFilterById(extras.getLong(DownloadManager.EXTRA_DOWNLOAD_ID))
            val c: Cursor = mDownloadManager.query(q)
            if (c.moveToFirst()) {
                val status: Int = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    var fullPath: String? = null
                    var source: File? = null
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        fullPath = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                        source = File(Uri.parse(fullPath).getPath())
                    } else {
                        fullPath =
                            c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME))
                        source = File(fullPath)
                    }
                }
            }
            c.close()
        }
        Objects.requireNonNull(context).unregisterReceiver(this)
    }
}