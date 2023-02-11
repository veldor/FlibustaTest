package net.veldor.flibusta_test.model.interfaces

import net.veldor.flibusta_test.model.selection.DownloadLink
import net.veldor.flibusta_test.model.selection.FoundEntity

interface BookCheckInterface {
    fun checkBookAvailability(item: DownloadLink, callback: (String) -> Unit)

    fun addToDownloadQueue(item: DownloadLink)

    fun showBookDownloadOptions(book: FoundEntity)

    fun selectDownloadDir(onPrepared: () -> Unit)
}