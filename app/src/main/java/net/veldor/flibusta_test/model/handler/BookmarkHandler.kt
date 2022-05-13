package net.veldor.flibusta_test.model.handler

import android.content.Context
import android.util.Log
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.file.MyFileReader
import net.veldor.flibusta_test.model.selections.BookmarkItem
import net.veldor.flibusta_test.model.selections.blacklist.BlacklistType
import org.w3c.dom.Element

class BookmarkHandler private constructor() {

    fun getBookmarkCategories(context: Context): List<BookmarkItem> {
        // read data from XML
        val results = ArrayList<BookmarkItem>()
        val rawData = MyFileReader.getOpdsBookmarks()
        val dom = BlacklistType.getDocument(rawData)
        val nodes = dom?.documentElement?.childNodes
        results.add(BookmarkItem(context.getString(R.string.no_category_title), null, null))
        Log.d("surprise", "BookmarkHandler.kt 18: nodes len is ${nodes?.length}")
        if (nodes != null && nodes.length > 0) {
            var counter = 0
            while (nodes.item(counter) != null) {
                val node = nodes.item(counter)
                if (node.hasAttributes()) {
                    // look for category
                    if (node.attributes.getNamedItem("type").textContent == TYPE_CATEGORY) {
                        results.add(
                            BookmarkItem(
                                node.attributes.getNamedItem("name").textContent,
                                TYPE_CATEGORY,
                                null
                            )
                        )
                    }
                }
                ++counter
            }
        }
        return results
    }

    fun addCategory(category: String) {
        val rawData = MyFileReader.getOpdsBookmarks()
        val dom = BlacklistType.getDocument(rawData)
        // check existence
        val nodes = dom?.documentElement?.childNodes
        var found = false
        if (nodes != null && nodes.length > 0) {
            var counter = 0
            while (nodes.item(counter) != null) {
                val node = nodes.item(counter)
                if (node.hasAttributes()) {
                    // look for category
                    if (node.attributes.getNamedItem("type").textContent == TYPE_CATEGORY && node.attributes.getNamedItem(
                            "name"
                        ).textContent == category
                    ) {
                        found = true
                        break
                    }
                }
                counter++
            }
        }
        if (!found) {
            val newNode = dom?.createElement("item")
            newNode?.setAttribute("type", TYPE_CATEGORY)
            newNode?.setAttribute("name", category)
            dom?.documentElement?.appendChild(newNode)
            // save
            val resultString = BlacklistType.getStringFromDocument(dom)
            MyFileReader.saveBookmarksList(resultString)
        }
    }

    fun addBookmark(category: String?, name: String, link: String) {
        val rawData = MyFileReader.getOpdsBookmarks()
        val dom = BlacklistType.getDocument(rawData)
        if (dom != null) {
            var targetNode: Element? = null
            if (category == null) {
                targetNode = dom.documentElement
            } else {
                // search for category
                val nodes = dom.documentElement?.childNodes
                if (nodes != null && nodes.length > 0) {
                    var counter = 0
                    while (nodes.item(counter) != null) {
                        val node = nodes.item(counter)
                        if (node.hasAttributes()) {
                            // look for category
                            if (node.attributes.getNamedItem("type").textContent == TYPE_CATEGORY && node.attributes.getNamedItem(
                                    "name"
                                ).textContent == category
                            ) {
                                targetNode = node as Element
                                break
                            }
                        }
                        counter++
                    }
                }
            }
            val newNode = dom.createElement("item")
            newNode?.setAttribute("type", TYPE_BOOKMARK)
            newNode?.setAttribute("name", name)
            newNode?.setAttribute("link", link)
            targetNode?.appendChild(newNode)
            val resultString = BlacklistType.getStringFromDocument(dom)
            MyFileReader.saveBookmarksList(resultString)
        }
    }

    fun bookmarkInList(bookmarkLink: String?): Boolean {
        if (bookmarkLink != null) {
            // check list for bookmark
            val rawData = MyFileReader.getOpdsBookmarks()
            if (rawData.contains("\"$bookmarkLink\"")) {
                return true
            }
        }
        return false
    }

    fun deleteBookmark(lastRequestedUrl: String?) {
        if (lastRequestedUrl != null) {
            val rawData = MyFileReader.getOpdsBookmarks()
            val dom = BlacklistType.getDocument(rawData)
            val items = dom?.getElementsByTagName("item")
            var counter = 0
            while (items?.item(counter) != null) {
                val node = items.item(counter)
                if (node.hasAttributes() && node.attributes.getNamedItem("link").textContent == lastRequestedUrl) {
                    node.parentNode.removeChild(node)
                    // save and exit
                    val resultString = BlacklistType.getStringFromDocument(dom)
                    MyFileReader.saveBookmarksList(resultString)
                    return
                }
                counter++
            }
        }
    }


    companion object {
        @kotlin.jvm.JvmStatic
        var instance: BookmarkHandler = BookmarkHandler()
            private set


        const val TYPE_CATEGORY = "category"
        const val TYPE_BOOKMARK = "bookmark"
    }
}