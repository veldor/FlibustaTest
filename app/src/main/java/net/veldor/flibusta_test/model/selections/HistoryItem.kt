package net.veldor.flibusta_test.model.selections

class HistoryItem() {

    var pressedItemId: String? = null
    var currentRequest: String? = null
    var nextPageLink: String? = null
    val rawResults = ArrayList<String>()
}