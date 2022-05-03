package net.veldor.flibusta_test.model.handler

import android.util.Log
import net.veldor.flibusta_test.model.components.Translator
import net.veldor.flibusta_test.model.db.DatabaseInstance
import net.veldor.flibusta_test.model.helper.UrlHelper
import net.veldor.flibusta_test.model.selections.DownloadLink
import net.veldor.flibusta_test.model.db.entity.BooksDownloadSchedule
import net.veldor.flibusta_test.model.web.UniversalWebClient

object DownloadLinkHandler {
    fun addDownloadLink(link: DownloadLink) {
        val newScheduleElement = BooksDownloadSchedule()
        Log.d("surprise", "DownloadLinkHandler.kt 10: here")
        newScheduleElement.bookId = link.id!!
        newScheduleElement.author = link.author!!
        newScheduleElement.link = link.url!!
        newScheduleElement.format = link.mime!!
        newScheduleElement.size = link.size ?: "0"
        newScheduleElement.authorDirName = link.authorDirName!!
        newScheduleElement.sequenceDirName = link.sequenceDirName!!
        newScheduleElement.reservedSequenceName = link.reservedSequenceName
        newScheduleElement.name = UrlHelper.getBookName(link)
        DatabaseInstance.instance.mDatabase.booksDownloadScheduleDao().insert(newScheduleElement)
    }

    fun createDownloadLinkFromHref(link: String): DownloadLink? {
        Log.d("surprise", "DownloadLinkHandler.kt 29: request $link")
        val info = UniversalWebClient().noMirrorRawRequest(link, true)
        Log.d("surprise", "DownloadLinkHandler.kt 31: result ${info.statusCode}")
        if (info.statusCode < 400) {
            val downloadLink = DownloadLink()
            downloadLink.url = link.replaceBefore("/b/", "")
            info.headers?.forEach { header ->
                when (header.key) {
                    "Content-Disposition" -> {
                        val bookName =
                            header.value.substringAfter("filename=").replace("\"", "")
                        Log.d("surprise", "DownloadLinkHandler.kt 40: $bookName")
                        val shortMime: String
                        val inZip: Boolean
                        // check format
                        if (bookName.endsWith(".zip")) {
                            val nameWithoutZip = bookName.dropLast(4)
                            shortMime = nameWithoutZip.substringAfterLast(".")
                            inZip = true
                        } else {
                            shortMime = bookName.substringAfterLast(".")
                            inZip = false
                        }
                        val targetMime = if (inZip) "$shortMime.zip" else shortMime
                        downloadLink.mime =
                            FormatHandler.getFullFromShortMime(targetMime)
                        downloadLink.id = link.replace("fb2", "").filter { it.isDigit() }
                        // get author and book name
                        val clearName = bookName.substringBefore(".")
                        val contentArray = clearName.split("_")
                        val aName: String
                        var bName: String
                        val sName: String
                        if (contentArray.size > 1) {
                            aName = contentArray[0]
                            bName = clearName.replace(aName + "_", "")
                            // попробую найти имя серии
                            if(bName.contains("_")){
                                sName = bName.substringBeforeLast("_").substringBeforeLast("_")
                                bName = bName.replace(sName + "_", "")
                                Log.d("surprise", "DownloadLinkHandler.kt 69: sequence name is $sName")
                            }
                            else{
                                sName = ""
                            }

                        } else {
                            aName = ""
                            bName = clearName
                            sName = ""
                        }
                        // translate it to Cyrillic
                        val cyrillicAuthor = Translator.translateToRussian(aName)
                        val cyrillicBookName = Translator.translateToRussian(bName)
                        val cyrillicSequenceName = Translator.translateToRussian(sName)
                        downloadLink.author = cyrillicAuthor
                        downloadLink.authorDirName = cyrillicAuthor
                        downloadLink.name =
                            cyrillicBookName.replace("-", " ")
                        downloadLink.sequenceDirName = cyrillicSequenceName.replace("-", " ")
                    }
                    "Content-Length" -> {
                        downloadLink.size = GrammarHandler.getTextSize(header.value.toLong())
                    }
                }
            }
            return downloadLink
        }
        Log.d("surprise", "DownloadLinkHandler.kt 95: wrong link check")
        return null
    }
}