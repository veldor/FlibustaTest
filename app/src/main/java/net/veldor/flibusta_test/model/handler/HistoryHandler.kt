package net.veldor.flibusta_test.model.handler

import net.veldor.flibusta_test.model.selection.HistoryItem
import java.util.*

object HistoryHandler {
    private val mHistory = Stack<HistoryItem>()

    fun addToHistory(item: HistoryItem) {
        if (!PreferencesHandler.saveOpdsHistory) {
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
}