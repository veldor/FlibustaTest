package net.veldor.flibusta_test.model.selection

import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.db.entity.BooksDownloadSchedule
import net.veldor.flibusta_test.model.handler.FormatHandler
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.helper.MimeHelper
import net.veldor.flibusta_test.model.helper.UrlHelper
import java.io.*
import java.nio.file.Files

class RootDownloadDir {
    val destinationFileUri: Uri?
        get() {
            if (destinationFile != null) {
                return destinationFile?.uri
            }
            if (compatDestinationFile != null) {
                return compatDestinationFile?.toUri()
            }
            return null
        }

    var destinationFile: DocumentFile? = null
    var compatDestinationFile: File? = null
    fun canWrite(): Boolean {
        if (root != null) {
            Log.d("surprise", "canWrite 15:  check modern root")
            return root?.exists() == true && root?.isDirectory == true && root?.canWrite() == true
        }
        if (compatRoot != null) {
            Log.d("surprise", "canWrite 17:  check compat root")
            return compatRoot?.exists() == true && compatRoot?.isDirectory == true && compatRoot?.canWrite() == true
        }
        return false
    }

    fun saveFile(file: File, book: BooksDownloadSchedule, extension: String) {
        val baseExtension = MimeHelper.getDownloadMime(book.format)
        if (baseExtension != extension) {
            book.name = "${book.name}.$baseExtension"
        }
        book.name = "${book.name}.$extension"
        book.format = FormatHandler.getFullFromShortMime(extension)
        if (root != null) {
            // create necessary dir structure
            var currentRoot = root
            if (book.reservedSequenceName.isNotEmpty()) {
                currentRoot = currentRoot!!.createDirIfNotExists(book.reservedSequenceName)
                copyFileData(currentRoot, book, extension, file)
            } else if (!PreferencesHandler.createAuthorDir && !PreferencesHandler.createSequenceDir) {
                copyFileData(currentRoot, book, extension, file)
            } else if (PreferencesHandler.createAuthorDir
                && !PreferencesHandler.createSequenceDir
            ) {
                //==== создаю только папку автора
                if (PreferencesHandler.createDifferentDirs) {
                    currentRoot = currentRoot!!.createDirIfNotExists("Авторы")
                }
                if (book.authorDirName.isNotEmpty()) {
                    currentRoot = currentRoot!!.createDirIfNotExists(book.authorDirName)
                }
                copyFileData(currentRoot, book, extension, file)
            } else if (!PreferencesHandler.createAuthorDir
                && PreferencesHandler.createSequenceDir
            ) {
                if (book.sequenceDirName.isNotEmpty()) {
                    if (PreferencesHandler.createDifferentDirs) {
                        currentRoot = currentRoot!!.createDirIfNotExists("Серии")
                    }
                    val subDirs = book.sequenceDirName.split("$|$")
                    subDirs.forEach {
                        val sequenceRoot = currentRoot!!.createDirIfNotExists(it)
                        copyFileData(sequenceRoot, book, extension, file)
                    }
                }
            } else if (
                PreferencesHandler.createAuthorDir &&
                PreferencesHandler.createSequenceDir
            ) {
// если есть серия
                if (book.sequenceDirName.isNotEmpty()) {
                    // если выбрано сохранение серий внутри папки автора- сохраню внутри
                    if (PreferencesHandler.sequencesInAuthorDir) {
                        if (PreferencesHandler.createDifferentDirs) {
                            currentRoot = currentRoot!!.createDirIfNotExists("Авторы")
                        }
                        if (book.authorDirName.isNotEmpty()) {
                            currentRoot =
                                currentRoot!!.createDirIfNotExists(book.authorDirName.trim())
                        }
                        val subDirs = book.sequenceDirName.split("$|$")
                        subDirs.forEach {
                            val sequenceRoot = currentRoot!!.createDirIfNotExists(it)
                            copyFileData(sequenceRoot, book, extension, file)
                        }
                    } else {
                        if (PreferencesHandler.createDifferentDirs) {
                            if (PreferencesHandler.createDifferentDirs) {
                                currentRoot = currentRoot!!.createDirIfNotExists("Серии")
                            }
                        }
                        val subDirs = book.sequenceDirName.split("$|$")
                        subDirs.forEach {
                            val sequenceRoot = currentRoot!!.createDirIfNotExists(it)
                            copyFileData(sequenceRoot, book, extension, file)
                        }
                    }
                } else {
                    if (PreferencesHandler.createDifferentDirs) {
                        if (PreferencesHandler.createDifferentDirs) {
                            currentRoot = currentRoot!!.createDirIfNotExists("Авторы")
                        }
                    }
                    if (book.authorDirName.isNotEmpty()) {
                        currentRoot =
                            currentRoot!!.createDirIfNotExists(book.authorDirName.trim())
                    }
                    copyFileData(currentRoot, book, extension, file)
                }
            } else {
                copyFileData(currentRoot, book, extension, file)
            }
        } else if (compatRoot != null) {
            Log.d("surprise", "RootDownloadDir: 141 save to compat root dir")
            var currentRoot = compatRoot
            if (book.reservedSequenceName.isNotEmpty()) {
                currentRoot = currentRoot!!.createDirIfNotExists(book.reservedSequenceName)
                copy(file, File(currentRoot, book.name))
            } else if (!PreferencesHandler.createAuthorDir && !PreferencesHandler.createSequenceDir) {
                copy(file, File(currentRoot, book.name))
            } else if (PreferencesHandler.createAuthorDir
                && !PreferencesHandler.createSequenceDir
            ) {
                //==== создаю только папку автора
                if (PreferencesHandler.createDifferentDirs) {
                    currentRoot = currentRoot!!.createDirIfNotExists("Авторы")
                }
                if (book.authorDirName.isNotEmpty()) {
                    currentRoot = currentRoot!!.createDirIfNotExists(book.authorDirName)
                }
                copy(file, File(currentRoot, book.name))
            } else if (!PreferencesHandler.createAuthorDir
                && PreferencesHandler.createSequenceDir
            ) {
                if (book.sequenceDirName.isNotEmpty()) {
                    if (PreferencesHandler.createDifferentDirs) {
                        currentRoot = currentRoot!!.createDirIfNotExists("Серии")
                    }
                    val subDirs = book.sequenceDirName.split("$|$")
                    subDirs.forEach {
                        val sequenceRoot = currentRoot!!.createDirIfNotExists(it)
                        copy(file, File(sequenceRoot, book.name))
                    }
                }
            } else if (
                PreferencesHandler.createAuthorDir &&
                PreferencesHandler.createSequenceDir
            ) {
// если есть серия
                if (book.sequenceDirName.isNotEmpty()) {
                    // если выбрано сохранение серий внутри папки автора- сохраню внутри
                    if (PreferencesHandler.sequencesInAuthorDir) {
                        if (PreferencesHandler.createDifferentDirs) {
                            currentRoot = currentRoot!!.createDirIfNotExists("Авторы")
                        }
                        if (book.authorDirName.isNotEmpty()) {
                            currentRoot =
                                currentRoot!!.createDirIfNotExists(book.authorDirName.trim())
                        }
                        val subDirs = book.sequenceDirName.split("$|$")
                        subDirs.forEach {
                            val sequenceRoot = currentRoot!!.createDirIfNotExists(it)
                            copy(file, File(sequenceRoot, book.name))
                        }
                    } else {
                        if (PreferencesHandler.createDifferentDirs) {
                            if (PreferencesHandler.createDifferentDirs) {
                                currentRoot = currentRoot!!.createDirIfNotExists("Серии")
                            }
                        }
                        val subDirs = book.sequenceDirName.split("$|$")
                        subDirs.forEach {
                            val sequenceRoot = currentRoot!!.createDirIfNotExists(it)
                            copy(file, File(sequenceRoot, book.name))
                        }
                    }
                } else {
                    if (PreferencesHandler.createDifferentDirs) {
                        if (PreferencesHandler.createDifferentDirs) {
                            currentRoot = currentRoot!!.createDirIfNotExists("Авторы")
                        }
                    }
                    if (book.authorDirName.isNotEmpty()) {
                        currentRoot =
                            currentRoot!!.createDirIfNotExists(book.authorDirName.trim())
                    }
                    copy(file, File(currentRoot, book.name))
                }
            }
        } else {
            Log.d("surprise", "RootDownloadDir: 227 save to external memory")
            Log.d("surprise", "RootDownloadDir: 228 extension is $extension")
            var currentRoot = App.instance.getExternalFilesDir("media")
            if (book.reservedSequenceName.isNotEmpty()) {
                currentRoot = currentRoot!!.createDirIfNotExists(book.reservedSequenceName)
                copy(file, File(currentRoot, book.name))
            } else if (!PreferencesHandler.createAuthorDir && !PreferencesHandler.createSequenceDir) {
                copy(file, File(currentRoot, book.name))
            } else if (PreferencesHandler.createAuthorDir
                && !PreferencesHandler.createSequenceDir
            ) {
                //==== создаю только папку автора
                if (PreferencesHandler.createDifferentDirs) {
                    currentRoot = currentRoot!!.createDirIfNotExists("Авторы")
                }
                if (book.authorDirName.isNotEmpty()) {
                    currentRoot = currentRoot!!.createDirIfNotExists(book.authorDirName)
                }
                copy(file, File(currentRoot, book.name))
            } else if (!PreferencesHandler.createAuthorDir
                && PreferencesHandler.createSequenceDir
            ) {
                if (book.sequenceDirName.isNotEmpty()) {
                    if (PreferencesHandler.createDifferentDirs) {
                        currentRoot = currentRoot!!.createDirIfNotExists("Серии")
                    }
                    val subDirs = book.sequenceDirName.split("$|$")
                    subDirs.forEach {
                        val sequenceRoot = currentRoot!!.createDirIfNotExists(it)
                        copy(file, File(sequenceRoot, book.name))
                    }
                }
            } else if (
                PreferencesHandler.createAuthorDir &&
                PreferencesHandler.createSequenceDir
            ) {
// если есть серия
                if (book.sequenceDirName.isNotEmpty()) {
                    // если выбрано сохранение серий внутри папки автора- сохраню внутри
                    if (PreferencesHandler.sequencesInAuthorDir) {
                        if (PreferencesHandler.createDifferentDirs) {
                            currentRoot = currentRoot!!.createDirIfNotExists("Авторы")
                        }
                        if (book.authorDirName.isNotEmpty()) {
                            currentRoot =
                                currentRoot!!.createDirIfNotExists(book.authorDirName.trim())
                        }
                        val subDirs = book.sequenceDirName.split("$|$")
                        subDirs.forEach {
                            val sequenceRoot = currentRoot!!.createDirIfNotExists(it)
                            copy(file, File(sequenceRoot, book.name))
                        }
                    } else {
                        if (PreferencesHandler.createDifferentDirs) {
                            if (PreferencesHandler.createDifferentDirs) {
                                currentRoot = currentRoot!!.createDirIfNotExists("Серии")
                            }
                        }
                        val subDirs = book.sequenceDirName.split("$|$")
                        subDirs.forEach {
                            val sequenceRoot = currentRoot!!.createDirIfNotExists(it)
                            copy(file, File(sequenceRoot, book.name))
                        }
                    }
                } else {
                    if (PreferencesHandler.createDifferentDirs) {
                        if (PreferencesHandler.createDifferentDirs) {
                            currentRoot = currentRoot!!.createDirIfNotExists("Авторы")
                        }
                    }
                    if (book.authorDirName.isNotEmpty()) {
                        currentRoot =
                            currentRoot!!.createDirIfNotExists(book.authorDirName.trim())
                    }
                    copy(file, File(currentRoot, book.name))
                }
            }
        }
    }

