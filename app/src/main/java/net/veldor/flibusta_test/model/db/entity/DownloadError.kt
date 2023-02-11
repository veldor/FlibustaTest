package net.veldor.flibusta_test.model.db.entity

import androidx.room.Entity

@Entity
class DownloadError: BooksDownloadSchedule() {
    fun copyDataFrom(book: BooksDownloadSchedule) {
        bookId = book.bookId
        link = book.link
        name = book.name
        author = book.author
        authorDirName = book.authorDirName
        format = book.format
        reservedSequenceName = book.reservedSequenceName
        sequenceDirName = book.sequenceDirName
        size = book.size
    }

    @JvmField
    var error = ""
}