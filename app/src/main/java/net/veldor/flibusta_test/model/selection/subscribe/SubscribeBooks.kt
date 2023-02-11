package net.veldor.flibusta_test.model.selection.subscribe

import net.veldor.flibusta_test.model.util.MyFileReader


object SubscribeBooks : SubscribeType(){
    override val subscribeName = "book"

    init {
        subscribeFileName = MyFileReader.BOOKS_SUBSCRIBE_FILE
        refreshSubscribeList()
    }
}