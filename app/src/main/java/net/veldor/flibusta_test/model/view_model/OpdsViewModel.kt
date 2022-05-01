package net.veldor.flibusta_test.model.view_model

import android.text.format.Formatter
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.model.db.DatabaseInstance
import net.veldor.flibusta_test.model.db.entity.ReadedBooks
import net.veldor.flibusta_test.model.delegate.FormatAvailabilityCheckDelegate
import net.veldor.flibusta_test.model.delegate.PictureLoadedDelegate
import net.veldor.flibusta_test.model.delegate.SearchResultActionDelegate
import net.veldor.flibusta_test.model.handler.*
import net.veldor.flibusta_test.model.helper.StringHelper
import net.veldor.flibusta_test.model.parser.OpdsParser
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_AUTHOR
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_AUTHORS
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_BOOK
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_GENRE
import net.veldor.flibusta_test.model.selections.DownloadLink
import net.veldor.flibusta_test.model.selections.HistoryItem
import net.veldor.flibusta_test.model.selections.RequestItem
import net.veldor.flibusta_test.model.selections.opds.FoundEntity
import net.veldor.flibusta_test.model.selections.opds.SearchResult
import net.veldor.flibusta_test.model.web.UniversalWebClient


class OpdsViewModel : ViewModel() {
    private var lastScrolled: Int = -1
    var searchResultsDelegate: SearchResultActionDelegate? = null
    private var currentWork: Job? = null
    private val _liveRequestState: MutableLiveData<String> = MutableLiveData(STATUS_WAIT)
    val liveRequestState: LiveData<String> = _liveRequestState
    private val _requestResult: ArrayList<SearchResult> = arrayListOf()
    private var formatDelegate: FormatAvailabilityCheckDelegate? = null

    fun request(
        request: RequestItem?
    ) {
        if (request == null) {
            return
        }
        var appendResult = request.append
        CoverHandler.dropPreviousLoading()
        if (currentWork != null) {
            currentWork!!.cancel()
            _liveRequestState.postValue(STATUS_CANCELLED)
        }
        currentWork = viewModelScope.launch(Dispatchers.IO) {
            if (request.addToHistory && _requestResult.isNotEmpty()) {
                // make a copy of result
                val requestResult = arrayListOf<SearchResult>()
                requestResult += _requestResult
                requestResult.forEach {
                    it.clickedElementIndex = request.clickedElementIndex
                }
                HistoryHandler.instance.addToHistory(HistoryItem(requestResult))
            }
            if (!request.append) {
                _requestResult.clear()
            }
            // if require load all pages- do it in cycle, otherwise- make a single request
            if (PreferencesHandler.instance.opdsPagingType) {
                // do a single request
                val roundResult = doRequestRound(request.request, request.append)
                if (roundResult != null) {
                    if (!currentWork!!.isCancelled) {
                        _requestResult.add(roundResult)
                        searchResultsDelegate?.receiveSearchResult(roundResult)
                    } else {
                        _liveRequestState.postValue(STATUS_CANCELLED)
                    }
                }
            } else {
                var link: String? = request.request
                while (!currentWork!!.isCancelled) {
                    if (link != null) {
                        val roundResult =
                            doRequestRound(link, appendResult) ?: break
                        if (!appendResult) {
                            appendResult = true
                        }
                        if (!currentWork!!.isCancelled) {
                            searchResultsDelegate?.receiveSearchResult(roundResult)
                            _requestResult.add(roundResult)
                            link = getNextPageLink()
                        } else {
                            _liveRequestState.postValue(STATUS_CANCELLED)
                        }
                    } else {
                        break
                    }
                }
            }
        }
    }

    private fun doRequestRound(
        request: String,
        append: Boolean
    ): SearchResult? {
        _liveRequestState.postValue(STATUS_REQUESTING)
        // make a request
        val response = UniversalWebClient().rawRequest(request)
        val statusCode = response.statusCode
        if (statusCode == 200 && response.inputStream != null) {
            val answerString = StringHelper.streamToString(response.inputStream)
            // check what answer string is opds
            if (!answerString.isNullOrEmpty() && answerString.startsWith("<?xml version=\"1.0\" encoding=\"utf-8\"?>")) {
                // got content, parse it
                _liveRequestState.postValue(STATUS_REQUESTED)
                val parser = OpdsParser(answerString)
                val results = parser.parse()
                _liveRequestState.postValue(STATUS_PARSED)
                val searchResult = SearchResult()
                searchResult.appended = append
                searchResult.size = results.size
                if (results.isNotEmpty()) {
                    searchResult.type = results[0].type
                }
                searchResult.results = results
                searchResult.filteredList = parser.filteredList
                searchResult.nextPageLink = parser.nextPageLink
                searchResult.filtered = parser.filtered
                return if (!currentWork!!.isCancelled) {
                    _liveRequestState.postValue(STATUS_READY)
                    searchResult
                } else {
                    _liveRequestState.postValue(STATUS_CANCELLED)
                    null
                }
            }
        }
        _liveRequestState.postValue(STATUS_REQUEST_ERROR)
        return null
    }

    fun getPreviousResults(): ArrayList<SearchResult> {
        return _requestResult
    }

    fun saveScrolledPosition(s: Int) {
        lastScrolled = s
    }

    fun loadInProgress(): Boolean {
        return currentWork?.isActive == true
    }

    fun getNextPageLink(): String? {
        return _requestResult.lastOrNull()?.nextPageLink
    }

    fun cancelSearch() {
        currentWork?.cancel()
    }

    fun downloadPic(book: FoundEntity, delegate: PictureLoadedDelegate) {
        viewModelScope.launch(Dispatchers.IO) {
            CoverHandler().downloadFullPic(book)
            delegate.pictureLoaded()
        }
    }

    fun checkFormatAvailability(item: DownloadLink) {
        viewModelScope.launch(Dispatchers.IO) {
            // get information about link
            val result = UniversalWebClient().rawRequest(item.url!!)
            if (result.statusCode == 200) {
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

    fun getScrolledPosition(): Int {
        return lastScrolled
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

    fun replacePreviousResults(previousResults: ArrayList<SearchResult>) {
        _requestResult.clear()
        _requestResult += previousResults
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

    fun markUnread(item: FoundEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseInstance.instance.mDatabase.readBooksDao()
                .delete(DatabaseInstance.instance.mDatabase.readBooksDao().getBookById(item.id))
        }
    }

    fun getAutocomplete(type: String): ArrayList<String> {
        return FilesHandler.getSearchAutocomplete(type)
    }

    companion object {
        const val STATUS_WAIT = "wait"
        const val STATUS_REQUESTING = "requesting"
        const val STATUS_REQUESTED = "requested"
        const val STATUS_PARSED = "parsed"
        const val STATUS_READY = "ready"
        const val STATUS_CANCELLED = "cancelled"
        const val STATUS_REQUEST_ERROR = "request error"
    }
}