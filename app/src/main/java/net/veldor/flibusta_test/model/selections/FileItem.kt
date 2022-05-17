package net.veldor.flibusta_test.model.selections

import androidx.documentfile.provider.DocumentFile

class FileItem {
    var name: String = ""
    var type: String = ""
    lateinit var file: DocumentFile
    var size: String? = null
}