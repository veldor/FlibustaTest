package net.veldor.flibusta_test.model.selections.subscribe

import net.veldor.flibusta_test.model.file.MyFileReader


class SubscribeGenre private constructor() : SubscribeType(){

    override val subscribeName = "genre"

    companion object {
        @JvmStatic
        var instance: SubscribeGenre = SubscribeGenre()
            private set
    }
    init {
        subscribeFileName = MyFileReader.GENRES_SUBSCRIBE_FILE
        refreshSubscribeList()
    }
}