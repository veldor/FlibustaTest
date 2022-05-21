package net.veldor.flibusta_test.model.selections.blacklist

import android.util.Log
import androidx.lifecycle.MutableLiveData
import net.veldor.flibusta_test.model.file.MyFileReader
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import java.util.*
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerConfigurationException
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

abstract class BlacklistType {
    open val blacklistName = "empty"
    open var blacklistFileName = "empty"

    @kotlin.jvm.JvmField
    val liveBlacklistAdd = MutableLiveData<BlacklistItem>()

    @kotlin.jvm.JvmField
    val liveBlacklistRemove = MutableLiveData<BlacklistItem>()

    private var mDom: Document? = null
    private var mBlacklistValues: ArrayList<BlacklistItem> = arrayListOf()
    private val mExistentValues = ArrayList<String>()

    fun refreshBlacklist() {
        // получу данны файла подписок
        val rawData = MyFileReader.getBlacklist(blacklistFileName)
        mDom = getDocument(rawData)
        mBlacklistValues = ArrayList()
        if (mDom != null) {
            val values = mDom!!.getElementsByTagName(blacklistName)
            var counter = 0
            var blacklistItem: BlacklistItem
            while (values.item(counter) != null) {
                blacklistItem = BlacklistItem(
                    values.item(counter).firstChild.nodeValue,
                    blacklistName
                )
                mExistentValues.add(blacklistItem.name)
                mBlacklistValues.add(blacklistItem)
                ++counter
            }
        }
    }

    fun getBlacklist(): ArrayList<BlacklistItem> {
        if (mBlacklistValues.isEmpty()) {
            refreshBlacklist()
        }
        return mBlacklistValues
    }

    fun addValue(value: String): BlacklistItem? {
        if (!mExistentValues.contains(value.lowercase())) {
            val elem = mDom!!.createElement(blacklistName)
            val text = mDom!!.createTextNode(value.lowercase())
            elem.appendChild(text)
            mDom!!.documentElement.insertBefore(elem, mDom!!.documentElement.firstChild)
            MyFileReader.saveBlacklist(blacklistFileName, getStringFromDocument(mDom))
            refreshBlacklist()
            return BlacklistItem(blacklistName, value)
        }
        return null
    }

    fun deleteValue(name: String) {
        val books = mDom!!.getElementsByTagName(blacklistName)
        var book: Node
        val length = books.length
        var counter = 0
        if (length > 0) {
            while (counter < length) {
                book = books.item(counter)
                if (name == book.textContent) {
                    book.parentNode.removeChild(book)
                    break
                }
                counter++
            }
            MyFileReader.saveBlacklist(blacklistFileName, getStringFromDocument(mDom))
        }
        refreshBlacklist()
    }

    fun changeValue(oldValue: String, newValue: String) {
        val lowerNewValue = newValue.lowercase()
        val books = mDom!!.getElementsByTagName(blacklistName)
        var book: Node
        var innerBook: Node
        val length = books.length
        var counter = 0
        if (length > 0) {
            while (counter < length) {
                book = books.item(counter)
                if (oldValue == book.textContent) {
                    // if new value exists- delete old
                    var innerCounter = 0
                    while (innerCounter < length) {
                        innerBook = books.item(innerCounter)
                        if (lowerNewValue == innerBook.textContent) {
                            book.parentNode.removeChild(book)
                            MyFileReader.saveBlacklist(
                                blacklistFileName,
                                getStringFromDocument(mDom)
                            )
                            return
                        }
                        innerCounter++
                    }
                    book.textContent = lowerNewValue
                    break
                }
                counter++
            }
            MyFileReader.saveBlacklist(blacklistFileName, getStringFromDocument(mDom))
        }
        refreshBlacklist()
    }

    fun getBlacklist(which: Int): ArrayList<BlacklistItem> {
        val unsorted = getBlacklist()
        when (which) {
            1 -> {
                unsorted.reverse()
            }
            2 -> {
                unsorted.sortBy { it.name }
            }
            3 -> {
                unsorted.sortBy { it.name }
                unsorted.reverse()
            }
        }
        return unsorted
    }

    companion object {

        fun getDocument(rawText: String?): Document? {
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder: DocumentBuilder
            try {
                dBuilder = dbFactory.newDocumentBuilder()
                val `is` = InputSource(StringReader(rawText))
                return dBuilder.parse(`is`)
            } catch (e: ParserConfigurationException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: SAXException) {
                e.printStackTrace()
            }
            return null
        }

        fun getStringFromDocument(doc: Document?): String {
            val domSource = DOMSource(doc)
            val writer = StringWriter()
            val result = StreamResult(writer)
            val tf = TransformerFactory.newInstance()
            val transformer: Transformer
            try {
                transformer = tf.newTransformer()
                transformer.transform(domSource, result)
            } catch (e: TransformerConfigurationException) {
                e.printStackTrace()
            } catch (e: TransformerException) {
                e.printStackTrace()
            }
            return writer.toString()
        }

        fun delete(item: BlacklistItem) {
            when (item.type) {
                "book" -> BlacklistBooks.instance.deleteValue(item.name)
                "author" -> BlacklistAuthors.instance.deleteValue(item.name)
                "sequence" -> BlacklistSequences.instance.deleteValue(item.name)
                "genre" -> BlacklistGenre.instance.deleteValue(item.name)
                "format" -> BlacklistFormat.instance.deleteValue(item.name)
            }
        }

        fun change(item: BlacklistItem, newValue: String) {
            when (item.type) {
                "book" -> BlacklistBooks.instance.changeValue(item.name, newValue)
                "author" -> BlacklistAuthors.instance.changeValue(item.name, newValue)
                "sequence" -> BlacklistSequences.instance.changeValue(item.name, newValue)
                "genre" -> BlacklistGenre.instance.changeValue(item.name, newValue)
                "format" -> BlacklistFormat.instance.changeValue(item.name, newValue)
            }
            item.name = newValue
        }
    }

}