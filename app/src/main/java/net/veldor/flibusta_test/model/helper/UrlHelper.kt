package net.veldor.flibusta_test.model.helper

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import androidx.annotation.RequiresApi
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.db.entity.BooksDownloadSchedule
import net.veldor.flibusta_test.model.handler.FormatHandler
import net.veldor.flibusta_test.model.handler.GrammarHandler
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.selections.DownloadLink

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
        if (PreferencesHandler.instance.isCustomMirror) {
            val customMirror = PreferencesHandler.instance.customMirror
            if (customMirror.isNotEmpty()) {
                return customMirror
            }
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
        val rootDir = PreferencesHandler.instance.getDownloadDirLocation()
        val bookName = if (handleName) getBookName(link) else link.name
        // получу конечное расположение файла
        sb.append(PreferencesHandler.instance.getDownloadDirLocation())
        if (link.reservedSequenceName.isNotEmpty()) {
            return "$rootDir${link.reservedSequenceName}/$bookName"
        }
        if (!PreferencesHandler.instance.createSequenceDir &&
            !PreferencesHandler.instance.createAuthorDir
        ) {
            //==== папки не нужны, сохраняю в корень
            return "$rootDir$bookName"
        } else if (PreferencesHandler.instance.createAuthorDir
            && !PreferencesHandler.instance.createSequenceDir
        ) {
            //==== создаю только папку автора
            if (PreferencesHandler.instance.createDifferentDirs) {
                sb.append("Авторы/")
            }
            if (link.authorDirName != null) {
                sb.append("${link.authorDirName!!.trim()}/")
            }
            sb.append(bookName)
            return sb.toString()
        } else if (!PreferencesHandler.instance.createAuthorDir
            && PreferencesHandler.instance.createSequenceDir
        ) {
            //==== создаю только папку серии
            // придётся копировать файл в папку каждой серии по отдельности
            if (link.sequenceDirName != null) {
                if (PreferencesHandler.instance.createDifferentDirs) {
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
            PreferencesHandler.instance.createAuthorDir &&
            PreferencesHandler.instance.createSequenceDir
        ) {
            // если есть серия
            if (link.sequenceDirName != null) {
                // если выбрано сохранение серий внутри папки автора- сохраню внутри
                if (PreferencesHandler.instance.sequencesInAuthorDir) {
                    if (PreferencesHandler.instance.createDifferentDirs) {
                        if (PreferencesHandler.instance.createDifferentDirs) {
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
                    if (PreferencesHandler.instance.createDifferentDirs) {
                        if (PreferencesHandler.instance.createDifferentDirs) {
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
                if (PreferencesHandler.instance.createDifferentDirs) {
                    if (PreferencesHandler.instance.createDifferentDirs) {
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
        var bookName =
            link.name!!.replace("[^\\d\\w- ]".toRegex(), "")
        val bookMime = FormatHandler.getShortFromFullMime(link.mime!!)

        if (PreferencesHandler.instance.isAuthorInBookName) {
            if (link.author.isNullOrEmpty()) {
                bookName = "Без автора_$bookName"
            } else {
                if (bookName.length / 2 + link.author!!.length / 2 < 110) {
                    bookName = GrammarHandler.getAuthorLastName(link.author) + "_" + bookName
                }
            }
        }
        if (PreferencesHandler.instance.isSequenceInBookName) {
            if (!link.nameInSequence.isNullOrEmpty()) {
                if (bookName.length / 2 + link.nameInSequence!!.length / 2 < 110) {
                    bookName = bookName + "_" + link.nameInSequence
                }
            }
        }
        if (bookName.length / 2 > 220) {
            bookName = bookName.substring(0, 110) + "..."
        }
        bookName = "$bookName ${link.id}.$bookMime"
        return bookName
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
        return requestString.matches(Regex(".+/b/\\d+/.+"))
    }
}