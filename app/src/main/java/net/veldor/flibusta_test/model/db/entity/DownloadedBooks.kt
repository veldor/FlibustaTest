package net.veldor.flibusta_test.model.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class DownloadedBooks {
    @PrimaryKey(autoGenerate = true)
    var id = 0
    var bookId = ""
    var destination: String? = null
    var relativePath: String? = null
}