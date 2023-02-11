package net.veldor.flibusta_test.model.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
open class BooksDownloadSchedule {

    @PrimaryKey(autoGenerate = true)
    var id = 0

    @JvmField
    var bookId = ""

    @JvmField
    var link = ""

    @JvmField
    var name = ""

    @JvmField
    var size = ""

    @JvmField
    var author = ""

    @JvmField
    var format = ""

    @JvmField
    var authorDirName = ""

    @JvmField
    var sequenceDirName = ""

    @JvmField
    var reservedSequenceName = ""
}