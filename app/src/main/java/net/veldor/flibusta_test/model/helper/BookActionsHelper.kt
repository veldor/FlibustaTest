package net.veldor.flibusta_test.model.helper

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.utils.TransportUtils.intentCanBeHandled
import java.io.File

object BookActionsHelper {
    fun openBook(file: DocumentFile) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(
            file.uri,
            file.type
        )
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intentCanBeHandled(intent)) {
            App.instance.startActivity(
                Intent.createChooser(
                    intent,
                    App.instance.getString(R.string.open_with_title)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } else {
            Toast.makeText(
                App.instance.applicationContext,
                App.instance.applicationContext.getString(R.string.app_not_fount_title),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun shareBook(file: DocumentFile) {
        if (file.type != "application/x-mobipocket-ebook") {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.putExtra(Intent.EXTRA_STREAM, file.uri)
            shareIntent.type = file.type
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            App.instance.startActivity(
                Intent.createChooser(
                    shareIntent,
                    App.instance.getString(R.string.share_with_title)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } else {
            Log.d("surprise", "shareFile: try share mobi as file")
            val docId = DocumentsContract.getDocumentId(file.uri)
            val split = docId.split(":").toTypedArray()
            // получу файл из documentFile и отправлю его
            var file1 = getFileFromDocumentFile(file)
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
                    shareIntent.type = file.type
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    App.instance.startActivity(
                        Intent.createChooser(
                            shareIntent,
                            App.instance.getString(R.string.share_with_title)
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
        }
    }
    private fun getFileFromDocumentFile(df: DocumentFile): File? {
        val docId: String
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            docId = DocumentsContract.getDocumentId(df.uri)
            val split = docId.split(":").toTypedArray()
            val storage = split[0]
            val path = "///storage/" + storage + "/" + split[1]
            return File(path)
        }
        return null
    }
}