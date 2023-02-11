package net.veldor.flibusta_test.model.selection.filter

import net.veldor.flibusta_test.model.util.MyFileReader


object BlacklistGenre : BlacklistType(){

    override val blacklistName = "genre"

    init {
        blacklistFileName = MyFileReader.GENRES_BLACKLIST_FILE
        refreshBlacklist()
    }
}