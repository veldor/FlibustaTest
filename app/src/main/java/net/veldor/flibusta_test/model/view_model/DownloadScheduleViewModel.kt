package net.veldor.flibusta_test.model.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.veldor.flibusta_test.model.db.DatabaseInstance
import net.veldor.flibusta_test.model.db.entity.BooksDownloadSchedule
import net.veldor.flibusta_test.model.db.entity.DownloadError


class DownloadScheduleViewModel : ViewModel() {
    fun reloadAllErrors() {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseInstance.instance.reloadDownloadErrors()
        }
    }

    fun dropErrorQueue() {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseInstance.instance.dropDownloadErrorsQueue()
        }
    }

    fun dropDownloadQueue() {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseInstance.instance.dropDownloadQueue()
        }
    }

    fun deleteFromQueue(book: BooksDownloadSchedule) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseInstance.instance.mDatabase.booksDownloadScheduleDao().delete(book)
        }
    }

    fun reloadError(downloadError: DownloadError) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseInstance.instance.mDatabase.booksDownloadScheduleDao()
                .insert(downloadError as BooksDownloadSchedule)
            DatabaseInstance.instance.mDatabase.downloadErrorDao().delete(downloadError)
        }
    }
}