package net.veldor.flibusta_test.model.helper

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import androidx.annotation.RequiresApi
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.db.entity.BooksDownloadSchedule
import net.veldor.flibusta_test.model.handler.FormatHandler
import net.veldor.flibusta_test.model.handler.GrammarHandler
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.selection.DownloadLink
import net.veldor.flibusta_test.model.selection.FoundEntity

object UrlHelper {
    @kotlin.jvm.JvmStatic
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    fun getPath(context: Context?, uri: Uri): String? {
        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            println("getPath() uri: $uri")
            println("getPath() uri authority: " + uri.authority)
            println("getPath() uri path: " + uri.path)

            // ExternalStorageProvider
            if ("com.android.externalstorage.documents" == uri.authority) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                println("getPath() docId: " + docId + ", split: " + split.size + ", type: " + type)

                // This is for checking Main Memory
                return if ("primary".equals(type, ignoreCase = true)) {
                    if (split.size > 1) {
                        Environment.getExternalStorageDirectory().toString() + "/" + split[1] + "/"
                    } else {
                        Environment.getExternalStorageDirectory().toString() + "/"
                    }
                    // This is for checking SD Card
                } else {
                    "storage" + "/" + docId.replace(":", "/")
                }
            }
        }
        return null
    }

    @kotlin.jvm.JvmStatic
    fun getBaseUrl(): String {
        if (PreferencesHandler.isCustomMirror) {
            val customMirror = PreferencesHandler.customMirror
            if (customMirror.isNotEmpty()) {
                return customMirror.trim()
            }
        }
        if (PreferencesHandler.useTorMirror) {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
                return PreferencesHandler.BASE_TOR_URL
            }
            return PreferencesHandler.BASE_TOR_URL
        }
        if (PreferencesHandler.connectionType == PreferencesHandler.CONNECTION_MODE_TOR) {
            return PreferencesHandler.BASE_TOR_URL
        }
        return PreferencesHandler.BASE_URL
    }

    @kotlin.jvm.JvmStatic
    fun getSearchRequest(searchType: Int, request: String?): String {
        // базовый URL зависит от исползуемого соединения
        val urlConstructor = StringBuilder()
        urlConstructor.append("/opds/")
        when (searchType) {
            R.id.searchBook -> urlConstructor.append("search?searchType=books&searchTerm=")
                .append(request)
            R.id.searchAuthor -> urlConstructor.append("search?searchType=authors&searchTerm=")
                .append(request)
            R.id.searchSequence -> urlConstructor.append("sequences/")
                .append(request)
            R.id.searchGenre -> urlConstructor.append("genres/")
                .append(request)
        }
        return urlConstructor.toString()
    }

    fun getDownloadedBookPath(link: DownloadLink, handleName: Boolean): String {
        val sb = StringBuffer()
        val root = PreferencesHandler.rootDownloadDir
        val bookName = if (handleName) getBookName(link) else link.name
        var rootPath = root.path
        if (!rootPath.endsWith("/")) {
            rootPath += "/"
        }
        // получу конечное расположение файла
        if (link.reservedSequenceName.isNotEmpty()) {
            return "$rootPath${link.reservedSequenceName}/$bookName"
        }
        sb.append(rootPath)
        if (!PreferencesHandler.createSequenceDir &&
            !PreferencesHandler.createAuthorDir
        ) {
            //==== папки не нужны, сохраняю в корень
            return "$rootPath$bookName"
        } else if (PreferencesHandler.createAuthorDir
            && !PreferencesHandler.createSequenceDir
        ) {
            //==== создаю только папку автора
            if (PreferencesHandler.createDifferentDirs) {
                sb.append("Авторы/")
            }
            if (link.authorDirName != null) {
                sb.append("${link.authorDirName!!.trim()}/")
            }
            sb.append(bookName)
            return sb.toString()
        } else if (!PreferencesHandler.createAuthorDir
            && PreferencesHandler.createSequenceDir
        ) {
            //==== создаю только папку серии
            // придётся копировать файл в папку каждой серии по отдельности
            if (link.sequenceDirName != null) {
                if (PreferencesHandler.createDifferentDirs) {
                    sb.append("Серии/")
                }
                val subDirs = link.sequenceDirName!!.split("$|$")
                subDirs.forEach {
                    sb.append("${it.trim()}/")
                    // create dir if not exists and save file to it
                    sb.append(bookName)
                    return sb.toString()
                }
            } else {
                sb.append(bookName)
                return sb.toString()
            }
        } else if (
            PreferencesHandler.createAuthorDir &&
            PreferencesHandler.createSequenceDir
        ) {
            // если есть серия
            if (link.sequenceDirName != null && link.sequenceDirName!!.isNotEmpty()) {
                // если выбрано сохранение серий внутри папки автора- сохраню внутри
                if (PreferencesHandler.sequencesInAuthorDir) {
                    if (PreferencesHandler.createDifferentDirs) {
                        if (PreferencesHandler.createDifferentDirs) {
                            sb.append("Авторы/")
                        }
                    }
                    if (link.authorDirName != null) {
                        sb.append(link.authorDirName!!.trim())
                    }
                    val subDirs = link.sequenceDirName!!.split("$|$")
                    subDirs.forEach {
                        sb.append("/${it.trim()}/")
                        // create dir if not exists and save file to it
                        sb.append(bookName)
                        return sb.toString()
                    }
                } else {
                    if (PreferencesHandler.createDifferentDirs) {
                        if (PreferencesHandler.createDifferentDirs) {
                            sb.append("Серии/")
                        }
                    }
                    val subDirs = link.sequenceDirName!!.split("$|$")
                    subDirs.forEach {
                        sb.append("${it.trim()}/")
                        // create dir if not exists and save file to it
                        sb.append(bookName)
                        return sb.toString()
                    }
                }
            } else {
                if (PreferencesHandler.createDifferentDirs) {
                    if (PreferencesHandler.createDifferentDirs) {
                        sb.append("Авторы/")
                    }
                }
                if (link.authorDirName != null) {
                    sb.append("${link.authorDirName!!.trim()}/")
                }
                sb.append(bookName)
                return sb.toString()
            }
        }
        return "Что-то пошло не так, путь к файлу не определён"
    }

    fun getBookName(link: DownloadLink): String {
        if (link.editedName != null) {
            return link.editedName!!
        }
        var bookName =
            link.name!!
        if (PreferencesHandler.isAuthorInBookName) {
            if (link.author.isNullOrEmpty()) {
                bookName = "Без автора_$bookName"
            } else {
                if (bookName.length / 2 + link.author!!.length / 2 < 110) {
                    bookName = GrammarHandler.getAuthorLastName(link.author) + "_" + bookName
                }
            }
        }
        if (PreferencesHandler.isSequenceInBookName) {
            if (!link.nameInSequence.isNullOrEmpty()) {
                if (bookName.length / 2 + link.nameInSequence!!.length / 2 < 110) {
                    bookName = bookName + "_" + link.nameInSequence
                }
            }
        }
        if (bookName.length / 2 > 220) {
            bookName = bookName.substring(0, 110) + "..."
        }
        bookName = "${bookName.replace("[^\\d\\w- /\\\\]".toRegex(), "")} ${link.id}"
        return bookName
    }

    fun getBookNameWithoutExtension(book: FoundEntity): String {
        var bookName =
            book.name!!.replace("[^\\d\\w- ]".toRegex(), "")

        if (PreferencesHandler.isAuthorInBookName) {
            if (book.author.isNullOrEmpty()) {
                bookName = "Без автора_$bookName"
            } else {
                if (bookName.length / 2 + book.author!!.length / 2 < 110) {
                    bookName = GrammarHandler.getAuthorLastName(book.author) + "_" + bookName
                }
            }
        }
        if (PreferencesHandler.isSequenceInBookName) {
            if (!book.sequencesComplex.isEmpty()) {
                if (bookName.length / 2 + book.sequencesComplex.trim()
                        .replace("Серия: ", "").length / 2 < 110
                ) {
                    bookName = bookName + "_" + book.sequencesComplex.trim().replace("Серия: ", "")
                }
            }
        }
        if (bookName.length / 2 > 220) {
            bookName = bookName.substring(0, 110) + "..."
        }
        return "$bookName ${book.id}"
    }

    fun getDownloadedBookPath(link: BooksDownloadSchedule): String {
        val tempLink = DownloadLink()
        tempLink.name = link.name
        tempLink.author = link.author
        tempLink.reservedSequenceName = link.reservedSequenceName
        tempLink.authorDirName = link.authorDirName
        tempLink.mime = link.format
        tempLink.sequenceDirName = link.sequenceDirName
        return getDownloadedBookPath(tempLink, false)
    }

    fun isBookDownloadLink(requestString: String): Boolean {
        return requestString.matches(Regex("/b/\\d+/.+"))
    }
}