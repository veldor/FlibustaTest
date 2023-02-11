package net.veldor.flibusta_test.model.handler

import android.content.Context
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.selection.DownloadFormat

object FormatHandler {

    private val formats = listOf(
        DownloadFormat("fb2", "application/fb2"),
        DownloadFormat("fb2", "application/fb2+zip"),
        DownloadFormat("mobi", "application/x-mobipocket-ebook"),
        DownloadFormat("epub", "application/epub"),
        DownloadFormat("epub", "application/epub+zip"),
        DownloadFormat("pdf", "application/pdf"),
        DownloadFormat("pdf", "application/pdf+zip"),
        DownloadFormat("djvu", "application/djvu"),
        DownloadFormat("html", "application/html"),
        DownloadFormat("html", "application/html+zip"),
        DownloadFormat("doc", "application/msword"),
        DownloadFormat("doc", "application/doc"),
        DownloadFormat(
            "docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        ),
        DownloadFormat("txt", "application/txt"),
        DownloadFormat("txt", "application/txt+zip"),
        DownloadFormat("rtf", "application/rtf"),
        DownloadFormat("rtf", "application/rtf+zip"),
        DownloadFormat("zip", "application/zip"),
        DownloadFormat("chm", "application/vnd.ms-htmlhelp"),
        DownloadFormat("htm", "application/htm"),
        DownloadFormat("htm", "application/htm+zip"),
        DownloadFormat("prc", "application/prc"),
        DownloadFormat("prc", "application/prc+zip"),
    )

    fun getAllFormats(): List<DownloadFormat> {
        return formats
    }

    fun getShortFromFullMime(fullMime: String?): String {
        formats.forEach {
            if (it.longName == fullMime) {
                return it.shortName
            }
        }
        Log.d("surprise", "FormatHandler.kt 53: not found full mime $fullMime")
        return "--"
    }

    fun getShortFromFullMimeWithoutZip(fullMime: String?): String {
        formats.forEach {
            if (it.longName == fullMime) {
                return it.shortName.replace(".zip", "")
            }
        }
        return "--"
    }

    fun getTextColor(fullMime: String?, context: Context): Int {
        when (fullMime) {
            "application/fb2+zip" -> {
                return ResourcesCompat.getColor(
                    App.instance.resources,
                    R.color.fb2_color,
                    context.theme
                )
            }
            "application/x-mobipocket-ebook" -> {
                return ResourcesCompat.getColor(
                    App.instance.resources,
                    R.color.mobi_color,
                    context.theme
                )
            }
            "application/epub+zip" -> {
                return ResourcesCompat.getColor(
                    App.instance.resources,
                    R.color.epub_color,
                    context.theme
                )
            }
            "application/pdf" -> {
                return ResourcesCompat.getColor(
                    App.instance.resources,
                    R.color.pdf_color,
                    context.theme
                )
            }
            "application/djvu" -> {
                return ResourcesCompat.getColor(
                    App.instance.resources,
                    R.color.djvu_color,
                    context.theme
                )
            }
            "application/html+zip" -> {
                return ResourcesCompat.getColor(
                    App.instance.resources,
                    R.color.html_color,
                    context.theme
                )
            }
            "application/msword" -> {
                return ResourcesCompat.getColor(
                    App.instance.resources,
                    R.color.doc_color,
                    context.theme
                )
            }
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> {
                return ResourcesCompat.getColor(
                    App.instance.resources,
                    R.color.docx_color,
                    context.theme
                )
            }
            "application/txt+zip" -> {
                return ResourcesCompat.getColor(
                    App.instance.resources,
                    R.color.txt_color,
                    context.theme
                )
            }
            "application/rtf+zip" -> {
                return ResourcesCompat.getColor(
                    App.instance.resources,
                    R.color.rtf_color,
                    context.theme
                )
            }
            "application/zip" -> {
                return ResourcesCompat.getColor(
                    App.instance.resources,
                    R.color.zip_color,
                    context.theme
                )
            }
            "application/vnd.ms-htmlhelp" -> {
                return ResourcesCompat.getColor(
                    App.instance.resources,
                    R.color.chm_color,
                    context.theme
                )
            }
        }
        return ResourcesCompat.getColor(context.resources, R.color.invertable_black, context.theme)
    }

    fun isSame(mime: String?, type: String?): Boolean {
        if(type != null){
            formats.forEach {
                if (it.longName == mime) {
                    return it.shortName.contains(type)
                }
            }
        }
        return false
    }

    fun getFullFromShortMime(shortMime: String): String {
        formats.forEach {
            if(it.shortName == shortMime){
                return it.longName
            }
        }
        return "application/octet-stream"
    }
}