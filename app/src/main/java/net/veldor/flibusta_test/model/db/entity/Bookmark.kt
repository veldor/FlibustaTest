package net.veldor.flibusta_test.model.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class Bookmark {
    @PrimaryKey(autoGenerate = true)
    var id = 0
    @JvmField
    var name = ""
    @JvmField
    var link = ""
}