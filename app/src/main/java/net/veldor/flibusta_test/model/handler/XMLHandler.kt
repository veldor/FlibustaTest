package net.veldor.flibusta_test.model.handler

import android.util.Log
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.db.DatabaseInstance
import net.veldor.flibusta_test.model.db.entity.*
import net.veldor.flibusta_test.model.helper.MimeHelper
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_BOOK
import net.veldor.flibusta_test.model.selection.BookmarkItem
import net.veldor.flibusta_test.model.selection.DownloadLink
import net.veldor.flibusta_test.model.selection.FoundEntity
import org.jsoup.Jsoup
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.IOException
import java.io.InputStream
import java.io.StringReader
import java.io.StringWriter
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerConfigurationException
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory

object XMLHandler {
    private const val SEARCH_VALUE_NAME = "string"
    fun getSearchAutocomplete(autocompleteText: String?): ArrayList<String> {
        val searchValues = ArrayList<String>()
        val searchList = getDocument(autocompleteText)
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

    private fun getSearchAutocomplete(searchList: Document?): ArrayList<String> {
        val searchValues = ArrayList<String>()
        // найду значения строк
        val values = searchList!!.getElementsByTagName(SEARCH_VALUE_NAME)
        var counter = 0
        while (values.item(counter) != null) {
            searchValues.add(values.item(counter).firstChild.nodeValue)
            ++counter
        }
        return searchValues
    }

    private fun getDocument(rawText: String?): Document? {
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

    private fun getStringFromDocument(doc: Document?): String {
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

    @kotlin.jvm.JvmStatic
    fun putSearchValue(value: String, type: String): Boolean {
        // получу содержимое файла
        val rawXml = FilesHandler.getSearchAutocompleteString(type)
        val dom = getDocument(rawXml)!!
        val values = getSearchAutocomplete(dom)
        if (!values.contains(value)) {
            val elem = dom.createElement(SEARCH_VALUE_NAME)
            val text = dom.createTextNode(value)
            elem.appendChild(text)
            dom.documentElement.insertBefore(elem, dom.documentElement.firstChild)
            FilesHandler.saveSearchAutocomplete(getStringFromDocument(dom), type)
            Log.d("surprise", "putSearchValue: appended autocomplete value $value on $type")
            return true
        } else {
            // перенесу значение на верх списка
            val existentValues = dom.getElementsByTagName(SEARCH_VALUE_NAME)
            if (existentValues != null && existentValues.length > 0) {
                var count = values.size
                while (count > 1) {
                    --count
                    val node = existentValues.item(count)
                    if (node.textContent == value) {
                        dom.documentElement.insertBefore(node, dom.documentElement.firstChild)
                        FilesHandler.saveSearchAutocomplete(getStringFromDocument(dom), type)
                        Log.d(
                            "surprise",
                            "putSearchValue: moved up autocomplete value $value on $type"
                        )
                        return true
                    }
                }
            }
        }
        return false
    }

    fun searchDownloadLinks(textStream: InputStream): HashMap<String, FoundEntity> {
        val booksList = HashMap<String, FoundEntity>()
        val dom: org.jsoup.nodes.Document
        val url = "http://flibusta.is"
        dom = Jsoup.parse(textStream, "UTF-8", url)
        val links = dom.select("a")
        var href: String?
        var bookId: String? = null
        var downloadLink: DownloadLink
        val downloadLinkPattern = Regex("/b/\\d+/.+")
        val bookNamePattern = Regex("/b/\\d+")
        links.forEach { link ->
            href = link.attr("href")
            if (href != null) {
                bookId = href!!.replace("fb2", "").filter { it.isDigit() }
                if (href!!.matches(bookNamePattern)) {
                    // create book entity if not exists
                    if (!booksList.containsKey(bookId)) {
                        booksList[bookId!!] = FoundEntity()
                        booksList[bookId]?.id = bookId
                        booksList[bookId]?.type = TYPE_BOOK
                        booksList[bookId]?.name = link.text()
                    }
                }
                else if (href!!.matches(downloadLinkPattern) && !href!!.endsWith("read")) {
                    // found link
                    if(booksList[bookId] == null){
                        val bookName = dom.select("div#main h1.title")
                        booksList[bookId!!] = FoundEntity()
                        booksList[bookId]?.id = bookId
                        booksList[bookId]?.type = TYPE_BOOK
                        booksList[bookId]?.name = bookName[0].text()
                    }
                    downloadLink = DownloadLink()
                    downloadLink.url = href
                    downloadLink.id = bookId
                    downloadLink.name = booksList[bookId]?.name
                    downloadLink.mime = MimeHelper.getMimeFromLink(href!!)
                    booksList[bookId]?.downloadLinks?.add(downloadLink)
                }
            }
        }
        //now, remove all items without download links
       val iterator = booksList.iterator()
        iterator.forEach {
            if(it.value.downloadLinks.isEmpty()){
                iterator.remove()
            }
        }
        return booksList
    }

    @kotlin.jvm.JvmStatic
    fun handleBackup(zin: ZipInputStream) {
        try {
            val s = StringBuilder()
            val buffer = ByteArray(1024)
            var read: Int
            while (zin.read(buffer, 0, 1024).also { read = it } >= 0) {
                s.append(String(buffer, 0, read))
            }
            val xml = s.toString()
            val document = getDocument(xml)
            val factory = XPathFactory.newInstance()
            val xPath = factory.newXPath()
            var entries =
                xPath.evaluate("/readed_books/book", document, XPathConstants.NODESET) as NodeList
            if (entries.length > 0) {
                var counter = 0
                val entriesLen = entries.length
                var node: Node
                while (counter < entriesLen) {
                    node = entries.item(counter)
                    // получу идентификатор книги
                    val id = node.attributes.getNamedItem("id").textContent
                    val rb = ReadedBooks()
                    rb.bookId = id
                    if (DatabaseInstance.mDatabase.readBooksDao()
                            .getBookById(id) == null
                    ) {
                        DatabaseInstance.mDatabase.readBooksDao().insert(rb)
                    }
                    ++counter
                }
                return
            }
            entries = xPath.evaluate(
                "/downloaded_books/book",
                document,
                XPathConstants.NODESET
            ) as NodeList
            Log.d("surprise", "handleBackup: downloaded books size is ${entries.length}")
            if (entries.length > 0) {
                var counter = 0
                val entriesLen = entries.length
                var node: Node
                while (counter < entriesLen) {
                    node = entries.item(counter)
                    // получу идентификатор книги
                    val rb = DownloadedBooks()
                    rb.bookId = node.attributes.getNamedItem("id").textContent
                    rb.destination = node.attributes.getNamedItem("destination").textContent
                    DatabaseInstance.mDatabase.downloadedBooksDao().insert(rb)
                    ++counter
                }
                return
            }

            entries = xPath.evaluate(
                "/download_schedule/link",
                document,
                XPathConstants.NODESET
            ) as NodeList
            if (entries.length > 0) {
                var counter = 0
                val entriesLen = entries.length
                var node: Node
                while (counter < entriesLen) {
                    node = entries.item(counter)
                    val scheduleElement = BooksDownloadSchedule()
                    val bookId = node.attributes.getNamedItem("bookId").textContent
                    val link = node.attributes.getNamedItem("link").textContent
                    val name = node.attributes.getNamedItem("name").textContent
                    val size = node.attributes.getNamedItem("size").textContent
                    val author = node.attributes.getNamedItem("author").textContent
                    val format = node.attributes.getNamedItem("format").textContent
                    val authorDirName = node.attributes.getNamedItem("authorDirName").textContent
                    val sequenceDirName =
                        node.attributes.getNamedItem("sequenceDirName").textContent
                    val reservedSequenceName =
                        node.attributes.getNamedItem("reserveSequenceName").textContent
                    scheduleElement.bookId = bookId
                    scheduleElement.link = link
                    scheduleElement.name = name
                    scheduleElement.size = size
                    scheduleElement.author = author
                    scheduleElement.format = format
                    scheduleElement.authorDirName = authorDirName
                    scheduleElement.sequenceDirName = sequenceDirName
                    scheduleElement.reservedSequenceName = reservedSequenceName
                    DatabaseInstance.mDatabase.booksDownloadScheduleDao()
                        .insert(scheduleElement)
                    ++counter
                }
                return
            }
            entries = xPath.evaluate(
                "/download_schedule_errors/link",
                document,
                XPathConstants.NODESET
            ) as NodeList
            if (entries.length > 0) {
                var counter = 0
                val entriesLen = entries.length
                var node: Node
                while (counter < entriesLen) {
                    node = entries.item(counter)
                    val scheduleElement = DownloadError()
                    val bookId = node.attributes.getNamedItem("id").textContent
                    val link = node.attributes.getNamedItem("link").textContent
                    val name = node.attributes.getNamedItem("name").textContent
                    val size = node.attributes.getNamedItem("size").textContent
                    val author = node.attributes.getNamedItem("author").textContent
                    val format = node.attributes.getNamedItem("format").textContent
                    val error = node.attributes.getNamedItem("error").textContent
                    val authorDirName = node.attributes.getNamedItem("authorDirName").textContent
                    val sequenceDirName =
                        node.attributes.getNamedItem("sequenceDirName").textContent
                    val reservedSequenceName =
                        node.attributes.getNamedItem("reserveSequenceName").textContent
                    scheduleElement.bookId = bookId
                    scheduleElement.link = link
                    scheduleElement.name = name
                    scheduleElement.size = size
                    scheduleElement.author = author
                    scheduleElement.format = format
                    scheduleElement.error = error
                    scheduleElement.authorDirName = authorDirName
                    scheduleElement.sequenceDirName = sequenceDirName
                    scheduleElement.reservedSequenceName = reservedSequenceName
                    DatabaseInstance.mDatabase.downloadErrorDao().insert(scheduleElement)
                    ++counter
                }
                return
            }


            entries =
                xPath.evaluate("/bookmarks/bookmark", document, XPathConstants.NODESET) as NodeList
            if (entries.length > 0) {
                var counter = 0
                val entriesLen = entries.length
                var node: Node
                while (counter < entriesLen) {
                    node = entries.item(counter)
                    // получу идентификатор книги
                    val name = node.attributes.getNamedItem("name").textContent
                    val link = node.attributes.getNamedItem("link").textContent
                    BookmarkHandler.addBookmark(
                        BookmarkItem(
                            "",
                            App.instance.getString(R.string.no_category_title),
                            null,
                            null
                        ), name, link
                    )
                    ++counter
                }
            }
        } catch (e: IOException) {
            Log.d("surprise", "handleBackup: error")
            e.printStackTrace()
        } catch (e: XPathExpressionException) {
            Log.d("surprise", "handleBackup: error1")
            e.printStackTrace()
        }
    }
}