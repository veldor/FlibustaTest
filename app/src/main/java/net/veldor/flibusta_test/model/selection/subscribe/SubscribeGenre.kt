package net.veldor.flibusta_test.model.selection.subscribe

import net.veldor.flibusta_test.model.util.MyFileReader


object SubscribeGenre : SubscribeType(){

    override val subscribeName = "genre"

    init {
        subscribeFileName = MyFileReader.GENRES_SUBSCRIBE_FILE
        refreshSubscribeList()
    }
}