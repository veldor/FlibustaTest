package net.veldor.flibusta_test.model.handler

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.WorkManager
import net.lingala.zip4j.ZipFile
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.model.connection.Connector
import net.veldor.flibusta_test.model.converter.Fb2ToEpubConverter
import net.veldor.flibusta_test.model.db.DatabaseInstance
import net.veldor.flibusta_test.model.db.entity.BooksDownloadSchedule
import net.veldor.flibusta_test.model.db.entity.DownloadError
import net.veldor.flibusta_test.model.db.entity.DownloadedBooks
import net.veldor.flibusta_test.model.helper.MimeHelper
import net.veldor.flibusta_test.model.helper.NetworkHelper
import net.veldor.flibusta_test.model.selection.BooksDownloadProgress
import net.veldor.flibusta_test.model.selection.RootDownloadDir
import net.veldor.flibusta_test.model.worker.DownloadBooksWorker
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream


object DownloadHandler {
    val liveBookDownloadProgress: MutableLiveData<BooksDownloadProgress> = MutableLiveData()
    private var downloadAllWorker: OneTimeWorkRequest? = null
    private var currentWork: Operation? = null
    private var downloadCancelled: Boolean = false
    private val mutableDownloadInProgress = MutableLiveData(false)
    val downloadInProgress: LiveData<Boolean> = mutableDownloadInProgress

    fun download(book: BooksDownloadSchedule, currentProgress: BooksDownloadProgress): Boolean {
        Log.d("surprise", "DownloadHandler: 35 ${book.format}")
        Log.d("surprise", "DownloadHandler: 35 ${book.link}")
        var errorReason = ""
        try {
            downloadCancelled = false
            var lastTickTime = 0L
            currentProgress.currentlyLoadedBookName = book.name
            val startTime = System.currentTimeMillis()
            val request =
                Connector().rawRequest(book.link, false)
            if (request.statusCode < 400) {
                currentProgress.bookFullSize = request.longContentLength
                currentProgress.currentlyLoadedBookStartTime = System.currentTimeMillis()
                liveBookDownloadProgress.postValue(currentProgress)
                val extension = NetworkHelper().getExtensionFromHeaders(request.headers)
                val file =
                    File.createTempFile("book", extension)
                val out: OutputStream = FileOutputStream(file)
                var totalRead = 0
                var read: Int
                val buffer = ByteArray(1024)
                while (request.inputStream!!.read(buffer).also { read = it } > 0) {
                    if (downloadCancelled) {
                        // stop process
                        out.close()
                        request.inputStream?.close()
                        file.delete()
                        downloadCancelled = false
                        NotificationHandler.closeBookLoadingProgressNotification()
                        return false
                    }
                    totalRead += read
                    out.write(buffer, 0, read)
                    if (System.currentTimeMillis() - lastTickTime > 1000) {
                        lastTickTime = System.currentTimeMillis()
                        currentProgress.bookLoadedSize = file.length()
                        liveBookDownloadProgress.postValue(currentProgress)
                        if (PreferencesHandler.showDownloadProgress) {
                            NotificationHandler.createBookLoadingProgressNotification(
                                request.longContentLength,
                                file.length(),
                                book.name,
                                startTime,
                                lastTickTime
                            )
                        }
                    }
                }
                out.close()
                request.inputStream?.close()
                currentProgress.bookLoadedSize = file.length()
                liveBookDownloadProgress.postValue(currentProgress)
                NotificationHandler.closeBookLoadingProgressNotification()
                val rootDir = PreferencesHandler.rootDownloadDir
                if (PreferencesHandler.unzipLoaded) {
                    Log.d("surprise", "DownloadHandler: 89 unzip loaded file")
                    unzipLoaded(rootDir, file, book, extension)
                } else {
                    rootDir.saveFile(file, book, extension)
                }
                if (PreferencesHandler.sendToKindle) {
                    Log.d("surprise", "DownloadHandler: 98 send book to kindle")
                    SendToKindleHandler().send(rootDir)
                }
                if (PreferencesHandler.useCompanionApp && PreferencesHandler.autosendToCompanionApp) {
                    if (rootDir.compatDestinationFile != null) {
                        if (PreferencesHandler.isConvertFb2ForCompanion && rootDir.compatDestinationFile!!.name.endsWith(
                                ".fb2"
                            )
                        ) {
                            Log.d(
                                "surprise",
                                "CompanionAppHandler: 22 convert fb2 to epub for send"
                            )
                            val epubFile =
                                Fb2ToEpubConverter().getEpubFile(rootDir.compatDestinationFile!!)
                            val encodedFile = GrammarHandler.toBase64(epubFile)
                            CompanionAppHandler.sendViaWorker(
                                encodedFile,
                                rootDir.getCompatFileRelativePath().replace("fb2", "epub")
                            )
                        } else {
                            val encodedFile =
                                GrammarHandler.toBase64(rootDir.compatDestinationFile!!)
                            CompanionAppHandler.sendViaWorker(
                                encodedFile,
                                rootDir.getCompatFileRelativePath()
                            )
                        }
                    } else if (rootDir.destinationFile != null) {
                        if (PreferencesHandler.isConvertFb2ForCompanion && rootDir.destinationFile!!.name?.endsWith(
                                ".fb2"
                            ) == true
                        ) {
                            Log.d(
                                "surprise",
                                "CompanionAppHandler: 22 convert fb2 to epub for send"
                            )
                            val epubFile =
                                Fb2ToEpubConverter().getEpubFile(rootDir.destinationFile!!)
                            val encodedFile = GrammarHandler.toBase64(epubFile)
                            CompanionAppHandler.sendViaWorker(
                                encodedFile,
                                rootDir.getFileRelativePath().replace("fb2", "epub")
                            )
                        } else {
                            val encodedFile =
                                GrammarHandler.toBase64(App.instance, rootDir.destinationFile!!)
                            CompanionAppHandler.sendViaWorker(
                                encodedFile,
                                rootDir.getFileRelativePath()
                            )
                        }
                    }
                }
                if (DatabaseInstance.mDatabase.downloadedBooksDao()
                        .getBookById(book.bookId) == null
                ) {
                    val newItem = DownloadedBooks()
                    newItem.bookId = book.bookId
                    newItem.destination = rootDir.destinationFileUri?.toString()
                    newItem.relativePath = rootDir.getRelativePath()
                    DatabaseInstance.mDatabase.downloadedBooksDao()
                        .insert(newItem)
                }
                NotificationHandler.createSuccessBookLoadNotification(
                    rootDir
                )
                return true
            }
        } catch (t: Throwable) {
            errorReason = t.message ?: "Unknown error"
            t.printStackTrace()
        }
        val errorInfo = DownloadError()
        errorInfo.copyDataFrom(book)
        errorInfo.error = errorReason
        DatabaseInstance.mDatabase.downloadErrorDao().insert(errorInfo)
        NotificationHandler.closeBookLoadingProgressNotification()
        NotificationHandler.showBookDownloadErrorNotification(errorInfo)
        return false
    }

