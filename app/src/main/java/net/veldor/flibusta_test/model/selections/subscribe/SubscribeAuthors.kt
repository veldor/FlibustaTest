package net.veldor.flibusta_test.model.selections.subscribe

import net.veldor.flibusta_test.model.file.MyFileReader


class SubscribeAuthors private constructor() : SubscribeType(){
    override val subscribeName = "author"

    companion object {

        @JvmStatic
        var instance: SubscribeAuthors = SubscribeAuthors()
            private set
    }
    init {
        subscribeFileName = MyFileReader.AUTHORS_SUBSCRIBE_FILE
        refreshSubscribeList()
    }
}