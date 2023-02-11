package net.veldor.flibusta_test.model.helper

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.handler.FormatHandler
import net.veldor.flibusta_test.model.util.TransportUtils
import java.io.File

object BookActionsHelper {
    fun openBook(file: DocumentFile) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(
            file.uri,
            file.type
        )
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        if (TransportUtils.intentCanBeHandled(intent)) {
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
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun shareBookToKindle(file: DocumentFile) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.putExtra(Intent.EXTRA_STREAM, file.uri)
        shareIntent.type = file.type
        Log.d("surprise", "BookActionsHelper: 63 type is ${shareIntent.type}")
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        shareIntent.`package` = "com.amazon.kindle"
        App.instance.startActivity(shareIntent)
    }

    fun shareBook(file: File) {
        if (file.exists()) {
            val fileUri: Uri? = try {
                FileProvider.getUriForFile(
                    App.instance,
                    "net.veldor.flibusta_test.provider",
                    file
                )
            } catch (e: IllegalArgumentException) {
                Log.e(
                    "File Selector",
                    "The selected file can't be shared: $file"
                )
                null
            }
            Log.d("surprise", "shareFile: uri is $fileUri")
            // отправлю запрос на открытие файла
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri)
            shareIntent.type = MimeHelper.getMimeFromFileName(file.name)
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            val resInfoList: List<ResolveInfo> = App.instance.packageManager.queryIntentActivities(
                shareIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            for (resolveInfo in resInfoList) {
                val packageName: String = resolveInfo.activityInfo.packageName
                App.instance.grantUriPermission(
                    packageName,
                    fileUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            App.instance.startActivity(
                Intent.createChooser(
                    shareIntent,
                    App.instance.getString(R.string.share_with_title)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun openBook(file: File) {
        if (file.exists()) {
            val fileUri: Uri? = try {
                FileProvider.getUriForFile(
                    App.instance,
                    "net.veldor.flibusta_test.provider",
                    file
                )
            } catch (e: IllegalArgumentException) {
                Log.e(
                    "File Selector",
                    "The selected file can't be open: $file"
                )
                null
            }
            // отправлю запрос на открытие файла
            val shareIntent = Intent(Intent.ACTION_VIEW)
            shareIntent.setDataAndType(fileUri, MimeHelper.getMimeFromFileName(file.name))
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            App.instance.startActivity(
                Intent.createChooser(
                    shareIntent,
                    App.instance.getString(R.string.open_with_title)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun shareBookToKindle(file: File) {


        val fileUri: Uri? = try {
            FileProvider.getUriForFile(
                App.instance,
                "net.veldor.flibusta_test.provider",
                file
            )
        } catch (e: IllegalArgumentException) {
            Log.e(
                "File Selector",
                "The selected file can't be shared: $file"
            )
            null
        }

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri)
        shareIntent.type = FormatHandler.getFullFromShortMime(file.extension)
        Log.d("surprise", "BookActionsHelper: 142 ${shareIntent.type}")
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        shareIntent.`package` = "com.amazon.kindle"
        App.instance.grantUriPermission(
            "com.amazon.kindle",
            fileUri,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        App.instance.startActivity(shareIntent)
    }
}