package net.veldor.flibusta_test.model.selections.subscribe

import net.veldor.flibusta_test.model.file.MyFileReader


class SubscribeBooks private constructor() : SubscribeType(){
    override val subscribeName = "book"

    companion object {
        @JvmStatic
        var instance: SubscribeBooks = SubscribeBooks()
            private set
    }
    init {
        subscribeFileName = MyFileReader.BOOKS_SUBSCRIBE_FILE
        refreshSubscribeList()
    }
}