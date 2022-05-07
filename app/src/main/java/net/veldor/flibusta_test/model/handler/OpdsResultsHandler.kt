package net.veldor.flibusta_test.model.handler

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import net.veldor.flibusta_test.model.selections.opds.SearchResult

class OpdsResultsHandler private constructor() {
    fun clear() {
        _requestResult = arrayListOf()
    }

    fun add(roundResult: SearchResult) {
        _requestResult.add(roundResult)
        // count all results, if it is too much- notify about it
        if (PreferencesHandler.instance.saveOpdsHistory && !PreferencesHandler.instance.disableHistoryMessageViewed) {
            var counter = 0
            _requestResult.forEach {
                it.results.forEach { _ ->
                    counter++
                    if (counter > 500) {
                        _livePossibleMemoryOverflow.postValue(true)
                        return
                    }
                }
            }
        }
    }

    fun getResults(): java.util.ArrayList<SearchResult> {
        return _requestResult
    }

    fun set(previousResults: java.util.ArrayList<SearchResult>) {
        _requestResult = previousResults
    }

    private val _livePossibleMemoryOverflow: MutableLiveData<Boolean> = MutableLiveData()
    val livePossibleMemoryOverflow: LiveData<Boolean> = _livePossibleMemoryOverflow
    val resultsSize: Int
        get() {
            return _requestResult.size
        }
    private var _requestResult: ArrayList<SearchResult> = arrayListOf()

    companion object {
        @kotlin.jvm.JvmStatic
        var instance: OpdsResultsHandler = OpdsResultsHandler()
            private set
    }
}