package net.veldor.flibusta_test.model.selection.subscribe

import net.veldor.flibusta_test.model.util.MyFileReader


object SubscribeAuthors  : SubscribeType(){
    override val subscribeName = "author"

    init {
        subscribeFileName = MyFileReader.AUTHORS_SUBSCRIBE_FILE
        refreshSubscribeList()
    }
}