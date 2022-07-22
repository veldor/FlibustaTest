package net.veldor.flibusta_test.model.selections.subscribe

import net.veldor.flibusta_test.model.file.MyFileReader
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerConfigurationException
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

abstract class SubscribeType {
    open val subscribeName = "empty"
    open var subscribeFileName = "empty"

    private var mDom: Document? = null
    private var mSubscribeValues: ArrayList<SubscribeItem> = arrayListOf()
    private val mExistentValues = ArrayList<String>()

    fun refreshSubscribeList() {
        // получу данны файла подписок
        val rawData = MyFileReader.getSubscribeList(subscribeFileName)
        mDom = getDocument(rawData)
        mSubscribeValues = ArrayList()
        if (mDom != null) {
            val values = mDom!!.getElementsByTagName(subscribeName)
            var counter = 0
            var subscribeItem: SubscribeItem
            while (values.item(counter) != null) {
                subscribeItem = SubscribeItem(
                    values.item(counter).firstChild.nodeValue,
                    subscribeName
                )
                mExistentValues.add(subscribeItem.name)
                mSubscribeValues.add(subscribeItem)
                ++counter
            }
        }
    }

    fun getSubscribeList(): ArrayList<SubscribeItem> {
        if (mSubscribeValues.isEmpty()) {
            refreshSubscribeList()
        }
        return mSubscribeValues
    }

    fun addValue(value: String): SubscribeItem? {
        if (!mExistentValues.contains(value.lowercase())) {
            val elem = mDom!!.createElement(subscribeName)
            val text = mDom!!.createTextNode(value.lowercase())
            elem.appendChild(text)
            mDom!!.documentElement.insertBefore(elem, mDom!!.documentElement.firstChild)
            MyFileReader.saveSubscribeList(subscribeFileName, getStringFromDocument(mDom))
            refreshSubscribeList()
            return SubscribeItem(value, subscribeName)
        }
        return null
    }

    fun deleteValue(name: String) {
        val books = mDom!!.getElementsByTagName(subscribeName)
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
            MyFileReader.saveSubscribeList(subscribeFileName, getStringFromDocument(mDom))
        }
        refreshSubscribeList()
    }

    fun changeValue(oldValue: String, newValue: String) {
        val lowerNewValue = newValue.lowercase()
        val books = mDom!!.getElementsByTagName(subscribeName)
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
                            MyFileReader.saveSubscribeList(
                                subscribeFileName,
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
            MyFileReader.saveSubscribeList(subscribeFileName, getStringFromDocument(mDom))
        }
        refreshSubscribeList()
    }

    fun getSubscribeList(which: Int): ArrayList<SubscribeItem> {
        val unsorted = getSubscribeList()
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

        fun delete(item: SubscribeItem) {
            when (item.type) {
                "book" -> SubscribeBooks.instance.deleteValue(item.name)
                "author" -> SubscribeAuthors.instance.deleteValue(item.name)
                "sequence" -> SubscribeSequences.instance.deleteValue(item.name)
                "genre" -> SubscribeGenre.instance.deleteValue(item.name)
            }
        }

        fun change(item: SubscribeItem, newValue: String) {
            when (item.type) {
                "book" -> SubscribeBooks.instance.changeValue(item.name, newValue)
                "author" -> SubscribeAuthors.instance.changeValue(item.name, newValue)
                "sequence" -> SubscribeSequences.instance.changeValue(item.name, newValue)
                "genre" -> SubscribeGenre.instance.changeValue(item.name, newValue)
            }
            item.name = newValue
        }
    }

}