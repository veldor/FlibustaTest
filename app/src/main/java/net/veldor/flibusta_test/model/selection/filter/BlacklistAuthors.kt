package net.veldor.flibusta_test.model.selection.filter

import net.veldor.flibusta_test.model.util.MyFileReader


object BlacklistAuthors  : BlacklistType() {
    override val blacklistName = "author"

    init {
        blacklistFileName = MyFileReader.AUTHORS_BLACKLIST_FILE
        refreshBlacklist()
    }
}