    private fun copyFileData(
        sequenceRoot: DocumentFile?,
        book: BooksDownloadSchedule,
        extension: String,
        file: File
    ) {
        Log.d("surprise", "RootDownloadDir: 311 $extension")
        val previousFileVersion =
            sequenceRoot!!.findFile(book.name)
        if (previousFileVersion != null && previousFileVersion.exists()) {
            previousFileVersion.delete()
        }
        destinationFile =
            sequenceRoot.createFile(
                extension,
                book.name
            )
        Log.d("surprise", "RootDownloadDir: 322 ${destinationFile?.name}")
        val uri = destinationFile!!.uri
        val stream = App.instance.contentResolver.openOutputStream(uri)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Files.copy(file.toPath(), stream)
        } else {
            file.inputStream().copyTo(stream!!)
        }
    }

    val path: String
        get() {
            if (root != null) {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    UrlHelper.getPath(App.instance, root!!.uri) ?: "not set"
                } else {
                    "old version"
                }
            }
            if (compatRoot != null) {
                return compatRoot?.absolutePath ?: "get error"
            }
            if (PreferencesHandler.storageAccessDenied) {
                return App.instance.getExternalFilesDir("media")?.absolutePath
                    ?: "error get external dir"
            }
            return "Not set"
        }
    var root: DocumentFile? = null
    var compatRoot: File? = null

    @Throws(IOException::class)
    fun copy(src: File?, dst: File?) {
        Log.d("surprise", "RootDownloadDir: 276 ${src?.absolutePath} copy to ${dst?.absolutePath}")
        val `is`: InputStream = FileInputStream(src)
        `is`.use { `in` ->
            val out: OutputStream = FileOutputStream(dst)
            out.use { o ->
                // Transfer bytes from in to out
                val buf = ByteArray(1024)
                var len: Int
                while (`in`.read(buf).also { len = it } > 0) {
                    o.write(buf, 0, len)
                }
            }
        }
        `is`.close()
        src?.delete()
        compatDestinationFile = dst
    }

    fun getDestinationFileType(): String? {
        return destinationFile?.type
    }

    fun getDestinationFileName(): String? {
        if (destinationFile != null) {
            return destinationFile?.name
        }
        if (compatDestinationFile != null) {
            return compatDestinationFile?.name
        } else {
            return App.instance.getString(R.string.no_name_title)
        }
    }

    fun getCompatFileRelativePath(): String {
        return compatDestinationFile!!.absolutePath.replace(compatRoot!!.absolutePath, "")
    }

    fun getFileRelativePath(): String {
        var path = destinationFile!!.name
        var parent = destinationFile!!.parentFile
        while (parent != root) {
            path = "${parent!!.name}/$path"
            parent = parent.parentFile
        }
        return path!!
    }

    fun getRelativePath(): String? {
        if(root != null){
            return getFileRelativePath()
        }
        if(compatRoot != null){
            return getCompatFileRelativePath()
        }
        return null
    }
}


fun DocumentFile.createDirIfNotExists(name: String): DocumentFile? {
    if (findFile(name) == null) {
        return createDirectory(name)
    }
    return findFile(name)
}

fun File.createDirIfNotExists(name: String): File {
    val newDir = File(absoluteFile, name)
    if (!newDir.isDirectory) {
        newDir.mkdirs()
        return newDir
    }
    return newDir
}