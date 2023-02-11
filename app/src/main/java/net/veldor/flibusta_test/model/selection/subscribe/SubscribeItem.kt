package net.veldor.flibusta_test.model.selection.subscribe

class SubscribeItem(var name: String, val type: String){
    companion object{
        const val TYPE_BOOK = "book"
        const val TYPE_AUTHOR = "author"
        const val TYPE_GENRE = "genre"
        const val TYPE_SEQUENCE = "sequence"
    }
}