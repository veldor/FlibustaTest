package net.veldor.flibusta_test.model.view_model

import androidx.lifecycle.ViewModel
import net.veldor.flibusta_test.model.handler.BookmarkHandler
import net.veldor.flibusta_test.model.selection.BookmarkItem

class BookmarksViewModel : ViewModel() {

    fun deleteCategory(item: BookmarkItem) {
        BookmarkHandler.deleteCategory(item)
    }

    fun deleteBookmark(item: BookmarkItem) {
        BookmarkHandler.deleteBookmark(item.link)
    }

    fun changeBookmark(category: BookmarkItem, bookmark: BookmarkItem) {
        BookmarkHandler.changeBookmark(category, bookmark)
    }
}