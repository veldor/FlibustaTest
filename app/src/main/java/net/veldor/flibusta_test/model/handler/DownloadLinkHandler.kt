package net.veldor.flibusta_test.model.handler

import net.veldor.flibusta_test.model.db.DatabaseInstance
import net.veldor.flibusta_test.model.helper.UrlHelper
import net.veldor.flibusta_test.model.selections.DownloadLink
import net.veldor.flibusta_test.model.db.entity.BooksDownloadSchedule

object DownloadLinkHandler {
    fun addDownloadLink(link: DownloadLink) {
        val newScheduleElement = BooksDownloadSchedule()
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
}