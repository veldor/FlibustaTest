package net.veldor.flibusta_test.model.selection.subscribe

import net.veldor.flibusta_test.model.util.MyFileReader


object SubscribeSequences : SubscribeType() {
    override val subscribeName = "sequence"

    init {
        subscribeFileName = MyFileReader.SEQUENCES_SUBSCRIBE_FILE
        refreshSubscribeList()
    }
}