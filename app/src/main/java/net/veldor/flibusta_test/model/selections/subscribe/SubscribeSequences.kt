package net.veldor.flibusta_test.model.selections.subscribe

import net.veldor.flibusta_test.model.file.MyFileReader

class SubscribeSequences private constructor() : SubscribeType() {
    override val subscribeName = "sequence"

    companion object {

        @JvmStatic
        var instance: SubscribeSequences = SubscribeSequences()
            private set
    }

    init {
        subscribeFileName = MyFileReader.SEQUENCES_SUBSCRIBE_FILE
        refreshSubscribeList()
    }
}