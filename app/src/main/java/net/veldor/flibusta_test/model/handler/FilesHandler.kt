package net.veldor.flibusta_test.model.handler

import android.util.Log
import androidx.documentfile.provider.DocumentFile
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_AUTHOR
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_AUTHORS
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_BOOK
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_GENRE
import net.veldor.flibusta_test.model.selection.filter.BlacklistType
import java.io.*

object FilesHandler {
    private const val SEARCH_VALUE_NAME = "string"
    private const val SEARCH_BOOK_AUTOCOMPLETE_FILE = "searchBookAutocomplete.xml"
    private const val SEARCH_AUTHOR_AUTOCOMPLETE_FILE = "searchAuthorAutocomplete.xml"
    private const val SEARCH_GENRE_AUTOCOMPLETE_FILE = "searchGenreAutocomplete.xml"
    private const val SEARCH_SEQUENCE_AUTOCOMPLETE_FILE = "searchSequenceAutocomplete.xml"
    private const val SEARCH_AUTOCOMPLETE_NEW =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><search> </search>"

    fun getSearchAutocomplete(type: String): ArrayList<String> {
        val searchValues = ArrayList<String>()
        val searchList = BlacklistType.getDocument(getSearchAutocompleteString(type))
        // найду значения строк
        if (searchList != null) {
            val values = searchList.getElementsByTagName(SEARCH_VALUE_NAME)
            var counter = 0
            while (values.item(counter) != null) {
                searchValues.add(values.item(counter).firstChild.nodeValue)
                ++counter
            }
        }
        return searchValues
    }

    private fun makeFile(file: File, content: String) {
        try {
            val writer = FileWriter(file)
            writer.append(content)
            writer.flush()
            writer.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getSearchAutocompleteString(type: String): String {
        val autocompleteFile = when (type) {
            TYPE_BOOK -> File(App.instance.filesDir, SEARCH_BOOK_AUTOCOMPLETE_FILE)
            TYPE_AUTHOR, TYPE_AUTHORS -> File(
                App.instance.filesDir,
                SEARCH_AUTHOR_AUTOCOMPLETE_FILE
            )
            TYPE_GENRE -> File(App.instance.filesDir, SEARCH_GENRE_AUTOCOMPLETE_FILE)
            else -> File(App.instance.filesDir, SEARCH_SEQUENCE_AUTOCOMPLETE_FILE)
        }

        if (autocompleteFile.length() == 0L) {
            autocompleteFile.delete()
        }

        if (!autocompleteFile.exists()) {
            makeFile(autocompleteFile, SEARCH_AUTOCOMPLETE_NEW)
        }
        val text = StringBuilder()
        try {
            val br = BufferedReader(FileReader(autocompleteFile))
            var line: String?
            while (br.readLine().also { line = it } != null) {
                text.append(line)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return text.toString()
    }

    fun saveSearchAutocomplete(value: String, type: String) {
        val autocompleteFile = when (type) {
            TYPE_BOOK -> File(App.instance.filesDir, SEARCH_BOOK_AUTOCOMPLETE_FILE)
            TYPE_AUTHOR, TYPE_AUTHORS -> File(
                App.instance.filesDir,
                SEARCH_AUTHOR_AUTOCOMPLETE_FILE
            )
            TYPE_GENRE -> File(App.instance.filesDir, SEARCH_GENRE_AUTOCOMPLETE_FILE)
            else -> File(App.instance.filesDir, SEARCH_SEQUENCE_AUTOCOMPLETE_FILE)
        }
        makeFile(autocompleteFile, value)
    }

    fun getBookRelativePath(file: DocumentFile): String {
        val root = PreferencesHandler.rootDownloadDir
        if (root.root != null) {
            var path = file.name
            var parent = file.parentFile
            while (parent != null && parent.uri != root.root?.uri) {
                Log.d("surprise", "FilesHandler: 97 parent file is ${parent.uri}")
                Log.d("surprise", "FilesHandler: 99 root file is ${root.root?.uri}")
                path = "${parent.name}/$path"
                parent = parent.parentFile
            }
            return path!!
        }
        return file.name!!
    }

    fun getBookRelativePath(file: File): String {
        val root = PreferencesHandler.rootDownloadDir
        if(root.compatRoot != null){
            return file.absolutePath.replace(root.compatRoot!!.absolutePath, "")
        }
        return file.name
    }
}