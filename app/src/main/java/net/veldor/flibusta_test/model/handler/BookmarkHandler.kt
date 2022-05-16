package net.veldor.flibusta_test.model.handler

import android.content.Context
import android.util.Log
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.file.MyFileReader
import net.veldor.flibusta_test.model.selections.BookmarkItem
import net.veldor.flibusta_test.model.selections.blacklist.BlacklistType
import net.veldor.flibusta_test.model.utils.RandomString
import org.w3c.dom.Element
import org.w3c.dom.NodeList

class BookmarkHandler private constructor() {

    fun getBookmarkCategories(context: Context): List<BookmarkItem> {
        // read data from XML
        val results = ArrayList<BookmarkItem>()
        val rawData = MyFileReader.getOpdsBookmarks()
        val dom = BlacklistType.getDocument(rawData)
        val nodes = dom?.documentElement?.childNodes
        results.add(BookmarkItem("", context.getString(R.string.no_category_title), null, null))
        if (nodes != null && nodes.length > 0) {
            var counter = 0
            while (nodes.item(counter) != null) {
                val node = nodes.item(counter)
                if (node.hasAttributes()) {
                    // look for category
                    if (node.attributes.getNamedItem("type").textContent == TYPE_CATEGORY) {
                        results.add(
                            BookmarkItem(
                                node.attributes.getNamedItem("id").textContent,
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

    fun addCategory(category: String): BookmarkItem {
        val rawData = MyFileReader.getOpdsBookmarks()
        val dom = BlacklistType.getDocument(rawData)!!
        // check existence
        val nodes = dom.documentElement.childNodes
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
                        return BookmarkItem(
                            id = node.attributes.getNamedItem("id").textContent,
                            name = node.attributes.getNamedItem("name").textContent,
                            type = TYPE_CATEGORY,
                            link = null
                        )
                    }
                }
                counter++
            }
        }
        val newNode = dom.createElement("item")
        newNode?.setAttribute("id", RandomString().nextString())
        newNode?.setAttribute("type", TYPE_CATEGORY)
        newNode?.setAttribute("name", category)
        dom.documentElement.appendChild(newNode)
        // save
        val resultString = BlacklistType.getStringFromDocument(dom)
        MyFileReader.saveBookmarksList(resultString)
        return BookmarkItem(
            id = newNode.attributes.getNamedItem("id").textContent,
            name = newNode.attributes.getNamedItem("name").textContent,
            type = TYPE_CATEGORY,
            link = null
        )
    }

    fun addBookmark(category: BookmarkItem, name: String, link: String) {
        val rawData = MyFileReader.getOpdsBookmarks()
        val dom = BlacklistType.getDocument(rawData)!!
        val targetNode: Element? = if (category.id.isEmpty()) {
            dom.documentElement
        } else {
            dom.getElementById(category.id)
        }
        val newNode = dom.createElement("item")
        newNode?.setAttribute("type", TYPE_BOOKMARK)
        newNode?.setAttribute("id", RandomString().nextString())
        newNode?.setAttribute("name", name)
        newNode?.setAttribute("link", link)
        targetNode?.appendChild(newNode)
        val resultString = BlacklistType.getStringFromDocument(dom)
        MyFileReader.saveBookmarksList(resultString)
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
                if (node.hasAttributes() && node.attributes.getNamedItem("type").textContent == TYPE_BOOKMARK && node.attributes.getNamedItem(
                        "link"
                    ).textContent == lastRequestedUrl
                ) {
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

    fun get(category: String?): java.util.ArrayList<BookmarkItem> {
        Log.d("surprise", "BookmarkHandler.kt 148: search for $category")
        val results = ArrayList<BookmarkItem>()
        val rawData = MyFileReader.getOpdsBookmarks()
        val dom = BlacklistType.getDocument(rawData)
        val nodes: NodeList? = if (category == null) {
            dom?.documentElement?.childNodes
        } else {
            var result: NodeList? = null
            // find requested category
            val nodes = dom?.documentElement?.childNodes
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
                            result = node.childNodes
                            break
                        }
                    }
                    counter++
                }
            }
            result
        }
        if (nodes != null && nodes.length > 0) {
            var counter = 0
            while (nodes.item(counter) != null) {
                val node = nodes.item(counter)
                if (node.hasAttributes()) {
                    if (node.attributes.getNamedItem("type").textContent == TYPE_CATEGORY) {
                        results.add(
                            BookmarkItem(
                                id = node.attributes.getNamedItem("id").textContent,
                                name = node.attributes.getNamedItem("name").textContent,
                                type = TYPE_CATEGORY,
                                link = null
                            )
                        )
                    } else if (node.attributes.getNamedItem("type").textContent == TYPE_BOOKMARK) {
                        results.add(
                            BookmarkItem(
                                id = node.attributes.getNamedItem("id").textContent,
                                name = node.attributes.getNamedItem("name").textContent,
                                type = TYPE_BOOKMARK,
                                link = node.attributes.getNamedItem("link").textContent
                            )
                        )
                    }
                }
                counter++
            }
        }
        return results
    }

    fun deleteCategory(item: BookmarkItem) {
        val rawData = MyFileReader.getOpdsBookmarks()
        val dom = BlacklistType.getDocument(rawData)
        val element = dom?.getElementById(item.id)
        if (element != null) {
            element.parentNode.removeChild(element)
            // save and exit
            val resultString = BlacklistType.getStringFromDocument(dom)
            MyFileReader.saveBookmarksList(resultString)
        }
    }

    fun changeBookmark(category: BookmarkItem, bookmark: BookmarkItem) {
        val rawData = MyFileReader.getOpdsBookmarks()
        val dom = BlacklistType.getDocument(rawData)!!
        val bookmarkItem = dom.getElementById(bookmark.id)
        val categoryItem = if (category.id.isEmpty()) {
            dom.documentElement
        } else {
            dom.getElementById(category.id)
        }
        bookmarkItem.setAttribute("name", bookmark.name)
        bookmarkItem.setAttribute("link", bookmark.link)
        categoryItem.appendChild(bookmarkItem)
        val resultString = BlacklistType.getStringFromDocument(dom)
        MyFileReader.saveBookmarksList(resultString)
    }

    fun getCategoryPosition(context: Context, item: BookmarkItem): Int {
        val rawData = MyFileReader.getOpdsBookmarks()
        val dom = BlacklistType.getDocument(rawData)!!
        val element = dom.getElementById(item.id)
        if (element.parentNode.isSameNode(dom.documentElement)) {
            Log.d("surprise", "BookmarkHandler.kt 241: bookmark in root")
            return 0
        }
        val categoryName = element.parentNode.attributes.getNamedItem("name").textContent
        // get all categories
        val categories = getBookmarkCategories(context)
        var counter = 0
        categories.forEach {
            if (it.name == categoryName) {
                return counter
            }
            counter++
        }
        return 0
    }

    fun getBookmarkCategoryName(item: BookmarkItem): String? {
        val rawData = MyFileReader.getOpdsBookmarks()
        val dom = BlacklistType.getDocument(rawData)!!
        val element = dom.getElementById(item.id)
        val parent = element.parentNode
        if (parent.isSameNode(dom.documentElement)) {
            return null
        }
        return parent.attributes.getNamedItem("name").textContent
    }


    companion object {
        @kotlin.jvm.JvmStatic
        var instance: BookmarkHandler = BookmarkHandler()
            private set


        const val TYPE_CATEGORY = "category"
        const val TYPE_BOOKMARK = "bookmark"
    }
}