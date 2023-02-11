package net.veldor.flibusta_test.model.view_model

import android.os.Build
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.model.db.DatabaseInstance
import net.veldor.flibusta_test.model.handler.SubscribesHandler
import net.veldor.flibusta_test.model.handler.XMLHandler
import java.io.*
import java.util.ArrayList
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


class PreferencesBackupViewModel : ViewModel() {
    companion object{
        private const val BUFFER = 1024
        const val PREF_BACKUP_NAME = "data1"
        const val DOWNLOADED_BOOKS_BACKUP_NAME = "data2"
        const val READED_BOOKS_BACKUP_NAME = "data3"
        const val AUTOFILL_BACKUP_NAME = "data4"
        const val BOOKS_SUBSCRIBE_BACKUP_NAME = "data5"
        const val AUTHORS_SUBSCRIBE_BACKUP_NAME = "data6"
        const val SEQUENCES_SUBSCRIBE_BACKUP_NAME = "data7"
        const val BOOKMARKS_BACKUP_NAME = "data8"
        const val DOWNLOAD_SCHEDULE_BACKUP_NAME = "data9"
        const val BLACKLIST_BOOKS_BACKUP_NAME = "data10"
        const val BLACKLIST_AUTHORS_BACKUP_NAME = "data11"
        const val BLACKLIST_GENRES_BACKUP_NAME = "data12"
        const val BLACKLIST_SEQUENCES_BACKUP_NAME = "data13"
        const val GENRE_SUBSCRIBE_BACKUP_NAME = "data14"
        const val BLACKLIST_FORMAT_BACKUP_NAME = "data15"

        // new entities
        const val AUTOFILL_BOOKS_BACKUP_NAME = "data20"
        const val AUTOFILL_AUTHOR_BACKUP_NAME = "data21"
        const val AUTOFILL_SEQUENCE_BACKUP_NAME = "data22"
        const val AUTOFILL_GENRE_BACKUP_NAME = "data23"
        const val BOOKMARKS_OPDS_BACKUP_NAME = "data24"
        const val SUBSCRIBE_BOOK_BACKUP_NAME = "data25"
        const val SUBSCRIBE_AUTHOR_BACKUP_NAME = "data26"
        const val SUBSCRIBE_GENRE_BACKUP_NAME = "data27"
        const val SUBSCRIBE_SEQUENCE_BACKUP_NAME = "data28"
        const val BLACKLIST_BOOK_BACKUP_NAME = "data29"
        const val BLACKLIST_AUTHOR_BACKUP_NAME = "data30"
        const val BLACKLIST_GENRE_BACKUP_NAME = "data31"
        const val BLACKLIST_SEQUENCE_BACKUP_NAME = "data32"
        const val BLACKLIST_MIME_BACKUP_NAME = "data33"
        const val DOWNLOADS_SCHEDULE_BACKUP_NAME = "data34"
        const val DOWNLOADS_SCHEDULE_ERROR_BACKUP_NAME = "data35"
    }
    fun doBackup(
        base: Boolean,
        downloaded: Boolean,
        read: Boolean,
        autofill: Boolean,
        bookmarks: Boolean,
        subscriptions: Boolean,
        filters: Boolean,
        downloadSchedule: Boolean,
        callback: (File) -> Unit
    ) {
        val backupFile = File.createTempFile("preferencesBackup", ".zip")
        val dest = FileOutputStream(backupFile)
        val out = ZipOutputStream(BufferedOutputStream(dest))
        val dataBuffer = ByteArray(BUFFER)
        if (base) {
            val sharedPrefsFile: File =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    File(App.instance.dataDir.toString() + "/shared_prefs/net.veldor.flibusta_test_preferences.xml")
                } else {
                    File(
                        Environment.getDataDirectory()
                            .toString() + "/shared_prefs/net.veldor.flibusta_test_preferences.xml"
                    )
                }
            writeToZip(out, dataBuffer, sharedPrefsFile, PREF_BACKUP_NAME)
        }
        if (downloaded) {
            val books = DatabaseInstance.mDatabase.downloadedBooksDao().allBooks
            if (books != null && books.isNotEmpty()) {
                // создам XML
                val xmlBuilder = StringBuilder()
                xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><downloaded_books>")
                for (book in books) {
                    xmlBuilder.append("<book id=\"")
                    xmlBuilder.append(book!!.bookId)
                    xmlBuilder.append("\"")
                    if(book.destination != null){
                        xmlBuilder.append(" destination=\"")
                        xmlBuilder.append(book.destination)
                        xmlBuilder.append("\"")
                    }
                    xmlBuilder.append("/>")
                }
                xmlBuilder.append("</downloaded_books>")
                val text = xmlBuilder.toString()
                writeTextToZip(
                    out, dataBuffer, text,
                    DOWNLOADED_BOOKS_BACKUP_NAME
                )
            }
        }
        if(read){
            val books = DatabaseInstance.mDatabase.readBooksDao().allBooks
            if (books != null && books.isNotEmpty()) {
                // создам XML
                val xmlBuilder = StringBuilder()
                xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><readed_books>")
                for (book in books) {
                    xmlBuilder.append("<book id=\"")
                    xmlBuilder.append(book!!.bookId)
                    xmlBuilder.append("\"/>")
                }
                xmlBuilder.append("</readed_books>")
                val text = xmlBuilder.toString()
                writeTextToZip(
                    out, dataBuffer, text,
                    READED_BOOKS_BACKUP_NAME
                )
            }
        }
        if(autofill){
            var autofillFile: File =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    File(App.instance.dataDir.toString() + "/files/searchBookAutocomplete.xml")
                } else {
                    File(
                        Environment.getDataDirectory()
                            .toString() + "/files/searchBookAutocomplete.xml"
                    )
                }
            if (autofillFile.isFile) {
                writeToZip(out, dataBuffer, autofillFile, AUTOFILL_BOOKS_BACKUP_NAME)
            }
            autofillFile =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    File(App.instance.dataDir.toString() + "/files/searchAuthorAutocomplete.xml")
                } else {
                    File(
                        Environment.getDataDirectory()
                            .toString() + "/files/searchAuthorAutocomplete.xml"
                    )
                }
            if (autofillFile.isFile) {
                writeToZip(out, dataBuffer, autofillFile, AUTOFILL_AUTHOR_BACKUP_NAME)
            }
            autofillFile =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    File(App.instance.dataDir.toString() + "/files/searchGenreAutocomplete.xml")
                } else {
                    File(
                        Environment.getDataDirectory()
                            .toString() + "/files/searchGenreAutocomplete.xml"
                    )
                }
            if (autofillFile.isFile) {
                writeToZip(out, dataBuffer, autofillFile, AUTOFILL_GENRE_BACKUP_NAME)
            }
            autofillFile =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    File(App.instance.dataDir.toString() + "/files/searchSequenceAutocomplete.xml")
                } else {
                    File(
                        Environment.getDataDirectory()
                            .toString() + "/files/searchSequenceAutocomplete.xml"
                    )
                }
            if (autofillFile.isFile) {
                writeToZip(out, dataBuffer, autofillFile, AUTOFILL_SEQUENCE_BACKUP_NAME)
            }
        }
        if(bookmarks){
            val bookmarksFile =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    File(App.instance.dataDir.toString() + "/files/bookmarksOpds.xml")
                } else {
                    File(
                        Environment.getDataDirectory()
                            .toString() + "/files/bookmarksOpds.xml"
                    )
                }
            if (bookmarksFile.isFile) {
                writeToZip(out, dataBuffer, bookmarksFile, BOOKMARKS_OPDS_BACKUP_NAME)
            }
        }
        if(subscriptions){
            var subscribeFile: File =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    File(App.instance.dataDir.toString() + "/files/booksSubscribe.xml")
                } else {
                    File(
                        Environment.getDataDirectory()
                            .toString() + "/files/booksSubscribe.xml"
                    )
                }
            if (subscribeFile.isFile) {
                writeToZip(out, dataBuffer, subscribeFile, SUBSCRIBE_BOOK_BACKUP_NAME)
            }
            subscribeFile =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    File(App.instance.dataDir.toString() + "/files/authorsSubscribe.xml")
                } else {
                    File(
                        Environment.getDataDirectory()
                            .toString() + "/files/authorsSubscribe.xml"
                    )
                }
            if (subscribeFile.isFile) {
                writeToZip(out, dataBuffer, subscribeFile, SUBSCRIBE_AUTHOR_BACKUP_NAME)
            }
            subscribeFile =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    File(App.instance.dataDir.toString() + "/files/genresSubscribe.xml")
                } else {
                    File(
                        Environment.getDataDirectory()
                            .toString() + "/files/genresSubscribe.xml"
                    )
                }
            if (subscribeFile.isFile) {
                writeToZip(out, dataBuffer, subscribeFile, SUBSCRIBE_GENRE_BACKUP_NAME)
            }
            subscribeFile =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    File(App.instance.dataDir.toString() + "/files/sequencesSubscribe.xml")
                } else {
                    File(
                        Environment.getDataDirectory()
                            .toString() + "/files/sequencesSubscribe.xml"
                    )
                }
            if (subscribeFile.isFile) {
                writeToZip(out, dataBuffer, subscribeFile, SUBSCRIBE_SEQUENCE_BACKUP_NAME)
            }
        }
        if(filters){
            var blacklistFile: File =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    File(App.instance.dataDir.toString() + "/files/booksBlacklist.xml")
                } else {
                    File(
                        Environment.getDataDirectory()
                            .toString() + "/files/booksBlacklist.xml"
                    )
                }
            if (blacklistFile.isFile) {
                writeToZip(out, dataBuffer, blacklistFile, BLACKLIST_BOOK_BACKUP_NAME)
            }
            blacklistFile =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    File(App.instance.dataDir.toString() + "/files/authorsBlacklist.xml")
                } else {
                    File(
                        Environment.getDataDirectory()
                            .toString() + "/files/authorsBlacklist.xml"
                    )
                }
            if (blacklistFile.isFile) {
                writeToZip(out, dataBuffer, blacklistFile, BLACKLIST_AUTHOR_BACKUP_NAME)
            }
            blacklistFile =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    File(App.instance.dataDir.toString() + "/files/genresBlacklist.xml")
                } else {
                    File(
                        Environment.getDataDirectory()
                            .toString() + "/files/genresBlacklist.xml"
                    )
                }
            if (blacklistFile.isFile) {
                writeToZip(out, dataBuffer, blacklistFile, BLACKLIST_GENRE_BACKUP_NAME)
            }
            blacklistFile =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    File(App.instance.dataDir.toString() + "/files/sequencesBlacklist.xml")
                } else {
                    File(
                        Environment.getDataDirectory()
                            .toString() + "/files/sequencesBlacklist.xml"
                    )
                }
            if (blacklistFile.isFile) {
                writeToZip(out, dataBuffer, blacklistFile, BLACKLIST_SEQUENCE_BACKUP_NAME)
            }
            blacklistFile =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    File(App.instance.dataDir.toString() + "/files/formatBlacklist.xml")
                } else {
                    File(
                        Environment.getDataDirectory()
                            .toString() + "/files/formatBlacklist.xml"
                    )
                }
            if (blacklistFile.isFile) {
                writeToZip(out, dataBuffer, blacklistFile, BLACKLIST_MIME_BACKUP_NAME)
            }
        }
        if(downloadSchedule){
            val schedule =
                DatabaseInstance.mDatabase.booksDownloadScheduleDao().allBooks
            if (schedule != null && schedule.isNotEmpty()) {
                // создам XML
                val xmlBuilder = StringBuilder()
                xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><download_schedule>")
                for (link in schedule) {
                    xmlBuilder.append("<link id=\"${link.bookId}\"")
                    xmlBuilder.append("link=\"${link.link}\"")
                    xmlBuilder.append(" name=\"${link.name}\"")
                    xmlBuilder.append(" size=\"${link.size}\"")
                    xmlBuilder.append(" author=\"${link.author}\"")
                    xmlBuilder.append(" format=\"${link.format}\"")
                    xmlBuilder.append(" authorDirName=\"${link.authorDirName}\"")
                    xmlBuilder.append(" sequenceDirName=\"${link.sequenceDirName}\"")
                    xmlBuilder.append(" reserveSequenceName=\"${link.reservedSequenceName}\"")
                    xmlBuilder.append("/>")
                }
                xmlBuilder.append("</download_schedule>")
                val text = xmlBuilder.toString()
                writeTextToZip(
                    out, dataBuffer, text,
                    DOWNLOADS_SCHEDULE_BACKUP_NAME
                )
            }
            val errorsSchedule = DatabaseInstance.mDatabase.downloadErrorDao().allBooks
            if (errorsSchedule != null && errorsSchedule.isNotEmpty()) {
                // создам XML
                val xmlBuilder = StringBuilder()
                xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><download_schedule_errors>")
                for (link in errorsSchedule) {
                    xmlBuilder.append("<link id=\"${link.bookId}\"")
                    xmlBuilder.append("link=\"${link.link}\"")
                    xmlBuilder.append(" name=\"${link.name}\"")
                    xmlBuilder.append(" author=\"${link.author}\"")
                    xmlBuilder.append(" format=\"${link.format}\"")
                    xmlBuilder.append(" authorDirName=\"${link.authorDirName}\"")
                    xmlBuilder.append(" sequenceDirName=\"${link.sequenceDirName}\"")
                    xmlBuilder.append(" reserveSequenceName=\"${link.reservedSequenceName}\"")
                    xmlBuilder.append(" size=\"${link.size}\"")
                    xmlBuilder.append(" error=\"${link.error}\"")
                    xmlBuilder.append("/>")
                }
                xmlBuilder.append("</download_schedule_errors>")
                val text = xmlBuilder.toString()
                writeTextToZip(
                    out, dataBuffer, text,
                    DOWNLOADS_SCHEDULE_ERROR_BACKUP_NAME
                )
            }
        }
        out.close()
        dest.close()
        callback(backupFile)
    }


    private fun writeToZip(
        stream: ZipOutputStream,
        dataBuffer: ByteArray,
        oldFileName: File,
        newFileName: String
    ) {
        if (oldFileName.exists()) {
            val fis: FileInputStream
            try {
                fis = FileInputStream(oldFileName)
                val origin = BufferedInputStream(fis, BUFFER)
                val entry = ZipEntry(newFileName)
                stream.putNextEntry(entry)
                var count: Int
                while (origin.read(dataBuffer, 0, BUFFER)
                        .also { count = it } != -1
                ) {
                    stream.write(dataBuffer, 0, count)
                }
                origin.close()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun writeTextToZip(
        stream: ZipOutputStream,
        dataBuffer: ByteArray,
        text: String,
        zipEntityName: String
    ) {
        try {
            val origin = BufferedInputStream(text.byteInputStream(), BUFFER)
            val entry = ZipEntry(zipEntityName)
            stream.putNextEntry(entry)
            var count: Int
            while (origin.read(dataBuffer, 0, BUFFER)
                    .also { count = it } != -1
            ) {
                stream.write(dataBuffer, 0, count)
            }
            origin.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    private fun extractFromZip(zis: ZipInputStream, fileName: File) {
        try {
            val fout = FileOutputStream(fileName)
            var c = zis.read()
            while (c != -1) {
                fout.write(c)
                c = zis.read()
            }
            zis.closeEntry()
            fout.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    private fun getFilesList(dl: File): ArrayList<String> {
        val result = ArrayList<String>()
        val fileData = dl.inputStream()
        val zin = ZipInputStream(fileData)
        var ze: ZipEntry?
        while (zin.nextEntry.also { ze = it } != null) {
            result.add(ze!!.name)
        }
        zin.close()
        fileData.close()
        return result
    }


    private fun getFilesList(dl: DocumentFile): ArrayList<String> {
        val result = ArrayList<String>()
        val fileData = App.instance.contentResolver.openInputStream(dl.uri)
        val zin = ZipInputStream(fileData)
        var ze: ZipEntry?
        while (zin.nextEntry.also { ze = it } != null) {
            result.add(ze!!.name)
        }
        return result
    }

    fun checkReserve(file: DocumentFile): BooleanArray {
        // прочитаю список файлов в архиве
        val filesInZip = getFilesList(file)
        val result = BooleanArray(15)
        result[0] = filesInZip.contains(PREF_BACKUP_NAME)

        result[1] = filesInZip.contains(DOWNLOADED_BOOKS_BACKUP_NAME)

        result[2] = filesInZip.contains(READED_BOOKS_BACKUP_NAME)

        result[3] = filesInZip.contains(AUTOFILL_BOOKS_BACKUP_NAME)
                || filesInZip.contains(AUTOFILL_AUTHOR_BACKUP_NAME)
                || filesInZip.contains(AUTOFILL_GENRE_BACKUP_NAME)
                || filesInZip.contains(AUTOFILL_SEQUENCE_BACKUP_NAME)
                || filesInZip.contains(AUTOFILL_BACKUP_NAME)

        result[4] = filesInZip.contains(BOOKMARKS_OPDS_BACKUP_NAME)

        result[5] = filesInZip.contains(SUBSCRIBE_BOOK_BACKUP_NAME)
                || filesInZip.contains(SUBSCRIBE_AUTHOR_BACKUP_NAME)
                || filesInZip.contains(SUBSCRIBE_GENRE_BACKUP_NAME)
                || filesInZip.contains(SUBSCRIBE_SEQUENCE_BACKUP_NAME)
                || filesInZip.contains(BOOKS_SUBSCRIBE_BACKUP_NAME)
                || filesInZip.contains(AUTHORS_SUBSCRIBE_BACKUP_NAME)
                || filesInZip.contains(SEQUENCES_SUBSCRIBE_BACKUP_NAME)
                || filesInZip.contains(GENRE_SUBSCRIBE_BACKUP_NAME)

        result[6] = filesInZip.contains(BLACKLIST_BOOK_BACKUP_NAME)
                || filesInZip.contains(BLACKLIST_AUTHOR_BACKUP_NAME)
                || filesInZip.contains(BLACKLIST_GENRE_BACKUP_NAME)
                || filesInZip.contains(BLACKLIST_SEQUENCE_BACKUP_NAME)
                || filesInZip.contains(BLACKLIST_BOOKS_BACKUP_NAME)
                || filesInZip.contains(BLACKLIST_AUTHORS_BACKUP_NAME)
                || filesInZip.contains(BLACKLIST_GENRES_BACKUP_NAME)
                || filesInZip.contains(BLACKLIST_SEQUENCES_BACKUP_NAME)
                || filesInZip.contains(BLACKLIST_FORMAT_BACKUP_NAME)
                || filesInZip.contains(BLACKLIST_MIME_BACKUP_NAME)

        result[7] = filesInZip.contains(DOWNLOADS_SCHEDULE_BACKUP_NAME)
                || filesInZip.contains(DOWNLOADS_SCHEDULE_ERROR_BACKUP_NAME)
                || filesInZip.contains(DOWNLOAD_SCHEDULE_BACKUP_NAME)
        return result
    }

    fun checkReserve(file: File): BooleanArray {
        val filesInZip = getFilesList(file)
        val result = BooleanArray(15)
        result[0] = filesInZip.contains(PREF_BACKUP_NAME)

        result[1] = filesInZip.contains(DOWNLOADED_BOOKS_BACKUP_NAME)

        result[2] = filesInZip.contains(READED_BOOKS_BACKUP_NAME)

        result[3] = filesInZip.contains(AUTOFILL_BOOKS_BACKUP_NAME)
                || filesInZip.contains(AUTOFILL_AUTHOR_BACKUP_NAME)
                || filesInZip.contains(AUTOFILL_GENRE_BACKUP_NAME)
                || filesInZip.contains(AUTOFILL_SEQUENCE_BACKUP_NAME)
                || filesInZip.contains(AUTOFILL_BACKUP_NAME)

        result[4] = filesInZip.contains(BOOKMARKS_OPDS_BACKUP_NAME)

        result[5] = filesInZip.contains(SUBSCRIBE_BOOK_BACKUP_NAME)
                || filesInZip.contains(SUBSCRIBE_AUTHOR_BACKUP_NAME)
                || filesInZip.contains(SUBSCRIBE_GENRE_BACKUP_NAME)
                || filesInZip.contains(SUBSCRIBE_SEQUENCE_BACKUP_NAME)
                || filesInZip.contains(BOOKS_SUBSCRIBE_BACKUP_NAME)
                || filesInZip.contains(AUTHORS_SUBSCRIBE_BACKUP_NAME)
                || filesInZip.contains(SEQUENCES_SUBSCRIBE_BACKUP_NAME)
                || filesInZip.contains(GENRE_SUBSCRIBE_BACKUP_NAME)

        result[6] = filesInZip.contains(BLACKLIST_BOOK_BACKUP_NAME)
                || filesInZip.contains(BLACKLIST_AUTHOR_BACKUP_NAME)
                || filesInZip.contains(BLACKLIST_GENRE_BACKUP_NAME)
                || filesInZip.contains(BLACKLIST_SEQUENCE_BACKUP_NAME)
                || filesInZip.contains(BLACKLIST_BOOKS_BACKUP_NAME)
                || filesInZip.contains(BLACKLIST_AUTHORS_BACKUP_NAME)
                || filesInZip.contains(BLACKLIST_GENRES_BACKUP_NAME)
                || filesInZip.contains(BLACKLIST_SEQUENCES_BACKUP_NAME)
                || filesInZip.contains(BLACKLIST_FORMAT_BACKUP_NAME)
                || filesInZip.contains(BLACKLIST_MIME_BACKUP_NAME)

        result[7] = filesInZip.contains(DOWNLOADS_SCHEDULE_BACKUP_NAME)
                || filesInZip.contains(DOWNLOADS_SCHEDULE_ERROR_BACKUP_NAME)
                || filesInZip.contains(DOWNLOAD_SCHEDULE_BACKUP_NAME)
        return result
    }

    fun restoreReserve(
        modernFile: DocumentFile,
        base: Boolean,
        downloaded: Boolean,
        read: Boolean,
        autofill: Boolean,
        bookmarks: Boolean,
        subscriptions: Boolean,
        filters: Boolean,
        downloadSchedule: Boolean,
        callback: () -> Unit?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val fileData = App.instance.contentResolver.openInputStream(modernFile.uri)
            restore(
                fileData,
                base,
                downloaded,
                read,
                autofill,
                bookmarks,
                subscriptions,
                filters,
                downloadSchedule,
                callback
            )
        }
    }

    fun restoreReserve(
        compatFile: File,
        base: Boolean,
        downloaded: Boolean,
        read: Boolean,
        autofill: Boolean,
        bookmarks: Boolean,
        subscriptions: Boolean,
        filters: Boolean,
        downloadSchedule: Boolean,
        callback: () -> Unit?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val fileData = compatFile.inputStream()
            restore(
                fileData,
                base,
                downloaded,
                read,
                autofill,
                bookmarks,
                subscriptions,
                filters,
                downloadSchedule,
                callback
            )
        }
    }

    private fun restore(
    inputStream: InputStream?,
    base: Boolean,
    downloaded: Boolean,
    read: Boolean,
    autofill: Boolean,
    bookmarks: Boolean,
    subscriptions: Boolean,
    filters: Boolean,
    downloadSchedule: Boolean,
    callback: () -> Unit?
    ){
        val zin = ZipInputStream(inputStream)
        var ze: ZipEntry?
        var targetFile: File
        while (zin.nextEntry.also { ze = it } != null) {
            when (ze!!.name) {
                PREF_BACKUP_NAME -> {
                    if (base) {
                        targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            File(App.instance.dataDir.toString() + "/shared_prefs/net.veldor.flibusta_test_preferences.xml")
                        } else {
                            File(
                                Environment.getDataDirectory()
                                    .toString() + "/shared_prefs/net.veldor.flibusta_test_preferences.xml"
                            )
                        }
                        extractFromZip(zin, targetFile)
                    }
                }
                DOWNLOADED_BOOKS_BACKUP_NAME -> {
                    if (downloaded) {
                        DatabaseInstance.mDatabase.downloadedBooksDao().deleteTable()
                        XMLHandler.handleBackup(zin)
                    }
                }
                READED_BOOKS_BACKUP_NAME -> {
                    if (read) {
                        DatabaseInstance.mDatabase.readBooksDao().deleteTable()
                        XMLHandler.handleBackup(zin)
                    }
                }
                AUTOFILL_BOOKS_BACKUP_NAME -> {
                    if (autofill) {
                        targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            File(App.instance.dataDir.toString() + "/files/searchBookAutocomplete.xml")
                        } else {
                            File(
                                Environment.getDataDirectory()
                                    .toString() + "/files/searchBookAutocomplete.xml"
                            )
                        }
                        extractFromZip(zin, targetFile)
                    }
                }
                AUTOFILL_AUTHOR_BACKUP_NAME -> {
                    if (autofill) {
                        targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            File(App.instance.dataDir.toString() + "/files/searchAuthorAutocomplete.xml")
                        } else {
                            File(
                                Environment.getDataDirectory()
                                    .toString() + "/files/searchAuthorAutocomplete.xml"
                            )
                        }
                        extractFromZip(zin, targetFile)
                    }
                }
                AUTOFILL_GENRE_BACKUP_NAME -> {
                    if (autofill) {
                        targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            File(App.instance.dataDir.toString() + "/files/searchGenreAutocomplete.xml")
                        } else {
                            File(
                                Environment.getDataDirectory()
                                    .toString() + "/files/searchGenreAutocomplete.xml"
                            )
                        }
                        extractFromZip(zin, targetFile)
                    }
                }
                AUTOFILL_SEQUENCE_BACKUP_NAME -> {
                    if (autofill) {
                        targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            File(App.instance.dataDir.toString() + "/files/searchSequenceAutocomplete.xml")
                        } else {
                            File(
                                Environment.getDataDirectory()
                                    .toString() + "/files/searchSequenceAutocomplete.xml"
                            )
                        }
                        extractFromZip(zin, targetFile)
                    }
                }
                BOOKMARKS_OPDS_BACKUP_NAME -> {
                    if (bookmarks) {
                        targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            File(App.instance.dataDir.toString() + "/files/bookmarksOpds.xml")
                        } else {
                            File(
                                Environment.getDataDirectory()
                                    .toString() + "/files/bookmarksOpds.xml"
                            )
                        }
                        extractFromZip(zin, targetFile)
                    }
                }
                SUBSCRIBE_BOOK_BACKUP_NAME -> {
                    if (subscriptions) {
                        targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            File(App.instance.dataDir.toString() + "/files/booksSubscribe.xml")
                        } else {
                            File(
                                Environment.getDataDirectory()
                                    .toString() + "/files/booksSubscribe.xml"
                            )
                        }
                        extractFromZip(zin, targetFile)
                    }
                }
                SUBSCRIBE_AUTHOR_BACKUP_NAME -> {
                    if (subscriptions) {
                        targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            File(App.instance.dataDir.toString() + "/files/authorsSubscribe.xml")
                        } else {
                            File(
                                Environment.getDataDirectory()
                                    .toString() + "/files/authorsSubscribe.xml"
                            )
                        }
                        extractFromZip(zin, targetFile)
                    }
                }
                SUBSCRIBE_GENRE_BACKUP_NAME -> {
                    if (subscriptions) {
                        targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            File(App.instance.dataDir.toString() + "/files/genresSubscribe.xml")
                        } else {
                            File(
                                Environment.getDataDirectory()
                                    .toString() + "/files/genresSubscribe.xml"
                            )
                        }
                        extractFromZip(zin, targetFile)
                    }
                }
                SUBSCRIBE_SEQUENCE_BACKUP_NAME -> {
                    if (subscriptions) {
                        targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            File(App.instance.dataDir.toString() + "/files/sequencesSubscribe.xml")
                        } else {
                            File(
                                Environment.getDataDirectory()
                                    .toString() + "/files/sequencesSubscribe.xml"
                            )
                        }
                        extractFromZip(zin, targetFile)
                    }
                }
                BLACKLIST_BOOK_BACKUP_NAME -> {
                    if (filters) {
                        targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            File(App.instance.dataDir.toString() + "/files/booksBlacklist.xml")
                        } else {
                            File(
                                Environment.getDataDirectory()
                                    .toString() + "/files/booksBlacklist.xml"
                            )
                        }
                        extractFromZip(zin, targetFile)
                    }
                }
                BLACKLIST_AUTHOR_BACKUP_NAME -> {
                    if (filters) {
                        targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            File(App.instance.dataDir.toString() + "/files/authorsBlacklist.xml")
                        } else {
                            File(
                                Environment.getDataDirectory()
                                    .toString() + "/files/authorsBlacklist.xml"
                            )
                        }
                        extractFromZip(zin, targetFile)
                    }
                }
                BLACKLIST_GENRE_BACKUP_NAME -> {
                    if (filters) {
                        targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            File(App.instance.dataDir.toString() + "/files/genresBlacklist.xml")
                        } else {
                            File(
                                Environment.getDataDirectory()
                                    .toString() + "/files/genresBlacklist.xml"
                            )
                        }
                        extractFromZip(zin, targetFile)
                    }
                }
                BLACKLIST_SEQUENCE_BACKUP_NAME -> {
                    if (filters) {
                        targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            File(App.instance.dataDir.toString() + "/files/sequencesBlacklist.xml")
                        } else {
                            File(
                                Environment.getDataDirectory()
                                    .toString() + "/files/sequencesBlacklist.xml"
                            )
                        }
                        extractFromZip(zin, targetFile)
                    }
                }
                BLACKLIST_MIME_BACKUP_NAME -> {
                    if (filters) {
                        targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            File(App.instance.dataDir.toString() + "/files/formatBlacklist.xml")
                        } else {
                            File(
                                Environment.getDataDirectory()
                                    .toString() + "/files/formatBlacklist.xml"
                            )
                        }
                        extractFromZip(zin, targetFile)
                    }
                }
                DOWNLOADS_SCHEDULE_BACKUP_NAME -> {
                    if (downloadSchedule) {
                        DatabaseInstance.mDatabase.booksDownloadScheduleDao()
                            .deleteTable()
                        XMLHandler.handleBackup(zin)
                    }
                }
                DOWNLOADS_SCHEDULE_ERROR_BACKUP_NAME -> {
                    if (downloadSchedule) {
                        DatabaseInstance.mDatabase.downloadErrorDao().deleteTable()
                        XMLHandler.handleBackup(zin)
                    }
                }
                // тут импортирую старые файлы
                AUTOFILL_BACKUP_NAME -> {
                    // добавлю файл как автозаполнение ко всем категориям
                    if (autofill) {
                        targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            File(App.instance.dataDir.toString() + "/files/searchSequenceAutocomplete.xml")
                        } else {
                            File(
                                Environment.getDataDirectory()
                                    .toString() + "/files/searchSequenceAutocomplete.xml"
                            )
                        }
                        extractFromZip(zin, targetFile)
                        targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            File(App.instance.dataDir.toString() + "/files/searchBookAutocomplete.xml")
                        } else {
                            File(
                                Environment.getDataDirectory()
                                    .toString() + "/files/searchBookAutocomplete.xml"
                            )
                        }
                        extractFromZip(zin, targetFile)
                        targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            File(App.instance.dataDir.toString() + "/files/searchAuthorAutocomplete.xml")
                        } else {
                            File(
                                Environment.getDataDirectory()
                                    .toString() + "/files/searchAuthorAutocomplete.xml"
                            )
                        }
                        extractFromZip(zin, targetFile)
                        targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            File(App.instance.dataDir.toString() + "/files/searchGenreAutocomplete.xml")
                        } else {
                            File(
                                Environment.getDataDirectory()
                                    .toString() + "/files/searchGenreAutocomplete.xml"
                            )
                        }
                        extractFromZip(zin, targetFile)
                    }
                }
                BOOKS_SUBSCRIBE_BACKUP_NAME -> {
                    if (subscriptions) {
                        targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            File(App.instance.dataDir.toString() + "/files/booksSubscribe.xml")
                        } else {
                            File(
                                Environment.getDataDirectory()
                                    .toString() + "/files/booksSubscribe.xml"
                            )
                        }
                        extractFromZip(zin, targetFile)
                        SubscribesHandler.convertToPatterns(targetFile)
                    }
                }
                AUTHORS_SUBSCRIBE_BACKUP_NAME -> {
                    if (subscriptions) {
                        targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            File(App.instance.dataDir.toString() + "/files/authorsSubscribe.xml")
                        } else {
                            File(
                                Environment.getDataDirectory()
                                    .toString() + "/files/authorsSubscribe.xml"
                            )
                        }
                        extractFromZip(zin, targetFile)
                        SubscribesHandler.convertToPatterns(targetFile)
                    }
                }
                SEQUENCES_SUBSCRIBE_BACKUP_NAME -> {
                    if (subscriptions) {
                        targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            File(App.instance.dataDir.toString() + "/files/sequencesSubscribe.xml")
                        } else {
                            File(
                                Environment.getDataDirectory()
                                    .toString() + "/files/sequencesSubscribe.xml"
                            )
                        }
                        extractFromZip(zin, targetFile)
                        SubscribesHandler.convertToPatterns(targetFile)
                    }
                }
                GENRE_SUBSCRIBE_BACKUP_NAME -> {
                    if (subscriptions) {
                        targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            File(App.instance.dataDir.toString() + "/files/genresSubscribe.xml")
                        } else {
                            File(
                                Environment.getDataDirectory()
                                    .toString() + "/files/genresSubscribe.xml"
                            )
                        }
                        extractFromZip(zin, targetFile)
                        SubscribesHandler.convertToPatterns(targetFile)
                    }
                }
                BLACKLIST_BOOKS_BACKUP_NAME -> {
                    if (filters) {
                        targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            File(App.instance.dataDir.toString() + "/files/booksBlacklist.xml")
                        } else {
                            File(
                                Environment.getDataDirectory()
                                    .toString() + "/files/booksBlacklist.xml"
                            )
                        }
                        extractFromZip(zin, targetFile)
                    }
                }
                BLACKLIST_AUTHORS_BACKUP_NAME -> {
                    if (filters) {
                        targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            File(App.instance.dataDir.toString() + "/files/authorsBlacklist.xml")
                        } else {
                            File(
                                Environment.getDataDirectory()
                                    .toString() + "/files/authorsBlacklist.xml"
                            )
                        }
                        extractFromZip(zin, targetFile)
                    }
                }
                BLACKLIST_GENRES_BACKUP_NAME -> {
                    if (filters) {
                        targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            File(App.instance.dataDir.toString() + "/files/genresBlacklist.xml")
                        } else {
                            File(
                                Environment.getDataDirectory()
                                    .toString() + "/files/genresBlacklist.xml"
                            )
                        }
                        extractFromZip(zin, targetFile)
                    }
                }
                BLACKLIST_SEQUENCES_BACKUP_NAME -> {
                    if (filters) {
                        targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            File(App.instance.dataDir.toString() + "/files/sequencesBlacklist.xml")
                        } else {
                            File(
                                Environment.getDataDirectory()
                                    .toString() + "/files/sequencesBlacklist.xml"
                            )
                        }
                        extractFromZip(zin, targetFile)
                    }
                }
                BLACKLIST_FORMAT_BACKUP_NAME -> {
                    if (filters) {
                        targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            File(App.instance.dataDir.toString() + "/files/formatBlacklist.xml")
                        } else {
                            File(
                                Environment.getDataDirectory()
                                    .toString() + "/files/formatBlacklist.xml"
                            )
                        }
                        extractFromZip(zin, targetFile)
                    }
                }

                DOWNLOAD_SCHEDULE_BACKUP_NAME -> {
                    if (downloadSchedule) {
                        DatabaseInstance.mDatabase.booksDownloadScheduleDao()
                            .deleteTable()
                        XMLHandler.handleBackup(zin)
                    }
                }

                BOOKMARKS_BACKUP_NAME -> {
                    if (bookmarks) {
                        DatabaseInstance.mDatabase.bookmarksDao().deleteTable()
                        XMLHandler.handleBackup(zin)
                    }
                }
            }
        }
        zin.close()
        inputStream?.close()
        callback()
    }
}