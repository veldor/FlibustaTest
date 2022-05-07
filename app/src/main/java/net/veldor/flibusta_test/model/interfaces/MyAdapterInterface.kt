package net.veldor.flibusta_test.model.interfaces

import net.veldor.flibusta_test.model.db.entity.DownloadedBooks
import net.veldor.flibusta_test.model.selections.opds.FoundEntity

interface MyAdapterInterface {
    fun clearList()
    fun appendContent(results: ArrayList<FoundEntity>)
    fun setHasNext(isNext: Boolean)
    fun getResultsSize(): Int
    fun setNextPageLink(link: String?)
    fun setLoadInProgress(state: Boolean)
    fun sort()
    fun getClickedItemId(): Long
    fun notEmpty(): Boolean
    fun getList(): ArrayList<FoundEntity>
    fun containsBooks(): Boolean
    fun containsAuthors(): Boolean
    fun containsGenres(): Boolean
    fun containsSequences(): Boolean
    fun getItemPositionById(clickedItemId: Long):Int
    fun markClickedElement(clickedElementIndex: Long)
    fun markBookRead(item: FoundEntity)
    fun markBookUnread(item: FoundEntity)
    fun markAsDownloaded(item: DownloadedBooks?)
    fun itemFiltered(item: FoundEntity)
}