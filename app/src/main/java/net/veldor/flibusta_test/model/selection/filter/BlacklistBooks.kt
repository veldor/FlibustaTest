package net.veldor.flibusta_test.model.selection.filter

import net.veldor.flibusta_test.model.util.MyFileReader


object BlacklistBooks : BlacklistType(){
    override val blacklistName = "book"


    init {
        blacklistFileName = MyFileReader.BOOKS_BLACKLIST_FILE
        refreshBlacklist()
    }
}