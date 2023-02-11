package net.veldor.flibusta_test.model.data_source

import androidx.paging.PositionalDataSource
import net.veldor.flibusta_test.model.db.DatabaseInstance
import net.veldor.flibusta_test.model.db.entity.DownloadedBooks

internal class DownloadFilesDataSource :
    PositionalDataSource<DownloadedBooks>() {
    override fun loadInitial(
        params: LoadInitialParams,
        callback: LoadInitialCallback<DownloadedBooks>
    ) {
        val totalCount = DatabaseInstance.mDatabase.downloadedBooksDao().count()

        val result = DatabaseInstance.mDatabase.downloadedBooksDao()
            .request(params.requestedStartPosition, params.requestedLoadSize)
        callback.onResult(result, 0, totalCount)
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<DownloadedBooks>) {

        val result = DatabaseInstance.mDatabase.downloadedBooksDao()
            .request(params.startPosition, params.loadSize)
        callback.onResult(result)
    }
}