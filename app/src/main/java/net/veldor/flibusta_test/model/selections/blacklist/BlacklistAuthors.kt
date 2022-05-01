package net.veldor.flibusta_test.model.selections.blacklist

import net.veldor.flibusta_test.model.file.MyFileReader

class BlacklistAuthors private constructor() : BlacklistType() {
    override val blacklistName = "author"

    companion object {

        @JvmStatic
        var instance: BlacklistAuthors = BlacklistAuthors()
            private set
    }

    init {
        blacklistFileName = MyFileReader.AUTHORS_BLACKLIST_FILE
        refreshBlacklist()
    }
}