    private fun unzipLoaded(
        rootDir: RootDownloadDir,
        file: File,
        book: BooksDownloadSchedule,
        extension: String
    ) {
        if (file.name.endsWith("zip")) {
            val zipFile = ZipFile(file)
            // count entities in file
            val fileHeaders = zipFile.fileHeaders
            if (fileHeaders.size == 1) {
                val extractedFileName = fileHeaders[0].fileName
                Log.d("surprise", "DownloadHandler.kt 372: $extractedFileName")
                zipFile.extractFile(fileHeaders[0], App.instance.cacheDir.path)
                val extractedFile = File(App.instance.cacheDir, extractedFileName)
                val newExtension = extractedFile.extension
                book.format = FormatHandler.getFullFromShortMime(newExtension)
                rootDir.saveFile(extractedFile, book, newExtension)
            } else {
                rootDir.saveFile(file, book, extension)
            }
        } else {
            rootDir.saveFile(file, book, extension)
        }
    }

    fun cancelDownload() {
        downloadCancelled = true
        if (downloadAllWorker != null) {
            WorkManager.getInstance(App.instance).cancelWorkById(downloadAllWorker!!.id)
        }
    }

    fun startDownload() {
        if (mutableDownloadInProgress.value == false) {
            mutableDownloadInProgress.postValue(true)
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
        mutableDownloadInProgress.postValue(false)
    }

    private const val TAG_DOWNLOAD = "download"
}