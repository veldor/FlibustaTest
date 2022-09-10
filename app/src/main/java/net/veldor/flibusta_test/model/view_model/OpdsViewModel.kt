package net.veldor.flibusta_test.model.view_model

import android.text.format.Formatter
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.model.db.DatabaseInstance
import net.veldor.flibusta_test.model.db.entity.DownloadedBooks
import net.veldor.flibusta_test.model.db.entity.ReadedBooks
import net.veldor.flibusta_test.model.delegate.BookInfoAddedDelegate
import net.veldor.flibusta_test.model.delegate.FormatAvailabilityCheckDelegate
import net.veldor.flibusta_test.model.delegate.OpdsObserverDelegate
import net.veldor.flibusta_test.model.delegate.PictureLoadedDelegate
import net.veldor.flibusta_test.model.handler.*
import net.veldor.flibusta_test.model.helper.StringHelper
import net.veldor.flibusta_test.model.helper.UrlHelper
import net.veldor.flibusta_test.model.parser.NewOpdsParser
import net.veldor.flibusta_test.model.selections.BookmarkItem
import net.veldor.flibusta_test.model.selections.DownloadLink
import net.veldor.flibusta_test.model.selections.OpdsStatement
import net.veldor.flibusta_test.model.selections.RequestItem
import net.veldor.flibusta_test.model.selections.opds.FoundEntity
import net.veldor.flibusta_test.model.utils.CacheUtils
import net.veldor.flibusta_test.model.web.UniversalWebClient


open class OpdsViewModel : ViewModel() {
    private var checkWork: Job? = null
    private var checkBooksWork: Job? = null
    private var bookInfoDelegate: BookInfoAddedDelegate? = null
    private var currentWork: Job? = null
    private var formatDelegate: FormatAvailabilityCheckDelegate? = null

    fun request(
        request: RequestItem?
    ) {
        if (request == null) {
            return
        }
        OpdsStatement.instance.requestLaunched()
        if (request.addToHistory) {
            // add current condition to history
            OpdsStatement.instance.saveToHistory()
        } else {
            CacheUtils.requestClearCache()
        }
        OpdsStatement.instance.setCurrentRequest(request.request)
        CoverHandler.dropPreviousLoading()
        if (currentWork != null) {
            currentWork!!.cancel()
        }
        currentWork = viewModelScope.launch(Dispatchers.IO) {
            // получу результаты запроса
            getData(request)
            if (currentWork?.isCancelled != true) {
                OpdsStatement.instance.requestFinished()
            }
        }
    }

    private fun getData(request: RequestItem) {
        Log.d("surprise", "getData: request data")
        val response = UniversalWebClient().rawRequest(request.request, false)
        if (currentWork?.isCancelled == true) {
            OpdsStatement.instance.requestCancelled()
            return
        }
        if (response.inputStream != null) {
            val answerString = StringHelper.streamToString(response.inputStream)
            // check what answer string is opds
            if (!answerString.isNullOrEmpty() && answerString.startsWith("<?xml version=\"1.0\" encoding=\"utf-8\"?>")) {
                // сохраню строку в список результатов
                OpdsStatement.instance.addRawResults(answerString)
                // получу результаты запроса
                val parser = NewOpdsParser(answerString)
                parser.parse(currentWork)
            }else {
                // ошибка запроса
                OpdsStatement.instance.requestFailed()
            }
        } else {
            // ошибка запроса
            OpdsStatement.instance.requestFailed()
        }
    }

    fun loadInProgress(): Boolean {
        return currentWork?.isActive == true
    }

    fun cancelSearch() {
        currentWork?.cancel()
        OpdsStatement.instance.requestCancelled()
    }

    fun downloadPic(book: FoundEntity, delegate: PictureLoadedDelegate) {
        viewModelScope.launch(Dispatchers.IO) {
            CoverHandler().downloadFullPic(book)
            delegate.pictureLoaded()
        }
    }

    fun checkFormatAvailability(item: DownloadLink) {
        currentWork?.cancel()
        checkWork = viewModelScope.launch(Dispatchers.IO) {
            // get information about link
            val result = UniversalWebClient().rawRequest(item.url!!, false)
            if(isActive){
                if (result.statusCode == 200 && result.contentLength > 0) {
                    formatDelegate?.formatAvailable(
                        Formatter.formatFileSize(
                            App.instance,
                            result.contentLength.toLong()
                        )
                    )
                } else {
                    formatDelegate?.formatUnavailable()
                }
            }
        }
    }

    fun setFormatDelegate(delegate: FormatAvailabilityCheckDelegate) {
        formatDelegate = delegate
    }

