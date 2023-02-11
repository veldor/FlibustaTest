package net.veldor.flibusta_test.model.selection

class BooksDownloadProgress {
    var operationStartTime: Long = 0
    var booksInQueue = 0
    var successLoads = 0
    var loadErrors = 0
    var currentlyLoadedBookName: String? = null
    var currentlyLoadedBookStartTime: Long = 0
    var bookFullSize: Long = -1
    var bookLoadedSize: Long = -1
}