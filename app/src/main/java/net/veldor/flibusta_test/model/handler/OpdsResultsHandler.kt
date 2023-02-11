package net.veldor.flibusta_test.model.handler

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import net.veldor.flibusta_test.model.selection.SearchResult

object OpdsResultsHandler {
    fun clear() {
        _requestResult = arrayListOf()
    }

    fun add(roundResult: SearchResult) {
        Log.d("surprise", "OpdsResultsHandler.kt 14: adding to previously loaded results")
        _requestResult.add(roundResult)
        // count all results, if it is too much- notify about it
        if (PreferencesHandler.saveOpdsHistory && !PreferencesHandler.disableHistoryMessageViewed) {
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

    fun set(previousResults: java.util.ArrayList<SearchResult>) {
        Log.d("surprise", "set: ${previousResults.size}")
        _requestResult = previousResults
    }

    private val _livePossibleMemoryOverflow: MutableLiveData<Boolean> = MutableLiveData()
    val livePossibleMemoryOverflow: LiveData<Boolean> = _livePossibleMemoryOverflow
    private var _requestResult: ArrayList<SearchResult> = arrayListOf()

}