    fun addToDownloadQueue(selectedLink: DownloadLink?) {
        if (selectedLink != null) {
            DownloadLinkHandler.addDownloadLink(selectedLink)
            if (PreferencesHandler.instance.downloadAutostart) {
                DownloadHandler.instance.startDownload()
            }
        }
    }

    fun markRead(item: FoundEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (item.id != null && DatabaseInstance.instance.mDatabase.readBooksDao()
                    .getBookById(item.id) == null
            ) {
                val newItem = ReadedBooks()
                newItem.bookId = item.id!!
                DatabaseInstance.instance.mDatabase.readBooksDao().insert(newItem)
            }
        }
    }

    fun markDownloaded(item: FoundEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (item.id != null && DatabaseInstance.instance.mDatabase.downloadedBooksDao()
                    .getBookById(item.id) == null
            ) {
                val newItem = DownloadedBooks()
                newItem.bookId = item.id!!
                DatabaseInstance.instance.mDatabase.downloadedBooksDao().insert(newItem)
            }
        }
    }

    fun markUnread(item: FoundEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseInstance.instance.mDatabase.readBooksDao()
                .delete(DatabaseInstance.instance.mDatabase.readBooksDao().getBookById(item.id))
        }
    }

    fun markNoDownloaded(item: FoundEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseInstance.instance.mDatabase.downloadedBooksDao()
                .delete(
                    DatabaseInstance.instance.mDatabase.downloadedBooksDao().getBookById(item.id)
                )
        }
    }

    fun getAutocomplete(type: String): ArrayList<String> {
        return FilesHandler.getSearchAutocomplete(type)
    }

    fun checkItemsFilled(booksList: List<FoundEntity>?) {
        checkBooksWork?.cancel()
        checkBooksWork = viewModelScope.launch(Dispatchers.IO) {
            bookInfoDelegate?.checkProgress(0, 1, booksList?.size)
            var counter = 1
            var linksCounter = 1

            var info: DownloadLink?
            booksList?.forEach { book ->
                if (book.name == null) {
                    book.downloadLinks.forEach { link ->
                        if (!this.isActive || checkBooksWork?.isCancelled == true) {
                            return@launch
                        }
                        if (link.url != null) {
                            info =
                                DownloadLinkHandler.createDownloadLinkFromHref(UrlHelper.getBaseUrl() + link.url!!)
                            if (info != null) {
                                link.author = info!!.author
                                link.name = info!!.name
                                link.authorDirName = info!!.authorDirName
                                link.sequenceDirName = info!!.sequenceDirName
                                link.size = info!!.size
                                link.mime = info!!.mime
                                link.id = info!!.id

                                if (book.name == null) {
                                    book.name = link.name
                                    book.author = link.author
                                    book.sequencesComplex = link.sequenceDirName?: ""
                                }
                            }
                        }
                        linksCounter++
                        bookInfoDelegate?.checkProgress(linksCounter, counter, booksList.size)
                    }
                    bookInfoDelegate?.infoAdded(book)
                }
                counter++
                bookInfoDelegate?.checkProgress(linksCounter, counter, booksList.size)
            }
            counter++
            bookInfoDelegate?.checkProgress(linksCounter, counter, booksList?.size)
        }
    }

    fun setBookInfoAddedDelegate(delegate: BookInfoAddedDelegate) {
        bookInfoDelegate = delegate
    }

    fun removeBookInfoAddedDelegate() {
        bookInfoDelegate = null
        checkBooksWork?.cancel()
    }

    fun cancelBookInfoLoad() {
        checkBooksWork?.cancel()
    }

    fun addBookmark(category: BookmarkItem, name: String, link: String) {
        BookmarkHandler.instance.addBookmark(category, name, link)
    }

    fun readyToCreateBookmark(): Boolean {
        return OpdsStatement.instance.getCurrentRequest() != null
    }

    fun getBookmarkLink(): String? {
        return OpdsStatement.instance.getCurrentRequest()
    }

    fun removeBookmark() {
        BookmarkHandler.instance.deleteBookmark(OpdsStatement.instance.getCurrentRequest())
    }

    fun addBlacklistItem(item: FoundEntity, target: String) {
        viewModelScope.launch(Dispatchers.IO) {
            FilterHandler.addToBlacklist(item, target)
        }
    }

    fun addSubscribeItem(item: FoundEntity, target: String) {
        viewModelScope.launch(Dispatchers.IO) {
            SubscribesHandler.addSubscribe(item, target)
        }
    }

    fun drawBadges(delegate: OpdsObserverDelegate) {
        viewModelScope.launch(Dispatchers.IO) {
        Thread.sleep(500)
            delegate.drawBadges()
    }

    }
}