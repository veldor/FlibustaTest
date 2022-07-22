package net.veldor.flibusta_test.model.handler

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.model.helper.BookActionsHelper
import java.io.File


class SendToKindleHandler {
    fun send(destinationFile: DocumentFile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val docId = DocumentsContract.getDocumentId(destinationFile.uri)
            val split = docId.split(":").toTypedArray()
            // получу файл из documentFile и отправлю его
            var file1 = BookActionsHelper.getFileFromDocumentFile(destinationFile)
            if (file1 != null) {
                if (!file1.exists()) {
                    file1 = File(
                        Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                    )
                }
                if (file1.exists()) {
                    val fileUri: Uri? = try {
                        FileProvider.getUriForFile(
                            App.instance,
                            "net.veldor.flibusta_test.provider",
                            file1
                        )
                    } catch (e: IllegalArgumentException) {
                        Log.e(
                            "File Selector",
                            "The selected file can't be shared: $file1"
                        )
                        null
                    }
                    Log.d("surprise", "shareFile: uri is $fileUri")
                    // отправлю запрос на открытие файла
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri)
                    shareIntent.type = "application/x-mobipocket-ebook"
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    App.instance.startActivity(shareIntent)
                }
            }
        }
    }
}