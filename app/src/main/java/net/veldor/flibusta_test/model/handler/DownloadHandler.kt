package net.veldor.flibusta_test.model.handler

import android.os.Build
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.WorkManager
import com.msopentech.thali.toronionproxy.FileUtilities
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.model.db.DatabaseInstance
import net.veldor.flibusta_test.model.db.entity.BooksDownloadSchedule
import net.veldor.flibusta_test.model.db.entity.DownloadError
import net.veldor.flibusta_test.model.db.entity.DownloadedBooks
import net.veldor.flibusta_test.model.exception.WrongDownloadDirException
import net.veldor.flibusta_test.model.selections.BooksDownloadProgress
import net.veldor.flibusta_test.model.utils.RandomString
import net.veldor.flibusta_test.model.web.UniversalWebClient
import net.veldor.flibusta_test.model.worker.DownloadBooksWorker
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class DownloadHandler private constructor() {

    val liveBookDownloadProgress: MutableLiveData<BooksDownloadProgress> = MutableLiveData()
    private var downloadAllWorker: OneTimeWorkRequest? = null
    private var currentWork: Operation? = null
    private var downloadCancelled: Boolean = false
    private val _downloadInProgress = MutableLiveData(false)
    val downloadInProgress: LiveData<Boolean> = _downloadInProgress

    fun download(book: BooksDownloadSchedule, currentProgress: BooksDownloadProgress): Boolean {
        Log.d("surprise", "DownloadHandler.kt 40: progress download book here")
        var lastTickTime = 0L
        currentProgress.currentlyLoadedBookName = book.name
        val startTime = System.currentTimeMillis()
        if (PreferencesHandler.instance.showDownloadProgress) {
            NotificationHandler.instance.createBookLoadingProgressNotification(
                0,
                0,
                book.name,
                startTime,
                System.currentTimeMillis() + 100
            )
        }
        downloadCancelled = false
        try {
            currentProgress.bookLoadedSize = 0
            liveBookDownloadProgress.postValue(currentProgress)
            val response = UniversalWebClient().rawRequest(book.link, false)
            if (response.statusCode in 200..310 && response.inputStream != null) {
                currentProgress.bookFullSize = response.longContentLength
                currentProgress.currentlyLoadedBookStartTime = System.currentTimeMillis()
                liveBookDownloadProgress.postValue(currentProgress)
                if (PreferencesHandler.instance.showDownloadProgress) {
                    NotificationHandler.instance.createBookLoadingProgressNotification(
                        response.longContentLength,
                        0,
                        book.name,
                        startTime,
                        System.currentTimeMillis()
                    )
                }
                // ready to load
                val tempFile = File.createTempFile(RandomString().nextString(), null)
                tempFile.deleteOnExit()
                val out: OutputStream = FileOutputStream(tempFile)
                var read: Int
                val buffer = ByteArray(1024)
                while (response.inputStream.read(buffer).also { read = it } > 0) {
                    if (downloadCancelled) {
                        // stop process
                        out.close()
                        response.inputStream.close()
                        tempFile.delete()
                        downloadCancelled = false
                        NotificationHandler.instance.closeBookLoadingProgressNotification()
                        return false
                    }
                    out.write(buffer, 0, read)
                    if (System.currentTimeMillis() - lastTickTime > 1000) {
                        lastTickTime = System.currentTimeMillis()
                        currentProgress.bookLoadedSize = tempFile.length()
                        liveBookDownloadProgress.postValue(currentProgress)
                        if (PreferencesHandler.instance.showDownloadProgress) {
                            NotificationHandler.instance.createBookLoadingProgressNotification(
                                response.longContentLength,
                                tempFile.length(),
                                book.name,
                                startTime,
                                lastTickTime
                            )
                        }
                    }
                }
                out.close()
                response.inputStream.close()
                currentProgress.bookLoadedSize = tempFile.length()
                liveBookDownloadProgress.postValue(currentProgress)
                NotificationHandler.instance.closeBookLoadingProgressNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val destinationDir = getDestinationDir(book)
                    if (destinationDir == null || destinationDir.name == null) {
                        val errorInfo = DownloadError()
                        errorInfo.copyDataFrom(book)
                        errorInfo.error = "Не удалось идентифицировать папку загрузок"
                        DatabaseInstance.instance.mDatabase.downloadErrorDao().insert(errorInfo)
                        NotificationHandler.instance.closeBookLoadingProgressNotification()
                        NotificationHandler.instance.showBookDownloadErrorNotification(errorInfo)
                        throw WrongDownloadDirException()
                    }
                    var destinationFile = destinationDir.findFile(book.name)
                    if (destinationFile != null) {
                        if (destinationFile.length() != tempFile.length()) {
                            val namePart = book.name.substringBeforeLast(".")
                            val mimePart = book.name.substringAfterLast(".")
                            var counter = 1
                            while (true) {
                                val test =
                                    destinationDir.findFile("$namePart ($counter) .$mimePart")
                                if (test == null) {
                                    destinationFile = destinationDir.createFile(
                                        book.format,
                                        "$namePart ($counter).$mimePart"
                                    )
                                    break
                                } else {
                                    if (test.length() == tempFile.length()) {
                                        destinationFile = test
                                        break
                                    }
                                }
                                // make a new name
                                counter++
                            }
                        }
                    } else {
                        destinationFile = destinationDir.createFile(book.format, book.name)
                    }
                    // check destination file exists
                    if (destinationFile != null) {
                        val uri = destinationFile.uri
                        val stream = App.instance.contentResolver.openOutputStream(uri)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            Files.copy(tempFile.toPath(), stream)
                        } else {
                            tempFile.inputStream().copyTo(stream!!)
                        }
                        stream?.flush()
                        stream?.close()
                        // если файл загружен и всё ок- покажу уведомление о скачанной книге
                        if (destinationFile.isFile && destinationFile.length() > 100) {
                            if (PreferencesHandler.instance.sendToKindle && destinationFile.name?.endsWith(
                                    ".mobi"
                                ) == true
                            ) {
                                SendToKindleHandler().send(destinationFile)
                            }
                            if (DatabaseInstance.instance.mDatabase.downloadedBooksDao()
                                    .getBookById(book.bookId) == null
                            ) {
                                val newItem = DownloadedBooks()
                                newItem.bookId = book.bookId
                                DatabaseInstance.instance.mDatabase.downloadedBooksDao()
                                    .insert(newItem)
                            }
                            try {
                                Log.d("surprise", "download: ${destinationFile.type}")
                                // try to unzip file
                                if (destinationFile.type == "application/zip" && PreferencesHandler.instance.unzipLoaded) {
                                    // try to unzip file
                                    destinationFile = unzipFile(destinationFile)!!
                                }
                            } catch (t: Throwable) {
                                t.printStackTrace()
                            }
                            NotificationHandler.instance.createSuccessBookLoadNotification(
                                destinationFile
                            )
                            NotificationHandler.instance.closeBookLoadingProgressNotification()
                            return true
                        }
                    }
                } else {
                    val destinationDir = getCompatDestinationDir(book)
                    Log.d(
                        "surprise",
                        "DownloadHandler.kt 189: destination dir is ${destinationDir?.path}"
                    )
                    if (destinationDir != null) {
                        var destinationFile = File(destinationDir, book.name)
                        Log.d(
                            "surprise",
                            "DownloadHandler.kt 192: destination file is $destinationFile"
                        )
                        if (destinationFile.isFile) {
                            Log.d("surprise", "DownloadHandler.kt 194: file exists")
                            if (destinationFile.length() != tempFile.length()) {
                                val namePart = book.name.substringBeforeLast(".")
                                val mimePart = book.name.substringAfterLast(".")
                                var counter = 1
                                while (true) {
                                    val test =
                                        File(destinationDir, "$namePart ($counter) .$mimePart")
                                    if (!test.isFile) {
                                        destinationFile = File(
                                            destinationDir,
                                            "$namePart ($counter).$mimePart"
                                        )
                                        break
                                    } else {
                                        if (test.length() == tempFile.length()) {
                                            destinationFile = test
                                            break
                                        }
                                    }
                                    // make a new name
                                    counter++
                                }
                            }
                        }
                        // check destination file exists
                        tempFile.inputStream().copyTo(destinationFile.outputStream())
                        // если файл загружен и всё ок- покажу уведомление о скачанной книге
                        if (destinationFile.isFile && destinationFile.length() > 100) {
                            if (PreferencesHandler.instance.sendToKindle && destinationFile.name?.endsWith(
                                    ".mobi"
                                ) == true
                            ) {
                                SendToKindleHandler().send(destinationFile)
                            }
                            if (DatabaseInstance.instance.mDatabase.downloadedBooksDao()
                                    .getBookById(book.bookId) == null
                            ) {
                                val newItem = DownloadedBooks()
                                newItem.bookId = book.bookId
                                DatabaseInstance.instance.mDatabase.downloadedBooksDao()
                                    .insert(newItem)
                            }
                            try {
                                // try to unzip file
                                if (destinationFile.name.endsWith(".zip") && PreferencesHandler.instance.unzipLoaded) {
                                    // try to unzip file
                                    Log.d(
                                        "surprise",
                                        "DownloadHandler.kt 249: not implemented unzip file here"
                                    )
                                    //todo unzipping file
                                    //destinationFile = unzipFile(destinationFile)!!
                                }
                            } catch (t: Throwable) {
                                t.printStackTrace()
                            }
                            NotificationHandler.instance.createSuccessBookLoadNotification(
                                destinationFile
                            )
                            NotificationHandler.instance.closeBookLoadingProgressNotification()
                            return true
                        }
                    } else {
                        return false
                    }
                }
            }
        } catch (e: WrongDownloadDirException) {
            throw WrongDownloadDirException()
        } catch (e: Throwable) {
            Log.d("surprise", "download: book load error found")
            e.printStackTrace()
        }
        val errorInfo = DownloadError()
        errorInfo.copyDataFrom(book)
        DatabaseInstance.instance.mDatabase.downloadErrorDao().insert(errorInfo)
        NotificationHandler.instance.closeBookLoadingProgressNotification()
        NotificationHandler.instance.showBookDownloadErrorNotification(errorInfo)
        return false
    }

    private fun unzipFile(destinationFile: DocumentFile): DocumentFile? {
        Log.d("surprise", "unzipFile: unzipping")
        // count files in array
        var size = 0
        val inputStream = App.instance.contentResolver.openInputStream(destinationFile.uri)
        ZipInputStream(inputStream).use { zis ->
            while (true) {
                zis.nextEntry ?: break
                size += 1
            }
        }
        Log.d("surprise", "unzipFile: files in zip: $size")
        inputStream?.close()


        // создам временный файл zip
        val tempZip = File.createTempFile(RandomString().nextString(), null)
        tempZip.deleteOnExit()
        val iStream = App.instance.contentResolver.openInputStream(destinationFile.uri)
        val oStream = FileOutputStream(tempZip)

        if (iStream != null) {
            val buffer = ByteArray(1024)
            var read: Int
            while (iStream.read(buffer).also { read = it } != -1) {
                oStream.write(buffer, 0, read)
            }
            iStream.close()
            // write the output file (You have now copied the file)
            // write the output file (You have now copied the file)
            oStream.flush()
            oStream.close()
            // попробую разархивировать
            val zipInputStream = ZipInputStream(FileInputStream(tempZip))
            var zipEntry: ZipEntry?
            // count entries

            while (zipInputStream.nextEntry.also { zipEntry = it } != null) {
                val newName = destinationFile.name?.dropLast(4)
                val file = File(tempZip.parentFile, zipEntry!!.name)
                // check the file is target format
                if (newName?.substringAfterLast(".") != file.name.substringAfterLast(".")) {
                    Log.d("surprise", "unzipFile: skip wrong file ${file.name}")
                    continue
                }
                if (zipEntry!!.isDirectory) {
                    if (!file.exists() && !file.mkdirs()) {
                        throw RuntimeException("Could not create directory $file")
                    }
                } else {
                    if (file.exists() && !file.delete()) {
                        throw RuntimeException(
                            "Could not delete file in preparation for overwriting it. File - " +
                                    file.absolutePath
                        )
                    }
                    if (!file.createNewFile()) {
                        throw RuntimeException("Could not create file $file")
                    }
                    val fileOutputStream: OutputStream = FileOutputStream(file)
                    FileUtilities.copyDoNotCloseInput(zipInputStream, fileOutputStream)
                    Log.d("surprise", "unzipFile: success unzipped ${file.name}")
                    // copy file to destination path and delete
                    Log.d("surprise", "unzipFile: new name is $newName")
                    val root = destinationFile.parentFile
                    if (root != null) {
                        var tf = root.findFile(newName)
                        if (tf != null) {
                            if (destinationFile.length() != file.length()) {
                                val namePart = file.name.substringBeforeLast(".")
                                val mimePart = file.name.substringAfterLast(".")
                                var counter = 1
                                while (true) {
                                    val test = root.findFile("$namePart ($counter) .$mimePart")
                                    if (test == null) {
                                        tf = root.createFile(
                                            FormatHandler.getFullFromShortMime(mimePart),
                                            "$namePart ($counter).$mimePart"
                                        )
                                        break
                                    } else {
                                        if (test.length() == file.length()) {
                                            tf = test
                                            break
                                        }
                                    }
                                    // make a new name
                                    counter++
                                }
                            }
                        } else {
                            tf = root.createFile(
                                FormatHandler.getFullFromShortMime(
                                    file.name.substringAfterLast(".")
                                ), newName
                            )
                        }
                        if (tf != null) {
                            val uri = tf.uri
                            val stream = App.instance.contentResolver.openOutputStream(uri)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                Files.copy(file.toPath(), stream)
                            } else {
                                file.inputStream().copyTo(stream!!)
                            }
                            stream?.flush()
                            stream?.close()
                            // ready!!
                            file.delete()
                            Log.d("surprise", "unzipFile: unzipped!!")
                            destinationFile.delete()
                            return tf
                        }
                        // now copy temp file to new file and delete zip

                    }
                }
            }
        }
        return null
    }

    private fun getDestinationDir(book: BooksDownloadSchedule): DocumentFile? {
        var targetDir = PreferencesHandler.instance.getDownloadDir()
        if (targetDir != null) {
            if (book.reservedSequenceName.isNotEmpty()) {
                return targetDir.createDirIfNotExists(book.reservedSequenceName)
            }
            if (!PreferencesHandler.instance.createAuthorDir &&
                !PreferencesHandler.instance.createSequenceDir
            ) {
                // создам файл
                return targetDir
            } else if (PreferencesHandler.instance.createAuthorDir
                && !PreferencesHandler.instance.createSequenceDir
            ) {
                // create author dir and not create sequence dir
                if (PreferencesHandler.instance.createDifferentDirs) {
                    targetDir = targetDir.createDirIfNotExists("Авторы")
                }
                return if (book.author.trim().isNotEmpty()) {
                    targetDir?.createDirIfNotExists(book.author.trim())
                } else {
                    targetDir?.createDirIfNotExists("Автор неизвестен")
                }
            } else if (!PreferencesHandler.instance.createAuthorDir
                && PreferencesHandler.instance.createSequenceDir
            ) {
                // create sequence dir and not create author dir
                if (book.sequenceDirName.isNotEmpty()) {
                    if (PreferencesHandler.instance.createDifferentDirs) {
                        targetDir = targetDir.createDirIfNotExists("Серии")
                    }
                    // сохраню в папке первой серии
                    val subDirs = book.sequenceDirName.split("$|$")
                    return targetDir?.createDirIfNotExists(subDirs[0])
                }
            } else {
                // если есть серия
                if (book.sequenceDirName.isNotEmpty()) {
                    // если выбрано сохранение серий внутри папки автора- сохраню внутри
                    if (PreferencesHandler.instance.sequencesInAuthorDir) {
                        if (PreferencesHandler.instance.createDifferentDirs) {
                            targetDir = targetDir.createDirIfNotExists("Авторы")
                        }
                        targetDir = if (book.author.trim().isNotEmpty()) {
                            targetDir?.createDirIfNotExists(book.author.trim())
                        } else {
                            targetDir?.createDirIfNotExists("Автор неизвестен")
                        }
                        val subDirs = book.sequenceDirName.split("$|$")
                        return targetDir?.createDirIfNotExists(subDirs[0])
                    } else {
                        if (PreferencesHandler.instance.createDifferentDirs) {
                            targetDir = targetDir.createDirIfNotExists("Серии")
                        }
                        val subDirs = book.sequenceDirName.split("$|$")
                        return targetDir?.createDirIfNotExists(subDirs[0])
                    }
                } else {
                    if (PreferencesHandler.instance.createDifferentDirs) {
                        targetDir = targetDir.createDirIfNotExists("Авторы")
                    }
                    return if (book.author.trim().isNotEmpty()) {
                        targetDir?.createDirIfNotExists(book.author.trim())
                    } else {
                        targetDir?.createDirIfNotExists("Автор неизвестен")
                    }
                }
            }
        }
        return null
    }

    private fun getCompatDestinationDir(book: BooksDownloadSchedule): File? {
        var targetDir = PreferencesHandler.instance.getCompatDownloadDir()
        if (targetDir != null) {
            if (book.reservedSequenceName.isNotEmpty()) {
                return targetDir.createDirIfNotExists(book.reservedSequenceName)
            }
            if (!PreferencesHandler.instance.createAuthorDir &&
                !PreferencesHandler.instance.createSequenceDir
            ) {
                // создам файл
                return targetDir
            } else if (PreferencesHandler.instance.createAuthorDir
                && !PreferencesHandler.instance.createSequenceDir
            ) {
                // create author dir and not create sequence dir
                if (PreferencesHandler.instance.createDifferentDirs) {
                    targetDir = targetDir.createDirIfNotExists("Авторы")
                }
                return if (book.author.trim().isNotEmpty()) {
                    targetDir.createDirIfNotExists(book.author.trim())
                } else {
                    targetDir.createDirIfNotExists("Автор неизвестен")
                }
            } else if (!PreferencesHandler.instance.createAuthorDir
                && PreferencesHandler.instance.createSequenceDir
            ) {
                // create sequence dir and not create author dir
                if (book.sequenceDirName.isNotEmpty()) {
                    if (PreferencesHandler.instance.createDifferentDirs) {
                        targetDir = targetDir.createDirIfNotExists("Серии")
                    }
                    // сохраню в папке первой серии
                    val subDirs = book.sequenceDirName.split("$|$")
                    return targetDir.createDirIfNotExists(subDirs[0])
                }
            } else {
                // если есть серия
                if (book.sequenceDirName.isNotEmpty()) {
                    // если выбрано сохранение серий внутри папки автора- сохраню внутри
                    if (PreferencesHandler.instance.sequencesInAuthorDir) {
                        if (PreferencesHandler.instance.createDifferentDirs) {
                            targetDir = targetDir.createDirIfNotExists("Авторы")
                        }
                        targetDir = if (book.author.trim().isNotEmpty()) {
                            targetDir.createDirIfNotExists(book.author.trim())
                        } else {
                            targetDir.createDirIfNotExists("Автор неизвестен")
                        }
                        val subDirs = book.sequenceDirName.split("$|$")
                        return targetDir.createDirIfNotExists(subDirs[0])
                    } else {
                        if (PreferencesHandler.instance.createDifferentDirs) {
                            targetDir = targetDir.createDirIfNotExists("Серии")
                        }
                        val subDirs = book.sequenceDirName.split("$|$")
                        return targetDir.createDirIfNotExists(subDirs[0])
                    }
                } else {
                    if (PreferencesHandler.instance.createDifferentDirs) {
                        targetDir = targetDir.createDirIfNotExists("Авторы")
                    }
                    return if (book.author.trim().isNotEmpty()) {
                        targetDir.createDirIfNotExists(book.author.trim())
                    } else {
                        targetDir.createDirIfNotExists("Автор неизвестен")
                    }
                }
            }
        }
        return null
    }

    fun cancelDownload() {
        downloadCancelled = true
        if (downloadAllWorker != null) {
            WorkManager.getInstance(App.instance).cancelWorkById(downloadAllWorker!!.id)
        }
    }

    fun startDownload() {
        if (_downloadInProgress.value == false) {
            _downloadInProgress.postValue(true)
            // запущу рабочего, который будет грузить книжки
            downloadAllWorker = OneTimeWorkRequest.Builder(
                DownloadBooksWorker::class.java
            )
                .addTag(TAG_DOWNLOAD)
                .build()
            currentWork = WorkManager.getInstance(App.instance)
                .enqueueUniqueWork(
                    TAG_DOWNLOAD,
                    ExistingWorkPolicy.REPLACE,
                    downloadAllWorker!!
                )
        }
    }

    fun downloadFinished() {
        _downloadInProgress.postValue(false)
    }

    companion object {
        @JvmStatic
        var instance: DownloadHandler = DownloadHandler()
            private set
        const val TAG_DOWNLOAD = "download"
    }
}

private fun File.createDirIfNotExists(dirName: String): File {
    val target = File(this, dirName)
    if (!target.isDirectory) {
        target.mkdir()
    }
    return target
}

fun DocumentFile.createDirIfNotExists(name: String): DocumentFile? {
    if (findFile(name) == null) {
        return createDirectory(name)
    }
    return findFile(name)
}