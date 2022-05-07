package net.veldor.flibusta_test.model.selections.opds

class SearchResult {
    var appended: Boolean = false
    var size: Int = 0
    var filtered: Int = 0
    var type: String? = null
    var nextPageLink: String? = null
    var requestLink: String? = null
    lateinit var results: ArrayList<FoundEntity>
    lateinit var filteredList: ArrayList<FoundEntity>
    var clickedElementIndex: Long = -1

}