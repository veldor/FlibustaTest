package net.veldor.tor_client.model.managers

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.zip.ZipInputStream

class ZipFileManager {
    @Throws(Exception::class)
    fun extractZipFromInputStream(inputStream: InputStream?, outputPathDir: String) {
        val outputFile: File = File(removeEndSlash(outputPathDir))
        if (!outputFile.isDirectory) {
            check(outputFile.mkdir()) { "ZipFileManager cannot create output dir $outputPathDir" }
        }
        ZipInputStream(inputStream).use { zipInputStream ->
            var zipEntry = zipInputStream.nextEntry
            while (zipEntry != null) {
                if (zipEntry.isDirectory) {
                    val fileName = zipEntry.name
                    val fileFullName =
                        File(outputPathDir + "/" + removeEndSlash(fileName))
                    if (!fileFullName.isDirectory) {
                        check(fileFullName.mkdirs()) { "ZipFileManager cannot create output dirs structure: dir " + fileFullName.absolutePath }
                    }
                } else {
                    val fileName = zipEntry.name
                    val fileFullName =
                        File(outputPathDir + "/" + removeEndSlash(fileName))
                    val fileParent = File(
                        removeEndSlash(
                            Objects.requireNonNull(fileFullName.parent)
                        )
                    )
                    if (!fileParent.isDirectory) {
                        check(fileParent.mkdirs()) { "ZipFileManager cannot create output dirs structure: dir " + fileParent.absolutePath }
                    }
                    FileOutputStream(fileFullName).use { outputStream ->
                        copyData(
                            zipInputStream,
                            outputStream
                        )
                    }
                }
                zipEntry = zipInputStream.nextEntry
            }
        }
    }

    private fun removeEndSlash(path: String): String {
        if (path.trim().endsWith("/")) {
            return path.trim().removeSuffix("/")
        }
        return path
    }

    @Throws(java.lang.Exception::class)
    private fun copyData(`in`: InputStream, out: OutputStream) {
        val buffer = ByteArray(8 * 1024)
        var len: Int
        while (`in`.read(buffer).also { len = it } > 0) {
            out.write(buffer, 0, len)
        }
    }
}