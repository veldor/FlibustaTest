package net.veldor.flibusta_test.model.handler

import android.util.Log
import org.w3c.dom.Document
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
                        Log.d("surprise", "putSearchValue: moved up autocomplete value $value on $type")
                        return true
                    }
                }
            }
        }
        return false
    }
}