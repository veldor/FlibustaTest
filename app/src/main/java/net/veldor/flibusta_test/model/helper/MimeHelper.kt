package net.veldor.flibusta_test.model.helper

object MimeHelper {
    private val DOWNLOAD_MIMES: HashMap<String, String> = hashMapOf(
        "application/fb2+zip" to "fb2",
        "application/fb2" to "fb2",
        "application/x-mobipocket-ebook" to "mobi",
        "application/epub+zip" to "epub",
        "application/epub" to "epub",
        "application/pdf" to "pdf",
        "application/djvu" to "djvu",
        "application/msword" to "doc",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" to "docx",
        "application/html+zip" to "html",
        "application/txt+zip" to "txt",
        "application/rtf+zip" to "rtf",
        "application/zip" to "zip",
        "application/vnd.ms-htmlhelp" to "chm",
        "application/prc" to "prc",
    )

    @JvmStatic
    fun getDownloadMime(mime: String): String {
        if (mime == "application/epub") {
            return "epub"
        }
        if (mime == "application/djvu+zip") {
            return "djvu"
        }
        if (mime == "application/doc") {
            return "doc"
        }
        if (mime == "application/docx") {
            return "docx"
        }
        if (mime == "application/jpg") {
            return "jpg"
        }
        if (mime == "application/pdf+zip") {
            return "pdf"
        }
        if (mime == "application/rtf+zip") {
            return "rtf"
        }
        if (mime == "application/txt") {
            return "txt"
        }
        if (mime == "application/vnd.ms-htmlhelp") {
            return "chm"
        }
        return if (DOWNLOAD_MIMES.containsKey(mime)) {
            DOWNLOAD_MIMES[mime]!!
        } else mime
    }

    fun getMimeFromFileName(name: String): String {
        DOWNLOAD_MIMES.forEach {
            if (name.endsWith(it.value)) {
                return it.key
            }
        }
        return "application/txt"
    }

    fun clearName(name: String): String {
        if (name.endsWith(".zip")) {
            return name.removeSuffix(".zip").substringBeforeLast(".")
        }
        return name.substringBeforeLast(".")
    }

    fun getMimeFromLink(text: String): String {
        val extension = text.substringAfterLast("/")
        DOWNLOAD_MIMES.forEach {
            if (extension == it.value) {
                return it.key
            }
        }
        return "application/txt"
    }
}