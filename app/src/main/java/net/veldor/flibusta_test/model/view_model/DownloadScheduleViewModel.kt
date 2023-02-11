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
            DatabaseInstance.reloadDownloadErrors()
        }
    }

    fun dropErrorQueue() {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseInstance.dropDownloadErrorsQueue()
        }
    }

    fun dropDownloadQueue() {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseInstance.dropDownloadQueue()
        }
    }

    fun deleteFromQueue(book: BooksDownloadSchedule) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseInstance.mDatabase.booksDownloadScheduleDao().delete(book)
        }
    }

    fun reloadError(downloadError: DownloadError) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseInstance.mDatabase.booksDownloadScheduleDao()
                .insert(downloadError as BooksDownloadSchedule)
            DatabaseInstance.mDatabase.downloadErrorDao().delete(downloadError)
        }
    }
}