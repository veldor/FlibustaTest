package net.veldor.flibusta_test.model.selection.filter

import net.veldor.flibusta_test.model.util.MyFileReader


object BlacklistSequences : BlacklistType() {
    override val blacklistName = "sequence"

    init {
        blacklistFileName = MyFileReader.SEQUENCES_BLACKLIST_FILE
        refreshBlacklist()
    }
}