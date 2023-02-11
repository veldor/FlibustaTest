package net.veldor.flibusta_test.model.selection

import java.io.Serializable

class DownloadLink : Serializable {
    var editedName: String? = null
    var url: String? = null
    var id: String? = null
    @JvmField
    var mime: String? = null
    var name: String? = null
    var nameInSequence: String? = null
    var author: String? = null
    var size: String? = null
    var authorDirName: String? = null
    var sequenceDirName: String? = null
    var reservedSequenceName: String = ""
}