package net.veldor.flibusta_test.model.selection.filter

import net.veldor.flibusta_test.model.util.MyFileReader


object BlacklistFormat : BlacklistType(){

    override val blacklistName = "format"

    init {
        blacklistFileName = MyFileReader.FORMAT_BLACKLIST_FILE
        refreshBlacklist()
    }
}