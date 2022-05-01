package net.veldor.flibusta_test.model.selections.blacklist

import net.veldor.flibusta_test.model.file.MyFileReader

class BlacklistSequences private constructor() : BlacklistType() {
    override val blacklistName = "sequence"

    companion object {

        @JvmStatic
        var instance: BlacklistSequences = BlacklistSequences()
            private set
    }

    init {
        blacklistFileName = MyFileReader.SEQUENCES_BLACKLIST_FILE
        refreshBlacklist()
    }
}