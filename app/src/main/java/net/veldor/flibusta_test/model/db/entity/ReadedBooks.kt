package net.veldor.flibusta_test.model.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class ReadedBooks {
    @PrimaryKey(autoGenerate = true)
    var id = 0
    var bookId:String = ""
}