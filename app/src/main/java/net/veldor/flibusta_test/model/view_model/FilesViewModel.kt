package net.veldor.flibusta_test.model.view_model

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.veldor.flibusta_test.model.db.DatabaseInstance
import net.veldor.flibusta_test.model.db.entity.DownloadedBooks
import net.veldor.flibusta_test.model.handler.CompanionAppHandler
import java.io.File


class FilesViewModel : ViewModel() {
    fun sendToCompanion(context: Context, file: DocumentFile, filename: String) {
        viewModelScope.launch(Dispatchers.IO) {
            CompanionAppHandler.send(context, file, filename)
        }
    }

    fun sendToCompanion(file: File, filename: String) {
        viewModelScope.launch(Dispatchers.IO) {
            CompanionAppHandler.send(file, filename)
        }
    }

    fun deleteItem(item: DownloadedBooks) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseInstance.mDatabase.downloadedBooksDao().delete(item)
        }
    }

}