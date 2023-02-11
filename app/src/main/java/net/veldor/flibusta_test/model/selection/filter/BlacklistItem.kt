package net.veldor.flibusta_test.model.selection.filter

class BlacklistItem(var name: String, val type: String){

    companion object{
        const val TYPE_BOOK = "book"
        const val TYPE_AUTHOR = "author"
        const val TYPE_GENRE = "genre"
        const val TYPE_SEQUENCE = "sequence"
        const val TYPE_FORMAT = "format"
    }
}