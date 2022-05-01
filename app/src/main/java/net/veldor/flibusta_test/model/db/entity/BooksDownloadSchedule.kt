package net.veldor.flibusta_test.model.db.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import net.veldor.flibusta_test.model.selections.CurrentBookDownloadProgress

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