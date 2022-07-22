package net.veldor.flibusta_test.model.handler

import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.model.db.DatabaseInstance
import net.veldor.flibusta_test.model.selections.subscribe.SubscribeType
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ReserveSettingsHandler {

    fun backup(dir: File, options: BooleanArray): File? {
        /*try {
            if (dir.exists() && dir.isDirectory) {
                if (!dir.canWrite()) {
                    Log.d("surprise", "can't write in dir $dir")
                } else {
                    val sdf = SimpleDateFormat("yyyy MM dd", Locale.ENGLISH)
                    val filename =
                        "Резервная копия Flibusta downloader от " + sdf.format(Date()) + ".zip"
                    sCompatBackupFile =
                        File(dir, filename)
                    sCompatBackupFile!!.createNewFile()
                    val dest = FileOutputStream(sCompatBackupFile)
                    val out = ZipOutputStream(BufferedOutputStream(dest))
                    val dataBuffer = ByteArray(BUFFER)
                    val backupDir =
                        File(Environment.getExternalStorageDirectory(), BACKUP_DIR_NAME)
                    if (!backupDir.exists()) {
                        val result = backupDir.mkdirs()
                        if (result) {
                            Log.d("surprise", "ReserveWorker doWork: dir created")
                        }
                    }
                    if (options[0]) {
                        val sharedPrefsFile: File =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                File(App.instance.dataDir.toString() + "/shared_prefs/net.veldor.flibustaloader_preferences.xml")
                            } else {
                                File(
                                    Environment.getDataDirectory()
                                        .toString() + "/shared_prefs/net.veldor.flibustaloader_preferences.xml"
                                )
                            }
                        writeToZip(out, dataBuffer, sharedPrefsFile, PREF_BACKUP_NAME)
                    }
                    if (options[3]) {
                        // сохраню автозаполнение поиска
                        val autocompleteFile =
                            File(App.instance.filesDir, MyFileReader.SEARCH_AUTOCOMPLETE_FILE)
                        if (autocompleteFile.isFile) {
                            writeToZip(
                                out, dataBuffer, autocompleteFile,
                                AUTOFILL_BACKUP_NAME
                            )
                        }
                    }
                    // сохраню чёрные списки
                    var blacklistFile =
                        File(App.instance.filesDir, MyFileReader.BOOKS_BLACKLIST_FILE)
                    if (options[9]) {
                        if (blacklistFile.isFile) {
                            writeToZip(
                                out, dataBuffer, blacklistFile,
                                BLACKLIST_BOOKS_BACKUP_NAME
                            )
                        }
                    }
                    if (options[10]) {
                        blacklistFile =
                            File(App.instance.filesDir, MyFileReader.AUTHORS_BLACKLIST_FILE)
                        if (blacklistFile.isFile) {
                            writeToZip(
                                out, dataBuffer, blacklistFile,
                                BLACKLIST_AUTHORS_BACKUP_NAME
                            )
                        }
                    }
                    if (options[11]) {
                        blacklistFile =
                            File(App.instance.filesDir, MyFileReader.GENRES_BLACKLIST_FILE)
                        if (blacklistFile.isFile) {
                            writeToZip(
                                out, dataBuffer, blacklistFile,
                                BLACKLIST_GENRES_BACKUP_NAME
                            )
                        }
                    }
                    if (options[12]) {
                        blacklistFile =
                            File(App.instance.filesDir, MyFileReader.SEQUENCES_BLACKLIST_FILE)
                        if (blacklistFile.isFile) {
                            writeToZip(
                                out, dataBuffer, blacklistFile,
                                BLACKLIST_SEQUENCES_BACKUP_NAME
                            )
                        }
                    }
                    if (options[13]) {
                        blacklistFile =
                            File(App.instance.filesDir, MyFileReader.FORMAT_BLACKLIST_FILE)
                        if (blacklistFile.isFile) {
                            writeToZip(
                                out, dataBuffer, blacklistFile,
                                BLACKLIST_FORMAT_BACKUP_NAME
                            )
                        }
                    }
                    var subscriptionFile =
                        File(App.instance.filesDir, MyFileReader.BOOKS_SUBSCRIBE_FILE)
                    if (options[5]) {
                        // сохраню подписки
                        if (subscriptionFile.isFile) {
                            writeToZip(
                                out, dataBuffer, subscriptionFile,
                                BOOKS_SUBSCRIBE_BACKUP_NAME
                            )
                        }
                    }
                    if (options[6]) {
                        subscriptionFile =
                            File(App.instance.filesDir, MyFileReader.AUTHORS_SUBSCRIBE_FILE)
                        if (subscriptionFile.isFile) {
                            writeToZip(
                                out, dataBuffer, subscriptionFile,
                                AUTHORS_SUBSCRIBE_BACKUP_NAME
                            )
                        }
                    }
                    if (options[8]) {
                        subscriptionFile =
                            File(App.instance.filesDir, MyFileReader.SEQUENCES_SUBSCRIBE_FILE)
                        if (subscriptionFile.isFile) {
                            writeToZip(
                                out, dataBuffer, subscriptionFile,
                                SEQUENCES_SUBSCRIBE_BACKUP_NAME
                            )
                        }
                    }
                    if (options[7]) {
                        subscriptionFile =
                            File(App.instance.filesDir, MyFileReader.GENRES_SUBSCRIBE_FILE)
                        if (subscriptionFile.isFile) {
                            writeToZip(
                                out, dataBuffer, subscriptionFile,
                                GENRE_SUBSCRIBE_BACKUP_NAME
                            )
                        }
                    }

                    // первым делом- получу из базы данных списки прочитанных и скачанных книг
                    val books = db.downloadedBooksDao().allBooks
                    if (books != null && books.isNotEmpty()) {
                        // создам XML
                        val xmlBuilder = StringBuilder()
                        xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><downloaded_books>")
                        for (book in books) {
                            xmlBuilder.append("<book id=\"")
                            xmlBuilder.append(book!!.bookId)
                            xmlBuilder.append("\"/>")
                        }
                        xmlBuilder.append("</downloaded_books>")
                        val text = xmlBuilder.toString()
                        Log.d("surprise", "ReserveSettingsWorker doWork $text")
                        val f1 = File(backupDir, "downloaded_books")
                        val writer = FileWriter(f1)
                        writer.append(text)
                        writer.flush()
                        writer.close()
                        if (options[1]) {
                            writeToZip(
                                out, dataBuffer, f1,
                                DOWNLOADED_BOOKS_BACKUP_NAME
                            )
                        }
                        val result = f1.delete()
                        if (!result) {
                            Log.d(
                                "surprise",
                                "ReserveSettingsWorker doWork не удалось удалить временный файл"
                            )
                        }
                    }
                    val rBooks = db.readBooksDao().allBooks
                    if (rBooks != null && rBooks.isNotEmpty()) {
                        // создам XML
                        val xmlBuilder = StringBuilder()
                        xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><readed_books>")
                        for (book in rBooks) {
                            xmlBuilder.append("<book id=\"")
                            xmlBuilder.append(book!!.bookId)
                            xmlBuilder.append("\"/>")
                        }
                        xmlBuilder.append("</readed_books>")
                        val text = xmlBuilder.toString()
                        val f1 = File(backupDir, "readed_books")
                        val writer = FileWriter(f1)
                        writer.append(text)
                        writer.flush()
                        writer.close()
                        if (options[2]) {
                            writeToZip(out, dataBuffer, f1, READED_BOOKS_BACKUP_NAME)
                        }
                        val result = f1.delete()
                        if (!result) {
                            Log.d(
                                "surprise",
                                "ReserveSettingsWorker doWork не удалось удалить временный файл"
                            )
                        }
                    }
                    // закладки
                    val rBookmarks = db.bookmarksDao().allBookmarks
                    if (rBookmarks.isNotEmpty()) {
                        // создам XML
                        val xmlBuilder = StringBuilder()
                        xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><bookmarks>")
                        for (bookmark in rBookmarks) {
                            xmlBuilder.append("<bookmark name=\"")
                            xmlBuilder.append(bookmark.name)
                            xmlBuilder.append("\" link=\"")
                            xmlBuilder.append(bookmark.link)
                            xmlBuilder.append("\"/>")
                        }
                        xmlBuilder.append("</bookmarks>")
                        val text = xmlBuilder.toString()
                        val f1 = File(backupDir, "bookmarks")
                        val writer = FileWriter(f1)
                        writer.append(text)
                        writer.flush()
                        writer.close()
                        if (options[4]) {
                            writeToZip(out, dataBuffer, f1, BOOKMARKS_BACKUP_NAME)
                        }
                        val result = f1.delete()
                        if (!result) {
                            Log.d(
                                "surprise",
                                "ReserveSettingsWorker doWork не удалось удалить временный файл"
                            )
                        }
                    }
                    // список загрузки
                    val rDownloadSchedule = db.booksDownloadScheduleDao().allBooks
                    if (rDownloadSchedule != null && rDownloadSchedule.isNotEmpty()) {
                        // создам XML
                        val xmlBuilder = StringBuilder()
                        xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><schedule>")
                        for (schedule in rDownloadSchedule) {
                            xmlBuilder.append("<item bookId=\"")
                            xmlBuilder.append(schedule.bookId)
                            xmlBuilder.append("\" link=\"")
                            xmlBuilder.append(schedule.link)
                            xmlBuilder.append("\" name=\"")
                            xmlBuilder.append(schedule.name)
                            xmlBuilder.append("\" size=\"")
                            xmlBuilder.append(schedule.size)
                            xmlBuilder.append("\" author=\"")
                            xmlBuilder.append(schedule.author)
                            xmlBuilder.append("\" format=\"")
                            xmlBuilder.append(schedule.format)
                            xmlBuilder.append("\" authorDirName=\"")
                            xmlBuilder.append(schedule.authorDirName)
                            xmlBuilder.append("\" sequenceDirName=\"")
                            xmlBuilder.append(schedule.sequenceDirName)
                            xmlBuilder.append("\" reservedSequenceName=\"")
                            xmlBuilder.append(schedule.reservedSequenceName)
                            xmlBuilder.append("\"/>")
                        }
                        xmlBuilder.append("</schedule>")
                        val text = xmlBuilder.toString()
                        val f1 = File(backupDir, "schedule")
                        val writer = FileWriter(f1)
                        writer.append(text)
                        writer.flush()
                        writer.close()
                        if (options[14]) {
                            writeToZip(
                                out, dataBuffer, f1,
                                DOWNLOAD_SCHEDULE_BACKUP_NAME
                            )
                        }
                        val result = f1.delete()
                        if (!result) {
                            Log.d(
                                "surprise",
                                "ReserveSettingsWorker doWork не удалось удалить временный файл"
                            )
                        }
                    }
                    out.close()
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        Log.d("surprise", "BackupSettings.kt 159  backup: size is ${sCompatBackupFile?.length()}")*/
        return sCompatBackupFile
    }

    fun backup(dir: DocumentFile, options: BooleanArray): DocumentFile? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val sdf = SimpleDateFormat("yyyy/MM/dd HH-mm-ss", Locale.ENGLISH)
            val filename = "Резервная копия Flibusta downloader от " + sdf.format(Date())
            sBackupFile =
                dir.createFile("application/zip", filename)
            val dataBuffer = ByteArray(BUFFER)
            val out =
                ZipOutputStream(App.instance.contentResolver.openOutputStream(sBackupFile!!.uri))
            if (options[0]) {
                // сохраню файл с общими настройками
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
            if (options[1]) {
                // сохраню данные из базы с загруженными книгами
                val books = DatabaseInstance.instance.mDatabase.downloadedBooksDao().allBooks
                if (books != null && books.isNotEmpty()) {
                    // создам XML
                    val xmlBuilder = StringBuilder()
                    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><downloaded_books>")
                    for (book in books) {
                        xmlBuilder.append("<book id=\"")
                        xmlBuilder.append(book!!.bookId)
                        xmlBuilder.append("\"/>")
                    }
                    xmlBuilder.append("</downloaded_books>")
                    val text = xmlBuilder.toString()
                    writeTextToZip(
                        out, dataBuffer, text,
                        DOWNLOADED_BOOKS_BACKUP_NAME
                    )
                }
            }
            if (options[2]) {
                // сохраню данные из базы с загруженными книгами
                val books = DatabaseInstance.instance.mDatabase.readBooksDao().allBooks
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
            if (options[3]) {
                // сохраню все автозаполнения
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
            if (options[4]) {
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
            if (options[5]) {
                // сохраню все подписки
                var autofillFile: File =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        File(App.instance.dataDir.toString() + "/files/booksSubscribe.xml")
                    } else {
                        File(
                            Environment.getDataDirectory()
                                .toString() + "/files/booksSubscribe.xml"
                        )
                    }
                if (autofillFile.isFile) {
                    writeToZip(out, dataBuffer, autofillFile, SUBSCRIBE_BOOK_BACKUP_NAME)
                }
                autofillFile =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        File(App.instance.dataDir.toString() + "/files/authorsSubscribe.xml")
                    } else {
                        File(
                            Environment.getDataDirectory()
                                .toString() + "/files/authorsSubscribe.xml"
                        )
                    }
                if (autofillFile.isFile) {
                    writeToZip(out, dataBuffer, autofillFile, SUBSCRIBE_AUTHOR_BACKUP_NAME)
                }
                autofillFile =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        File(App.instance.dataDir.toString() + "/files/genresSubscribe.xml")
                    } else {
                        File(
                            Environment.getDataDirectory()
                                .toString() + "/files/genresSubscribe.xml"
                        )
                    }
                if (autofillFile.isFile) {
                    writeToZip(out, dataBuffer, autofillFile, SUBSCRIBE_GENRE_BACKUP_NAME)
                }
                autofillFile =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        File(App.instance.dataDir.toString() + "/files/sequencesSubscribe.xml")
                    } else {
                        File(
                            Environment.getDataDirectory()
                                .toString() + "/files/sequencesSubscribe.xml"
                        )
                    }
                if (autofillFile.isFile) {
                    writeToZip(out, dataBuffer, autofillFile, SUBSCRIBE_SEQUENCE_BACKUP_NAME)
                }
            }
            if (options[6]) {
                // сохраню фильтры
                var autofillFile: File =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        File(App.instance.dataDir.toString() + "/files/booksBlacklist.xml")
                    } else {
                        File(
                            Environment.getDataDirectory()
                                .toString() + "/files/booksBlacklist.xml"
                        )
                    }
                if (autofillFile.isFile) {
                    writeToZip(out, dataBuffer, autofillFile, BLACKLIST_BOOK_BACKUP_NAME)
                }
                autofillFile =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        File(App.instance.dataDir.toString() + "/files/authorsBlacklist.xml")
                    } else {
                        File(
                            Environment.getDataDirectory()
                                .toString() + "/files/authorsBlacklist.xml"
                        )
                    }
                if (autofillFile.isFile) {
                    writeToZip(out, dataBuffer, autofillFile, BLACKLIST_AUTHOR_BACKUP_NAME)
                }
                autofillFile =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        File(App.instance.dataDir.toString() + "/files/genresBlacklist.xml")
                    } else {
                        File(
                            Environment.getDataDirectory()
                                .toString() + "/files/genresBlacklist.xml"
                        )
                    }
                if (autofillFile.isFile) {
                    writeToZip(out, dataBuffer, autofillFile, BLACKLIST_GENRE_BACKUP_NAME)
                }
                autofillFile =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        File(App.instance.dataDir.toString() + "/files/sequencesBlacklist.xml")
                    } else {
                        File(
                            Environment.getDataDirectory()
                                .toString() + "/files/sequencesBlacklist.xml"
                        )
                    }
                if (autofillFile.isFile) {
                    writeToZip(out, dataBuffer, autofillFile, BLACKLIST_SEQUENCE_BACKUP_NAME)
                }
                autofillFile =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        File(App.instance.dataDir.toString() + "/files/formatBlacklist.xml")
                    } else {
                        File(
                            Environment.getDataDirectory()
                                .toString() + "/files/formatBlacklist.xml"
                        )
                    }
                if (autofillFile.isFile) {
                    writeToZip(out, dataBuffer, autofillFile, BLACKLIST_MIME_BACKUP_NAME)
                }
            }
            if (options[7]) {
                // сохраню данные из базы с загруженными книгами
                val schedule =
                    DatabaseInstance.instance.mDatabase.booksDownloadScheduleDao().allBooks
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
                val errorsSchedule = DatabaseInstance.instance.mDatabase.downloadErrorDao().allBooks
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
        }
        return sBackupFile
    }

    fun restore(file: DocumentFile, options: BooleanArray) {
        Log.d("surprise", "ReserveSettingsHandler.kt 882: restoring...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val fileData = App.instance.contentResolver.openInputStream(file.uri)
            val zin = ZipInputStream(fileData)
            var ze: ZipEntry?
            var targetFile: File
            while (zin.nextEntry.also { ze = it } != null) {
                Log.d("surprise", "ReserveSettingsHandler.kt 605: ${ze!!.name}")
                when (ze!!.name) {
                    PREF_BACKUP_NAME -> {
                        if (options[0]) {
                            targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                File(App.instance.dataDir.toString() + "/shared_prefs/net.veldor.flibusta_test_preferences.xml")
                            } else {
                                File(
                                    Environment.getDataDirectory()
                                        .toString() + "/shared_prefs/net.veldor.flibusta_test_preferences.xml"
                                )
                            }
                            extractFromZip(zin, targetFile)
                            Log.d(
                                "surprise",
                                "ReserveSettingsHandler.kt 616: global preferences restored"
                            )
                        }
                    }
                    DOWNLOADED_BOOKS_BACKUP_NAME -> {
                        if (options[1]) {
                            DatabaseInstance.instance.mDatabase.downloadedBooksDao().deleteTable()
                            XMLHandler.handleBackup(zin)
                            Log.d(
                                "surprise",
                                "ReserveSettingsHandler.kt 625: downloaded books restored"
                            )
                        }
                    }
                    READED_BOOKS_BACKUP_NAME -> {
                        if (options[2]) {
                            DatabaseInstance.instance.mDatabase.readBooksDao().deleteTable()
                            XMLHandler.handleBackup(zin)
                            Log.d(
                                "surprise",
                                "ReserveSettingsHandler.kt 625: read books restored"
                            )
                        }
                    }
                    AUTOFILL_BOOKS_BACKUP_NAME -> {
                        if (options[3]) {
                            targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                File(App.instance.dataDir.toString() + "/files/searchBookAutocomplete.xml")
                            } else {
                                File(
                                    Environment.getDataDirectory()
                                        .toString() + "/files/searchBookAutocomplete.xml"
                                )
                            }
                            extractFromZip(zin, targetFile)
                            Log.d(
                                "surprise",
                                "ReserveSettingsHandler.kt 616: autofill books restored"
                            )
                        }
                    }
                    AUTOFILL_AUTHOR_BACKUP_NAME -> {
                        if (options[3]) {
                            targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                File(App.instance.dataDir.toString() + "/files/searchAuthorAutocomplete.xml")
                            } else {
                                File(
                                    Environment.getDataDirectory()
                                        .toString() + "/files/searchAuthorAutocomplete.xml"
                                )
                            }
                            extractFromZip(zin, targetFile)
                            Log.d(
                                "surprise",
                                "ReserveSettingsHandler.kt 616: autofill author restored"
                            )
                        }
                    }
                    AUTOFILL_GENRE_BACKUP_NAME -> {
                        if (options[3]) {
                            targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                File(App.instance.dataDir.toString() + "/files/searchGenreAutocomplete.xml")
                            } else {
                                File(
                                    Environment.getDataDirectory()
                                        .toString() + "/files/searchGenreAutocomplete.xml"
                                )
                            }
                            extractFromZip(zin, targetFile)
                            Log.d(
                                "surprise",
                                "ReserveSettingsHandler.kt 616: autofill genre restored"
                            )
                        }
                    }
                    AUTOFILL_SEQUENCE_BACKUP_NAME -> {
                        if (options[3]) {
                            targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                File(App.instance.dataDir.toString() + "/files/searchSequenceAutocomplete.xml")
                            } else {
                                File(
                                    Environment.getDataDirectory()
                                        .toString() + "/files/searchSequenceAutocomplete.xml"
                                )
                            }
                            extractFromZip(zin, targetFile)
                            Log.d(
                                "surprise",
                                "ReserveSettingsHandler.kt 616: autofill sequence restored"
                            )
                        }
                    }
                    BOOKMARKS_OPDS_BACKUP_NAME -> {
                        if (options[4]) {
                            targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                File(App.instance.dataDir.toString() + "/files/bookmarksOpds.xml")
                            } else {
                                File(
                                    Environment.getDataDirectory()
                                        .toString() + "/files/bookmarksOpds.xml"
                                )
                            }
                            extractFromZip(zin, targetFile)
                            Log.d(
                                "surprise",
                                "ReserveSettingsHandler.kt 616: bookmarks restored"
                            )
                        }
                    }
                    SUBSCRIBE_BOOK_BACKUP_NAME -> {
                        if (options[5]) {
                            targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                File(App.instance.dataDir.toString() + "/files/booksSubscribe.xml")
                            } else {
                                File(
                                    Environment.getDataDirectory()
                                        .toString() + "/files/booksSubscribe.xml"
                                )
                            }
                            extractFromZip(zin, targetFile)
                            Log.d(
                                "surprise",
                                "ReserveSettingsHandler.kt 616: book subscribes restored"
                            )
                        }
                    }
                    SUBSCRIBE_AUTHOR_BACKUP_NAME -> {
                        if (options[5]) {
                            targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                File(App.instance.dataDir.toString() + "/files/authorsSubscribe.xml")
                            } else {
                                File(
                                    Environment.getDataDirectory()
                                        .toString() + "/files/authorsSubscribe.xml"
                                )
                            }
                            extractFromZip(zin, targetFile)
                            Log.d(
                                "surprise",
                                "ReserveSettingsHandler.kt 616: authors subscribes restored"
                            )
                        }
                    }
                    SUBSCRIBE_GENRE_BACKUP_NAME -> {
                        if (options[5]) {
                            targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                File(App.instance.dataDir.toString() + "/files/genresSubscribe.xml")
                            } else {
                                File(
                                    Environment.getDataDirectory()
                                        .toString() + "/files/genresSubscribe.xml"
                                )
                            }
                            extractFromZip(zin, targetFile)
                            Log.d(
                                "surprise",
                                "ReserveSettingsHandler.kt 616: genre subscribes restored"
                            )
                        }
                    }
                    SUBSCRIBE_SEQUENCE_BACKUP_NAME -> {
                        if (options[5]) {
                            targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                File(App.instance.dataDir.toString() + "/files/sequencesSubscribe.xml")
                            } else {
                                File(
                                    Environment.getDataDirectory()
                                        .toString() + "/files/sequencesSubscribe.xml"
                                )
                            }
                            extractFromZip(zin, targetFile)
                            Log.d(
                                "surprise",
                                "ReserveSettingsHandler.kt 616: genre sequencesSubscribe restored"
                            )
                        }
                    }
                    BLACKLIST_BOOK_BACKUP_NAME -> {
                        if (options[6]) {
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
                        if (options[6]) {
                            targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                File(App.instance.dataDir.toString() + "/files/authorsBlacklist.xml")
                            } else {
                                File(
                                    Environment.getDataDirectory()
                                        .toString() + "/files/authorsBlacklist.xml"
                                )
                            }
                            extractFromZip(zin, targetFile)
                            Log.d(
                                "surprise",
                                "ReserveSettingsHandler.kt 616: authorsBlacklist restored"
                            )
                        }
                    }
                    BLACKLIST_GENRE_BACKUP_NAME -> {
                        if (options[6]) {
                            targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                File(App.instance.dataDir.toString() + "/files/genresBlacklist.xml")
                            } else {
                                File(
                                    Environment.getDataDirectory()
                                        .toString() + "/files/genresBlacklist.xml"
                                )
                            }
                            extractFromZip(zin, targetFile)
                            Log.d(
                                "surprise",
                                "ReserveSettingsHandler.kt 616: genresBlacklist restored"
                            )
                        }
                    }
                    BLACKLIST_SEQUENCE_BACKUP_NAME -> {
                        if (options[6]) {
                            targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                File(App.instance.dataDir.toString() + "/files/sequencesBlacklist.xml")
                            } else {
                                File(
                                    Environment.getDataDirectory()
                                        .toString() + "/files/sequencesBlacklist.xml"
                                )
                            }
                            extractFromZip(zin, targetFile)
                            Log.d(
                                "surprise",
                                "ReserveSettingsHandler.kt 616: sequencesBlacklist restored"
                            )
                        }
                    }
                    BLACKLIST_MIME_BACKUP_NAME -> {
                        if (options[6]) {
                            targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                File(App.instance.dataDir.toString() + "/files/formatBlacklist.xml")
                            } else {
                                File(
                                    Environment.getDataDirectory()
                                        .toString() + "/files/formatBlacklist.xml"
                                )
                            }
                            extractFromZip(zin, targetFile)
                            Log.d(
                                "surprise",
                                "ReserveSettingsHandler.kt 616: sequencesBlacklist restored"
                            )
                        }
                    }
                    DOWNLOADS_SCHEDULE_BACKUP_NAME -> {
                        if (options[7]) {
                            DatabaseInstance.instance.mDatabase.booksDownloadScheduleDao()
                                .deleteTable()
                            XMLHandler.handleBackup(zin)
                            Log.d(
                                "surprise",
                                "ReserveSettingsHandler.kt 625: download schedule restored"
                            )
                        }
                    }
                    DOWNLOADS_SCHEDULE_ERROR_BACKUP_NAME -> {
                        if (options[7]) {
                            DatabaseInstance.instance.mDatabase.downloadErrorDao().deleteTable()
                            XMLHandler.handleBackup(zin)
                            Log.d(
                                "surprise",
                                "ReserveSettingsHandler.kt 625: download schedule errors restored"
                            )
                        }
                    }
                    // тут импортирую старые файлы
                    AUTOFILL_BACKUP_NAME -> {
                        // добавлю файл как автозаполнение ко всем категориям
                        if (options[3]) {
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
                        if (options[5]) {
                            targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                File(App.instance.dataDir.toString() + "/files/booksSubscribe.xml")
                            } else {
                                File(
                                    Environment.getDataDirectory()
                                        .toString() + "/files/booksSubscribe.xml"
                                )
                            }
                            extractFromZip(zin, targetFile)
                            SubscribesHandler.instance.convertToPatterns(targetFile)
                        }
                    }
                    AUTHORS_SUBSCRIBE_BACKUP_NAME -> {
                        if (options[5]) {
                            targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                File(App.instance.dataDir.toString() + "/files/authorsSubscribe.xml")
                            } else {
                                File(
                                    Environment.getDataDirectory()
                                        .toString() + "/files/authorsSubscribe.xml"
                                )
                            }
                            extractFromZip(zin, targetFile)
                            SubscribesHandler.instance.convertToPatterns(targetFile)
                        }
                    }
                    SEQUENCES_SUBSCRIBE_BACKUP_NAME -> {
                        if (options[5]) {
                            targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                File(App.instance.dataDir.toString() + "/files/sequencesSubscribe.xml")
                            } else {
                                File(
                                    Environment.getDataDirectory()
                                        .toString() + "/files/sequencesSubscribe.xml"
                                )
                            }
                            extractFromZip(zin, targetFile)
                            SubscribesHandler.instance.convertToPatterns(targetFile)
                        }
                    }
                    GENRE_SUBSCRIBE_BACKUP_NAME -> {
                        if (options[5]) {
                            targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                File(App.instance.dataDir.toString() + "/files/genresSubscribe.xml")
                            } else {
                                File(
                                    Environment.getDataDirectory()
                                        .toString() + "/files/genresSubscribe.xml"
                                )
                            }
                            extractFromZip(zin, targetFile)
                            SubscribesHandler.instance.convertToPatterns(targetFile)
                        }
                    }
                    BLACKLIST_BOOKS_BACKUP_NAME -> {
                        if (options[6]) {
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
                        if (options[6]) {
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
                        if (options[6]) {
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
                        if (options[6]) {
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
                        if (options[6]) {
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
                        if (options[7]) {
                            DatabaseInstance.instance.mDatabase.booksDownloadScheduleDao()
                                .deleteTable()
                            XMLHandler.handleBackup(zin)
                        }
                    }

                    BOOKMARKS_BACKUP_NAME -> {
                        if (options[1]) {
                            DatabaseInstance.instance.mDatabase.downloadedBooksDao().deleteTable()
                            XMLHandler.handleBackup(zin)
                        }
                    }
                }
            }
            zin.close()
        }
    }

    fun restore(file: File, options: BooleanArray) {

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
        val fis: FileInputStream
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

    fun getFilesList(dl: DocumentFile): ArrayList<String> {
        val result = ArrayList<String>()
        val fileData = App.instance.contentResolver.openInputStream(dl.uri)
        val zin = ZipInputStream(fileData)
        var ze: ZipEntry?
        while (zin.nextEntry.also { ze = it } != null) {
            result.add(ze!!.name)
        }
        return result
    }

    fun getFilesList(dl: File): ArrayList<String> {
        val result = ArrayList<String>()
        val fileData = dl.inputStream()
        val zin = ZipInputStream(fileData)
        var ze: ZipEntry?
        while (zin.nextEntry.also { ze = it } != null) {
            result.add(ze!!.name)
        }
        return result
    }


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

    @JvmField
    var sBackupFile: DocumentFile? = null
    private var sCompatBackupFile: File? = null
}