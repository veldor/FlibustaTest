package net.veldor.flibusta_test.model.selection

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import net.veldor.flibusta_test.model.delegate.OpdsObserverDelegate
import net.veldor.flibusta_test.model.handler.CoverHandler
import net.veldor.flibusta_test.model.handler.HistoryHandler
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.parser.OpdsParser
import net.veldor.tor_client.model.connection.WebResponse

object OpdsStatement {

    init {
        Log.d("surprise", "OpdsStatement: 16 i initialized")
    }

    const val STATE_AWAITING = 0
    const val STATE_LOADING = 1
    const val STATE_READY = 2
    const val STATE_ERROR = 3
    const val STATE_CANCELLED = 4

    var notInitialized = true
    var noResults = false

    private var filteredCounter: Int = 0
    private var pressedItemId: String? = null
    var delegate: OpdsObserverDelegate? = null
    val blockedEntities: ArrayList<FoundEntity> = arrayListOf()
    private val rawResults = ArrayList<String>()
    val results: ArrayList<FoundEntity> = arrayListOf()
    private var currentRequest: String? = null
    private val _requestState: MutableLiveData<Int> = MutableLiveData(STATE_AWAITING)
    val requestState: LiveData<Int> = _requestState
    private var nextPageLink: String? = null

    fun requestLaunched() {
        _requestState.postValue(STATE_LOADING)
        noResults = false
    }

    fun setCurrentRequest(request: String) {
        currentRequest = request
    }

    fun requestFailed(request: RequestItem, response: WebResponse) {
        _requestState.postValue(STATE_ERROR)
        delegate?.hasConnectionError(request, response)
    }

    fun requestFinished() {
        _requestState.postValue(STATE_READY)
    }

    fun setNextPageLink(value: String?) {
        nextPageLink = value
    }

    fun isNextPageLink(): Boolean {
        return nextPageLink != null
    }

    fun addParsedResult(foundedEntity: FoundEntity?) {
        if (foundedEntity != null) {
            delegate?.itemInserted(foundedEntity)
            if (foundedEntity.coverUrl != null && foundedEntity.coverUrl!!.isNotEmpty() && PreferencesHandler.showCovers && !PreferencesHandler.showCoversByRequest) {
                // load pic in new Thread
                CoverHandler().loadPic(foundedEntity)
            }
        }
    }

    fun getNextPageLink(): String? {
        return nextPageLink
    }

    fun addRawResults(answerString: String) {
        rawResults.add(answerString)
    }

    fun saveToHistory() {
        if (rawResults.isNotEmpty()) {
            // создам новый элемент истории на основе текущего
            val historyItem = HistoryItem()
            // следующая страница
            historyItem.nextPageLink = nextPageLink
            // текущий запрос
            historyItem.currentRequest = currentRequest
            Log.d("surprise", "saveToHistory: save to history current request $currentRequest")
            // сырые результаты
            rawResults.forEach {
                historyItem.rawResults.add(it)
            }
            // активный пункт выдачи
            historyItem.pressedItemId = pressedItemId
            HistoryHandler.addToHistory(historyItem)
            // обнулю все текущие значения
            nextPageLink = null
            currentRequest = null
            filteredCounter = 0
            rawResults.clear()
            blockedEntities.clear()
            pressedItemId = null
            Log.d("surprise", "saveToHistory: history saved!")
        } else {
            Log.d("surprise", "saveToHistory: empty state, do not save")
        }
    }

    fun setPressedItem(item: FoundEntity?) {
        Log.d("surprise", "setPressedItem: set pressed ${item?.link}")
        pressedItemId = item?.link
    }

    fun load(lastResults: HistoryItem?) {
        if (lastResults != null) {
            // загружу данные
            nextPageLink = lastResults.nextPageLink
            currentRequest = lastResults.currentRequest
            rawResults.clear()
            filteredCounter = 0
            blockedEntities.clear()
            pressedItemId = lastResults.pressedItemId
            lastResults.rawResults.forEach {
                rawResults.add(it)
                val parser = OpdsParser(it)
                parser.parse(null)
            }
        }
    }

    fun requestCancelled() {
        _requestState.postValue(STATE_CANCELLED)
    }

    fun addFilteredResult(foundedEntity: FoundEntity) {
        if (PreferencesHandler.showFilterStatistics) {
            blockedEntities.add(foundedEntity)
        } else {
            ++filteredCounter
        }
        delegate?.itemFiltered(foundedEntity)
    }

    fun getBlockedResultsSize(): Int {
        if (PreferencesHandler.showFilterStatistics) {
            return blockedEntities.size
        }
        return filteredCounter
    }

    fun getCurrentRequest(): String? {
        return currentRequest
    }

    fun getPressedItemId(): String? {
        return pressedItemId
    }

    fun prepareRequestFromHistory() {
        nextPageLink = null

    }

    fun setNoValuesFound() {
        noResults = true
        delegate?.hasNoResults()
    }

    fun setValuesFound() {
        noResults = false
        delegate?.resultsFound()
    }

}
