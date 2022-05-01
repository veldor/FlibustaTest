package net.veldor.flibusta_test.model.db.entity

import android.util.Log
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import net.veldor.flibusta_test.model.selections.CurrentBookDownloadProgress

@Entity
class DownloadError: BooksDownloadSchedule() {
    fun copyDataFrom(book: BooksDownloadSchedule) {
        bookId = book.bookId
        name = book.name
        author = book.author
        authorDirName = book.authorDirName
        format = book.format
        link = book.link
        reservedSequenceName = book.reservedSequenceName
        sequenceDirName = book.sequenceDirName
        size = book.size
    }

    @JvmField
    var error = ""
}