package net.veldor.flibusta_test.model.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import net.veldor.flibusta_test.model.db.entity.BooksDownloadSchedule
import net.veldor.flibusta_test.model.db.entity.DownloadError

@Dao
interface DownloadErrorDao {
    @get:Query("SELECT COUNT(id) FROM DownloadError")
    val queueSize: Int

    @get:Query("SELECT * FROM DownloadError LIMIT 1")
    val firstQueuedBook: DownloadError?

    @Query("SELECT * FROM DownloadError WHERE bookId = :id")
    fun getBookById(id: String?): DownloadError?

    @get:Query("SELECT * FROM DownloadError")
    val allBooks: List<DownloadError>?

    @get:Query("SELECT * FROM DownloadError")
    val allBooksLive: LiveData<List<DownloadError>>?

    @Insert
    fun insert(book: DownloadError?)

    @Delete
    fun delete(book: DownloadError?)
}