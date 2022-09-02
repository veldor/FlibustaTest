package net.veldor.flibusta_test.model.handler

import net.veldor.flibusta_test.model.selections.HistoryItem
import java.util.*

class HistoryHandler private constructor() {
    private val mHistory = Stack<HistoryItem>()

    fun addToHistory(item: HistoryItem) {
        if (!PreferencesHandler.instance.saveOpdsHistory) {
            item.rawResults.clear()
        }
        mHistory.push(item)
    }

    val isEmpty: Boolean
        get() = mHistory.size == 0

    val lastPage: HistoryItem?
        get() = if (mHistory.size > 0) {
            mHistory.pop()
        } else null

    companion object {
        @kotlin.jvm.JvmStatic
        var instance: HistoryHandler = HistoryHandler()
            private set
    }
}