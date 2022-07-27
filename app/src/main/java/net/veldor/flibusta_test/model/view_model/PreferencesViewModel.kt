package net.veldor.flibusta_test.model.view_model

import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.veldor.flibusta_test.model.handler.ReserveSettingsHandler
import java.io.File


class PreferencesViewModel : StartViewModel(){

    private val _liveBackupFile = MutableLiveData<DocumentFile>()
    val liveBackupData: LiveData<DocumentFile> = _liveBackupFile

    private val _liveCompatBackupFile = MutableLiveData<File>()
    val liveCompatBackupData: LiveData<File> = _liveCompatBackupFile

    private val _livePrefsRestored = MutableLiveData<Boolean>()
    val livePrefsRestored: LiveData<Boolean> = _livePrefsRestored

    fun backup(path: DocumentFile, checkedOptions: BooleanArray) {
        viewModelScope.launch(Dispatchers.IO) {
            _liveBackupFile.postValue(ReserveSettingsHandler.backup(path, checkedOptions))
        }
    }

    fun backup(path: File, checkedOptions: BooleanArray) {
        TODO("Not yet implemented")
    }

    fun restore(path: DocumentFile, checkedOptions: BooleanArray) {
        viewModelScope.launch(Dispatchers.IO) {
            ReserveSettingsHandler.restore(path, checkedOptions)
        }
    }

    fun restore(path: File, checkedOptions: BooleanArray) {
        TODO("Not yet implemented")
    }

    fun checkReserve(file: File): BooleanArray {
        TODO("Not yet implemented")
    }

    fun checkReserve(file: DocumentFile): BooleanArray {
        // прочитаю список файлов в архиве
        val filesInZip = ReserveSettingsHandler.getFilesList(file)
        val result = BooleanArray(15)
        result[0] = filesInZip.contains(ReserveSettingsHandler.PREF_BACKUP_NAME)

        result[1] = filesInZip.contains(ReserveSettingsHandler.DOWNLOADED_BOOKS_BACKUP_NAME)

        result[2] = filesInZip.contains(ReserveSettingsHandler.READED_BOOKS_BACKUP_NAME)

        result[3] = filesInZip.contains(ReserveSettingsHandler.AUTOFILL_BOOKS_BACKUP_NAME)
                || filesInZip.contains(ReserveSettingsHandler.AUTOFILL_AUTHOR_BACKUP_NAME)
                || filesInZip.contains(ReserveSettingsHandler.AUTOFILL_GENRE_BACKUP_NAME)
                || filesInZip.contains(ReserveSettingsHandler.AUTOFILL_SEQUENCE_BACKUP_NAME)
                || filesInZip.contains(ReserveSettingsHandler.AUTOFILL_BACKUP_NAME)

        result[4] = filesInZip.contains(ReserveSettingsHandler.BOOKMARKS_OPDS_BACKUP_NAME)

        result[5] = filesInZip.contains(ReserveSettingsHandler.SUBSCRIBE_BOOK_BACKUP_NAME)
                || filesInZip.contains(ReserveSettingsHandler.SUBSCRIBE_AUTHOR_BACKUP_NAME)
                || filesInZip.contains(ReserveSettingsHandler.SUBSCRIBE_GENRE_BACKUP_NAME)
                || filesInZip.contains(ReserveSettingsHandler.SUBSCRIBE_SEQUENCE_BACKUP_NAME)
                || filesInZip.contains(ReserveSettingsHandler.BOOKS_SUBSCRIBE_BACKUP_NAME)
                || filesInZip.contains(ReserveSettingsHandler.AUTHORS_SUBSCRIBE_BACKUP_NAME)
                || filesInZip.contains(ReserveSettingsHandler.SEQUENCES_SUBSCRIBE_BACKUP_NAME)
                || filesInZip.contains(ReserveSettingsHandler.GENRE_SUBSCRIBE_BACKUP_NAME)

        result[6] = filesInZip.contains(ReserveSettingsHandler.BLACKLIST_BOOK_BACKUP_NAME)
                || filesInZip.contains(ReserveSettingsHandler.BLACKLIST_AUTHOR_BACKUP_NAME)
                || filesInZip.contains(ReserveSettingsHandler.BLACKLIST_GENRE_BACKUP_NAME)
                || filesInZip.contains(ReserveSettingsHandler.BLACKLIST_SEQUENCE_BACKUP_NAME)
                || filesInZip.contains(ReserveSettingsHandler.BLACKLIST_BOOKS_BACKUP_NAME)
                || filesInZip.contains(ReserveSettingsHandler.BLACKLIST_AUTHORS_BACKUP_NAME)
                || filesInZip.contains(ReserveSettingsHandler.BLACKLIST_GENRES_BACKUP_NAME)
                || filesInZip.contains(ReserveSettingsHandler.BLACKLIST_SEQUENCES_BACKUP_NAME)
                || filesInZip.contains(ReserveSettingsHandler.BLACKLIST_FORMAT_BACKUP_NAME)
                || filesInZip.contains(ReserveSettingsHandler.BLACKLIST_MIME_BACKUP_NAME)

        result[7] = filesInZip.contains(ReserveSettingsHandler.DOWNLOADS_SCHEDULE_BACKUP_NAME)
                || filesInZip.contains(ReserveSettingsHandler.DOWNLOADS_SCHEDULE_ERROR_BACKUP_NAME)
                || filesInZip.contains(ReserveSettingsHandler.DOWNLOAD_SCHEDULE_BACKUP_NAME)
        return result
    }
}