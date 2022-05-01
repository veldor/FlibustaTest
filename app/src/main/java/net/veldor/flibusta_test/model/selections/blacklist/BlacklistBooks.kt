package net.veldor.flibusta_test.model.selections.blacklist

import net.veldor.flibusta_test.model.file.MyFileReader

class BlacklistBooks private constructor() : BlacklistType(){
    override val blacklistName = "book"

    companion object {
        @JvmStatic
        var instance: BlacklistBooks = BlacklistBooks()
            private set
    }
    init {
        blacklistFileName = MyFileReader.BOOKS_BLACKLIST_FILE
        refreshBlacklist()
    }
}