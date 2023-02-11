package net.veldor.flibusta_test.model.view_model

import android.content.Context
import android.text.format.Formatter
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.connection.Connector
import net.veldor.flibusta_test.model.db.DatabaseInstance
import net.veldor.flibusta_test.model.db.entity.DownloadedBooks
import net.veldor.flibusta_test.model.db.entity.ReadedBooks
import net.veldor.flibusta_test.model.delegate.BookInfoAddedDelegate
import net.veldor.flibusta_test.model.delegate.OpdsObserverDelegate
import net.veldor.flibusta_test.model.delegate.PictureLoadedDelegate
import net.veldor.flibusta_test.model.handler.*
import net.veldor.flibusta_test.model.helper.MimeHelper
import net.veldor.flibusta_test.model.helper.UrlHelper
import net.veldor.flibusta_test.model.parser.OpdsParser
import net.veldor.flibusta_test.model.selection.*
import net.veldor.tor_client.model.helper.StringHelper
import java.util.*
import kotlin.collections.ArrayList


open class OpdsViewModel : ViewModel() {
    private var lastDialog: AlertDialog? = null
    private var checkWork: Job? = null
    private var checkBooksWork: Job? = null
    private var bookInfoDelegate: BookInfoAddedDelegate? = null
    private var currentWork: Job? = null

    fun request(
        request: RequestItem?
    ) {
        if (request == null) {
            return
        }
        OpdsStatement.requestLaunched()
        if (request.addToHistory) {
            // add current condition to history
            OpdsStatement.saveToHistory()
        }
        OpdsStatement.setCurrentRequest(request.request)
        CoverHandler.dropPreviousLoading()
        if (currentWork != null) {
            currentWork!!.cancel()
        }
        currentWork = viewModelScope.launch(Dispatchers.IO) {
            // получу результаты запроса
            getData(request)
            if (currentWork?.isCancelled != true) {
                OpdsStatement.requestFinished()
            }
        }
    }

    private fun getData(request: RequestItem) {
        val response = Connector().rawRequest(request.request, false)
        if (currentWork?.isCancelled == true) {
            OpdsStatement.requestCancelled()
            return
        }
        if (response.inputStream != null) {
            val answerString = StringHelper.streamToString(response.inputStream)
            // check what answer string is opds
            if (!answerString.isNullOrEmpty() && answerString.startsWith("<?xml version=\"1.0\" encoding=\"utf-8\"?>")) {
                // сохраню строку в список результатов
                OpdsStatement.addRawResults(answerString)
                // получу результаты запроса
                val parser = OpdsParser(answerString)
                parser.parse(currentWork)
            } else {
                // ошибка запроса
                OpdsStatement.requestFailed(request, response)
            }
        } else {
            // ошибка запроса
            OpdsStatement.requestFailed(request, response)
        }
    }

    fun loadInProgress(): Boolean {
        return currentWork?.isActive == true
    }

    fun cancelSearch() {
        currentWork?.cancel()
        OpdsStatement.requestCancelled()
    }

    fun downloadPic(book: FoundEntity, delegate: PictureLoadedDelegate) {
        viewModelScope.launch(Dispatchers.IO) {
            CoverHandler().downloadFullPic(book)
            delegate.pictureLoaded()
        }
    }

    fun checkFormatAvailability(context: Context, item: DownloadLink, callback: (String) -> Unit) {
        checkWork?.cancel()
        checkWork = viewModelScope.launch(Dispatchers.IO) {
            // get information about link
            val result = Connector().rawRequest(item.url!!, false)
            if (isActive) {
                if (result.statusCode == 200 && result.contentLength > 0) {
                    callback(
                        String.format(
                            Locale.ENGLISH,
                            context.getString(R.string.format_available_pattern),
                            MimeHelper.getDownloadMime(item.mime!!),
                            Formatter.formatFileSize(
                                App.instance,
                                result.contentLength.toLong()
                            )
                        )

                    )
                } else {
                    callback(context.getString(R.string.format_unavailable_message))
                }
            }
        }
    }


    fun addToDownloadQueue(selectedLink: DownloadLink?) {
        if (selectedLink != null) {
            DownloadLinkHandler.addDownloadLink(selectedLink)
            if (PreferencesHandler.downloadAutostart) {
                DownloadHandler.startDownload()
            }
        }
    }

    fun markRead(item: FoundEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (item.id != null && DatabaseInstance.mDatabase.readBooksDao()
                    .getBookById(item.id) == null
            ) {
                val newItem = ReadedBooks()
                newItem.bookId = item.id!!
                DatabaseInstance.mDatabase.readBooksDao().insert(newItem)
            }
        }
    }

    fun markDownloaded(item: FoundEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (item.id != null && DatabaseInstance.mDatabase.downloadedBooksDao()
                    .getBookById(item.id) == null
            ) {
                val newItem = DownloadedBooks()
                newItem.bookId = item.id!!
                DatabaseInstance.mDatabase.downloadedBooksDao().insert(newItem)
            }
        }
    }

    fun markUnread(item: FoundEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseInstance.mDatabase.readBooksDao()
                .delete(DatabaseInstance.mDatabase.readBooksDao().getBookById(item.id))
        }
    }

    fun markNoDownloaded(item: FoundEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseInstance.mDatabase.downloadedBooksDao()
                .delete(
                    DatabaseInstance.mDatabase.downloadedBooksDao().getBookById(item.id)
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
                                    book.sequencesComplex = link.sequenceDirName ?: ""
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

    fun readyToCreateBookmark(): Boolean {
        return OpdsStatement.getCurrentRequest() != null
    }

    fun getBookmarkLink(): String? {
        return OpdsStatement.getCurrentRequest()
    }

    fun removeBookmark() {
        BookmarkHandler.deleteBookmark(OpdsStatement.getCurrentRequest())
    }

    fun drawBadges(delegate: OpdsObserverDelegate) {
        viewModelScope.launch(Dispatchers.IO) {
            Thread.sleep(500)
            delegate.drawBadges()
        }

    }

    fun restoreLastDialog(): AlertDialog? {
        return lastDialog
    }

    fun login(login: String, password: String, callback: (result: Boolean) -> Unit?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = Connector().requestLogin(login, password)
                Log.d("surprise", "OpdsViewModel: 270 ${result.statusCode}")
                if (result.statusCode != 401) {
                    val string = StringHelper.streamToString(result.inputStream)
                    Log.d("surprise", "OpdsViewModel: 275 $string")
                    if (string?.startsWith("<?xml version=\"1.0\" encoding=\"utf-8\"?>") == true) {
                        result.headers.forEach {
                            if (it.key == "Set-Cookie") {
                                // save session cookie
                                val cookie = it.value.substringBefore(";")
                                Log.d("surprise", "OpdsViewModel: 281 saving cookie $cookie")
                                PreferencesHandler.authCookie = cookie
                            }
                        }
                        callback(true)
                        return@launch
                    }
                }
            } catch (_: Throwable) {
            }
            callback(false)
        }
    }
}