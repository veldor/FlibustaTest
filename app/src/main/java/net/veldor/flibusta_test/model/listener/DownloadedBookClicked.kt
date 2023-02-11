package net.veldor.flibusta_test.model.listener

import net.veldor.flibusta_test.model.db.entity.DownloadedBooks

interface DownloadedBookClicked {
    fun clicked(item: DownloadedBooks?)
}