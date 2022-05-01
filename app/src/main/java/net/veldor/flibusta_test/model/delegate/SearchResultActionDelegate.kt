package net.veldor.flibusta_test.model.delegate

import net.veldor.flibusta_test.model.selections.opds.SearchResult

interface SearchResultActionDelegate {
    fun receiveSearchResult(searchResult: SearchResult)
}